/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.wls;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

/**
 *
 * @author SuMario
 */
public class WlsServer extends TcpHost{
    private final HashMap<String,String> map;
    private WlsUser wlsUser=null;
    
    public WlsServer(HashMap<String,String> nh) { this(nh,"WlsServer"); }
    public WlsServer(HashMap<String,String> nh,String name) {
    
        super(null,name);
        this.map = new HashMap<String,String>();
        final String func="WlsServer(HashMap<String,String> nh)";
        Iterator<String> itter = nh.keySet().iterator();
        while(itter.hasNext()) {
            String k = itter.next().toLowerCase();
            if      ( k.equals("#text") ) {}
            else if ( k.equals("ssl")   ) {
                String val = nh.get(k);
                String nam = nh.get("name");
                printf(getFunc(func),3,"ssl value:"+val);
                if ( val != null & ! val.isEmpty() ) {
                     for(String valk : val.split("\n") ) {
                         valk =valk.replaceAll("\t", "").replaceAll(" ","");
                         if ( ! valk.isEmpty() ) {
                             String m = "";
                             if ( valk.equals(nam)     ) { m="sslname";     }
                             else if ( isBoolean(valk) ) { m="sslenabled";  }
                             else if ( isPort(valk)    ) { m="ssllistenport";     }
                             else if ( isHostIp(valk)  ) { m="ssllistenaddress";  }
                            if ( isValidServerKey(m) ) { 
                                  printf(getFunc(func),2,"ssl key:"+m+":  value:"+valk+":");
                                  map.put(m, valk);
                            } 
                         }    
                     }
                }
            }
            else {
                final String m=k.replaceAll("-", "");
                if ( isValidServerKey(m) && nh.get(k) != null )  { map.put(m, nh.get(k)); }
            }    
        }
        
    }
    
    public WlsServer(Properties prop, String name) {
        super(null,name);
        this.map=new HashMap<String,String>();
        Iterator itter =prop.keySet().iterator();
        while(itter.hasNext()) {
            String n = (String) itter.next();
            this.map.put(n, prop.getProperty(n));
        }
    }
    public WlsServer(Properties prop) { this(prop,"WlsServer"); }
    
    public String  getServerValue(String key) { 
        final String k = key.toLowerCase();
        if ( k.contains("pass") ) {
            return crypt.getUnCrypted(k);
        }
        return getReplaceSeparatorBack(this.map.get(k)); 
    }
    public boolean setServerValue(String key, String value ) {
        if ( key == null || ! isValidServerKey(key.toLowerCase()) ) { return false; }
        if ( key.toLowerCase().contains("pass") ) {
            this.map.put(key.toLowerCase(), crypt.getCrypted(value) );
        }
        this.map.put(key.toLowerCase(), getReplaceSeparator(value));
        return true;
    }
    public boolean isValidServerKey(String key) { 
        switch(key) {
            case "name":                return true;
            case "listenport":          return true;
            case "listenaddress":       return true;
            case "sslenabled":          return true;
            case "ssllistenport":       return true;
            case "ssllistenaddress":    return true;
            case "machine":             return true;
            case "cluster":             return true;
            case "adminserver":         return true;
            case "adminuser":           return true;
            case "adminpass":           return true;
            
        }
        return false;
    
    }
    
    public static void main(String[] args) {
        HashMap<String,String> m = new HashMap<String,String> ();
        for (int i=0; i< args.length; i++ ){
            if ( args[i].startsWith("-") ) {  m.put(args[i].substring(1).toLowerCase(), args[++i]);  }
        }
        WlsServer ws = new WlsServer(m);
        System.out.println("WlsServer Info:"+ws);
    }
    
    @Override
    public String toString() {
        StringBuilder sw = new StringBuilder();
        sw.append("name:").append(map.get("name") );
        Iterator<String> itter = map.keySet().iterator();
        while(itter.hasNext()) {
            String k = itter.next();
            if ( ! k.matches("name") )
                sw.append(" ").append(k).append(":").append(map.get(k));
        }
        return sw.toString();
    }
}
