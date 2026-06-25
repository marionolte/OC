/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.io.java;

import com.macmario.io.file.LargeReadFile;
import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author SuMario
 */
public class GCFile extends LargeReadFile{
    public GCFile(String dir, String file) { super(dir,file); }
    public GCFile(            String file) { super(file); }
    public GCFile(            File   file) { super(file); }

    public void addLine(String s) {
        
    }
    
    
    public void check() {
        final String func=getFunc("check()");
        printf(func,3,"next:"+this.hasNext()+":"); //3
        while( this.hasNext() ) {
              printf(func,3,"read next::"); //3
          
              StringBuilder sw = this.read();
              getBlocks(sw);
              printf(func,3,"read:"+sw.toString()+":"); //3
        } 
        close();
        printf(func,4,"check completed"); //4
    }
    
    private void getBlocks(StringBuilder sw) {
        final String func=getFunc("getBlocks(StringBuilder sw)");  
        Pattern pa = Pattern.compile("˜\\{|\\}");
        Matcher ma = pa.matcher(sw.toString());
        int pos=0; int i=-1; ArrayList<GCBlock> ar = new ArrayList<GCBlock>();
        while ( ma.find(pos) ) {
            String s = sw.substring(++pos, ma.start()).replace("{", "");
            printf(func,3,"sp["+(++i)+"]"+s   );
            ar.add( new GCBlock(s) );
            pos=ma.end(); 
        }
        
    }
    
    public static void main(String[] args) {
          int d=0;
          for (String f : args ) {
              
              if ( f.matches("-d") ) { d++; }
              else {
                  System.out.println("check:"+f);  
                GCFile gc = new GCFile(f);
                       gc.debug=d;
                       gc.check();
              }       
          }
          System.out.println("done.");
    }
}
