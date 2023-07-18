/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.tcp;

import java.io.IOException;
import java.io.ObjectOutputStream;
import com.macmario.io.buffer.RingBufferObjects;
import com.macmario.io.thread.RunnableT;


/**
 *
 * @author SuMario
 */
public class ObjectWriterThread extends RunnableT {
    private ClientSocket cs;
    private Thread  th;
    
    private RingBufferObjects buf = new RingBufferObjects();
    
    private ObjectOutputStream out=null;
    

    
    public ObjectWriterThread(ClientSocket cs) { 
        this.cs = cs;
        th = new Thread(this, "reader thread client socket");
        th.start();
        
    }
    
    public boolean getObjWriterReady(){ return (out==null)? false:true; }
    
    public synchronized void write(Object m) {  
        boolean a=false;
        do {
             if ( ! buf.isFull() ) { buf.push(m); a=true; } else { sleep(1000); }
        }while( ! a );
    }
    
    public void flush(){
      try {  
        if ( out != null  ) { out.flush();  }
      } catch(IOException io) {}  
    }
    
    public void close(){
        try {  
                if ( out != null  ) { out.close();  }
        } catch(IOException io) {}  
    }
    
    void setWriter(ObjectOutputStream   out) {
        synchronized (this) {
            this.out=out;
            this.th.notify();
            this.notify();
        }    
    }
    
    @Override
    public void run() {
        
        while ( cs.isConnected() ){
            if ( buf.getSize() > 0  ) {
                try {
                      if (out != null ) {
                          out.writeObject( buf.pop() );
                          flush();
                      } else {
                        try {
                          synchronized (this) { wait(500); }
                        }catch(Exception e) {}  
                      }
                } catch (IOException ex) {}
                
            } else {
               sleep(500);  
            }
            
        }        
        close();
        setRunning();
    }
}
