/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.ssh;

import com.trilead.ssh2.Session;
import com.macmario.general.Version;
import com.macmario.io.crypt.Crypt;
import com.macmario.io.crypt.GetPassword;
import com.macmario.io.file.ReadFile;
import com.macmario.io.file.SecFile;
import static com.macmario.io.lib.IOLib.execReadToString;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Properties;

/**
 *
 * @author SuMario
 */
public class SSHpass extends Version{
    private int exit=255;
    private SSHshell ssh=null;
    private SSHshell pssh=null;
    private String rprescript="";
    private String rpostscript="";
    private String rrollback="";
    private boolean postexec;
    private boolean preexec;
    private Session session;
    
    public boolean connect() { 
        if ( proxyhost.isEmpty() || proxyport == -1 ) {
            if ( connect(host,port,user,pass) ) {
                session= ssh.getSession();
                return true;
            } else { return false; }
           
        } else {
            if ( proxyConnect(proxyhost,proxyport,proxyuser,proxypass) ) {
                session = pssh.getSession();
                
                
                return true;
            } else {
                return false;
            }    
        }
    }
    public boolean connect(String host, int port, String user, String pass) {
        if ( ssh != null ) {}  //disconnect 
        
        ssh = new SSHshell(host,port,user,pass,false);
        ssh.debug=debug;
        ssh.setProxy();
        ssh.setKeyFile(kFile);
        ssh.start();
        return ssh.login();
    }
    
    public boolean proxyConnect(String host, int port, String user, String pass) {
        pssh = new SSHshell(host,port,user,pass,false);
        pssh.debug=debug;
        pssh.setProxyConnect(proxytype);
        pssh.setProxy();
        pssh.setKeyFile(proxykFile);
        pssh.start();
        return pssh.login();
    }
    
    

    
    public int runScript() {
        final String func=getFunc("runScript()");
        int step=-1;
        completeOK=true;
        printf(func,2," run if set prescript:\t"+prescript);
        if ( ! prescript.isEmpty() ) {
             step=1;
             printf(func,3," run prescript:\t"+prescript);
             completeOK=runPreScript();
             printf(func,3," run prescript:\t"+prescript+" complete with "+completeOK);
             if ( ! completeOK ) { 
                 printf(func,3," run rollback:\t"+step+" ");
                 rollback(step); 
                 printf(func,1," return in step:\t"+step+" ");
                 return step; 
             }
        }
        printf(func,2," run connection");
        connect();
        if ( ! ssh.isLogin() ) {
               printf(func,1," ssh connection not established");
               if ( step >0 ) {
                   printf(func,3," run rollback:\t"+step+" ");
                   rollback(step); 
               }
               printf(func,3," return :\t0 ->"+step+" ");
               return 0;
        } else {
               printf(func,1," ssh connection established");
               step=2;
        }
        
        printf(func,2," run remote prescript :"+rprescript);
        if ( ! rprescript.isEmpty() ) {
            step=3;
            completeOK=runRemotePreScript();
            if ( ! completeOK ) { rollback(step); return step; }
        }
        printf(func,2," run remote script :"+script);
        if ( ! script.isEmpty() ) {
             step=3;
             completeOK=runWorkScript();
             if ( ! completeOK ) { rollback(step); return step; }
        }
        printf(func,2," run remote postscript :"+rpostscript);
        if ( ! rpostscript.isEmpty() ) {
             step=4;
             completeOK=runRemotePostScript();
             if ( ! completeOK ) { rollback(step); return step; }
        }
        printf(func,2," run local postscript :"+postscript);
        if ( ! postscript.isEmpty() ) {
             step=5;
             completeOK=runPostScript();
             if ( ! completeOK ) { rollback(step); return step; }
        }
        step=6;
        
        return step;
    }
    
    
    private boolean runRemotePostScript() {
    StringBuilder sw = new StringBuilder(ssh.sendSingleCommand(this.rpostscript+"\n"));
        for( String s : sw.toString().split("\n")) {
            if ( s.startsWith("ERROR") ) { return false;}
        }
        return true;
    }

