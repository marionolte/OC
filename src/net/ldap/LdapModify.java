/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.ldap;

import static general.Version.printf;
import io.file.ReadFile;
import java.io.IOException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.PagedResultsControl;
import static net.ldap.LdapMain.getEnv;
import static net.ldap.LdapMain.getLdapContext;
import static net.ldap.LdapMain.getMyScope;
import static net.ldap.LdapMain.getSearchSizelimit;
import static net.ldap.LdapMain.getSearchTimeout;

/**
 *
 * @author SuMario
 */
public class LdapModify extends LdapMain {
 
    
    public static LdapModify getInstance( String protocol, String hostname, int port, String userDN, String userPWD, String filter , String auth ) throws NamingException {
        LdapModify lm = new LdapModify();
                  lm.initialize(lm,  protocol,  hostname,  port,  userDN,  userPWD,  filter ,  auth);
                  lm.init();
        return lm;
    }
    
    public static LdapModify getInstance() throws NamingException{
        LdapModify lm = new LdapModify();
                   lm.initialize(lm);
                   lm.init();
        return lm;
    }
    static public LdapModify getInstance(String[] ar) throws NamingException {
        protocol="ldap";
        hostname="localhost";
        port=389;
        userdn="cn=admin";
        userpw="";
        filter="objectclass=*";
        auth="simple";
        scanner(ar,myusage);
        return getInstance();
    }
    
    private LdapModify() {
        name="LdapModify";
    }
    
    private String[] validate(String s) {
        String[] sp = s.split(";");
        if      ( sp[0].equalsIgnoreCase("add")    ){ sp[0]="add";    }
        else if ( sp[0].equalsIgnoreCase("delete") ){ sp[0]="delete"; }
        else if ( sp[0].equalsIgnoreCase("insert") ){ sp[0]="add";    }
        else if ( sp[0].equalsIgnoreCase("mod")    ){ sp[0]="modify"; }
        else if ( sp[0].equalsIgnoreCase("modify") ){ sp[0]="modify"; }
        else { sp[0]="unknown"; }
        
        
        return new String[]{ sp[0], sp[1], sp[2], sp[3] };
    }
    
    public void modify(String op, String dn, String attr, String val) {
        final String func=getFunc("modify(String op, String dn, String attr, String val)");
        NamingEnumeration<SearchResult> entry = null;
        boolean opdone=false;
        try { 
            entry = search(dn);
            
            while(entry.hasMore()) {
                  SearchResult sr = entry.next();
                  printf(func,2," find entry to modify ");
            }
            opdone=true;
        } 
        catch( NamingException ne ) {
            if ( ! op.matches("insert") ) { throw new LdapException(ne.getMessage()); }
        } 
        catch(IOException io ) {}
        
        if (op.matches("insert") ) {
            
        }
        
        if ( ! opdone ) { }
    }
    private NamingEnumeration<SearchResult> search(String dn) throws NamingException, IOException {
        final String func=getFunc("search(String dn)");
        printf(func,2,"run search against :"+dn);
        
        getLdapContext().setRequestControls(new Control[] {new PagedResultsControl( getSearchSizelimit(), Control.CRITICAL) }); 
        
        SearchControls ctls = new SearchControls();
                       ctls.setReturningAttributes(new String[]{"objectclass"});
        if ( getSearchTimeout() > 0)
                       ctls.setTimeLimit(getSearchTimeout());
        
        
        if ( getMyScope().equals(LdapScope.sub))
           ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        if ( getMyScope().equals(LdapScope.one))
           ctls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
        if ( getMyScope().equals(LdapScope.base))
           ctls.setSearchScope(SearchControls.OBJECT_SCOPE);
        
        
        NamingEnumeration<SearchResult> results = getLdapContext().search( dn, getEnv("java.naming.ldap.attributes.binary") , ctls);
        
        return results;
        
    }
    
    synchronized public void modify(String file) {
        ReadFile f = new ReadFile(file);
               if ( f.isReadableFile() ) {
                    for ( String s: f.readOut().toString().split("\n") ) {
                         if ( ! s.isEmpty() ) {
                            String[] sp = validate(s);
                            if ( sp[0].matches("unknown") ) {
                                throw new LdapException("unkown ldapmodify operation provided");
                            }
                            if ( sp.length == 4) {
                                modify(sp[0],sp[1],sp[2], sp[3]);
                            } else {
                                modify(sp[0],sp[1],null,null); 
                            }    
                         }
                    } 
               }
    }
    
    static private String myusage="\nusage():\noption: [-h hostname] [-p port] [-D adminDN ] [-j passwordfile] -f <file for operation>\n";
    public static void main(String[] args) throws Exception {
        scanner(args,myusage);
        
        if ( ! usage ) { 
            // LdapModify ls = getInstance(); //getInstance(protocol,hostname,port,userdn,userpw,filter,auth);
            LdapModify ls = getInstance(protocol,hostname,port,userdn,userpw,filter,auth);

            if ( ls == null ) {
                System.out.println("ERROR: doesn't create an LdapModify object");
                error_code=-1;
            } else {
               ls.modify(operationfile);
            }
        }    
        System.exit(error_code);
    }
    
}
