/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.io.wls;

import com.macmario.main.MainTask;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 *
 * @author SuMario
 */
public class Wls extends MainTask{
    public Wls(String[] args) {
        super(args,"Wls");
        setProperties(parseArgs(args));
    }
    
    public void wlsTest() {
        
    }
    
    public void wlsCheckOut(){
        
    }
    
    public void wlsUpdate() {
        System.out.println("wlsUpdate"); 
        loadProperty(new String[]{"connect","comn","c"});
        loadProperty(new String[]{"property","prop","p"});
        
    }
    
    public synchronized StringBuilder startWLSTRemote(String f) {
        Properties p = getIMapProperties(new String[]{"connect","comn","c"});
        
        return startWLST(f);
    }
    public synchronized StringBuilder startWLST(String f) {
            
            String dom = System.getenv("DOMAINHOME");
            if ( dom == null || dom.isEmpty() ) {
                 dom = System.getProperty("DOMAINHOME");
            }
            if ( dom == null || dom.isEmpty() ) {
                 Properties p = getIMapProperties(new String[]{"connect","comn","c"});
                 dom = getProperty("DOMAINHOME","",p);
            }
            Process p = null;
            ProcessBuilder pb = new ProcessBuilder("bash", "-c","( cd "+dom+" &&  . ./bin/setDomainEnv.sh && java weblogic.WLST "+f+" 2>&1 )");
            //pb.directory(new File (wls.getDomainLocation() ) );
            try {
                p = pb.start(); 
            } catch(java.io.IOException ioe) {
                
            }
            
            InputStreamReader isr = new InputStreamReader(p.getInputStream());
            BufferedReader br = new BufferedReader(isr);
            String line;
            StringBuilder sw = new StringBuilder();
            try {
                while ((line = br.readLine()) != null) {
                    sw.append(line.trim()).append("\n");
                }
            } catch(java.io.IOException ioe) {
                
            }     
            return sw;
    }
    
    public static void main(String[] args) {
       Wls w = new Wls(args);   
           if ( w.isCommand("TEST") ) { w.wlsTest(); }
           else if ( w.isCommand("CHECKOUT") ) { w.wlsCheckOut(); }
           else if ( w.isCommand("UPDATE")   ) { w.wlsUpdate();}
    }

    
}
