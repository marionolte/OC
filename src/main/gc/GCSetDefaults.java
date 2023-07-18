/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.gc;

import com.macmario.general.Version;
import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * @author SuMario
 */
public class GCSetDefaults extends Version {
    final public HashMap<String,String> basemap;
    final public HashMap<String,String>   exmap;
    final public HashMap<String,String>   gcmap;
    
    private Long xmx;
    public GCSetDefaults() {
         basemap = new HashMap<String,String>();
         basemap.put("Xmx", "64m"); this.xmx=64*1024L;
         basemap.put("Xms", "");
         basemap.put("NewSize", "");
         basemap.put("MaxNewSize", "");
         basemap.put("Xss", "");
         
         exmap = new HashMap<String,String>();
         
         gcmap = new HashMap<String,String>();
    }
 
    
    public boolean isBaseProperty(String key) {
        String k = getBaseKey(key);
        return basemap.containsKey(k);
    }
    public String getBaseKey(String key) {
        String k = getAlternateKey(key).replaceAll("[0-9][k,m,g,t]$", "").replaceAll("[0-9]", "");
        printf(getFunc("getBaseKey(String key)"),2,"return base key:"+k+":  from:"+key+":");
        return k;
    }
    
    public String getBaseValue(String key) {
        String k = getBaseKey(key);
        String v = key.replaceAll(k, "");
        printf(getFunc("getBaseValue(String key)"),2,"return base val:"+v+":  from:"+key+":");
        return v;
    }
    public boolean isXXProperty(String key) {
        return (key.equals("XX"));
    }
    public void setXXProperty(String key, String val) { exmap.put(key, val); }
    public String getXXProperty(String key){
        if ( key == null || key.isEmpty() ) { return ""; }
        String ret = (gcmap.containsKey(key))?"":"-XX:"+key+"="+exmap.get(key)+" ";
        printf(getFunc("getXXProperty(String key)"),2,"get XX value for key:"+key+": val:"+ret+":");
        return ret;        
    }
   
    
    public String getAlternateKey(String k) {
        if      ( k.equals("NewSize") ) { return "Xmn"; }
       
        return k;
    }
    
    public void setBaseValue(String k, String v) {
        if ( k.matches("Xmx") ) { Long l =getLong(v);  this.xmx=l; }
        this.basemap.put(k, v);
    }
    
    public String getBaseSet() {
        StringBuilder sw = new StringBuilder();
        Iterator<String> itter = basemap.keySet().iterator();
        while (itter.hasNext() ) {
            sw.append(getBaseProperty(itter.next()));
        }
        return sw.toString();
    }
    
    public String getXXSet() {
        StringBuilder sw = new StringBuilder();
        Iterator<String> itter = exmap.keySet().iterator();
        while (itter.hasNext() ) {
            sw.append(getXXProperty(itter.next()));
        }
        return sw.toString();
    }
    
    public String getGCSet() {
        StringBuilder sw = new StringBuilder();
        Iterator<String> itter = gcmap.keySet().iterator();
        while (itter.hasNext() ) {
            sw.append(getBaseProperty(itter.next()));
        }
        return sw.toString();
    }
    
    private String getBaseProperty(String key) {
        final String func=getFunc("getBaseProperty(String key)");
        String ret="";
        printf(func,0,"get base property from:"+key+": val:"+basemap.get(key)+":");
        if      ( key.equals("Xmx")         ) { ret=""+basemap.get("Xmx"); }
        else if ( key.equals("Xms")         ) { ret=(basemap.get("Xms").isEmpty()     )?""+basemap.get("Xmx"):basemap.get("Xms");       }
        else if ( key.equals("NewSize")  ||
                  key.equals("Xmn")         ) { ret=(basemap.get("NewSize").isEmpty() )? getNewSize()         :basemap.get("NewSize"); }
        else if ( key.equals("Xss")         ) { ret=(basemap.get(key).isEmpty())?"1024k":basemap.get(key); }
        
        printf(func,0,"return for base property :"+key+": the value:"+"-"+key+ret+" :");
        if ( ret.isEmpty() ) { return ret; }
        return "-"+key+ret+" ";
    }
    
    private String getNewSize() {
        final String func=getFunc("getNewSize()");
        int fac=2;  
        if ( this.xmx < 712*1024*1024L ) { fac=3; }
        printf(func,0," use factor :"+fac+":");
        String s = getShortMem( (this.xmx/fac) );
        String ret =( s.isEmpty() || s.equals("k")) ? "":s; 
        printf(func,0," return -Xmn:"+ret+":  from s:"+s+":");
        return ret;
    }
    
    private String getShortMem(Long l) {
        final String func=getFunc("getShortMem(Long l)");
        String ret="k";
               
        String val="";
        
        printf(func,0, "like to parse:"+l);
        //l = l - (l % 1024);
        printf(func,0, "like to parse:"+l);
             
        while (l>1024) {
            l =l / 1024;
            ret.replaceAll("g", "t").replaceAll("m", "g").replaceAll("k", "m");
            printf(func,0, "have :"+l+": with ret:"+ret+":");
        }
        printf(func,0, "return from parse:"+l+":"+val+ret+":");
        return val+""+ret;
    }
    private Long getLong(String v) {
        final String func=getFunc("getLong(String v)");
        printf(func,0,"parse to get Long :"+v+": ");
        if ( v == null || v.isEmpty() ) { return 0L; }
        v = v.toLowerCase(); 
        
        Long l = Long.parseLong(v.replaceAll("[a-z]", ""));
        while ( v.contains("[a-z]") ) {
             if      ( v.contains("m") ) { l=l*1024; v.replaceAll("m", "" ); }
             else if ( v.contains("g") ) { l=l*1024; v.replaceAll("g", "m"); }
             else if ( v.contains("t") ) { l=l*1024; v.replaceAll("t", "g"); }
        }         
        printf(func,0,"parse to get Long from low :"+v+":  getting:"+l+":");
        return l;
    }
}
