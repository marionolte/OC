/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.comm.mail;

import com.macmario.general.Version;
import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;

/**
 *
 * @author SuMario
 * - See more at: https://javamail.java.net/docs/SSLNOTES.txt#sthash.hWMHJJV8.dpuf
 */
class MyTrustManager extends Version implements X509TrustManager {

    public MyTrustManager() {
    }

    @Override
     public void checkClientTrusted(X509Certificate[] cert, String authType) { 
        // everything is trusted 
     } 
     
    @Override
     public void checkServerTrusted(X509Certificate[] cert, String authType) { 
            // everything is trusted 
     } 
     
    @Override
     public X509Certificate[] getAcceptedIssuers() { 
            return new X509Certificate[0]; 
     } 
    
}
