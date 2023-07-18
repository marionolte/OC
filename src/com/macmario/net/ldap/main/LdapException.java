/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.ldap.main;

/**
 *
 * @author SuMario
 */
public class LdapException extends RuntimeException {

    public LdapException(String context_not_initialized) {
        super(context_not_initialized);
    }
    
}
