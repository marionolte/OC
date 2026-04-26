/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.io;

import com.macmario.io.thread.RunnableT;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import main.Mos;


/**
 *
 * @author SuMario
 */
public class Console extends RunnableT {
    private volatile boolean keepOn = true;
    private volatile ArrayList<String> map = new ArrayList<String>();
    private BufferedReader reader;
    private Thread term=null;
    private boolean silient;
    private Mos mos;
    public Console( Mos mos ) {
        this();
        this.mos=mos;
        this.silient=mos.silent;
        this.lock="Console";
    }
    
    public Console() {
        // System.console();
        InputStreamReader isr = new InputStreamReader(System.in);
        reader = new BufferedReader(isr);
        this.mos=null;
        this.lock="Console";
    }
    String prompt = "> ";
    String urlString;

    public void newLine(){
        System.out.print("\n"+prompt);
    }
    public void update(BufferedReader read) {
        this.reader=read;
        
    }
    
    public boolean isStopped() { return ! keepOn; }
    
    public String getLine(){
        if ( ! isRunning() ) { start(); }
        while( map.isEmpty() ) { sleep(300); }
        return map.remove(0);
    }
    
    
    public String getConsolePassword(){
            return new String( System.console().readPassword() );
    }
    
    @Override
    public void run() {
        keepOn = true;
      try {  
        term = new RunWhenShuttingDown();
        Runtime.getRuntime().addShutdownHook( term );
        do {
          try {  
            urlString = reader.readLine(); 
            if ( urlString !=null ) { 
                if ( urlString.matches("q")) { if (mos!=null) mos.stopProgress=true;  setClosed(); } 
                else {
                    map.add(urlString.replaceAll("\t", " "));
                    if( urlString.matches("quit") || urlString.matches("close") ) { setClosed(); }
                }    
            }
          } catch(IOException io ) {}
        } while( ! isClosed() );
      } catch (Exception e) {
         if (mos!=null && ! mos.isClosed() ) { 
               mos.stopProgress=true;
               mos.setClosed();
         }
      }  
      //System.out.println("INFO: console closed");
    }
    
    public class RunWhenShuttingDown extends Thread { 
            @Override
            public void run() { 
                if ( keepOn ) {
                    System.out.print("INFO: receive shutting down control .. "); 
                    keepOn = false; 
                    if ( mos != null &&! mos.isClosed() ) { mos.setClosed(); }
                    System.out.println("done");
                }    
            } 
            
    } 
}
