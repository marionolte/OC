/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.comm.mail;

import com.sun.mail.imap.IMAPFolder;
import com.macmario.general.Version;
import com.macmario.io.account.User;
import com.macmario.io.crypt.Crypt;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import com.macmario.io.file.ReadFile;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import com.macmario.net.tcp.Host;


/**
 *
 * @author SuMario
 */
public class Imap extends Version {
     private User       user;
     private Properties props;
     private Session    session=null;
     private Folder     folder;
     private Store      store;
     private String     storefile; 
     //private String     host;
     private String     port;
     private final ServiceHost host;
    
    public Imap(User user, Host ho) {
        this.user=user;
        this.host=new ServiceHost("imap", ho.getHost());
    }
    public Imap(String u, String p) {
        this(new User(u,p) {},new Host("localhost"){ int port=993; int timeout=30000; });
    } 
    public Imap(String u, String p, Host host) {
        this(new User(u,p) {},host);
    }
    
    public void setClosed() throws MessagingException{ closed=true;
        if ( store == null ) { return; }
        Long d = System.currentTimeMillis()+60000L;
        setCLoutter:
        while( scanRun ) { 
            if ( d > System.currentTimeMillis() ) sleep(500); 
            else break setCLoutter;
        }
        close();
    }
    
    private void init() {
        final String func="Imap::init()";
        println(2,func+" user:"+user.getUsername()+":  password ("+((user.getPassword().isEmpty())?"NO PASSWORD":"PASSWORD are set")+") to IMAP=>"+host+":"+port);

        
        storefile=System.getProperty("user.dir")+File.separator+"store";   
        
        println(2, func+"check to load "+storefile);
        ReadFile fa= new ReadFile(storefile);
        if ( fa.isReadableFile() ) {
             println(2, func+"like to load objects from "+storefile);
             Iterator<LMessage> itter = fa.loadObjects();
             println(1, func+"has objects "+itter.hasNext() );
             while(itter.hasNext()) { 
                 LMessage lmsg = itter.next();
                 println(1, func+"load cached msg:"+lmsg.getID()+" =>"+lmsg);
                 mmap.put(lmsg.getID(), lmsg);
                 mailar.add(lmsg);
             }
        } else {
            println(3, func+"storefile "+storefile+" are not readable");
        }
       
        /*
            props.setProperty("mail.imap.ssl.enable", "true"); 
            props.setProperty("mail.imap.ssl.socketFactory.class", "MySSLSocketFactory"); 
            props.setProperty("mail.imap.ssl.socketFactory.fallback", "false"); 
            
            Session session = Session.getInstance(props, null); 
            - See more at: https://javamail.java.net/docs/SSLNOTES.txt#sthash.hWMHJJV8.dpuf
        */
        
        props = new Properties();
        props.put("mail.imap.user", user.getUsername() );
        props.put("mail.imap.auth", "true");
        props.put("mail.imap.host", host.getHost());
        props.put("mail.imap.port", port);
        props.put("mail.imap.connectiontimeout", host.timeout ); //imapConnectTimeout);//Socket connection timeout value in milliseconds. Default is infinite timeout.
        props.put("mail.imap.timeout", 30000 ); // idle time
        props.put("mail.imap.connectionpooltimeout", 30000);
        props.put("mail.imap.ssl.enable", "true");
        props.put("mail.imap.ssl.protocols", "TLSv1");
        props.put("mail.imaps.socketFactory.fallback", "false");
        //props.put("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.setProperty("mail.imap.ssl.socketFactory.class", "comm.mail.MySSLSocketFactory"); 
        //props.put("mail.imap.socketFactory.fallback", "false");
        //SASL  auth 
        props.put("mail.imap.sasl.enable", "true");  // for testing 
        props.setProperty("mail.imap.sasl.usecanonicalhostname","true");
        props.setProperty("mail.imap.sasl.mechanisms", "LOGIN PLAIN");
        props.setProperty("mail.imap.auth.login.disable", "false");
        props.setProperty("mail.imap.auth.plain.disable", "false");
        
        if ( debug > 2 ) {
           props.setProperty("mail.debug", "true"); 
           props.setProperty("javax.security.sasl.level", "FINEST");
        }

        println(3, func+"get instance with properties:"+props);
        
        session = Session.getInstance(props, 
                
                        new javax.mail.Authenticator() {
                             @Override
                             protected PasswordAuthentication getPasswordAuthentication() {
                                        return new PasswordAuthentication(user.getUsername(), user.getPassword()); 
                             }
                        }
                );
        //session.setDebugOut( System.err );
        
        
        
        try {  
            store = session.getStore("imap");
            println(2,func+" store init:"+store);
        } catch(Exception e) {
            println(1,func+" init imap store - getting exception "+e.getMessage());
            if( debug >0 ) {e.printStackTrace();}
        }
        
        MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
                          mc.addMailcap("application/xml;;  x-java-content-handler=com.sun.mail.handlers.text_plain");
                          mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
                          mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
                          mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
                          mc.addMailcap("multipart/mixed;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
                          mc.addMailcap("multipart/text;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
                          mc.addMailcap("multipart/plain;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
                          mc.addMailcap("multipart/xml;; x-java-content-handler=com.sun.mail.handlers.text_plain");
                          mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
                          mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
        CommandMap.setDefaultCommandMap(mc);
    }
    
    private void open() throws MessagingException {
        final String func="Imap::open()";
        if ( store == null ) { 
            init();
            if ( store == null ) {
                println(1,func+" imap store could not initialized - ERROR - return");
                return;
            } else {
                println(3,func+" imap store initialized");
            }   
        } else {
            println(2, func+" imap store already initialized");
        }
        
        if ( ! store.isConnected() ) {
            println(2,func+" imap initialize connection to "+host.getHost()+":"+port+"  pass("+( (user.isPasswordSet() )?"EMPTY":"ARE SET")+") user("+user+")");
            if ( isWindows() ) {
                store.connect(user.getUsername(), user.getPassword() );
            } else {
                store.connect(host.getHost(), Integer.parseInt(port), user.getUsername(), user.getPassword());
            }    
            println(3,func+" imap initialize connection to "+host+":"+port+" done ("+store+")");
        } else {
            println(3, func+" imap store already connected");
        }
        
        if ( folder == null ) {
            folder = (IMAPFolder) store.getFolder("inbox");
            println(3,func+" initialize folder "+folder );
        } else {
            println(3, func+" imap folder to INBOX already connected");
        }   
        
        if(!folder.isOpen()) {
            folder.open(Folder.READ_ONLY);
            println(2,func+"imap folder are open in read only mode");
        } else {
            println(2,func+"imap folder already open");
        }
    }
    
    private void close() throws MessagingException {
        if ( folder != null && folder.isOpen() ) {
            try { folder.close(true); } catch(java.lang.IllegalStateException is) { } finally { folder=null; }
        }
        try { store.close(); } catch(Exception e) {} finally{ store=null; }
    }
    
    synchronized public ArrayList<LMessage> getMailHeads() throws MessagingException{ 
        scan(); 
        return mailar; 
    } 
    
    final static public String sepa="|_@_|";
    volatile private ArrayList<LMessage> mailar = new ArrayList<LMessage>(); 
    private int newMail=-1; 
    private int allMail=-1;
    public int getUnreadMails(){ 
        try { newMail=folder.getUnreadMessageCount(); } catch (Exception e) { }
        finally{
               return newMail;  
        } 
    }
    public int getMailCount() { 
        try { allMail=folder.getMessageCount(); } catch (Exception e) { }
        finally{
                return allMail; 
        }
    }
    
    public int getNewMailCunt() { 
        int i=-1;
        try { i=folder.getNewMessageCount(); } catch (Exception e) { }
        finally{
            return i; 
        }
    }
    
    /* from the mail api
    private final static int ANSWERED_BIT       = 0x01;
    private final static int DELETED_BIT        = 0x02;
    private final static int DRAFT_BIT          = 0x04;
    private final static int FLAGGED_BIT        = 0x08;
    private final static int RECENT_BIT         = 0x10;
    private final static int SEEN_BIT           = 0x20;
    private final static int USER_BIT           = 0x80000000;
    */
    
    volatile private HashMap<String, LMessage> mmap=new HashMap<String, LMessage>();
    public  LMessage getMessage(String id) { return mmap.get(id); }
    private LMessage updateMsg(Message msg) {
        if ( msg == null ) { return null; }
        LMessage lmsg = getMessage(""+msg.getMessageNumber());
        if ( lmsg == null ) {
            if ( ! msg.isExpunged() ) {
              lmsg = new LMessage(msg);
              mmap.put(lmsg.getID(), lmsg);
            } 
        } else {
             lmsg.updateMsg();
             if ( lmsg.isDeleted() ) { mmap.remove(lmsg.getID()); }
        }
        return lmsg;
    }
    public HashMap<String, LMessage> getMailList()  { return mmap; }
    
    public int getMessageCount(){ return mmap.size(); }
    public Iterator<String> getMessageIDs() { return mmap.keySet().iterator(); }
    
    private boolean closed=false;
    private boolean scanRun=false;
    private int lastMsg=-1;
    synchronized private void scan() throws MessagingException {
        final String func="Imap::scan()";
        boolean clean=false ; scanRun=true;
        println(3, func+" skip imap open closed:"+closed);
        if ( closed ) { 
            println(1, func+" return now - closed:"+closed);
            scanRun=false; return; 
        }
        open();
        println(2, func+" store "+store);
        if ( store == null ) { open(); }
        println(2, func+" folder "+folder);
        if ( folder  != null ) { 
          println(2, func+" imap "+store+ " online:"+imapOnline);  
          if (imapOnline) {  
                println(2, func+" scan messages in "+folder);
                newMail = folder.getUnreadMessageCount();
                Message[] messages = folder.getMessages();
                allMail = messages.length;
                println(2, func+" receive "+messages.length+" messages");
                ArrayList<LMessage> ar = new ArrayList<LMessage>(); 
                for ( int i= messages.length-1;  i>0 ; i--) { // folder.getMessageCount(); i>0; i-- ) {
                           LMessage msg = updateMsg(messages[i]);
                           if ( msg != null ) {
                                
                                if ( readUpdate ) { while(readUpdate) { sleep(300); } }
                                if (      readAll 
                                     || ( read7days  && msg.receivedInOneWeek()     )
                                     || ( read96hour && msg.receivedInLast96Hours() )
                                     || ( read48hour && msg.receivedInLast48Hours() )
                                     || ( read24hour && msg.receivedInLast24Hours() )   
                                   ) { 
                                    if ( ! msg.isDeleted() ) { 
                                        println(2,func+"add mail:"+msg.getID()+" to mailstore");
                                        ar.add(msg); 
                                    } 
                                } else {
                                     i=-1;
                                }
                           }  
                }

                mailar=ar;
                try {
                    FileOutputStream fout = new FileOutputStream(storefile, true);
                    ObjectOutputStream oos = new ObjectOutputStream(fout);
                    for (int cnt = 0; cnt < ar.size(); cnt++) {
                         oos.writeObject(ar.get(cnt));
                    } 
                } catch(Exception e) {}    
           }     
        }
        scanRun=false;
    }
    public boolean getScan() { return scanRun; }
    
    private boolean imapOnline=true;
    public void    setImapOnline() { imapOnline=true; }
    public void    setImapOffline(){ imapOnline=false; }
    public boolean isImapOnline()  { return imapOnline; }
    public boolean isImapOffline() { return !isImapOnline(); }

    private boolean read24hour=false;
    private boolean read48hour=true;
    private boolean read96hour=false;
    private boolean read7days=false;
    private boolean readAll=false;
    
    volatile private boolean readUpdate;
    public void setImapTimeFrame(String msg) {
        readUpdate=true;
        final String func="Imap::setImapTimeFrame(String msg) - ";
        println(2,func+" msg:"+msg+":");
        read24hour=false;
        read48hour=false;
        read96hour=false;
        read7days =false;
        readAll   =false;
        
        switch(msg) {
            case "24 hours": read24hour=true; break;
            case "48 hours": read48hour=true; break;
            case "96 hours": read96hour=true; break;
            case "7 days"  : read7days =true; break;
            case "All"     : readAll   =true; break;    
            default: read48hour=true;
        }
        readUpdate=false;
    }
    
    public void setImapHost(String host) {  this.host.setHost(host); }
    public void setImapPort(String port) {  this.host.setPort(port); }
    
    static public Imap getInstance(String[] args) throws MessagingException {
         Crypt crypt = new Crypt();
         String u=""; String p=""; String ho=""; String po="";
         if ( args.length > 0 ) {
             for(int i=0; i< args.length; i++ ) {
                if      ( args[i].matches("-d")   ) { Imap.debug++; }
                else if ( args[i].matches("-u")   ) { u=args[++i]; }
                else if ( args[i].matches("-j" )  ) { p=(new ReadFile(args[++i])).readOut().toString(); }
                else if ( args[i].matches("-je")  ) { p=crypt.getUnCrypted((new ReadFile(args[++i])).readOut().toString()); }
                else if ( args[i].matches("-p" )  ) { p=args[++i]; }
                else if ( args[i].matches("-host")) {ho=args[++i]; }
                else if ( args[i].matches("-port")) {po=args[++i]; }
             }
         }
         if ( ! u.isEmpty() && ! p.isEmpty() ) {
             Imap imap = new Imap(u,p);
                  if( ! ho.isEmpty() ) { imap.setImapHost(ho); }
                  if( ! po.isEmpty() ) { imap.setImapPort(po); }
                  imap.init();
                           
            return imap;
         }
         return null;
    }
    
    public static void main(String[] args) throws Exception {
         Imap imap = getInstance(args);
         
         if (  imap != null ) {
                  imap.scan();
         } else {
             System.out.println("missing properties");
             System.exit(-1);
         }
    }
        
}
