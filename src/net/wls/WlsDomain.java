/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.wls;

import static general.Version.printf;
import io.file.ReadDir;
import io.file.ReadFile;
import io.file.XMLReadFile;
import java.io.File;
import java.util.HashMap;
import main.MainTask;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
/**
 *
 * @author SuMario
 */
public class WlsDomain extends MainTask{
    private ReadDir confdir;
    private XMLReadFile conf;
    public WlsDomain(String[] args) {
       super(args,"WlsDomain");
    }
    
    private void init() {
       final String func="init()";
       confdir = new ReadDir(".");
       String te = getProperty("conf");
       printf(getFunc(func),0,"tmpconf:"+te);
       if (te != null && ! te.isEmpty() ) { confdir=new ReadDir(te); }
       printf(getFunc(func),0,"confDir is "+confdir+"  with parameter "+te);
       if ( confdir.isDirectory() ) {
            conf=new XMLReadFile(confdir.getFQDNDirName()+File.separator+"config"+File.separator+"config.xml");
            if ( ! conf.isReadableFile() ) {
                throw new RuntimeException("ERROR: "+((conf!=null)?conf.getFQDNFileName():"NULL-FILE")+" is not a readable config.xml");
            }
            NodeList nl = conf.getNodeList("server");
            if ( nl != null && nl.getLength() > 0 ) {
                for ( int i=0;  i < nl.getLength() ; i++ ) {
                                 Node n                     = nl.item(i);
                                 HashMap<String, String> nh = conf.getAttributes(n);
                                 String nlm = nh.get("listen-port");
                                 if ( nlm != null && ! nlm.isEmpty() ) {
                                     printf(func,0,"listen port :"+nlm);
                                     //if ( ! nlm.isEmpty() ) { readDomains(nlm); }
                                 }
                             } 
            } else {
                throw new RuntimeException("ERROR: no servers found in config.xml");
            }
                 
       } else {
           throw new RuntimeException("ERROR: "+confdir.getFQDNDirName()+" is not a directory ");
       }
       
          te = getProperty("pwfile");
          if (te != null && ! te.isEmpty() ) {
               ReadFile nf = new ReadFile(te); 
               if ( nf.isReadableFile() ) {
                    String dec=  crypt.getUnCrypted(nf.readOut().toString());
               } else {
                   printf(getFunc(func),0,"ERROR: Couldn't read pwfile:"+nf.getFQDNFileName() );
               }
          } else {
             printf(getFunc(func),0,"WARNING: pwfile not defined"); 
          }
          
    }
    
    public static void main(String[] args) {
        WlsDomain wl = getInstance(args);
    }
    
    public static WlsDomain getInstance(String[] args) {
        WlsDomain wl = new WlsDomain(args);
                  wl.init();
        return wl;
    }
    
    public static WlsDomain getInstance(String confDir, String passwordFile) {
        String[] args = new String[] { "conf", confDir, "pwfile", passwordFile};
        WlsDomain wl = getInstance(args);
        return wl;          
    }
    
    public static WlsDomain getInstance(String confDir) {
        String[] args = new String[] { "conf", confDir };
        WlsDomain wl = getInstance(args);
        return wl;          
    }
}
