/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.io.java;

import com.macmario.general.Version;
import java.util.Calendar;
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
        final String func=getFunc("GCBlock(String s)");
        String n=getBlockBefore(s);
      this.gcmsg=getBlockAfter(n);
      
        printf(func,3,"msg:"+this.gcmsg+":<-\nbefore:"+before+":<-\nafter:"+after+":<-");
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
            printf(func,4,"sp["+(++i)+"]="+s);
            
            if      ( s.contains("=")             ) { 
                      String[] sp = s.split("="); 
                      map.put(sp[0], s.substring(sp[0].length()+1));
                      printf(func,2,"key="+sp[0]+":  value:"+map.get(sp[0])+":");
            } else if (  s.indexOf('.') > 1 ) { //"\\\\\\.") ) {
                printf(func,3,"time since start:"+s+"");
                map.put("timestartup", s);
            } else if ( s.equals("threshold") )  {
                String k=s;
                pos=ma.end(); ma.find(pos);
                s = this.gcmsg.substring(pos, ma.start()); 
                map.put(k, s);
                printf(func,3,"k="+k+"="+s+":");
                pos=ma.end(); ma.find(pos);  pos=ma.end(); ma.find(pos);
                k= k+this.gcmsg.substring(pos, ma.start()); 
                pos=ma.end(); ma.find(pos);
                s = this.gcmsg.substring(pos, ma.start()); 
                map.put(k, s);
                printf(func,3,"k="+k+"="+s+":");
            } else if ( s.contains("T") && s.matches(".*-\\d+-.*") && ! s.contains("GC") ) {
                map.put("time", s); 
                pos=ma.end(); ma.find(pos);
                s = this.gcmsg.substring(pos, ma.start()); 
                map.put("time", map.get("time")+":"+s);
                pos=ma.end(); ma.find(pos);
                s = this.gcmsg.substring(pos, ma.start());
                map.put("time", map.get("time")+":"+s);
                map.put("timestamp", getTimeStamp(map.get("time")));
                printf(func,2,"key=time:  value:"+map.get("time")+":   timestamp:"+map.get("timestamp")+":");
            } else if ( s.contains("T") && s.matches(".*-\\d+-.*") &&  s.contains("GC") ) {
                map.put("gctime", s.replaceAll("GC", "")); 
                pos=ma.end(); ma.find(pos);
                s = this.gcmsg.substring(pos, ma.start());
                map.put("gctime", map.get("gctime")+":"+s);
                pos=ma.end(); ma.find(pos);
                s = this.gcmsg.substring(pos, ma.start());
                map.put("gctime", map.get("gctime")+":"+s);
                map.put("gctimestamp", getTimeStamp(map.get("gctime")));
                printf(func,2,"key=gctime:  value:"+map.get("gctime")+":   gctimestamp:"+map.get("gctimestamp")+":");
            } else if ( s.matches(".*->.*") ) {
                String[] sp = s.split("->");
                printf(func,3,"b(->)   ->|"+s+"|<-  0:"+sp[0]+": 1:"+sp[sp.length-1]+":");
                Long d1=parseLong(sp[0]);
                Long d2=parseLong(sp[sp.length-1]);
                printf(func,2,"d1:"+d1+":  d2:"+d2+":   result:"+(d1-d2)+" bytes freed");
                map.put("memstart", ""+d1);
                map.put("memstop",  ""+d2);
                map.put("memfree",  ""+(d1-d2));
            } else if ( s.matches("ParNew") ) { map.put("GCTYP", s);
            } else {
                if ( ! s.isEmpty() ) { printf(func,3,"no map for |>"+s+"<-|");  }
            }
            
            pos=ma.end();
        }
        
        String pre="FIRST";
               updateCheckout(pre,this.before);
               pre="AFTER";
               updateCheckout(pre,this.after);
        
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
    
    private void updateCheckout(String pre,String msg) {
        final String func=getFunc("updateCheckout(String pre,String msg)");
        Pattern pa = Pattern.compile("\\ |,|:|\\(|\\)|\\[|\\]");
        Matcher ma = pa.matcher(msg.replaceAll("\r", " ").replaceAll("\n", " "));
        int pos=0; int i=-1;
        while(ma.find(pos)) {
            String s = msg.substring(pos, ma.start());
            printf(func,4,pre+"sp["+(++i)+"]="+s);
            pos=ma.end();
        }    
    }
    
    private String getTimeStamp(String s) {
        final String func=getFunc("getTimeStamp(String s)");
        //2017-05-31T16:33:29.483+0200
        if ( s!= null && ! s.isEmpty() ) {
            if ( s.contains("T")) {
                printf(func,3,"split time|"+s+"|");
                String[] ap=new String[] { "1970", "01", "01", "00", "00", "00", "000" };
                Matcher ma = Pattern.compile("T|-|:|\\+|\\.").matcher(s);
                int pos=0; int c=0;
                while(ma.find(pos)) {
                    String a=s.substring(pos, ma.start());
                    printf(func,3,pos+"-"+ma.start()+" get|"+a+"|   ("+ma.group()+")");
                    if ( c < ap.length && ! a.isEmpty() ) {
                        ap[c]=a; c++;
                        printf(func,2,"ap["+c+"]="+a);
                    }
                    pos=ma.end();
                }
                return ""+getDate(ap[0],ap[1],ap[2], ap[3],ap[4], ap[5], ap[6]);
            } else { return s; }            
        }        
        return "0";
    }
    
    private final long getDate(String yyyy, String MM, String dd, String HH, String mm, String ss, String SSS) {
        final String func=getFunc("getDate(String yyyy, String MM, String dd, String HH, String mm, String ss, String SSS)");
        Calendar calendar = Calendar.getInstance();
        if (yyyy == null || MM == null || dd == null || HH == null || mm == null || ss == null || SSS == null) {
            throw new IllegalArgumentException("One or more date parts are missing.");
        }
        calendar.set(Calendar.YEAR,         Integer.valueOf(yyyy));
        printf(func,4,"after year "+yyyy+"  ->"+calendar.getTimeInMillis());
        calendar.set(Calendar.MONTH,        Integer.valueOf(MM) - 1);
        printf(func,4,"after month "+MM+"  ->"+calendar.getTimeInMillis());
        calendar.set(Calendar.DAY_OF_MONTH, Integer.valueOf(dd));
        printf(func,4,"after day "+dd+"  ->"+calendar.getTimeInMillis());
        calendar.set(Calendar.HOUR_OF_DAY,  Integer.valueOf(HH));
        printf(func,4,"after hour "+HH+"  ->"+calendar.getTimeInMillis());
        calendar.set(Calendar.MINUTE,       Integer.valueOf(mm));
        printf(func,4,"after minute "+mm+"  ->"+calendar.getTimeInMillis());
        calendar.set(Calendar.SECOND,       Integer.valueOf(ss));
        printf(func,4,"after sec  "+ss+"  ->"+calendar.getTimeInMillis());
        calendar.set(Calendar.MILLISECOND,  Integer.valueOf(SSS));
        printf(func,4,"after msec "+SSS+"  ->"+calendar.getTimeInMillis());
        return calendar.getTimeInMillis();
    }
    
    private Long parseLong(String s) {
        try {
            Long d1 = 1L;
            if      ( s.toUpperCase().contains("K") ) { s=s.toUpperCase().replaceAll("K", ""); d1=1024L; }
            else if ( s.toUpperCase().contains("M") ) { s=s.toUpperCase().replaceAll("M", ""); d1=1024L*1024; }
            
            return Long.parseLong(s)*d1;
            
        } catch(Exception e) {
            
        }
        
        return 0L;
    }
}
