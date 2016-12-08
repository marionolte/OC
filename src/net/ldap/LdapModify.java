/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.ldap;

import javax.naming.NamingException;

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
    
    private LdapModify() {
        name="LdapModify";
    }
    
    static private String myusage="\nusage():\noption: [-h hostname] [-p port] [-D adminDN ] [-j passwordfile] -f <file for operation>\n";
    public static void main(String[] args) throws Exception {
        scanner(args,myusage);
        
        if ( ! usage ) { 
            LdapModify ls = getInstance(); //getInstance(protocol,hostname,port,userdn,userpw,filter,auth);

            if ( ls == null ) {
                System.out.println("ERROR: doesn't create an LdapModify object");
                error_code=-1;
            } else {

            }
        }    
        System.exit(error_code);
    }
    
}
