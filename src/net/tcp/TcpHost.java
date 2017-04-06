/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.tcp;

import io.file.ReadFile;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Scanner;
import main.Http;
import main.MainTask;

/**
 *
 * @author SuMario
 */
public abstract class TcpHost extends MainTask{
    
    public TcpHost() { this(null,"TcpHost");}
    public TcpHost(String[] args,String name) { super(args,name); }
    
    public boolean isBoolean(String k) {
         return (k!= null && ( k.toLowerCase().equals("true") || k.toLowerCase().equals("false") ));
    }
    
    public int isInteger(String k) {
        int i=-1;
        try {
          i = Integer.parseInt(k);
          return i;
        } catch(Exception e) {}
        return i;
    }
    
    public boolean isPort(String k) {
        int i = isInteger(k);        
        return (i>0 && i<(64*1024-1));
    }
    
    public boolean isHostIp(String k) {
          
       return false; 
    }
    
    
    private HashMap<String,String> map = new HashMap(); 
    private ArrayList<InetAddress> addr=new ArrayList();
    
    public boolean isLocalAddress(String host){
        if ( host == null || host.isEmpty() ) { throw new RuntimeException("NULL is not a valid name for a host"); }
        else if (   host.toLowerCase().equals("loopback") 
                 || host.toLowerCase().equals("localhost")
                 || host.toLowerCase().equals("127.0.0.1")
                 || host.toLowerCase().equals("::1")        ) { return true; }
        
        readHosts();
        if ( map.containsValue(host.toLowerCase())){ return true;}
        
        return false;
    }
    
    private void readHosts() {
        //Pattern pa = Pattern.compile("^"+host+"[\\ ,\\t]", Pattern.CASE_INSENSITIVE);
        if ( ! map.isEmpty() ) { return; }
        ReadFile fa = new ReadFile("/etc/hosts");
        
        for(String s : fa.readOut().toString().split("\n")) {
            if ( s.startsWith("[1-9]") ) {
                 String[] sp =  s.trim().split("[\t, ]");
                 boolean first=true;
                 for (int i=1; i< sp.length; i++) {
                     if ( !sp[i].isEmpty() ) {                         
                         if ( first ) {  map.put(sp[0], sp[i].toLowerCase()); first=false;}
                         map.put(sp[i].toLowerCase(), sp[0]);
                     }
                 }
            }
        }
        
        separateInterfaces();
    }
    
    
    public Enumeration<NetworkInterface> getNetInferfaces() {
        try {
          return NetworkInterface.getNetworkInterfaces();
        } catch(Exception e) {
          return null;  
        } 
    }
    private void separateInterfaces() {
        final String func =getFunc("separateInterfaces()");
        final Enumeration<NetworkInterface> nifs = getNetInferfaces();
        if ( nifs != null ) {
           while (nifs.hasMoreElements()) { 
                NetworkInterface                       nif = nifs.nextElement();
                Enumeration<java.net.InetAddress> addrs = nif.getInetAddresses();
                while ( addrs.hasMoreElements() ) {
                    InetAddress inf = addrs.nextElement();
                    if ( inf.isAnyLocalAddress() ) {
                          String ho = inf.getHostAddress().toLowerCase();  String ip=inf.getHostAddress();
                          if ( ho == null || ho.isEmpty() ) { ho=ip; }
                          map.put(ho,ip); map.put(ip, ho);
                    }
                }
           }
           
        } else {
          printf(func,1,"no network interfaces found");  
        }
    }
    
    
    public boolean isReachable(String url) throws Exception { return isReachable(new URL(url)); }
    public boolean isReachable(URL    url) throws Exception { 
        Http ht = new Http(url);
             ht.setTimeout(5000);
             ht.connect();
        return ( ht.getResponseCode() <= 500 );
    }
    
    
    public static String getHostname() { 
       try { return execReadToString("hostname"); } catch(java.io.IOException io){ return "localhost"; }
    }
    
    
    public static String execReadToString(String execCommand) throws java.io.IOException {
        Process proc = Runtime.getRuntime().exec(execCommand);
        try (InputStream stream = proc.getInputStream()) {
            try (Scanner s = new Scanner(stream).useDelimiter("\\A")) {
                return s.hasNext() ? s.next().trim() : "";
            }
        }
    }
}
