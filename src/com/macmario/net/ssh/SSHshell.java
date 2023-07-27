/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.ssh;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.HTTPProxyData;
import com.trilead.ssh2.KnownHosts;
import com.trilead.ssh2.SCPClient;
import com.trilead.ssh2.Session;
import com.macmario.io.file.ReadDir;
import com.macmario.io.file.ReadFile;
import com.macmario.io.file.SecFile;
import com.macmario.io.thread.RunnableT;

//import io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 * @author SuMario
 */
public class SSHshell  extends RunnableT {
    private final String  host;
    private final int     port;
    private final String  user;
    private final String  pass;
    private       String  phost;
    private       int     pport;
    private       String  puser;
    private       String  ppass;
    //private final Console console;
    private final boolean guiMode;
    
    private int   sid=-1;
    
    private Connection conn;
    private Session    sess;
    private OutputStreamWriter outw;
    private OutputStream out;
    private InputStream in;
    private InputStream err;
    private File keyFile=null;
    private String keyFileString="";
    
    public KnownHosts database = new KnownHosts();
    
    final public String knownHostPath ;
    final public String idDSAPath ;
    final public String idRSAPath ;
          public String idDSAPathProxy ;
          public String idRSAPathProxy ;
          
    private SSHDB sshdb ;      
    
   static public String confDir=System.getProperty("user.home")+File.separator+".ssh";
   
    private boolean _success = false;  
    
    public SSHshell(String host, String user, String pass) {
        this(host,22,user,pass,false);
    }
    public SSHshell(String host, String user, String pass, boolean gui) {
        this(host,22,user,pass,gui);
    }
    public SSHshell(String host, int port, String user, String pass,boolean gui) {
        final String func="SSHshell::";
        this.debug=SSHshell.debug;
        this.host=host;
        this.port=(port >0 && port < 64*1024-1)?port:22;
        this.user=user;
        this.pass=pass;
        this.guiMode=gui;
        
        //this.console=new Console();
    
        knownHostPath=SSHshell.confDir+File.separator+"known_hosts";
        idDSAPath    =SSHshell.confDir+File.separator+"id_dsa";
        idRSAPath    =SSHshell.confDir+File.separator+"id_rsa";
        
        printf(func,1," knownHostPath:\t"+knownHostPath+"\nidDSAPath:\t\t"+idDSAPath+"\nidRSAPath:\t\t"+idRSAPath);
        
        sshdb = new SSHDB(SSHshell.confDir+File.separator+".local.sdb");
        init();
        printf(func,1,"end Constructor");
    }
    
    public SSHshell(SecFile con) {
        final String func="SSHshell::";
        this.debug=SSHshell.debug;
        
        Properties p = con.getProperties();
        
        knownHostPath=SSHshell.confDir+File.separator+"known_hosts";
        idDSAPath    = p.getProperty("DKEY", SSHshell.confDir+File.separator+"id_dsa");
        idRSAPath    = p.getProperty("KEY", SSHshell.confDir+File.separator+"id_rsa");
        
        idDSAPathProxy    = p.getProperty("PROXYDKEY", SSHshell.confDir+File.separator+"id_dsa");
        idRSAPathProxy    = p.getProperty("PROXYKEY", SSHshell.confDir+File.separator+"id_rsa");
        
        int po;
        try{ po=Integer.parseInt(p.getProperty("PORT", "22")); } catch(java.lang.NumberFormatException ne) { po=22;}
        
        this.host=p.getProperty("HOST","localhost");
        this.port=(po >0 && po < 64*1024-1)?po:22;
        this.user=p.getProperty("USER", System.getProperty("user.name") );
        String pa=p.getProperty("PASS", "" );
        ReadFile fa = new ReadFile(pa);
                 if ( fa.isReadableFile() ) {
                      SecFile fp = new SecFile(fa);
                 }
        this.pass=p.getProperty("PASS", "" );
        this.guiMode=false;
        
        try{ po=Integer.parseInt(p.getProperty("PROXYPORT", "22")); } catch(java.lang.NumberFormatException ne) { po=22;}
        
        this.phost=p.getProperty("PROXYHOST","localhost");
        this.pport=(po >0 && po < 64*1024-1)?po:22;
        this.puser=p.getProperty("PROXYUSER", System.getProperty("user.name") );
        this.ppass=p.getProperty("PROXYPASS", "" );
        
        sshdb = new SSHDB(SSHshell.confDir+File.separator+".local.sdb");
        
        init();
        printf(func,1,"end Constructor");
    }
    
