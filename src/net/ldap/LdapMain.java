/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.ldap;

import general.Version;
import io.crypt.Crypt;
import io.file.ReadFile;
import io.file.SecFile;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import net.tcp.Host;



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
        final String func=name+"::initialize( LdapMain ob, String protocol, String hostname, int port, String userDN, String userPWD, String filter , String auth )";
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
     
        printf(func,4,"initialize complete");
    }
    
    static public void initialize( LdapMain ob){
        final String func=name+";:initialize( LdapMain ob){";
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
        printf(func,4,"initialize complete");
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
        final String func=name+"::init()";
        printf(func,4,"init start");
        ctx=new InitialLdapContext(env, null);
        printf(func,4,"init complete "+ctx);
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
    static HashMap<String,String> map = new  HashMap<String,String> ();
    static public void scanner(String[] args,final String use) {    
        String func=name+"::scanner(Sting[] args,final String use)";
        printf(func,3," usage |"+use+"|");
        Pattern pa = Pattern.compile("\\]|\\[|<|>");
        Matcher ma = pa.matcher(use);
        int pos=0;
        while(ma.find(pos)) {
            String msg = use.substring(pos,ma.start());
            printf(func,3," find |"+msg+"| pos:"+pos+" to:"+ma.start()+" "+msg.indexOf(" ") );
            if ( msg.indexOf(" ") > 0 ) {
                String v="true";
                String[] sp = msg.split(" ");
                printf(func,3," sp[0] |"+sp[0]+"|");
                if ( sp.length>0 && sp[0].startsWith("-") ) {
                    if ( sp.length > 1 ) { 
                        v=use.substring(pos,ma.start()).substring(sp[0].length()+1); 
                    }
                    printf(func,2," save |"+sp[0]+"="+v+"|");
                    map.put(sp[0], v);
                    map.put("_default_"+sp[0], v);
                }
            } else {
                printf(func,3," msg without spaces |"+msg+"|");
                if ( msg.startsWith("-") ) {
                    map.put(msg, "false");
                }else if ( msg.equals("objectlist")) {
                    map.put(msg, "");
                }
            } 
            printf(func,3," new pos |"+ma.end()+"| of "+use.length());
            pos=ma.end();
        }
        printf(func,3," end map |"+map+"|");
        if (args.length > 0) {
            for(int i=0; i<args.length; i++) {
                printf(func,3," property:"+args[i]+":");
                if ( args[i].startsWith("-") && ! args[i].equals("-d") ) { 
                   if ( map.get(args[i]) != null ) {
                        printf(func,2," map:"+args[i]+":");
                        String p=args[i];
                        String v="true";
                        if ( args.length > (i+1) && ! args[i+1].startsWith("-") ) {
                            v=args[++i];
                        }
                        printf(func,2," map:"+p+"="+v+":");
                        map.put(p, v);
                   } else {
                       usage=true; log(use);
                   }  
                }
                else if ( ! args[i].startsWith("-") ){
                   printf(func,2," add object List:"+args[i]+":"); 
                   objList.add(args[i]);  
                }
            }
            hostname=(map.get("-h")==null || map.get("-h").equals(map.get("_default_-h")))?"localhost":map.get("-h");
              baseDN=(map.get("-b")==null || map.get("-b").equals(map.get("_default_-b")))?getDefaultBaseDN():map.get("-b");
              userdn=(map.get("-D") != null ) ?
                         (map.get("-D").equals(map.get("_default_-D")) )?"cn=admin":map.get("-D")
                      :
                         (map.get("-a")==null || map.get("-a").equals(map.get("_default_-a")) )?"cn=admin":map.get("-a")
                      ;
              userpw=(map.get("-P")==null || map.get("-P") !=null && ! map.get("-P").equals("password") ) ? map.get("-P"):"";
              protocol=( map.get("-ssl") != null && map.get("-ssl").equals("true") )?"ldaps":"ldap";
              try {
                            port= Integer.parseInt( map.get("-p") );
               }catch(Exception e) {      
                            port=((protocol.equals("ldaps"))?636:389);
              }        
              scope= (( map.get("-s") != null && ! map.get("-s").isEmpty() )? getScope(map.get("-s")):LdapScope.sub);
               
              if ( map.get("-j") != null && ! map.get("-j").isEmpty() &&  ! map.get("-j").equals("_default_-j") &&  (new ReadFile(map.get("-j")).isReadableFile())   ) {
                    userpw=(String )getLinesFromFile(map.get("-j")).get(0);
              }
               
              if ( map.get("-of") != null && ! map.get("-of").isEmpty()  &&  (new ReadFile(map.get("-of")).isReadableFile())   ) {
                    operationfile=map.get("-f");
              }
              
              if ( map.get("-f") != null && ! map.get("-f").equals(map.get("_default_-f") )   ) {
                    filter=map.get("-f");
              }

                                 
               
            printf(func,2,"map:"+map);
            
            /*for(int i=0; i<args.length; i++) {      
                if      ( args[i].matches("-h")  && args.length>i+1 ) { hostname=args[++i]; printf(func,2," host:"+hostname+":"); }
                else if ( args[i].matches("-b")  && args.length>i+1 ) {   baseDN=args[++i]; printf(func,2," baseDN:"+baseDN+":"); }
                else if ( args[i].matches("-a")  && args.length>i+1 ) {   userdn=args[++i]; printf(func,2," USER:"+userdn+":");   }
                else if ( args[i].matches("-D")  && args.length>i+1 ) {   userdn=args[++i]; printf(func,2," USER:"+userdn+":");}
                else if ( args[i].matches("-P")  && args.length>i+1 ) {   userpw=args[++i]; printf(func,2," PASS:"+userpw+":"); }
                else if ( args[i].matches("-p")  && args.length>i+1 ) {   port = Integer.parseInt(args[++i]); printf(func,2," port:"+port+":");}
                else if ( args[i].matches("-ssl")                   ) {   protocol="ldaps"; printf(func,2," SSL:"+protocol+":");}
                else if ( args[i].matches("-s")  && args.length>i+1 ) {   scope=getScope(args[++i]); printf(func,2," scope:"+scope+":"); }
                else if ( args[i].matches("-j")  && args.length>i+1 ) {   userpw=(String )getLinesFromFile(args[++i]).get(0); printf(func,2," PASS:"+userpw+":");}
                else if ( args[i].matches("-f")  && args.length>i+1 ) {   operationfile=args[++i];  printf(func,2," opFile:"+operationfile+":");}
                else if ( args[i].matches("-help") || args[i].matches("--help") ) { usage=true; log(use); }
                else if ( args[i].matches("-d")                                 ) { debug++; }
                
                if      ( args[i].matches("-d") ) { debug++; }
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
                else if ( args[i].startsWith("-") ){
                    
                }
                else {                    
                    printf(func,2,"filter/objectlist for :"+args[i]+":");
                    if ( args[i].contains("=") ) {
                        filter = (filter == null)?args[i]:filter+" "+args[i];
                    }else {
                        objList.add(args[i]);
                    }
                }
            }*/
        } else {
            usage=true; 
        }
    }
    
    static String getProtocol() { protocol=(map.get("-ssl")!= null && map.get("-ssl").equals("true"))?"ldaps":"ldap"; return protocol; }
    static String getHostname() { return (map.get("-h").equals("hostname"))?hostname:map.get("-h"); }
    static int    getPort() {  
    
        String p=map.get("-p"); 
        
        try {
            int i =Integer.parseInt(p);
            if ( i > Host.getMinPort() && i <= Host.getMaxPort()) { return i;}
        } catch(Exception e) {}
        return (getProtocol().equals("ldaps"))?636:389;
    }
    static String getUserDN()   { return userdn; }
    static String getUserPass() { return userpw; }
    static String getAuth()     { return auth; }
    static String getFilter()   { return filter; }
    
    static ArrayList getAttrList() { return objList; }
    static String    getBaseDN()   { return baseDN; }
    
    
    static String getDefaultBaseDN() {
         StringBuilder sw = new StringBuilder();
         String ho=Host.getDomainname();
         //System.out.println("domain:"+ho);
         String[] sp = ho.split("\\.");
         for ( String a : sp ) {
            if (! a.isEmpty() ) { 
                //System.out.println("sw length:"+sw.length());
                if ( sw.length() >0 ) { sw.append(","); } 
                sw.append("dc=").append(a);
            }
         }
         if ( sw.length()==0 ) { sw.append("dc=example,dc=com"); }
         //System.out.println("basedn:"+sw.toString()+":");
         return sw.toString();
    }
    
    static {
       name="LdapMain";
    }
}