    private boolean runPostScript() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private boolean runWorkScript() {
       String sep = File.separator;
       StringBuilder sw = new StringBuilder();
       if ( this.script     != null && ! script.isEmpty() ) {
            
            if ( sw.length() == 0 && ! script.contains(" ") ) {
               sw.append(ssh.sendSingleCommand(script));
            } else {
               String f =  sep+"tmp"+ sep +  GetPassword.getMediumPassword();
               ssh.send("echo '"+sw.toString()+"' > " + f+"\n");
               ssh.sendSingleCommand("chmod 755 "+f +"\n");
               
               sw= ssh.sendSingleCommand(f+"\n");
               ssh.sendSingleCommand("echo remove "+f+" && rm "+f+" 2>&1\n");
            }   
            return checkError(sw);
       } else { return false; }
      
    }

    private boolean runRemotePreScript() {
        StringBuilder sw = new StringBuilder(ssh.sendSingleCommand(this.rprescript+"\n"));
        return checkError(sw);
    }

    private boolean runPreScript() {
        return this.runLocalScript(this.prescript);
    }

    private void rollback(int step) {
        String f = "";
        switch(step) {
            
        }
    }

    private boolean runLocalScript(String sfile) {
       final String func=getFunc("runLocalScript(String sfile)");
       try {
            StringBuilder sw = new StringBuilder( execReadToString(sfile) );
            return checkError(sw);
       }catch(java.io.IOException io) {
            printf(func,1,"prescript runs in error "+io.getMessage(),io);
            return false;  
       }     
       
    } 
    
    private boolean checkError(StringBuilder sw) {
        for( String s : sw.toString().split("\n")) {
            if ( s.startsWith("ERROR") ) { return false;}
        }
        return true;
}   

    public int runScript1() {
        final String func=getFunc("runScript()");
       completeOK=false; 
       StringBuilder asw = new StringBuilder();
       if ( prescript != null && ! prescript.isEmpty() && script != null && ! script.isEmpty() ) {
           printf(func,3,"run pre script:"+prescript);
           try {
                asw.append(execReadToString("cat "+script+" | "+ prescript));
           } catch (Exception e) {
                printf(func,1,"prescript runs in error "+e.getMessage(),e);
                return 1;
           }  
       }
       
       connect();
       String sep = File.separator;
       StringBuilder sw = new StringBuilder();
       if ( script     != null && ! script.isEmpty() ) {
            
            if ( asw.length() == 0 && ! script.contains(" ") ) {
               sw.append(ssh.sendSingleCommand(script));
            } else {
               String f =  sep+"tmp"+ sep +  GetPassword.getMediumPassword();
               ssh.send("echo '"+asw.toString()+"' > " + f+"\n");
               ssh.sendSingleCommand("chmod 755 "+f +"\n");
               
               sw= ssh.sendSingleCommand(f+"\n");
               ssh.sendSingleCommand("echo remove "+f+" && rm "+f+" 2>&1\n");
            }   
            
       }
       if ( postscript != null && ! postscript.isEmpty() ) {
            printf(func,3,"run post script:"+postscript);
            try {
                execReadToString(postscript);
            } catch (Exception e) {
                printf(func,1,"postscript runs in error "+e.getMessage(),e);
                return 101;
            }    
            
       }
       completeOK=true; 
       return 0;
    }
    
    private boolean completeOK=false;
    public boolean isValid() { return this.completeOK; }
    
    public static void main(String[] args) {
        SSHpass s=null;
        try {
            s = getInstance(args);
            s.runScript();
        } catch (Exception e) {
            System.out.println("ERROR: stopping with error "+e.getMessage());
            e.printStackTrace();
            SSHpass.usage();
            System.exit(-1);
        }
        System.exit(s.exit);
    }
    
    final public static String myusage="usage() sshpass\n [-conn <connection file>] [-script <sciptfile>] [-pre <preaction script>] [-rpre <remote preaction script>] [-post <postaction script>] [-rpost <remote postaction script>] [-roll <local rollback script>] [-rroll <local rollback script>]\n";
    
