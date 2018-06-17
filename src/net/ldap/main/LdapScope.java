/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.ldap.main;

/**
 *
 * @author SuMario
 */
public enum LdapScope {
    base, sub, one;
    
    static private LdapScope ls=LdapScope.base;
    
    static public LdapScope getId(String info) {
        if      ( info.toLowerCase().matches("base") ) { return LdapScope.base; }
        else if ( info.toLowerCase().matches("sub")  ) { return LdapScope.sub; }
        else if ( info.toLowerCase().matches("one")  ) { return LdapScope.one; }
        throw new LdapScopeException("not supported scope");
    }
    
    static public void setId(LdapScope scope) { ls=scope;}
    
    static public boolean compare(LdapScope comp) {
        return ls.compare(comp);
    }
    
    static public boolean equal(LdapScope comp) {
        return ls.equals(comp);
    }
}
