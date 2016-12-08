/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.ldap;

import general.Version;
import io.crypt.Crypt;
import io.file.ReadFile;
import io.file.WriteFile;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;



/**
 *
 * @author SuMario
 */
abstract public class LdapMain extends Version{
    static public  int error_code=0; 
    static         Hashtable env;
    static private LdapContext ctx;
    static private String initial_context_factory="com.sun.jndi.ldap.LdapCtxFactory"; 
    static private String initial_auth="simple";
    static private LdapScope scope=LdapScope.base;
    static private int timeout=0;
    static private int size=1024;
    static private Crypt crypt = new Crypt();
    
    static public String name;
    
    static public void initialize( LdapMain ob, String protocol, String hostname, int port, String userDN, String userPWD, String filter , String auth ) throws NamingException{
    
        ob.updateEnv(Context.INITIAL_CONTEXT_FACTORY, getInitContextFactory() );
            
        if ( filter != null ) ob.updateEnv("java.naming.ldap.attributes.binary", filter);
        if ( userDN != null ) ob.updateEnv(Context.SECURITY_PRINCIPAL,           userDN);
        if ( userPWD!= null ) ob.updateEnv(Context.SECURITY_CREDENTIALS,         userPWD);

        if ( needBind( new String[] {Context.SECURITY_PRINCIPAL, Context.SECURITY_CREDENTIALS } ) ) {
            ob.updateEnv(Context.SECURITY_AUTHENTICATION, auth );
        }
        
        String pr = (protocol!=null && protocol.toLowerCase().matches("ldaps"))? "ldaps":"ldap";
        String ho = ( ( hostname!=null )? hostname:"localhost" );
        String po = (port >0 && port < 65536 )? ""+port : ( pr.matches("ldap")  )? "389":"636";
        
        ob.updateEnv(Context.PROVIDER_URL,  pr+"://" + ho + ":" + po );
        
    }
    
    static public void initialize( LdapMain ob){
        ob.updateEnv(Context.INITIAL_CONTEXT_FACTORY, getInitContextFactory() );
            
        if ( filter != null ) ob.updateEnv("java.naming.ldap.attributes.binary", filter);
        if ( userdn != null ) ob.updateEnv(Context.SECURITY_PRINCIPAL,           userdn);
        if ( userpw != null ) ob.updateEnv(Context.SECURITY_CREDENTIALS,         userpw);

        if ( needBind( new String[] {Context.SECURITY_PRINCIPAL, Context.SECURITY_CREDENTIALS } ) ) {
            ob.updateEnv(Context.SECURITY_AUTHENTICATION, auth );
        }
        
        String pr = (protocol!=null && protocol.toLowerCase().matches("ldaps"))? "ldaps":"ldap";
        String ho = ( ( hostname!=null )? hostname:"localhost" );
        String po = (port >0 && port < 65536 )? ""+port : ( pr.matches("ldap")  )? "389":"636";
        
        ob.updateEnv(Context.PROVIDER_URL,  pr+"://" + ho + ":" + po );
    }
    
    static public void setInitContextFactory(String con) { LdapMain.initial_context_factory=con;}
    static public String getInitContextFactory() { return LdapMain.initial_context_factory; }
    
    static public void   setInitAuth(String auth) { if ( auth!=null && ! auth.isEmpty() ) { LdapMain.initial_auth=auth;}}
    static public String getInitAuth(           ) { return LdapMain.initial_auth; }
    
    static public void      setInitEnv(Hashtable env ) { if (env!= null && !env.isEmpty() ) {LdapMain.env=env; } }
    static public Hashtable getInitEnv(              ) { return LdapMain.env; }
    
    static public void        setLdapContext(LdapContext ctx ) { LdapMain.ctx=ctx; }
    static public LdapContext getLdapContext(               ) { return LdapMain.ctx; }
    
    static public LdapScope getScope(String info) {
          scope = LdapScope.getId(info);          
          return scope;
    }
    
    static public void setSearchTimeout(int t){ if(t>=0) timeout=0; }
    static public int  getSearchTimeout(){ return timeout; }
    
    static public void setSearchSizelimit(int t) { if (t>=0) size=t; }
    static public int getSearchSizelimit() { return size; }
    
    static public LdapScope getMyScope() { return scope; }
    
    static public void updateEnv(String attr, String val){ 
        if (env == null ) { env = new Hashtable(); }
        env.put(attr, val);
    }
    
    static public void init() throws NamingException {
        ctx=new InitialLdapContext(env, null); 
    }
    
    static public boolean needBind( String[] list ) {
        if ( list.length > 0 ) {
            for( int i=0; i<list.length; i++ ) {
                if ( env.get( list[i] ) != null ) { return true; }
            }
        }
        return false;
    }
    
    
    static public String getEnv(String attribute) {
        if ( env == null ) { return ""; }
        String s=(String) env.get(attribute);
        return (s==null)?"":s;
    }
    
