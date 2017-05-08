/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.wls;

import general.Version;
import io.file.ReadDir;
import io.file.ReadFile;
import io.file.SecFile;
import io.file.WriteFile;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author SuMario
 */
public class WlsToolConfig extends Version{

    public static void main(String[] args) {
        final String func="WlsToolConfig::main(String[] args)";
        WlsToolConfig w = new WlsToolConfig();
                      w.getLocation();
        if ( args.length > 0 ) {
            ArrayList<String> dirs = new ArrayList();
            for(int i=0; i< args.length; i++) {
                if ( args[i].matches("-dest")   ) { w.checkConfig(args[++i]); } 
                else if ( args[i].matches("-d") ) { debug++; }
                else {  dirs.add(args[i]); }    
            }
            for ( String s : dirs ) {
                 printf(func,2, "updateConfig:"+s+":"); 
                 w.updateConfig(s);
            }
            
        } else {
            System.out.println("ERROR: need domaindiras property");
            System.exit(-1);
        }
        
        System.exit(0);
    }

    private ReadDir loc = null;
    public  void setLocation(String dir) {
        if ( dir == null || dir.isEmpty() ) { loc = new ReadDir(System.getProperty("user.home")+File.separator+"bin"); }
    }
    public ReadDir getLocation(){
          if ( loc == null ) { setLocation(null);}
          return loc;
    }
    
    private HashMap<String,WlsDomain> ar = new HashMap();
    public void updateConfig(String dir) {
        final String func=getFunc("updateConfig(String dir)"); 
        if ( dir == null || dir.isEmpty() ) { return; }
        ReadDir d = new ReadDir(dir);
        if (d.isReadable()){
            printf(func,2,"Dir:"+d.getFQDNDirName()+":  domain:"+d.getDirName()+":");
            WlsDomain wd = new WlsDomain(d.getDirName());
                      wd.setDomainLocation(d.getFQDNDirName());
                      ask4User(wd);
                      ar.put(wd.getDomainName(),wd);
                      
                      SecFile fd = new SecFile(d.getFQDNDirName()+File.separator+"domainkeys");
                      if ( ! fd.isReadableFile() ) {
                           this._needUpdate=true;
                      }
        } else {
            printf(func,2,"not a readable directory :"+dir);
        }
    }
    
    java.io.Console console = System.console();
    public void ask4User(WlsDomain wd) {
        System.out.println("INFO: check domain "+wd.getDomainName()+" from location "+wd.getDomainLocation());
        String u =  wd.getAdminUser(); 
        if ( u == null || u.isEmpty() ) {
             System.out.println("Domain Admin User [weblogic] : "); 
             String readLine = console.readLine().trim();
             if ( readLine.isEmpty() ) { u="weblogic"; } else { u=readLine; }
        }
        String p= wd.getAdminPassword();
        if ( p == null || p.isEmpty() ) {
            char[] pass = console.readPassword("Admin User "+u+" Password : ", (Object[]) new String[]{});
            p=new String(pass);
        }
        String un =  wd.getNodeUser(); 
        if ( un == null || un.isEmpty() ) {
             System.out.println("Domain NodeManager User [] : "); 
             String readLine = console.readLine().trim();
             if ( readLine.isEmpty() ) { u="weblogic"; } else { u=readLine; }
        }
        String pn= wd.getNodePassword();
        if ( pn == null || pn.isEmpty() ) {
            char[] pass = console.readPassword("Admin NodeUserUser "+pn+" Password : ", (Object[]) new String[]{});
            pn=new String(pass);
        }
        
    }

    public void checkConfig(String dest) {
          ReadDir d = new ReadDir(dest);  
          if ( ! d.isDirectory() ) {
               d.mkdirs();
          }
          setLocation(dest);
          ReadDir dn= new ReadDir(dest+File.separator+"lib");
          if ( ! dn.isDirectory() ) { this._needUpdate=true;  }

          if ( ! this._needUpdate ) {
            ReadFile df = new ReadFile(dest+File.separator+"lib"+File.separator+"OC.jar");
            if ( ! df.isReadableFile() || ! df.getFQDNFileName().replaceAll(File.separator, sepa).matches(jarfile.replaceAll(File.separator, sepa))) {
                  this._needUpdate=true;
            }
          }
          
          if ( ! this._needUpdate ) {
            ReadFile fn = new ReadFile(d.getFile("domain.info").getFQDNFileName());
            if ( fn.isReadableFile() ) {
                String[] sp = fn.readOut().toString().split("\n");
                StringBuilder sw = new StringBuilder();
                for ( String line : sp) {
                      if ( line.contains("DOMAINHOME=")) {
                          String[] tp = line.split("\"");
                          
                      }
                }
            } else { this._needUpdate=true; }  // domain.info not exist
          } 
    
    }

    private final String sepa="__@@__";
    
    private boolean _needUpdate=false;
    public boolean isUpdateNeeded() { return _needUpdate; }

    public void updateDestination(String dest) {
        
        if ( isUpdateNeeded() ) {
           ReadDir dn= new ReadDir(dest+File.separator+"lib");
           if ( ! dn.isDirectory() ) { dn.mkdirs(); }
           
           ReadFile df = new ReadFile(dest+File.separator+"lib"+File.separator+"OC.jar");
           if ( ! df.isReadableFile() || ! df.getFQDNFileName().replaceAll(File.separator, sepa).matches(jarfile.replaceAll(File.separator, sepa))) {
                WriteFile dfr=new WriteFile(jarfile);
                          dfr.copy(new File(dn.getFQDNDirName()+File.separator+"OC.jar"));
           }
           WriteFile wt = new WriteFile(dest+File.separator+"domain.info");
           StringBuilder wta = new StringBuilder();
           StringBuilder sw = wt.readOut();
           String temp = getOutString( new BufferedInputStream( WlsToolConfig.class.getResourceAsStream("/setup/config/domain.info") ) );
           if ( temp != null && ! temp.isEmpty() ) {
                for( String sp : temp.split("\n") ) {
                    wta.append(sp.trim()).append("\n");
                    if ( sp.trim().matches("### begin domain") ) {
                         boolean begin=false;
                         for( String s : sw.toString().split("\n") ) {
                             if      ( s.trim().startsWith("### begin domain") ) { begin=true; }
                             else if ( s.trim().startsWith("### end   domain") ) { begin=false; }
                             if ( begin && ! s.startsWith("#") && s.contains("DOMAINHOME") ) {
                                    String[] tp = s.split("\"");  String[] mp = tp[2].split(File.separator);
                                    System.out.println("DOMAIN:"+mp[ mp.length-1]+": PATH:"+tp[2]+":");
                                         
                             }
                         }
                        
                    }
                } 
           }
           
           //System.out.println(txt);
        } else {
            System.out.println("no update needed");
        }
    }

    synchronized String getOutString(BufferedInputStream in) {
        final String func=getFunc("getOutString(BufferedInputStream in)");
        StringBuilder st = new StringBuilder();
        try {
            int c;  StringBuilder sw= new StringBuilder();
            while( (c=in.available()) >0 ) {
                byte[] b = new byte[c];
                c=in.read(b);
                if (sw.length() >0 ) { sw.delete(0, sw.capacity()); }
                for(int i=0; i<c; i++ ) {  sw.append( (char)b[i]  );    }
                st.append(sw.toString());
            }
        } catch(IOException io) {
            printf(func,1,"ERROR: resoucse could not loaded - error "+io.getMessage(), io);
        }  
        return st.toString();
    }
    
    
}
