/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.ldap;

import static general.Version.printf;
import io.file.ReadFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;

/**
 *
 * @author SuMario
 */
public class LdapModify extends LdapMain {
 
    
    public static LdapModify getInstance( String protocol, String hostname, int port, String userDN, String userPWD, String filter , String auth , String basedn ) throws NamingException {
        LdapModify lm = new LdapModify();
                   lm.protocol=protocol;
                   lm.hostname=hostname;
                   lm.port=port;
                   lm.userdn=userDN;
                   lm.userpw=userPWD;
                   lm.filter=filter;
                   lm.auth=auth;
                   lm.baseDN=baseDN;
                   lm.initialize(lm,  protocol,  hostname,  port,  userDN,  userPWD,  filter ,  auth);
        
                   lm.init();
        return lm;
        //return getInstance(new String[] { (getProtocol().equals("ldaps"))?"-ssl":"","-h",hostname,"-p",""+port,"-D",userdn,"-w",userpw,"-f",filter,auth,"-b",basedn});
    }
    
    public static LdapModify getInstance() throws NamingException{
        /*LdapModify lm = new LdapModify();
                   lm.initialize(lm);
                   lm.init();
        return lm;*/
        return getInstance(new String[]{});
    }
    static public LdapModify getInstance(String[] ar) throws NamingException {
        protocol="ldap";
        hostname="localhost";
        port=389;
        userdn="cn=admin";
        userpw="";
        filter="objectclass=*";
        auth="simple";
        baseDN=getDefaultBaseDN();
        scanner(ar,myusage);
        return getInstance(protocol,hostname,port,userdn,userpw,filter,auth,baseDN);
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
            printf(func,3,"search dn:"+dn+": results with entry:"+entry);
            
            while(entry.hasMore()) {
                  SearchResult sr = entry.next();
                  printf(func,2," find entry to modify ");
            }
            opdone=true;
        } 
        catch( NamingException ne ) {
            printf(func,1,"ERROR: message "+ne.getMessage(),ne);
            if ( ! op.matches("insert") ) { throw new LdapException(ne.getMessage()); }
        } 
        catch(IOException io ) {
            printf(func,1,"ERROR: message "+io.getMessage(),io);
        }
        
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
    
    LdapContext ctx=null; 
    synchronized private SearchResult getDN(String dn) throws NamingException, IOException {
              String ldapURL = protocol+"://" + hostname + ":" + port;
              env.put(Context.INITIAL_CONTEXT_FACTORY, getLdapContextFactory() );
              env.put(Context.URL_PKG_PREFIXES,        getLdapNameingFactory() );
              env.put(Context.SECURITY_AUTHENTICATION, "simple");
	      if ( userdn != null ) {
                env.put(Context.SECURITY_PRINCIPAL, userdn);
              	env.put(Context.SECURITY_CREDENTIALS, userpw);
	      }
              env.put("java.naming.ldap.attributes.binary", filter);
              env.put(Context.PROVIDER_URL, ldapURL); 

              //creating the JNDI context
              ctx = new InitialLdapContext(env, null);  

              //creating the PagedResultsControl and add it to the context
              ctx.setRequestControls(new Control[] {new PagedResultsControl( pageSize, Control.CRITICAL) }); 

              NamingEnumeration results = ctx.search( baseDN, filter, new SearchControls());
              
             
     
        return (SearchResult) ((results !=null)?results.next():null);
    }
    synchronized private void modifyDN(String dn,ArrayList ar) throws InvalidNameException, NamingException, IOException {
        SearchResult entry = getDN(dn);
        
        Name name =  new CompositeName().add( ( (entry != null)? entry.getNameInNamespace():dn ) );
        
        BasicAttribute  obj = new BasicAttribute("objectclass");
        ArrayList ua  = new ArrayList();
        Hashtable map = new Hashtable();
        if ( entry == null ) {
             map.put("objectclass", obj); ua.add("objectclass");
        
            for ( int i=0; i<ar.size(); i++ ) {
                    BasicAttribute ba = (BasicAttribute) ar.get(i);
                    String[] sp =  ba.toString().split(":");
                    BasicAttribute  ob = (BasicAttribute) map.get(sp[0]);
                                    ua.add(sp[0]);
                                    ob = new BasicAttribute(sp[0]);
                                    map.put(sp[0], ob);
                                    ob.add( ba.toString().substring(sp[0].length()+2));
                    
            }
            BasicAttributes ent = new BasicAttributes();

            for ( int i=0; i<ua.size(); i++ ) {
                        final String a = (String) ua.get(i);
                        ent.put( (BasicAttribute) map.get(a) );
            }
            ctx.createSubcontext(name, ent);
        } else {
            ModificationItem[] mods = new ModificationItem[ar.size()];
                for ( int i=0; i<ar.size(); i++ ) {
                //mods[i] = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("objectclass", ar.get(i)) );
                    mods[i] = ( ModificationItem ) ar.get(i);
                    
            }

            //Perform the requested modifications on the named object
            ctx.modifyAttributes(name, mods);
        }    
    }
    
    synchronized public void operate() {
        final String func=getFunc("operate()");
        //System.out.println("attrLIst:"+lm.attrList);
        if ( attrList != null ) {
            printf(func,3,"entries to modify exist :"+attrList);
           Iterator<String> itter = attrList.keySet().iterator();
           while(itter.hasNext()) {
                 String dn = itter.next(); 
                 printf(func,2,"like to modify dn:"+dn);
                 HashMap<String,String> m = attrList.get(dn);
                 
                 int j = m.size()/2+1;
                 for ( int i=0; i<=j ; i++) {
                     String[] sp = m.get("op"+i+"attr").split(": ");
                     printf(func,1,"call modify :"+m.get("op"+i)+": :"+dn+": :"+sp[0]+": :"+m.get("op"+i+"attr").substring(sp[0].length()+2)+":");
        
                     modify(m.get("op"+i),dn,sp[0],m.get("op"+i+"attr").substring(sp[0].length()+2));
                 }
           }
        }                                
    }
    synchronized public void singlemodify(String op, String dn, String attr, String val) {
        
    }
    
    static private String myusage="\nusage():\noption: [-h hostname] [-p port] [-D adminDN ] [-j passwordfile] <-f <file for operation>|-o <add|del|mod>:dn:attribute:value>\n";
    public static void main(String[] args) throws Exception {
        scanner(args,myusage);
        
        if ( ! usage ) { 
            // LdapModify ls = getInstance(); //getInstance(protocol,hostname,port,userdn,userpw,filter,auth);
            LdapModify ls = getInstance(protocol,hostname,port,userdn,userpw,filter,auth,baseDN);

            if ( ls == null ) {
                System.out.println("ERROR: doesn't create an LdapModify object");
                error_code=-1;
            } else {
               if ( operationfile != null && (new ReadFile(operationfile)).isReadableFile() ) { 
                    ls.modify(operationfile);
               } else {
                    ls.operate();
               }
            }
        }    
        System.exit(error_code);
    }
    
}
