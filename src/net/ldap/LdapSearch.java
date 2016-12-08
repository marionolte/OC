/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.ldap;

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
import static net.ldap.LdapMain.auth;
import static net.ldap.LdapMain.filter;
import static net.ldap.LdapMain.hostname;
import static net.ldap.LdapMain.port;
import static net.ldap.LdapMain.protocol;
import static net.ldap.LdapMain.userdn;
import static net.ldap.LdapMain.userpw;

/**
 *
 * @author SuMario
 */
public class LdapSearch  extends LdapMain{
    static public LdapSearch getInstance( String protocol, String hostname, int port, String userDN, String userPWD, String filter , String auth ) throws NamingException {
        LdapSearch ls = new LdapSearch();
        log("getInstance()",3,"initalize "+name+" with :"+protocol+":"+userDN+":"+userPWD+"//"+hostname+":"+port);
        
        ls.initialize(ls,  protocol,  hostname,  port,  userDN,  userPWD,  filter ,  auth);
        
        ls.init();
        log("getInstance()",3,"return Object "+name);
        return ls;
    }
    
    static public LdapSearch getInstance() throws NamingException {
        return getInstance(protocol,hostname,port,userdn,userpw,filter,auth);
    }
    
    private LdapSearch() {  name="LdapSearch"; }
    
    static private String myusage="\nusage():\noption: [-h hostname] [-p port] [-D adminDN ] [-j passwordfile] [-b baseDN ] [-f filter]  <attribut list>\n";
    
    private byte[] cookie = null;
    
    public NamingEnumeration search(String baseDN, String filter, ArrayList attr) throws NamingException, IOException {
        if ( getLdapContext() == null ) 
            throw new LdapException("Context not initialized");
        final String func="search(String baseDN, String filter, ArrayList attr)";
        
        getLdapContext().setRequestControls(new Control[] {new PagedResultsControl( getSearchSizelimit(), Control.CRITICAL) }); 
        
        SearchControls ctls = new SearchControls();
        
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
           
        log(func,2,"run search against :"+baseDN);
        NamingEnumeration results = getLdapContext().search( baseDN, getEnv("java.naming.ldap.attributes.binary") , ctls);
        
        
        
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
                
        
        return results;
        
    }
    
    
    public void printResults(NamingEnumeration namEnum ) throws NamingException {
        while (namEnum != null && namEnum.hasMore()) {
               SearchResult entry = (SearchResult) namEnum.next();
               System.out.println("dn: "+entry.getNameInNamespace() );
               
               Attributes attr = entry.getAttributes();
               NamingEnumeration en = attr.getAll();
               while(en!= null && en.hasMore() ) {
                   Attribute at = (Attribute) en.next();
                   String sp[]  = at.toString().substring(at.getID().length()+1).split(",");
                   for (int i=0; i<sp.length; i++) {
                      System.out.println(at.getID()+":"+sp[i]);
                   }
               }
               System.out.println("");
               //NamingEnumeration en=at.getIDs();
               ///for (int i=0; i<at.size(); i++ ) {
               //    System.out.println("at("+i+"):"+(String) at.get(i));
               //}
               
        }
    }
    
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