    void setSID(int sid) { if(sid>=0){ this.sid=sid;}}
    int  getSID(){ return this.sid; }
    private void init() {
        final String func=getFunc("init()");   
        File knownHostFile = new File(knownHostPath);
        if ( knownHostFile.exists() )  {
            try {
                database.addHostkeys(knownHostFile);
            }catch (java.io.IOException e) { 
                printf(func,1,"ERROR database reason:"+e.getMessage());
            }
        }
        start();
    }
    
   
    @Override
    public void    setClosed(){ 
        super.setClosed();
        if ( in   != null ) try {  in.close(); }catch(java.io.IOException io) { }finally{   in=null; }
        if ( outw != null ) try {outw.close(); }catch(java.io.IOException io) { }finally{ outw=null; }
        if ( out  != null ) try { out.close(); }catch(java.io.IOException io) { }finally{  out=null; }
        if ( err  != null ) try { err.close(); }catch(java.io.IOException io) { }finally{  err=null; }
        
        if ( sess != null ) { sess.close(); sess=null; }
        if ( conn != null ) { conn.close(); conn=null; }
    }
    
    public Session            getSession(    ) { return sess; }
    public InputStream        getOutput(     ) { return in;   }
    public OutputStream       getInput(      ) { return out;  }
    public OutputStreamWriter getInputWriter() { return outw; }
    
    public boolean write(int c) { try { out.write(c); if (c==10) { flush(); } }catch(IOException io){ return false; } return true; }
    public boolean flush()      { try { out.flush();                          } catch(IOException io){ return false; } return true;  }
    public byte[] read() throws IOException {
        int count = in.available();
        if ( count > 0 ) { 
            byte[] b=new byte[count];
            in.read(b, 0, count);
            return b;
        }
           count = err.available();
        if ( count > 0 ) { 
            byte[] b=new byte[count];
            err.read(b, 0, count);
            return b;
        }   
        
        return null;
    }
    
    public boolean isLoggedIn() { return login; }
    
    private String proxytype="EXEC";
    public void setProxyConnect(String typ) {
        proxytype=( typ == null || typ.toUpperCase().equals("HTTP") )?"HTTP":"EXEC";
    }
    public void setConnection() {
        final String func=getFunc("setConnection()");
            _initSession=false;
            
            printf(func,2,"create ssh connection to "+user+":"+pass+"@"+getHost());
            conn = new Connection(host,port);
            printf(func,3,"conn defined");
            if ( proxy != null ) { 
                printf(func,3,"set proxy");
                conn.setProxyData(proxy); 
                printf(func,3,"set proxy complete");
            }
            printf(func,3,"call connect");
            try { conn.connect(); } 
            catch(java.io.IOException io) {
                printf(func,1,"call connect error:"+io.getMessage());
            }
            //setUnClosed();
            printf(func,2,"call connect return");
            
    }
    
    private boolean _initSession=false;
    
