/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.comm.checker;

import java.io.File;
import java.util.regex.Pattern;

/**
 *
 * @author SuMario
 */
class PatternTest implements Runnable {
    
        final private String myPattern;
        private StringBuilder sw;
        private boolean closed=false;
        private boolean av=false;
        private PatternTest pt=null;
        private Pattern r;
        private int debug=-1;
        
        Thread th;
        PatternTest(String pattern,int debug){
            this.myPattern=pattern;
            
            r = Pattern.compile(pattern);
            this.debug=debug;
        }
        
        public void setPatternTest(PatternTest pt){ this.pt=pt; }
        
        public String getName(){return this.myPattern; }
        
        public synchronized void setTest(StringBuilder sw, File f, CheckTimer ckt) {
            log(2,"thread "+myPattern+" wait for clean progress - (sleep when true) have:"+av);
            while( av ) { sleep(500); }
            av=true; 
            log(2,"thread "+myPattern+" wait completed ");
            synchronized ( myPattern ) {
                this.sw=sw; lfile=f; this.ckt=ckt;
                log(2,"thread "+myPattern+" receive new test");
            }
        }
        
        private CheckTimer ckt;
        private File lfile;
        @Override
        public void run() {
            log(2,"PatternThread :"+myPattern+": started");
            while ( ! closed ) {
                if (  ! av ) { log(2,"PatternThread "+myPattern+" wait for new test"); }
                while( ! av ) { sleep(500); }
                log(2,"PatternThread "+myPattern+" start testing "); 
                    // pattern test code here
                 String[] sp = sw.toString().split("\n");
                 for (int i=0; i< sp.length; i++ ) {
                     if ( ckt.check(sp[i]) ) { 
                        if ( valid(sp[i])) {                         
                            System.out.println(lfile.toString()+"|"+sp[i]+"|"); 
                        }
                     }   
                 }
                 log(2, "PatternThread "+myPattern+" testing stopped");
                 if ( av ) {
                    if ( pt != null ) { 
                        log(2,"PatternThread "+myPattern+" set new Tester on :"+pt.getName());
                        pt.setTest(sw,lfile,ckt); 

                    } else {
                        log(4,"PatternThread "+myPattern+" do not have a parent Tester");    
                    }
                    av=false;
                 } else { sleep(500); }
                     
            }
            log(2,"PatternThread :"+myPattern+" finished");
        }
        
        
        private boolean valid(String s) {    // regular expression return boolean from Macher.find()  
               return (r.matcher(s)).find();
        }
         
        private void sleep(long wait) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException ex) {
                log(5, "PatternThread :"+myPattern+"  wakeuped");
            }
        }
    
    private void log(final int level, final String msg) {
       if ( debug >= level  ) 
        System.out.println(msg);
    }

   
}

