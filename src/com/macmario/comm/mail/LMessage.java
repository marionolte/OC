/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.comm.mail;

import com.macmario.general.Version;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePartDataSource;

/**
 *
 * @author SuMario
 */
public class LMessage extends Version implements Serializable{
    final String mid;
    final Message msg;
    private String subject="";
    private String from="";
    private String to="";
    private String cc="";
    private String bcc="";
    private String ret="";  // return addr
    private String mflag="";
    boolean mdel=false;
    boolean mans=false;
    boolean mdelex=false;
    private Date revDate;
    private Date sendData;
    LMessage(Message msg) {
        this.mid=""+msg.getMessageNumber(); 
        this.msg=msg;
        updateMsg();
    }

    public String getID() { return mid; }
 
    public void updateMsg() {
       try { 
        if ( to.isEmpty() || subject.isEmpty() ) {   
            this.subject=msg.getSubject();
            StringBuilder sw = new StringBuilder();
            for ( Address f : msg.getFrom() ) {
                if (sw.length() > 0L ) { sw.append(";"); }
                sw.append(f.toString());
            }
            this.from=sw.toString();
            sw.replace(0, sw.length(), "");
            Address[] addr = msg.getRecipients(Message.RecipientType.TO);
            if (addr != null)
                for ( Address f : addr ) {
                      if (sw.length() > 0L ) { sw.append(";"); }
                      sw.append(f.toString());
                }
            this.to=sw.toString();
            
            sw.replace(0, sw.length(), "");
            addr = msg.getRecipients(Message.RecipientType.CC);
            if (addr != null)
                for ( Address f : addr ) {
                      if (sw.length() > 0L ) { sw.append(";"); }
                      sw.append(f.toString());
                }
            this.cc=sw.toString();
            
            sw.replace(0, sw.length(), "");
            addr =  msg.getRecipients(Message.RecipientType.BCC);
            if (addr != null)
                for ( Address f : addr) {
                      if (sw.length() > 0L ) { sw.append(";"); }
                      sw.append(f.toString());
                }
            this.bcc=sw.toString();
            
            this.revDate=msg.getReceivedDate();
            this.sendData=msg.getSentDate();
        }
        mflag=( msg.isSet(Flags.Flag.SEEN) )?"SEEN":"NEW";
        mdel=msg.isSet(Flags.Flag.DELETED);
        mans=msg.isSet(Flags.Flag.ANSWERED);
        mdelex=msg.isExpunged();
       } catch(javax.mail.MessagingException m) {
       
       } 
    }
    
    public String getSubjectIds() { return findIds(this.getSubject()); }
    public String getAllIds(    ) { return findIds(this.getSubject()+this.getBody()); } 
    
    private String findIds(String a){
        StringBuilder sw = new StringBuilder();
            Pattern pa = Pattern.compile("3-[1-9][0-9]*|[1-9][0-9]*[0-9]\\.1|[1-9][0-9]*[0-9]");
            Matcher ma = pa.matcher(a);
        
        int start=0;
        while( ma.find(start) ) {
            String s=a.substring(ma.start(), ma.end());
            if ( s.length() > 6 ) {
                if ( sw.length() >0 ) { sw.append(","); }
                sw.append(s);
            }
            start=ma.end();
        }
        return sw.toString();
    }
    
    private boolean isHtml=false;
    
    public boolean isDeleted(     ) { return mdel;   }
    public boolean isExpurged(    ) { return mdelex; }
    public boolean isNew(         ) { return (mflag.matches("NEW")  && ! mdel && ! mdelex ); }
    public boolean isRead(        ) { return (mflag.matches("SEEN") && ! mdel && ! mdelex ); }
    public boolean isAnswered(    ) { return (mans                  && ! mdel && ! mdelex ); }
    
    public Date    getReceiveDate() { return this.revDate;  }
    public Date    getSendDate(   ) { return this.sendData; }
    public String  getFrom(       ) { return this.from; }
    public String  getReplayTo(   ) { return this.ret;  }
    public String  getSubject(    ) { return this.subject; }
    public boolean isBodyHtml(    ) { if (bodyTxt == null ){ getBody();} return this.isHtml; }  
    
