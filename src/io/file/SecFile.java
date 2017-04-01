/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.file;

import io.crypt.Base64;
import io.crypt.Crypt;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author SuMario
 */
public class SecFile extends ReadFile {

    private final WriteFile rFile;
    private final Crypt     crypt;
    
    public SecFile(String fn) { this(new File(fn)); }
    public SecFile(File   fn) { 
        super(fn);
        this.rFile = new WriteFile(fn);
        this.crypt = new Crypt();
        this.crypt.setCustomerKey(rFile.getFQDNFileName());
    }
    
    public boolean isCrypted() {
        StringBuilder sw=rFile.readOut();
        return (sw!=null && sw.length()>0 && sw.toString().endsWith("=") );
    }
    
    public boolean crypt() {
        if ( ! rFile.isBinaryFile() )  {
              String s= rFile.readOut().toString();
                     s= crypt.getCrypted(s.replaceAll("==$", "="));
              rFile.replace( s+((s.endsWith("="))?"":"=") );
        } else {
            InputStream in = getInputStream();
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
    
    @Override
    public StringBuilder readOut() {
        StringBuilder sw = new StringBuilder(crypt.getUnCrypted(super.readOut().toString()));
        return sw;
    }
    
    @Override
    public StringBuilder readOut(String begin, String end) {
        StringBuilder sw = new StringBuilder( crypt.getUnCrypted(super.readOut(begin, end).toString()));
        return sw;
    }
    
    @Override
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

    @Override
    public String findInFile(String pat) {
        
        StringBuilder sw=new StringBuilder();
        
        Pattern pa  = Pattern.compile(pat, Pattern.CASE_INSENSITIVE);
        Matcher ma  = pa.matcher("");
        try {
            
         String line;
         BufferedReader rb = new java.io.BufferedReader( new java.io.FileReader(filer) );
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

    @Override
    public StringBuilder getStringBuilderRef ( ) { return readOut(); }

    @Override
    public String getString() { return readOut().toString(); }

    
    public static void main(String[] args) {
        for (String arg : args) {
            SecFile f = new SecFile(arg);
            System.out.println("OUT:"+f.readOut().toString()+":");
        }
    }
}
