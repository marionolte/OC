/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.comm.checker;

import java.util.ArrayList;

/**
 *
 * @author SuMario
 */
public class CheckTimer {
    final String[] begin;
    final String[] end;
    boolean begintest=false;
    boolean endtest=false;
    int debug=-1;
    
    
    CheckTimer(String[] begin, String[] end, int debug) {
        this.begin=begin; this.end=end;
        for (int i=0; i< begin.length; i++ ) { if(begin[i]!=null){begintest=true; i=begin.length; } }
        for (int i=0; i<   end.length; i++ ) { if(  end[i]!=null){  endtest=true; i=  end.length; } }
        this.debug=debug;
    }
    
    private boolean checked=true;
    private boolean found=false;
    boolean check(String line) {
        if ( !begintest && ! endtest ) { return true; }
        if ( line == null || line.isEmpty() ) { return found; }
        setTimePattern();
        if ( checked ) {
           // search for start  
           log(3,"check line|@|"+line+"|@|"); 
           String[] sp = line.split("[\\W]");
           short test=0;
           for (int i=0; i< sp.length; i++ ) {
               if ( sp[i].isEmpty() ) {  test++;
               } else {
                sp[i]=getMonth(sp[i]);   
                if (  sp[i].startsWith("[0-9]") && sp[i].endsWith("[0-9]") ) {   
                    if ( test < 2 ) {
                     log(3,"splitout ("+i+"/"+sp.length+") |@|"+sp[i]+"|@|");
                    } else { return found; }
                }
               } 
           }
        } else {
           // start found / search for end  
        }
        return found;    
    }

    private ArrayList arrayB=null;
    private ArrayList arrayE=null;
    
    private String getMonth(String m) {
        final String n =m.toUpperCase();
        if      ( n.matches("JAN") || n.startsWith("JAN") ){m="01"; }
        else if ( n.matches("FEB") || n.startsWith("FEB") ){m="02"; }
        else if ( n.matches("MRZ") || n.startsWith("MRZ") ){m="03"; }
        else if ( n.matches("APR") || n.startsWith("APR") ){m="04"; }
        else if ( n.matches("MAY") || n.startsWith("MAY") ){m="05"; }
        else if ( n.matches("JUN") || n.startsWith("JUN") ){m="06"; }
        else if ( n.matches("JUL") || n.startsWith("JUL") ){m="07"; }
        else if ( n.matches("AUG") || n.startsWith("AUG") ){m="08"; }
        else if ( n.matches("SEP") || n.startsWith("SEP") ){m="09"; }
        else if ( n.matches("OCT") || n.startsWith("OCT") ){m="10"; }
        else if ( n.matches("NOV") || n.startsWith("NOV") ){m="11"; }
        else if ( n.matches("DEC") || n.startsWith("DEC") ){m="12"; }
        
        return m;
    }
    
    private void setTimePattern() {
       // DAY-MONTH-YEAR HH:MIN:SEC - exampe to use '01-MAR-2013 10:12:45'  
       // 0   1     2    3  4   5 
    // <Mar 21, 2013 10:01:29 AM CET>
    // <2013-03-21 10:01:09    <2013-Mar-21, 10:01:29 AM CET>
    // [20/Mar/2013:15:19:47
    // [2013-03-20T15:14:07.523+02:00]     
        if ( arrayB != null ) { return; }
        arrayB=new ArrayList();
        arrayE=new ArrayList();
        if ( begintest ) {
             String a = getTime(begin);
             StringBuilder sw = getDate(begin,a);
        }
        if ( endtest ) {
             String a = getTime(end);
             StringBuilder sw = getDate(end,a);
            
        }
        
    }
    
    private String getTime(String[] a) {
        StringBuilder sw=new StringBuilder();
        sw.append( ( ( a[3] != null )? a[3]:"" ) ).append( (a[4]!=null )? ":":""  );
        sw.append( ( ( a[4] != null )? a[4]:"" ) ).append( (a[5]!=null )? ":":""  );
        sw.append( ( ( a[5] != null )? a[5]:"" ) );
        log(2,"return time|"+sw.toString()+"|");
        return sw.toString();
    }
    
    private StringBuilder getDate(String[] a,String t) {
        StringBuilder sw=new StringBuilder();
        //sw.append( "."+a[0]+"."+a[1]+  );
        log(2,"getDate \n|@|"+sw.toString()+"\n|@|");
        return sw;
    }
    
    void reset() {
        if      ( begintest ) { checked=true;  } 
        else if ( endtest   ) { checked=false; }  
        found=false;
    }
    
    private void log(final int level, final String msg) {
       if ( debug >= level  ) 
        System.out.println(msg);
    }
}
