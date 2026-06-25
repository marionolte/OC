/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.comm.mail;

import java.util.regex.Pattern;

/**
 *
 * @author SuMario
 */
public class MailHead {
    final private String sepa;
    public String id;
    public boolean newflag;
    public String from="";
    public String to="";
    public String subject="empty subject";
    public String receive=""; 
    public MailHead(LMessage list) {
        StringBuilder sw = new StringBuilder();
        for ( int i=0; i<Imap.sepa.length() ; i++ ) {
            char c = Imap.sepa.charAt(i);
            if ( c== '|' || c == '@' ) { sw.append("\\"); }
            sw.append(c);
        }
        this.sepa=sw.toString();
        //System.out.println("sepa =>"+sepa+"<= LIST =>|"+list+"|<=");
        
        this.id     = list.getID();
        this.newflag= list.isNew();
        this.from   = list.getFrom();
        this.to     = list.getTo();
        this.subject= list.getSubject();
        this.receive= list.getSendDate().toString();
        
        
        //System.out.println("=>|"+toString()+"|<=");
    }

    public boolean inSubject(String sr) {
        Pattern pa = Pattern.compile(sr);
        return pa.matcher(this.subject).find();
    }
    public boolean isOwner(String u) {
        return ( u != null && ! u.isEmpty() && ! from.isEmpty() && from.toLowerCase().contains(u.toLowerCase()) );
    }
    
    public boolean isNewMail() { return this.newflag;  }
 
    
    @Override
    public String toString() {
        StringBuilder sw=new StringBuilder();        
        sw.append( ((isNewMail() )?"New ":"    ")+id ).append(" "+to).append("\t").append( subject );
        return sw.toString();
    }
    
}
