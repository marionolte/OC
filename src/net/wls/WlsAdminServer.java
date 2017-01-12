/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.wls;

import java.util.HashMap;
import java.util.Properties;

/**
 *
 * @author SuMario
 */
public class WlsAdminServer extends WlsServer {
    
    public WlsAdminServer(Properties prop) { super(prop,"WlsAdminServer"); }
    public WlsAdminServer(HashMap<String,String> nh) { super(nh,"WlsAdminServer"); }
    
}
