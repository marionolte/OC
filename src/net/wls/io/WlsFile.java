/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.wls.io;

import io.file.ReadFile;
import io.java.GCFile;
import io.thread.RunnableT;
import java.io.File;
import static java.lang.System.gc;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author SuMario
 */
public class WlsFile extends ReadFile {
    
    public WlsFile(String dir, String file) { super(dir, file); }    
    public WlsFile(String file            ) { super(file); }    
    public WlsFile(File file              ) { super(file); }
    
    private GCFile gcfile = null ;
    
    
    public void analyze() {
        ArrayList<WlsMsg> ar = new ArrayList(); 
        WlsMsg msg=null;    Pattern pa = Pattern.compile("^(\\d+.*|-\\d+.*)");
    
        for ( String s : super.readOut().toString().split("\n") ) {
            System.out.println("line >|"+s+"|<");
            if ( (pa.matcher(s)).find()                                                               ) { if (gcfile != null) { gcfile.addLine(s); } } // gclog
            else if ( msg == null ||   s.startsWith("<") || s.startsWith("####<") || msg.isComplete() ) { msg = new WlsMsg(s);  ar.add(msg); }
            else if ( msg != null && ! s.startsWith("<")                                              ) { boolean b = msg.add(s);  }
            
        }
        if ( ar.size() == 0 ) {
            System.out.println("INFO: no messages found");
            return;
        }
        System.out.println("start WlsA");
        WlsA ws = new WlsA(ar); ws.start();
        System.out.println("started WlsA");
        while( ws.isRunning() && ! ws.isClosed() ) { sleep(100); }
        System.out.println("closed WlsA");
        for( WlsMsg ms : ar ) {
            System.out.println("msg  >|"+ms.getMessage()+"|<");
        }
        
    }
    
    public static void main(String[] args) {
        for( String s : args ) {  
            ( new WlsFile(s) ).analyze();
        }
    }
    
    class WlsA extends RunnableT {
        ArrayList<WlsMsg> ar; 
         WlsA(ArrayList ar){
           this.ar=ar;  
             
         }
         
         
        @Override
        public void run() {
            setRunning();
            System.out.println("WlsA running");
            
            int l=1;
            if      ( ar.size() > 5001 ) { l=100; }
            else if ( ar.size() > 1001 ) { l=50;  }
            else if ( ar.size() >  501 ) { l=20;  }
            else if ( ar.size() >  101 ) { l=10;  }
            else if ( ar.size() >   51 ) { l=2;  }
            
            int range= ar.size() / l +1;
            int i=0; 
            System.out.println("range:"+range+" ");
            ArrayList<WlsAnna> arm= new ArrayList();
            while(i < ar.size() ) {  arm.add(new WlsAnna(ar,i,i+range));  i += range; }
            System.out.println("have added "+arm.size()+" WlsAnna threads");
            while( arm.size() > 0 ) {
                WlsAnna wa = arm.remove(0);
                        while(wa.isRunning() && ! wa.isClosed() ) { sleep(100); }
            }
            
            System.out.println("have completed all WlsAnna threads");
            
            setRunning();
            setClosed();
            
        }    
    }
    
    class WlsAnna extends RunnableT {
         ArrayList<WlsMsg> ar; int start; int stop;
         WlsAnna(ArrayList ar, int start, int stop){
           this.ar=ar; this.start=start; this.stop=stop; 
           start();    
         }
                 
         
        @Override
        public void run() {
            setRunning();
            for( int i=start; i<stop && i<ar.size() ; i++ ) {
                WlsMsg ms = ar.get(i);
                       ms.analyse();
                
            }    
            setRunning();
            setClosed();
        }        
    }
}