    public void setSession() throws IOException {
        final String func=getFunc("setSession()");
        if ( ! isLogin()  ) { login(); }
        if ( _initSession ) { return; }
        if ( this.isSSHShell() ) {
            if ( sess == null ) {
                printf(func,3,"take session");
                sess = conn.openSession();
                printf(func,3,"take err");
                err  = sess.getStderr();
                printf(func,3,"take in");
                in   = sess.getStdout();
                printf(func,3,"take out");
                out  = sess.getStdin();
                printf(func,3,"take utf8 out");
                outw = new OutputStreamWriter(out, "utf-8");
                if ( ! setShell("bash") ) {
                     printf(func,1,"shell not set");
                     _initSession=false;
                     throw new java.io.IOException("shell could not started");
                }
                _initSession=true;
                printf(func,3,"ssh login completed "+user+"@"+getHost());
                sleep(2000);
                String[] sr =this.stdoutReceived().toString().split("\n");
                lastLine=sr[ sr.length-1 ];
                printf(func,2,">|"+lastLine+"|<");
            }    
        } else {
                printf(func,3,scom+" login completed "+user+"@"+getHost());
                _initSession=true;
        }
        printf(func,4,"setSession return");
    }
    
    public void setKeyFile(String k) { this.keyFileString=k;}
    public void setKeyFile(File k) { this.keyFile=k;}
    public boolean login(){
        if( isLogin() ) { return isLogin(); }
        
        final String func=getFunc("login()");
        printf(func,3,"host:"+host+":"+port+" u="+user+": p:"+pass+": keyfile:"+keyFile);
             if ( host == null || host.isEmpty() ) { printf(func,1,"ERROR: hostname are not set");          _success=false; return _success; }
             if ( user == null || user.isEmpty() ) { printf(func,1,"ERROR: user are not set");              _success=false; return _success;  }
             if ( pass == null || pass.isEmpty() ) { printf(func,1,"ERROR: password are not set or empty"); _success=false; return _success;  }
        
        try {
            printf(func,3,"set Connection");
            setConnection();
            
            printf(func,3,"ssh connection open to "+getHost()+"  conn:"+conn);
            
            try {
                Set<String> availableMethods = new HashSet<String>(Arrays.asList(conn.getRemainingAuthMethods(user)));
                printf(func,2,"user auth:"+availableMethods+":   keyfile:"+((keyFile==null)?"NULL":keyFile.getCanonicalFile()));
            } catch(Exception e) {
                printf(func,1,"ERROR: call methods fails with:"+e.getMessage());
            }    
            boolean isAuthenticated = false; 
            if ( keyFile != null &&conn.isAuthMethodAvailable(user, "publickey") ) {
                try {
                    printf(func,3,"public key auth:"+user+":"+((pass.isEmpty())?"empty":"KeyPASS SET")+":  keyFile:"+keyFile);
                    isAuthenticated = conn.authenticateWithPublicKey(user, keyFile, pass);
                    printf(func,3,"public key auth for user:"+user+": end with :"+isAuthenticated);
                } catch(Exception e) {
                    System.out.println("key error "+e.getMessage());
                    printf(func,1,"ERROR: public key authentication:"+e.getMessage());
                    keyFile=null;
                }    
                printf(func,2,"Public Key Auth successfully:"+isAuthenticated);    
            }
            if ( ! isAuthenticated ) { 
                if  ( keyFile == null && conn.isAuthMethodAvailable(user, "password")) {
                        printf(func,3,"user/pass auth");
                        try {
                            isAuthenticated = conn.authenticateWithPassword(user, pass);
                        } catch(Exception e) {
                            printf(func,1,"ERROR: user/password authentication fails with:"+e.getMessage());
                        }    
                } else if ( conn.isAuthMethodAvailable(user, "publickey") ) {
                    try {
                        printf(func,3,"public key auth:"+user+":"+((pass.isEmpty())?"empty":"KeyPASS SET")+":  keyFile:"+keyFile);
                        isAuthenticated = conn.authenticateWithPublicKey(user, keyFile, pass);
                        printf(func,3,"public key auth for user:"+user+": end with"+isAuthenticated);
                    } catch(Exception e) {
                        System.out.println("key error "+e.getMessage());
                        printf(func,1,"ERROR: public key authentication:"+e.getMessage());
                    }    
                    printf(func,2,"Public Key Auth successfully:"+isAuthenticated);    
                } else if (conn.isAuthMethodAvailable(user, "keyboard-interactive") ) {
                    log("ERROR: authentication possible only in interactive mode - change the sshd_conf with PasswordAuthentication yes or use public key");
                    _success=false;
                    return _success;
                } 
            }    
            
            if (!isAuthenticated || ! conn.isAuthenticationComplete() )  { 
                printf(func,1,"ssh authentication fails to "+getHost()+" user:"+user+":  pass:"+pass+":");
                throw new java.io.IOException("Authentication fails");
            }
            
            printf(func,2,"user "+user+" is authenticated");
            
            login=true;
            
        } catch(Exception io) {
             printf(func,1,"ERROR: "+io.getMessage()); 
             setClosed();
             _success=false;
             return _success; 
        }
        
        _success=isLogin();
        return _success; 
    }
    
