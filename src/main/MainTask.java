/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import general.Version;
import java.io.File;
import java.util.Properties;

/**
 *
 * @author SuMario
 */
abstract class MainTask extends Version{
    Properties prop=null;
    public Properties parseArgs(String[] args) {
        Properties p = new Properties();
        if ( args != null && args.length > 0 ) {
            for(int i=0; i < args.length; i++ ) {
                if ( args[i].matches("-d") ) { debug++; }
                else if ( args[i].matches("-version")  ){ p.setProperty("COMMAND", "VERSION"); }
                else if ( args[i].contains("=")        ){ String[] sp=args[i].split("=") ;  p.setProperty(sp[0], args[i].substring(sp[0].length()+1)); }
                else {
                    if( args[i].startsWith("-") ) {
                        String a=args[i].replaceAll("^-", "");
                        p.setProperty(a, getReplaceSeparator(args[++i]));
                        //System.out.println(a+"="+p.getProperty(a)+"|");
                    } else {
                        p.setProperty("COMMAND", args[i].toUpperCase() ); 
                    }    
                }
            }        
        }      
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
   
}
