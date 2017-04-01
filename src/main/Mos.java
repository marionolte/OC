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
import net.wls.WlsToolConfig;

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
        printf(func,0," trans args["+args.length+"]=from args["+j+"]");
        int a=0;
        for ( int i=j; i <args.length; i++ ) {
              printf(func,0," trans ar["+a+"/"+i+"]=args["+i+"]");
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
            else if ( args[i].matches("-wlsconfig")){ wlsConfigTools(args,i); fin=true; }
            else if ( args[i].matches("-crypt")   ) { crypt.runArgs(getArgsLower(args,++i)); }//  fin=runs("io.crypt.Crypt",getArgsLower(args,i++)); } 
            else if ( args[i].matches("-d")       ) { debug++; }
            else if ( args[i].matches("-version") ) { version(); }
            else {
                usage();
            }
            if ( fin ) { setClosed(); return; }
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
           //System.out.println("done.");    
    }
    
    private void version() {
        System.out.println(this.getFullInfo());
    }
    
    private static void usage() {
        System.out.println("Options:\n"
                + "\t\t-version \t\t-\tprint version information\n\n"
                + "\t\t-crypt <crypt|uncrypt> <String|File>\n\t\t\t\t\t-\tTest URL Connection to URL\n\n"
                + "\t\t-testssl <host> <port>\t-\tTest SSL Connection to the server and port \n"
                + "\t\t-testhttp <url> [url1,]\t-\tTest URL Connection to URL\n"
                + "\n\t\t-wlsconfig <dir> [-location <script dir>]\n\t\t\t\t\t-\tConfigure Wls Starting scripts in directory <dir>\n");
        System.exit(-1);
    }
}
