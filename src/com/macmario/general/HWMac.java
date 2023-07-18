/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.general;

import com.macmario.io.crypt.Crypt;
import static com.macmario.io.lib.IOLib.execReadToString;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Scanner;

/**
 *
 * @author SuMario
 */
public class HWMac extends Version{

    static private String  sysId="";
    static private String  usrId="";
    static private String hostId="";
    
    static public String getSystemId() {
        BufferedReader a = null;
        if ( ! HWMac.sysId.isEmpty() ) { return HWMac.sysId; }
        
        if      ( isWindows()       ) { a = read(new String[] { "wmic", "bios", "get", "serialnumber" });}
        else if ( isMac()           ) { a = read(new String[] { "/usr/sbin/system_profiler", "SPHardwareDataType" }); }//, " | awk '{ if($1==\"Serial\"){print $NF} }'" }); }
        else if ( isSolaris()       ) { }
        else if ( isAIX()           ) { }
        else if ( isUnix()          ) { a = read(new String[] { "awk", "'BEGIN{s=\"\"}{ s=\"Serial Number: \"$0 }END{print s}'", "/var/lib/dbus/machine-id", "2>/dev/null"} ); }
    
        if ( a != null ) {
            HWMac.sysId = readOut(a);
        }
    
        return HWMac.sysId;
            
    }
    
    static public String getUserInfo() {
        if (! HWMac.usrId.isEmpty()) { return HWMac.usrId; }
        
        HWMac.usrId =  System.getProperty("os.name")+"@"
                      +System.getProperty("os.version")+"@"                
                      +System.getProperty("os.arch")+"@"
                      +getHostName()
                      +System.getProperty("user.name");
        
        return HWMac.usrId;
    }
    
    private static String readOut(BufferedReader br) {
        String sn="";
        try {
            String marker = "Serial Number:";
            String line="";
            while ((line = br.readLine()) != null) {
		    if (line.contains(marker)) {
					sn = line.split(marker)[1].trim().replaceAll("[\t, ]", "");
					break;
		    }
                    if ("SerialNumber".equals(line)) {
					sn = br.readLine().trim();
					break;
                    }
                    
            }
        } catch(java.io.IOException io ) {}    
        return sn;
    }
    
    private static BufferedReader read(String[] command) {

		OutputStream os = null;
		InputStream is = null;

		Runtime runtime = Runtime.getRuntime();
		Process process = null;
		try {
			process = runtime.exec(command);
		} catch (IOException e) { }

		os = process.getOutputStream();
		is = process.getInputStream();

		try {
			os.close();
		} catch (IOException e) { }

		return new BufferedReader(new InputStreamReader(is));
    }
    
    public static String getHostName()  {
        if ( ! HWMac.hostId.isEmpty() ) { return HWMac.hostId; }
        try  {
            HWMac.hostId = execReadToString("hostname");
        } catch(Exception e) {
            HWMac.hostId="";
        }
        if ( HWMac.hostId.isEmpty() && HWMac.hostId.contains(".") ) {
             HWMac.hostId = (HWMac.hostId.split("."))[0];
        }
        
        if ( HWMac.hostId.isEmpty() ) {
            String OS = System.getProperty("os.name").toLowerCase();

            if (OS.indexOf("win") >= 0) {
                HWMac.hostId = ( System.getenv("COMPUTERNAME") == null ) ? "localhost" : System.getenv("COMPUTERNAME") ;
            } else {
                HWMac.hostId = ( System.getenv("HOSTNAME") == null ) ? "localhost" : System.getenv("HOSTNAME"); 
            }
        }
        return HWMac.hostId;
    }

    
}