    private String bodyTxt=null;
    public String  getBody(       ) { 
        final String func="LMessage::getBody() - ";
        StringBuilder sw = new StringBuilder();
        if (bodyTxt != null ) { return bodyTxt; }
        try {
            Object content = msg.getContent();
            String contentReturn = "please contact developer to verify message mime type of ";            

            if (msg.isMimeType("text/*")) {
                isHtml =(msg.isMimeType("text/html")); 
                contentReturn=(String) content;
                println(2, func+"plain message - take context =>"+contentReturn);
            } else {
                println(2, func+"not plain text message:"+msg);
            }
            
            if        (content instanceof String)  {
                contentReturn = (String) content;
                println(2, func+"plain message - take context =>"+contentReturn);                    
            
            } else if (content instanceof Multipart) {
                Multipart mpart = (Multipart) content;
                int j = mpart.getCount();
                println(3, func+"getMultiPart count:"+j);
                if ( j > 0 ) {
                    println(3, func+"getMultiPart from  "+j+" elements");
                    for ( int i = 0 ; i< mpart.getCount(); i++ ) {
                            BodyPart part = mpart.getBodyPart(i);
                            if (part.isMimeType("text/*")) {
                                println(3,func+"contentType msgpart["+i+"]="+part.getContentType());
                                isHtml = (part.isMimeType("text/html"));
                                contentReturn = part.getContent().toString();
                            }   
                    }
                } else {
                    println(2, func+"getMultiPart only one =>|"+mpart.getBodyPart(0).toString()+"|<=");
                    //contentReturn = mpart.getBodyPart(0).toString();
                    
                    contentReturn = content.toString();
                }
            } else if (content instanceof InputStream) {
                    println(2, func+"get an InputStream message");
                    InputStream is = (InputStream)content;
                    StringBuilder mp = new StringBuilder();
                        // Assumes character content (not binary images)
                        int c;
                        while ((c = is.read()) != -1) {
                            mp.append((char)c);
                        }
                        
                    contentReturn = mp.toString();
            } else {
                contentReturn = contentReturn +" "+content;
                println(1, func+"unkown content instance "+content);
            } 
            println(3,func+"isHTML:"+isHtml);
            if (! isHtml ) { isHtml=(contentReturn.toLowerCase().contains("<html>")); }
            println(2,func+"after final check isHTML:"+isHtml);
            sw.append(contentReturn);
            bodyTxt=sw.toString();
        } 
        catch (IOException io         ) { sw.append("io error "+io.getMessage()+" - not loaded"); } 
        catch (MessagingException mio ) { sw.append("message error "+mio.getMessage()+" - not loaded"); }
        return sw.toString(); //(( isHtml )?io.Html2Text.extractText(sw.toString()): sw.toString()) ;
    }
    public boolean isRelayEqualFrom() { return (ret.isEmpty() || from.toLowerCase().equals(ret.toLowerCase())); }
    public String  getReceivers() {
           StringBuilder sw = new StringBuilder(this.to);
           if ( ! this.cc.isEmpty() ) {  sw.append("\tcc:"+this.cc); }
           if ( ! this.bcc.isEmpty()) {  sw.append("\tbcc:"+this.bcc); }
           return sw.toString();
    }
    public String getTo() { return this.to;  }
    public String getCC() { return this.cc;  }
    public String getBCC(){ return this.bcc; }
    
    
    public boolean receivedInLast24Hours() { return received(24);  }
    public boolean receivedInLast48Hours() { return received(48);  }
    public boolean receivedInLast96Hours() { return received(96);  }
    public boolean receivedInOneWeek()     { return received(7*24);}
    public boolean receivedToday() {
      
       Calendar calendar = GregorianCalendar.getInstance(); 
                calendar.setTime(new Date());  
            return received(calendar.get(Calendar.HOUR_OF_DAY));
        
    }
    
    synchronized private boolean received(int hours) {
        boolean b=false;
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, - hours);
        Date dday = cal.getTime();
        return (this.revDate != null )?this.revDate.after(dday):false;
    }
    
    private String sepa="|_@_|";
    public void   setSeparator(String sepa) {this.sepa=sepa; }
    public String getSeparator(           ) { return this.sepa; }
    
    @Override
    public String toString() {
       StringBuilder sw=new StringBuilder(); 
       sw.append( this.isNew()?"SEEN":"NEW" ).append(sepa);
       sw.append(to).append(sepa).append(cc).append(sepa).append(bcc).append(sepa).append(from).append(sepa).append(ret).append(sepa);
       sw.append(getSubject()).append(sepa).append(this.revDate.toString()).append(sepa).append(this.sendData.toString());
       
       return sw.toString();
    }
    
}
