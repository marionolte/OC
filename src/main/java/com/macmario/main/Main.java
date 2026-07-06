/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.main;


import com.macmario.comm.checker.Checker;
import com.macmario.io.logs.LogRotation;
import com.macmario.io.net.PullHttp;
import com.macmario.io.net.Http;
import com.macmario.net.ssh.SSHshell;
import com.macmario.general.Updater;
import com.macmario.comm.mail.Imap;
import com.macmario.io.Console;
import com.macmario.io.crypt.Crypt;
import com.macmario.io.crypt.GetPassword;
import com.macmario.io.db.SecDb;
import com.macmario.io.file.ReadDir;
import com.macmario.io.file.ReadFile;
import com.macmario.io.file.SecDBZipFile;
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
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;




/**
 *
 * @author SuMario
 */
public class Main extends Updater{

    public boolean stopProgress=false;
    public boolean silent=false;
    private final Crypt   crypt;
    private final Console console;
    private String[] args=null;
    private int _exit=-1;

    
    public Main(String[] args) throws IOException {
        super();
        this.args=args;
        this.crypt = new Crypt();
        this.console = new Console(this);
        this.console.setRunning();
        IOLib.fillJarMap(jarfile);
    }
    
    private boolean testssl(String url) {
        //System.out.println("url:"+url+":");
        URI u; String ho=null; int po=-1;
        try {
            u = new URI(url);
            ho = u.getHost();
            po =u.getPort();
            po =( po == -1 )?443:po;
        } catch ( java.net.URISyntaxException io ){
            System.out.println("ERROR:  URI not correct with "+url+" - reason:"+io.getMessage());
            return false;
        }    
        return testssl(ho, po); 
    }
    private boolean testssl(String ho, String po) { return testssl(ho, Integer.parseInt(po)); }
    private boolean testssl(String ho, int po   ) { 
        System.out.println("test server "+ho+":"+po);
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
            if ( args.length > 0 ){
                ArrayList<String> ar = new ArrayList<>();
                for( int i=0; i<args.length; i++ ) {
                    if ( args[i].equals("-d")        ){ debug++; }
                    else { ar.add(args[i]); }
                }
                if  ( ar.isEmpty() ) { this.usage(); }
                else {
                    args=new String[ar.size()];
                    for ( int j=0; j<ar.size();j++){ args[j]=ar.get(j); }
                }                    
            }    
            for( int i=0; i<args.length; i++ ) {
                printf(func,3,"test args["+i+"/"+args.length+"]:"+args[i]+":");
                if      ( args[i].equals("-testssl") ) { 
                                                          String a1= args[++i];
                                                          
                                                          String a2= ( args.length>(i+1) )?args[++i]:null;
                                                          printf(func,3,"testssl a1->:"+a1+":  a2->"+a2);
                                                          if ( isNotNullOrEmpty(a2) ) {
                                                              _exit = (  testssl(a1,a2) )?0:1;   
                                                          } else { 
                                                              _exit = (  testssl(a1)    )?0:1;   
                                                          }                                                          
                                                          fin=true; 
                }
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
                else if ( args[i].matches("-genpassword")){    String pw=""; i++; 
                                                               int len=12;
                                                               if ( args.length>i ) {
                                                                    for( int j=i; i<args.length; i++, j++ ) {
                                                                      if ( args[i].equals("-len") ) { len=getInt(args[++i]); } 
                                                                       else{ pw=args[i]; }                                                                    
                                                                    }
                                                               }
                                                               //System.out.println("pw:"+pw+": "+len);
                                                               System.out.println( getNewPassword(len, pw )  );                           
                                                                                                               fin=true; _exit=0; }
                else if ( args[i].matches("-logrotate") ){       this.logRotate(getArgsLower(args,++i));        fin=true; _exit=0; }
                else if ( args[i].matches("-portscan")  ){       this.portScanner(getArgsLower(args,++i));      fin=true; _exit=0; }
                else if ( args[i].matches("-wlsconfig") ){ _exit=this.wlsConfigTools(getArgsLower(args,++i));   fin=true; }
                else if ( args[i].matches("-wlsinfo")   ){ this.wlsInfoTools(getArgsLower(args,++i));     fin=true; donemsg=false; }
                else if ( args[i].matches("-mwinfo")    ){ this.mwInfo(getArgsLower(args,++i));           fin=true; donemsg=false; }
                else if ( args[i].matches("-wlsrota")   ){ this.wlsRotate(getArgsLower(args,++i));        fin=true; donemsg=false; }
                else if ( args[i].matches("-logrota")   ){ this.logApacheRotate(getArgsLower(args,++i));  fin=true; donemsg=false; }
                else if ( args[i].matches("-crypt")    ||
                          args[i].matches("-uncrypt")   ){ crypt.runArgs(getArgsLower(args,i));           fin=true; }  
                else if ( args[i].matches("-rota")      ){ this.logRotate(getArgsLower(args,++i));     fin=true; }
                else if ( args[i].matches("-gclog1")    ){ this.gcLog(getArgsLower(args,++i));         fin=true; }
                else if ( args[i].matches("-update")    ){ this.updateJar();                           fin=true; }
                else if ( args[i].matches("-unsecure")  ){ this.unsecureFile(getArgsLower(args,++i));  fin=true; }
                else if ( args[i].matches("-secure")    ){ this.secureFile(getArgsLower(args,++i));    fin=true; }
                else if ( args[i].matches("-getsecinfo")){ this.unsecureFile(getArgsLower(args,++i));  fin=true; }
                else if ( args[i].matches("-pwfile")    ){ this.setPassword(args[++i]);                   fin=true; }
                else if ( args[i].matches("-pwInfo")    ){ this.getPWFromFile(getArgsLower(args,++i));    fin=true; }
                else if ( args[i].matches("-gclog")     ){ this.checkGC(getArgsLower(args,++i));          fin=true; }
                else if ( args[i].matches("-gcfile")    ){ this.checkGCFile(getArgsLower(args,++i));      fin=true; }
                else if ( args[i].matches("-checker")   ){ this.runChecker(getArgsLower(args,++i));       fin=true; }
                else if ( args[i].matches("-secdb")     ){ this.getSecDb(getArgsLower(args,++i));         fin=true; }
                else if ( args[i].matches("-secdbfile") ){ this.getSecDbFile(getArgsLower(args,++i));     fin=true; }
                else if ( args[i].startsWith("-ldap")   ){ this.runLdap(args[i].substring(1),getArgsLower(args,++i));  fin=true; }
                else if ( args[i].matches("-d")         ){ } // needs empty - run in pre-scan
                else if ( args[i].matches("-keepass")   ){ this.getKeePass(getArgsLower(args,++i));       fin=true; }
                else if ( args[i].matches("-monitor")   ){ this.runMonitor(getArgsLower(args,++i));       fin=true; }
                else if ( args[i].matches("-newpass")   ){ this.getNewPassword(getArgsLower(args,++i));   fin=true; }
                else if ( args[i].matches("-diff")      ){ this.getFileDiff(getArgsLower(args,++i));      fin=true; }
                else if ( args[i].matches("-git")       ){ this.getGit(getArgsLower(args,++i));           fin=true; }
                else if ( args[i].matches("-imap")      ){ this.getMail(getArgsLower(args,++i));          fin=true; }
                else if ( args[i].matches("-pullhttp")  ){ this.getPullHttp(getArgsLower(args,++i));      fin=true; }
                else if ( args[i].matches("-version")   ){ this.version(); _exit=0;                     fin=true; donemsg=false; }
                else if ( args[i].matches("-sysinfo")   ){ this.getSysInfo(getArgsLower(args,++i));     fin=true; }
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
    
   private void getKeePass(String[] ar) {
        final String func = getFunc("getKeePass(String[] ar)");
        final String prog = (System.getProperty("prog") == null) ? "" : System.getProperty("prog");
        final String usage = "usage: " + prog + " -keepass <action> [options]\n"
            + "  actions:\n"
            + "    create      -db <file> (-pw <pw>|-j <secfile>) [-name <dbname>] [-format kdbx31|kdbx4]\n"
            + "    list        -db <file> (-pw <pw>|-j <secfile>) [-group <path>]\n"
            + "    get         -db <file> (-pw <pw>|-j <secfile>) [-group <path>] -title <title>\n"
            + "    addentry    -db <file> (-pw <pw>|-j <secfile>) [-group <path>] -title <title>"
            + " [-user <u>] [-epw <pw>] [-url <url>] [-notes <n>]\n"
            + "    updateentry -db <file> (-pw <pw>|-j <secfile>) -uuid <uuid>"
            + " [-title <t>] [-user <u>] [-epw <pw>] [-url <url>] [-notes <n>]\n"
            + "    delentry    -db <file> (-pw <pw>|-j <secfile>) (-uuid <uuid> | -group <path> -title <title>)\n"
            + "    addgroup    -db <file> (-pw <pw>|-j <secfile>) -group <path>\n"
            + "    delgroup    -db <file> (-pw <pw>|-j <secfile>) -group <path>\n"
            + "    passwd      -db <file> (-pw <pw>|-j <secfile>) -newpw <newpw>\n"
            + "    attach      -db <file> (-pw <pw>|-j <secfile>) (-uuid <uuid> | -group <path> -title <title>) -file <file> [-att <name>]\n"
            + "    listattach  -db <file> (-pw <pw>|-j <secfile>) (-uuid <uuid> | -group <path> -title <title>)\n"
            + "    extract     -db <file> (-pw <pw>|-j <secfile>) (-uuid <uuid> | -group <path> -title <title>) -att <name> -out <file>\n"
            + "    delattach   -db <file> (-pw <pw>|-j <secfile>) (-uuid <uuid> | -group <path> -title <title>) -att <name>\n";

        if (ar == null || ar.length == 0) { System.out.println(usage); _exit = 1; return; }

        String action = ar[0].toLowerCase();
        String dbFile = null, pw = null, pwFile = null, newpw = null, dbName = null, format = null;
        String group = null, title = null, user = null, epw = null, url = null, notes = null, uuid = null;
        String attFile = null, attName = null, outFile = null;

        try {
            for (int i = 1; i < ar.length; i++) {
                if      (ar[i].equals("-db")     && i + 1 < ar.length) { dbFile  = ar[++i]; }
                else if (ar[i].equals("-pw")     && i + 1 < ar.length) { pw      = ar[++i]; }
                else if (ar[i].equals("-j")      && i + 1 < ar.length) { pwFile  = ar[++i]; }
                else if (ar[i].equals("-newpw")  && i + 1 < ar.length) { newpw   = ar[++i]; }
                else if (ar[i].equals("-name")   && i + 1 < ar.length) { dbName  = ar[++i]; }
                else if (ar[i].equals("-format") && i + 1 < ar.length) { format  = ar[++i]; }
                else if (ar[i].equals("-group")  && i + 1 < ar.length) { group   = ar[++i]; }
                else if (ar[i].equals("-title")  && i + 1 < ar.length) { title   = ar[++i]; }
                else if (ar[i].equals("-user")   && i + 1 < ar.length) { user    = ar[++i]; }
                else if (ar[i].equals("-epw")    && i + 1 < ar.length) { epw     = ar[++i]; }
                else if (ar[i].equals("-url")    && i + 1 < ar.length) { url     = ar[++i]; }
                else if (ar[i].equals("-notes")  && i + 1 < ar.length) { notes   = ar[++i]; }
                else if (ar[i].equals("-uuid")   && i + 1 < ar.length) { uuid    = ar[++i]; }
                else if (ar[i].equals("-file")   && i + 1 < ar.length) { attFile = ar[++i]; }
                else if (ar[i].equals("-att")    && i + 1 < ar.length) { attName = ar[++i]; }
                else if (ar[i].equals("-out")    && i + 1 < ar.length) { outFile = ar[++i]; }
                else { printf(func, 2, "WARNING: ignoring unknown/!incomplete option: " + ar[i]); }
            }

            if (dbFile == null) { System.out.println("ERROR: -db <file> is required\n" + usage); _exit = 1; return; }
            // -j reads the master password from a secure file (project convention); -pw takes it literally.
            if (pw == null && pwFile != null) { pw = getPassword(pwFile); }
            if (pw == null) { System.out.println("ERROR: master password required (-pw or -j)\n" + usage); _exit = 1; return; }

            File db = new File(dbFile);

            // ---- actions that do not require opening an existing database -----------------
            if (action.equals("create") || action.equals("new")) {
                com.macmario.io.crypt.KeePass.Format fmt = "kdbx31".equalsIgnoreCase(format)
                        ? com.macmario.io.crypt.KeePass.Format.KDBX31
                        : com.macmario.io.crypt.KeePass.Format.KDBX4;
                com.macmario.io.crypt.KeePass kp = com.macmario.io.crypt.KeePass.create(dbName, fmt);
                kp.save(db, pw);
                System.out.println("created KeePass database: " + db + " (" + fmt + ")");
                _exit = 0; return;
            }
            if (action.equals("passwd") || action.equals("chpw")) {
                if (newpw == null) { System.out.println("ERROR: -newpw <newpw> is required\n" + usage); _exit = 1; return; }
                com.macmario.io.crypt.KeePass.changeMasterPassword(db, pw, newpw);
                System.out.println("master password changed for: " + db);
                _exit = 0; return;
            }

            // ---- all remaining actions operate on an opened database ----------------------
            com.macmario.io.crypt.KeePass kp = com.macmario.io.crypt.KeePass.open(db, pw);
            boolean save = false;

            switch (action) {
                case "list":
                    com.macmario.io.crypt.KeePass.Format f = kp.getFormat();
                    System.out.println("database: " + (kp.getDatabaseName() == null ? "(unnamed)" : kp.getDatabaseName())
                            + " [" + f + "]");
                    keePassPrintTree(kp.findGroup(group), 0);
                    break;

                case "get": {
                    if (title == null) { System.out.println("ERROR: -title <title> is required\n" + usage); _exit = 1; return; }
                    org.linguafranca.pwdb.kdbx.jackson.JacksonEntry e = kp.findEntry(group, title);
                    if (e == null) { System.out.println("entry not found: title=" + title + " group=" + (group == null ? "/" : group)); _exit = 1; return; }
                    System.out.println("uuid    : " + e.getUuid());
                    System.out.println("title   : " + e.getTitle());
                    System.out.println("user    : " + e.getUsername());
                    System.out.println("password: " + e.getPassword());
                    System.out.println("url     : " + e.getUrl());
                    System.out.println("notes   : " + e.getNotes());
                    System.out.println("attach  : " + kp.listAttachments(e.getUuid()));
                    break;
                }
                case "addentry":
                    if (title == null) { System.out.println("ERROR: -title <title> is required\n" + usage); _exit = 1; return; }
                    org.linguafranca.pwdb.kdbx.jackson.JacksonEntry added = kp.addEntry(group, title, user, epw, url, notes);
                    System.out.println("added entry: " + added.getTitle() + " uuid=" + added.getUuid());
                    save = true;
                    break;

                case "updateentry":
                    if (uuid == null) { System.out.println("ERROR: -uuid <uuid> is required\n" + usage); _exit = 1; return; }
                    boolean upd = kp.updateEntry(java.util.UUID.fromString(uuid), title, user, epw, url, notes);
                    System.out.println(upd ? "updated entry: " + uuid : "entry not found: " + uuid);
                    if (!upd) { _exit = 1; return; }
                    save = true;
                    break;

                case "delentry":
                    boolean delE = (uuid != null)
                            ? kp.deleteEntry(java.util.UUID.fromString(uuid))
                            : kp.deleteEntry(group, title);
                    System.out.println(delE ? "deleted entry" : "entry not found");
                    if (!delE) { _exit = 1; return; }
                    save = true;
                    break;

                case "addgroup":
                    if (group == null) { System.out.println("ERROR: -group <path> is required\n" + usage); _exit = 1; return; }
                    kp.addGroup(group);
                    System.out.println("added group: " + group);
                    save = true;
                    break;

                case "delgroup":
                    if (group == null) { System.out.println("ERROR: -group <path> is required\n" + usage); _exit = 1; return; }
                    boolean delG = kp.deleteGroup(group);
                    System.out.println(delG ? "deleted group: " + group : "group not found: " + group);
                    if (!delG) { _exit = 1; return; }
                    save = true;
                    break;

                case "attach": {
                    if (attFile == null) { System.out.println("ERROR: -file <file> is required\n" + usage); _exit = 1; return; }
                    java.util.UUID id = keePassResolveUuid(kp, uuid, group, title);
                    if (id == null) { System.out.println("ERROR: entry not found (use -uuid or -group/-title)"); _exit = 1; return; }
                    File fi = new File(attFile);
                    kp.addAttachment(id, attName == null ? fi.getName() : attName, fi);
                    System.out.println("attached " + (attName == null ? fi.getName() : attName) + " to " + id);
                    save = true;
                    break;
                }
                case "listattach": {
                    java.util.UUID id = keePassResolveUuid(kp, uuid, group, title);
                    if (id == null) { System.out.println("ERROR: entry not found (use -uuid or -group/-title)"); _exit = 1; return; }
                    System.out.println("attachments: " + kp.listAttachments(id));
                    break;
                }
                case "extract": {
                    if (attName == null || outFile == null) { System.out.println("ERROR: -att <name> and -out <file> are required\n" + usage); _exit = 1; return; }
                    java.util.UUID id = keePassResolveUuid(kp, uuid, group, title);
                    if (id == null) { System.out.println("ERROR: entry not found (use -uuid or -group/-title)"); _exit = 1; return; }
                    boolean ok = kp.extractAttachment(id, attName, new File(outFile));
                    System.out.println(ok ? "extracted " + attName + " -> " + outFile : "attachment not found: " + attName);
                    if (!ok) { _exit = 1; return; }
                    break;
                }
                case "delattach": {
                    if (attName == null) { System.out.println("ERROR: -att <name> is required\n" + usage); _exit = 1; return; }
                    java.util.UUID id = keePassResolveUuid(kp, uuid, group, title);
                    if (id == null) { System.out.println("ERROR: entry not found (use -uuid or -group/-title)"); _exit = 1; return; }
                    boolean ok = kp.deleteAttachment(id, attName);
                    System.out.println(ok ? "deleted attachment: " + attName : "attachment not found: " + attName);
                    if (!ok) { _exit = 1; return; }
                    save = true;
                    break;
                }
                default:
                    System.out.println("ERROR: unknown action: " + action + "\n" + usage);
                    _exit = 1;
                    return;
            }

            if (save) { kp.save(db, pw); printf(func, 3, "INFO: saved " + db); }
            _exit = 0;

        } catch (com.macmario.io.crypt.KeePass.KeePassException ke) {
            System.out.println("ERROR: " + ke.getMessage());
            printf(func, 1, "KeePass failure:", ke);
            _exit = 1;
        } catch (IllegalArgumentException iae) {
            System.out.println("ERROR: " + iae.getMessage() + "\n" + usage);
            _exit = 1;
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
                printf(func, 1, "unexpected failure:", e);
            _exit = -1;
        }
   }

   /** Resolves a target entry UUID either directly or by group-path + title. Returns {@code null} if not found. */
   private java.util.UUID keePassResolveUuid(com.macmario.io.crypt.KeePass kp, String uuid, String group, String title) {
        if (uuid != null) { return java.util.UUID.fromString(uuid); }
        if (title == null) { return null; }
        org.linguafranca.pwdb.kdbx.jackson.JacksonEntry e = kp.findEntry(group, title);
        return (e == null) ? null : e.getUuid();
   }

   /** Prints the group/entry tree under {@code group} with indentation; tolerates a {@code null} group. */
   private void keePassPrintTree(org.linguafranca.pwdb.kdbx.jackson.JacksonGroup group, int depth) {
        if (group == null) { System.out.println("(group not found)"); return; }
        String pad = "  ".repeat(depth);
        System.out.println(pad + "[" + (group.getName() == null ? "/" : group.getName()) + "]");
        for (org.linguafranca.pwdb.kdbx.jackson.JacksonEntry e : group.getEntries()) {
            System.out.println(pad + "  - " + e.getTitle() + "  (uuid=" + e.getUuid() + ")");
        }
        for (org.linguafranca.pwdb.kdbx.jackson.JacksonGroup g : group.getGroups()) {
            keePassPrintTree(g, depth + 1);
        }
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
   
   private void  getSecDbFile( String[] ar) { 
        System.out.println("secDBFile");
        SecDBZipFile.main(ar);
        System.out.println("secDBFile fin");
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
        } catch(NamingException|IOException ne) {
            System.out.println("ERROR: "+ne.getMessage());
            printf(func,1,"full message:",ne);
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
    
    private void getPWFromFile(String[] ar){
        System.out.println("getPWFromFile");
        for ( String f: ar ) {
            System.out.println("f0:"+f);
            if ( isNotNullOrEmpty(f)){
                System.out.println("f1:"+f);
                ReadFile rf = new ReadFile(f);
                if ( rf.isReadableFile()) {
                    System.out.println("f2:"+f);
                    String s = rf.readOut().toString();
                    System.out.println("s:"+s);
                    if ( crypt.isCrypted(s) ) {
                        System.out.println("is crypted");
                        s= crypt.getUnCrypted(s);
                        System.out.println("new s:"+s);
                    }
                    System.out.println(s);
                }else {
                    System.out.println("WARN: not a readable file: "+f);
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
               
               ArrayList<String> fr = new ArrayList<>();
               ArrayList<String> fl = new ArrayList<>();  
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
           Main m = new Main(args); m.silent=true;
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
                + "\n\t\t-keepass \t\t-\thandle KeePass database information\n"
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
            IOLib.fillJarMap(jarfile);
            String a = ("/"+pack).replaceAll("\\.", "\\/")+"/";
            String[] cllist = IOLib.getClassFromPackage(pack);
            for ( String cl : cllist ) {
                String clret = IOLib.getValueFromClass(cl.replaceAll("/", "\\."),"free");
                //printf(func,3,"cl:"+cl+": free:"+( clret == null || (clret != null && clret.equals("true")))+" =>"+( clret == null )+"||"+(clret != null && clret.equals("true"))+" ==>"+((clret !=null)?clret:"NULL" ));
                        
                if ( clret == null || (clret != null && clret.equals("true"))) {        
                        clret = IOLib.getValueFromClass(cl.replaceAll("/", "\\."),key);
                        if ( clret != null ) {
                             printf(func,3,"cl:"+cl+": key:"+key+": clret:"+clret);
                             sw.append("class:"+cl.replaceAll("/", "\\.")+": attribute:"+key+":\n");
                             sw.append(clret).append("\n");
                        }
                }        
            }

       } catch(IOException e) {
            printf(func,1,"ERROR: "+e.getMessage(), e);
       }     
        
        printf(func,3,"outgoing  :"+sw.toString()+":" );
            
        return sw.toString();
    }
}
