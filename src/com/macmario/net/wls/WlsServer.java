/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.wls;

import com.macmario.net.tcp.TcpHost;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import com.macmario.io.net.Http;
import com.macmario.main.MainTask;
import com.macmario.net.tcp.ClientSocket;

/**
 *
 * @author SuMario
 */
public class WlsServer extends TcpHost{
    private final HashMap<String,String> map;
    private Http    ht=null;
    private String  baseUrl=null;
    final WlsDomain dom;
    
    WlsServer(WlsServer ws , String name , WlsDomain dom) {
        super(null,name);
        this.map=ws.map; 
        this.dom=dom;
    }
    public WlsServer(HashMap<String,String> nh, WlsDomain dom) throws Exception { this(nh,"WlsServer",dom); }
    public WlsServer(HashMap<String,String> nh,String name, WlsDomain dom) throws Exception {   
        super(null,name); this.dom=dom;
        final String func=getFunc("WlsServer(HashMap<String,String> nh,String name)");
        this.map = new HashMap<String,String>();
        Iterator<String> itter = nh.keySet().iterator();
        map.put("sslenabled", "false");
        while(itter.hasNext()) {
            String k = itter.next().toLowerCase();
            printf(func,3,"get key="+k+"|  value =>|"+nh.get(k)+"|<=");
            if      ( k.equals("#text") ) {}
            else if ( k.equals("ssl")   ) {
                String val = nh.get(k);
                String nam = nh.get("name");
                printf(func,3,"ssl value:"+val);
                if ( val != null & ! val.isEmpty() ) {
                     for(String valk : val.split("\n") ) {
                         valk =valk.replaceAll("\t", "").replaceAll(" ","");
                         if ( ! valk.isEmpty() ) {
                             printf(func,3,"k:"+k+": valk ="+valk+"|");
                             String m = "";
                             if ( valk.equals(nam)     ) { m="sslname";     }
                             else if ( isBoolean(valk) ) { m="sslenabled";  }
                             else if ( isPort(valk)    ) { m="ssllistenport";     }
                             else if ( isHostIp(valk)  ) { m="ssllistenaddress";  }
                             
                             if ( isValidServerKey(m) ) { 
                                      printf(func,2,"ssl key:"+m+":  value:"+valk+":");
                                      map.put(m, valk);
                             } 
                         }    
                     }
                }
            }
            else if ( k.endsWith("listen-port")) { 
                printf(func,2,"listen if :: k:"+k+": valk ="+nh.get(k)+"|");
                map.put(k.replaceAll("-", ""), nh.get(k));
            }
            else if ( k.endsWith("listen-address")) { 
                printf(func,2,"listen if :: k:"+k+": valk ="+nh.get(k)+"|");
                map.put(k.replaceAll("-", ""), nh.get(k));
                if( map.get("ssllistenaddress") == null ) { map.put("ssllistenaddress",nh.get(k)); }
            }
            else {
                final String m=k.replaceAll("-", "");
                printf(func,3,"else :: key:"+m+":  have =>"+nh.get(k));
                if ( isValidServerKey(m) ) { //&& nh.get(k) != null )  { 
                    printf(func,2,"else key:"+m+":  value:"+nh.get(k)+":");
                    map.put(m, nh.get(k)); 
                }
            }    
        }
        init();
    }
    
