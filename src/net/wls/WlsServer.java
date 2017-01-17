/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.wls;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import main.Http;

/**
 *
 * @author SuMario
 */
public class WlsServer extends TcpHost{
    private final HashMap<String,String> map;
            WlsUser wu=null;
    private Http    ht=null;
    private String  baseUrl=null;
    
    WlsServer(WlsServer ws , String name ) {
        super(null,name);
        this.map=ws.map;
    }
    public WlsServer(HashMap<String,String> nh) throws Exception { this(nh,"WlsServer"); }
    public WlsServer(HashMap<String,String> nh,String name) throws Exception {   
        super(null,name);
        if (debug < 3 ) { while ( debug < 3 ) { debug++; } }
        this.map = new HashMap<String,String>();
        final String func="WlsServer(HashMap<String,String> nh)";
        Iterator<String> itter = nh.keySet().iterator();
        while(itter.hasNext()) {
            String k = itter.next().toLowerCase();
            printf(func,0,"get key="+k+"|  value =>|"+nh.get(k)+"|<=");
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
            else if ( k.endsWith("listen-port")) { 
                map.put(k.replaceAll("-", ""), nh.get(k));
            }
            else {
                final String m=k.replaceAll("-", "");
                printf(getFunc(func),3,"key:"+m+":  have =>"+nh.get(k));
                if ( isValidServerKey(m) ) { //&& nh.get(k) != null )  { 
                    printf(getFunc(func),2,"key:"+m+":  value:"+nh.get(k)+":");
                    map.put(m, nh.get(k)); 
                }
            }    
        }
        init();
    }
    
    public WlsServer(Properties prop, String name) throws Exception {
        super(null,name);
        this.map=new HashMap<String,String>();
        Iterator itter =prop.keySet().iterator();
        while(itter.hasNext()) {
            String n = (String) itter.next();
            this.map.put(n, prop.getProperty(n));
        }
        init();
    }
    public WlsServer(Properties prop) throws Exception { this(prop,"WlsServer"); init(); }
    
    public String  getServerValue(String key, String def) {
        if ( key == null || key.isEmpty() ) { return ""; } 
        final String k = key.toLowerCase();
        String val=this.map.get(k);
        if ( val == null ) { val=def; }
        if ( val == null ) { return null; }
        if ( k.contains("pass") && ! val.isEmpty() ) {
            return getReplaceSeparatorBack(crypt.getUnCrypted(val));
        }
        return getReplaceSeparatorBack(val);
    }
    public String  getServerValue(String key) { return getServerValue(key, null); }
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
            case "name":                
            case "listenport":          
            case "listenaddress":       
            case "sslenabled":          
            case "ssllistenport":       
            case "ssllistenaddress":    
            case "machine":             
            case "cluster":             
            case "adminserver":         
            case "adminuser":           
            case "adminpass":           
            case "domain":              
            case "domainlocation":      
            case "enabled":             return true;
            
        }
        return false;
    
    }
    
    
    private void init() throws Exception {
        this.baseUrl=( (getBooleanProperty("sslenabled"))?
                                    "https://"+getServerValue("ssllistenaddress","localhost")+":"+getServerValue("ssllistenport","7002")
                                :
                                    "https://"+getServerValue("listenaddress","localhost")+":"+getServerValue("listenport","7001") 
                          );
        
        if ( getServerValue("adminuser") != null && getServerValue("adminpass") != null ) {
             wu = new WlsUser(this.baseUrl, getServerValue("adminuser"),getServerValue("adminpass"));
        }
        
        if ( ht == null ) {
            printf(getFunc("init()"),0, "baseurl :"+this.baseUrl);
            sleep(1000);
            ht = new Http(new URL(this.baseUrl));
        }    
    }
    
    private HashMap<String, String> conMap= new HashMap<String,String>();
    
    public void testAlive() throws Exception{ init(); connect(this.baseUrl); }
    
    public void connect(String url) throws Exception { 
        printf(getFunc("connect(String url)"),0,"url =>"+url+":<=");
        connect(new URL(url)); 
    }
    public void connect(URL url) throws Exception {
        final String func="connect(URL url)";
        init();
        printf(getFunc(func),0," url :"+url); 
        HashMap<String, String> cmap= new HashMap<String,String>();
        cmap.put("startupdate",   ""+System.currentTimeMillis());
        try { 
            ht.connect( url );
            cmap.put("alive",        ""+(ht.getResponseCode() != 503 ));
            cmap.put("responsecode", ""+(ht.getResponseCode()));
            cmap.put("response",         ht.getResponse().toString() );
            cmap.put("headers",          ht.getHeaders());
            cmap.put("error", "");
        } catch(Exception e ) {
            cmap.put("alive", "503");
            cmap.put("error", e.getMessage());
        }
        cmap.put("lastupdate",   ""+System.currentTimeMillis());
        conMap=cmap;
        
    }
    
    public void setTimeout(int time) { ht.setTimeout(time); }
        
    public WlsAdminServer getAdminInstance(){ 
        WlsAdminServer a = new WlsAdminServer(this);
        return a; 
    }
    
    public static void main(String[] args) throws Exception {
        HashMap<String,String> m = new HashMap<String,String> ();
        for (int i=0; i< args.length; i++ ){
            if ( args[i].startsWith("-") ) {  m.put(args[i].substring(1).toLowerCase(), args[++i]);  }
        }
        WlsServer ws = new WlsServer(m);
        System.out.println("WlsServer Info:"+ws);
    }
    
    public Iterator<String> getMapIterator() {
        return map.keySet().iterator();
    }
    @Override
    public String toString() {
        StringBuilder sw = new StringBuilder();
        sw.append("name:").append(map.get("name") );
        Iterator<String> itter = getMapIterator();
        while(itter.hasNext()) {
            String k = itter.next();
            if ( ! k.matches("name") )
                sw.append(" ").append(k).append(":").append(map.get(k));
        }
        return sw.toString();
    }
}
