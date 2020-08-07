/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.file;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;

/**
 *
 * @author SuMario
 */
public class HCLReadFile extends ReadFile {
    
    public HCLReadFile(String dir, String file) {
        super(dir, file);
    }
    
    public HCLReadFile(String nfile) { this(new File(nfile.replaceAll("^~", System.getProperty("user.home")+File.separator)) ) ; }
    
    public HCLReadFile(File nfile) {
        super(nfile);
    }
    
    
    private long _parsed =0L;
    
    private String parse() { 
        long d = System.currentTimeMillis();
        boolean f = false;
        String s="";
        
        try {
          s=parse(super.readOut()); 
        } catch( Exception e) {
            f=true;
        }
        finally {
            _parsed=(f)?_parsed:d;
            return s;
        }
    }
    
    private String sepa="{,},\\,,";
    private String parse(StringBuilder src) {
        System.out.println("parse");
        Pattern par = Pattern.compile(sepa);
        Matcher ma = par.matcher(src.toString());
        
        System.out.println("parse:"+src.toString()+"|<-\n");
        
        int start=0;
        while ( ma.find(start) ) {
            String grp = ma.group();
            System.out.println("found =>|"+ma.group()+"| from "+start+" to "+ma.start()+"  // end "+ma.end());
            
            start=ma.end();
        }
        
        
        //JSONObject obj = new JSONObject("{"+src.toString()+"}");
        
        System.out.println("end parse");
        return "";
    }
    
    
    public static void main(String[] args)  throws Exception {
        
         System.out.println("read:"+(new HCLReadFile(args[0]) ).parse() );
    }
}
