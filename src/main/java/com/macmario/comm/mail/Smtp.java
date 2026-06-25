/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.comm.mail;

import com.macmario.general.Version;
import com.macmario.io.account.User;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import com.macmario.net.tcp.Host;

/**
 *
 * @author SuMario
 */
public class Smtp extends Version {
     private User       user;
     private String     userAlias;
     private Properties props;
     private Session    session=null;
     private final Host host;
    
    public Smtp(User user, Host ho) {
        this.user=user;
        this.host=ho;
        init();
    }
    public Smtp(User u) {
        this(u, new Host("localhost"){{ int port=465; } }); 
        
    }      
    public Smtp(String u, String p) {
        this(new User(u,p) {});
    }
    
    private void init() {
        this.props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", host.getHost() );
        props.put("mail.smtp.port", host.getPort("smtp") );
        props.put("mail.smtp.ssl.enable", "true");
        //props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.protocols", "TLSv1"); 
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", "false");
        
        String[] sp = user.getUsername().split("@");  sp = sp[0].split(".");
        StringBuilder sw = new StringBuilder();
        for ( int i=0; i<sp.length; i++) {
            if (sw.length()>0 ) { sw.append(" "); }
            sw.append( sp[i].toLowerCase().replaceAll("^[a-z]", "[A-Z]"));
        }
        this.userAlias=sw.toString();
        
        session = Session.getDefaultInstance(props, 
                
                        new javax.mail.Authenticator() {
                             @Override
                             protected PasswordAuthentication getPasswordAuthentication() {
                                        return new PasswordAuthentication(user.getUsername(), user.getPassword() ); 
                             }
                        }
                );
    }
    
    
    public synchronized boolean send(String to, String toAlias, String subject, String msgBody){
        init();
        if ( to == null || to.isEmpty() ) { to=user.getUsername(); toAlias=userAlias; }
        if (subject == null) { subject="INFO: DELETE Testmessage - account has been activated"; }
        Message msg = new MimeMessage(session);
         try {
            msg.setFrom(new InternetAddress(user.getUsername(), userAlias));
            msg.addRecipient(Message.RecipientType.TO, new InternetAddress(to, toAlias));
            msg.setSubject(subject);
            msg.setText(msgBody);
            
            Transport.send(msg);
            
            return true;
         } catch(Exception e) {
             System.out.println("sending with error - "+e.getMessage()); 
             e.printStackTrace();
            return false;
         }   
    }
    
}