    private static void usage() {
        System.out.println(myusage
                         + "\t\t connection file \t- hold the crypted account information \n"
                         + "\t\t\t\t\tformat:\n"
                         + "\t\t\t\t\t\tUSER=<USERNAME>\n"
                         + "\t\t\t\t\t\tPASS=<PASSWORD or PASSWORDFILE>\n"
                         + "\t\t\t\t\t\tHOST=<HOSTNAME>\n"
                         + "\t\t\t\t\t\tPORT=<PORT>\n"
                         + "\t\t\t\t\t\tKEY=<ssh keyfile>\n\n"
                         + "\t\t\t\t\t\tPROXYUSER=<USERNAME>\n"
                         + "\t\t\t\t\t\tPROXYPASS=<PASSWORD or PASSWORDFILE>\n"
                         + "\t\t\t\t\t\tPROXYHOST=<HOSTNAME>\n"
                         + "\t\t\t\t\t\tPROXYPORT=<PORT>\n"
                         + "\t\t\t\t\t\tPROXYKEY=<ssh keyfile>\n\n"   
                         + "\t\t\t\t\t\tPROXYTYPE=<HTTP|Exec>\n"        
                         + "\t\t script file \t\t- are the script which are runs remote\n"
                         + "\t\t preaction  script \t- are a local script, which runs before the script runs on local system\n"
                         + "\t\t\t\t\t   (ends the scriptname the script file will regenerated with the pre scripti - only local)\n"
                         + "\t\t remote preaction  script - are a remote  tool/script, which runs before the script runs on the remote system\n"
                         + "\t\t postaction script \t- are a local script, which runs after the script on local system\n"
                         + "\t\t remote postaction script - are a remote tool/script, which runs after the script runs on the remote system\n"
                         + "\t\t\t\t\t   (ends the template than the outcome of the script will passt throw the postfile - only local)\n"
                         + "\t\t rollback script \t- are called, when a outcome line starts with the key ERROR: \n"
                         + "\t\t remote rollback script - are called, when a outcome line starts with the key ERROR: and the script/remote prescript are called\n"
                                 
                         + "\t\t\t\t\t"
                       );
    }
    
    private String script="";
    private String prescript="";
    private String postscript="";
    private String rollback="";
    private Properties prop;
   
    private String host="";
    private int    port=-1;
    private String user="";
    private String pass="";
    private File kFile=null;
    
    private String proxyhost="";
    private int    proxyport=-1;
    private String proxyuser="";
    private String proxypass="";
    private String proxykFile=null;
    private String proxytype="EXEC";
    
    public static SSHpass getInstance(String[] args) {
        try {
            SSHpass sshp = new SSHpass(args); 

            return sshp;
        } catch (Exception e)  {
            return null;
        }   
    }
    
