/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.java;

import general.Version;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author SuMario
 */
class GCBlock extends Version {

    private String before="";
    private String after="";
    private String gcmsg;
    HashMap<String,String> map = new HashMap<String, String>();
    
    GCBlock(String s) {
        this.debug=4;
        String n=getBlockBefore(s);
      this.gcmsg=getBlockAfter(n);
      
        System.out.println("msg:"+this.gcmsg+":<-\nbefore:"+before+":<-\nafter:"+after+":<-");
        checkout();
    }
    
    private String getBlockBefore(String n) {
         Pattern pa = Pattern.compile("[1-2][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]T");
         Matcher ma = pa.matcher(n); ma.find();
         this.before = n.substring(0, ma.start());
         return n.substring(ma.start());
    }
    
    private String getBlockAfter(String n) {
         Pattern pa = Pattern.compile("Heap after");
         Matcher ma = pa.matcher(n); ma.find();
         this.after = n.substring(ma.start());
         return n.substring(0,ma.start());
    }
    
    private void checkout() {
        final String func=getFunc("checkout()");
        Pattern pa = Pattern.compile("\\ |,|:|\\(|\\)|\\[|\\]");
        Matcher ma = pa.matcher(this.gcmsg.replaceAll("\r", " ").replaceAll("\n", " "));
        int pos=0; int i=-1;
        while(ma.find(pos)) {
            String s = this.gcmsg.substring(pos, ma.start());
            printf(func,3,"sp["+(++i)+"]="+s);
            
            if      ( s.contains("=")             ) { String[] sp = s.split("="); map.put(sp[0], s.substring(sp[0].length()+1));printf(func,2,"key="+sp[0]+":  value:"+map.get(sp[0])+":");}
            else if ( s.contains("T[0-9][0-9]")   ) { System.out.println("a >"+s);
                map.put("time", s); pos=ma.end(); s = this.gcmsg.substring(pos, ma.start()); map.put("time", map.get("time")+":"+s);}
            
            pos=ma.end();
        }
        if ( debug > 1 ) {
            Iterator<String> itter = map.keySet().iterator();
            StringBuilder aw= new StringBuilder();
            while ( itter.hasNext() ) {
                String k = itter.next();
                aw.append(k).append("=").append(map.get(k)).append("|\n");
            }
            printf(func,2,"map contains:\n"+aw.toString());
        }
    }
}
