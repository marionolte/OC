/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.wls;

import general.Version;
import io.file.ReadDir;
import io.file.SecFile;
import io.file.WriteFile;
import java.io.File;
import java.util.ArrayList;

/**
 *
 * @author SuMario
 */
public class WlsToolConfig extends Version{

    public static void main(String[] args) {
        WlsToolConfig w = new WlsToolConfig();
                      w.getLocation();
        if ( args.length > 0 ) {
            for(int i=0; i< args.length; i++) {
                if ( args[i].matches("-dest") ){ w.checkConfig(args[++i]); } 
                else {
                    w.updateConfig(args[i]);
                }    
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
    
    private ArrayList<WlsDomain> ar = new ArrayList();
    public void updateConfig(String dir) {
        final String func=getFunc("updateConfig(String dir)"); 
        if ( dir == null || dir.isEmpty() ) { return; }
        ReadDir d = new ReadDir(dir);
        if (d.isReadable()){
            printf(func,2,"Dir:"+d.getFQDNDirName()+":  domain:"+d.getDirName()+":");
            WlsDomain wd = new WlsDomain(d.getDirName());
                      wd.setDomainLocation(d.getFQDNDirName());
                      ask4User(wd);
                      ar.add(wd);
                      
                      SecFile fd = new SecFile(d.getFQDNDirName()+File.separator+"domainkeys");
                      /*if ( ! fd.isReadableFile() ) {
                          
                      }*/
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
            char[] pass = console.readPassword("Admin User "+u+" Password : ", new String[]{});
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
            char[] pass = console.readPassword("Admin NodeUserUser "+pn+" Password : ", new String[]{});
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
          if ( ! dn.isDirectory() ) { dn.mkdir(); }
          WriteFile fn = new WriteFile(d.getFile("domain.info"));
          
          StringBuilder sp = fn.readOut();
          StringBuilder sw = new StringBuilder();
          System.out.println("capa:"+sp.capacity());
          if ( sp.capacity() > 0 ) {
            for ( String line : sp.toString().split("\n")) {
                System.out.println("line:"+line);

            }
          } else { this._needUpdate=true; }   
    
    }

    
    private boolean _needUpdate=false;
    public boolean isUpdateNeeded() { return _needUpdate; }

    public void updateDestination() {
        if ( isUpdateNeeded() ) {
           System.out.println("");
        }
    }
    
}
