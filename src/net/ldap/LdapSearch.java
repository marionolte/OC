/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.ldap;

import net.ldap.main.LdapMain;
import net.ldap.main.LdapException;
import net.ldap.main.LdapScope;
import java.io.IOException;
import java.util.ArrayList;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;
import static net.ldap.main.LdapMain.auth;
import static net.ldap.main.LdapMain.filter;
import static net.ldap.main.LdapMain.hostname;
import static net.ldap.main.LdapMain.port;
import static net.ldap.main.LdapMain.protocol;
import static net.ldap.main.LdapMain.userdn;
import static net.ldap.main.LdapMain.userpw;

/**
 *
 * @author SuMario
 */
public class LdapSearch  extends LdapMain{
    
    static public LdapSearch getInstance( String protocol, String hostname, int port, String userDN, String userPWD, String filter , String auth , String baseDN) throws NamingException {
        //printf("getIN",0,"create instance");
        LdapSearch ls = new LdapSearch();
                   ls.protocol=protocol;
                   ls.port=port;
                   ls.hostname=hostname;
                   ls.userdn=userDN;
                   ls.userpw=userPWD;
                   ls.filter=filter;
                   ls.auth=auth;
                   ls.baseDN=baseDN;
        printf("getInstance()",0,"initalize "+name+" with :"+protocol+":"+userDN+":"+userPWD+"//"+hostname+":"+port+"/"+baseDN+"?"+filter);
        
        ls.initialize(ls,  protocol,  hostname,  port,  userDN,  userPWD,  filter ,  auth);
        
        ls.init();
        printf("getInstance()",3,"return Object "+name);
        return ls;
    }
    
    private boolean persist=false;
    public boolean setPersistentSearch(boolean b) { persist=b; return getPersistentSearch();}
    public boolean getPersistentSearch() {return persist; }
    
    static public LdapSearch getInstance() throws NamingException {
        return getInstance(getProtocol(),hostname,port,userdn,userpw,filter,auth,getBaseDN());
    }
    static public LdapSearch getInstance(String[] ar) throws NamingException {
        protocol="ldap";
        hostname="localhost";
        port=389;
        userdn="cn=admin";
        userpw="";
        filter="objectclass=*";
        auth="simple";
        baseDN=getDefaultBaseDN();
        scanner(ar,myusage);
        //printf("aaa",0,"user "+getUserDN()+" local:"+userpw+" pw:"+getUserPass()+":  map:"+map.get("-w"));
        //printf("aaa",0,"port "+getPort()+"   local:"+port+" port:"+getPort()+":  map:"+map.get("-p"));
        //printf("aaa",0,"filter:"+getFilter()+":");
        
        return getInstance(getProtocol(),getHostname(),getPort(),getUserDN(),getUserPass(),getFilter(),getAuth(),getBaseDN());
    }
    
    private LdapSearch() {  name="LdapSearch"; }
    
    static private String myusage="\nusage():\noption: [-h hostname] [-p port] [-D adminDN ] [-j passwordfile] [-a <simple|>] [-b baseDN ] [-f filter]  <attribut list>\n";
    
    private byte[] cookie = null;
    
