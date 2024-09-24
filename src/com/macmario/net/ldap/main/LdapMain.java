/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.ldap.main;

import com.macmario.general.Version;
import com.macmario.io.crypt.Crypt;
import com.macmario.io.file.ReadFile;
import com.macmario.io.file.SecFile;
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
import com.macmario.net.tcp.Host;



/**
 *
 * @author SuMario
 */
abstract public class LdapMain extends Version{
     public  int error_code=0; 
             Hashtable env;
     private LdapContext ctx;
     private String initial_context_factory="com.sun.jndi.ldap.LdapCtxFactory"; 
     private String initial_auth="simple";
     private LdapScope scope=LdapScope.base;
     private int timeout=0;
     private Crypt crypt = new Crypt();
    
     public String name;
    
     public void initialize( LdapMain ob, String protocol, String hostname, int port, String userDN, String userPWD, String filter , String auth ) throws NamingException{
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
    
     public void initialize( LdapMain ob){
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
        printf(func,3,"port:"+port);
        String po = (port >Host.getMinPort() && port < Host.getMaxPort() )? ""+port : ( pr.matches("ldap")  )? "389":"636";
        printf(func,2,"provider url =>"+pr+"://" + ho + ":" + po);
        ob.updateEnv(Context.PROVIDER_URL,  pr+"://" + ho + ":" + po );
        printf(func,4,"initialize complete");
    }
    
     public void setInitContextFactory(String con) { initial_context_factory=con;}
     public String getInitContextFactory() { return initial_context_factory; }
    
     public void   setInitAuth(String auth) { if ( auth!=null && ! auth.isEmpty() ) { initial_auth=auth;}}
     public String getInitAuth(           ) { return initial_auth; }
    
     public void      setInitEnv(Hashtable env ) { if (env!= null && !env.isEmpty() ) {this.env=env; } }
     public Hashtable getInitEnv(              ) { return env; }
    
     public void        setLdapContext(LdapContext ctx ) { ctx=ctx; }
     public LdapContext getLdapContext(               ) { return ctx; }
    
     public Hashtable getEnv() { return env; } 
    
     public String getScope() { return scope.get(); }
     public LdapScope getScope(String info) {
          scope = LdapScope.getId(info);          
          return scope;
     }
     
    
     public void setSearchTimeout(int t){ if(t>=0) timeout=0; }
     public int  getSearchTimeout(){ return timeout; }
    
     public void setSearchSize(int t){ if(t>=0) searchlimit=0; }
     public int  getSearchSize(){ return searchlimit; }
    
     
     public LdapScope getMyScope() { return scope; }
    
     public void updateEnv(String attr, String val){ 
        if (env == null ) { env = new Hashtable(); }
        env.put(attr, val);
    }
    
     public void init() throws NamingException {
        final String func=name+"::init()";
        printf(func,4,"init start");
        ctx=new InitialLdapContext(env, null);
        printf(func,4,"init complete "+ctx);
    }
    
     public boolean needBind( String[] list ) {
        if ( list.length > 0 ) {
            for( int i=0; i<list.length; i++ ) {
                if ( env.get( list[i] ) != null ) { return true; }
            }
        }
        return false;
    }
    
    
     public String getEnv(String attribute) {
        if ( env == null ) { return ""; }
        String s=(String) env.get(attribute);
        return (s==null)?"":s;
    }
    
     public ArrayList<String> getLinesFromFile(String fname) {
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
    
     public void bind(String baseDN, Object o                 ) throws NamingException { ctx.bind(baseDN, o);}
     public void bind(Name name,     Object o                 ) throws NamingException { ctx.bind(name, o); }
     public void bind(Name name,     Object o, Attributes attr) throws NamingException { ctx.bind(name, o, attr); }
     public void bind(String baseDN, Object o, Attributes attr) throws NamingException { ctx.bind(baseDN, o, attr); }
    
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
    
     public    String protocol="ldap";
     public    String hostname="localhost";
     public    int    port=389;
     public    String userdn=null;
     public    String userpw=null;
     public    String filter=null;
     public    String auth="simple";
     public    String baseDN=null;
     public ArrayList<String> objList = new ArrayList();
     public HashMap<String, HashMap<String,String> > attrList =null; //= new ArrayList();
     public    String operationfile=null;
     public   boolean usage=false;
                  int pageSize = 10;
     private      int searchlimit=1024;
     private  String  _myusage="";
     
     Properties conn = new Properties();
     public HashMap<String,String> map = new  HashMap<String,String> ();
     public void scanner(String[] args,final String use) { 
        this._myusage=use;
        String func=name+"::scanner(String[] args,final String use)";
        printf(func,3," usage |"+use+"|");
        Pattern pa = Pattern.compile("\\]|\\[|<|>");
        Matcher ma = pa.matcher(use);
        int pos=0; int found=0;
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
            if ( map.get("-w") == null ) { map.put("-w", "password");     map.put("_default_-w", "password");      }
            if ( map.get("-j") == null ) { map.put("-j", "passwordfile"); map.put("_default_-j", "passwordfile");  }
            if ( map.get("-a") == null ) { map.put("-a", "authtype");     map.put("_default_-a", "authtype");      }
            
            if ( map.get("-help") == null ) { map.put("-help", "usage");  map.put("_default_-help", "usage");      }   
            
            
            printf(func,3," new pos |"+ma.end()+"| of "+use.length());
            pos=ma.end();
        }
        printf(func,3," end map |"+map+"|");
        if (args.length > 0) {
            for(int i=0; i<args.length; i++) {
                printf(func,3," property:"+args[i]+":");
                if ( ! args[i].isEmpty() ) {
                    if ( args[i].startsWith("-") && ! args[i].equals("-d") ) { 
                       if ( map.get(args[i]) != null ) {
                            printf(func,2," map:"+args[i]+":");
                            String p=args[i];
                            String v="true";
                            if ( args.length > (i+1) && ! args[i+1].startsWith("-") ) {
                                v=args[++i];
                            }
                            if ( p.equals("-o") && ! map.get(p).equals(map.get("_default_-o"))) {
                                v=map.get(p)+"\n"+v;
                            }
                            printf(func,2," map:"+p+"="+v+":");
                            map.put(p, v); found++;
                       } else {
                           System.out.println("unknown argument "+args[i]);
                           usage=true; log(use);
                       }  
                    }
                    else if ( ! args[i].startsWith("-") ){
                       printf(func,3," add object to list:"+args[i]+":"); 
                       addAttrList(args[i]);
                       printf(func,3,"object list contains:"+getAttrList().size()+" elements");
                    }
                }   
            }
            
            if(  ! getMapValue("-help").isEmpty() || found == 0 ) {
                 usage=true; log(use);
            }
            
            printf(func,3,"map scanned:"+map);
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
                      printf(func,3,"exception "+e.getMessage());
                            port=((protocol.equals("ldaps"))?636:389);
              }                 
              scope= (( map.get("-s") != null && ! map.get("-s").isEmpty() )? getScope(map.get("-s")):LdapScope.sub);
              //System.out.println("scope="+scope);
               
              //System.out.println("userpw:"+userpw);
              if ( (userpw == null || userpw.isEmpty()) && map.get("-j") != null && ! map.get("-j").isEmpty() &&  ! map.get("-j").equals(map.get("_default_-j")) &&  (new ReadFile(map.get("-j")).isReadableFile())   ) {
                    userpw=(String )getLinesFromFile(map.get("-j")).get(0);
              }
              //System.out.println("userpw:"+userpw+"    -w:"
              //        +((map.get("-w")!=null)?map.get("-w")+":  :"+map.get("_default_-w")+":":"NULL")
              //        +"  -j:"+((map.get("-j")!=null)?map.get("-j")+":  :"+map.get("_default_-j")+":":"NULL")
              //        +"-j default:"+( map.get("-j").equals(map.get("_default_-j"))) );
              if ( map.get("-j").equals(map.get("_default_-j")) && map.get("-w") != null &&  !  map.get("-w").equals(map.get("_default_-w"))  ) { 
                  userpw=map.get("-w"); 
              }
              //System.out.println("userpw:"+userpw+":");
               
              if ( map.get("-of") != null && ! map.get("-of").isEmpty()  &&  (new ReadFile(map.get("-of")).isReadableFile())   ) {
                    operationfile=map.get("-f");
              }
              
              
              if ( map.get("-f") != null && ! map.get("-f").equals(map.get("_default_-f") )   ) {
                    filter=map.get("-f");
              }

              //System.out.println("mod -o:"+map.get("-o")+"  "+! map.get("-o").isEmpty());
              if ( map.get("-o") != null  && ! map.get("-o").isEmpty() && ! map.get("-o").equals(map.get("_default_-o"))  ) {
                  //System.out.println("mao(o):"+map.get("-o"));
                  if ( attrList == null ) { attrList = new HashMap<String, HashMap<String,String> >(); }
                  for ( String o : map.get("-o").split("\n") ) {
                        // -o <add|del|mod>:dn:attribute:value>
                        String[] sp = o.split(":");
                        sp[0]=sp[0].toLowerCase();

                        String dn  = sp[1];
                        String attr= sp[2];
                        String val = o.substring( sp[0].length()+sp[1].length()+sp[2].length()+3);

                        printf(func,3,("operator:"+sp[0]+":\ndn:"+dn+":\nattr:"+attr+":\nval:"+val+":"));

                        HashMap<String,String> m=attrList.get(dn);
                        if ( m==null ) { m=new HashMap(); }
                        int j = m.size()/2+1;
                        //System.out.println("j:"+j+"  m"+(m.size()%2+1)+"  m1:"+(m.size()/2+1) );
                        m.put("op"+j+"attr", attr+": "+ val);
                        m.put("op"+j, sp[0]);

                        attrList.put(dn, m);
                  }     
              }   
              
              
              String f=getFromMap("-pg");
              if ( ! f.isEmpty() && ! f.equals("true") ){
                    int si=this.getPageSize();
                    try {
                        printf(func,3,"check search size limit update with "+f);
                          si=Integer.parseInt(f);
                    } catch (NumberFormatException e){
                    }
                    this.setPageSize(si);
              }   
              
              f=getFromMap("-sizelimit");
              if ( ! f.isEmpty() && ! f.equals("true") ){
                    int si=this.getSearchSize();
                    try {
                        printf(func,3,"check search size limit update with "+f);
                          si=Integer.parseInt(f);
                    } catch (NumberFormatException e){
                    }
                    this.setSearchSize(si);
              }
              getScope(getFromMap("-scope")); 
           
              printf(func,3,"map:"+map);
              if ( debug > 0 ) {
                  ArrayList<String> a = getAttrList();
                  for(String s: a) {
                      printf(func,1,"attribut list contains:"+s+":   "+a.contains(s) );
                  }
              }
            
        } else {
            printf(func,1,"no arguments provided");
            usage=true; 
        }
    }
     
