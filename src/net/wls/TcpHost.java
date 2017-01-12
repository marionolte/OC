/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.wls;

import io.file.ReadFile;
import java.net.URL;
import java.util.regex.Pattern;
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
    
    public boolean isLocalAddress(String host){
        if ( host == null || host.isEmpty() ) { throw new RuntimeException("NULL is not a valid name for a host"); }
        else if (   host.toLowerCase().equals("loopback") 
                 || host.toLowerCase().equals("localhost")
                 || host.toLowerCase().equals("127.0.0.1")
                 || host.toLowerCase().equals("::1")        ) { return true; }
        
        Pattern pa = Pattern.compile("^"+host+"[\\ ,\\t]", Pattern.CASE_INSENSITIVE);
        ReadFile fa = new ReadFile("/etc/hosts");
        if ( (pa.matcher(fa.readOut().toString())).find() ) { return true; }
        
        return false;
    }
    
    public boolean isReachable(String url) throws Exception { return isReachable(new URL(url)); }
    public boolean isReachable(URL    url) throws Exception { 
        Http ht = new Http(url);
             ht.setTimeout(5000);
             ht.connect();
        return ( ht.getResponseCode() <= 500 );
    }
}
