/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.wls;

import static general.Version.printf;
import io.file.ReadDir;
import io.file.ReadFile;
import io.file.SecFile;
import io.file.XMLReadFile;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
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
            WlsUser wu;
    private ReadDir confdir;
    private XMLReadFile conf;
    private final HashMap<String, WlsServer> servers; 
    private final HashMap<String, WlsNodeManager> nmsrv;
    private String _domainname;
    private String _mwhome="";
    private String _wlhome="";
    private String _adminserver="AdminServer";
    private String _nodeMUser="";
    private String _nodeMPass="";
    
    public WlsDomain(String[] args) {
       super(args,"WlsDomain");
       final String func="WlsDomain(String[] args)";
       WlsServer.debug=debug;
       servers = new  HashMap<String, WlsServer>();
       nmsrv   = new  HashMap<String, WlsNodeManager>();
       printf(getFunc(func),3,"conf:"+getProperty("conf"));
       if ( getProperty("conf","none").matches("none") ) {
        if ( ! getProperty("domain","none").matches("none") ) {
            this._domainname=getProperty("domain");
            loadLocation();
        }
       }
       
       try { init(); } catch(Exception e) {}
       
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
       if ( _loaded ) { return; }
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
            conf.debug=debug;
            if ( ! conf.isReadableFile() ) {
                te = getProperty("initfile");
                if ( te == null || te.isEmpty() )
                    throw new RuntimeException("ERROR: "+((conf!=null)?conf.getFQDNFileName():"NULL-FILE")+" not domain directory found with "+conf.getFQDNFileName());
            } else {
                NodeList nl = conf.getNodeList("domain");
                HashMap<String, String> domh = conf.getAttributes(nl.item(0));
                if ( domh.get("name") == null ) {
                    conf.nodeReadout(nl,domh);
                }
                this._domainname = domh.get("name");
                
                nl = conf.getNodeList("admin-server-name");
                this._adminserver=nl.item(0).getTextContent();
                
                nl = conf.getNodeList("node-manager-username");
                this._nodeMUser=nl.item(0).getTextContent();
                
                nl = conf.getNodeList("node-manager-password-encrypted");
                this._nodeMPass=nl.item(0).getTextContent();
                
                nl = conf.getNodeList("node-manager");
                printf(func,3,"get NodeManager List:"+( (nl!=null)?nl.getLength():"NULL")+":" );
                for ( int i=0; i< nl.getLength(); i++ ) {
                        WlsNodeManager m = new WlsNodeManager(getDomainName()); m.debug = debug;
                        m.updateNodeManager(nl.item(i));
                        this.nmsrv.put(m.getMachineName(), m);
                        printf(func,2,"nodemanager:"+i+"  =>"+m.toString() );
                
                }
                
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
                WlsServer ws = new WlsServer(nh, this);
                servers.put(nh.get("name"), ws);
        }
        //te = getProperty("initfile");
        if (te != null && ! te.isEmpty() ) {
            printf(func,2,"INFO: test to initialize initfile:"+te); //3 
            ReadFile nf = new ReadFile(te); 
            if ( nf.isReadableFile() ) {
               printf(func,2,"INFO: read initfiles "+te);    
            }
        } else {
            printf(func,2,"WARNING: initfile not defined");
        }
       
        te = getProperty("pwfile");
        if (te == null || te.isEmpty() ) {
            te = confdir.getFQDNDirName()+File.separator+"domainkeys"; 
        }
        SecFile nf = new SecFile(te); 
        printf(func,2,"INFO:  like to read pwfile:"+nf.getFQDNFileName()+" is readable:"+nf.isReadableFile() );
        if ( nf.isReadableFile() ) {
             if ( ! nf.isCrypted() ) { 
                 printf(func,0,"INFO:  update pwfile:"+nf.getFQDNFileName()+" - crypt it" );       
                 nf.crypt(); 
             }
             String a = nf.readOut().toString();
             String dec= (a.endsWith("="))? crypt.getUnCrypted(a):a ;
             String u=""; String p=""; String nmu=""; String nmp="";
             for(String s: dec.split("\n")) {
                 String[] sp = s.trim().split("=");
                 if      ( sp[0].toLowerCase().matches("username")) {   u=s.substring(sp[0].length()+1).trim(); }
                 else if ( sp[0].toLowerCase().matches("password")) {   p=s.substring(sp[0].length()+1).trim(); }
                 else if ( sp[0].toLowerCase().matches("nmuser")  ) { nmu=s.substring(sp[0].length()+1).trim(); }
                 else if ( sp[0].toLowerCase().matches("nmpass")  ) { nmp=s.substring(sp[0].length()+1).trim(); }
             }
             this.wu = new WlsUser(new URL("http://localhost:7001"),u,p);
             printf(func,2,"INFO: wu user:"+crypt.getUnCrypted(wu.getUsername())+"|"+u+"|<|  p:"+(p.matches(crypt.getUnCrypted(wu.getPassword()))) );
        
             if ( ! nmsrv.isEmpty() ) {
                Iterator<String> itter = nmsrv.keySet().iterator();
                while(itter.hasNext()) {
                      WlsNodeManager w = nmsrv.get( itter.next() );
                      if ( w != null ) { w.setNodeManagerUser(nmu); w.setNodeManagerPass(nmp); }
                }
             }
             if ( ! servers.isEmpty() ) {
                Iterator<String> itter = servers.keySet().iterator();
                while(itter.hasNext()) {
                      WlsServer w = servers.get( itter.next() );
                      if ( w != null ) { w.setAdminUser(u); w.setAdminPass(p); }
                }
             }
        } else {
             printf(func,2,"ERROR: Couldn't read pwfile:"+nf.getFQDNFileName() );
        }
        
        nf = new SecFile(confdir.getFQDNDirName()+File.separator+"bin"+File.separator+"setDomainEnv."+( (isWindows())?"cmd":"sh") );
        if ( nf.isReadableFile() ) {
             for(String s : nf.readOut().toString().split("\n") ) {
                 
                if ( s.startsWith("WL_HOME")) {
                    s= s.trim();
                    this._wlhome=s.substring("WL_HOME=\"".length(), s.length()-1 );
                    ReadDir df = new ReadDir(this._wlhome);
                    this._mwhome=df.getParentFile().getAbsolutePath();
             
                }
             }
             
        }
          
        debug=savedebug; 
        _loaded=true;  
    }
    
    public String getDomainName()   { return this._domainname;   }
    public String getWeblogicHome() { return this._wlhome;       }
    public String getMWHome()       { return this._mwhome;       }
    
    public HashMap<String, WlsServer> getServers() { return servers; }
    
    public String getAdminUrl() {
        if ( ! _loaded ) { try { init(); }catch(Exception e){} }
        WlsAdminServer w = getAdminServer(); 
        return w.getAdminUrl();
    }
    
    public String getAdminStopUrl() {
        if ( ! _loaded ) { try { init(); }catch(Exception e){} }
        WlsAdminServer w = getAdminServer(); 
        return w.getAdminStopUrl();
    }
    
    public String getAdminServerName() {
        if ( ! _loaded ) { try { init(); }catch(Exception e){} }
        WlsAdminServer w = getAdminServer(); 
        return w.getAdminServerName();
    }
    
    private WlsAdminServer wsadm =null;
    public WlsAdminServer getAdminServer() {
        if ( wsadm != null ) { return wsadm; }
        WlsServer w=null;
        
        Iterator<String> itter = getServers().keySet().iterator();
        outter:
        while( itter.hasNext() ) {
                    WlsServer s = servers.get(itter.next());
                    if ( s != null && s.isAdminServer() ) {
                        wsadm=s.getAdminInstance();
                        break outter;
                    }
        }
        
        return wsadm;
    }

    public String getAdminOnline() {  return (getAdminServer()).getOnline(); }
        
    public ArrayList<WlsServer> getNoneAdminServers() {
        getAdminServer();
        
        ArrayList<WlsServer> lmap = new ArrayList<WlsServer>();
        Iterator<String> itter = getServers().keySet().iterator();
        while( itter.hasNext() ) {
                    WlsServer s = servers.get(itter.next());
                    if ( s != null && ! s.isAdminServer() ) {
                        lmap.add(s);
                    }
        }
        return lmap;
    }
    
    public ArrayList<WlsNodeManager> getNodeManagers() {
        ArrayList<WlsNodeManager> lmap = new ArrayList<WlsNodeManager>();
        Iterator<String> itter = this.nmsrv.keySet().iterator();
        while( itter.hasNext() ) {
             lmap.add( this.nmsrv.get(itter.next()) );
        }
        return lmap;
    }
    
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
