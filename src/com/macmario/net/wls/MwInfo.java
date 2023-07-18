/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.wls;

import com.macmario.general.Version;
import com.macmario.io.file.ReadDir;
import com.macmario.io.file.ReadFile;
import com.macmario.io.file.XMLReadFile;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author SuMario
 */
public class MwInfo extends Version {
    private static final String home;
    private static final String localdir;
    
    public static String usage() {
        return "[-dest <script dir ["+System.getProperty("user.home")+File.separator+"bin]>] [-reconfig] [-silient] <domaindir <domaindir1...>>";
    }
    
    private static ArrayList<ReadDir> getMWDirs(String[] ulist) {
        ArrayList<ReadDir> ar = new ArrayList<ReadDir>();
            for ( String s : ulist) {
                if ( ! s.isEmpty() ) {
                    ReadDir d = new ReadDir(s);
                    if ( d.isDirectory() ) { ar.add(d); }
                }
            }
            String[] sp = new String[]{ "/etc/oraInst.loc", home+File.separator+"oraInventory","C:\\Program Files\\Oracle\\Inventory" };
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
            ar.add(new ReadDir(home));
            if ( ! home.equals(localdir) ) { ar.add( new ReadDir(localdir)); }
            return ar;
    }
    
    private static ArrayList<ReadDir> getInstallDirectories(ArrayList<ReadDir> ar) {
        final String func=getFu("getInstallDirectories(ArrayList<ReadDir> ar)");
        ArrayList<ReadDir> arr = new ArrayList<ReadDir>();
        printf(func,3,"check "+ar.size()+" directories");
        while ( ar.size() > 0 ) {
           ReadDir  d = ar.remove(0);
           
           
           try {  
             
             String[] fp= d.getFiles("inventory.xml|beahomelist", true);
             printf(func,3,"found "+fp.length+" files");
             for ( String f : fp ) {   
                 printf(func,2,"check file :"+d.getParent()+File.separator+f);
                 if ( f.contains("inventory") && ! f.contains(".patch") && ! f.contains("oneoffs") && ! f.contains("backup")  ) {
                         printf(func,2,"scan find file :"+d.getParent()+File.separator+f);

                         XMLReadFile xm = new XMLReadFile(d.getParent()+File.separator+f);
                            NodeList xn = xm.getNodeList("HOME");
                            if ( xn != null && xn.getLength() > 0  ) {
                                for ( int i=0;  i < xn.getLength() ; i++ ) {
                                    Node n                     = xn.item(i);
                                    HashMap<String, String> nh = xm.getAttributes(n);
                                    xm.getAttributes(n);

                                    String nl = nh.get("LOC");
                                    if ( nl != null && ! nl.isEmpty() ) {
                                        printf(func,2,"find location :"+nl);
                                        if ( ! nl.isEmpty() ) { 
                                            printf(func,1,"add location :"+nl);
                                            arr.add(new ReadDir(nl)); 
                                        }
                                    }
                                } 
                            }
                            else if ( f.contains("beahomelist")) {
                                ReadFile fn = new ReadFile(f); 
                                for (String s : fn.readOut().toString().split(";") ) {
                                    printf(func,1,"find location :"+s);
                                    if ( ! s.isEmpty() ) { 
                                        printf(func,1,"add location :"+s);
                                        arr.add(new ReadDir(s)); 
                                    }
                                }
                            }

                    }
             }
           } catch(Exception e) {
               printf(func,1,"run in error :"+e.getMessage(),e);
           }  
        }   
        printf(func,1, "return "+arr.size()+" elements");
        return arr;
    }
    
    private static StringBuilder readDomains(String mwhome) {
        final String func=getFu("readDomains(String mwhome)");
        ArrayList<String> m = new ArrayList();  
                          m.add(mwhome+File.separator+"Opatch"+File.separator+"opatch");
                          m.add("lsinventory");
        String opatch = com.macmario.io.lib.IOLib.launch(m);
                                            
        StringBuilder sw = new StringBuilder();
        XMLReadFile nf = new XMLReadFile(mwhome+File.separator+"domain-registry.xml");
        NodeList    xn = nf.getNodeList("domain");
        if ( xn != null && xn.getLength() > 0  ) {
            for ( int i=0;  i < xn.getLength() ; i++ ) {
                  Node n                     = xn.item(i);
                  //printf(func,0,"domain:"+n);
                  HashMap<String, String> nh = nf.getAttributes(n);
                  String nl = nh.get("location");
                  if ( nl != null && ! nl.isEmpty() ) {
                        printf(func,2,"find domain location :"+nl);
                        try { 
                             sw.append("Domain ").append(nl).append("\n");
                             WlsDomainInfo w = new WlsDomainInfo(nl);
                                           w.setOpatch(opatch);
                             printf(func,2,"domain:"+w);
                             sw.append(w.getDomainInfo());
                             sw.append("#####\n\n");
                        }catch(Exception re) {
                                         printf(func,0,"no domain location :"+nl);
                                         if ( debug > 0 ) {
                                             re.printStackTrace();
                                         }
                        } //no domain entry
                        
                  }

            }
        } 
        return sw; 
    }    
    
    
    public synchronized static String info(String[] ulist) {
            StringBuilder sw = new StringBuilder();
                                   
            for (ReadDir s:getInstallDirectories(getMWDirs(ulist)) ) {
                   sw.append("found Middleware Location ").append(s.getFQDNDirName()).append(" \n");
                   sw.append(readDomains(s.getFQDNDirName()));
            }
        return sw.toString();
    }
    
    public static void main(String[] args) {
         System.out.println(info(args));
    }
    
    static {
        home=System.getProperty("user.home");
        localdir=System.getProperty("user.dir");
    }
    
    static private String getFu(String func){ return "net.wls.MwInfo::"+func; }
}
