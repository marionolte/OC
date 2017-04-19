/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.file;

import io.crypt.Base64;
import io.crypt.Crypt;
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
public class SecFile {//extends ReadFile {

    private final WriteFile rFile;
    private final Crypt     crypt;
    
    public SecFile(String fn,int level) { this(new File(fn),level); }
    public SecFile(String fn) { this(new File(fn),1); }
    public SecFile(File   fn,int level) { 
        //super(fn);
        this.rFile = new WriteFile(fn);
        this.crypt = new Crypt();
        this.crypt.setCustomKey(rFile.getFQDNFileName());
        setCryptLevel(level);
        
        if ( ! isCrypted() ) { crypt(); }
    }
    
    public void setCryptLevel(int level) {
        this.crypt.setCryptLevel( (level>0)?level:0 );
    }
    
    public void setHostKey(  String key) { this.crypt.setHostKey(key); }
    public void setUserKey(  String key) { this.crypt.setUserKey(key); }
    public void setCustomKey(String key) { this.crypt.setCustomKey(key); }
    
    
    public boolean delete() { return rFile.delete(); }
    public boolean append(String line) { return rFile.append( crypt.getCrypted(line) ); }
    
    public boolean isCrypted() {
        StringBuilder sw=rFile.readOut();
        return (sw!=null && sw.length()>0 && sw.toString().endsWith("=") );
    }
    
    private boolean crypt() {
        if ( ! rFile.isBinaryFile() )  {
              String s= rFile.readOut().toString();
                     s= crypt.getCrypted(s.replaceAll("==$", "="));
              rFile.replace( s+((s.endsWith("="))?"":"=") );
        } else {
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
                    
            }  
            sw.append( ((sw.substring(sw.capacity()-1).matches("="))?"":"=") );
            rFile.replace(sw.toString());
            
        }
        
        return isCrypted();
    }
    
    public boolean uncrypt() {
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
              } catch(java.io.IOException io ) {}  
            }
        }
        return isCrypted();
    }
    
    public StringBuilder readOut() {
        StringBuilder sw = new StringBuilder(crypt.getUnCrypted(rFile.readOut().toString()));
        return sw;
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


}
