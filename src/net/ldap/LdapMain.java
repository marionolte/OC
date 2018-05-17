/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.ldap;

import general.Version;
import io.crypt.Crypt;
import io.file.SecFile;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
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
    
    static public ArrayList<String> getLinesFromFile(String fname) {
        String line; 
        ArrayList<String> a = new ArrayList();
        SecFile f = new SecFile(fname);
        if  ( f.isReadableFile() ) {
             for ( String s :f.readOut().toString().split("\n") ){ a.add(s); }
        }
        
        if ( a.size() == 0 ) {
            throw new RuntimeException("ERROR: problems to read from file "+fname);
        }
        if ( debug >4 ) {
            for(String t:a) {
                System.out.println("t:"+t+":");
            }
        }
        return a;
    }
    
    static public void bind(String baseDN, Object o                 ) throws NamingException { ctx.bind(baseDN, o);}
    static public void bind(Name name,     Object o                 ) throws NamingException { ctx.bind(name, o); }
    static public void bind(Name name,     Object o, Attributes attr) throws NamingException { ctx.bind(name, o, attr); }
    static public void bind(String baseDN, Object o, Attributes attr) throws NamingException { ctx.bind(baseDN, o, attr); }
    
    public String getLdapContextFactory(){
         // sun "com.sun.jndi.ldap.LdapCtxFactory"
         // ibm "com.ibm.jndi.LDAPCtxFactory";
         
         String s="com.sun.jndi.ldap.LdapCtxFactory";
         
         if (  isAIX() ) {
             s="com.ibm.jndi.LDAPCtxFactory";
         }
         
         System.setProperty("java.naming.factory.initial", s);
	 return s;
     }
     
     public String getLdapNameingFactory() {
        //env.put("java.naming.factory.url.pkgs", "com.ibm.jndi"); 
        String s="com.sun.jndi";
        if ( isAIX() ) {
            s="com.ibm.jndi";
        }
        return s;
     }
    
    static public    String protocol="ldap";
    static public    String hostname="localhost";
    static public    int    port=389;
    static public    String userdn=null;
    static public    String userpw=null;
    static public    String filter=null;
    static public    String auth="simple";
    static public    String baseDN=null;
    static public ArrayList objList = new ArrayList();
    static public HashMap<String, HashMap<String,String> > attrList =null; //= new ArrayList();
    static public    String operationfile=null;
    static public   boolean usage=false;
    static              int pageSize = 10;
     
    static Properties conn = new Properties();
    static public void scanner(String[] args,final String use) {    
        String func="scanner(Sting[] args,final String use)";
        if (args.length > 0) {
            for(int i=0; i<args.length; i++) {
                printf(func,3," property:"+args[i]+":");
                if      ( args[i].matches("-h")  && args.length>i+1 ) { hostname=args[++i]; printf(func,2," host:"+hostname+":"); }
                else if ( args[i].matches("-b")  && args.length>i+1 ) {   baseDN=args[++i]; printf(func,2," baseDN:"+baseDN+":");}
                else if ( args[i].matches("-a")  && args.length>i+1 ) {   userdn=args[++i]; printf(func,2," USER:"+userdn+":");}
                else if ( args[i].matches("-D")  && args.length>i+1 ) {   userdn=args[++i]; printf(func,2," USER:"+userdn+":");}
                else if ( args[i].matches("-P")  && args.length>i+1 ) {   userpw=args[++i]; printf(func,2," PASS:"+userpw+":"); }
                else if ( args[i].matches("-p")  && args.length>i+1 ) {   port = Integer.parseInt(args[++i]); printf(func,2," port:"+port+":");}
                else if ( args[i].matches("-ssl")                   ) {   protocol="ldaps"; printf(func,2," SSL:"+protocol+":");}
                else if ( args[i].matches("-s")  && args.length>i+1 ) {   scope=getScope(args[++i]); printf(func,2," scope:"+scope+":"); }
                else if ( args[i].matches("-j")  && args.length>i+1 ) {   userpw=(String )getLinesFromFile(args[++i]).get(0); printf(func,2," PASS:"+userpw+":");}
                else if ( args[i].matches("-f")  && args.length>i+1 ) {   operationfile=args[++i];  printf(func,2," opFile:"+operationfile+":");}
                else if ( args[i].matches("-help") || args[i].matches("--help") ) { usage=true; log(use); }
                else if ( args[i].matches("-d")                                 ) { debug++; }
                else if ( args[i].matches("-conn") && args.length>i+1 ){ SecFile fa=new SecFile(args[++i]);  
                                                                         
                                                                         try { conn.load( new ByteArrayInputStream(fa.readOut().toString().getBytes("UTF-8")) ); } 
                                                                         catch(java.io.IOException io) {
                                                                             printf(func,1,"Exception:"+io.getMessage()+" - with file"+args[i]);
                                                                         }
                }
                else if ( args[i].matches("-o")  && args.length>i+1 ) {
                    if ( attrList == null ) { attrList = new HashMap<String, HashMap<String,String> >();}
                    // -o <add|del|mod>:dn:attribute:value>
                    String[] sp = args[++i].split(":");
                    sp[0]=sp[0].toLowerCase();
                    
                    String dn  = sp[1];
                    String attr= sp[2];
                    String val = args[i].substring( sp[0].length()+sp[1].length()+sp[2].length()+3);
                    
                    printf(func,3,("operator:"+sp[0]+":\ndn:"+dn+":\nattr:"+attr+":\nval:"+val+":"));
                    
                    HashMap<String,String> m=attrList.get(dn);
                    if ( m==null ) { m=new HashMap(); }
                    int j = m.size()/2+1;
                    //System.out.println("j:"+j+"  m"+(m.size()%2+1)+"  m1:"+(m.size()/2+1) );
                    m.put("op"+j+"attr", attr+": "+ val);
                    m.put("op"+j, sp[0]);
                
                    attrList.put(dn, m);
                } 
                else {                    
                    printf(func,2,"filter/objectlist for :"+args[i]+":");
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
    
    static {
       name="LdapMain";
    }
}
