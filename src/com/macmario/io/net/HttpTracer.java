/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.io.net;

import com.macmario.io.file.ReadDir;
import com.macmario.io.file.ReadFile;
import java.io.File;
import java.util.Calendar;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


/**
 *
 * @author SuMario
 */
public class HttpTracer {
    final ReadFile file;
    final File odir;
    int debug=0;
    HttpTracer(int pos, String f) {
        file=new ReadFile(f);
        Calendar now = Calendar.getInstance();
        odir=new File("."+File.separator+"out"
                         +now.get(Calendar.YEAR)       +(now.get(Calendar.MONTH)+1) +now.get(Calendar.DAY_OF_MONTH)+"-"
                         +now.get(Calendar.HOUR_OF_DAY)+now.get(Calendar.MINUTE)    +now.get(Calendar.SECOND) );
    }
    
    void store(StringBuilder req, StringBuilder resp, StringBuilder sb,int count) {
        if ( debug > 0) {
            System.out.println("----"+count+"---");
            System.out.println("REQUEST:\n"+req.toString()+"\nREQUEST-END");
            System.out.println("RESPONSE:\n"+resp.toString()+"\nRESPONSE-END");
            System.out.println("BODY:\n"+sb.toString()+"\nBODY-END");
         }
         ReadDir  d=new ReadDir(odir+File.separator);
                  if ( ! d.isExist() ) {  d.mkdirs();}
                  
         ReadFile f=new ReadFile(odir+File.separator+count+".request.txt");   f.save(req); 
                  f=new ReadFile(odir+File.separator+count+".response.txt");  f.save(resp);
                  f=new ReadFile(odir+File.separator+count+".body.html");     f.save(sb);
         
    } 
    void trace() {
         StringBuilder sw = file.readOut();
         Pattern p = Pattern.compile("REQUEST---START|REQUEST---END|CONTENT---BEGIN|CONTENT---END");
         String[] sp=sw.toString().split("\n");
         boolean body=false;    StringBuilder sb  = new StringBuilder(); 
         boolean request=false; StringBuilder req = new StringBuilder(); StringBuilder resp = new StringBuilder();
         int count=0;
         for ( int i=0; i <sp.length; i++) {
                Matcher m = p.matcher(sp[i]);
                if ( m.find() ) {
                    if ( m.group().matches("REQUEST---START")     ) { request=true; count++; sb= new StringBuilder(); req = new StringBuilder();  resp = new StringBuilder(); }
                    else if ( m.group().matches("REQUEST---END")  ) { request=false; store(req,resp,sb,count); }
                    else if ( m.group().matches("CONTENT---BEGIN")) { body=true; }
                    else if ( m.group().matches("CONTENT---END")  ) { body=false;}
                    //String found=sw.substring(m.start(), m.end());
                    //System.out.println("BEGIN:"+m.group()+"\n|"+found+"|\nEND");
                } else {
                   if ( body ) {
                        if ( sb.length() >0 ) { sb.append("\n"); }
                        sb.append(sp[i]);
                   } else {
                        if ( request ) {
                           if ( sp[i].length()>3 ) {
                                if ( req.length() >0 ) { req.append("\n"); }
                                req.append(sp[i]); 
                           } else {
                                request=false;
                           }     
                        } else {
                           if ( resp.length() >0 ) { resp.append("\n"); }
                                resp.append(sp[i]); 
                        }
                   }
                }
         }
    }
    
    public static void main(String[] args) {
        if ( args.length == 0 ) {
             System.out.println("File missing to separate");
             System.exit(-1);
        }
        
        for(int i=0; i<args.length; i++ ) {
            HttpTracer ht = new HttpTracer(i+1,args[i]);
                       ht.trace();
        }
    }
    
}
