/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.tcp;

import com.macmario.io.thread.RunnableT;
import java.io.BufferedReader;



/**
 *
 * @author SuMario
 */
public class ReaderThread extends RunnableT {
    private ClientSocket cs;
    private Thread  th;
    //private RingBuffer buf = new RingBuffer();  // Strings
    private Object[] el = ( Object[] ) new Object[ 16384 ];;
    private BufferedReader in=null;
    
    
    private int runs=-1; 
    
    private ReaderThread() { }
    ReaderThread(ClientSocket cs) { 
        this();
        this.cs = cs;
        
        th = new Thread(this, "reader thread client socket");
        th.start();
        
    }

    void setReader(BufferedReader    in){this.in = in; }
    
    
    
    private int number = 0;      // number of elements on queue
    private int first  = 0;      // first element in the queue
    private int last   = 0;      // last element in the queue
    private boolean isFull()  { return (number == this.el.length)?true:false; }
    private boolean isEmpty() { return (number == 0             )?true:false; }
   
    private boolean push( Object o ) { 
        String meth="push( Object o )";
        printf(meth,5,"starting");
        if ( this.number == this.el.length ) { return false; }
        printf(meth,4,"getLock   last:"+last+"  length:"+this.el.length);
        getLock("queue");
        printf(meth,4,"getLock");
        
        this.el[last] = o;
        this.last = (this.last + 1) % this.el.length ;  // set next element wrap-around
        this.number++;
        
        printf(meth,5,"clearLock");
        clearLock("queue");
        printf(meth,3,"new Object stored");
        return true;
    }
        
    public synchronized Object pop() { 
        if ( isEmpty() ) { return ""; }
        
         getLock("release");
        
        Object item = this.el[ this.first ];
                      this.el[ this.first ] = null;      // cleanup for gc
                      this.number--;
                      this.first = ( this.first + 1 ) % this.el.length; // set next element + wrap around
        
        clearLock("release");
        
        return  item ; 
    }
    
    private String locked=null;
    private boolean islocked() { 
        if ( locked == null ) { return false; }
        String f = new String(locked);
        if (  locked == f  && hold+300<System.currentTimeMillis() ) { return false; }
        return true; 
    }

    private void getLock(String s) {
            String meth="getLock(String s)";
            
            printf(meth,4,"lock is locked=="+islocked()+" with "+locked+"  request:"+s);
            while( this.islocked() ) {
                //System.out.println("locked :"+lock);
                   printf(meth,6,"lock is locked=="+islocked()+" with "+locked+"  requested is:"+s);
                try {
                   this.wait(300);
                } catch ( InterruptedException l ) {}
            }
            this.locked=s; hold=System.currentTimeMillis();
            printf(meth,4,"lock locked has now:"+s);
    }

    private long hold=System.currentTimeMillis(); 
    
    private synchronized void clearLock(String s) {
        if ( this.locked.equals(s)) { locked=null; hold=System.currentTimeMillis(); }
    }
    
    
    private boolean couldRun(){
       if ( runs < 0 ) { return true; } 
       return  cs.isConnected();
    }
    
    public void close() {
        runs=1;
    }
    
    
    private final String lock=this.toString(); 
    
    public void push(String m) {  
        
        boolean a=false;
        do {
             printf("push(String m)",5,"new data add:"+a);
             if( a ) { sleep(1000); }  
             a=true; 
             printf("push(String m)",4,"new data :"+m+":"); 
        }while( ! push( (Object) m) );
        printf("push(String m)",3, "new String pushed");
    }
    
    
    public boolean isReady(){
        //return ( isEmpty() ) ? false : true;
        return ! isEmpty();
    }
    
    public synchronized String read() {  
        if( isEmpty() ) { return ""; }
        return (String) pop(); 
        
    }
    
    public synchronized String readln() { 
        printf("readln()",5,"check for new data ... "); 
        while ( ! isReady() ) { sleep(500); }
        printf("readln()",5,"new data received"); 
        return readAll();
    }
    
    public synchronized String readAll() {
        
            StringBuilder req = new StringBuilder();
            while ( ! isEmpty() ) {
                req.append( this.pop());
            }
            return req.toString();
        
    }
    
    @Override
    public void run() {
         setRunning();
         int n; char[] b = new char[16384]; 
         StringBuilder req = new StringBuilder(); 
         do {
             try {
              if ( in != null && in.ready() ) {
                
                  n = in.read(b);
                  if ( n > 0 ) {
                        req.append(b,0,n);
                        printf("run()",6,"append :"+req.toString() );
                  }
                  if ( req.length() >0 ) { 
                      printf("run()",4,"push append |"+req.toString()+"|"); 
                      this.push(req.toString()); 
                      req = new StringBuilder(); 
                  }
                
              } else {
                  sleep(300);
              }
             } catch (Exception ex){} 
              
         } while ( couldRun() ) ;  
         setRunning();
    }
    
   
}
