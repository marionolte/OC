/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.comm.mail;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

/**
 *
 * @author SuMario
 * //- See more at: https://javamail.java.net/docs/SSLNOTES.txt#sthash.hWMHJJV8.dpuf
 */
public class MySSLSocketFactory extends SSLSocketFactory{
     private SSLSocketFactory factory;
     
        public MySSLSocketFactory() { 
            try { 
                SSLContext sslcontext = SSLContext.getInstance("TLS");
                sslcontext.init(null, new TrustManager[] { new MyTrustManager()}, null); 
                factory = (SSLSocketFactory)sslcontext.getSocketFactory();
         
            } catch(Exception ex) { 
                // ignore 
            } 
        } 
    
        
        public static SocketFactory getDefault() { return new MySSLSocketFactory(); } 

    
        public Socket createSocket() throws IOException { return factory.createSocket(); } 
        public Socket createSocket(Socket socket, String s, int i, boolean flag) throws IOException { 
            return factory.createSocket(socket, s, i, flag); 
        } 
        public Socket   createSocket(InetAddress inaddr, int i, InetAddress inaddr1, int j) throws IOException { 
            return factory.createSocket(inaddr, i, inaddr1, j); 
        } 
        public Socket   createSocket(InetAddress inaddr, int i) throws IOException { return factory.createSocket(inaddr, i); } 
        public Socket   createSocket(String s, int i, InetAddress inaddr, int j) throws IOException { return factory.createSocket(s, i, inaddr, j); } 
        public Socket   createSocket(String s, int i                           ) throws IOException { return factory.createSocket(s, i); } 
        public String[] getDefaultCipherSuites()   { return factory.getDefaultCipherSuites(); } 
        public String[] getSupportedCipherSuites() { return factory.getSupportedCipherSuites(); } 
}
