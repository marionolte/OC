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
        System.out.println("create WlsA - "+(System.currentTimeMillis()-starttime));
        WlsA ws = new WlsA(ar); ws.start();
        System.out.println("started WlsA - "+(System.currentTimeMillis()-starttime));
        while( ws.isRunning() //&& ! ws.isClosed() 
             ) { sleep(100); }
        System.out.println("closed WlsA - "+(System.currentTimeMillis()-starttime));
        for( WlsMsg ms : ar ) {
            //System.out.println("msg  >|"+ms.getMessage()+"|<");
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
            if      ( this.ar.size() >100001 ) { l=100; }
            else if ( this.ar.size() >10001  ) { l=50;  }
            else if ( this.ar.size() > 1001  ) { l=20;  }
            else if ( this.ar.size() >  501  ) { l=10;  }
            else if ( this.ar.size() >   51  ) { l=2;   }
            
            int range= this.ar.size() / l +1;
            int i=0; 
            System.out.println("WlsA - range:"+range+" ");
            ArrayList<WlsAnna> arm= new ArrayList();
            long d=System.currentTimeMillis();
            while(i < ar.size() ) {  
                System.out.println("WlsA - add new WlsAnna["+i+"] - "+(System.currentTimeMillis()-d));
                arm.add(new WlsAnna(ar,i,i+range));  
                System.out.println("WlsA - start   WlsAnna["+i+"] - "+(System.currentTimeMillis()-d));
                arm.get(arm.size()-1).start(); 
                System.out.println("WlsA - started WlsAnna["+i+"] - "+(System.currentTimeMillis()-d));
                i += range; 
            }
            System.out.println("WlsA - have added "+arm.size()+" WlsAnna threads - runs:"+(System.currentTimeMillis()-d));
            i=0;
            while( arm.size() > 0 ) {
                WlsAnna wa = arm.remove(0);
                long fa = System.currentTimeMillis();
                System.out.println("WlsAnna["+(i++)+"] - wait4complete:"+fa);
                        while(wa.isRunning() 
                              //  && ! wa.isClosed() 
                             ) { sleep(100); }
                long f = System.currentTimeMillis();
                System.out.println("WlsAnna["+(i++)+"] - completed:"+f +"   ("+(f-fa)+")");
                
            }            
            System.out.println("have completed all WlsAnna threads - runs:"+(System.currentTimeMillis()-d));
            
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
