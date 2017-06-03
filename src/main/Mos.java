/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import net.ssh.SSHshell;
import general.Updater;
import io.Console;
import io.crypt.Crypt;
import io.file.ReadDir;
import io.file.ReadFile;
import io.file.SecFile;
import io.file.WriteFile;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import javax.naming.NamingException;
import static net.ldap.LdapMain.objList;
import net.ldap.LdapSearch;
import net.ssl.TestSSLServer;
import net.tcp.PortScanner;
import net.wls.WlsDomain;
import net.wls.WlsDomainLogRotation;
import net.wls.WlsToolConfig;
import net.wls.WlsUserEnv;
import org.eclipselabs.garbagecat.GCMain;

/**
 *
 * @author SuMario
 */
public class Mos extends Updater{

    public boolean stopProgress=false;
    public boolean silent=false;
    private final Crypt   crypt;
    private final Console console;
    private String[] args=null;
    private int _exit=-1;
    
    public Mos(String[] args) {
        super();
        this.args=args;
        this.crypt = new Crypt();
        this.console = new Console(this);
        this.console.setRunning();
        
    }
    
    private boolean testssl(String ho, String po) { return testssl(ho, Integer.parseInt(po)); }
    private boolean testssl(String ho, int po   ) { 
            TestSSLServer t = new TestSSLServer(ho,po);
                          t.test(); 
                          
            silent=true; 
            return  t.isValid();
    }
    
    private boolean ldap(String[] arg) {
        final String func=getFunc("ldap(String[] arg)");
        silent=true;
        String mod="usage";
        String bindDN=""; String modDN=""; String bindPW=""; String modPW="";
        String bindHost="localhost"; String modHost=""; int bindPort=-1; int modPort=-1; boolean bindSSL=false; boolean modSSL=false;
        String filter="objectclass=*"; String baseDN=""; String baseModDN=""; 
        try {
            for (int i=0; i<args.length; i++ ) {
                if      ( arg[i].matches("-D")     ) { bindDN=arg[++i];  if(modDN.isEmpty()){ modDN=bindDN;} }
                else if ( arg[i].matches("-Dmod")  ) { modDN=arg[++i]; }
                else if ( arg[i].matches("-j")     ) { bindPW=getPassword(arg[++i]); if(modPW.isEmpty()){ modPW=bindPW;} }
                else if ( arg[i].matches("-w")     ) { bindPW=arg[++i]; if(modPW.isEmpty()){ modPW=bindPW;}  }
                else if ( arg[i].matches("-wmod")  ) {  modPW=arg[++i]; }
                else if ( arg[i].matches("-jmod")  ) {  modPW=getPassword(arg[++i]); }
                else if ( arg[i].matches("-h")     ) { bindHost=arg[++i]; if(modHost.isEmpty()){ modHost=bindHost;} }
                else if ( arg[i].matches("-hmod")  ) {  modHost=arg[++i]; }
                else if ( arg[i].matches("-p")     ) { bindPort=Integer.parseInt(arg[++i]); if(modPort == -1){ modPort=bindPort;} }
                else if ( arg[i].matches("-pmod")  ) {  modPort=Integer.parseInt(arg[++i]); }
                else if ( arg[i].matches("-filter")) { filter=arg[++i];}
                else if ( arg[i].matches("-b")     ) { baseDN=arg[++i]; if (baseModDN.isEmpty()){ baseModDN=baseDN;} }
                else {
                   if ( ! arg[i].isEmpty() ) { mod=arg[i].toLowerCase().trim(); }  
                }
            }
            if(modHost.isEmpty()){ modHost=bindHost;}
            if(bindPort == -1){ bindPort=(bindSSL)?636:389; } 
            if( modPort == -1){ modPort=bindPort; } 
            
        } catch(RuntimeException rt) { mod="usage"; log("ERROR: "+rt.getMessage()+" - interrupt process"); }
        
        try {
            switch(mod) {
                case "search" :
                        // getInstance( String protocol, String hostname, int port, String userDN, String userPWD, String filter , String auth )
                        LdapSearch l = LdapSearch.getInstance( ( "ldap"+((bindSSL)?"s":"")) , bindHost, bindPort, bindDN, bindPW, filter, "");
                        
                                   return l.printResults( l.search(baseDN, filter, objList) );
                    //break;

                default: 
                    System.out.println("");
                    return false;
            }
        } catch(RuntimeException | NamingException | IOException ex) {
            printf(func,1,"LDAPCommand error:"+ex.getMessage());
            return false;
        }  
    }
    