    public WlsServer(Properties prop, String name) throws Exception {
        super(null,name);  this.dom=new WlsDomain("name");
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
        final String func=getFunc("getServerValue(String key, String def)");
        printf(func,2,"key:"+((key==null)?"NULL":key)+" def:"+((def==null)?"NULL":def)+":");
        if ( key == null || key.isEmpty() ) { return def; } 
        final String k = key.toLowerCase();
        String val=this.map.get(k);
        if ( val == null ) { val=def; }
        if ( val == null ) { return def; }
        if ( k.contains("pass") && ! val.isEmpty() ) {
            return getReplaceSeparatorBack(crypt.getUnCrypted(val));
        }
        printf(func,2,"key:"+key+": =>"+val+"<=");
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
    
    public void   setAdminUser(String u) { this.map.put("adminuser", u); }
    public void   setAdminPass(String p) { this.map.put("adminpass", p); }
    
    public String getAdminServerHost() {
        return null;
    }
    public boolean isAdminServer() {
        final String s = this.map.get("adminserver");
        return ( s != null &&  s.matches("true") );
    }
    
    public String getNodeManager() {
        final String s = this.map.get("machine");
        return ( s == null )? "":s ;
    }
    
    @Override
    public String getName() { String s = getServerValue("name");   return (  (s!=null && ! s.isEmpty() )?s:"unknown") ; }
    public String getURIString(){
        
        //String s = getProperty("enabled"); if ( s==null || s.isEmpty() ) { return null; }
        String        s = getServerValue("sslenabled");
        boolean bs = ( s !=  null && s.matches("true") );
        StringBuilder sw=new StringBuilder( (bs)?"https://":"http://" );
               s = (bs)?getServerValue("ssllistenaddress","localhost"):getServerValue("listenaddress","localhost");
              sw.append( (s==null||s.isEmpty())?"localhost":s  );
               s = (bs)?getServerValue("ssllistenport"):getServerValue("listenport");
               
              sw.append( ( (s==null || s.isEmpty())?"/":":"+s+"/")   );
              
        return sw.toString(); 
        //return this.baseUrl+"/";
    }
    
    public String getHost(){ 
        String        s = getServerValue("sslenabled");
        boolean bs = ( s !=  null && s.matches("true") );
        return (bs)?getServerValue("ssllistenaddress","localhost"):getServerValue("listenaddress","localhost");
    }
    
    public int getPort() {
        String        s = getServerValue("sslenabled");
        boolean bs = ( s !=  null && s.matches("true") );
        return  Integer.parseInt( (bs)?getServerValue("ssllistenport"):getServerValue("listenport") ) ;
    }
    
    public boolean isRunning() {
        final String func=getFunc("isRunning()");
        boolean b = false;
        String u = getURIString();
        printf(func,3,"server:"+getName()+" uri =>"+u+"<=");
        try { 
            Http ht = new Http(new URL(u) );
                 
                 ht.setTimeout(3000);  ht.setTrustAll();
                 ht.connect();
                 printf(func,2,"connection ends with :"+ht.getResponseCode());
                 if ( ht.getResponseCode() != -1 ) { b=true; }
                 
        } catch(Exception io) {
            printf(func,1,"server:"+getName()+" uri =>"+u+"<=  produce errror:"+io.getMessage());
            io.printStackTrace();
        }    
        return b;
    }
    
    private void init() throws Exception {
        /*this.baseUrl=( (getBooleanProperty("sslenabled"))?
                                    "https://"+getServerValue("ssllistenaddress","localhost")+":"+getServerValue("ssllistenport","7002")
                                :
                                    "https://"+getServerValue("listenaddress","localhost")+":"+getServerValue("listenport","7001") 
                          );
        */
        this.baseUrl=this.getURIString();
        
        if ( getServerValue("adminuser") != null && getServerValue("adminpass") != null ) {
             dom.wu = new WlsUser(this.baseUrl, getServerValue("adminuser"),getServerValue("adminpass"));
        }
        
        if ( ht == null ) {
            printf(getFunc("init()"),2, "baseurl :"+this.baseUrl);
            sleep(1000);
            ht = new Http(new URL(this.baseUrl));
        }    
    }
    
    private HashMap<String, String> conMap= new HashMap<String,String>();
    
    public void testAlive() throws Exception{ init(); connect(this.baseUrl); }
    
    public void connect(String url) throws Exception { 
        printf(getFunc("connect(String url)"),2,"url =>"+url+":<=");
        connect(new URL(url)); 
    }
    public void connect(URL url) throws Exception {
        final String func="connect(URL url)";
        init();
        printf(getFunc(func),2," url :"+url); 
        HashMap<String, String> cmap= new HashMap<String,String>();
        cmap.put("startupdate",   ""+System.currentTimeMillis());
        try { 
            ht.setTimeout(3000);
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
    
    public void starting() {
        final String func=getFunc("starting()");
        if ( isAdminServer() ) {
            printf(func,0,"request to start wls admin severer:"+getName()+" on server:");
        } else {
            printf(func,0,"request to start wls severer:"+getName());
        }
    }
    
    public String getOnline() {
        boolean b=false;
        final String func=getFunc("getOnline()");
        try {
            //connect(this.getURIString()); 
            //if (ht.getResponseCode() >=200 ) { b=true; }
            ClientSocket cs = new ClientSocket(getHost(),getPort(),false, true);                        
                         b=cs.isReachable(1000);
                         cs.setClosed(); cs.close();
        } catch(Exception e) {
            printf(func,1,"is reacheable ends for : "+getHost()+":"+getPort()+" with ERROR:"+e.getMessage());
        }
        
        return (b)?"1":"0";
    }
    
    public void stopping() {
        final String func=getFunc("stopping()");
    }
    
    public void setTimeout(int time) { ht.setTimeout(time); }
        
    public WlsAdminServer getAdminInstance(){ 
        if ( isAdminServer() ) {
            WlsAdminServer a = new WlsAdminServer(this, dom);
            return a;
        }
        return null; 
    }
    
    public static void main(String[] args) throws Exception {
        HashMap<String,String> m = new HashMap<String,String> ();
        for (int i=0; i< args.length; i++ ){
            if ( args[i].startsWith("-") ) {  m.put(args[i].substring(1).toLowerCase(), args[++i]);  }
        }
        //WlsServer ws = new WlsServer(m);
        //System.out.println("WlsServer Info:"+ws);
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
