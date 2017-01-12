/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.wls;

import io.file.ReadDir;
import io.file.ReadFile;
import io.file.XMLReadFile;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import main.MainTask;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author SuMario
 */
public class Wls extends MainTask{
    
    public Wls(String[] args) {
        super(args,"Wls");        
    }
    
    public void scan() {
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
    
    private void readDomains(String mwhome) {
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
                                         printf(func,0,"domain:"+w);
                                     }catch(RuntimeException re) {
                                         printf(func,1,"no domain location :"+nl);
                                     } //no domain entry
                                 }
            } 
            
        }
    }
    
    private String usage="";
    public void run() {
        if      ( isCommand("VERSION") ) { System.out.println("WlsDomainChecker - "+Wls.getFullInfo()); exitCode=0; }
        else if ( isCommand("USAGE")   ) { System.out.println(usage); exitCode=0; }
        else if ( isCommand("SCAN")    ) { scan(); }
        else {
            System.out.println(usage); exitCode=-1; 
        }
    }
    
    public static void main(String[] args) {
        Wls w = new Wls(args);
            w.run();
        System.exit(w.exitCode);
    }

    
    
}
