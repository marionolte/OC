/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.macmario.net.tcp;

import com.macmario.io.thread.RunnableT;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.HashMap;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import com.macmario.net.exception.SocketException;
import com.macmario.io.file.Cache;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.SocketAddress;


/**
 *
 * @author SuMario
 */
public class ClientSocket extends RunnableT{

    private boolean inSSL=false;
    private String host;
    private int port;
    private java.net.Socket socket;
    private SSLSocket ssocket;
    private BufferedOutputStream out=null;
    private BufferedReader in=null;
    private Thread  t;
    private String user="";
    private String password="";
    private ReaderThread rt;
    private WriterThread wt;
    private boolean onError=false;
    private HashMap map = new HashMap();
    private String localhostname ;
    private Proxy  proxy=null;
    private Socket proxysocket;
    
    
    public ClientSocket(){ setMyHostName(); addSSLTrustedDomain(getMyDomainname()); }
    public ClientSocket(String host, String port             ) throws UnknownHostException, IOException { this(host, Integer.valueOf( port ),false, false);}
    public ClientSocket(String host, int port                ) throws UnknownHostException, IOException { this(host, port,                false, false);}
    public ClientSocket(String host, int port, boolean inssl ) throws UnknownHostException, IOException { this(host, port,                inssl, false);}
    public ClientSocket(String host, int port, boolean inssl, boolean noproxy ) throws UnknownHostException, IOException {
        this.inSSL=inssl;
         this.host=host;
         this.port=port;
         
         setMyHostName();
         if(noproxy) { this.setNoProxy(); } 
         
         if ( inssl && map.isEmpty() ) {
             map.put("provider","SunJSSE");      
             map.put("trusttype","SunX509");
             map.put("truststore","JKS");
             map.put("truststorefile","mytruststore");
             map.put("password","changeit");
             map.put("trustall",""+false);
             map.put("trustmydomain",""+true);
             map.put("domain0","example.com");
             map.put("domaincount","1");
         }
         if ( inssl ) { addSSLTrustedDomain(getMyDomainname()); }
         
         startComm();
    }

    public String[] getMyDomainname(){
        String meth="getMyDomainname()";
        String lim = "\\.";
        String[] lf=localhostname.split("\\.");
        int i=(lf.length >0)? lf.length-1:0;
        
       printf(meth,3,"have for :"+localhostname+":  "+i+" domain levels =>"+lf.length);
        
        String[] sp = new String[i+1];
        if ( i==0 ) {
             sp[0]="local";
        } else {
             StringBuilder sb=new StringBuilder(); int j=0;
             for (i=lf.length; i>0; i--) {
                 if (j>0) { 
                     sp[j]=lf[i-1]+sb.toString();
                     printf("getMyDomainname()",2,"add domain :"+sp[j]);
                 }
                 sb.append(".").append(lf[i-1]);
                 j++;
             }
        }
        printf("getMyDomainname()",1,"return "+sp.length+" domains from:"+localhostname+":");
        return sp;
    }
    private void setMyHostName() {
        try {
         localhostname = java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
          localhostname="localhost";  
        } 
    }
    
    public void setPort(String port){ setPort( Integer.parseInt(port) ); } 
    public void setPort(int port){ this.port=port;}
    public int  getPort(        ){ return port; }

    public void   setHost(String host){
        this.host=getHost(host);
        if ( ! this.host.matches(host) ) {
            String[] fp=host.split(":");  
            setPort(fp[fp.length-1]);
        } 
    }
    public String getHost(           ){ return host; }
    public static String getHost(String hostname) {
        String[] fp=hostname.split(":");
        StringBuilder b=new StringBuilder(); b.append(fp[0]);
        if ( fp.length > 1) {
           for (int i=1; i<fp.length-1; i++) {
                b.append(":"+fp[i]);
           }
           try { 
               int i = Integer.parseInt(fp[fp.length-1] );
           } catch (Exception e) {
               b.append(":"+fp[fp.length-1]); 
           }
        }
        return b.toString();
    }