    private SSHpass(String[] args) throws Exception {
        final String func=getFunc("SSHpass(String[] args)");
        boolean config=false;
        Crypt cr=new Crypt();
        HashMap<String, String> imap = com.macmario.io.lib.IOLib.scanner(args, myusage);
        
        printf(func,3,"ask for help:"+com.macmario.io.lib.IOLib.getMappedValue("--help",imap)+":"+imap.get("_default_--help")+":  ->"+( ! com.macmario.io.lib.IOLib.getMappedValue("--help",imap).isEmpty() ));
        if ( ! com.macmario.io.lib.IOLib.getMappedValue("--help",imap).isEmpty() ) {
            printf(func,1,"usage");
            usage();
            return;
        }   
        
        printf(func,3,"check for connection:"+( ! com.macmario.io.lib.IOLib.getMappedValue("-conn",imap).isEmpty()  ) );
        if ( ! com.macmario.io.lib.IOLib.getMappedValue("-conn",imap).isEmpty()  ){
            SecFile f= new SecFile(imap.get("-conn"));
            if ( ! f.isReadableFile() ) { throw new RuntimeException(f.getFileName()+" is not a readable file"); }
            StringBuilder sw=new StringBuilder(f.readOut().toString().trim());
            printf("SSHpass","SSHpass",2,"sw:"+sw.toString());
            prop = new Properties();
            prop.load( new ByteArrayInputStream(sw.toString().getBytes("UTF-8")) );
                //System.out.println("host:"+prop.getProperty("HOST"));
            host = prop.getProperty("HOST");  if (host==null) { host="localhost"; }
            try{ port=Integer.parseInt(prop.getProperty("PORT")); }catch(Exception e) { port = 22; }
            user = prop.getProperty("USER");  if ( user == null ) { user=System.getProperty("user.name"); }
            pass = prop.getProperty("PASS");
            if ( pass != null ) {
                    ReadFile fp = new ReadFile(pass);
                    if ( fp.isReadableFile() ) {
                         f = new SecFile(pass);
                         pass=f.readOut().toString();
                    }
            } else { pass=""; }
            if ( prop.getProperty("KEY") != null ) {
                    ReadFile fp = new ReadFile(prop.getProperty("KEY"));
                    if ( fp.isReadableFile() ) {
                            kFile=fp.getFile();
                    } else {
                        System.out.println("ERROR: key file "+prop.getProperty("KEY")+" is not a readable file");
                        throw new RuntimeException("key file "+prop.getProperty("KEY")+" is not a readable file");
                    }        
            }
            proxyhost = prop.getProperty("PROXYHOST");  if (host==null) { host=""; }
            try{ proxyport=Integer.parseInt(prop.getProperty("PROXYPORT")); }catch(Exception e) { proxyport = -1; }
            proxyuser = prop.getProperty("PROXYUSER");  if ( proxyuser == null ) { proxyuser=System.getProperty("user.name"); }
            proxypass = prop.getProperty("PROXYPASS");
            if ( proxypass != null ) {
                    ReadFile fp = new ReadFile(proxypass);
                    if ( fp.isReadableFile() ) {
                         f = new SecFile(proxypass);
                         proxypass=f.readOut().toString();
                    }
            } else { proxypass=""; }
            if ( prop.getProperty("PROXYKEY") != null ) {
                    proxykFile=prop.getProperty("PROXYKEY");
            }
            if ( prop.getProperty("PROXYTYPE") != null ) {
                    proxytype=( prop.getProperty("PROXYTYPE") != null && prop.getProperty("PROXYTYPE").toUpperCase().equals("HTTP") )? "HTTP":"EXEC";                    
            }
        } else {
            System.out.println("ERROR: key file "+prop.getProperty("KEY")+" is not a readable file");
            throw new RuntimeException("key file "+prop.getProperty("KEY")+" is not a readable file");
        }    
    
        
        if ( ! imap.get("-script").equals(imap.get("_default_-script") ) ) { 
            this.script=imap.get("-script");
            //        ((new ReadFile(args[++i])).readOut()).toString(); 
        } else {
            System.out.println("WARNING: no script to execute - return");
            return;
        }
        
        if ( ! imap.get("-pre").equals(imap.get("_default:-pre") ) ) { 
           this.prescript=imap.get("-pre");
           this.preexec=true;
           if ( this.prescript.equals("\\.template")) {
                this.prescript=(new ReadFile(this.prescript).readOut()).toString(); 
                this.preexec=false;
           } 
        }
        
        if ( ! imap.get("-rpre").equals(imap.get("_default:-rpre") ) ) { 
           this.rprescript=imap.get("-rpre");
        }
        
        if ( ! imap.get("-post").equals(imap.get("_default:-post") ) ) { 
           this.postscript=imap.get("-post");
           this.postexec=true;
           if ( this.postscript.equals("\\.template")) {
                this.postscript=(new ReadFile(this.postscript).readOut()).toString(); 
                this.postexec=false;
           } 
        }
        
        if ( ! imap.get("-rpost").equals(imap.get("_default:-rpost") ) ) { 
           this.rprescript=imap.get("-rpost");
        }
         
        if ( ! imap.get("-roll").equals(imap.get("_default:-roll") ) ) { 
           this.rollback=imap.get("-roll");
        }
        
        if ( ! imap.get("-rroll").equals(imap.get("_default:-rroll") ) ) { 
           this.rrollback=imap.get("-rroll");
        }
        
        
        printf("SSHpass","SSHpass",3," host:"+host+": port:"+port+": user:"+user+": pass:"+( (pass!=null && !pass.isEmpty() )?"SET":"NULL")+":"+pass+":");
        
        if (    user != null && ! user.isEmpty() 
             && pass != null && ! pass.isEmpty()
             && host != null && ! host.isEmpty() 
           ) { config=true; } 
        
        printf("SSHpass","SSHpass",3, (config)?"configured":"fail for host:"+host+": port:"+port+": user:"+user+": pass:"+( (pass!=null && !pass.isEmpty() )?"SET":"NULL")+":");
        if ( ! config ) { throw new RuntimeException("sshpass could not run without connection file");}
    }

    
    
    
}
