/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import general.Version;
import io.account.User;
import io.file.ReadFile;
import io.file.SecFile;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

/**
 *
 * @author SuMario
 */
class PullHttp extends Version{
    Http ht = null ;
    HashMap<String, String> map;
    Properties prop;
    Properties refp;
    User       user;
    String use="usage: -conn <connection file> [-profile <profile>]";
    PullHttp(String[] ar) {
        final String func="PullHttp(String[] ar)";
        map = io.lib.IOLib.scanner(ar, use);
        System.out.println("map:"+map);
        
        prop = (new ReadFile(map.get("-profile"))).getProperties();
        //refp = new Properties();
        //Iterator itter= prop.keySet().iterator();
        //while ( itter.hasNext() ) {
        //         String k = (String) itter.next();
        //         refp.put( (String) prop.get(k), k);
        //}
        
        Properties p=(new SecFile(map.get("-conn"))).getProperties();
        
        user = new User(p.getProperty("USER"), p.getProperty("PASSWORD")){};
        
    }
    
    private URL getLogin() {  return getURL("login"); }
    private URL getLogout(){  return getURL("logout"); }
    
    private String getLoginFormPost() { return getKey("loginPOST"); }
    private String getLoginOK() { return getKey("loginOK"); }
    
    private String getKey(String k){
         if ( k == null || k.isEmpty() ) { return "";} 
         String s = (String)prop.get(k);
         return (s==null)?"":s;
    }
    
    private URL getURL(String k){
         try { 
           return new URL( getKey(k) );
       } catch ( java.net.MalformedURLException ue) {
           return (URL) null;
       } 
    }
    
    private String getLoginPost() {
        String s = getKey("loginTYP");
        if ( s.isEmpty() || s.toUpperCase().equals("FORM") ) { return getLoginFormPost(); }
        
        return "";
    }
    
    
    void run() {
        URL u = getLogin();
        if ( u != null ) {
          try{   
            ht = new Http( u );
            if ( user.isPasswordSet() ) {
                 String s = getLoginPost().replaceAll("@@user@@", user.getUsername()).replaceAll("@@pass@@", user.getPassword() );
                 ht.setPost(new StringBuilder(s) );
                 ht.connect();
                 StringBuilder sw = ht.getResponse();
                 
            }
          }catch(Exception e){
          }  
        }
        
        
           u = getLogout();
        if ( u != null  && ht != null ) {            
            try {
                ht.connect(u);
            } catch(java.io.IOException io) {
                
            }
        }   
    }
}
