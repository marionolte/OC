/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.tcp;

import java.io.ObjectInputStream;
import java.util.ArrayList;
import com.macmario.io.buffer.RingBufferObjects;
import com.macmario.io.thread.RunnableT;

/**
 *
 * @author SuMario
 */
public class ObjectReaderThread extends RunnableT {
    private ClientSocket cs;
    private Thread  th;
    private RingBufferObjects buf = new RingBufferObjects();
    private ObjectInputStream in;

    private int runs=-1; 
    
    
    ObjectReaderThread(ClientSocket cs) { 
        this.cs = cs;
        th = new Thread(this, "reader thread client socket");
        th.start();
    }

    
    void setReader(ObjectInputStream in){
        synchronized (this) {
            this.in= in; 
            this.th.notify();
            this.notify();
        }    
        
    }
    
    private boolean couldRun(){
       if ( runs < 0 ) { return true; } 
       return  cs.isConnected();
    }
    
    
    private final String lock=this.toString(); 
    
    public synchronized void push(Object m) {  
        
        boolean a=false;
        do {
             if ( ! buf.isFull() ) { 
               synchronized(lock) {   
                 buf.push(m); a=true; 
               }   
             } else { sleep(1000); }
        }while( ! a );
       
    }
    
    public boolean isReady(){
        synchronized(lock) { 
            return (buf.isEmpty() ) ? false : true;
        }
    }
    
    public synchronized Object read() {  
        if( buf.isEmpty() ) { return null; }
        synchronized(lock) { 
            return buf.pop(); 
        }
    }
    
    public synchronized Object[] readAll() {
        synchronized(lock) {
            ArrayList req = new ArrayList();
            while ( ! buf.isEmpty() ) {
                req.add( buf.pop());
            }
            Object[] n = new Object[ req.size() ];
            for (int i=0; i<req.size(); i++ ) { n[i]=req.get(i); }
            return n;
        }
    }
    
    @Override
    public void run() {
        setRunning();
         do {
             try {
              if ( in == null ) {
                  synchronized (this) {
                      try { wait(1000); } catch (Exception e) {}
                  }
              } else {
                   Object readObject = in.readObject();
                   if ( readObject != null ){ push(  readObject ); }
              }
             } catch (Exception ex){} 
              
         } while ( couldRun() ) ;  
         setRunning();
    }
}
