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
import java.net.URL;
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
    private WlsUser wu;
    private ReadDir confdir;
    private XMLReadFile conf;
    private final HashMap<String, WlsServer> servers; 
    private String _domainname;
    private String _adminserver="AdminServer";
    public WlsDomain(String[] args) {
       super(args,"WlsDomain");
       final String func="WlsDomain(String[] args)";
       WlsServer.debug=debug;
       servers = new  HashMap<String, WlsServer>();
       printf(getFunc(func),3,"conf:"+getProperty("conf"));
       if ( getProperty("conf","none").matches("none") ) {
        if ( ! getProperty("domain","none").matches("none") ) {
            this._domainname=getProperty("domain");
            loadLocation();
        }
       }
       
       if ( debug >= 3 ) {
            int i=0;
            for(String s : args) {
                printf(getFunc(func),3," value["+i+"/"+args.length+"] :"+args[i++] );
            }
       }
    }
    
    public WlsDomain(String domname) {
        this(new String[]{"-domain", domname});
        this._domainname = domname;
    }
    
    public void setDomainLocation(String dir) { 
        setProperty("conf",dir); 
        try { init(); } catch(Exception e) {}
    }
    
    public String getDomainLocation() {
        try { 
            init(); 
        }catch(Exception e) {
            printf(getFunc("getDomainLocation()"),1,"ERROR:"+e.getMessage(),e);
            
            return null; 
        }
        return confdir.getFQDNDirName();
    }
    
    private boolean _loaded=false;
    public boolean isLoaded() { return _loaded; }
    private void loadLocation() {
        final String func=getFunc("loadLocation()");
        printf(func,2,"location:"+getProperty("conf","none")+":  =>"+ (System.getProperty("user.dir")+File.separator+"location") );
        if ( getProperty("conf","none").matches("none") ) {
                ReadFile f = new ReadFile(  (System.getProperty("user.dir")+File.separator+"location"));
                String[] sp = f.readOut().toString().split("\n");
                for(String s: sp) {
                    String[] fp = s.trim().split(";");
                    if ( fp[0].matches(this._domainname)) {
                        setProperty("conf", fp[1]);
                    }
                }
         }
    }
    private void init() throws Exception {
       loadLocation();
       final String func=getFunc("init()");
       HashMap<String, HashMap<String, String>> nmh = new HashMap<String, HashMap<String, String>>();
       int savedebug=debug;
// debug=4;
       confdir = new ReadDir(".");
       String te = getProperty("conf");
       printf(func,3,"tmpconf:"+te+":");
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
                
                nl = conf.getNodeList("admin-server-name");
                this._adminserver=nl.item(0).getTextContent();
                
                     nl = conf.getNodeList("server");
                if ( nl != null && nl.getLength() > 0 ) {
                    printf(getFunc(func),3,"INFO: "+nl.getLength()+" server[s] found ");
                    for ( int i=0;  i < nl.getLength() ; i++ ) {
                                     Node n                     = nl.item(i);
                                     printf(func,3,"INFO: get Node:"+n+":" );
                                     HashMap<String, String> nh = conf.getAttributes(n);
                                     printf(func,3,"INFO: get NodeAttribute Hash:"+nh+":" );
                                        NodeList nlf = n.getChildNodes();
                                        if ( nlf != null && nlf.getLength() > 0 ) {
                                            conf.nodeReadout(nlf,nh);
                                        }
                                     nh.put("domain", this._domainname );
                                     nh.put("domainlocation", confdir.getFQDNDirName());
                                     nmh.put(nh.get("name"), nh);
                                     if ( nh.get("name").matches(this._adminserver) ) {
                                         nh.put("adminserver", "true");
                                     } else {
                                         nh.put("adminserver", this._adminserver);
                                     }
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
       
        Iterator<String> it = nmh.keySet().iterator();
        while(it.hasNext()) {
                HashMap<String,String> nh = nmh.get(it.next());
                WlsServer ws = new WlsServer(nh);
                servers.put(nh.get("name"), ws);
        }
        //te = getProperty("initfile");
        if (te != null && ! te.isEmpty() ) {
            printf(getFunc(func),2,"INFO: test to initialize initfile:"+te); //3 
            ReadFile nf = new ReadFile(te); 
            if ( nf.isReadableFile() ) {
               printf(getFunc(func),2,"INFO: read initfiles "+te);    
            }
        } else {
            printf(getFunc(func),2,"WARNING: initfile not defined");
        }
       
        te = getProperty("pwfile");
        if (te == null || te.isEmpty() ) {
            te = confdir.getFQDNDirName()+File.separator+"domainkeys"; 
        }
        ReadFile nf = new ReadFile(te); 
        if ( nf.isReadableFile() ) {
             String dec=  crypt.getUnCrypted(nf.readOut().toString());
             String u=""; String p="";
             for(String s: dec.split("\n")) {
                 String[] sp = s.trim().split("=");
                 if      ( sp[0].toLowerCase().matches("username")) { u=s.substring(sp[0].length()+1).trim(); }
                 else if ( sp[0].toLowerCase().matches("password")) { p=s.substring(sp[0].length()+1).trim(); }
             }
             this.wu = new WlsUser(new URL("http://localhost:7001"),u,p);
        } else {
             printf(func,2,"ERROR: Couldn't read pwfile:"+nf.getFQDNFileName() );
        }
        
          
        debug=savedebug; 
        _loaded=true;  
    }
    
    public String getDomainName() {  return this._domainname;  }
    
    public HashMap<String, WlsServer> getServers() { return servers; }
    
    String store() {
        StringBuilder sw = new StringBuilder();
        Iterator<String> itwd = servers.keySet().iterator();
        while( itwd.hasNext() ) {
            WlsServer wd = servers.get(itwd.next());
                      sw.append("server :{servername:").append(wd.getServerValue("name")).append(" ");
                      Iterator<String> itter = wd.getMapIterator();
                      while(itter.hasNext()) {
                            String k = itter.next();
                            if ( ! k.matches("name") )
                                sw.append(" ").append(k).append(":").append(wd.getServerValue(k));
                      }
                      sw.append("}\n");
                      
        }
        return sw.toString();
    }
    
    public void testAlive() throws Exception {
          Iterator<String> itter = servers.keySet().iterator();
          WlsAdminServer wa =  (servers.get( itter.next() )).getAdminInstance();
                         wa.testAlive();
          while(itter.hasNext()) {
              WlsServer ws = servers.get( itter.next() );
                        ws.testAlive();
          }
    }
    
    
    private void updateInitFile() {
         String te = getProperty("initfile");
         if ( te == null || te.isEmpty() ) { te=confdir+File.separator+"initfile"+getDomainName(); }
    }
    
    public static void main(String[] args) throws Exception {
        WlsDomain wl = getInstance(args);
        if ( wl.getBooleanProperty("store") ) {
            System.out.println("update ");
            wl.updateInitFile();
        }
    }
    
    public static WlsDomain getInstance(String[] args) throws Exception {
        WlsDomain wl = new WlsDomain(args);
                  wl.init();
        return wl;
    }
    
    public static WlsDomain getInstance(String confDir, String passwordFile) throws Exception {
        String[] args = new String[] { "-conf", confDir, "-pwfile", passwordFile};
        WlsDomain wl = getInstance(args);
        return wl;          
    }
    
    public static WlsDomain getInstance(String confDir) throws Exception {
        String[] args = new String[] { "-conf", confDir };
        WlsDomain wl = getInstance(args);
        return wl;          
    }
}