    public void printUsage() { log(this._myusage); }
     
     private String getFromMap(String k) {
         String f=map.get("k");
         String v=( ( f != null && ! f.isEmpty() && ! f.equals( map.get("_default_"+k) ) )?f:"");
         return v;
     }
    
     public void   setPageSize(int t) { if (t>0){ pageSize=t; }}
     public int    getPageSize() { return pageSize; }
     public String getProtocol() { protocol=(map.get("-ssl")!= null && map.get("-ssl").equals("true"))?"ldaps":"ldap"; return protocol; }
     public String getHostname() { return (map.get("-h").equals("hostname"))?hostname:map.get("-h"); }
     public int    getPort() {  
    
        String p=map.get("-p"); 
        
        try {
            int i =Integer.parseInt(p);
            if ( i > Host.getMinPort() && i <= Host.getMaxPort()) { return i;}
        } catch(Exception e) {}
        return (getProtocol().equals("ldaps"))?636:389;
    }
     public String getUserDN()   { return userdn; }
     public String getUserPass() { return userpw; }
     public String getAuth()     { return auth; }
     public String getFilter()   { return (filter!=null&& ! filter.isEmpty() )?filter:"objectclass=*"; }
    
     public ArrayList getAttrList() { return objList; }
     public void      addAttrList(String s) {  if ( ! objList.contains(s)) { objList.add(s); } }
     public String    getBaseDN()   { return baseDN; }
    
    
     public String getDefaultBaseDN() {
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
     
    public String getMapValue(String a){
        if ( a != null ) {
            if ( map.get(a) != null && ! map.get(a).equals(map.get("_default_"+a))) { return map.get(a); }
        }
        return "";
    } 
    
    static {
       //name="LdapMain";
    }
}