    private String getPassword(String fn) {
        SecFile f = new SecFile(fn);
        String s = f.readOut().toString();
        return s;
    }
    
    private boolean setPassword(String fn) {
        SecFile f = new SecFile(fn);
        String s = new String ( System.console().readPassword("set new password in secure file "+fn, (Object[]) new String[]{}) );
        if ( s != null && ! s.isEmpty() ) { f.replace(s); }
        return ( s.matches(f.readOut().toString()));
    }
    
    private String[] getArgsLower(String[]args,int j) {
        final String func="getArgsLower(String[]args,int j)";
        String[] ar = new String[ args.length-j ];
        printf(func,3," trans args["+args.length+"]=from args["+j+"]");
        int a=0;
        for ( int i=j; i <args.length; i++ ) {
              printf(func,2," trans ar["+a+"/"+i+"]=args["+i+"]");
              ar[a]=args[i]; a++;
        }
        return ar;
    }
    boolean fin=false;  private boolean donemsg=true;
    private void parseArgs() throws Exception{
        final String func="parseArgs()";
        
        for( int i=0; i<args.length; i++ ) {
            if      ( args[i].matches("-testssl") ) { _exit = ( testssl(args[++i],args[++i])     )?0:1;   fin=true; }
            else if ( args[i].matches("-debugssl")) { System.setProperty("javax.net.debug","ssl"); }
            else if ( args[i].matches("-sshcomm") ) { _exit = (sshCommand(getArgsLower(args,++i)))?0:1;   fin=true; printf(func,3, "INFO: sshComm parseArgs closed"); }
            else if ( args[i].matches("-ldap")    ) { _exit = (ldap( getArgsLower(args,++i) )    )?0:1;   fin=true; }
            else if ( args[i].matches("-testhttp")) { String[] ar = getArgsLower(args,++i);
                                                      printf(func,1,"testhttp - start");
                                                      boolean b=true;
                                                      for (String s: ar) {
                                                            printf(func,2,"testhttp:"+s);
                                                            Http ht= new Http(new URL(s) ); 
                                                                 System.out.println( ht.getResponse().toString());
                                                                 if( ! b || ht.getResponseCode()<=0 || ht.getResponseCode() > 403 ) { b=false;}      
                                                      }
                                                      printf(func,1,"testhttp - fin");
                                                      fin=true;
                                                      _exit=(b)?0:1;
                                                    }
            else if ( args[i].matches("-logrotate")){ logRotate(getArgsLower(args,++i));        fin=true; }
            else if ( args[i].matches("-portscan") ){ portScanner(getArgsLower(args,++i));      fin=true; }
            else if ( args[i].matches("-wlsconfig")){ wlsConfigTools(getArgsLower(args,++i));   fin=true; }
            else if ( args[i].matches("-wlsinfo")  ){ wlsInfoTools(getArgsLower(args,++i));     fin=true; donemsg=false; }
            else if ( args[i].matches("-wlsrota")  ){ wlsRotate(getArgsLower(args,++i));        fin=true; donemsg=false; }
            else if ( args[i].matches("-logrota")  ){ logApacheRotate(getArgsLower(args,++i));  fin=true; donemsg=false; }
            else if ( args[i].matches("-crypt")    ||
                      args[i].matches("-uncrypt")  ){ crypt.runArgs(getArgsLower(args,i));      fin=true; }  
            else if ( args[i].matches("-rota")     ){ logRotate(getArgsLower(args,++i));        fin=true; }
            else if ( args[i].matches("-gclog")    ){ gcLog(getArgsLower(args,++i));            fin=true; }
            else if ( args[i].matches("-update")   ){ updateJar();                              fin=true; }
            else if ( args[i].matches("-unsecure") ){ unsecureFile(getArgsLower(args,++i));     fin=true; }
            else if ( args[i].matches("-secure")   ){ secureFile(getArgsLower(args,++i));       fin=true; }
            else if ( args[i].matches("-pwfile")   ){ this.setPassword(args[++i]);              fin=true; }
            else if ( args[i].matches("-gclog")    ){ checkGC(getArgsLower(args,++i));          fin=true; }
            else if ( args[i].matches("-d")        ){ debug++; }
            else if ( args[i].matches("-version")  ){ version(); _exit=0;                       fin=true; donemsg=false; }
            else {
                usage(); _exit=1; fin=true; 
            }
            printf(func,4,"parse closed");
            if ( fin ) { setClosed(); return; }
        } 
    }
    
