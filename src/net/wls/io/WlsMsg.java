/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.wls.io;

import io.thread.RunnableT;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author SuMario
 */
class WlsMsg {
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
        if ( complete ) { return; }
        
        complete=true;
    }
}
