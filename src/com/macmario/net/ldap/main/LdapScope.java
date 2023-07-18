/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.ldap.main;

/**
 *
 * @author SuMario
 */
public enum LdapScope {
    base, sub, one;
    
    static private LdapScope ls=base;
    
    static public LdapScope getId(String info) {
        if      ( info.isEmpty() || info.toLowerCase().matches("sub")  ) { ls=LdapScope.sub;  return ls;  }
        else if (                   info.toLowerCase().matches("base") ) { ls=LdapScope.base; return ls; }
        else if (                   info.toLowerCase().matches("one")  ) { ls=LdapScope.one; return ls;  }
        throw new LdapScopeException("not supported scope");
    }
    
    static public String get() {
        if      ( ls.equals(one) ) { return "one";  }
        else if ( ls.equals(base)) { return "base"; }
        return "sub";
    }
    static public void setId(LdapScope scope) { ls=scope;}
    
    static public boolean compare(LdapScope comp) {
        return ls.compare(comp);
    }
    
    static public boolean equal(LdapScope comp) {
        return ls.equals(comp);
    }
}
