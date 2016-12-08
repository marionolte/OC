/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import general.Version;
import io.crypt.Crypt;
import io.file.ReadFile;
import io.file.WriteFile;
import java.io.ByteArrayInputStream;
import java.util.Properties;

/**
 *
 * @author SuMario
 */
public class SSHpass extends Version{
    private int exit=255;
    private SSHshell ssh=null;
    
    public boolean connect() { return connect(host,port,user,pass); }
    public boolean connect(String host, int port, String user, String pass) {
        if ( ssh != null ) {}  //disconnect 
        
        (ssh = new SSHshell(host,port,user,pass,false)).start();
        return ssh.login();
    }
    
    public static void main(String[] args) {
        SSHpass s=null;
        try {
            s = new SSHpass(args);
            if ( s.prescript != null && ! s.prescript.isEmpty() ) {
                
            }
            s.connect();
            if ( s.script     != null && ! s.script.isEmpty() ) {
                s.ssh.send(s.script);
            }
            if ( s.postscript != null && ! s.postscript.isEmpty() ) {
                
            }
        } catch (Exception e) {
            System.out.println("ERROR: stopping with error "+e.getMessage());
            e.printStackTrace();
            SSHpass.usage();
            System.exit(-1);
        }
        System.exit(s.exit);
    }
    
    private static void usage() {
        System.out.println("sshpass -conn <connection file> -script <sciptfile> [-pre <preaction script>] [-post <postaction script>]\n"
                         + "\t\t connection file - hold the crypted account information \n"
                         + "\t\t script file \t- are the script which are runs remote\n\n"
                         + "\t\t preaction  script \t- are a local script, which runs before the script on this system\n"
                         + "\t\t postaction script \t- are a local script, which runs after the script on this system\n");
        
    }
    private String script;
    private String prescript;
    private String postscript;
    private Properties prop;
   
    private String host="";
    private int    port=-1;
    private String user="";
    private String pass="";
    
    private SSHpass(String[] args) throws Exception {
        boolean config=false;
        Crypt cr=new Crypt();
        for ( int i=0; i< args.length; i++ ) {
            if ( args[i].matches("-conn") ) { 
                WriteFile f= new WriteFile(args[++i]);
                if ( ! f.isReadableFile() ) { throw new RuntimeException(f.getFileName()+" is not a readable file"); }
                StringBuilder sw=new StringBuilder(f.readOut().toString().trim());
                if ( sw.capacity() > 0 && ! sw.toString().endsWith("=") ) {
                        f.delete();
                        f.append( cr.getCrypted(sw.toString()),true);
                } else {
                        sw.replace(0, sw.capacity(), cr.getUnCrypted(sw.toString()));
                }
                
                printf("SSHpass","SSHpass",2,"sw:"+sw.toString());
                prop = new Properties();
                prop.load( new ByteArrayInputStream(sw.toString().getBytes("UTF-8")) );
            
                host = prop.getProperty("HOST");  if (host==null) { host="localhost"; }
                port = Integer.parseInt(prop.getProperty("PORT")); 
                user = prop.getProperty("USER");
                pass = prop.getProperty("PASS");
            }
            else if ( args[i].matches("-script") ) { this.script=((new ReadFile(args[++i])).readOut()).toString(); }
            else if ( args[i].matches("-pre")    ) { this.prescript=((new ReadFile(args[++i])).readOut()).toString(); }
            else if ( args[i].matches("-post")   ) { this.postscript=((new ReadFile(args[++i])).readOut()).toString(); }
            else if ( args[i].matches("\\-d")      ) { debug++; }
            else {
                printf("SSHpass","SSHpass",1,"unknown :"+args[i]+":");
            }
            
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