    public void     setSSL(boolean ssl){ this.inSSL=ssl;}
    public boolean  getSSL(           ){ return inSSL; }

    public void setUser(String string) {
        String[] sp = string.split(":");
        if ( sp.length==2 ){
            user=sp[0]; password=sp[1];
        } else {
            user=string; password="";
        }
        if ( user == null ) { user=""; }
    }
    
    public String getUser    (){ return (user!=null)?user:""; }
    public String getPassword(){ return (password!=null)?password:""; }

    public boolean startComm() { // throws UnknownHostException, IOException{
         boolean b=false;
         try { initSocket(); b=true; }
          catch ( UnknownHostException he ) { }
          catch ( IOException io ) {}

         if ( b ) {
            super.start();
            t=getThread();
        
            rt = new ReaderThread(this); rt.setReader(in);  rt.start();
            wt = new WriterThread(this); wt.setWriter(out); wt.start();
         }
         return b; 
    }

    public boolean connectSocket() throws UnknownHostException, IOException {
        initSocket();
        return isConnected();
    }
    
    public boolean isReachable(int timeout) {
        final String func=getFunc("isReachable()");
        
        printf(func,3,"is reacheable called for"+getHost()+":"+getPort());
        SocketAddress saddr = new InetSocketAddress(getHost(), getPort());
        Socket sock = new Socket();
        int time=(timeout >0 && timeout<30000)?timeout:1000;
        try {
               sock.setSoTimeout(     time);
               sock.setSoLinger(true, time);
               sock.setTcpNoDelay(true);
               sock.setReuseAddress(false);
        } catch(Exception e) {}       
        boolean online = false;
        try {
             sock.connect(saddr,time);
             sleep(500);
             online=( sock.isConnected() && ! sock.isClosed() );
             try {sock.close();}catch(Exception e){}
        } catch (IOException io) {
            printf(func,1,"socket exception for : "+getHost()+":"+getPort()+" reason "+io.getMessage());
             
            online=false;       
        }
        
        printf(func,2,"is reacheable ends for : "+getHost()+":"+getPort()+" with "+online);
        
        return online;
    }
    
    private SocketTyp connSSLTyp=SocketTyp.TLS;
    
    public  void setSSLConnectionType(Enum typ) { }
    
    private String    proxyHost=null;
    private int       proxyPort=-1;
    private    Socket proxyTunnel=null;
    private SSLSocket proxySSlTunnel=null;
    private boolean   proxySSL=false;
    private boolean   noproxy=false;
    
    public void setNoProxy() { this.noproxy=true; proxyHost=null; checkProxy(); }
    
