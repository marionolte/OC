/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.tcp;

import java.io.InputStream;
import java.util.Scanner;

/**
 *
 * @author SuMario
 */
public class Host extends TcpHost{
    
    public static String getHostname() { 
       try { return execReadToString("hostname"); } catch(java.io.IOException io){ return "localhost"; }
    }
    
    public static String getSerial() { 
       try { return execReadToString("hostname"); } catch(java.io.IOException io){ return "localhost"; }
    }
    
    public static String getMainMac() {
      try { 
          String out=execReadToString( (  (isWindows())?"ipconfig /all":"ifconfig -a" ) );
        
        System.out.println("out:"+out+":");
      }catch(java.io.IOException ie) {}  
      return "00:37:17:44:88:EF";
    }
    
    public static String execReadToString(String execCommand) throws java.io.IOException {
        Process proc = Runtime.getRuntime().exec(execCommand);
        try (InputStream stream = proc.getInputStream()) {
            try (Scanner s = new Scanner(stream).useDelimiter("\\A")) {
                return s.hasNext() ? s.next().trim() : "";
            }
        }
    }
    
    public static void main(String[] args) {
          System.out.println("Hostname:"+getHostname());
          System.out.println("MacMain :"+getMainMac()+":");
    }
}
