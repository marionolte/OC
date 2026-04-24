/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.wls;

import com.macmario.io.file.ReadFile;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import com.macmario.main.MainTask;

/**
 *
 * @author SuMario
 */
public class WlsRollingRestart extends MainTask{

    private final int _rollingWaitTime;
    private final WlsDomain domain;
    private final HashMap<String, String> rmap = new HashMap<>();
    
    public WlsRollingRestart(String[] args) {
        super(args,"WlsRollingRestart");
        final String func=getFunc("WlsRollingRestart(String[] args)");
        if ( getProperty("domain", "none").matches("none") ) {
            Wls w = new Wls( new String[]{"-scan"});
            this.domain = w.getDomain(getProperty("domain"));         // = new WlsDomain( new String[] { "-conf", getProperty("domain") } );
        } else {
            WlsDomain.debug=debug;
            this.domain = new WlsDomain(getProperty("domain"));
        }    
        printf(func,2,"domain:"+getProperty("domain")+": loc:"+getProperty("domainlocation","none")+":");
        if ( getProperty("domainlocation","none").matches("none") ) {
                ReadFile f = new ReadFile(  (System.getProperty("user.dir")+File.separator+"location"));
                String[] sp = f.readOut().toString().split("\n");
                final String dom = getProperty("domain");
                for(String s: sp) {
                    String[] fp = s.trim().split(";");
                    if ( fp[0].matches(dom)) {
                        printf(func,2,"readout for domain:"+getProperty("domain")+": loc:"+fp[1]+":  from"+f.getFQDNFileName());
        
                        setProperty("domainlocation", fp[1]);
                        this.domain.setDomainLocation(fp[1]);
                    }
                }
        } else {
                 printf(func,2,"take domain:"+getProperty("domain")+": domainlocaltion:"+getProperty("domainlocation"));
                 this.domain.setDomainLocation(getProperty("domainlocation"));
        }
        
        this._rollingWaitTime= getIntProperty("waittime", "30000");
        printf(func,2,"domain-info:"+this.domain);
        
        if ( this.domain != null ) {
            readDomain();
        }
    }
    
    private boolean _domainRead=false;
    private HashMap<String, WlsServer> smap;
    public boolean readDomain() {
        final String func=getFunc("readDomain()");
        boolean b = _domainRead;
        if ( _domainRead ) { return _domainRead; }
        printf(func,2,"domain:"+this.domain);
        
        setProperty("domainlocation", this.domain.getDomainLocation() );
        
        smap = this.domain.getServers();
        _domainRead = ( smap != null && ! smap.isEmpty() );
        
        for (String a : getProperty("server","all").toLowerCase().split(",") ) {
            if ( a.matches("all") ) {
               Iterator<String> itter = smap.keySet().iterator();
               while( itter.hasNext() ) {
                   rmap.put(itter.next(), "true");
               }    
            } else {
               Iterator<String> itter = smap.keySet().iterator();
               while( itter.hasNext() ) {
                   String c = (itter.next());
                   if ( c.toLowerCase().matches(a) ) {
                        rmap.put(c, "true");
                   }     
               }  
            }
        }
        
        return _domainRead;
    }
    
    public WlsServer getAdminServer() {
        WlsServer w=null;
        if ( readDomain() ) {
             Iterator<String> itter = smap.keySet().iterator();
             outter:
                while( itter.hasNext() ) {
                    WlsServer s = smap.get(itter.next());
                    if ( s != null && s.isAdminServer() ) {
                        w=s;
                        break outter;
                    }
                }
        }
        return w;
    }
    
    public boolean rolling() {
        boolean b = readDomain();
        if ( b ) {
             b=false;
             WlsAdminServer adm = null;
             ArrayList<WlsServer> ws = new ArrayList<WlsServer>();
             Iterator<String> itter = smap.keySet().iterator();
             while(itter.hasNext()) {
                 WlsServer s = smap.get(itter.next());
                 if ( s != null ) {
                    if ( s.isAdminServer() ) {
                        adm = s.getAdminInstance();
                        String f = rmap.get( s.getName() );
                        if ( f != null &&  f.matches("true") ) { }
                        updateAdmOnline(adm);
                        
                    } else {
                       String f = rmap.get( s.getName() ); 
                       if ( f != null &&  f.matches("true") ) { // || s.isRunning() ) { 
                           ws.add(s); 
                       } else {
                           // System.out.println("INFO: wls server: "+s.getServerValue("name")+" is down - leave untouched");
                       }
                    }
                 }
             }
             if ( adm != null ) {
                 if ( rmap.get(adm.getServerValue("name")).matches("true") ) {
                    if ( adm.isRunning() ) {
                         adm.stopping();
                    }
                    adm.starting();
                 }    
                 
                 while ( ! ws.isEmpty() ) {
                      WlsServer s = ws.remove(0);
                                s.stopping();
                                s.starting();
                 }
                 
             } else {
                 System.out.println("ERROR: wls admin server is not readable from config file");
                 this.exitCode=-1;
             }
        } else {
            System.out.println("ERROR: couldn't read domain:"+this.domain.getDomainName()+" from domain location:"+domain.getProperty("domainlocation"));
            this.exitCode=-1;
        }
        return b;
    }
    
    private void updateAdmOnline(WlsAdminServer adm) {
        adm.getStatus();
    }
    
    public static void main(String[] args) {
        WlsRollingRestart w = new WlsRollingRestart(args);
          System.out.println("Rooling Restart "+  ((w.rolling())?"done":"faild") );
        
    }
    
}
