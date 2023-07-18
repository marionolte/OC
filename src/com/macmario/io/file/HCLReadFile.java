/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.io.file;

import com.macmario.io.buffer.MapList;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    
    
    MapList map = null;
    
    private String sepa="{,},\\,,";
    private String parse(StringBuilder src) {
        System.out.println("like to parse ->|"+src.toString()+"|<-\n");
        Pattern par = Pattern.compile("\\{|\\}|\\,");
        // "\\{\"|\"\\}\\],\"|\":\\{\"|\":\\[\\{\"|\":\"|\",\"|\":|,\"");
        
        //int start=0;
         String en="";
        MapList ml = null;
         
        for ( String s : src.toString().split("\n") ) {
            s=s.trim();
            if ( s.endsWith("\\")) {
                en=s.substring(0, s.length()-2);
            } else {
                if ( ! s.startsWith("#")) {
                    //System.out.println("Line ->|"+s+"|<-");
                    s=en+s; en="";
                    Matcher ma = par.matcher(s); int start=0;
                    if ( ma.find(start)) {
                        while ( ma.find(start) ) {
                            String grp = ma.group();
                            String f   = s.substring(start, ma.end());
                            //System.out.println("found =>|"+ma.group()+"| from "+start+" to "+ma.start()+"  // end "+ma.end()
                            //                   +" ->|"+f+"|<-");

                            
                            if ( ma.group().equals("{") ) {
                                if ( ml == null ) { ml = new MapList(); 
                                                    if ( map == null ) { map=ml;}
                                } else { ml = ml.getNewList(ml); }
                                ml.setName( getFirstValue(f,"=").replaceAll(" ", "").replaceAll("\\"+grp, "") );
                            } else if ( ma.group().equals("}") ) {
                             //   System.out.println("find "+grp+" in:"+f);
                                ml = ml.getMaster();
                            } else if ( ma.group().equals(",") ) {
                                String[] sp = f.split("=");
                                sp[0] = sp[0].replaceAll(" ","");
                                if ( f.contains("\"") ) {
                                    ml.setValue(sp[0], f.substring(sp[0].length()+3, f.length()-2));
                                    ml.setValueType(sp[0], "val");
                                } else {
                                    System.out.println("hier :"+sp[0]+":  = :"+f.substring(sp[0].length()+1, f.length()-1));
                                    ml.setValue(sp[0], f.substring(sp[0].length()+1, f.length()-1));
                                    ml.setValueType(sp[0], "func");
                                }   
                            } else {
                               // System.out.println("no matched :"+f);
                            }

                            start=ma.end();
                        }
                    } else {
                        String[] sp = s.split("=");
                        //System.out.println("hier1 :"+sp[0]+":  = :"+s.substring(sp[0].length()+2, s.length()-1)+":");
                                    
                        if ( s.contains("\"") ) {
                            ml.setValue(sp[0].replaceAll(" ",""), s.substring(sp[0].length()+3, s.length()-1));
                            ml.setValueType(sp[0], "val");
                        } else {
                            ml.setValue(sp[0].replaceAll(" ",""), s.substring(sp[0].length()+2, s.length()-1));
                            ml.setValueType(sp[0], "func");
                        }   
                    }    
                } else {
                    //System.out.println("skip line |"+s+"|");
                    ml.setComment(s);
                }   
            }
        }
        
        //JSONObject obj = new JSONObject("{"+src.toString()+"}");
        
        System.out.println("end parse");
        return "";
    }
    
    private String getFirstValue(String txt, String lim) {
        if ( txt == null || txt.isEmpty() || lim == null ) {  return ""; }
        String[] sp = txt.split(lim);
        return sp[0];
    }
    
    @Override
    public String toString() {
        
        StringBuilder sw = new StringBuilder();
        if ( map == null || map.isEmpty() ) { sw.append("empty"); }
        else {
            sw.append(map.getInfo(""));
        }
    
        return sw.toString();
    }
    
    public static void main(String[] args)  throws Exception {
         HCLReadFile hf = new HCLReadFile(args[0]) ;
                     hf.parse();
         System.out.println("out ->|\n"+hf.toString()+"\n|-");
    }
}