    String lastLine="\\$ ";
    
    
    
    public boolean setShell(String shell) throws IOException {
        boolean b=true;    
        if ( isLogin() ) { setSession(); }
        try {  sess.requestPTY(shell); } catch (java.io.IOException io) { b=false; }
        try {  sess.startShell();      } catch (java.io.IOException io) { b=false; } finally { return b; }
    }
    
    public Connection getConnection() throws IOException {
        if ( conn == null ) { login(); }
        return conn;
    }
    
    public void setSubConnect( ) throws IOException{
         Connection con = getConnection();
         
         
    }
    
    public SCPClient getSCPClient() throws IOException { 
        Connection con = getConnection();
        if ( con.isAuthenticationComplete() ) {
             return new SCPClient(con); 
        } else {
             return null;
        }
    }
    public void scpTo( String    localFile                  ) throws IOException { scpTo(new String[]{localFile},System.getProperty("user.dir")); }
    public void scpTo( String[] localFiles                  ) throws IOException { scpTo(localFiles             ,System.getProperty("user.dir")); }
    public void scpTo( String    localFile, String remoteDir) throws IOException { scpTo(new String[]{localFile},remoteDir); }
    public void scpTo( String[] localFiles, String remoteDir) throws IOException {
        SCPClient scp = getSCPClient();
        if ( scp != null ) {
             scp.put(localFiles, remoteDir);
        } else {
            throw new IOException("no scp channel to send "+localFiles.length+" file[s]");
        }     
    }
    
    public void scpFrom( String[] remoteFiles                 ) throws IOException { scpFrom( remoteFiles,              System.getProperty("user.dir")); }
    public void scpFrom( String   remoteFile                  ) throws IOException { scpFrom( new String[]{remoteFile}, System.getProperty("user.dir")); }
    public void scpFrom( String   remoteFile,  String localDir) throws IOException { scpFrom( new String[]{remoteFile}, localDir); }
    public void scpFrom( String[] remoteFiles, String localDir) throws IOException {
        SCPClient scp = getSCPClient();
        if ( scp != null ) {
            scp.get(remoteFiles, localDir);
        } else {
            throw new IOException("no scp channel to receive "+remoteFiles.length+" file[s]");
        }
    }
    
    public String getHost(){ return host+((port==22)?"":":"+port); }
    boolean login = false;
    public boolean isLogin() { return login; }
    
