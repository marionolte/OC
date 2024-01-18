/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;


import com.macmario.comm.checker.Checker;
import com.macmario.io.logs.LogRotation;
import com.macmario.io.net.PullHttp;
import com.macmario.io.net.Http;
import com.macmario.net.ssh.SSHshell;
import com.macmario.general.Updater;
import com.macmario.comm.mail.Imap;
import com.macmario.io.Console;
import com.macmario.io.account.PasswordTyp;
import com.macmario.io.crypt.Crypt;
import com.macmario.io.crypt.GetPassword;
import com.macmario.io.db.SecDb;
import com.macmario.io.file.ReadDir;
import com.macmario.io.file.ReadFile;
import com.macmario.io.file.SecFile;
import com.macmario.io.file.WriteFile;
import com.macmario.io.git.Git;
import com.macmario.io.java.GCFile;
import com.macmario.io.lib.IOLib;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import javax.naming.NamingException;
import com.macmario.net.ldap.LdapUserBlk;
import com.macmario.net.ssh.SSHpass;
import com.macmario.net.ssl.TestSSLServer;
import com.macmario.net.tcp.PortScanner;
import com.macmario.net.wls.WlsDomain;
import com.macmario.net.wls.WlsDomainLogRotation;
import com.macmario.net.wls.WlsToolConfig;
import com.macmario.net.wls.WlsUserEnv;
import org.eclipselabs.garbagecat.GCMain;
import com.macmario.net.ldap.LdapSearch;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;




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

    private final IOLib lib = new IOLib();
    
    public Mos(String[] args) throws IOException {
        super();
        this.args=args;
        this.crypt = new Crypt();
        this.console = new Console(this);
        this.console.setRunning();
        lib.fillJarMap(jarfile);
    }
    
    private boolean testssl(String ho, String po) { return testssl(ho, Integer.parseInt(po)); }
    private boolean testssl(String ho, int po   ) { 
            TestSSLServer t = new TestSSLServer(ho,po);
                          t.test(); 
                          
            silent=true; 
            return  t.isValid();
    }
    

    private boolean ldapBulk(String[] arg) {
        LdapUserBlk ob = new LdapUserBlk();
                    ob.scanArgs(arg);
            ob.search();
            ob.finishing();
          
            System.out.println(ob.getCount()+" ldap entries found \tmodified:"+ob.getModified());
            
         return (ob.getCount()>0 && ob.getModified()>0);   
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
                        // getInstance( String protocol, String hostname, int port, String userDN, String userPWD, String filter , String auth , String baseDn)
                        LdapSearch l = LdapSearch.getInstance( ( "ldap"+((bindSSL)?"s":"")) , bindHost, bindPort, bindDN, bindPW, filter, "", baseDN);
                        
                                   return l.printResults( l.search(baseDN, filter, l.getAttrList()) );
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
        return f.readOut().toString();
    }
    
    private boolean setPassword(String fn) {
        SecFile f = new SecFile(fn);
        String s = new String ( System.console().readPassword("set new password in secure file "+fn, (Object[]) new String[]{}) );
        if ( s != null && ! s.isEmpty() ) { f.replace(s); }
        return ( s.matches(f.readOut().toString()));
    }
    private String getNewPassword(int len,String typ){
        com.macmario.io.crypt.PasswordTyp tp=com.macmario.io.crypt.PasswordTyp.fromString(typ);
        len= ( tp.equals(com.macmario.io.crypt.PasswordTyp.MEDIUM) && len < 12 )?12:len;
        len= ( tp.equals(com.macmario.io.crypt.PasswordTyp.STRONG) && len < 16 )?16:len;
        return GetPassword.getPassword(len, tp);
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

    
    boolean fin=false;  private boolean donemsg=true; private boolean parseCompleted=false;
    private void parseArgs() {
        final String func=getFunc("parseArgs()");
        parseCompleted=false;
        try {
            int argu=0;
            for( int i=0; i<args.length; i++ ) {
                if ( args[i].matches("-d")        ){ debug++; argu++; }
            }
            if ( argu == args.length ) { this.usage(); }  // goes direct out
            for( int i=0; i<args.length; i++ ) {
                printf(func,3,"test args["+i+"/"+args.length+"]:"+args[i]+":");
                if      ( args[i].matches("-testssl") ) { _exit = ( testssl(args[++i],args[++i])     )?0:1;   fin=true; }
                else if ( args[i].matches("-debugssl")) { System.setProperty("javax.net.debug","ssl"); }
                else if ( args[i].matches("-sshcomm") ) { _exit = (sshCommand(getArgsLower(args,++i)))?0:1;   fin=true; printf(func,3, "INFO: sshComm parseArgs closed"); }
                else if ( args[i].matches("-sshpass") ) { _exit = (sshScript(getArgsLower(args,++i)) )?0:1;   fin=true; printf(func,3, "INFO: sshScript  parseArgs closed"); }
                else if ( args[i].matches("-sshcluster")){_exit = (sshCluster(getArgsLower(args,++i)) )?0:1;  fin=true; printf(func,3, "INFO: sshCluster parseArgs closed"); }
                else if ( args[i].matches("-ldapbulk")) { _exit = (ldapBulk( getArgsLower(args,++i) ))?0:1;   fin=true; }
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
                else if ( args[i].matches("-genpassword")){    String pw=""; i++; if( args.length>i ){ pw=args[i]; }                    
                                                               System.out.println( getNewPassword(12, pw )  );                           
                                                                                                               fin=true; _exit=0; }
                else if ( args[i].matches("-logrotate")){       this.logRotate(getArgsLower(args,++i));        fin=true; _exit=0; }
                else if ( args[i].matches("-portscan") ){       this.portScanner(getArgsLower(args,++i));      fin=true; _exit=0; }
                else if ( args[i].matches("-wlsconfig")){ _exit=this.wlsConfigTools(getArgsLower(args,++i));   fin=true; }
                else if ( args[i].matches("-wlsinfo")  ){ this.wlsInfoTools(getArgsLower(args,++i));     fin=true; donemsg=false; }
                else if ( args[i].matches("-mwinfo")   ){ this.mwInfo(getArgsLower(args,++i));           fin=true; donemsg=false; }
                else if ( args[i].matches("-wlsrota")  ){ this.wlsRotate(getArgsLower(args,++i));        fin=true; donemsg=false; }
                else if ( args[i].matches("-logrota")  ){ this.logApacheRotate(getArgsLower(args,++i));  fin=true; donemsg=false; }
                else if ( args[i].matches("-crypt")    ||
                          args[i].matches("-uncrypt")  ){ crypt.runArgs(getArgsLower(args,i));           fin=true; }  
                else if ( args[i].matches("-rota")        ){ this.logRotate(getArgsLower(args,++i));     fin=true; }
                else if ( args[i].matches("-gclog1")      ){ this.gcLog(getArgsLower(args,++i));         fin=true; }
                else if ( args[i].matches("-update")      ){ this.updateJar();                           fin=true; }
                else if ( args[i].matches("-unsecure")    ){ this.unsecureFile(getArgsLower(args,++i));  fin=true; }
                else if ( args[i].matches("-secure")   )   { this.secureFile(getArgsLower(args,++i));    fin=true; }
                else if ( args[i].matches("-getsecinfo"))  { this.unsecureFile(getArgsLower(args,++i));  fin=true; }
                else if ( args[i].matches("-pwfile")   ){ this.setPassword(args[++i]);                   fin=true; }
                else if ( args[i].matches("-pwInfo")   ){ this.unsecureInfo(getArgsLower(args,++i));     fin=true; }
                else if ( args[i].matches("-gclog")    ){ this.checkGC(getArgsLower(args,++i));          fin=true; }
                else if ( args[i].matches("-gcfile")   ){ this.checkGCFile(getArgsLower(args,++i));      fin=true; }
                else if ( args[i].matches("-checker")  ){ this.runChecker(getArgsLower(args,++i));       fin=true; }
                else if ( args[i].matches("-secdb")    ){ this.getSecDb(getArgsLower(args,++i));         fin=true; }
                else if ( args[i].startsWith("-ldap")  ){ this.runLdap(args[i].substring(1),getArgsLower(args,++i));  fin=true; }
                else if ( args[i].matches("-d")        ){ } // needs empty - run in pre-scan
                else if ( args[i].matches("-monitor")  ){ this.runMonitor(getArgsLower(args,++i));       fin=true; }
                else if ( args[i].matches("-newpass")  ){ this.getNewPassword(getArgsLower(args,++i));   fin=true; }
                else if ( args[i].matches("-diff")     ){ this.getFileDiff(getArgsLower(args,++i));      fin=true; }
                else if ( args[i].matches("-git")      ){ this.getGit(getArgsLower(args,++i));           fin=true; }
                else if ( args[i].matches("-imap")     ){ this.getMail(getArgsLower(args,++i));          fin=true; }
                else if ( args[i].matches("-pullhttp") ){ this.getPullHttp(getArgsLower(args,++i));      fin=true; }
                else if ( args[i].matches("-version")    ){ this.version(); _exit=0;                     fin=true; donemsg=false; }
                else if ( args[i].matches("-sysinfo")    ){ this.getSysInfo(getArgsLower(args,++i));     fin=true; }
                else if ( args[i].matches("-fullversion")){ this.version();  _exit=0;                fin=true; donemsg=false; }
                else if ( args[i].matches("-cp" ) ) {}
                else {
                    usage(); sleep(300); _exit=1; throw new RuntimeException("force closing - unknown argument"); 
                }
                printf(func,4,"parse loop ["+i+"/"+args.length+"] ends");
                if ( fin ) {  setClosed(); return; } // throw new RuntimeException("closing"); }
            }
        } catch(IOException | RuntimeException | KeyManagementException | NoSuchAlgorithmException e) {
            printf(func,1,"closing parsing with "+e.getMessage(),e);
            fin=true;
        }   finally { 
            parseCompleted=true;
        }
        if ( fin ) { setClosed(); } 
        return;
   }
    
   private void getNewPassword(String[] ar) {
      
       System.out.println("password : "+GetPassword.getStrongPassword( )+" :" 
                                       +" "+GetPassword.getMediumPassword() +" :"
                                       +" "+GetPassword.getEasyPassword() );
                                       
                                                  
   }
   
   private void  getFileDiff(String[] ar) {  com.macmario.io.lib.IOLib.getFileDiff(ar);   }
   private void  getSysInfo( String[] ar) { 
   
   }
    
   private void  getSecDb( String[] ar) { 
        System.out.println("secDB");
        SecDb sdb = new SecDb(ar);
              
        System.out.println("secDB init");
        try { sdb.run(); } catch(Exception e) { printf("getSecDb( String[] ar)",0,"ERROR:"+e.getMessage(),e);}
        System.out.println("secDB fin");
   }
   
   private void checkGCFile(String[] ar) {
        
        for(String a : ar) {
            GCFile gc = new GCFile(a); gc.debug=debug;
            if ( gc.isReadableFile() ) {
                while(gc.hasNext()) { gc.check(); }
            }
        }
    }
    
    private void checkGC(String[] ar) {
        GCMain gc = new GCMain(ar);
               gc.scan();
    }
    
    private void getPullHttp(String[] ar) {
        PullHttp.debug=debug;
        PullHttp ph = new PullHttp(ar); 
                 ph.debug = debug;
                 ph.run();
    }
    
    private void runLdap(String foo, String[] ar ) {
        final String func=getFunc("runLdap(String foo, String[] ar ) ");
        try {
            switch(foo) {
                case "ldapsearch" : 
                                    printf(func,4,"like to create Ldap Search instance");
                                    com.macmario.net.ldap.LdapSearch ls = com.macmario.net.ldap.LdapSearch.getInstance(ar);
                                    printf(func,4,"Ldap Search instance created ");
                                    //ls.printResults( ls.search(ls.getBaseDN(), ls.getFilter(), ls.getAttrList()) );
                                    ls.printResults( ls.search() );
                                    while( ls.couldSearchAgain() ) {
                                        ls.printResults( ls.trysearch() );
                                    }
                                    this._exit = ls.error_code;
                                    break;
                case "ldapbind"   : 
                                    com.macmario.net.ldap.LdapBind lb   = com.macmario.net.ldap.LdapBind.getInstance(ar);
                                    System.out.println("bind "+((lb.bind())?"successful":"failed"));
                                    this._exit=lb.error_code;
                                    break;
                case "ldapblk"    :
                case "ldapuserblk":
                                    com.macmario.net.ldap.LdapUserBlk lu = com.macmario.net.ldap.LdapUserBlk.getInstance(ar);
                                                         lu.runSearch();
                                                         System.out.println(lu.getCount()+" ldap entries found \tmodified:"+lu.getModified());
                                    break;
                case "ldapmodify":
                                    //System.out.println("init start");
                                    com.macmario.net.ldap.LdapModify lm = com.macmario.net.ldap.LdapModify.getInstance(ar);
                                    int pa=-1;
                                    //System.out.println("init complete");
                                    if ( lm.operationfile != null ) {
                                         ReadFile fa = new ReadFile(lm.operationfile);
                                         if ( fa.isReadableFile() ) {
                                            lm.modify(lm.operationfile);
                                            pa=1;
                                         }   
                                    } else if ( lm.getLdifFile() != null  ) {
                                         ReadFile fa = new ReadFile(lm.getLdifFile());
                                         System.out.println("file: "+fa.isReadableFile());
                                         if ( fa.isReadableFile() ) {
                                            lm.modify(lm.getLdifFile());
                                            pa=2;
                                         }
                                         System.out.println("fa:"+fa.isReadableFile()+" done");
                                    } else if ( lm.attrList != null ){
                                         lm.operate();
                                         pa=3;
                                    } else {
                                        System.out.println("no modification operation found");
                                    }
                                    //System.out.println("use path "+pa);
                                    //net.ldap.LdapSearch la = net.ldap.LdapSearch.getInstance(ar);
                                    //la.printResults( la.search(la.getBaseDN(), la.getFilter(), la.getAttrList()) );
                                    break;
                case "ldapcopy":
                                    com.macmario.net.ldap.LdapCopy lc = com.macmario.net.ldap.LdapCopy.getInstance(ar);
                                    System.out.println("le"+ar.length);
                                                     if ( lc != null && ! lc.usage ) {
                                                           lc.debug=debug;
                                                          try { 
                                                             printf(func,3," start ldap copy");  
                                                              lc.copy(); 
                                                             printf(func,3," complete ldap copy"); 
                                                          }
                                                          catch(RuntimeException re) {
                                                              lc.printUsage();
                                                          }
                                                          catch(Exception e) {
                                                              printf(func,1,"ldapcopy with excetion "+e.getMessage(),e);
                                                          }
                                                      } else {
                                                          printf(func,1,"no instance ");
                                                          lc.printUsage();
                                                      }     
                                    break;
                default:
                    System.out.println("ERROR: "+foo+" not found");
                    this._exit=1;
            }
        } catch(NamingException ne) {
            System.out.println("ERROR: "+ne.getMessage());
            printf(func,1,"full message:",ne);
            this._exit=-1;
        } catch(IOException io) {
            System.out.println("ERROR: "+io.getMessage());
            printf(func,1,"full message:",io);
            this._exit=-1;
        }     
    }
    private void runChecker(String[] ar) {
        Checker ch=new Checker(ar);
                ch.verify();
             _exit=ch.getResult();
    }
    
    private void runMonitor(String[] ar) {
        com.macmario.io.perf.Perf p= com.macmario.io.perf.Perf.getInstance(ar);        
                     p.debug=debug;
                     //if ( p.printUsage ) { System.out.println("usage: "+System.getProperty("prog")+" "+p.usage()); } 
                     p.test(); 
    }
    
    private void secureFile(String[] ar) {
        if ( ar.length > 0 ) {
            for (int i=0; i<ar.length; i++ ) {
                if ( ! ar[i].isEmpty()  )  {
                    if ( ar[i].equals("-help") ) {
                        System.out.println("usage: "+System.getProperty("prog")+" -secure <file <file1 ...>>");
                        return;
                    } else if ( ar[i].equals("-d") ) {    
                    } else {
                        ReadFile rf = new ReadFile(ar[i]);
                        if ( rf.isReadableFile() ) {
                            SecFile wf = new SecFile(ar[i]);
                               if ( ! wf.isCrypted() ) {
                                   wf.crypt();
                               }
                        } else {
                            System.out.println("WARNING: "+ar[i]+" is not a readable file - skipping");
                        }
                    }     
                }     
            }
        }
    }
    private void unsecureInfo(String[] ar) { System.out.println(getPassword(ar[0]));}
    private void unsecureFile(String[] ar) {
        if ( ar.length > 0 ) {
            for (int i=0; i<ar.length; i++ ) {
                if ( ar[i].equals("-help") ) {
                        System.out.println("usage: "+System.getProperty("prog")+" -unsecure <file <file1 ...>>");
                        return;
                    } else if ( ar[i].equals("-d") ) {    
                    } else {
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
    }
    
    private void getGit(String[] ar) {
        final String func=getFunc("getGit(String[] ar)");
        try { 
               Git g = new Git(ar);
                   g.response(ar);
        } catch(Exception me){
            printf(func,1,"Message read rrror : "+me.getMessage(),me);
        }       
    }
    
    private void getMail(String[] ar) {
        final String func=getFunc("getMail(String[] ar)");
        try { 
               Imap imap = com.macmario.comm.mail.Imap.getInstance(ar);
        } catch(Exception me){
            printf(func,1,"Message read rrror : "+me.getMessage(),me);
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
        com.macmario.net.wls.WlsDomainLogRotation.parseArgs(args);
        final String func=getFunc("wlsRotate(String[] args)");
        for ( String s : com.macmario.net.wls.WlsDomainLogRotation.dirs.split(com.macmario.net.wls.WlsDomainLogRotation.sepa) ) {
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
        com.macmario.net.apache.LogRotation.parseArgs(args);
        
        for ( String s : com.macmario.net.apache.LogRotation.dirs.split(com.macmario.net.apache.LogRotation.sepa) ) {
            if ( ! s.isEmpty() ) {
                ReadDir di = new ReadDir(s);
                if ( di.isDirectory() ) {
                com.macmario.net.apache.LogRotation wlog = new com.macmario.net.apache.LogRotation(di);
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
    
    private boolean sshCluster(String[] args) {
        final String func=getFunc("sshCluster(String[] args)");
        printf(func,2,"sshCluster(sshCommand] start");
        com.macmario.net.ssh.SSHCluster.debug=debug;
        com.macmario.net.ssh.SSHCluster sc = com.macmario.net.ssh.SSHCluster.getInstance(args);
        if ( sc.isValid() ) {
            sc.start();
            while( ! sc.isRunning() ) { sleep(100);}
            
            sc.setClosed();
            return true;
        } else {
            sc.usage();
        }
        return false;
    }
    
    private boolean sshCommand(String[] args) {
         final String func=getFunc("sshCommand(String[] args)");
         printf(func,2,"sshCommand start");
         
         SSHshell.debug=debug;
         SSHshell ssh = SSHshell.getInstance(args);

         if (ssh == null ){  return true; }
         if ( ssh.isSSHShell() ) {
            try { 
                printf(func,3,"send command :"+ssh.getCommand());
                System.out.println(ssh.sendSingleCommand().toString()); 
            } catch(IOException|NullPointerException e) { 
                printf(func,1,"send command error :"+e.getMessage());
                return false; 
            }
            printf(func,2,"send command return :"+ssh.isValid());
         } else {

           printf(func,3,"transfer files");  
           try {
               
               ArrayList<String> fr = new ArrayList();
               ArrayList<String> fl = new ArrayList();  
               int way=-1;
               for (String arg : ssh.getCommand().split(" ") ) {
                   if      ( arg.matches("scp")   ) { } 
                   else if ( arg.startsWith(":")  ) { fr.add(arg.substring(1)); if(way==-1){ way=1; } } 
                   else   { fl.add(arg); if(way==-1){ way=2; } }
               }
               printf(func,2,"remote files:"+fr.size()+" local files:"+fl.size() );
               
               StringBuilder sw = new StringBuilder();
               String[] rfiles = new String[ fr.size() ]; for( int j=0; j<fr.size(); j++ ) { rfiles[j]=fr.get(j); sw.append(rfiles[j]).append(";"); }
               printf(func,3,"remote files:"+fr.size()+" files: "+sw.toString());
               sw = new StringBuilder();
               String[] lfiles = new String[ fl.size() ]; for( int j=0; j<fl.size(); j++ ) { lfiles[j]=fl.get(j);  sw.append(lfiles[j]).append(";");}
               printf(func,3,"local files:"+fl.size()+" files: "+sw.toString());
               
               printf(func,3,"remote files:"+rfiles.length+" local files:"+lfiles.length );
               if ( rfiles.length == 0 || lfiles.length == 0) { throw new IOException("missing properties"); }
               if ( way == 1 ) {
                    ReadDir d = new ReadDir(lfiles[0]);
                    if ( ! d.isDirectory() ) { throw new IOException(lfiles[0]+" is not a local directory"); }
                    printf(func,2,"like to send from"+rfiles+" to local:"+lfiles[0]);
                    ssh.scpFrom(rfiles, lfiles[0]);
               } else {
                    printf(func,0,"like to send local "+lfiles+" to remote "+rfiles[0]);
                    ssh.scpTo(lfiles, rfiles[0]);
               }     

           } catch (IOException io ) {
               printf(func,1,"scp command error :"+io.getMessage());
               return false; 
           }  
             
         }   
         return ssh.isValid();
    }
    

    private boolean sshScript(String[] args ) {
        final String func=getFunc("sshScript(String[] args )");
         printf(func,2,"sshScript start - "+args.length );
         if ( args.length == 0 ) { args = new String[]{"--help"};}
         //SSHpass.debug=debug;
         SSHpass ssh = SSHpass.getInstance(args); 
                 ssh.debug=debug;
                 ssh.runScript();
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
    private int wlsConfigTools(String[] args) {
          final String func=getFunc("wlsConfigTools(String[] args)");
          int ret=-1;
          String dest=System.getProperty("user.home")+File.separator+"bin";
          WlsToolConfig w = new WlsToolConfig(); w.debug=debug;
          boolean forced=false;
          for( int i=0; i<args.length; i++) {
              if (args[i].matches("-help")    ) { 
                    String prog = System.getProperty("prog");
                    System.out.println( ( (prog==null)?"":prog )+" "+w.usage()); 
                    return ret;
              } 
              else if ( args[i].matches("-d")     ) { w.debug++; }
              else if ( args[i].matches("-forced")) {forced=true;}
                  
          }
          int doms=0;
          for( int i=0; i <args.length; i++ ) {
              
                  if ( args[i].matches("-dest") ) { 
                      dest=args[++i]; 
                      printf(func,2,"update destination to :"+dest);
                  }
                  else if (args[i].matches("-reconfig")) {  w.setUpdateNeeded();   }
                  else if (args[i].matches("-blackout")) {  w.setBlackoutNeeded(); }
                  else if (args[i].matches("-d")) { }
                  else { 
                      printf(func,2,"call updateConfig for "+args[i]);                  
                      w.updateConfig(args[i]); doms++;
                  }  
          }
          if ( doms == 0 ) {
                  System.out.println("WARNING: no weblogic domain checked");
               if ( ! forced ) {
                   System.out.println("ERROR: use option -forced to proceed"); 
               }
          }
          printf(func,2,"check configuration on dest:"+dest);
          w.checkConfig(dest);
          if ( w.isUpdateNeeded() ) { 
              printf(func,2,"call destionation update - needed");
              w.updateDestination(dest); 
              ret=0;
          } else {
              printf(func,3,"call destionation updated not needed");
          }
          return ret;
    }
    
    private void mwInfo(String[] args ) {
          final String func=getFunc("mwInfo(String[] args)");
          int ret=-1;
          System.out.println(com.macmario.net.wls.MwInfo.info(args));
          
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
    
    public static void main(String[] args) throws Exception {
           Mos m = new Mos(args); m.silent=true;
               m.start();

               while( (m.isRunning() && ! m.fin) || ! m.parseCompleted ) { sleep(300); }
               if ( m.donemsg ) System.out.println("done."); 
               System.exit(m._exit);
    }
    
    private void version() {
        System.out.println(this.getFullInfo());
        if ( ((debug > 0)?true:false) ) {
            System.out.println("full version are: "+this.getDebugVersion());
        }    
    }
    
    private void usage() {
        System.out.println(this.getFullInfo()+"\n\nOptions:\n"
                + "\t\t-version \t\t-\tprint version information\n\n"
                + "\t\t-crypt "+crypt.usage(false)+"\n\t\t\t\t\t-\tcrypt or uncrypt a string or file\n\n"
                + "\t\t\n"        
                + "\t\t-testssl <host> <port>\t-\tTest SSL Connection to the server and port \n"
                + "\n\t\t-sshcomm "+SSHshell.usage()+"\n\t\t\t\t\t-\tsend a single ssh command\n"
                + "\t\t-portscan [-host <host>] [-pmin <min port>] [-pmax <max port>]\t-\tport  scanner \n"
                + "\n\t\t-testhttp <url> [url1,]\t-\tTest URL Connection to URL\n"
                + "\n\t\t-checker "+Checker.usage()+"\n"
                + getVersionString("net.ldap") 
                //+ "\n\t\t-ldap -D <bindDN> -j <Password File> <-h <Host>> <-p <Port>> -filter <filter> -b <baseDN>\n"
                + "\n\t\t-wlsconfig "+WlsToolConfig.usage()+"\n\t\t\t\t\t-\tConfigure Wls Starting scripts in directory <dest>"
                //+ "\n\t\t-wlsconfig [-dest <script dir [.]>] <domaindir <domaindir1...>>\n\t\t\t\t\t-\tConfigure Wls Starting scripts in directory <dest>\n"
                + "\n\t\t-wlsinfo <domainhome> [<-server <servername>]\t-\n\t\t\t\t\tprint domain use information\n"
                + "\n\t\t-wlsrota "+WlsDomainLogRotation.usage()+"\n\t\t\t\t\tweblogic domain logrotation\n"
                + "\n\t\t-logrota "+com.macmario.net.apache.LogRotation.usage()+"\n\t\t\t\t\tapache|ohs logrotation\n"
                + "\n\t\t-pwfile <filename>\t-\tstore a password in a secure file\n"
                + "\n\t\t-secure <filename>\t-\tgenerate a secure file from filename\n"
                + "\n\t\t-unsecure <filename>\t-\tunsecure a secure file back to normal file\n"
                + "\n\t\t-mwinfo \t\t-\tget Middleware information\n"
                //+ "\n\t\t-logrotate\t"+(new LogRotation(new String[]{}).usage(false) )
                + "\n\n"
        );
        System.exit(-1);
    }
    private String getVersionString(String cl) {
        String[] inf=getValueFromClasses(cl,"myusage").split("\n");
        StringBuilder sw = new StringBuilder();
        int op=0;
        for (String g : inf) {
            if ( g.startsWith("class:") ) {
                 if ( sw.length() > 0 ) { sw.append("\n\n"); }
                 String[] sp = g.split(":");  sp=sp[1].split("\\.");
                 sw.append("\n\t\t-").append(sp[sp.length-1].toLowerCase()).append(" ");
                 op=0;
            }
            else if ( op==0 && g.startsWith("usage()") ) {}
            else if ( op==0 && g.startsWith("option:") ) {
                sw.append(g.substring("option:".length()));
                op=1;
            }
            else if ( op == 1 ) {
                sw.append(g).append("\n");
            }
        }
        if ( sw.length() > 0 ) { sw.append("\n\n"); }
        return sw.toString();
    }
    private String getValueFromClasses(String pack, String key) {
        final String func=getFunc("getValueFromClasses(String pack, String key)");
        StringBuilder sw = new StringBuilder();
        printf(func,4,"incoming");
        try {
            lib.fillJarMap(jarfile);
            String a = ("/"+pack).replaceAll("\\.", "\\/")+"/";
            String[] cllist = lib.getClassFromPackage(pack);
            for ( String cl : cllist ) {
                String clret = lib.getValueFromClass(cl.replaceAll("/", "\\."),"free");
                //printf(func,3,"cl:"+cl+": free:"+( clret == null || (clret != null && clret.equals("true")))+" =>"+( clret == null )+"||"+(clret != null && clret.equals("true"))+" ==>"+((clret !=null)?clret:"NULL" ));
                        
                if ( clret == null || (clret != null && clret.equals("true"))) {        
                        clret = lib.getValueFromClass(cl.replaceAll("/", "\\."),key);
                        if ( clret != null ) {
                             printf(func,3,"cl:"+cl+": key:"+key+": clret:"+clret);
                             sw.append("class:"+cl.replaceAll("/", "\\.")+": attribute:"+key+":\n");
                             sw.append(clret).append("\n");
                        }
                }        
            }

       } catch(Exception e) {
            printf(func,1,"ERROR: "+e.getMessage(), e);
       }     
        
        printf(func,3,"outgoing  :"+sw.toString()+":" );
            
        return sw.toString();
    }
}