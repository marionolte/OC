/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.ldap.main;

import java.io.IOException;
import java.util.ArrayList;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;
import com.macmario.net.ldap.LdapModify;
import com.macmario.net.ldap.LdapSearch;

/**
 *
 * @author SuMario
 */
public class LdapTrans extends LdapMain {

    private String from;
    private String to;
    private String[] userlist;

    private LdapTrans(String from, String to, String[] ul) {
        super();
        EnumProdState e = EnumProdState.PROD;
        this.from=from.toUpperCase(); this.to=to.toUpperCase();
        System.out.println("to:"+this.to+":  from:"+this.from+":");
        if ( ! e.isValidMode(this.to)   || ! e.isToOK(this.to) ) { throw new RuntimeException("ERROR: -to:"+this.to+" are not OK - possible are :"+e.printToOK()); }
        if ( ! e.isValidMode(this.from)                        ) { throw new RuntimeException("ERROR: -from:"+this.from+" are not OK - possible are :"+e.printFromOK()); }
        if ( this.from.equals(this.to)) {
            throw new RuntimeException("ERROR:  FROM:"+from+" are equals TO:"+to+" - this are nor allowed");
        }
        this.userlist = ul;
    }
    private LdapTrans() { super(); }
    
    private LdapSearch search;
    private LdapModify mod;
    private LdapSearch modsearch;
    private void trans() throws NamingException, IOException{
         search = LdapSearch.getInstance( conn.getProperty(from+"PROTO"), 
                                          conn.getProperty(from+"HOST"),
                                          Integer.parseInt( conn.getProperty(from+"PORT") ), 
                                          conn.getProperty(from+"USER"), 
                                          conn.getProperty(from+"PASS"),
                                          "objectclass=*", 
                                          "simple",
                                          conn.getProperty( from+"BASEDN")
                                        );
         search.getScope("sub");
         
         mod  = LdapModify.getInstance(   conn.getProperty( to+"PROTO"), 
                                          conn.getProperty( to+"HOST"),
                                          Integer.parseInt( conn.getProperty(to+"PORT") ), 
                                          conn.getProperty( to+"USER"), 
                                          conn.getProperty( to+"PASS"),
                                          "objectclass=*", 
                                          "simple",
                                          conn.getProperty( to+"BASEDN")
                                        );
         modsearch = LdapSearch.getInstance(
                                          conn.getProperty( to+"PROTO"), 
                                          conn.getProperty( to+"HOST"),
                                          Integer.parseInt( conn.getProperty(to+"PORT") ), 
                                          conn.getProperty( to+"USER"), 
                                          conn.getProperty( to+"PASS"),
                                          "objectclass=*", 
                                          "simple",
                                          conn.getProperty( to+"BASEDN")
                                        );
         modsearch.getScope("sub");
         
         objList.add("objectclass=*");
         for(String u:userlist) {
             if ( ! u.isEmpty() ) { trans(u); }
         }
    }
    
    
    public NamingEnumeration search(LdapContext ctx,int scope, String baseDN, String filter, ArrayList attr) throws NamingException, IOException {
        if ( getLdapContext() == null ) 
            throw new LdapException("Context not initialized");
        final String func="search(String baseDN, String filter, ArrayList attr)";
        
        ctx.setRequestControls(new Control[] {new PagedResultsControl( getPageSize(), Control.CRITICAL) }); 
        
        
        SearchControls ctls = new SearchControls();
        
        if (attr.size()>0) {
            String[] attrIDs = new String[attr.size()]; // { "sn", "telephonenumber", "golfhandicap", "mail" };
            for(int i=0; i<attr.size();i++) { attrIDs[i] = (String) attr.get(i); }
            ctls.setReturningAttributes(attrIDs);
        }
        if ( getSearchTimeout() > 0)
            ctls.setTimeLimit(getSearchTimeout());
        
        if      ( scope == 2 ) { ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);  }  // 2
        else if ( scope == 1 ) { ctls.setSearchScope(SearchControls.ONELEVEL_SCOPE); }  // 1
        else {                   ctls.setSearchScope(SearchControls.OBJECT_SCOPE);   }  // 0 base
           
        printf(func,2,"run search against :"+baseDN);
        NamingEnumeration results = ctx.search(baseDN, filter, ctls); //ctx.search( baseDN, getEnv("java.naming.ldap.attributes.binary") , ctls);
        
        
        return results;
        
    }
    
    private ArrayList objList=new ArrayList();
    private boolean trans(String user) throws NamingException, IOException {
        final String func="trans(String user) throws NamingException, IOException";
        String fi="(|(uid="+user+")(cn="+user+"))";
        System.out.println("find in:"+conn.getProperty(from+"BASE")+":  user:"+fi+":"); 
        //NamingEnumeration find  =    search.search(conn.getProperty(from+"BASE"), fi, objList );
        NamingEnumeration find  =  search(search.getLdapContext(),1,conn.getProperty(from+"BASE"), fi, objList );
        
        SearchResult userentry=null;
        System.out.println("find:"+find.hasMore());
        while (find != null && find.hasMore()) {
               SearchResult entry = (SearchResult) find.next();
               userentry = entry;
               System.out.println("find dn: "+entry.getNameInNamespace()+" =>"+entry.toString());
        }
        
        if ( userentry != null ) {
            SearchResult modentry=null;
            NamingEnumeration tofind  =  search(modsearch.getLdapContext(),1,conn.getProperty(to+"BASE"), fi, objList );
            while (tofind != null && tofind.hasMore()) {
                   SearchResult entry = (SearchResult) find.next();
                   modentry = entry;
                   System.out.println("mod  dn: "+entry.getNameInNamespace()+" =>"+entry.toString());
            }
            if ( modentry == null ) {
                printf(func,0,"user dn not exist on remote target");
            } 
            
            return true;
            
        } else {
            printf(func,0,"user "+user+" are not exist on from target ");
        }    
        
        return false;
    }

    static private String myusage="\nusage():\noption: -conn <connection-file> -from <PROD|QA|TEST> -to <QA|TEST> -u <user1<,user2...>>";
    public static void main(String[] args) {
        LdapTrans ls = new LdapTrans();
        //scanner(args, myusage);
        
        StringBuilder userlist=new StringBuilder();
        String from="";
        String   to="";
        for( int i=0; i< args.length; i++ ) {
            if      ( args[i].matches("-u")    ) { if (userlist.capacity()>0){userlist.append(","); }  userlist.append(args[++i]); }
            else if ( args[i].matches("-from") ) { from=args[++i];}
            else if ( args[i].matches("-to")   ) {   to=args[++i];}
        }
        
        if ( ls.usage || args == null || ls.conn.isEmpty() ) {
             if ( ! ls.usage ) { System.out.println(myusage); }
             System.exit(1);
        } 
        
        try {
            LdapTrans lt = new LdapTrans(from.toUpperCase(),to.toUpperCase(), userlist.toString().split(",") );
                      lt.trans();
            System.exit(0);
        } catch (Exception e) {
            printf("",0,"Ldap transport stopps running with reason - "+e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }   
    }

    
    
}