    public StringBuilder sendSingleCommand(String comm) {
        final String func=getFunc("sendSingleCommand(String comm)"); //debug=3;
        printf(func,2,"ask for user "+ user + " login ("+debug+")");
        if ( ! isLogin()  || conn==null || ( conn != null && ! conn.isAuthenticationComplete()) ) { 
            printf(func,3,"call login");
            login();
            printf(func,3,"login return:"+isLogin());
        }
        printf(func,2,"auth return user  "+ user + " login ("+isLogin()+")");
        
        StringBuilder sw=new StringBuilder();
        if ( isLogin() ) {
            printf(func,3,"login completed - set session login:"+isLogin()+":  for command:"+comm+":");
            /*try { 
                setSession(); 
            } catch(Exception io) { 
                printf(func,1,"user "+ user + " session creation fails with:"+io.getMessage());
            }*/
            printf(func,2,"user "+ user + " is logged in - send =>|"+comm+"<=");
            if ( send(comm+"\n") ) {
               sw.append(getFullResponse().toString().replaceAll("\\e\\[[\\d;]*[^\\d;]", ""));
               _success=true;
            } else { 
                printf(func,2,"ERROR - sending command :"+comm+":");
                sw.append("ERROR: Could not send command:").append(comm); 
                _success=false;
            }
            setClosed(); 
            try { conn.close(); } catch(Exception e){ } finally{ conn=null; }
            return sw;
        } else { 
            printf(func,2,"ERROR - login failed:");
            setClosed();
            _success=false;
            return new StringBuilder("ERROR: Authentication not successfully"); 
        }
        
    }
    
    public boolean isValid() { return _success; }
    public boolean send(String s) {
        final String func=getFunc("send(String s)");
        printf(func,1,"set session - in - if outw==null : "+(outw==null));
            
        if ( ! isLogin() ) { return false; }
        printf(func,3,"send now ");
        try {
            printf(func,1,"set session if outw==null : "+(outw==null));
            if (outw == null) { 
                printf(func,3,"set Session now");
                setSession();
                printf(func,3,"set Session after");
            }
            printf(func,2,"set session - after - if outw==null : "+(outw==null));
            
            outw.write(s);
            outw.flush();
            return true;
        } catch(Exception ex) {
            printf(func,1,"ERROR - send fails with:"+ex.getMessage());
            return false;
        }
    }
    
    //public TerminalDialog getFrame() { return ct.getTerminalDialog(); }
    
    public StringBuilder stdoutReceived() {
        StringBuilder sw=new StringBuilder();
        byte[] b = new byte[4096];
            try { 
                while( in.available() > 0) {
                    int j= in.available(); 
                    if (j > 4095) { j=4095;}
                    in.read(b, 0, j);
                    for ( int i=0; i<j; i++ ) { sw.append((char) b[i]); }
                }
            }catch(IOException io) {}  
         return sw;       
    }
    
    public StringBuilder stderrReceived() {
        StringBuilder sw=new StringBuilder();
        byte[] b = new byte[4096];
            try { 
                while( err.available() > 0) {
                    int j= err.available(); 
                    if (j > 4095) { j=4095;}
                    err.read(b, 0, j);
                    for ( int i=0; i<j; i++ ) { sw.append((char) b[i]); }
                }
            }catch(IOException io) {}  
         return sw;       
    }
    
    public void setProxy() {
        this.proxy=null;
        return;
        /*String ho=""; int po=3180; String u=null; String p=null;
        if ( System.getProperty("https.proxyHost") != null ) {
            ho=System.getProperty("https.proxyHost");
            try { po=Integer.parseInt( System.getProperty("https.proxyPort") ); }catch(Exception e) { po=3180; }
            u=System.getProperty("http.proxyUsername");
            p=System.getProperty("http.proxyPassword");
        } else if ( System.getProperty("http.proxyHost") != null ) {
            ho=System.getProperty("http.proxyHost");
            try { po=Integer.parseInt( System.getProperty("http.proxyPort") ); }catch(Exception e) { po=3180; }
            u=System.getProperty("http.proxyUsername");
            p=System.getProperty("http.proxyPassword");
        }
        if ( ho != null && !ho.isEmpty()) { setProxy(ho,po,u,p); }
        */
    }
    
