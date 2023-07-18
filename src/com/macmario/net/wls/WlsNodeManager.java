/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.wls;

import static com.macmario.general.Version.printf;
import java.util.HashMap;
import com.macmario.main.MainTask;
import com.macmario.net.tcp.ClientSocket;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author SuMario
 */
public class WlsNodeManager extends MainTask {
      private boolean bosted;
      private String  domain;
      private HashMap<String,String> map;
      public WlsNodeManager(String[] args, String dom) {
          super(args, "WlsNodeManager");
          this.bosted=( dom == null || dom.isEmpty() ); 
          this.domain=dom;
          this.map = new HashMap<String,String>();
          this.map.put("name", dom);
          this.map.put("domain", dom);
          this.map.put("listenaddress", "localhost");
      }
    
      public WlsNodeManager(String dom) {
          this(new String[]{}, dom);
      }
      
      public void updateNodeManager(Node m) {
          if ( m== null ) { return; } 
          final String func = getFunc("updateNodeManager(Node n)");
          NodeList nl = m.getChildNodes();
          for ( int i=0; i< nl.getLength(); i++ ) {
               Node n = nl.item(i);
               final String k = n.getNodeName().replaceAll("-", "").toLowerCase();
               boolean b = this.isValidNodeManagerKey(k);
               if ( b ) {
                    printf(func,2,"machine  value:"+n.getNodeName()+"="+n.getNodeValue()+": =>"
                                  +n.getLocalName()+"<= ||"+n.getTextContent()+"|| valid key:"+b);
                    map.put(k, ( (n.getNodeValue()==null) ? n.getTextContent() : n.getNodeValue() ) );
               }
                        
          }
      }
      
      public String getMachineName() { return map.get("name"); }
      
      public boolean isValidNodeManagerKey(String key) { 
        switch(key) {
            case "name":                
            case "listenport":          
            case "listenaddress":       
            case "sslenabled":          
            case "ssllistenport":       
            case "ssllistenaddress":    
            case "adminuser":           
            case "adminpass":           
            case "domain":              
            case "domainlocation":      
            case "enabled":             return true;
            
        }
        return false;
    
    }
    
      
    @Override
    public String toString() { return this.map.toString(); }  
    
    @Override
    public String getName() { String s = this.map.get("name");   return (  (s!=null && ! s.isEmpty() )?s:"unknown") ; }
    public String getURIString(){
        
        //String s = getProperty("enabled"); if ( s==null || s.isEmpty() ) { return null; }
        String        s = this.map.get("sslenabled");
        boolean bs = ( s !=  null && s.matches("true") );
        StringBuilder sw=new StringBuilder( (bs)?"https://":"http://" );
        
        sw.append(getHost()).append(getPort()).append("/");
        return sw.toString(); 
        
    }
    
    public String getURIPort(){ return ""+getPort(); }
    public String getURIHost(){ return ""+getHost(); }    

    public String getHost(){ 
        String        s = this.map.get("sslenabled");
        boolean bs = ( s !=  null && s.matches("true") );
        s = (bs)?this.map.get("ssllistenaddress"):this.map.get("listenaddress");
        return (s!=null && ! s.isEmpty() )?s:"localhost";
    }
    
    public int getPort() {
        String        s = this.map.get("sslenabled");
        boolean bs = ( s !=  null && s.matches("true") );
        String po = (bs)?this.map.get("ssllistenport"):this.map.get("listenport") ;
        return  Integer.parseInt( (po==null || po.isEmpty() )?"5556":po ) ;
    }
    
    public String getOnline() {
        boolean b=false;
        final String func=getFunc("getOnline()");
        try {
            //connect(this.getURIString()); 
            //if (ht.getResponseCode() >=200 ) { b=true; }
            ClientSocket cs = new ClientSocket(getHost(),getPort(),false,true);
                         b=cs.isReachable(1000);
                         cs.setClosed(); cs.close();
        } catch(Exception e) {
            printf(func,1,"is reacheable ends for : "+getHost()+":"+getPort()+" with ERROR:"+e.getMessage());
        }
        
        return (b)?"1":"0";

    }

    private String user="";
    public String getNodeManagerUser() { return this.user; }
    public void   setNodeManagerUser(String u) { this.user=(u==null)?"":u; }

    private String pw="";
    public void   setNodeManagerPass(String pw) {
        if ( pw == null || pw.isEmpty() ) { this.pw=""; return;}
        this.pw=crypt.getCrypted(pw);
    }
    public String getNodeManagerPass() { return (pw.isEmpty())?pw:crypt.getUnCrypted(this.pw); }
    
    public boolean isManagingServer(String srv) {
        if ( srv == null || srv.isEmpty() ) { return false; }
        return ( smap.get(srv) != null );
    }
    private HashMap<String,String> smap=new HashMap<String,String>();
    public void setManagedServer(String srv) {
        if ( srv == null || srv.isEmpty() ) { return; }
        smap.put(srv, srv);
    }
}