    public NamingEnumeration search() throws NamingException, IOException { 
        final String func=getFunc("search()");
        printf(func,2,"like to search with:"+getFilter()+": to basedn :"+getBaseDN()+": to get attributes:"+getAttrList());
        return search(getBaseDN(),getFilter(),getAttrList());
    }
    public NamingEnumeration search(String baseDN, String filter, ArrayList attr) throws NamingException, IOException {
        if ( getLdapContext() == null ) 
            throw new LdapException("Context not initialized");
        final String func=getFunc("search(String baseDN, String filter, ArrayList attr)");
        
        getLdapContext().setRequestControls(new Control[] {new PagedResultsControl( getSearchSizelimit(), Control.CRITICAL) }); 
        
        SearchControls ctls = new SearchControls();
        
        
        printf(func,3," attr list:"+attr.size());
        if (attr.size()>0) {
            String[] attrIDs = new String[attr.size()]; // { "sn", "telephonenumber", "golfhandicap", "mail" };
            for(int i=0; i<attr.size();i++) { attrIDs[i] = (String) attr.get(i); }
            ctls.setReturningAttributes(attrIDs);
        }
        if ( getSearchTimeout() > 0)
            ctls.setTimeLimit(getSearchTimeout());
        
        
        if ( getMyScope().equals(LdapScope.sub))
           ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        if ( getMyScope().equals(LdapScope.one))
           ctls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
        if ( getMyScope().equals(LdapScope.base))
           ctls.setSearchScope(SearchControls.OBJECT_SCOPE);
           
        printf(func,2,"run search against :"+baseDN+" filter:"+getFilter()+":");
        //NamingEnumeration results = getLdapContext().search( baseDN, getEnv("java.naming.ldap.attributes.binary") , ctls);
        NamingEnumeration results = getLdapContext().search(getBaseDN(), getFilter(), ctls);
        printf(func,2,"search returns elements:"+results.hasMore());
        
        
        
        //process the returned controls to get the cookie 
        Control[] controls = getLdapContext().getResponseControls();
        if (controls != null) {
                     for (int i = 0; i < controls.length; i++) {
                           if (controls[i] instanceof PagedResultsResponseControl) {
                              PagedResultsResponseControl prrc = (PagedResultsResponseControl) controls[i];
                              cookie = prrc.getCookie();
                           }
                      }
        }  
        //setting the cookie on the context for the next page search 
        getLdapContext().setRequestControls( new Control[] { new PagedResultsControl( getSearchSizelimit(), cookie, Control.CRITICAL) });
                
        printf(func,3,"return results:"+results);
        return results;
        
    }
    
    
    public boolean printResults(NamingEnumeration namEnum ) throws NamingException {
        final String func=getFunc("printResults(NamingEnumeration namEnum)");
        boolean b=false;
        printf (func,3," print results:"+(namEnum != null && namEnum.hasMore())+" nameEnum:"+namEnum);
        while (namEnum != null && namEnum.hasMore()) {
               SearchResult entry = (SearchResult) namEnum.next();
               System.out.println("dn: "+entry.getNameInNamespace() ); b=true;
               
               Attributes attr = entry.getAttributes();
               printf(func,3,"Attributes:"+attr);
               NamingEnumeration en = attr.getAll();
               while(en!= null && en.hasMore() ) {
                   Attribute at = (Attribute) en.next();
                   printf(func,3,"Attribute:"+at);
                   String sp[]  = at.toString().substring(at.getID().length()+1).split(",");
                   for (int i=0; i<sp.length; i++) {
                       Object ob=at.get();
                       String v= ( ob instanceof byte[] )? new String((byte[]) ob ):sp[i];
                       System.out.println(at.getID()+": "+v);
                       
                   }
               }
               System.out.println("");
               //NamingEnumeration en=at.getIDs();
               ///for (int i=0; i<at.size(); i++ ) {
               //    System.out.println("at("+i+"):"+(String) at.get(i));
               //}
               
        }
        return b;
    }
    
    /*private String encryptLdapPassword(String algorithm, String _password) {
        String sEncrypted =_password;
        if ((_password != null) && (_password.length() > 0)) {
            
            boolean bMD5 = algorithm.equalsIgnoreCase("MD5");
            boolean bSHA = algorithm.equalsIgnoreCase("SHA") || algorithm.equalsIgnoreCase("SHA1") || algorithm.equalsIgnoreCase("SHA-1");
            if (bSHA || bMD5) {
                String sAlgorithm = "MD5";
                if (bSHA) {
                    sAlgorithm = "SHA";
                }
                try {
                    MessageDigest md = MessageDigest.getInstance(sAlgorithm); 
                    md.update(_password.getBytes("UTF-8"));
                    sEncrypted = "{" + sAlgorithm + "}" + (new BASE64Encoder()).encode(md.digest());
                } catch (Exception e) {
                    sEncrypted = null;
                }
            }
        }
        return sEncrypted;
    }*/
    
    public static void main(String[] args) throws Exception{
        scanner(args,myusage);
        
        if ( ! usage ) { 
            
            LdapSearch ls = getInstance(); //getInstance(protocol,hostname,port,userdn,userpw,filter,auth);

            if ( ls == null ) {
                System.out.println("ERROR: doesn't create an LdapSearch object");
                error_code=-1;
            } else {
                do {
                    ls.printResults( ls.search(baseDN, filter, objList) );

                } while ( ls.cookie != null ); 
            }
        }
        System.exit(error_code);
    }

    
}
