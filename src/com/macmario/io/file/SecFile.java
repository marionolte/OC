/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.io.file;

import com.macmario.io.crypt.Base64;
import com.macmario.io.crypt.Crypt;
import com.macmario.io.thread.RunnableT;
import java.io.ByteArrayInputStream;
import java.util.Properties;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author SuMario
 */
public class SecFile extends ReadFile {

    private final WriteFile rFile;
    private final Crypt     crypt;
    
    public SecFile(String fn,int level) { this(new File(fn),level); }
    public SecFile(String fn) { this(new File(fn),1); }
    public SecFile(ReadFile fn) { this(fn.filer,1); }
    public SecFile(File   fn,int level) { 
        super(fn);
        final String func=getFunc("SecFile");
        this.rFile = new WriteFile(fn);
        printf(func,2,"create crypt ");
        
        this.crypt = new Crypt();
        printf(func,2,"add custom key");
        this.crypt.setCustomKey(rFile.getFQDNFileName());
        printf(func,2,"set crypt level secFile");
        setCryptLevel(1);
        printf(func,2,"test secFile");
        (new SecTest()).start(); //check isCrypted()
        printf(func,2,"test secFile started -  done");
        //System.out.println("secfile done");
        setCryptLevel(level);
        
        if ( isReadableFile() && ! isCrypted() ) { crypt(); }

    }
    
    final public void setCryptLevel(int level) {
        this.crypt.setCryptLevel( (level>0)?level:0 );
    }
    
    public void setHostKey(  String key) { this.crypt.setHostKey(key); }
    public void setUserKey(  String key) { this.crypt.setUserKey(key); }
    public void setCustomKey(String key) { this.crypt.setCustomKey(key); }
    
    
    public boolean delete() { return rFile.delete(); }
    public boolean  append(String line) { String f=crypt.getCrypted(line); return rFile.append( f+((f.endsWith("="))?"":"=")  ); }
    public boolean replace(String line) { rFile.truncate(); return append(line); }
    
    public final synchronized boolean isCrypted() {
        final String func=getFunc("isCrypted()");
        if ( rFile.isReadableFile() ) {
            StringBuilder sw=rFile.readOut();
            if ( sw == null ) { sw=new StringBuilder(); }
            boolean b=(sw.length()>0 && sw.toString().endsWith("=") && crypt.isCrypted(sw.toString()));
            printf(func,2,"readout =>|"+sw.toString()+"|<=  crypted:"+b);
            return b;
        }
        return false;
    }
    
    synchronized public boolean crypt() {
        final String func=getFunc("crypt()");
        
        boolean as = rFile.isBinaryFile() ;
        printf(func,2,"file "+rFile.getFQDNFileName()+" is a binary "+( (as)?"YES":"NO") );
        if ( ! as )  {
              String s= rFile.readOut().toString();
              printf(func,2,"crypt ascii =>|"+s+"|<=");
              if ( ! crypt.isCrypted(s) ) {
                     s= crypt.getCrypted(s.replaceAll("==$", "="));
                     printf(func,2,"crypt ascii have (1) =>|"+s+"|<=");
              }
              printf(func,2,"crypt ascii have (2) =>|"+s+"|<=");
              s=s+((s.endsWith("="))?"":"=");
              printf(func,2,"crypt ascii replace with =>|"+s+"|<=");
              rFile.replace( s );
        } else {
            printf(func,3,"is a Binary");
            InputStream in = rFile.getInputStream();
            StringBuilder sw = new StringBuilder("<BINARY>\n");
            
            byte[] b = new byte[1000];
            int i;
            try { 
                
                while( in.available() > 0 ) {
                
                    i=in.read(b);
                    byte[] bi = new byte[i]; for(int j=0; j<i; i++ ) { bi[j]=b[j]; }
            
                    sw.append( crypt.getCrypted(new String( Base64.encode( bi ) ) )).append("\n");
                  
                }
            } catch(java.io.IOException io ) {
              printf(func,1,"ERROR - crypt exception "+io.getMessage(),io);      
            }  
            printf(func,2,"crypted:"+sw.toString()+":  length:"+sw.length()+":"+sw.capacity());
            sw.append( ((sw.substring(sw.length()-1).matches("="))?"":"=") );
            rFile.replace(sw.toString());
            
        }
        
        return isCrypted();
    }
    
