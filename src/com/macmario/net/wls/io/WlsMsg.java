/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.wls.io;

import com.macmario.general.Version;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 *
 * @author SuMario
 */
class WlsMsg extends Version{
    final private StringBuilder sw;
          private boolean msg_compl=false;
    
    WlsMsg(String s) {
        sw = new StringBuilder(s);
    }

    boolean isComplete() {
       if ( msg_compl ) { return msg_compl; }
       msg_compl = ( sw.indexOf(">") > 0 && sw.toString().endsWith(">"));
       return msg_compl ;
    }
    
    boolean add(String s) {
        if ( msg_compl ) { throw new RuntimeException("not added - msg complete"); }
        sw.append(s);
        return this.isComplete();
    }

    String getMessage() {
        //if ( ! isComplete() ) { throw new RuntimeException("message not complete"); }
        return sw.toString();
    }

    
    private boolean complete=false;
    void analyse() {
        final String func=getFunc("analyse()");
        if ( complete ) { return; }
        ArrayList<String> ar = new ArrayList();
        int i=sw.indexOf("<"); int j=0;
        while ( i < sw.length() && i >= 0 ) {
            
            printf(func,0,"substr("+j+","+i+") of "+sw.length()+" sw|"+sw.substring(j, i).replaceAll("^<", "").replaceAll("> $", "").replaceAll(">$", "")+"|");
            ar.add(sw.substring(j, i).replaceAll("^<", "").replaceAll(">$", "").replaceAll("> $", ""));
            j=i; i=sw.indexOf("<",i+1);
            printf(func,3," ("+j+","+i+") of "+sw.length()+" -  new ");
        }
        printf(func,3,"last |"+sw.substring(j)+"|");
        ar.add(sw.substring(j).replaceAll("^<", "").replaceAll(">[ ]$", ""));
        
        this.msgtime=getDate(ar.get(1));
        this.msgtyp=ar.get(2);
        this.msgsrv=ar.get(3);
        
        complete=true;
        
        System.out.println(func+" msgTime:"+this.msgtime+":");
    }
    
    private long msgtime=0L;
    private String msgtyp="info";
    private String msgsrv="localhost";
    
    

    private long getDate(String get) {
        final String func=getFunc("getDate(String get)");
        long ret = 0L;
        if ( get != null && ! get.isEmpty() ) {
            String[] sp = new String[] { "MMM dd, yyyy, hh:mm:ss,SSS a Z" };
            for (String map : sp) {
                                                    // Jun 3, 2017, 9:18:42,360 PM CEST
                    SimpleDateFormat sdf = new SimpleDateFormat(map); //ss, a Z ");  //"E, dd MMM yyyy hh:mm:ss Z");
                      Date dat   = sdf.parse(get, new ParsePosition(0) );
                      if ( dat != null ) {
                          ret = dat.getTime();
                          printf(func,2,"time |"+get+"|  goes to |"+ret+"|");
                          return ret;
                      }
            }
        }
        System.out.println("time |"+get+"|  goes to |"+ret+"|");
        return ret;
    }
}
