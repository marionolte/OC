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
import java.util.Iterator;
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
    private final HashMap<String, WlsServer> servers; 
    private String _domainname;
    public WlsDomain(String[] args) {
       super(args,"WlsDomain");
       final String func="WlsDomain(String[] args)";
       servers = new  HashMap<String, WlsServer>();
       printf(getFunc(func),3,"conf:"+getProperty("conf"));
       if ( debug >= 3 ) {
            int i=0;
            for(String s : args) {
                printf(getFunc(func),3," value["+i+"/"+args.length+"] :"+args[i++] );
            }
       }
    }
    
    private void init() {
       final String func="init()";
       confdir = new ReadDir(".");
       String te = getProperty("conf");
       printf(getFunc(func),3,"tmpconf:"+te);
       if (te != null && ! te.isEmpty() ) { confdir=new ReadDir(te); }
       printf(getFunc(func),2,"confDir is "+((confdir!=null)?confdir.getAbsolutePath():"NULL")+"  with parameter "+te);
       if ( confdir.isDirectory() ) {
            conf=new XMLReadFile(confdir.getFQDNDirName()+File.separator+"config"+File.separator+"config.xml");
            if ( ! conf.isReadableFile() ) {
                te = getProperty("initfile");
                if ( te == null || te.isEmpty() )
                    throw new RuntimeException("ERROR: "+((conf!=null)?conf.getFQDNFileName():"NULL-FILE")+" is not a readable config.xml");
            } else {
                NodeList nl = conf.getNodeList("domain");
                HashMap<String, String> domh = conf.getAttributes(nl.item(0));
                if ( domh.get("name") == null ) {
                    conf.nodeReadout(nl,domh);
                }
                this._domainname = domh.get("name");
                
                         nl = conf.getNodeList("server");
                HashMap<String, HashMap<String, String>> nmh = new HashMap<String, HashMap<String, String>>();
                if ( nl != null && nl.getLength() > 0 ) {
                    printf(getFunc(func),3,"INFO: "+nl.getLength()+" server[s] found ");
                    boolean adminFound=false;
                    for ( int i=0;  i < nl.getLength() ; i++ ) {
                                     Node n                     = nl.item(i);
                                     printf(getFunc(func),3,"INFO: get Node:"+n+":" );
                                     HashMap<String, String> nh = conf.getAttributes(n);
                                     printf(getFunc(func),3,"INFO: get NodeAttribute Hash:"+nh+":" );
                                     NodeList nlf = n.getChildNodes();
                                     if ( nlf != null && nlf.getLength() > 0 ) {
                                         conf.nodeReadout(nlf,nh);
                                     }
                                     nmh.put(nh.get("name"), nh);
                                     WlsServer ws = new WlsServer(nh);
                                               
                                     servers.put(nh.get("name"), ws);
                    } 
                } else {
                    throw new RuntimeException("ERROR: no servers found in config.xml");
                }
            
                if ( debug > 0 ) {
                    Iterator<String> itter = servers.keySet().iterator();
                    while(itter.hasNext()) {
                        String k = itter.next();
                        printf(getFunc(func),1,"WlsServer "+k+" =>"+servers.get(k));
                    }
                }
            }    
        } else {
           te = getProperty("initfile");
           if (te == null ||  te.isEmpty() ) {
                throw new RuntimeException("ERROR: "+confdir.getFQDNDirName()+" is not a directory ");
           }     
        }
       
        //te = getProperty("initfile");
        if (te != null && ! te.isEmpty() ) {
            printf(getFunc(func),0,"INFO: test to initialize initfile:"+te); //3 
            ReadFile nf = new ReadFile(te); 
            if ( nf.isReadableFile() ) {
               printf(getFunc(func),0,"INFO: read initfiles "+te);    
            }
        } else {
            printf(getFunc(func),0,"WARNING: initfile not defined");
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
    
    public String getDomainName() {  return this._domainname;  }
    
    private void updateInitFile() {
         String te = getProperty("initfile");
         if ( te == null || te.isEmpty() ) { te=confdir+File.separator+"initfile"+getDomainName(); }
    }
    
    public static void main(String[] args) {
        WlsDomain wl = getInstance(args);
        if ( wl.getBooleanProperty("store") ) {
            System.out.println("update ");
            wl.updateInitFile();
        }
    }
    
    public static WlsDomain getInstance(String[] args) {
        WlsDomain wl = new WlsDomain(args);
                  wl.init();
        return wl;
    }
    
    public static WlsDomain getInstance(String confDir, String passwordFile) {
        String[] args = new String[] { "-conf", confDir, "-pwfile", passwordFile};
        WlsDomain wl = getInstance(args);
        return wl;          
    }
    
    public static WlsDomain getInstance(String confDir) {
        String[] args = new String[] { "-conf", confDir };
        WlsDomain wl = getInstance(args);
        return wl;          
    }
}