    private void checkProxy() {
        if ( proxyHost != null ) { return; }
        if ( noproxy ) { proxy=null; proxyHost=null;  return; }
        boolean ptyp=false;
        
        try {
            if ( inSSL &&  ( proxyHost != null && proxyPort>0 ) ) {
                proxySSL=true;
                proxyHost = System.getProperty("https.proxyHost");
                proxyPort = Integer.getInteger("https.proxyPort").intValue();
                
            }
        } catch(RuntimeException rl) {
                proxyHost=null;
        }
        try {
            if ( proxyHost == null ) {
                 proxyHost = System.getProperty("http.proxyHost");
                 proxyPort = Integer.getInteger("http.proxyPort").intValue();
                 proxySSL  = false;
            }
        } catch(RuntimeException rl) {
                proxyHost=null;
        }    
        try {
            if ( proxyHost == null ) {
                 proxyHost = System.getProperty("socks.proxyHost");
                 proxyPort = Integer.getInteger("socks.proxyPort").intValue();
                 proxySSL  = false;
                 ptyp=true;
            }
        } catch(RuntimeException rl) {
                proxyHost=null;
        }
        
        if ( proxyHost != null) {
             if (ptyp) {
                proxy = new Proxy( Proxy.Type.SOCKS, new InetSocketAddress(proxyHost, proxyPort) ); 
             } else {
                proxy = new Proxy( Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort) );  
             }
        } else {
                proxy = null;
        }
        //System.out.println("Proxy:"+proxy+" proxyHost:"+proxyHost);
    }
    
    private void initSocket() throws UnknownHostException, IOException {
       String meth=getFunc("initSocket()");
       checkProxy();
       if ( inSSL ) {
            try {
                printf(meth,4," like to open an new ssl client connection");
                SSLContext sslContext = SSLContext.getInstance( connSSLTyp.getName() );

                printf(meth,3," set up a TrustManager that trusts everything we need");
                sslContext.init( null, MyTrustManager.getTrustManager(map), new SecureRandom() );
                printf(meth,3,"sslContext with TrustManager initialized");
                proxyHost=null;
                if ( proxyHost == null ) {
                    printf(meth,3,"like to create a new direct ssl client connection");
                    ssocket= (SSLSocket) sslContext.getSocketFactory().createSocket(host, port);
                    printf(meth,3,"getting ssl socket - starting handshake");
                    ssocket.startHandshake();
                } else {
                    if ( proxySSL ) {
                        printf(meth,3,"like to create a new proxy SSL - ssl client connection");
                        proxySSlTunnel = (SSLSocket) sslContext.getSocketFactory().createSocket(proxyHost, proxyPort);
                        proxySSlTunnel.connect(new InetSocketAddress(host, port));
                        ssocket=proxySSlTunnel;
                        
                        
                    } else {
                        printf(meth,3,"like to create a new plain proxy connection for new ssl client connection");
                        // proxy over plain Socket
                        proxyTunnel = new java.net.Socket(proxyHost, proxyPort);
                        
                        
                    }
                }
                printf(meth,3,"ssl client connection created");
/*                /*
	     * Set up a socket to do tunneling through the proxy.
	     * Start it off as a regular socket, then layer SSL
	     * over the top of it.
	     *
	    tunnelHost = System.getProperty("https.proxyHost");
	    tunnelPort = Integer.getInteger("https.proxyPort").intValue();

	    Socket tunnel = new Socket(tunnelHost, tunnelPort);
	    doTunnelHandshake(tunnel, host, port);

	    /*
	     * Ok, let's overlay the tunnel socket with SSL.
	     *
	    SSLSocket socket =
		(SSLSocket)factory.createSocket(tunnel, host, port, true);

  */              
                
                
                //System.out.println("create ssl client socket");
                //SSLSocketFactory factory =  (SSLSocketFactory)SSLSocketFactory.getDefault();
                //System.out.println("have factory");
                //        ssocket =  (SSLSocket)factory.createSocket(host, port);
                //System.out.println("have ssl socket");
                        
                //        ssocket.startHandshake();
                //System.out.println("handshake complete");
                socket = (java.net.Socket) ssocket;


            } catch (Exception e) {
                throw new SocketException( e.getMessage() );
            }
        } else {
          try { 
            //test
            //proxyHost=null;
            Host ho = new Host(); 
            //System.out.println("proxyhost:"+proxyHost +" noproxy:"+noproxy); 
              
            if ( noproxy || proxyHost == null ||  ho.isLocalAddress(host) ){ //host.matches("localhost") || host.matches("127.0.0.1") ) {
                 printf(meth,2,"like to create socket to =>"+host+":"+port);
                 socket = new java.net.Socket(host, port);
            }else {
                 printf(meth,2,"like to create socket to =>"+host+":"+port+" over proxy =>"+proxyHost+":"+proxyPort);
                 //Socket s = new Socket(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("socks.mydom.com", 1080)));
                 if ( proxy == null ) { proxy=new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyHost, proxyPort)); }
                 proxysocket = new Socket(proxy);
                 proxysocket.connect(new InetSocketAddress(host,port));
                 socket=proxysocket;
            }
          }catch(Exception e) { 
              throw new SocketException( e.getMessage() );
          } 
        }
        if ( socket != null ) {
            printf(meth,2,"socket created "+socket);

            createOutputStream();
            //createInputStream();
        } else {
            printf(meth,1,"socket creation failed to host"+host+":"+port);
        }    
        
        printf(meth,4,"return");
    }

    public void setSocket   (   Socket sock ) { this.socket=sock; }
    public void setSSLSocket(SSLSocket sock ) { this.ssocket=sock; }
    
    public void setSocketTimeout(int i) throws java.net.SocketException {
        if ( i<0 ) { return; }
        if ( socket != null ) { socket.setSoTimeout(i); socket.setSoLinger(true, 1);}
        else {
            throw new SocketException( "no socket created");
        }
    }
    public int getSocketTimeout() { 
        int i=30000;
        try     { i=socket.getSoTimeout();}catch(Exception e){} 
        finally { return (i>0)?i:30000;   }
    }
    
    public boolean isOnError(){ return this.onError; }
    
    public boolean createOutputStream(){
        
        if (out == null) {
            try {
                // out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
                out = new BufferedOutputStream(socket.getOutputStream());
                
                
            } catch (IOException ex) {
                out=null;
                printf("createOutputStream()",1,"ERROR: creating OutputStream - reason:"+ex.getMessage()); 
                this.onError=true;
            }
            finally {
                if( wt != null ) { wt.setWriter(out); }
                printf("createOutputStream()",2,"output streams created:"+out);
            }
        }
        if ( in == null ) { createInputStream(); }
        // System.out.println("output stream created  ? :"+out);
        return ( out != null ) ? true: false;
    }

    public boolean createInputStream(){
        if ( in == null ) {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
            } catch (IOException ex) {
                 in=null; this.onError=true;
                 printf("createInputStream()",1,"ERROR: creating InputStream - reason:"+ex.getMessage());
            }
            finally {
                if ( rt != null ) { rt.setReader(in); }
                printf("createInputStream()",2,"input streams created:"+out);
            }
        }
        return ( in != null ) ? true: false;
    }
    
    public InputStream getInputStream(boolean bin) throws IOException {
        if ( bin ) {
           return (InputStream) new java.io.DataInputStream( socket.getInputStream() ) ;
        } else {
           return (InputStream) socket.getInputStream(); 
        }   
    }

    public boolean createObjectWriter(){
        if ( ! wt.getObjWriterReady() ) {
             
        }
        return  wt.getObjWriterReady();
    }
    
    public void write(Object o) {
        if ( wt.getObjectWrite() && ! wt.getObjWriterReady() ){ createObjectWriter(); }
        wt.write(o);
    }
    

    public void write(String s    ) throws UnsupportedEncodingException { 
        printf("write(String s)",2,"writing ==>"+s+"<==   object write:"+wt.getObjectWrite()+":" );
        //if ( wt.getObjectWrite() ) { write( (Object) s); } else { write(s.getBytes()); } 
        this.write(s.getBytes());
    }
    public void write(byte[] bytes){

        if ( createOutputStream() ) {
           try {
             out.write(bytes);
             out.flush();
             lastSendAction=System.currentTimeMillis();
           } catch(IOException io){ 
              throw new SocketException(io);
           }
        } else {
           throw new SocketException( "no output stream create on socket");
        }
    }


    private long lastReceiveAction=System.currentTimeMillis();
    private long lastSendAction=System.currentTimeMillis();
    private long lastReceiveCheck=System.currentTimeMillis();
    
    public boolean readAvailblabe(int wait){
        String meth="readAvailblabe(int wait)";
        boolean b=false;
        if ( createInputStream() ) {
           long d=System.currentTimeMillis();
           
           while ( ! b ) {
                
                try {
                    b=in.ready();
                    lastReceiveCheck=System.currentTimeMillis();
                } catch (IOException io){
                    printf(meth,0,"IOException :"+io);
                }

                if ( ! b ) {
                    if ( b || (System.currentTimeMillis()-d)>wait  ){ break; }
                    sleep(500);
                }
           }
           if (b) {
                printf(meth,2,"return avaliable "+b);
           } else {
                printf(meth,7,"return avaliable "+b);
           }     
        } else {
            printf(meth,4,"no InputStream created"); // 3
        }
        return b;
    }

    public boolean readAvailblabe(){
        return readAvailblabe(3000);
    }
    
    public int readAvailableCount() {
        if ( readAvailblabe(100) ) {
           try { 
             return in.read();
           } catch(IOException io) {
             return -1;  
           } 
        } else {
            return -1;
        }
    }
    
    public synchronized int    getReadCount(){ synchronized(addLock){ return tmp.toString().getBytes().length; } }
    public synchronized String read(int count) throws IOException {
        return read(count, getReadCount() );
    }
    
    public synchronized String read(int start, int last) throws IOException {
        
        StringBuilder s=new StringBuilder();

        byte[] bytes = (read()).getBytes();
        for (int i=start; i<bytes.length && i<last ;i++){ s.append( bytes[i]); }
        
        lastReceiveAction=System.currentTimeMillis();
        return s.toString();
    }

    private StringBuilder tmp = new StringBuilder();
    private String addLock="StringBuilder lock";

    public synchronized String waitOnRead(StringBuilder sw) throws IOException {
        final String func="waitOnRead()";
        //if ( createInputStream() ) {
        wt.write(sw.toString());
        
        long d=System.currentTimeMillis();
             out:
             while( ( ! rt.isReady() || d+30000 < System.currentTimeMillis() ) ) { 
                 sleep(100);
             }
             printf(func, 3, "wait for ready "+( System.currentTimeMillis()-d )+" msec "+rt.isReady());
             final String back = rt.readAll();
             printf(func, 3, "return:"+back+"|@|");
             return back;
        //}
        //return "";
    }
    public synchronized String read() throws IOException {
        final String func="read()";
        String line=""; int n; char[] b = new char[8192];
        if ( createInputStream() ) {
          printf(func, 2, "InputStream created");  
          synchronized ( addLock ){
            printf(func, 3, "addLock received");  
            tmp = new StringBuilder();
            //System.out.println("start read");
            while ( readAvailblabe() ) {
                    n=in.read(b);
                    tmp.append(b,0,n);
            }
            line=tmp.toString();
            lastReceiveAction=System.currentTimeMillis();
          }
        } else { printf(func,1,"InputStream not created"); }
        printf(func, 4, "return last received ("+lastReceiveAction+") |@|"+line+"|@|");
        return line;
    }

    public synchronized String readStream(InputStream is) throws IOException {
        StringBuilder mytmp=new StringBuilder();
        int n;
        byte[] b = new byte[8192];

        if ( is != null && is.available() > 0 ) {
          synchronized ( addLock ){
            while ( is.available() > 0 ) {
                    n=is.read(b);
                    for ( int i=0; i<n;i++) { mytmp.append( (char) b[i] ); }
            }
            lastReceiveAction=System.currentTimeMillis();
          }
        } else { }
        return mytmp.toString() ;
    }

    public String readln() throws IOException {
        // if (! createInputStream() ) { System.out.println("no input stream - return"); return ""; }
        
       
        printf("readln()",4,"read available now");

        String s = rt.readln().replaceAll("\r\n", "\n").replaceAll("\r", "\n");
        printf("readln()",2,"return String |@>|"+s+"|<@|");
        
        return s;
        
    }

    public  void close(){
        runs=false;
        if ( wt != null ) { wt.close(); }
        if ( rt != null ) { rt.close(); }

    }

    public void flush(){  if (wt!=null){ wt.flush(); } }

    private boolean runs=true;
    @Override
    public void run() {
            setRunning();
            while (runs) {
                    if ( socket.isClosed() ) { close(); }
                    sleep(100);
                    flush();
            }

            flush();
                
            
            try { if ( out != null ) out.close();  } catch (IOException ex) { }

            try { if ( in  != null ) in.close();  } catch (IOException ex) { }

            if (getSSL()) {
                try { if ( ssocket != null ) ssocket.close(); } catch (IOException ex) { }
            }
            try { if ( socket != null ) socket.close(); } catch (IOException ex) { }

    }


    public boolean isConnected(){
           boolean b;
           if ( ! getSSL() ) {
               b = (socket != null) ? ( !  socket.isClosed() &&  socket.isConnected() ) : false;
           }else{
               b = (ssocket!= null) ? ( ! ssocket.isClosed() && ssocket.isConnected() ) : false;
           }
           printf("isConnected()",7,"return "+b);
           return b;
    }

  
    public Socket getSocket() {
        if ( getSSL() ) {
            return (Socket) ssocket;
        } else {
            return socket;
        }
    }  
    
    private void setSSLvalues(String key, String value) { 
          key=key.toUpperCase();
          if ( key.matches("domain")) {
             int i = Integer.parseInt( (String) map.get("domaincount") ) ; i++;
             map.put(key+""+i, value);
             map.put("domaincount", ""+i);
          } else if ( key.matches("trustall")  ||  key.matches("trustmydomain") ) {   
             map.put(key , ""+( ( value.toLowerCase().matches("true") )?true:false ) );  
          } else {
             map.put(key, value);
          }
    }
    
    public void setSSLStorePasswd(String  val){ setSSLvalues("password",val); }
    public void setSSLTrustAllCerts(boolean b){ setSSLvalues("trustall",""+b); }
    public void setSSLTrustMyDomain(boolean b){ setSSLvalues("trustmydomain",""+b);}
    public void setSSLStoreFile(String  fname){ setSSLvalues("truststorefile",fname);}
    public void setSSLTrustStore(String typ){
          if ( StoreType.isValidTyp(typ) ) {    setSSLvalues("truststore",typ); }
          else {
              printf("setSSLTrustStore(String typ)",1,"not a valid TrustStore typ:"+typ+": - leave:"+(String)map.get("truststore"));
          }
    }
    public void setSSLProvider(String val)      { setSSLvalues("provider", val);} // SunJSSE
    public void setSSLTrustCertTyp(String val)  { setSSLvalues("trusttype",val);} // SunX509
    public void addSSLTrustedDomain(String[] val) { 
        if ( val == null || val.length == 0 ) { return; }
        for(int i=0; i<val.length; i++) {addSSLTrustedDomain(val[i]);}
    }
    public void addSSLTrustedDomain(String val) {
        if (val == null || val.isEmpty() || val.matches("\\.") ) { return; }
        setSSLvalues("domain",val.toLowerCase());
        setSSLTrustMyDomain(true);
    }
    public synchronized void delSSLTrustedDomain(String val) {
        if ( val == null || val.isEmpty() || val.matches(".")) { return; }
        val=val.toLowerCase();
        int j = Integer.parseInt( (String) map.get("domaincount") );
        StringBuilder sb=new StringBuilder();
        for (int i=0; i<j; i++) {
            String t = (String) map.get("domain"+i);
            if ( t != null && ! t.matches(val)) {
                sb.append(t).append("@");
            } 
            map.put("domain"+i,"trusted");
        }
        j=0;
        String[] sp= sb.toString().split("@");
        for(int i=0; i<sp.length; i++){
            map.put("domain"+i, sp[i]); j++;
        }
        map.put("domaincount",""+j);
        if ( j > 0 ) { setSSLTrustMyDomain(true); } else { setSSLTrustMyDomain(false); }
    }
    
    private boolean useClientCert = false;
    private String  ClientCertAlias = "mycert";
    //private Certificate[] clientCert; 

    private Cache cache=null;
    public void setCache(Cache cache) { this.cache=cache; }

    public boolean compare(String hos, int p, boolean sec) {
        return ( this.host==hos && this.port == p && this.inSSL == sec )? true:false;
    }

    
}
