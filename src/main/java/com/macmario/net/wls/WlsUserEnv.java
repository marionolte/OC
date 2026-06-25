/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.wls;

import com.macmario.general.Version;
import com.macmario.io.crypt.Crypt;
import com.macmario.io.file.ReadDir;
import com.macmario.io.file.WriteFile;
import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

/**
 *
 * @author SuMario
 */
public class WlsUserEnv extends Version {
    private static Crypt crypt=null;
    private static final String sepa="__@__";
    
    public static String updateEnv(String fname, String key) {
    //debug=4;
        final String func="WlsUserEnv::updateEnv(String fname, String key)";
        StringBuilder ret = new StringBuilder();
        WriteFile fn = new WriteFile(fname);
        final Hashtable<String,String> map = new Hashtable<String,String>();
        map.put("wlsUserId", "weblogic");
        map.put("DOMAINHOME", fn.getParent().getAbsolutePath().replaceAll(File.separator, sepa) );
        map.put("ADMINURL", "http://localhost:7001/");
        map.put("ADMINSTOPURL", "t3://localhost:7001/");
        map.put("DOMKEYSFOUND", "false");
        printf(func,2,"fn"+fn.getFQDNFileName()+" are readable:"+fn.isReadableFile());
        if ( fn.isReadableFile() ) {
             WlsDomain wsd = new WlsDomain(fn.getParent().getName()); wsd.debug=debug;
             if ( key.isEmpty() ) {
                try { wsd.setDomainLocation(fn.getParent().getCanonicalPath()); } catch(java.io.IOException io){
                    printf(func,1,"setlocation fail:"+io.getMessage());
                }
                map.put("ADMINURL",     wsd.getAdminUrl()       );
                map.put("ADMINSTOPURL", wsd.getAdminStopUrl()   ); 
                map.put("ADMINSERVER",  wsd.getAdminServerName());
                map.put("ADMINRUNNING", wsd.getAdminOnline()    );
                map.put("MWHOME",       wsd.getMWHome()         );
                map.put("WLHOME",       wsd.getWeblogicHome()   );
                map.put("wlsUserId",    wsd.getAdminUser()      );
                map.put("wlsPassword",  wsd.getAdminPassword()  );
             }
             ArrayList<WlsNodeManager> nmap = wsd.getNodeManagers();
             printf(func,3,"NodeManagers :"+nmap.toString()+":");
             ArrayList<WlsServer> lmap = wsd.getNoneAdminServers();
             printf(func,3,"ManagedServers :"+lmap.toString()+":");
             StringBuilder sm=new StringBuilder();
             while(lmap.size() >0 ) {
                 WlsServer ws = lmap.remove(0); ws.debug=debug;
                 sm.append(ws.getName()).append(",");
                 map.put("SERVER"+ws.getName()+"URL",     ws.getURIString());
                if ( server.isEmpty() || ws.getName().toLowerCase().matches(server)) {
                 map.put("SERVER"+ws.getName()+"RUNNING", ws.getOnline()   );
                }else{
                 map.put("SERVER"+ws.getName()+"RUNNING", "2");
                }
                String ns=ws.getNodeManager();
                 map.put("SERVER"+ws.getName()+"NODE",  ns );
                for ( int j=0; j< nmap.size() ; j++ ) {
                    WlsNodeManager s = nmap.get(j); s.debug=debug;
                    if ( s.getName().matches(ns)) {
                         s.setManagedServer(ws.getName());
                    }
                } 
             }
             map.put("SERVERS",sm.toString());
             map.put("DOMKEYSFOUND", ""+wsd._domainkeyLoaded);
             
             
             sm=new StringBuilder();
             while(nmap.size() >0 ) {
                 WlsNodeManager s = nmap.remove(0);
                 sm.append(s.getName()).append(",");
                 final String   nname="NODE"+s.getName();
                        map.put(nname+"URL",       s.getURIString());
                        map.put(nname+"HOST",      s.getHost()  );
                        map.put(nname+"PORT",   ""+s.getPort()  );
                    if ( server.isEmpty() || s.isManagingServer(server) ) {
                        map.put(nname+"RUNNING",   s.getOnline()   );
                        map.put(nname+"NODEUSER",  s.getNodeManagerUser());
                        map.put(nname+"NODEPASS",  s.getNodeManagerPass());
                    } else {
                        map.put(nname+"RUNNING","2");
                    }
             }
             map.put("NODES",sm.toString());
        
             if( crypt == null ) { crypt = new Crypt(); }
             StringBuilder sw = fn.readOut();
             if ( ! sw.substring(sw.length()-1).matches("=") ) {
                  fn.replace( crypt.getCrypted(sw.toString()));
             } else {
                  sw.replace(0, sw.capacity(), crypt.getUnCrypted(sw.toString()));
             }
             for ( String s : sw.toString().split("\n") ) {
                 String[] sp = s.split("=");
                 if ( key.isEmpty() ) {
                     if      ( sp[0].matches("username") ) {  map.put("wlsUserId",   s.substring(sp[0].length()+1) ); }
                     else if ( sp[0].matches("password") ) {  map.put("wlsPassword", s.substring(sp[0].length()+1) ); }
                     else { map.put(sp[0], s.substring(sp[0].length()+1) ); }
                 } else {
                    if      ( sp[0].matches("username") && key.matches(sp[0]) ) {  ret.append("export wlsUserId=\'"  ).append( s.substring(sp[0].length()+1) ).append("\'"); }
                    else if ( sp[0].matches("password") && key.matches(sp[0]) ) {  ret.append("export wlsPassword=\'").append( s.substring(sp[0].length()+1) ).append("\'"); }
                    else {  }
                 }
             }
             if ( key.isEmpty() ) {
                 Iterator<String> itter = map.keySet().iterator();
                 while(itter.hasNext() ) {
                     String a = itter.next();  String v=map.get(a);
                     if ( ! v.isEmpty() ) {
                          ret.append("export ").append(a).append("=\'").append(v.replaceAll(sepa, File.separator)).append("\'\n");
                     }     
                 }
             }
        }
        return ret.toString();
    }
    
    private static String server="";
    
    public static void setServer(String srv) {
        server = (srv==null || srv.isEmpty() || srv.matches("\\*"))?"":srv.toLowerCase();
    }
    
    public static void main(String[] args) {
        String f=""; String k="";
        if( args.length>0) {
            for( int i=0; i<args.length; i++ ) {
                if ( args[i].matches("-server") ) {
                        if ( args.length > i+1 ) { setServer(args[++i]); }
                        else { setServer("*"); }
                } else {        
                    ReadDir nf = new ReadDir(args[i]);
                    if ( nf.isDirectory() && nf.isReadable() ) { f=nf.getFQDNDirName(); } 
                    else { 
                            k=args[i]; 
                    }        
                }
            }
        }
        
        System.out.println(updateEnv(f+File.separator+"domainkeys",k)); 
    }
}