    static public ArrayList getLinesFromFile(String fname) {
        String line; 
        ArrayList a = new ArrayList();
        
        WriteFile f = new WriteFile(fname);
        if ( f.isReadableFile() ) {
             line = f.readOut().toString();
             if ( line.endsWith("=") ) {  line = crypt.getUnCrypted(line);  }
             else {
                  f.delete(); f.append(crypt.getCrypted(line));
             }
             a.add(line);
        }
        
        if ( a.size() == 0 ) {
            throw new RuntimeException("ERROR: problems to read from file "+fname);
        }
        
        return a;
    }
    
    static public void bind(String baseDN, Object o                 ) throws NamingException { ctx.bind(baseDN, o);}
    static public void bind(Name name,     Object o                 ) throws NamingException { ctx.bind(name, o); }
    static public void bind(Name name,     Object o, Attributes attr) throws NamingException { ctx.bind(name, o, attr); }
    static public void bind(String baseDN, Object o, Attributes attr) throws NamingException { ctx.bind(baseDN, o, attr); }
    
    
    
    static public    String protocol="ldap";
    static public    String hostname="localhost";
    static public    int port=389;
    static public    String userdn=null;
    static public    String userpw=null;
    static public    String filter=null;
    static public    String auth="simple";
    static public    String baseDN=null;
    static public ArrayList objList = new ArrayList();
    static public    String operationfile=null;
    static public   boolean usage=false;
    
    static Properties conn = new Properties();
    static public void scanner(String[] args,final String use) {    
        String func="scanner(Sting[] args,final String use)";
        if (args.length > 0) {
            for(int i=0; i<args.length; i++) {
                log(func,3," property:"+args[i]+":");
                if      ( args[i].matches("-h")  && args.length>i+1 ) { hostname=args[++i]; log(func,2," host:"+hostname+":"); }
                else if ( args[i].matches("-b")  && args.length>i+1 ) {   baseDN=args[++i]; log(func,2," baseDN:"+baseDN+":");}
                else if ( args[i].matches("-a")  && args.length>i+1 ) {   userdn=args[++i]; log(func,2," USER:"+userdn+":");}
                else if ( args[i].matches("-D")  && args.length>i+1 ) {   userdn=args[++i]; log(func,2," USER:"+userdn+":");}
                else if ( args[i].matches("-P")  && args.length>i+1 ) {   userpw=args[++i]; log(func,2," PASS:"+userpw+":"); }
                else if ( args[i].matches("-p")  && args.length>i+1 ) {   port = Integer.parseInt(args[++i]); log(func,2," port:"+port+":");}
                else if ( args[i].matches("-ssl")                   ) {   protocol="ldaps"; log(func,2," SSL:"+protocol+":");}
                else if ( args[i].matches("-s")  && args.length>i+1 ) {   scope=getScope(args[++i]); log(func,2," scope:"+scope+":"); }
                else if ( args[i].matches("-j")  && args.length>i+1 ) {   userpw=(String )getLinesFromFile(args[++i]).get(0); log(func,2," PASS:"+userpw+":");}
                else if ( args[i].matches("-f")  && args.length>i+1 ) {   operationfile=args[++i];  log(func,2," opFile:"+operationfile+":");}
                else if ( args[i].matches("-help") || args[i].matches("--help") ) { usage=true; printUsage(use); }
                else if ( args[i].matches("-d")                                 ) { debug++; }
                else if ( args[i].matches("-conn") && args.length>i+1 ){ WriteFile fa=new WriteFile(args[++i]); String m=fa.readOut().toString(); 
                                                                         
                                                                         if ( m.endsWith("=") ) {
                                                                            m=crypt.getUnCrypted(m);
                                                                         } else {
                                                                            if ( fa.isWriteableFile() ) {
                                                                                 String n=crypt.getCrypted(m);
                                                                                 fa.delete(); fa.append( n + ((n.endsWith("="))?"":"=")  );
                                                                            }
                                                                         }
                                                                         try { conn.load( new ByteArrayInputStream(m.getBytes("UTF-8")) ); } 
                                                                         catch(java.io.IOException io) {}
                }
                else {
                    log(func,2,"filter/objectlist for :"+args[i]+":");
                    if ( args[i].contains("=") ) {
                        filter = (filter == null)?args[i]:filter+" "+args[i];
                    }else {
                        objList.add(args[i]);
                    }
                }
            }
        } else {
            usage=true; 
        }
    }
    
    static Version v;
    
    static void printUsage(final String str) {
        System.out.println(str);
        error_code=-1;
    } 
    
    static public  void log(String method, int level, String msg ) { printf(name, level, method+" - "+msg); }
    
    static {
       name="LdapMain";
    }
}
