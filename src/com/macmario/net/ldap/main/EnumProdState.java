/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.ldap.main;

/**
 *
 * @author SuMario
 */
public enum EnumProdState {
        PROD,QA,TEST;
        
    boolean isValidMode(String mode) {
        System.out.println("test:"+mode+":");
        if ( mode.matches(""+PROD) || mode.matches(""+QA) || mode.matches(""+TEST)) { return true;}
        return false; 
    }
    
    boolean isToOK(String mode) {
        System.out.println("to test :"+mode+":");
        if ( mode.matches(""+QA) || mode.matches(""+TEST)) { return true;}
        return false;
    }
    
    String printToOK()   { return (QA+","+TEST); }    
    String printFromOK() { return PROD+printToOK(); }
}
