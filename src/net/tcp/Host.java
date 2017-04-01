/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.tcp;

import general.Version;
import java.io.InputStream;
import java.util.Scanner;

/**
 *
 * @author SuMario
 */
public class Host extends Version{
    
    public static String getHostname() { 
       try { return execReadToString("hostname"); } catch(java.io.IOException io){ return "localhost"; }
    }
    
    
    public static String execReadToString(String execCommand) throws java.io.IOException {
        Process proc = Runtime.getRuntime().exec(execCommand);
        try (InputStream stream = proc.getInputStream()) {
            try (Scanner s = new Scanner(stream).useDelimiter("\\A")) {
                return s.hasNext() ? s.next().trim() : "";
            }
        }
    }
    
}