    private void checkGC(String[] ar) {
        GCMain gc = new GCMain(ar);
               gc.scan();
    }
    
    
    private void secureFile(String[] ar) {
        if ( ar.length > 0 ) {
            for (int i=0; i<ar.length; i++ ) {
                ReadFile rf = new ReadFile(ar[i]);
                if ( rf.isReadableFile() ) {
                    SecFile wf = new SecFile(ar[i]);
                } else {
                    System.out.println("WARNING: "+ar[i]+" is not a readable file - skipping");
                }
            }
        }
    }
    private void unsecureFile(String[] ar) {
        if ( ar.length > 0 ) {
            for (int i=0; i<ar.length; i++ ) {
                SecFile sec = new SecFile(ar[i]);
                if ( sec.isReadableFile() ) {
                    WriteFile wf = new WriteFile(ar[i]);
                              wf.replace(sec.readOut().toString());
                } else {
                    System.out.println("WARNING: "+ar[i]+" is not a readable file - skipping");
                }
            }
        }
    }
    private void updateJar(){
        final String func=getFunc("updateJar()");
        String info="unknown";
        try {
            this._exit=1;
            
            Http.debug=debug; 
            
            Http ht = new Http(new URL(updateUrl+updateScript));  
            String[] sp = ht.getResponse().toString().trim().split("\\|");
            if ( debug >3)
                for ( int i=0; i<sp.length;i++) {
                    printf(func,4,"sp["+i+"]= |>"+sp[i]+"<|");
            }
            info=sp[0];
            
            printf(func,1, "Jar file "+jarfile+" in version "+getFullVersion()+" will replace with server version "+info);
            info=ht.connect(new URL(updateUrl+"OC-"+info+".jar"), jarfile+".1");
            if ( info.matches(sp[1])) {
                ReadFile f=new ReadFile(jarfile+".1"); 
                if ( f.isReadableFile() ) {
                //         f.move(new File(jarfile));
                    println("INFO: new application jar file "+jarfile+".1 with md5 checksum : "+info+" are ready");
                    println("INFO: replace existing jar file "+jarfile+"  with the new "+jarfile+".1");
                    this._exit=0;
                } else {
                    println("ERROR: jar file "+jarfile+".1 are not ready as file");
                }
                
            } else {
                println("ERROR: broken download to get new jar file from "+updateUrl+"OC-"+sp[0]+".jar");
                println("       Please update manually!");
            }    
        } catch(Exception e){
                println("ERROR: local version "+getFullVersion()+" could not updated to server version "+info); 
                this.donemsg=false;
        }  
    
    }
    
    private void logRotate(String[] args) {
        LogRotation lr = new LogRotation(args);
        if      ( lr.isCommand("VERSION") ) {  System.out.println("LogRotation v"+lr.getVersion()+" of "+lr.getFullInfo()); }
        else if ( lr.isCommand("ROTATE")  ) { lr.rotate();    }
        else  {                               lr.usage(true); }
    }
    
    private void wlsRotate(String[] args) {
        net.wls.WlsDomainLogRotation.parseArgs(args);
        final String func=getFunc("wlsRotate(String[] args)");
        for ( String s : net.wls.WlsDomainLogRotation.dirs.split(net.wls.WlsDomainLogRotation.sepa) ) {
            if ( ! s.isEmpty() ) {
                ReadDir di = new ReadDir(s);
                if ( di.isDirectory() ) {
                    printf(func,2,"rotate domain ->"+di.getDirName());
                    WlsDomain d = new WlsDomain(di.getDirName()); d.debug=debug;
                              d.setDomainLocation(di.getFQDNDirName());
                    WlsDomainLogRotation wlog = new WlsDomainLogRotation(d); wlog.debug=debug;
                                         wlog.rotate();
                                         
                }                         
            }
        }
        
    }
    
