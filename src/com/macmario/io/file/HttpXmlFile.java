/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.macmario.io.file;

import com.macmario.comm.checker.web.Header;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author SuMario
 */
public class HttpXmlFile extends XMLFile {

    public HttpXmlFile(String dir, String file) { super(dir, file); }
    public HttpXmlFile(String nfile){ this( new File(nfile) ); }
    public HttpXmlFile(File   n    ){ super(n); }
    
    private ArrayList<Header> xmap=null;
    public ArrayList<Header> getXMLObject() {
        Header.debug=debug;
        xmap = new ArrayList<Header>();
        final String func="getXMLObject()::";
        NodeList nl = super.getChildNodes();
        log(1, func+" NodeList nl:"+nl.getLength());
        if ( nl.getLength() > 0) {recReadNode(nl);}
        
        return xmap;
    }
    public ArrayList<Header> getPlainObject() {
        xmap = new ArrayList<Header>();
        String[] sp=readOut().toString().split("\n");
        Header a = null;  boolean head=true; Header.debug=debug;
        for ( int i=0; i<sp.length; i++) {
            log(2, "line["+i+"/"+sp.length+"] in  =>"+sp[i]+"|");
            while( i < sp.length-1 && ( sp[i].length() <2 || sp[i].startsWith("---") ) ) { i++; }
            log(2, "line["+i+"] length="+sp[i].length()+" out =>"+sp[i]+"|");
            
            if ( sp[i].startsWith("http") || sp[i].startsWith("/") ) {
               if ( a != null ) { a.setCheck(true); a.start(); } 
               a = new Header(); a.debug=this.debug; head=true; xmap.add(a);
               log(2,"request reached");
            } else if ( sp[i].startsWith("HTTP/") ) {  head=false; a.setHeader(false);
               log(2,"response reached");
            } 
            
            
            String[] fp=sp[i].split(":");
            if ( head ) {
                if ( sp[i].startsWith("GET") || sp[i].startsWith("POST") || sp[i].startsWith("HEAD") ) {
                    fp=sp[i].split(" ");
                    log(2,"set request header  HttpRequestMethod="+fp[0]+"|  HttpRequestUrl=>|"+fp[1]+"|<=  HttpRequestVersion="+fp[2]+"|");
                    if ( a == null ) { a = new Header(); a.debug=this.debug; head=true; xmap.add(a); }
                    a.setRequest("HttpRequestMethod",  fp[0].toUpperCase()); 
                    a.setRequest("HttpRequestUrl",     fp[1]); 
                    a.setRequest("HttpRequestVersion", fp[2]);
                    a.setHeader(true);
                } else {
                    if (  a.isHeader(fp[0]) ) { //  && sp[i].startsWith("[A-Z]") && sp[i].indexOf(":") >0 ) {
                        String f=sp[i].substring(fp[0].length()+2);  
                        log(2,"set request header "+fp[0]+" with =>|"+f+"|<=");  
                        if ( fp[0].startsWith("Cookie") ) { 
                                a.setRequestCookie(f);
                        } else {   
                                a.setRequest(fp[0], f);
                                if (fp[0].toLowerCase().equals("content-length")) {
                                    StringBuilder sw= new StringBuilder(); i++; sw.append(sp[i]); 
                                    log(2,"handle POST first line =>|"+sw.toString()+"|<=");
                                    if ( ! sp[i+1].isEmpty() && ! sp[i+1].startsWith("HTTP/1.") ) {
                                        i++;
                                        log(2,"handle POST second line["+i+"] add =>|"+sp[i]+"|<=");
                                        while( i < sp.length-1 && ( sp[i].length() >3 ) ) { sw.append("\n").append(sp[i]); i++; }
                                    }
                                    log(2,"handle POST header =>|"+sw.toString()+"|<=");
                                    int e = sw.indexOf("&"); 
                                    int b=0;
                                    do {
                                        log(2,"POST are ["+b+" to "+e+"] =>"+sw.substring(b, e)+"<=");
                                        a.setPostMsg(sw.substring(b, e) );
                                        b=e+1;
                                        e=sw.indexOf("&", b); if ( e == -1 ) { e=sw.length();  a.setPostMsg(sw.substring(b, e) ); }
                                        log(2,"POST position are  =>"+b+" to "+e+"<= "+sw.length());
                                    } while( (b+(e-b)) < sw.length() );    
                                }
                        } 
                            
                    } else if ( sp[i].toLowerCase().startsWith("http") ) {
                      a.setRequest("HttpRequestUrl",     sp[i]);
                    } else {
                      log(2,"set Post query header with =>|"+sp[i]+"|<=");   
                      String d = a.getRequest("Post");
                      a.setRequest("Post", ((d==null)?"":d)+sp[i]);
                    }
                }    
            } else {
                if ( sp[i].startsWith("HTTP/") ) {
                    fp=sp[i].split(" ");
                    if ( fp.length > 3 ) { 
                        for(int t=3; t<fp.length; t++){ fp[2]=fp[2]+" "+fp[t]; }
                    }
                    log(2,"set response  header  HttpResponsetMethod:"+fp[0]+"  HttpReturnCode=>|"+fp[1]+"|<=  HttpReturnDescription="+fp[2]+"|");
                    a.setResponse("HttpResponseMethod", fp[0]); a.setResponse("HttpReturnCode", fp[1]); a.setResponse("HttpReturnDescription", fp[2]);
                } else {
                    if ( ! sp[i].startsWith("---")) {
                        String f=sp[i].substring(fp[0].length()+2);
                        log(2,"set response header "+fp[0]+" with =>|"+f+"|<="); 
                        a.setResponse(fp[0],f);
                    }
                }    
            }
            
        }
        if ( a != null ) { a.setCheck(true); a.start(); }
        return xmap;
    }    
    
    
    private String key="__INIT__";
    
    private void recReadNode(NodeList nl) {
        final String func="recReadNode(NodeList nl):: ";
        StringBuilder va = new StringBuilder();
        for(int i=0; i< nl.getLength(); i++) {
                Node n =  nl.item(i);
                log(1, func+" name["+i+"]="+n.getNodeName()+"  value="+n.getNodeValue()+"|<==");
                if ( n.getNodeName().startsWith("#") ) { // value
                     va.append(n.getNodeValue());
                } else {
                     if ( ! n.getNodeName().toLowerCase().matches("value")) {
                        setTarget(key, va);
                        key = n.getNodeName();
                        va = new StringBuilder();
                     }
                     if (n.getNodeValue() != null ) { va.append(n.getNodeValue());}
                }
                NodeList nf = n.getChildNodes();
                if ( nf.getLength() > 0) {recReadNode(nf);}
        }
        setTarget(key, va);
    }
    
    private HashMap<String, String> map = new HashMap<String, String>();
    private void setTarget(String key, StringBuilder sw){
         final String func="setTarget(String key, StringBuilder sw)";
         log(1,func+" key="+key+"|<==  with value ==>"+sw.toString()+"<==" );
         if ( map.get(key) == null ) { map.put(key, sw.toString()); } else { map.put(key, map.get(key)+sw.toString()); }
    }
    
    private void log(final int level, String msg) {
       if ( debug >= level  ) {
           if ( level > 0 ) { msg="DEBUG("+level+"/"+ debug +") HttpXmlFile:: =>"+msg; }
           System.out.println(msg);
       } 
    }
}
