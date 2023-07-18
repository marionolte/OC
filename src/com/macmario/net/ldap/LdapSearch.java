/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.ldap;

import com.macmario.net.ldap.main.LdapMain;
import com.macmario.net.ldap.main.LdapException;
import com.macmario.net.ldap.main.LdapScope;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;


/**
 *
 * @author SuMario
 */
public class LdapSearch  extends LdapMain{
    
    static public LdapSearch getInstance( String protocol, String hostname, int port, String userDN, String userPWD, String filter , String auth , String baseDN) throws NamingException {
        //printf("getIN",0,"create instance");
        LdapSearch ls = new LdapSearch();
                   ls.getScope("sub");
                   ls.protocol=protocol;
                   ls.port=port;
                   ls.hostname=hostname;
                   ls.userdn=userDN;
                   ls.userpw=userPWD;
                   ls.filter=filter;
                   ls.auth=auth;
                   ls.baseDN=baseDN;
        ls.printf("getInstance()",2,"initalize "+ls.name+" with :"+ls.protocol+":"+ls.userdn+":"+ls.userpw+"//"+ls.hostname+":"+ls.port+"/"+ls.baseDN+"?"+ls.filter);
        
        ls.initialize(ls,  protocol,  hostname,  port,  userDN,  userPWD,  filter ,  auth);
        
        ls.init();
        ls.printf("getInstance()",3,"return Object "+ls.name);
        return ls;
    }
    
    private boolean persist=false;
    public boolean setPersistentSearch(boolean b) { persist=b; return getPersistentSearch();}
    public boolean getPersistentSearch() {return persist; }
    
    static public LdapSearch getInstance() throws NamingException {
        LdapSearch ls = new LdapSearch();
        ls.getScope("sub");
        ls.protocol="ldap";
        ls.hostname="localhost";
        ls.port=389;
        ls.userdn="cn=admin";
        ls.userpw="";
        ls.filter="objectclass=*";
        ls.auth="simple";
        ls.baseDN=ls.getDefaultBaseDN();
        return getInstance(ls.getProtocol(),ls.hostname,ls.port,ls.userdn,ls.userpw,ls.filter,ls.auth,ls.getBaseDN());
        //return ls;
    }
    static public LdapSearch getInstance(String[] ar) throws NamingException {
        LdapSearch ls = new LdapSearch();
        ls.getScope("sub");
        ls.protocol="ldap";
        ls.hostname="localhost";
        ls.port=389;
        ls.userdn="cn=admin";
        ls.userpw="";
        ls.filter="objectclass=*";
        ls.auth="simple";
        ls.baseDN=ls.getDefaultBaseDN();
        ls.scanner(ar,myusage);
        //printf("aaa",0,"user "+getUserDN()+" local:"+userpw+" pw:"+getUserPass()+":  map:"+map.get("-w"));
        //printf("aaa",0,"port "+getPort()+"   local:"+port+" port:"+getPort()+":  map:"+map.get("-p"));
        //printf("aaa",0,"filter:"+ls.getFilter()+":  scope:"+ls.getMyScope()+":"+ls.getScope()+":");
        
        LdapSearch lm=getInstance(ls.getProtocol(),ls.getHostname(),ls.getPort(),ls.getUserDN(),ls.getUserPass(),ls.getFilter(),ls.getAuth(),ls.getBaseDN());
                   lm.objList=ls.getAttrList();
                   lm.getScope(ls.getScope());
       return lm;
    }
    
    private LdapSearch() {  name="LdapSearch"; }
    
    static public String myusage="\nusage():\noption: [-h hostname] [-p port] [-D adminDN ] [-j passwordfile] [-a <simple|>] "
            + "[-scope <sub|one|base>] [-sizelimit <SearchSizeLimit>] [-pg <ldap lookup entry page>] [-b baseDN ] [-f filter]  <attribut list>\n";
    
    
    private byte[] cookie = null;
    
    public NamingEnumeration search() throws NamingException, IOException { 
        final String func=getFunc("search()");
        printf(func,2,"like to search with:"+getFilter()+": to basedn :"+getBaseDN()+": to get attributes:"+getAttrList());
        return search(getBaseDN(),getFilter(),getAttrList());
    }
    
    SearchControls ctls = null; 
    public NamingEnumeration search(String baseDN, String filter, ArrayList attr) throws NamingException, IOException {
        if ( getLdapContext() == null ) 
            throw new LdapException("Context not initialized");
        final String func=getFunc("search(String baseDN, String filter, ArrayList attr)");
        printf(func,4,"start seaching event");
        
        getLdapContext().setRequestControls(new Control[] {new PagedResultsControl( getPageSize(), Control.CRITICAL) }); 
        
        ctls = new SearchControls();
        
        
        
        // remove filter local
        printf(func,3," attr list:"+attr.size());
        if (attr.size()>0) {
            String[] attrIDs = new String[attr.size()]; // { "sn", "telephonenumber", "golfhandicap", "mail" };
            for(int i=0; i<attr.size();i++) { attrIDs[i] = (String) attr.get(i); }
            ctls.setReturningAttributes(attrIDs);
        }
        
        if ( getSearchTimeout() > 0)
            ctls.setTimeLimit(getSearchTimeout());
        
        printf(func,2,"search my scope:"+getMyScope());
        //if ( getMyScope().equals(LdapScope.sub))
           ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        if ( getMyScope().equals(LdapScope.one)) {
           ctls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
        }   
        if ( getMyScope().equals(LdapScope.base)) {
           ctls.setSearchScope(SearchControls.OBJECT_SCOPE);
        }   
           
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
        getLdapContext().setRequestControls( new Control[] { new PagedResultsControl( getPageSize(), cookie, Control.CRITICAL) });
                
        printf(func,3,"return results:"+results);
        return results;
        
    }
    