    private HTTPProxyData proxy=null;
    public void setProxy(String proxyHost, int proxyPort) { setProxy(proxyHost,proxyPort,null,null);}
    public void setProxy(String proxyHost, int proxyPort, String proxyUser, String proxyPass) {
        if ( host.toLowerCase().matches("localhost") || host.matches("127.0.0.1") || host.matches("::1")) { proxy=null; return; }
        if ( proxyUser ==null || proxyUser.isEmpty() ) {
            proxy=new HTTPProxyData(proxyHost, proxyPort);
        } else {
            proxy=new HTTPProxyData(proxyHost, proxyPort, proxyUser, proxyPass);
        } 
        
    }
    
    public StringBuilder getFullResponse() {
        final String func="SSHshell::getFullResponse() - ";
        StringBuilder sw=new StringBuilder();
        Pattern pa = Pattern.compile(lastLine);
        if ( isLogin() ) {
            int len=sw.length();
            boolean notComplete=false;
            while(! notComplete) {
                sw.append(stderrReceived().toString());
                sw.append(stdoutReceived().toString());
                if ( len == sw.length()) {
                     sleep(300);
                } else {                    
                    String[] sp = sw.toString().replaceAll("\\e\\[[\\d;]*[^\\d;]", "").split("\n");
                    if ( sp[ sp.length-1].equals(lastLine)) { notComplete=true; }
                    if (debug >0) log(func+"complete:"+notComplete+"  lastLine|"+lastLine+"|"+sp[ sp.length-1]+"|");
                }
            }    
        } else {
            sw.append("user not logged in\n");
        }
        return sw;
    }
    
    @Override
    public void run() {
        setRunning();
        while( ! isClosed() ) {
            sleep(300);
        }
        setRunning();
    }
    
    private String scom="ssh";
    public boolean isSSHShell() { return (scom.matches("ssh")); }
    
    private StringBuilder sCommand=null;
    public String getCommand() {  return (sCommand == null)?"":sCommand.toString() ; }
    public StringBuilder sendSingleCommand(           ) throws IOException{ return sendSingleCommand( sCommand.toString() ); } 
    public StringBuilder sendSingleCommand(byte[] send) throws IOException{ return sendSingleCommand( new String(send)         ); } 
    
    public static void main(String[] args) throws IOException {
           SSHshell ssh = getInstance(args);       
           System.out.println(ssh.sendSingleCommand(ssh.sCommand.toString()));
    }
    
