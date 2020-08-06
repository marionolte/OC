package net.server;

import io.thread.RunnableT;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Properties;


public class Server extends RunnableT {
    boolean started;
    boolean proto;
    String host;
    int port;
    StringBuilder msg;
    static Properties p=null;
    
    ArrayList<TCPServer> arstcp = new ArrayList<TCPServer>();
    ArrayList<TCPProxy>  arptcp = new ArrayList<TCPProxy>();
    
    Server(boolean proto, String host, int port, StringBuilder msg) {
        this();
        this.proto= proto;
        this.host = host;
        this.port = port;
        this.msg  = msg;
        this.started=false;
    }
    
    private Server() {
        
    }


    private String getHost(String s) {
        String [] sp = s.split(":");         
        return sp[0];
    }
    
    private int getPort(String s) {
        String [] sp = s.split(":");         
        return Integer.parseInt(sp[1]);
    }
    
    int TIMEOUT=30000;
    
    private void Closed() {
        this.setClosed();
        for (TCPServer t : this.arstcp) { t.setClosed(); }
        for (TCPProxy  t : this.arptcp) { t.setClosed(); }
    }
    
    @Override
    public void run() {
         System.out.println("start server");
         if ( p != null ) {
             try {
             int BACKLOG= Integer.parseInt( p.getProperty("BACKLOG", "0") );
             TIMEOUT= Integer.parseInt( p.getProperty("TIMEOUT", ""+TIMEOUT) );
             int ACCEPTDELAY = Integer.parseInt( p.getProperty("ACCEPTDELAY", "300") );
             
             String s = p.getProperty("TCPServer");
             if ( s != null ) { 
                 String h = getHost(s);
                 java.net.InetSocketAddress inet = (h.isEmpty())?new java.net.InetSocketAddress(getPort(s)) : new java.net.InetSocketAddress(host,getPort(s));
                 
                 ServerSocketChannel ssc = ServerSocketChannel.open();
                       ssc.socket().bind(inet,BACKLOG);
                       ssc.configureBlocking(false);
                 TCPServer t = new TCPServer(ssc, ACCEPTDELAY );
                           t.start();
                           this.arstcp.add(t);
                           
                 System.out.println("server:"+s);
             }
                 
                    s = p.getProperty("TCPProxy");
             if ( s != null ) { 
                 String h = getHost(s);
                 java.net.InetSocketAddress inet = (h.isEmpty())?new java.net.InetSocketAddress(getPort(s)) : new java.net.InetSocketAddress(host,getPort(s));
                 
                 ServerSocketChannel ssc = ServerSocketChannel.open();
                       ssc.socket().bind(inet,BACKLOG);
                       ssc.configureBlocking(false);
                 TCPProxy t = new TCPProxy(ssc, ACCEPTDELAY );
                           t.start();
                           this.arptcp.add(t);
                    System.out.println("proxy:"+s); 
             }  
             
             
                    s = p.getProperty("TCPClient");
             if ( s != null ) { 
                 System.out.println("client:"+s);
             }
           } catch(Exception e) {
               Closed();
           }        
         }
         started=true;


         started=false;
         System.out.println("stop  server");
    }


    private class LWorker extends RunnableT {
        private int buflength=8192;
        private final SocketChannel sc;
        private       SocketChannel client=null;
        TCPServer srv = null;
        TCPProxy  proxy  = null;
        
        private LWorker(SocketChannel sc) throws IOException {
            this.sc=sc;
        }
        
        @Override
        public void run() {
          System.out.println("start  lworker"); 
          setRunning();
          
          ByteBuffer buf = ByteBuffer.allocateDirect(buflength) ; 
          try {
            while ( sc.isConnected() && ! this.isClosed() ) {
                  int r=sc.read(buf);
                  if  (client == null)  {  sc.write(buf); } else { client.write(buf); }
            }
          }  catch(IOException io) { }
          try {  sc.close(); } catch(IOException io) {}
          if (srv   != null ) this.srv.remove(this); 
          if (proxy != null ) this.proxy.remove(this); 
          setRunning();
        }
    }
    
    private class TCPServer extends RunnableT{
        ServerSocketChannel ss;
        int delay;
        TCPServer(ServerSocketChannel ss, int delay){
            this.ss=ss;
            this.delay=(delay > 0)?delay:300;
        }
        
        ArrayList<LWorker> list = new ArrayList<LWorker>();
        
        boolean remove(LWorker lw) { return this.list.remove(lw); }
        
        @Override
        public void run() {
          System.out.println("start  tcpserver");  
          setRunning();
            while ( isRunning() ) {
                try {   
                  SocketChannel sc = ss.accept();
                                
                  if ( sc != null ) {
                      LWorker lw = new LWorker(sc);
                              lw.start();
                              lw.srv=this;
                              list.add(lw);
                  } else { 
                      sleep(delay); 
                  }
                } catch (IOException io) {
                    sleep(delay);
                }  
            }
         setRunning();
         System.out.println("stop  tcpserver");
         for ( LWorker lw : list ) { lw.setClosed(); }
         try{ ss.close(); } catch(IOException io ){}
        }
    }
    
    private class TCPProxy extends TCPServer{
        
        TCPProxy(ServerSocketChannel ss,int delay){
            super(ss,delay);
        }
        
        @Override
        public void run() {
         System.out.println("start tcpproxy");    
         setRunning();
            while ( isRunning() ) {
             try {   
                  SocketChannel sc = ss.accept();
                                
                  if ( sc != null ) {
                      LWorker lw = new LWorker(sc);
                              lw.start();
                              lw.proxy=this;
                              list.add(lw);
                  } else { 
                      sleep(delay); 
                  }
                } catch (IOException io) {
                    sleep(delay);
                }  
            }   
         setRunning();
         System.out.println("stop  tcpproxy");
         try{ ss.close(); } catch(IOException io ){}
        } 
    }


    private class UDPServer extends RunnableT {

        @Override
        public void run() {
            setRunning();
            while ( isRunning() ) {

            }
            setRunning();
        }
    }
    
    static Server getInstance(String[] ar) throws Exception{
        Server s = new Server();
        
        if ( ar.length > 0 ) 
            for (int i=0; i< ar.length; i++ ) {
                if ( ar[i].matches("-conf") ) { s.p =  new Properties(); 
                                                p.load( new FileInputStream(ar[++i])  ); }
            }
        
               s.start();
        return s; 
    }
    
    public static void main(String[] args) throws Exception {
           Server s = getInstance(args);
           while ( s.isRunning() ) { sleep(300); }
           s.Closed();
    }
}
