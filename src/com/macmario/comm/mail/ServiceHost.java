/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.comm.mail;

import com.macmario.net.tcp.Host;

/**
 *
 * @author SuMario
 */
public class ServiceHost extends Host{
    int timeout=30000;
    int port=-1;
    ServiceHost(String service, String host) {
        
    }
    
    void setPort(int port) { setPort(""+port);}
    void setPort(String port) { if(isPort(port)){ this.port=getPort(port);} }
}