    public static SSHshell getInstance(String[] args) {
           final String func="SSHshell::getInstance(String[] args) - ";
           //Crypt crypt=new Crypt();
           String  ho = "localhost";  int  po = 22;  int debug=0;  File kFile=null;
           String u=System.getProperty("user.name");  String p=""; StringBuilder comm = new StringBuilder();
           String conf=System.getProperty("user.dir")+File.separator+"config";
           String scom="ssh";
           SecFile fsec = null;
           if ( args.length > 0 ) {
                    for(int i=0; i<args.length; i++) {
                        if ( debug > 0 ) {
                            System.out.println("DEBUG[1/"+debug+"] "+func+"parse args["+i+"/"+args.length+"]="+args[i]);
                        }
                        if      ( args[i].startsWith("host=") || args[i].matches("-h") ) { ho=args[i].substring("host=".length());}
                        else if ( args[i].matches("-d")      ) { SSHshell.debug++; debug++; }
                        else if ( args[i].startsWith("user=")  ) { u=args[i].substring("user=".length());}
                        else if ( args[i].matches("-u")        ) { u=args[++i];}
                        else if ( args[i].startsWith("pass=")  ) { p=args[i].substring("pass=".length());}
                        else if ( args[i].matches("-pass")     ) { p=args[++i]; }
                        else if ( args[i].startsWith("port=")) { po= Integer.parseInt( args[i].substring("port=".length()) ); }
                        else if ( args[i].matches("-p")      ) { po= Integer.parseInt( args[++i] ); }
                        else if ( args[i].startsWith("dir=") ) { conf=  ( new ReadDir( args[i].substring("dir=".length()) )).getFQDNDirName(); }
                        else if ( args[i].matches("-j") ) { 
                                    SecFile f=new SecFile(args[++i]); 
                                    p = f.readOut().toString();
                        } 
                        else if (args[i].matches("-key") ) { ReadFile rf = new ReadFile(args[++i]); if ( rf.isReadableFile()) { kFile=new File(rf.getFQDNFileName()); } }
                        else if (args[i].matches("-help")) {  String prog = System.getProperty("prog");
                                                              System.out.println( ( (prog==null)?"":prog ) +usage()); 
                                                              return null;
                        }
                        else if (args[i].matches("-conn") ) {
                             fsec = new SecFile(args[++i]);
                             /*Properties prop = fsec.getProperties();
                             //System.out.println("conn: "+fsec.getFQDNFileName()+"  read:"+fsec.isReadableFile() + " all:"+prop+ " check:"+prop.containsKey("HOST"));
                             if ( prop.containsKey("HOST") ) {  ho=prop.getProperty("HOST");
                                 //System.out.println("comn: Host:"+ ho);
                             }
                             if ( prop.containsKey("USER") ) {  u=prop.getProperty("USER"); 
                                 //System.out.println("conn: User:"+u);
                             }
                             if ( prop.containsKey("KEY")  ) {  ReadFile rf = new ReadFile( prop.getProperty("KEY") ); 
                                 //System.out.println("conn: keyfile:"+rf.getFQDNFileName()+" read:"+rf.isReadableFile() );
                                                             if ( rf.isReadableFile() ) { kFile=new File(rf.getFQDNFileName()); }
                             }
                             if ( prop.containsKey("PASS") ) { 
                                    SecFile fpass = new SecFile( prop.getProperty("PASS") );
                                    if ( fpass.isReadableFile() ) {
                                         p=fpass.readOut().toString();
                                    } else {
                                         p=prop.getProperty("PASS") ; 
                                    }
                                    
                             }
                             if ( prop.contains("PORT") ) { po= Integer.parseInt( prop.getProperty("PORT") ); }
                              */  
                        }
                        else { 
                            if ( comm.length() > 0 ) { comm.append(" "); }
                            comm.append(args[i]);
                        }
                    }
           } else {
               String prog = System.getProperty("prog");
               System.out.println( ( (prog==null)?"":prog ) +usage()); 
               return null;
           }
           SSHshell.confDir=conf;
           SSHshell.debug=debug;
           if ( debug > 0 ) {
                System.out.println("DEBUG[1/"+debug+"] "+func+"ssh to "+u+"@"+ho+":"+po+"  with p>|"+p+"|<  command:"+comm.toString()+":");
           }
           SSHshell ssh = (fsec == null ) ? (new SSHshell(ho,po,u,p,false)): new SSHshell(fsec);
                    ssh.debug=debug;
                    ssh.setProxy();
                    ssh.sCommand=comm;
                    ssh.keyFile=kFile;
                    
                    String[] sp = comm.toString().split(" ");
                    for(int i=0; i<sp.length; i++) {
                        if ( ! sp[i].isEmpty() ) {
                           if ( sp[i].matches("scp") || sp[i].matches("sftp") ) { scom=sp[i]; }
                        }
                    }
                    printf(func,3,"scom:"+scom);
                    ssh.scom=scom;
           return ssh;
    }
    
    public static String usage() {
        StringBuilder sw = new StringBuilder();
        sw.append(" [host=<host[localhost]>] [port=<port[22]>] [user=<User [account name]>]")
          .append(" [-j <password file>] [-key <key file>] <command>");
        return sw.toString();
    }
    
    @Override
    public String toString() {
        return "SSHshell to "+this.user+"@"+this.host+":"+this.port;
    }
    
    
    private class ScpChannel extends RunnableT {

        private final InputStream in;
        private final OutputStream out;

        ScpChannel(InputStream in, OutputStream out) {
            this.in=in; this.out=out;
        }
                
        @Override
        public void run() {
            setRunning();
            
            
            
            setClosed();
        }
        
    }
    
}