    public NamingEnumeration trysearch() throws NamingException, IOException { 
        final String func=getFunc("trysearch()");
        printf(func,2,"like to search again");
        
        if (ctls == null ) { throw new IOException("invalid operation - not searched before");}
        
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
        getLdapContext().setRequestControls( new Control[] { new PagedResultsControl( getPageSize(), cookie, Control.CRITICAL) });
        
        return results;
    }
    public boolean couldSearchAgain() { return (cookie !=null); }
    
    
    public boolean printResults(NamingEnumeration namEnum ) throws NamingException {
        final String func=getFunc("printResults(NamingEnumeration namEnum)");
        final int savdeb=debug; //debug=4;
        boolean b=false;
        printf (func,3," print results:"+(namEnum != null && namEnum.hasMore())+" nameEnum:"+namEnum);
        HashMap<String, ArrayList<String>> imp = new HashMap<String, ArrayList<String>>();
        while (namEnum != null && namEnum.hasMore()) {
               SearchResult entry = (SearchResult) namEnum.next();
               ArrayList<String> ar = new ArrayList<String>(); ar.add(entry.getNameInNamespace());
               imp.put("dn", ar);
               //System.out.println("dn: "+entry.getNameInNamespace() ); 
               b=true;
        }       
        
        ArrayList<String> ar;
        while (namEnum != null && namEnum.hasMore()) {
               SearchResult entry = (SearchResult) namEnum.next();
               System.out.println("dn: "+entry.getNameInNamespace() ); b=true;

               
               Attributes attr = entry.getAttributes();
               printf(func,3,"Attributes:"+attr);
               NamingEnumeration en = attr.getAll();
               while(en!= null && en.hasMore() ) {
                   Attribute at = (Attribute) en.next();
                   ar = new ArrayList<String>();
                   printf(func,3,"Attribute:"+at);
                   String id=at.getID().toLowerCase();
                   String va=at.toString().substring(at.getID().length()+2);
                   printf(func,3,"id:"+id+":  :"+va+":");
                   if ( ! va.contains(" ") ) {
                        //String sp[]  = at.toString().substring(at.getID().length()+2).split(",");
                        String sp[]  = va.split(",");
                        if ( sp.length == 0 || sp.length >2 ) {
                              printf(func,4,id+": "+va);
                              ar.add(va);
                        } else {
                            for (int i=0; i<sp.length; i++) {
                                printf(func,4,"i="+i+":   sp[]=:"+sp[i]+":");
                                Object ob=at.get();

                                String v= (( ob instanceof byte[] )? new String((byte[]) ob ):sp[i]).replaceAll("^ ", "");
                                printf(func,4,id+": "+v);
                                ar.add(v);
                            }
                        }
                   } else {
                       printf(func,3,"id:"+id+":  with space value :"+va+":");
                       for ( String a : va.split(" ") ) {
                           if ( ! a.isEmpty() ) {
                                printf(func,3,"id:"+id+":  with space value :"+a.replaceAll(",$", "")+":");
                                ar.add(a.replaceAll(",$", "") );
                           }     
                       }
                   }     
                   printf(func,3,"put list for:"+id+" size :"+ar.size()+":");
                   imp.put(id, ar);
               }
               
               
               System.out.println("dn: "+(imp.get("dn")).get(0));
               ArrayList<String> a  = super.getAttrList();
               ArrayList<String> ab = new ArrayList();
               for ( String af : a ) { ab.add(af.toLowerCase()); }
               if ( debug >= 0 ) {
                   printf(func,1,"objList is Empty ? :"+a.isEmpty()+":  ");
                   for(String s: a ) {
                       printf(func,1,"print attribut :"+s+":  "+a.contains(s));
                   }
               }
               ar = imp.get("objectclass");
               if ( ar != null )
                 for ( int i=0; i<ar.size() ; i++ ) {
                   String o = ar.get(i);
                   if ( a.isEmpty() || a.contains("objectclass") ) System.out.println("objectclass: "+o);
                 }
               Iterator<String> itter = imp.keySet().iterator(); 
               while ( itter.hasNext() ) {
                   String f = itter.next();
                   if ( ! f.isEmpty() && ! f.equals("dn") && ! f.equals("objectclass")) {
                       ar = imp.get(f);
                       for(int i=0; i<ar.size(); i++) {
                           if ( a.isEmpty() || a.contains(f) || ab.contains(f) ) { System.out.println(f+": "+ar.get(i)); }
                       }

                   }
               }
               
               System.out.println("");
               //NamingEnumeration en=at.getIDs();
               ///for (int i=0; i<at.size(); i++ ) {
               //    System.out.println("at("+i+"):"+(String) at.get(i));
               //}
               
               
        }
        debug=savdeb;

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
        LdapSearch ls = getInstance(args); //scanner(args,myusage);
        
        if ( ! ls.usage ) { 
            
             //getInstance(protocol,hostname,port,userdn,userpw,filter,auth);

            if ( ls == null ) {
                System.out.println("ERROR: doesn't create an LdapSearch object");
                ls.error_code=-1;
            } else {
                do {
                    ls.printResults( ls.search(ls.getBaseDN(), ls.getFilter(), ls.getAttrList()) );

                } while ( ls.cookie != null ); 
            }
        }
        System.exit(ls.error_code);
    }

    
}
