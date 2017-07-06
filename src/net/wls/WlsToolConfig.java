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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 *
 * @author SuMario
 */
public class WlsToolConfig extends Version{

    public static String usage() {
        return "[-dest <script dir [.]>] [-reconfig] [-silient] <domaindir <domaindir1...>>";
    }
    
    boolean silent=false;

    public static void main(String[] args) {
        final String func="WlsToolConfig::main(String[] args)";
        WlsToolConfig w = new WlsToolConfig();
                      w.getLocation();
        if ( args.length > 0 ) {
            ArrayList<String> dirs = new ArrayList();
            for(int i=0; i< args.length; i++) {
                if ( args[i].matches("-dest")         ) { w.checkConfig(args[++i]); } 
                else if ( args[i].matches("-d")       ) { debug++; }
                else if ( args[i].matches("-silent")  ) { w.silent=true; }
                else if ( args[i].matches("-reconfig")) { w.setUpdateNeeded();}
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
            try {
                        WlsDecrypt wdc = new WlsDecrypt(wd);
                                   wdc.decrypt();
                        boolean b=false;           
                        if ( wdc.getUser().isEmpty() || wdc.getPass().isEmpty() || wdc.getNMUser().isEmpty() || wdc.getNMPass().isEmpty() ) { 
                            if ( silent ) { throw new RuntimeException("ERROR: empty users settings not allowed in silent "); }
                                   ask4User(wd); b=true;
                            wdc.setUser(  wd.getAdminUser()     ); 
                            wdc.setNMUser(wd.getNodeUser()      );
                            wdc.setPass(  wd.getAdminPassword() );
                            wdc.setNMPass(wd.getNodePassword()  );
                        }
                        if ( ! b && wd._OSUser.isEmpty() ) {
                            ask4User(wd);
                        }
                        if ( wdc.getUser().isEmpty() || wdc.getPass().isEmpty() || wdc.getNMUser().isEmpty() || wdc.getNMPass().isEmpty() ) {                     
                                   wd.updateAccounts( wdc.getUser(), wdc.getPass(), wdc.getNMUser(), wdc.getNMPass() );
                        }
                        
            }catch(java.io.IOException io) {
                                   printf(func,1,"ERROR: check decrypt information with error:"+io.getMessage());
            }           
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
        final String func=getFunc("ask4User(WlsDomain wd)");
        System.out.println("INFO: check domain "+wd.getDomainName()+" from location "+wd.getDomainLocation());
        String u  = wd.getAdminUser(); 
        String p  = wd.getAdminPassword();
        String un = wd.getNodeUser(); 
        String pn = wd.getNodePassword();
        String osu   = wd.getOSUser();
        String osp   = wd.getOSPassword();
        String oskey = wd.getOSUserkey();
        if ( u.isEmpty() || p.isEmpty() ) {
                  System.out.print("Domain Admin User ["+((u!=null && ! u.isEmpty())?u:"")+"] : "); 
                  String readLine = console.readLine().trim();
            //System.out.println("\nreadline:"+readLine+":");      
                  if ( readLine.isEmpty() ) { u=(u.isEmpty())?"weblogic":u; } else { u=readLine; }
            //System.out.println("u:"+u+":");      
                  wd.setAdminUser(u); 
               u= wd.getAdminUser();
            //System.out.println("u1:"+u+"");   
                  System.out.println("");
        }          
        
        if ( p.isEmpty() ) {
            char[] pass = console.readPassword("Admin User "+u+" Password : ", (Object[]) new String[]{});
            p=new String(pass);
            if ( p != null && ! p.isEmpty() ) { wd.setAdminPassword(p); }
            System.out.println("");
        }
        if ( un.isEmpty() || pn.isEmpty() ) {
             System.out.print("Domain NodeManager User ["+un+"] : "); 
             String readLine = console.readLine().trim();
             if ( readLine.isEmpty() ) { u="weblogic"; } else { u=readLine; }
             if ( un != null && ! un.isEmpty() ) { wd.setNodeUser(un); } else { wd.setNodeUser("weblogic"); } 
               un =  wd.getNodeUser();
             System.out.println(""); 
        }     
        
        if ( pn.isEmpty() ) {
            char[] pass = console.readPassword("Domain NodeUser User ["+un+"] Password : ", (Object[]) new String[]{});
            pn=(pass.length>0)?new String(pass):pn;
            wd.setNodePassword(pn); 
            System.out.println("");
        } 
        
        if ( osu.isEmpty() ) {
             System.out.print("Domain Remote User ["+osu+"] : "); 
             String readLine = console.readLine().trim();
             if ( ! readLine.isEmpty() ) { osu=readLine.trim(); wd._OSUser=osu; }
             System.out.println("");  
        }
        
        if ( ! osu.isEmpty() )  {
            if ( oskey.isEmpty() ) {
                System.out.print("Domain Remote User ["+osu+"] ssh keyfile : "); 
                String readLine = console.readLine().trim();
                if ( ! readLine.isEmpty() ) { oskey=(new ReadFile(readLine.trim())).getFQDNFileName(); wd._OSUserkey=oskey; }
                System.out.println("");  
            }
            if ( osp.isEmpty() ) {
                char[] pass = console.readPassword("Domain Remote User ["+osu+"] Password : ", (Object[]) new String[]{});
                osp=(pass.length>0)?new String(pass):"";
                wd._OSPass=osp; 
                System.out.println("");
            }
            
            
        }
        
    }

    public void checkConfig(String dest) {
          ReadDir d = new ReadDir(dest);  
          if ( ! d.isDirectory() ) {
               d.mkdirs();
          }
          setLocation(dest);
          ReadDir dn= new ReadDir(dest+File.separator+"lib");
          if ( ! dn.isDirectory() ) { this.setUpdateNeeded();  }

          if ( ! this.isUpdateNeeded() ) {
            ReadFile df = new ReadFile(dest+File.separator+"lib"+File.separator+"OC.jar");
            if ( ! df.isReadableFile() || ! df.getFQDNFileName().replaceAll(File.separator, sepa).matches(jarfile.replaceAll(File.separator, sepa))) {
                  this.setUpdateNeeded();
            }
          }
          
          if ( ! this.isUpdateNeeded() ) {
            ReadFile fn = new ReadFile(d.getFile("domain.info").getFQDNFileName());
            if ( fn.isReadableFile() ) {
                String[] sp = fn.readOut().toString().split("\n");
                StringBuilder sw = new StringBuilder();
                for ( String line : sp) {
                      if ( line.contains("DOMAINHOME=")) {
                          String[] tp = line.split("\"");
                          
                      }
                }
            } else { this.setUpdateNeeded(); }  // domain.info not exist
          } 
          System.out.println("update needed:"+this.isUpdateNeeded());
    }

    private final String sepa="__@@__";
    
    private boolean _needUpdate=false;
    public boolean  isUpdateNeeded() { return _needUpdate; }
    public boolean setUpdateNeeded() { _needUpdate=true; return this.isUpdateNeeded(); }

    public void updateDestination(String dest) {
        final String func=getFunc("updateDestination(String dest)");
        if ( isUpdateNeeded() ) {
           System.out.println("INFO: update create destination "+dest+" if needed"); 
           ReadDir dn= new ReadDir(dest+File.separator+"log");
           if ( ! dn.isDirectory() ) { dn.mkdirs(); }
           
                  dn= new ReadDir(dest+File.separator+"lib");
           if ( ! dn.isDirectory() ) { dn.mkdirs(); }
           
           
           
           System.out.print("INFO: update OC.jar .. ");
           ReadFile df = new ReadFile(dest+File.separator+"lib"+File.separator+"OC.jar");
           if ( ! df.isReadableFile() || ! df.getFQDNFileName().replaceAll(File.separator, sepa).matches(jarfile.replaceAll(File.separator, sepa))) {
                WriteFile dfr=new WriteFile(jarfile);
                          dfr.copy(new File(dn.getFQDNDirName()+File.separator+"OC.jar"));
           }
           System.out.println("done");
           System.out.print("INFO: update domain.info & domainkeys .. ");
           WriteFile wt = new WriteFile(dest+File.separator+"domain.info");
           StringBuilder wta = new StringBuilder();
           StringBuilder sw = wt.readOut();
           String temp = getOutString( new BufferedInputStream( WlsToolConfig.class.getResourceAsStream("/setup/config/domain.info") ) );
           printf(func,4,"read from resource domain.info:"+temp);
           if ( temp != null && ! temp.isEmpty() ) {
                printf(func,3,"domain.info:"+temp);
                for( String sp : temp.split("\n") ) {
                    wta.append(sp.trim()).append("\n");
                    //System.out.println("sp:"+sp.trim());
                    if ( sp.trim().matches("### begin domain") ) {
                         //System.out.println("check exsting\n"+sw.toString());
                         boolean begin=false;
                         for( String s : sw.toString().split("\n") ) {
                             if      ( s.trim().startsWith("### begin domain") ) { begin=true; }
                             else if ( s.trim().startsWith("### end   domain") ) { begin=false; }
                             if ( begin && ! s.startsWith("#") && s.contains("DOMAINHOME") ) {
                                    String[] tp = s.trim().split("\"");  
                                    String[] mp = tp[tp.length-1].replaceAll(File.separator+"$", "").split(File.separator);
                                    
                                    if ( ar.get(mp[ mp.length-1]) == null ) {
                                        WlsDomain d = new WlsDomain(mp[ mp.length-1]);
                                                  d.setDomainLocation(tp[tp.length-1]);
                                                  ar.put(d.getDomainName(), d);
                                    }                             
                             } 
                         }
                         
                         Iterator<String> itter = ar.keySet().iterator();
                         while( itter.hasNext() ) {
                                WlsDomain d = ar.get(itter.next());
                                //System.out.println("domain.info:"+d.getDomainName());
                                wta.append("if [[ \"$DOM\" == \"").append(d.getDomainName()).append("\" ]]; then \n");
                                wta.append("\texport DOMAINHOME=\"").append(d.getDomainLocation()).append("\"\n");
                                wta.append("\texport CTLUSER=\"").append(System.getProperty("user.name")).append("\"\n");
                                wta.append("\texport CTLPASSFILE=\"").append(dest+File.separator+"ctluserkey.pass").append("\"\n");
                                wta.append("fi\n");
                                
                                StringBuilder ft = new StringBuilder();
                                ft.append("username=").append(d.getAdminUser()    ).append("\n")
                                  .append("password=").append(d.getAdminPassword()).append("\n")
                                  .append("nmuser="  ).append(d.getNodeUser()     ).append("\n")
                                  .append("nmpass="  ).append(d.getNodePassword() ).append("\n"); 
                                if ( ! d._OSUser.isEmpty() ) {
                                    ft.append("osuser="   ).append(d._OSUser   ).append("\n")
                                      .append("ospass="   ).append(d._OSPass   ).append("\n")
                                      .append("osuserkey=").append(d._OSUserkey).append("\n");
                                }
                                SecFile fd = new SecFile(d.getDomainLocation()+File.separator+"domainkeys");
                                        fd.replace(ft.toString());
                      
                         }   
                         //System.out.println("existing check completed");
                        
                    }
                } 
                
           }
           if (wta.capacity() > 0 ) { 
               wt.append(wta, false); 
           }
           System.out.println("done");
           
           System.out.print("INFO: update base runscripts .. ");
           saveResourceFiles(jarfile, "/setup/bin", dest);
           System.out.println("done");
           
           System.out.print("INFO: update domain runscripts .. ");  
           String log   = getOutString( new BufferedInputStream( WlsToolConfig.class.getResourceAsStream("/setup/config/dom.logrota") ) );
           String state = getOutString( new BufferedInputStream( WlsToolConfig.class.getResourceAsStream("/setup/config/dom.status") ) );
           String serv  = getOutString( new BufferedInputStream( WlsToolConfig.class.getResourceAsStream("/setup/config/dom.server") ) );
           String node  = getOutString( new BufferedInputStream( WlsToolConfig.class.getResourceAsStream("/setup/config/dom.node") ) );
           
           Iterator<String> itter = ar.keySet().iterator();
           while( itter.hasNext() ) {
                 WlsDomain d = ar.get(itter.next());
                 final String dom=d.getDomainName();
                 
                 printf(func,2,"update domain:"+dom);
                 WriteFile wf = new WriteFile(dest+File.separator+d.getDomainName()+"status"); 
                           printf(func,3,"update "+wf.getFQDNFileName());
                           wf.append( state.replaceAll("@@DOMAIN@@", dom).getBytes(), false);
                           wf.setExecutable(true);
                           
                           wf = new WriteFile(dest+File.separator+d.getDomainName()+"WlsRota"); 
                           printf(func,3,"update "+wf.getFQDNFileName());
                           wf.append( log.replaceAll("@@DOMAIN@@", dom).getBytes(), false );
                           wf.setExecutable(true);   
                           
                           wf = new WriteFile(dest+File.separator+d.getDomainName()+"RollingRestart"); 
                           printf(func,3,"update "+wf.getFQDNFileName());                
                           wf.append( state.replaceAll(" status ", " rollingrestart ").replaceAll("@@DOMAIN@@", dom+" \\$\\@").getBytes(), false );
                           wf.setExecutable(true);   
                           
                    /*String na = d.getAdminServer().getAdminServerName();
                           wf = new WriteFile(dest+File.separator+d.getDomainName()+"Admin");
                           printf(func,3,"update "+wf.getFQDNFileName());
                           wf.append(serv.replaceAll("@@DOMAIN@@", dom).replaceAll("@@SERVER@@", na).getBytes(), false);  
                           wf.setExecutable(true);*/
                 HashMap<String, WlsServer> m = d.getServers();
                 Iterator<String> its = m.keySet().iterator();
                 while( its.hasNext() ) {
                        String n=its.next();
                        if ( ! n.isEmpty() ) {
                           wf = new WriteFile(dest+File.separator+d.getDomainName()+n); wf.append( serv.replaceAll("@@DOMAIN@@", dom)
                                                                                                       .replaceAll("@@SERVER@@", n).getBytes(), false);  
                           printf(func,3,"update "+wf.getFQDNFileName());
                           wf.setExecutable(true);
                        }
                 }
            
                 ArrayList<WlsNodeManager> ma = d.getNodeManagers();
                 while( ma.size() > 0 ) {
                        WlsNodeManager nm = ma.remove(0);
                        String n=nm.getMachineName();
                        if ( ! n.isEmpty() ) {
                           wf = new WriteFile(dest+File.separator+d.getDomainName()+n+"Node"); 
                           printf(func,3,"update "+wf.getFQDNFileName());
                           wf.append(  node.replaceAll("@@DOMAIN@@", dom).replaceAll("@@SERVER@@", n).replaceAll("@@MACHINE@@", n).getBytes(), false);  
                           wf.setExecutable(true);
                        }
                 }
           } 
           System.out.println("done");
        } else {
           System.out.println("INFO: no update needed");
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
    
    private void saveResourceFiles(String file, String path, String dest ) {
       final String func=getFunc("saveResourceFiles(String file, String path, String dest )"); 
       try { 
            String u = System.getProperty("user.name"); 
            ReadDir d = new ReadDir(dest); String dim=path.replaceAll("^/", "");
            ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
            ZipEntry ze;  byte[] buf = new byte[4*1024];
                while( (ze=zis.getNextEntry()) != null){
                    String fn = ze.getName();
                    printf(func,4," get entry :"+fn+":");
                    if ( fn.startsWith(dim)  && fn.length() > dim.length()+1 ) {
                         printf(func,1,"found entry from "+path+" with :"+fn+": length:"+fn.length() ); 
                         String[] sp = fn.split("/");
                         File nFile = new File(d.getFQDNDirName() + File.separator + sp[sp.length-1]);
                         FileOutputStream fos = new FileOutputStream(nFile);
                         int len;
                         while ((len = zis.read(buf)) > 0) { 
                             StringBuilder mp = new StringBuilder();
                             for ( int i=0; i<len; i++ ) {
                                    mp.append( (char) buf[i] );
                             }
                             //fos.write(buf, 0, len); 
                             fos.write( mp.toString().replaceAll(" \"oracle\" ", " \""+u+"\" ").getBytes() );
                         }
                         fos.close();
                         //close this ZipEntry
                         zis.closeEntry();
                         
                         nFile.setExecutable(true); 
                    }

                }    
       } catch(Exception e) {
             printf(func,1,"get zip Error :"+e.getMessage()+":");
       } 
     
    }  
}
