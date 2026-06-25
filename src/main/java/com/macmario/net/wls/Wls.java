/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.wls;

import com.macmario.io.file.ReadDir;
import com.macmario.io.file.ReadFile;
import com.macmario.io.file.WriteFile;
import com.macmario.io.file.XMLReadFile;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import com.macmario.main.MainTask;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author SuMario
 */
public class Wls extends MainTask{
    private final HashMap<String, WlsDomain> wlsd;
    
    public Wls(String[] args) {
        super(args,"Wls");
        wlsd = new HashMap<String, WlsDomain>();
    }
    
    public void scan() throws Exception {
        final String func=getFunc("scan()");
        String udir=System.getProperty("user.dir");
        ArrayList<ReadDir> ar = new ArrayList<ReadDir>();
        
        String ma = getProperty("conf");
        printf(func,3,"scan conf file :"+ma);
                      
        if ( ma != null && ! ma.isEmpty() ) {
            for ( String s : ma.split(",") ) {
                if ( ! s.isEmpty() ) {
                    ReadDir d = new ReadDir(s);
                    if ( d.isDirectory() ) { ar.add(d); }
                }
            }
        } else {
            // central oraInventory 
            String[] sp = new String[]{ "/etc/oraInst.loc", udir+File.separator+"oraInventory","C:\\Program Files\\Oracle\\Inventory" };
            for (String s:sp) {
                ReadDir d = new ReadDir(s);
                if ( d.isDirectory() ) { ar.add(d); }
                else if ( d.isFile() ) {
                    Properties p = (new ReadFile(s)).getProperties(); 
                    if ( p.getProperty("inventory_loc") != null ) {
                         d=new ReadDir(p.getProperty("inventory_loc"));
                         if ( d.isDirectory() ) { ar.add(d); }
                    }
                }
            }
            // new ReadFile("");
        }
        if ( ar.size() == 0 ) { ar.add( new ReadDir(udir) ); }
        printf(func,0,"check "+ar.size()+" directories");
        while ( ar.size() > 0 ) {
             ReadDir  d = ar.remove(0);
             String[] fp= d.getFiles("inventory.xml|beahomelist", true);
             printf(func,0,"found "+fp.length+" files");
             for ( String f : fp ) {                 
                    printf(func,0,"check file :"+d.getParent()+File.separator+f);
                    if ( f.contains("inventory") && ! f.contains(".patch") && ! f.contains("oneoffs") && ! f.contains("backup")  ) {
                         printf(func,0,"scan find file :"+d.getParent()+File.separator+f);

                         XMLReadFile xm = new XMLReadFile(d.getParent()+File.separator+f);
                            NodeList xn = xm.getNodeList("HOME");
                            if ( xn != null && xn.getLength() > 0  ) {
                                for ( int i=0;  i < xn.getLength() ; i++ ) {
                                    Node n                     = xn.item(i);
                                    HashMap<String, String> nh = xm.getAttributes(n);
                                    xm.getAttributes(n);

                                    String nl = nh.get("LOC");
                                    if ( nl != null && ! nl.isEmpty() ) {
                                        printf(func,0,"find location :"+nl);
                                        if ( ! nl.isEmpty() ) { readDomains(nl); }
                                    }
                                } 
                            }

                    }
                    else if ( f.contains("beahomelist")) {
                         ReadFile fn = new ReadFile(f); 
                         for (String s : fn.readOut().toString().split(";") ) {
                             if ( ! s.isEmpty() ) { readDomains(s); }
                         }
                    }
             }      
        }
    }
    
    public WlsDomain getDomain(String dname) {
         if (  wlsd.isEmpty() ) { try { scan(); } catch(Exception e){} }
         return wlsd.get(dname);
    }
    
    private void readDomains(String mwhome) throws Exception {
        final String func=getFunc("readDomains(String mwhome)");
        XMLReadFile nf = new XMLReadFile(mwhome+File.separator+"domain-registry.xml");
        NodeList    xn = nf.getNodeList("domain");
        if ( xn != null && xn.getLength() > 0  ) {
            for ( int i=0;  i < xn.getLength() ; i++ ) {
                                 Node n                     = xn.item(i);
                                 printf(func,0,"domain:"+n);
                                 HashMap<String, String> nh = nf.getAttributes(n);
                                 String nl = nh.get("location");
                                 if ( nl != null && ! nl.isEmpty() ) {
                                     printf(func,2,"find domain location :"+nl);
                                     try { 
                                         WlsDomain w = WlsDomain.getInstance(nl);
                                         printf(func,2,"domain:"+w);
                                         if ( getBooleanProperty("test") ) { w.testAlive(); }
                                         wlsd.put(w.getDomainName(), w);
                                         
                                     }catch(RuntimeException re) {
                                         printf(func,1,"no domain location :"+nl);
                                         if ( debug > 1 ) {
                                             re.printStackTrace();
                                         }
                                     } //no domain entry
                                 }
            } 
            
        }
    }
    
    private String store() {
        StringBuilder sw = new StringBuilder();
        Iterator<String> itwd = wlsd.keySet().iterator();
        while( itwd.hasNext() ) {
            WlsDomain wd = wlsd.get(itwd.next());
                      sw.append("[domain:{domainname:").append(wd.getDomainName()).append(" ");
                      sw.append(wd.store());
                      sw.append("}\n");
                      
        }
        return sw.toString();
    }
    
    private String usage="";
    public void run() throws Exception {
        final String func=getFunc("run()");
        if      ( isCommand("VERSION") ) { System.out.println("WlsDomainChecker - "+Wls.getFullInfo()); exitCode=0; }
        else if ( isCommand("USAGE")   ) { System.out.println(usage); exitCode=0; }
        else if ( isCommand("SCAN")    ) { 
            
            scan();
            String fn = System.getProperty("user.dir")+File.separator+".wlsdomains.propertie";        
            if ( getProperty("storefile")!=null ) {fn=getProperty("storefile"); }
            
            printf(func,0,"like to store ? =>"+getBooleanProperty("store")+"<= to file:"+fn );
            
            if ( getBooleanProperty("store")) {
                String store = store();
                printf(func,0,"like to store =>"+store+"<= to file:"+fn );
                (new WriteFile(fn)).replace(store);
            }
            exitCode=0;
        }
        else {
            System.out.println(usage); exitCode=-1; 
        }
        
        
    }
    
    public static void main(String[] args) throws Exception {
        Wls w = new Wls(args);
            w.run();
        System.exit(w.exitCode);
    }

    
    
}
