/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import io.Console;
import io.crypt.Crypt;
import io.file.ReadDir;
import io.file.WriteFile;
import io.thread.RunnableT;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.naming.NamingException;
import static net.ldap.LdapMain.objList;
import net.ldap.LdapSearch;
import net.ssl.TestSSLServer;
import net.tcp.PortScanner;
import net.wls.WlsDomain;
import net.wls.WlsDomainLogRotation;
import net.wls.WlsToolConfig;
import net.wls.WlsUserEnv;

/**
 *
 * @author SuMario
 */
public class Mos extends RunnableT{

    public boolean stopProgress=false;
    public boolean silent=false;
    private final Crypt   crypt;
    private final Console console;
    private String[] args=null;
    
    public Mos(String[] args) {
        super();
        this.args=args;
        this.crypt = new Crypt();
        this.console = new Console(this);
        this.console.setRunning();
        
    }
    
    private void testssl(String ho, String po) { testssl(ho, Integer.parseInt(po)); }
    private void testssl(String ho, int po   ) { (new TestSSLServer(ho,po)).test(); silent=true; }
    
    private void ldap(String[] arg) {
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
                        
                                   l.printResults( l.search(baseDN, filter, objList) );

                    break;

                default: 
                    System.out.println("");
                    return;
            }
        } catch(RuntimeException | NamingException | IOException ex) {
            
        }  
    }
    
    private String getPassword(String fn) {
        WriteFile f = new WriteFile(fn);
        String s = f.readOut().toString();
        if ( s == null || s.isEmpty() ) { return (String) null;}
        if( s.endsWith("=") ) {
            s=crypt.getUnCrypted(s);
        } else {
            String m=crypt.getCrypted(s);
            f.append(m);
        }
        return s;
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
    private void parseArgs() throws Exception{
        final String func="parseArgs()";
        boolean fin=false;
        for( int i=0; i<args.length; i++ ) {
            if      ( args[i].matches("-testssl") ) { testssl(args[++i],args[++i]); fin=true; }
            else if ( args[i].matches("-debugssl")) { System.setProperty("javax.net.debug","ssl"); }
            else if ( args[i].matches("-sshcomm") ) { sshCommand(getArgsLower(args,++i));      fin=true; }
            else if ( args[i].matches("-ldap")    ) { ldap( getArgsLower(args,++i) ); i=args.length;  fin=true; }
            else if ( args[i].matches("-testhttp")) { String[] ar = getArgsLower(args,++i);
                                                      printf(func,1,"testhttp - start");
                                                      for (String s: ar) {
                                                            printf(func,0,"testhttp:"+s);
                                                            Http ht= new Http(new URL(s) ); 
                                                                 System.out.println( ht.getResponse().toString());
                                                      }
                                                      printf(func,1,"testhttp - fin");
                                                      fin=true;
                                                    }
            else if ( args[i].matches("-logrotate")){ logRotate(getArgsLower(args,++i));        fin=true; }
            else if ( args[i].matches("-portscan") ){ portScanner(getArgsLower(args,++i));      fin=true; }
            else if ( args[i].matches("-wlsconfig")){ wlsConfigTools(getArgsLower(args,++i),0); fin=true; }
            else if ( args[i].matches("-wlsinfo")  ){ wlsInfoTools(getArgsLower(args,++i));     fin=true; }
            else if ( args[i].matches("-wlsrota")  ){ wlsRotate(getArgsLower(args,++i)); fin=true;}
            else if ( args[i].matches("-crypt")    ){ crypt.runArgs(getArgsLower(args,++i));    fin=true; }//  fin=runs("io.crypt.Crypt",getArgsLower(args,i++)); } 
            else if ( args[i].matches("-d")        ){ debug++; }
            else if ( args[i].matches("-version")  ){ version(); }
            else {
                usage();
            }
            if ( fin ) { setClosed(); return; }
        } 
    }
    
    private void logRotate(String[] args) {
        LogRotation lr = new LogRotation(args);
        if      ( lr.isCommand("VERSION") ) {  System.out.println("LogRotation v"+lr.getVersion()+" of "+lr.getFullInfo()); }
        else if ( lr.isCommand("ROTATE")  ) { lr.rotate();    }
        else  {                               lr.usage(true); }
    }
    
    private void wlsRotate(String[] args) {
        StringBuilder sw = new StringBuilder();
        final String sepa="__@@__";
        for ( int i=0; i< args.length; i++ ) {
            if        ( args[i].matches("-minsize") ){ WlsDomainLogRotation.minsize  = Long.parseLong(args[++i]); 
            } else if ( args[i].matches("-minold")  ){ WlsDomainLogRotation.minold   = Integer.parseInt(args[++i]);
            } else if ( args[i].matches("-maxold")  ){ WlsDomainLogRotation.maxold   = Integer.parseInt(args[++i]);
            } else if ( args[i].matches("-savefile")){ WlsDomainLogRotation.savefile = args[++i];
            } else {
                sw.append(sepa).append(args[i]);
            }        
        }
        
        for ( String s : sw.toString().split(sepa) ) {
            if ( ! s.isEmpty() ) {
                ReadDir di = new ReadDir(s);
                WlsDomain d = new WlsDomain(di.getDirName());
                          d.setDomainLocation(di.getFQDNDirName());
                WlsDomainLogRotation wlog = new WlsDomainLogRotation(d);
                                     wlog.rotate();
            }
        }
        
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
    
    private void sshCommand(String[] args) {
         SSHshell ssh = SSHshell.getInstance(args);
         System.out.println(ssh.sendSingleCommand());
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
         }
    }
    private void wlsConfigTools(String[] args, int j) {
          if ( args.length <= j+1 ) { return; }
          String dest=System.getProperty("user.home")+File.separator+"bin";
          WlsToolConfig w = new WlsToolConfig();
          for( int i=j+1; i <args.length; i++ ) {
              ReadDir d = new ReadDir(args[i]);
              if ( d.isDirectory() && d.isReadable() ) {
                w.updateConfig(args[i]);
              } else {
                  if ( args[i].matches("-dest") ) { dest=args[++i]; }
              }  
          }
          w.checkConfig(dest);
          if ( w.isUpdateNeeded() ) { w.updateDestination(); }
    }
    
    @Override
    public void run() {
        setRunning();
        boolean fail=false;
        if (args != null && args.length > 0 ) {
           try{ parseArgs(); 
                fail=true;
           }catch(Exception e) {}
        } else {
            usage();
        }
        if ( ! fail ) {
            // do someting else
        }
        setClosed();
            
    }
    
    
    public static void main(String[] args) {
           Mos m = new Mos(args); m.silent=true;
               m.start();
               //while( m.isRunning() ) { sleep(300); }
               //System.out.println("done.");    
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
                + "\n\t\t-logrotate\t"+(new LogRotation(new String[]{}).usage(false) )
                + "\n\n"
        );
        System.exit(-1);
    }
}
