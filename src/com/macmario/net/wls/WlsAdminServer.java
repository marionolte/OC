/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.wls;

import java.net.URL;
import java.util.HashMap;
import java.util.Properties;
import com.macmario.io.net.Http;

/**
 *
 * @author SuMario
 */
public class WlsAdminServer extends WlsServer {
    private String baseAdminUrl="/console/";
    private String serverStatControl="console.portal?_nfpb=true&_pageLabel=WLSServerControlTablePage";
    private Http ht=null;
    private boolean _init=false;
    
    public WlsAdminServer(Properties prop) throws Exception { super(prop,"WlsAdminServer"); }
    public WlsAdminServer(HashMap<String,String> nh,WlsDomain dom) throws Exception { super(nh,"WlsAdminServer",dom);  }
    
    WlsAdminServer(WlsServer ws,WlsDomain dom) { super(ws,"WlsAdminServer", dom);  }
    
    private void init(){
      if ( _init ) { return; }  
      try {  
        ht = new Http( new URL(this.getURIString()+this.baseAdminUrl) );
        _init=true;
      }catch(Exception e) {}  
    }
    
    public boolean connect() {
        init();
        try {
            ht.connect();
            return login() ;
        } catch(java.io.IOException io) {
            
        }
        return false;
    }
    
    private boolean login() throws java.io.IOException {
        final String _login="value='Login'";
        if ( dom.wu == null || dom.wu.getUsername() == null || dom.wu.getPassword() == null ) { return false; }
        if ( ht.verify(_login) ) {
             ht.setPost("J_username",           dom.wu.getUsername());
             ht.setPost("j_password",           dom.wu.getPassword());
             ht.setPost("j_character_encoding", "UTF-8");
             ht.connect(new URL(this.getURIString()+baseAdminUrl+"j_security_check"));
             return ! ht.verify(_login);
        }
        return true;
    }
    
    
    public void getStatus() {
        final String func=getFunc("getStatus()");
        connect();
        try { 
             printf(func,1,"like to connect:"+(this.getURIString()+baseAdminUrl+serverStatControl));
             ht.connect(new URL(this.getURIString()+baseAdminUrl+serverStatControl));
             System.out.println("state =>|"+ht.getResponse().toString()+"|<=");
        } catch(java.io.IOException io) {
        }     
    }
    
    public String getAdminUrl(){ return this.getURIString(); }
    
    public String getAdminStopUrl() {
        return getAdminUrl().replaceAll("^http", "t3");
    }
    
    public String getAdminServerName(){  return this.getName(); }
}
