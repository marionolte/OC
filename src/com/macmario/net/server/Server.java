/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.server;

import com.macmario.io.thread.RunnableT;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;


/**
 *
 * @author SuMario
 */
public class Server extends RunnableT {
   
    boolean started;
    boolean proto;
    String host;
    int port;
    StringBuilder msg;
    static Properties p=null;
    
    public Server(boolean proto, String host, int port, StringBuilder msg) {
        this();
        this.proto= proto;
        this.host = host;
        this.port = port;
        this.msg  = msg;
        this.started=false;
    }
    
    private Server() {
        
    }
    
    private String getHost(String s) {
        if ( s == null || s.isEmpty() ) { return "localhost";}
        String [] sp = s.split(":");         
        return (sp[0].length()>4)?sp[0]:"localhost";
    }
    
    private int getPort(String s) {
        String [] sp = s.split(":");   
        try {
            return Integer.parseInt(sp[ sp.length-1 ]);
        } catch (Exception e ) { return -1; }    
    }
    
    int TIMEOUT=30000;
    
    
    @Override
    public void run() {
        setRunning();
        try {
            if ( p.get("ADMINServer") != null ) { 
                  openAdmin(new URI((String) p.get("ADMINServer")));
            }
        } catch (URISyntaxException ex) {
        }    
        setRunning();
        Closed();
    }
    
    private void openAdmin(URI uri) {
        
    }
 
    private void Closed() {
        this.setClosed();
    }
    
    public Server getInstance(String[] ar) throws FileNotFoundException, IOException, URISyntaxException {
        Server s = new Server();
        s.p =  new Properties();
        if ( ar.length > 0 ) 
            for (int i=0; i< ar.length; i++ ) {
                if ( ar[i].matches("-conf")           ) { s.p.load( new FileInputStream(ar[++i])  ); }
                else if ( ar[i].matches("-tcpserver") ) { s.p.setProperty("TCPServer",    (new URI( ar[++i] ) ).toString() ); }
                else if ( ar[i].matches("-tcpproxy")  ) { s.p.setProperty("TCPProxy" ,    (new URI( ar[++i] ) ).toString() ); }
                else if ( ar[i].matches("-udpserver") ) { s.p.setProperty("UDPServer",    (new URI( ar[++i] ) ).toString() ); }
                else if ( ar[i].matches("-admin")     ) { s.p.setProperty("AdminConnect", (new URI( ar[++i] ) ).toString() ); }
                else if ( ar[i].matches("-adminserver")){ s.p.setProperty("ADMINServer",  (new URI( ar[++i] ) ).toString() ); }
                else if ( ar[i].matches("-adminproxy" )){ s.p.setProperty("ADMINProxy",   (new URI( ar[++i] ) ).toString() ); }
            }
        
               s.start();
        return s; 
    }
    
    public static void main(String[] args) throws Exception {
        Server s = (new Server()).getInstance(args);
        while ( s.isRunning() ) { sleep(300); }
        s.Closed();
    }

    
}
