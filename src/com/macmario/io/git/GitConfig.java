/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.macmario.io.git;

import com.macmario.io.file.ReadFile;
import java.util.HashMap;

/**
 *
 * @author MNO
 */
public class GitConfig {
    
    final private ReadFile conf;
          private long read;
          private final HashMap<String, HashMap> map;
    GitConfig(String file) { 
         conf = new ReadFile(file);
         map = new HashMap();
         map.put("global", new HashMap<String,String>());
         read=0L;
         read();
    }
    
    
    
    String getRepo() {
         return getRepo("master");
    }
    
    String getRepo(String branch) {
        read();
        HashMap<String, String> mp = map.get(branch);
        if ( mp != null ) {
            String ret = mp.get("url");
            if ( ret == null || ret.isEmpty() ) {
                mp = map.get( mp.get("remote") );
                if ( mp != null ) {
                    ret = mp.get("url");
                }
            }
            if ( ret == null ) ret="";
            return ret;
        }
        return "";
    }
    
    private void read() {
        if ( conf.isReadableFile() && read < conf.getLastModified() ) {
             String _mkey="global";
             HashMap<String, String> _map = map.get(_mkey);
             for ( String line : conf.readOut().toString().split("\n") ) {
                 if ( ! line.startsWith("#")) {
                   if ( line.startsWith("[") ) {
                      String sp[] = line.substring(1).split("}")[0].split(" ");
                      String brz=sp[0]; String nam=brz;
                      if ( sp.length > 1 ) {
                          nam = sp[1].replaceAll("^\"", "").replaceAll("\"$", "");
                      }
                      _map = map.get(nam);
                      if ( _map == null ) { _map = new HashMap(); }
                      _map.put("type", brz);
                      _map.put("name", nam);
                   } else {
                    if ( ! line.isEmpty() ) {
                        String[] sp = line.split("=");
                        sp[0] = sp[0].replaceAll("\t", "").replaceAll(" ", ""); 
                        if ( sp[1].contains("#")) { sp[1] = sp[1].split("#")[0]; }
                        sp[1] = sp[1].replaceAll("^ ", "").replaceAll(" $", "");
                        _map.put(sp[0], sp[1]);
                    }
                   }
                  
                 }
             }
             this.read = System.currentTimeMillis();
        }
    }
}
