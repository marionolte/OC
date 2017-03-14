/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.file;

import general.Version;
import io.crypt.Base64;
import io.crypt.Crypt;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

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
            int i = -1;
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
}
