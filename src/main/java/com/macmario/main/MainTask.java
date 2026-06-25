/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.main;

import com.macmario.general.Version;
import com.macmario.io.crypt.Crypt;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

/**
 *
 * @author SuMario
 */
public abstract class MainTask extends Version{
    public int exitCode=-1;
    private final String name;
    public Crypt crypt = new Crypt();
    
    public MainTask() { this(null,"MajorOfMainTask");}
    public MainTask(String[] args,String name) { 
        this.prop = parseArgs(args);
        this.name = name;
    }
    
    public String getName() { return this.name; }
    private Properties prop=null;
    public  void       setProperties(Properties prop) { this.prop = prop; }
    public  Properties getProperties() { return this.prop; }
    public  Properties parseArgs(String[] args) {
        final String func=getFunc("parseArgs(String[] args)");
        int savdebug=debug;
        debug=savdebug;
        Properties p = new Properties();
                   p.setProperty("COMMAND", "");
        if ( args != null && args.length > 0 ) {
            for(int i=0; i < args.length; i++ ) {
                if ( args[i].matches("-d") ) { debug++; }
                else if ( args[i].matches("-version")  ){ p.setProperty("COMMAND", "VERSION"); }
                else if ( args[i].contains("=")        ){ String[] sp=args[i].split("=") ;  p.setProperty(sp[0], args[i].substring(sp[0].length()+1)); }
                else {
                    if( args[i].startsWith("-") ) {
                        String a=args[i].replaceAll("^-", "");
                        printf(func,2,"args.length:"+args.length+":  i="+i+"  ("+( args.length > (i+i))+") - ("+( args.length > (i+i))+")" );
                        if ( args.length <= i+1 || ( args.length > (i+1) && args[ i+1 ].startsWith("-") ) ) {
                            p.setProperty(a,"true"); 
                            if ( p.getProperty("COMMAND") == null ) { p.setProperty("COMMAND",a.toUpperCase()); }
                        } else { 
                            printf(func,2,"set "+a+" with contant of "+args[ (i+1)]);
                            p.setProperty(a, getReplaceSeparator(args[++i]));
                        }    
                        //System.out.println(a+"="+p.getProperty(a)+"|");
                    }
                    else if ( p.get("COMMAND").toString().isEmpty() ) {
                        final String a=args[i].toUpperCase();
                        printf(func,2,"set COMMAND with "+a);
                        p.setProperty("COMMAND", a ); 
                    } else {
                        final String a=( p.getProperty("value") == null )?args[i]:p.getProperty("value")+" "+args[i];
                        printf(func,2,"set value with "+a);
                        p.setProperty("value", a);
                    }    
                }
            }        
        }
        debug=savdebug;
        return p;
    }
    
   private final String __rep="__@@__"; 
   public String getReplaceSeparator(String a) {
       return a.replaceAll(File.separator, __rep);
   } 
   public String getReplaceSeparatorBack(String a) {
       return a.replaceAll( __rep , File.separator);
   } 
   
   public String getCommand() {
       return ( prop == null || prop.getProperty("COMMAND") == null )? "" : prop.getProperty("COMMAND");
   }
   public boolean isCommand(String comm) {
       return ( getCommand().matches(comm) );
   } 
   
   
   public synchronized boolean getBooleanProperty(String key) { return getBooleanProperty(key,prop); }
   public synchronized boolean getBooleanProperty(String key,Properties p) {
       String a = getProperty(key,"false",p);
       return ( a!= null && a.toLowerCase().matches("true") );
   }
   
   public synchronized int getIntProperty(String key            ) { return getIntProperty(key,"-1"); }
   public synchronized int getIntProperty(String key, String def) { return getIntProperty(key,"-1",prop); }   
   public synchronized int getIntProperty(String key, String def, Properties p) {
       String a = getProperty(key,def,p);
       return Integer.parseInt(a);
   }
   
   
   public synchronized void   setProperty(String key, String val) { setProperty(key,val,prop); }
   public synchronized void   setProperty(String key, String val, Properties p) {
       if ( key != null && !key.isEmpty() ) { 
           printf(getFunc("setProperty(String key, String val)"),2,"key:"+((key==null)?"NULL":key)+";  val:"+((val==null)?"NULL":val));
           if ( key != null && ! key.isEmpty() ) {
               if ( val == null ) {
                 p.remove(key);
               } else {
                 p.setProperty(key, val); 
               } 
           }
       }
   }
   
   
   public synchronized String getProperty(String key            ) { return getProperty(key,""); }
   public synchronized String getProperty(String key, String def) { return getProperty(key,def,prop); }   
   public synchronized String getProperty(String key, String def, Properties p) {
       String a = p.getProperty(key, def);
       if ( a != null &&  a.contains(__rep) ) { return getReplaceSeparatorBack(a); }
       return a;
   }
   
   public  synchronized Properties getIMapProperties(String[] key) {
       
       Properties p = null;
       int i=0; String skey="";
       while ( p == null && i < key.length) {
           p = iMap.get(key[i]);
           if ( p != null ) {
               skey=key[i];
           }
           i++;
       }
       if ( p == null ) {
            p = new Properties();
            iMap.put(key[0], p);
       }
       return p;
   }
   private HashMap<String, Properties> iMap = new HashMap<String, Properties>();
   public synchronized void loadProperty(String[] key) {
       final String func=getFunc("loadProperties(String key)");
       Properties p = getIMapProperties(key);
           String v = getProperty(key[0]);
       try {
           p.load(new FileInputStream(v));
       } catch(FileNotFoundException fne) {
           
       } catch(IOException io) {
           
       } catch(NullPointerException npe) {
           
       }
   }
}
