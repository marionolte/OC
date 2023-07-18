/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.wls;

import com.macmario.io.file.ReadDir;
import com.macmario.io.file.ReadFile;
import com.macmario.io.file.XMLReadFile;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.macmario.io.net.Http;
import com.macmario.net.tcp.TcpHost;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author SuMario
 */
public class WlsDomainInfo extends TcpHost {
    private final HashMap<String,String> map = new HashMap();
    private final HashMap<String, HashMap<String,String>> smap = new HashMap();
    private final ReadDir domloc;
    private XMLReadFile conf;
    
    WlsDomainInfo(String nl) {
        final String func=getFunc("WlsDomainInfo(String nl)");
        printf(func,2,"info:"+nl);
        domloc = new ReadDir(nl);
        readConfig();
    }

    String getDomainInfo() {
        final String func=getFunc("getDomainInfo()");
        StringBuilder sw = new StringBuilder();
        sw.append("Domain: "        ).append(this.getDomainName()           ).append("\n");
        sw.append("Domain version: ").append(this.getDomainVersion()        ).append("\n");
        sw.append("Java version: "  ).append(this.map.get("java")           ).append("\n");
        sw.append("Java home: "     ).append(this.map.get("javahome")       ).append("\n");
        sw.append("Products: ").append("\n");
        sw.append("Patches: ").append(this.getPatchlist()).append("\n");
        sw.append("Adminserver: "   ).append(this.map.get("AdminServerName")).append("\n");
        
        HashMap<String,String> imap = new HashMap();
        Iterator<String> itter = smap.keySet().iterator();
        while ( itter.hasNext() ) {
            String srv = itter.next();
            String url = getUrl(srv);
            String urlstat="";
            try {
              if ( ! imap.containsKey(url) ) {  
                Http ht = new Http(new URL(url));
                     ht.setTimeout(1000);
                     ht.connect();
                     urlstat=(ht.getResponseCode()>199)?"UP":"DOWN";
                     imap.put(url, urlstat);
              } else {
                     urlstat=imap.get(url);
              }       
            }catch(Exception e) {
                     urlstat="DOWN";  
                     imap.put(url, urlstat);
            }
            sw.append("Server "+srv+" : ").append(url).append("  ("+urlstat+")").append("\n");
            
        }
        sw.append("#####\n\n");
        return sw.toString();
    }
    
    
    private void readConfig() {
        //debug=3;
        final String func=getFunc("readConfig()");
        conf=new XMLReadFile(domloc.getFQDNDirName()+File.separator+"config"+File.separator+"config.xml");
        conf.debug=debug;
        
        NodeList nl = conf.getNodeList("name");
        printf(func,3,"domain:"+nl.item(0).getTextContent());
        map.put("domain", nl.item(0).getTextContent());
        printf(func,1,"domain name:"+getDomainName());
        
        nl=conf.getNodeList("domain-version");
        map.put("domainversion",  nl.item(0).getTextContent());
        
                
        nl = conf.getNodeList("admin-server-name");
        map.put("AdminServerName", nl.item(0).getTextContent());
        
        // servers
        nl = conf.getNodeList("server");  HashMap<String,String> imap=new HashMap(); 
        
        for ( int i=0; i < nl.getLength(); i++) {
             Node n=nl.item(i);
             if ( n.hasChildNodes() ) {
                  //if ( ! imap.isEmpty() ) {
                  //    smap.put(imap.get("name"), imap);
                  //}
                  imap=new HashMap(); 
                  NodeList nlc = n.getChildNodes();
                  for (int j=0; j < nlc.getLength(); j++ ) {
                      
                      if ( ! nlc.item(j).getNodeName().startsWith("#") ) {
                          if ( nlc.item(j).getNodeName().equals("ssl")  ) {
                              NodeList nlt = nlc.item(j).getChildNodes();
                              for (int t=0; t < nlt.getLength(); t++ ) {
                                  if (       ! nlt.item(t).getNodeName().startsWith("#") 
                                          && ! nlt.item(t).getNodeName().equals("web-service") 
                                          //&& ! nlt.item(t).getNodeName().equals("ssl") 
                                      ) {
                                        printf(func,2,i+":"+j+":"+t+": =>"+nlt.item(t).getNodeName()+"="+nlt.item(t).getTextContent());
                                        imap.put("ssl_"+nlt.item(t).getNodeName(), nlt.item(t).getTextContent());
                                  }

                              }
                          } else if(nlc.item(j).getNodeName().equals("web-service") ) { 
                          } else if(nlc.item(j).getNodeName().equals("jta-migratable-target") ) {
                          } else {
                            printf(func,2,i+":"+j+":0: =>"+nlc.item(j).getNodeName()+"="+nlc.item(j).getTextContent());
                            imap.put(nlc.item(j).getNodeName(), nlc.item(j).getTextContent());
                          }  
                           
                      } 
                  }
                  printf(func,2,"imap:"+imap);   
                  smap.put(imap.get("name"), imap);
             }
        }
        
                nl = conf.getNodeList("node-manager-username");
                map.put("NMUSER", nl.item(0).getTextContent());
                
                nl = conf.getNodeList("node-manager-password-encrypted");
                map.put("NMUSERPASS", nl.item(0).getTextContent());
                
                nl = conf.getNodeList("node-manager");
                printf(func,3,"get NodeManager List:"+( (nl!=null)?nl.getLength():"NULL")+":" );
                for ( int i=0; i< nl.getLength(); i++ ) {
                        WlsNodeManager m = new WlsNodeManager(getDomainName()); m.debug = debug;
                        m.updateNodeManager(nl.item(i));
                        //this.nmsrv.put(m.getMachineName(), m);
                        printf(func,2,"nodemanager:"+i+"  =>"+m.toString() );
                
                }

                printf(func,2,"domain:"+getDomainName());
        ArrayList<String> m = new ArrayList();        
        ReadFile rf = new ReadFile(domloc.getFQDNDirName()+File.separator+"bin"+File.separator+"setDomainEnv.sh"); 
                final String fa= rf.readOut().toString();
                final Matcher ma = Pattern.compile("JAVA_HOME=").matcher(fa);
                int start=0;
                while ( ma.find(start) ) {
                    String[] sp = fa.substring(ma.end()).split("\n");
                    if ( ma.group().contains("JAVA_HOME") && sp[0].startsWith("\""+File.separator)) {
                        //System.out.println("find =>|"+sp[0]+"|<=");
                        map.put("javahome", sp[0].replaceAll("\"", "") );
                        String ja=sp[0].replaceAll("\"", "")+File.separator+"bin"+File.separator+"java";
                        m = new ArrayList(); m.add(ja); m.add("-version");
                        //System.out.println("findout "+ja);
                        String v=com.macmario.io.lib.IOLib.launch(m); //    .execReadToString( new String[]{ ja, "-version" } );
                        //System.out.println("findout =>|"+sp[0]+"|\n=>|"+v+"|<=");
                        sp = v.split("\n"); sp = sp[0].split(" "); sp=sp[sp.length-1].split("-");
                        map.put("java",sp[ 0 ].replaceAll("\\(", "").replaceAll("\"", "") );
                        
                    }
                    
                    start=ma.end();
                    
                }
                
    }
    