    public boolean uncrypt() {
        final String func=getFunc("uncrypt()");
        
        if ( isCrypted() ) {
            StringBuilder sw = readOut();
            if ( ! sw.substring(0, ("<BINARY>\n").length()).matches(("<BINARY>\n") ) ) {
                rFile.replace( crypt.getUnCrypted(sw.toString()));
            } else {
              try {  
                OutputStream out = rFile.getOutStream();
                int i=0; int j=sw.indexOf("\n", i);
                while ( j > 0  ) {
                    String s= sw.substring(i, j).trim();
                    if ( ! s.matches("=") && ! s.matches("<BINARY>") ) {
                        out.write( crypt.getUnCryptedByte(s) );
                    } 
                    i=j;
                    j=sw.indexOf("\n", i);
                }
                out.flush();
                out.close();
              } catch(java.io.IOException io ) {
                 printf(func,1,"ERROR - uncrypt exception - "+io.getMessage(),io); 
              }  
            }
        }
        return isCrypted();
    }
    
    @Override
    public StringBuilder readOut() {

        try {
            StringBuilder sw = new StringBuilder(crypt.getUnCrypted(super.readOut().toString()));
            return sw;
        } catch ( Exception e ) {
            return new StringBuilder("");
        }
        
    }
    @Override
    public Properties getProperties() {
       Properties p = new Properties();
       try { 
            InputStream ims = new ByteArrayInputStream( this.readOut().toString().getBytes("UTF-8") );
            p.load( ims ); 
       } 
       catch( java.io.IOException | NullPointerException ne){}
       
       return p;
    }
    
    public StringBuilder readOut(String begin, String end) {
        StringBuilder sw = new StringBuilder( crypt.getUnCrypted(rFile.readOut(begin, end).toString()));
        return sw;
    }
    
    public Pattern readPattern() {
       StringBuilder sw=readOut();
       StringBuilder sb=new StringBuilder();
       for ( String line : readOut().toString().split("\n") ) {
                if ( line != null && ! line.isEmpty() ) {
                   if ( sb.length() > 0 ) {
                       sb.append("|");
                   } 
                   sb.append(line);
                }
       }
       return Pattern.compile(sb.toString());
    }

    public String findInFile(String pat) {
        
        StringBuilder sw=new StringBuilder();
        
        Pattern pa  = Pattern.compile(pat, Pattern.CASE_INSENSITIVE);
        Matcher ma  = pa.matcher("");
        try {
         String line;
         //BufferedReader rb = new java.io.BufferedReader( new java.io.FileReader(filer) );
         java.io.BufferedReader rb = new java.io.BufferedReader( new StringReader( readOut().toString() ) );
         do {
            line=rb.readLine();
            if ( line != null && ! line.isEmpty() ) {
                   ma.reset(line); //reset the input
                   if (ma.find()) {
                        sw.append(line.trim()).append("\n");
                   }                    
            }
         } while ( line != null );
        } catch (Exception e){ }   
        
        return sw.toString();
    }
    
    public boolean isReadableFile()  { return rFile.isReadableFile();  }
    public boolean isWriteableFile() { return rFile.isWriteableFile(); }
    public boolean isExecutableFile(){ return rFile.isExecutableFile(); }

    public String getFQDNFileName()  { return rFile.getFQDNFileName(); }
    public String getFileName()      { return rFile.getFileName(); }
    
    
    public static void main(String[] args) {
        int level=1;
        ArrayList<String> ar = new ArrayList();
        for (String arg : args) {
            ReadFile f = new ReadFile(arg);
            if ( f.isReadableFile() ) {
                 ar.add(f.getFQDNFileName());
            } else {
                try {
                    level = Integer.parseInt(arg);
                }catch(Exception e) {
                    System.out.println("ERROR: "+arg+" is not a file or int");
                    System.exit(-1);
                }
            }
        }
        
        for(String arg : ar) {
            SecFile f = new SecFile(arg,level);
            System.out.println(f.readOut().toString());
        }    
    }

    
    private class SecTest extends RunnableT{
        

        @Override
        public void run() {
            setRunning();
            if ( isReadableFile() && ! isCrypted() ) {
                 crypt();
            }
            setClosed();
        }
         
        
    }

}
