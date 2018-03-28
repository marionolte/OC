/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.lib;


public class IOLib {
   static public String execReadToString(String execCommand) throws java.io.IOException {
            Process proc = Runtime.getRuntime().exec(execCommand);
            StringBuilder sw=new StringBuilder();
            try (java.io.InputStream stream = proc.getInputStream()) {
                try (java.util.Scanner s = new java.util.Scanner(stream).useDelimiter("\\A")) {
                    sw.append( (s.hasNext() ? s.next().trim() : ""));
                }
            }
            try (java.io.InputStream stream = proc.getErrorStream()) {
                try (java.util.Scanner s = new java.util.Scanner(stream).useDelimiter("\\A")) {
                   sw.append( (s.hasNext() ? s.next().trim() : ""));
                }
            }
            return sw.toString();
   }
}   