    private void logApacheRotate(String[] args) {
        net.apache.LogRotation.parseArgs(args);
        
        for ( String s : net.apache.LogRotation.dirs.split(net.apache.LogRotation.sepa) ) {
            if ( ! s.isEmpty() ) {
                ReadDir di = new ReadDir(s);
                if ( di.isDirectory() ) {
                net.apache.LogRotation wlog = new net.apache.LogRotation(di);
                                       wlog.rotate();
                }                       
            }
        }
        
    }
    
    private void gcLog(String[] args) {
         org.eclipselabs.garbagecat.GCMain m = new org.eclipselabs.garbagecat.GCMain(args);
                m.scan();
    }
    
    
    private void portScanner(String[] args) {
        String min=""; String max = ""; String host="localhost";
        for( int i=0; i<args.length; i++ ) {
              if      ( args[i].matches("-pmin") ) {  min=args[++i]; }
              else if ( args[i].matches("-pmax") ) {  max=args[++i]; }
              else if ( args[i].matches("-host") ) {  host=args[++i]; }
        }
        PortScanner pc=new PortScanner(host);
                    if ( ! max.isEmpty()) pc.setMaxPort(max); 
                    if ( ! min.isEmpty()) pc.setMinPort(min);
                    
        System.out.println("Scan host:"+host+" from min port: "+pc.getMinPort()+" to max port: "+pc.getMaxPort()+" for listening");
        pc.test();
    }
    
    private boolean sshCommand(String[] args) {
         final String func=getFunc("sshCommand(String[] args)");
         printf(func,2,"sshCommand start");
         
         SSHshell.debug=debug;
         SSHshell ssh = SSHshell.getInstance(args);
         if ( ssh.isSSHShell() ) {
            try { 
                printf(func,3,"send command :"+ssh.getCommand().toString());
                System.out.println(ssh.sendSingleCommand().toString()); 
            } catch(Exception e) { 
                printf(func,1,"send command error :"+e.getMessage());
                return false; 
            }
            printf(func,2,"send command return :"+ssh.isValid());
         } else {
           try {
               
               ArrayList<String> fr = new ArrayList();
               ArrayList<String> fl = new ArrayList();  
               int way=-1;
               for ( int i=0; i< args.length; i++) {
                   if ( args[i].matches("scp") ) {}
                   else if ( args[i].startsWith(":") ) { fr.add( args[i].substring(1) );  if(way==-1){ way=1; } }
                   else {                                fl.add( args[i].substring(1) );  if(way==-1){ way=2; }  }
               }
               String[] rfiles = new String[ fr.size() ]; for( int j=0; j<fr.size(); j++ ) { rfiles[j]=fr.get(j); }
               String[] lfiles = new String[ fr.size() ]; for( int j=0; j<fl.size(); j++ ) { lfiles[j]=fl.get(j); }
               
               if ( rfiles.length == 0 || lfiles.length == 0) { throw new IOException("missing properties"); }
               if ( way == 1 ) {
                    ReadDir d = new ReadDir(lfiles[0]);
                    if ( ! d.isDirectory() ) { throw new IOException(lfiles[0]+" is not a local directory"); }
                    ssh.scpFrom(rfiles, lfiles[0]);
               } else {
                    ssh.scpTo(lfiles, rfiles[0]);
               }     
           } catch (IOException io ) {
               printf(func,1,"scp command error :"+io.getMessage());
               return false; 
           }  
             
         }   
         return ssh.isValid();
    }
    
