/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.macmario.comm.checker;

import com.macmario.general.Version;
import com.macmario.comm.checker.web.Header;
import com.macmario.comm.checker.web.UserAgent;
import com.macmario.comm.checker.web.WebProfiler;
import com.macmario.io.file.HttpXmlFile;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 *
 * @author SuMario
 */
public class WebChecker extends Version{
    private ArrayList<File> ar;
    private String profile;
    private WebChecker(String[] args) {
        ar = new ArrayList<File>(); 
        if ( args.length > 0 ) {
            for(int i=0; i<args.length ; i++ ) {
                 if ( args[i].matches("-d") ) { debug++; } 
                 else if ( args[i].matches("-p") ) {  profile=args[++i];}
                 else {
                    File f = new File(args[i]);
                    if ( f.isFile() && f.canRead() ) {
                        ar.add(f);
                    } else {
                        log(0,"ERROR: could not read from file "+f+" - SKIP");
                    }
                 }   
            }
        }
    }
    
    private UserAgent ua=null; 
    private void test() {
        WebProfiler.debug=debug;
        boolean head=true;
        do {
           boolean foundHTTP=false;
           loadPattern(); 
           File f = ar.remove(0);
           log(0,"INFO: testing file "+f);
           boolean b=true;
           ArrayList<Header> hea = readFile(f);
           if ( hea.size() > 0 ) {
                WebProfiler.validate(hea,profile);
           } else {
               log(0, "ERROR: file "+f+" are not an HTTP Header trace file");
           } 
           
        } while(ar.size() > 0 );
        
    }
    
    public static void main(String[] args) {
        WebChecker wc = new WebChecker(args);
                   wc.test();   
    }
    
    private ArrayList<Header> readFile(File n) {
        HttpXmlFile xml = new HttpXmlFile(n); 
        xml.debug=debug;
        ArrayList<Header> mar;
        if ( xml.isXML() ) { 
            log(2, "File "+n+" are a xml file");
            mar = xml.getXMLObject();
        } else {
            log(2, "File "+n+" are a plain text file");
            mar = xml.getPlainObject();
        }    
        return mar;
    }
    
    private String pattern=null;
    private Pattern pt=null;
    private void loadPattern(){
            if ( pattern != null ) { return; }
            InputStream is = this.getClass().getResourceAsStream("/com/macmario/comm/checker/webpattern.properties");
            
            StringBuilder sw=new StringBuilder();
            String line;
            try {
                BufferedReader rb = new java.io.BufferedReader( new InputStreamReader(is, "UTF-8") );
                do {
                    line=rb.readLine();
                    if ( line != null && ! line.isEmpty() ) {
                        if ( sw.length() >0 ) {sw.append("\n"); }
                        sw.append(line);
                    }
                } while( line !=null );
                
            } catch(Exception e) {
                log(0,"ERROR: pattern read from jar with Exception "+e.toString());
                System.exit(-1);
            }
            pattern=sw.toString().replaceAll("\n","|");
            log(1," pattern has now ==>"+pattern+"<== ");
            
            pt = Pattern.compile(pattern);
            
            UserAgent.debug=debug;
            UrlChecker.debug=debug;
    } 
    
    private int debug=0;
    private void log(final int level, String msg) {
       if ( debug >= level  ) {
           if ( level > 0 ) { msg="DEBUG("+level+"/"+ debug +") =>"+msg; }
           System.out.println(msg);
       } 
    }
}
