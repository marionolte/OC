/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.wls;

import java.net.URL;
import java.util.HashMap;
import java.util.Properties;
import main.Http;

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
    public WlsAdminServer(HashMap<String,String> nh) throws Exception { super(nh,"WlsAdminServer");  }
    
    WlsAdminServer(WlsServer ws) { super(ws,"WlsAdminServer"); }
    
    private void init(){
      if ( _init ) { return; }  
      try {  
        ht = new Http( new URL(this.getURIString()+this.baseAdminUrl) );
        _init=true;
      }catch(Exception e) {}  
    }
    
    public void connect() {
        init();
        try {
            ht.connect();
            login(); 
                
            
        } catch(java.io.IOException io) {
            
        }
    }
    
    private boolean login() throws java.io.IOException {
        final String _login="value='Login'";
        if ( ht.verify(_login) ) {
             ht.setPost("J_username",           wu.getUsername());
             ht.setPost("j_password",           wu.getPassword());
             ht.setPost("j_character_encoding", "UTF-8");
             ht.connect(new URL(this.getURIString()+baseAdminUrl+"j_security_check"));
             return ! ht.verify(_login);
        }
        return true;
    }
    
    
    public void getStatus() {
        connect();
        try { 
             ht.connect(new URL(this.getURIString()+baseAdminUrl+serverStatControl));
             System.out.println("state =>|"+ht.getResponse().toString()+"|<=");
        } catch(java.io.IOException io) {
        }     
    }
}