    public String getUrl(String srv) {
        HashMap<String, String> imap = smap.get(srv);
        if ( imap == null || imap.isEmpty() ) {
            return "";
        }
        boolean ssl = imap.containsKey("ssl_name");
        StringBuilder sw = new StringBuilder();
        sw.append( ( (ssl)?"https://":"http://") );
        
        String ho=imap.get("listen-address");
        if ( ho == null ) { ho = getHostname(); }
        if ( ssl ) {
            ho=( imap.containsKey("ssl_listen-address") )?imap.get("ssl_listen-address"):ho;
            if ( ho == null ) { ho = getHostname(); }
        }
        if ( ho == null || ho.isEmpty()) { ho = "localhost"; }
        sw.append(ho);
        
        String p=(ssl)? (
                          imap.containsKey("ssl_listen-port")?
                                   imap.get("ssl_listen-port")
                                 :
                                   ""
                           
                        )
                        : (
                          imap.containsKey("listen-port")?
                                   imap.get("listen-port")
                                 :
                                   ""
                        );
        sw.append( (p.isEmpty())?"/":":"+p+"/" );
    
        return sw.toString();
    }
    
    public String getDomainName()    { return this.map.get("domain");  }
    public String getDomainVersion() { return this.map.get("domainversion"); }
    public String getValueOf(String srv, String name) {
        HashMap<String, String> imap = smap.get(srv);
        if ( imap == null || imap.isEmpty() || ! imap.containsKey(name) ) {
            return "";
        }
        
        return imap.get(name);
    }

    
    private ArrayList<String> patches = new ArrayList();
    private ArrayList<String> prods   = new ArrayList();
    
    void setOpatch(String opatch) {
        //System.out.println("opatch:"+opatch);
        int is=0;
        for ( String s : opatch.split("\n"))  {
           
           if ( is > 0 && ! s.isEmpty() ) {
               if ( is == 1  ) {
                   prods.add(s);
               }
           }
            
           if ( s.startsWith("Patch" ) ) {
               String sp[] = s.split(" ");
               String p = "";
               for ( String pa : sp ) {
                   if ( p.isEmpty() && com.macmario.io.lib.IOLib.isNumber(pa) ) {  p=pa; break; }
               }
               if ( ! p.isEmpty() ) {
                   //System.out.println("s[patch]=>"+p);
                   patches.add(p);
               }    
           }
           else if ( s.startsWith("Interim patches")) {
               is=0;
           }
           else if( s.startsWith("Installed Top-level Products") ) {
               is=1;
           }
           else if ( s.startsWith("There are") ) { is=0; }
        }
        
        
    }
    
    String getPatchlist(){
         StringBuilder sw = new StringBuilder();
         for ( String s : patches) {
             if ( sw.length() > 0 ) { sw.append(","); }
             sw.append(s);
         }
         return sw.toString();
    }
    
    String getProductlist(){
         StringBuilder sw = new StringBuilder();
         for ( String s : prods) {
             if ( sw.length() > 0 ) { sw.append("\n"); }
             sw.append("\t").append(s);
         }
         return sw.toString();
    }
}
