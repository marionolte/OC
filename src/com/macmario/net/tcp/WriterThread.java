/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.tcp;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import com.macmario.io.buffer.RingBuffer;
import com.macmario.io.thread.RunnableT;



/**
 *
 * @author SuMario
 */
public class WriterThread extends RunnableT {
    private ClientSocket cs;
    private Thread  th;
    
    private RingBuffer buf = new RingBuffer();
    
    private BufferedOutputStream   out=null;
    private ObjectOutputStream    oout=null;
    
    
    public WriterThread(ClientSocket cs) { 
        this.cs = cs;
        
        th = new Thread(this, "writer thread client socket");
        th.start();
    }
    
    public synchronized void write(Object o) {
        boolean a=false;
        do {
             if ( ! buf.isFull() ) { buf.push(o); a=true; } else { sleep(1000); }
        }while( ! a );
    }
    
    public synchronized void write(String m) {  write( (Object) m ); }
    
    public void flush(){
      try {  
        if ( out != null ) { out.flush();  }
        if (oout != null ) { oout.flush(); }
      } catch(IOException io) {}  
    }
    
    public void close(){
        try { 
            if ( out != null  ) { out.close();  }
        } catch(IOException io) {}  
    }
    
    void setWriter(BufferedOutputStream out){ this.out=out;}
    void setWriter(ObjectOutputStream out  ){ this.oout=out; }
    
    
    @Override
    public void run() {
        setRunning();
        while ( cs.isConnected() ){
            if ( buf.getSize() > 0  ) {
                try {
                      if (out != null ) { 
                          String f=(String) buf.pop();
                          printf("run()",5,"write :"+f+":");
                          out.write( f.getBytes() );
                          
                      } else if ( oout != null ) {
                          oout.writeObject( buf.pop() );
                          
                      }
                      flush();
                      
                } catch (IOException ex) {}
                
            } else {
               sleep(500);  
            }
            
        }        
        close();
        setRunning();
    }

    boolean getObjWriterReady() { return ( oout == null ) ? false : true; }
    boolean getObjectWrite()    { return ( oout == null ) ? false : true; }
        
 
    
    
}
