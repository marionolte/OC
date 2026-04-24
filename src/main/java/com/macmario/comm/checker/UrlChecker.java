/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.macmario.comm.checker;

import java.net.URL;

/**
 *
 * @author SuMario
 */
public class UrlChecker {
    public static int debug=0;

    static boolean test(String url) {
        try {
            log(2, "test url =>"+url+"<=");
            URL uri = new URL( ( (url.startsWith("/"))?"http://localhost"+url:url ) );
            log(3, "uri created "+url.toString() );
            String query=uri.getQuery();
            if (query != null && ! url.endsWith(query) ) { 
                log(2, "url=>"+url+"<= has query =>"+query+"<=");
                throw new RuntimeException("invalid query found"); 
            } else { 
                log(2, "base query check for |"+query+"| completed");
                /*if ( checkUnsavedChar(query) ) {
                    log(0, "ERROR query has unsaved characters - query =>"+query+"<=");
                    throw new RuntimeException("invalid query found");
                }*/
            }
            
        } catch (Exception e ) {
            log(1, "ERROR uri "+url+" generates the error "+e.getMessage());
            return false;
        }
        return true;
    }
    
    static void log(int level, String msg) { 
        if ( UrlChecker.debug >= level ) {
            if ( level > 0 ) { msg="DEBUG("+level+"/"+ UrlChecker.debug +") =>"+msg; } 
            System.out.println(msg);
        }
    }

    private static boolean checkUnsavedChar(String query) {
        boolean r=false;
        byte[] b = query.getBytes();
        for ( int i=0; i<b.length; i++ ) {
            log(3, "char b["+i+"]="+b[i]+" =>"+(char)b[i]);
        }
        return r;
    }
    
}
