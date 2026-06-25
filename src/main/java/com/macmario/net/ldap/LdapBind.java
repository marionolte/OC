/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.ldap;

import com.macmario.net.ldap.main.LdapMain;
import javax.naming.NamingException;

/**
 *
 * @author SuMario
 */
public class LdapBind extends LdapMain{
    
    public static LdapBind getInstance( String protocol, String hostname, int port, String userDN, String userPWD, String filter , String auth ) throws NamingException {
        LdapBind ls = new LdapBind();
        
        ls.initialize(ls,  protocol,  hostname,  port,  userDN,  userPWD,  filter ,  auth);
        
        return ls;
        
    }
    
    public static LdapBind getInstance() {
        LdapBind ls = new LdapBind();
                 ls.initialize(ls);
        
        return ls;
    }
    
    static public LdapBind getInstance(String[] ar) throws NamingException {
        LdapBind ls = new LdapBind();
        ls.protocol="ldap";
        ls.hostname="localhost";
        ls.port=389;
        ls.userdn="cn=admin";
        ls.userpw="";
        ls.filter="objectclass=*";
        ls.auth="simple";
        ls.scanner(ar,myusage);
        ls.initialize(ls);
        return ls;
    }
    
    private LdapBind() {
        name="LdapBind";
    }

    
    private boolean b =  false;
    
    public boolean bind() {
        final String func="bind()";
        b=false;
        try {
          err=null;
          initialize(this);
          printf(func,3," init to :"+getEnv());
          init();          
          b=true;  
        } catch(Exception e) {
          err="ERROR:"+e.toString(); error_code=-1; 
          b=false;  
        } finally {
          return isBind();
        }   
    }
    public boolean isBind() { return b; }
    
    private String err=null;
    public String getErrorMsg() { return err; }
    static public String myusage="\nusage():\noption: [-h hostname] [-p port] [-D adminDN ] [-j passwordfile] [-b baseDN ]";
    
    public static void main(String[] args) throws Exception {
        LdapBind lb = getInstance(args);  // scanner(args,myusage);
        
        if ( ! lb.usage ) {
            //proto,hostname,port,userdn,userpw,filter,auth);
            System.out.println("bind "+((lb.bind())?"successful":"failed"));
            System.exit(lb.error_code);
        } else {
            System.exit(-1);
        }
    }

    
}
