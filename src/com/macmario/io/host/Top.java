/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.io.host;

import com.macmario.general.Version;
import java.util.HashMap;

/**
 *
 * @author SuMario
 */
public class Top extends Version{


    final public static String myusage="\nusage():\n";
    
    public static void main(String[] args) {
       Top t = getInstance(args);
    }

    synchronized public static Top getInstance(String[] args) {
        if ( args.length == 0 ) { args=new String[]{""}; }
        HashMap<String, String> map = com.macmario.io.lib.IOLib.scanner(args,myusage);
        if ( map.get("_usage_").equals("true") ) {
            printUsage(""+myusage);
            System.exit(-1);
        }
        
        Top t = new Top(map);
        return t;
    }

    
    private final HashMap<String, String> map;
    private Top(HashMap<String, String> map) {
        this.map = map; 
    }
    
}
