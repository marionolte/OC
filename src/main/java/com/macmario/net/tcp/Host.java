/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.tcp;

import static com.macmario.io.lib.IOLib.execReadToString;
import java.util.Random;

/**
 *
 * @author SuMario
 */
public class Host extends TcpHost{
    String host = null; 
    private int port;
    private int timeout;
    
    public Host(){
           this.port=-1;
           this.timeout=10000;
    }
    public Host(String ho) { this(); host = ho; }
    public Host(String ho,int port,int timeout) {
           this();
           this.port=port;
           this.timeout=timeout;
    }
    
    public int getPort() { return this.port; }
    public int getTimeout() { return this.timeout; }
    
   /* public static String getHostname() { 
       try { return execReadToString("hostname"); } catch(java.io.IOException io){ return "localhost"; }
    }*/
    
    public static String getSerial() { 
       try { return execReadToString("hostname"); } catch(java.io.IOException io){ return "localhost"; }
    }
    
    public static String getMainMac() {
      String out=null;  
      try { 
          out=execReadToString( (  (isWindows())?"ipconfig /all":"ifconfig -a" ) );
          int ind = out.indexOf("ether "); 
          if ( ind > 0 ) {
              String[] sp = out.substring(ind).split(" ");
              out=sp[1].toUpperCase();
          } else {
              ind = out.toLowerCase().indexOf("physical address");
              if (ind != -1) {
                    ind = out.indexOf(":");
                    if (ind != -1) {
                        out = out.substring(ind + 1).trim();
                    }
              }      
          }       
          //System.out.println("out:"+out+":");
          return out;
      }catch(java.io.IOException 
              | StringIndexOutOfBoundsException 
              | NullPointerException ie) {}
      return (out!=null)?out:getRandMac();
    }
    
    static private String randMac=null;
    static private String getRandMac() {
        if ( randMac == null ) {
            randMac = getNewRandomMac("");
        }
        return randMac;
    }
    static public String getNewRandomMac(String base){
              base =(base!=null)?base:"";
        String mac = base;
        if ( isNotNullOrEmpty(mac) && ! mac.contains(":") ) {
            int len=mac.length();
            mac=mac.substring(0, 2);
            //System.out.println("mac:"+mac+": ->"+len);
            for ( int i=2; i<len; i++){
                if ( (i+1) < len ) {
                   mac += ":"+base.substring(i, i+2); 
                   //System.out.println("mac["+i+"]:"+mac+":");
                   i++;
                }   
            }
        }
        Random r = new Random();
        for (int i = 0; i < 6; i++) {
                int n = r.nextInt(255);
                mac += ":"+String.format("%02x", n);
        }
        //System.out.println("mac =>"+mac+"<- len:"+mac.length());    
        return ((mac.length()>17)?mac.substring(0,17):mac).toUpperCase();
    }
    
    public String getHost(){
        return (host==null)? TcpHost.getHostname():host;
    }
    public String setHost(String ho){
        host=ho;
        return getHost();
    }
    
    public static void main(String[] args) {
          System.out.println("Hostname:"+getHostname());
          System.out.println("MacMain :"+getMainMac()+":");
          System.out.println("RandMac :"+getNewRandomMac(args[0])+":");
    }

   
}