    private void wlsInfoTools(String[] args ) {
         WlsUserEnv wue = null;
    
         String domdir=""; String k="";
         if( args.length>0) {
            wue = new WlsUserEnv(); 
            for( int i=0; i<args.length; i++ ) {
                if ( args[i].matches("-server") ) {
                        if ( args.length > i+1 ) { wue.setServer(args[++i]); }
                        else { wue.setServer("*"); }
                } else {
                    ReadDir nf = new ReadDir(args[i]);
                    if ( nf.isDirectory() && nf.isReadable() ) { domdir=nf.getFQDNDirName(); } 
                    else { 
                                k=args[i]; 
                    }        
                }
            }
         }
         //System.out.println("wue:"+wue);
         if ( wue != null ) {
             //System.out.println("dom key:"+domdir+File.separator+"domainkeys"+":   k:"+k+":");
             System.out.println(wue.updateEnv(domdir+File.separator+"domainkeys",k));
         } else {
             
         }
    }
    private void wlsConfigTools(String[] args) {
          final String func=getFunc("wlsConfigTools(String[] args)");
          //if ( args.length <= j+1 ) { return; }
          String dest=System.getProperty("user.home")+File.separator+"bin";
          WlsToolConfig w = new WlsToolConfig(); w.debug=debug;
          for( int i=0; i <args.length; i++ ) {
              ReadDir d = new ReadDir(args[i]);
              if ( d.isDirectory() ) {
                  printf(func,2,"call updateConfig for "+args[i]);
                  w.updateConfig(args[i]);
              } else {
                  if ( args[i].matches("-dest") ) { 
                      dest=args[++i]; 
                      printf(func,2,"update destination to :"+dest);
                  }
              }  
          }
          printf(func,2,"check configuration on dest:"+dest);
          w.checkConfig(dest);
          if ( w.isUpdateNeeded() ) { 
              printf(func,2,"call destionation update - needed");
              w.updateDestination(dest); 
              
          } else {
              printf(func,3,"call destionation updated not needed");
          }
    }
    
    @Override
    public void run() {
        setRunning();
        final String func=getFunc("run()");
        printf(func,4, "INFO: start");
        boolean fail=false;
        if (args != null && args.length > 0 ) {
           try{ 
               printf(func,3, "INFO: parseArgs start ");
               parseArgs(); 
               fail=true;
               printf(func,3, "INFO: parseArgs return");
           }catch(Exception e) {
               printf(func,1, "ERROR:"+e.getMessage());
           }
        } else {
            usage();
        }
        if ( ! fail ) {
            // do someting else
        }
        setClosed();
        printf(func,4, "INFO: closed");
            
    }
    
    
    private boolean compareJarFileMD5(String md5) {
        ReadFile fa = new ReadFile(jarfile);
        return ( fa.getMD5().matches(md5));
    }
    
    public static void main(String[] args) {
           Mos m = new Mos(args); m.silent=true;
               m.start();
               while( m.isRunning() && ! m.fin ) { sleep(300); }
               if ( m.donemsg ) System.out.println("done."); 
               System.exit(m._exit);
    }
    
    private void version() {
        System.out.println(this.getFullInfo());
    }
    
    private void usage() {
        System.out.println("Options:\n"
                + "\t\t-version \t\t-\tprint version information\n\n"
                + "\t\t-crypt "+crypt.usage(false)+"\n\t\t\t\t\t-\tcrypt or uncrypt a string or file\n\n"
                + "\t\t-testssl <host> <port>\t-\tTest SSL Connection to the server and port \n"
                + "\n\t\t-sshcomm "+SSHshell.usage()+"\n\t\t\t\t\t-\tsend a single ssh command\n"
                + "\t\t-portscan [-host <host>] [-pmin <min port>] [-pmax <max port>]\t-\tport  scanner \n"
                + "\n\t\t-testhttp <url> [url1,]\t-\tTest URL Connection to URL\n"
                + "\n\t\t-ldap -D <bindDN> -j <Password File> <-h <Host>> <-p <Port>> -filter <filter> -b <baseDN>\n"
                + "\n\t\t-wlsconfig [-dest <script dir [.]>] <domaindir <domaindir1...>>\n\t\t\t\t\t-\tConfigure Wls Starting scripts in directory <dest>\n"
                + "\n\t\t-wlsinfo <domainhome> [<-server <servername>]\t-\n\t\t\t\t\tprint domain use information\n"
                + "\n\t\t-wlsrota "+WlsDomainLogRotation.usage()+"\n\t\t\t\t\tweblogic domain logrotation\n"
                + "\n\t\t-logrota "+net.apache.LogRotation.usage()+"\n\t\t\t\t\tapache|ohs logrotation\n"
                + "\n\t\t-pwfile <filename>\t-\tstore a password in a secure file\n"
                
                //+ "\n\t\t-logrotate\t"+(new LogRotation(new String[]{}).usage(false) )
                + "\n\n"
        );
        System.exit(-1);
    }
}