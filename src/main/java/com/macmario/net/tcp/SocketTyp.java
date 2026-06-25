/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.tcp;

/**
 *
 * @author SuMario
 */
public enum SocketTyp {
    TLS,SSL,PLAIN ;
    static SocketTyp e = SocketTyp.TLS;
    
    public void   setName(SocketTyp f) {
        SocketTyp.e=f;
    }
    public String getName(){
        String s="TLS";
        switch(e) {
            case SSL   : s="SSL"; break;
            case PLAIN : s="PLAIN";  break;
        }
        return s;
    }
}
