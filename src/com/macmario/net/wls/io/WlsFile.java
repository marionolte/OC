/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.wls.io;

import com.macmario.io.file.ReadFile;
import com.macmario.io.java.GCFile;
import com.macmario.io.thread.RunnableT;
import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 *
 * @author SuMario
 */
public class WlsFile extends ReadFile {
    
    public WlsFile(String dir, String file) { this(dir+File.separator+file); }    
    public WlsFile(String file            ) { this( new File(file) ); }    
    public WlsFile(File file              ) { 
            super(file);  
            starttime=System.currentTimeMillis();
    }
    private long starttime=0L;
    private GCFile gcfile = null ;
    
    
    public void analyze() {
        final String func=getFunc("analyze()");
        ArrayList<WlsMsg> ar = new ArrayList(); 
        WlsMsg msg=null;    Pattern pa = Pattern.compile("^(\\d+.*|-\\d+.*)");
    
        for ( String s : super.readOut().toString().split("\n") ) {
            printf(func,3,"line >|"+s+"|<");
            if ( (pa.matcher(s)).find()                                                               ) { if (gcfile != null) { gcfile.addLine(s); } } // gclog
            else if ( msg == null ||   s.startsWith("<") || s.startsWith("####<") || msg.isComplete() ) { msg = new WlsMsg(s.replace("####<", "<") );  ar.add(msg); }
            else if ( msg != null && ! s.startsWith("<")                                              ) { boolean b = msg.add(s.replace("####<", "<"));  }
            
        }
        if ( ar.size() == 0 ) {
            System.out.println("INFO: no messages found");
            return;
        }
        printf(func,4,"create WlsA - "+(System.currentTimeMillis()-starttime));
        WlsA ws = new WlsA(ar); ws.start();
        printf(func,4,"started WlsA - "+(System.currentTimeMillis()-starttime));
        while( ws.isRunning() //&& ! ws.isClosed() 
             ) { sleep(100); }
        printf(func,4,"closed WlsA - "+(System.currentTimeMillis()-starttime));
        for( WlsMsg ms : ar ) {
            System.out.println("msg  >|"+ms.getMessage()+"|<");
        }
        
    }
    
    public static void main(String[] args) {
        int debug=0;
        for( String s : args ) {  
            if ( s.matches("-d") ) { debug++; }
            WlsFile wf = new WlsFile(s);
                    wf.debug=debug;
                    wf.analyze();
        }
    }
    
    class WlsA extends RunnableT {
        ArrayList<WlsMsg> ar; 
         WlsA(ArrayList ar){
           this.ar=ar;  
         }
         
         
        @Override
        public void run() {
            final String func=getFunc("run()");
            setRunning();
            printf(func,4,"WlsA running");
            
            int l=1;
            if      ( this.ar.size() >100001 ) { l=100; }
            else if ( this.ar.size() >10001  ) { l=50;  }
            else if ( this.ar.size() > 1001  ) { l=20;  }
            else if ( this.ar.size() >  501  ) { l=10;  }
            else if ( this.ar.size() >   51  ) { l=2;   }
            
            int range= this.ar.size() / l +1;
            int i=0; 
            printf(func,3,"WlsA - range:"+range+" ");
            ArrayList<WlsAnna> arm= new ArrayList();
            long d=System.currentTimeMillis();
            while(i < ar.size() ) {  
                printf(func,4,"WlsA - add new WlsAnna["+i+"] - "+(System.currentTimeMillis()-d));
                arm.add(new WlsAnna(ar,i,i+range));  
                printf(func,4,"WlsA - start   WlsAnna["+i+"] - "+(System.currentTimeMillis()-d));
                arm.get(arm.size()-1).start(); 
                printf(func,4,"WlsA - started WlsAnna["+i+"] - "+(System.currentTimeMillis()-d));
                i += range; 
            }
            printf(func,3,"WlsA - have added "+arm.size()+" WlsAnna threads - runs:"+(System.currentTimeMillis()-d));
            i=0;
            while( arm.size() > 0 ) {
                WlsAnna wa = arm.remove(0);
                long fa = System.currentTimeMillis();
                printf(func,4,"WlsAnna["+(i++)+"] - wait4complete:"+fa);
                        while(wa.isRunning() 
                              //  && ! wa.isClosed() 
                             ) { sleep(100); }
                long f = System.currentTimeMillis();
                printf(func,4,"WlsAnna["+(i++)+"] - completed:"+f +"   ("+(f-fa)+")");
                
            }            
            printf(func,3,"have completed all WlsAnna threads - runs:"+(System.currentTimeMillis()-d));
            
            setRunning();
            setClosed();
            
        }    
    }
    
    class WlsAnna extends RunnableT {
         ArrayList<WlsMsg> ar; int start; int stop;
         WlsAnna(ArrayList ar, int start, int stop){
           this.ar=ar; this.start=start; this.stop=stop; 
         }
                 
         
        @Override
        public void run() {
            setRunning();
            long d= System.currentTimeMillis();
            System.out.println("WlsAnna["+start+"] started :"+d);
            int j=(this.ar.size()-1);
            int r=(j<stop)?j:stop;
            for( int i=start; i<r ; i++ ) {
                System.out.println("WlsAnna["+start+"] - get ["+i+"/"+j+"] for msg analyse  - "+(System.currentTimeMillis()-d));
                WlsMsg ms = ar.get(i);
                       ms.analyse();
                
            }    
            setRunning();
            setClosed();
            System.out.println("WlsAnna["+start+"] complete - "+isRunning()+" -  runs:"+(System.currentTimeMillis()-d));
        }        
    }
}
