/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.wls;

import general.Version;
import static general.Version.printf;
import io.file.ReadDir;
import io.file.WriteFile;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 * @author SuMario
 */
public class WlsDecrypt extends Version {

    private  WlsDomain wls=null;

    private WlsDecrypt(WlsDomain wls) {
        this.wls=wls;
    }
    
    private void decrypt() throws IOException {
        
        if ( wls != null ) {
             String script = getOutString( new BufferedInputStream( WlsDecrypt.class.getResourceAsStream("/net/wls/scripts/decrypthash.py") ) )
                                 .replace("@@USER@@", wls.getAdminUser())
                                 .replace("@@PASS@@", wls.getAdminPassword())
                                 .replace("@@NMUS@@", wls._nodeMUser)
                                 .replace("@@NMPA@@", wls._nodeMPass); 
             
            WriteFile wt = new WriteFile(wls.getDomainLocation()+File.separator+"info.py"); wt.replace(script);
            Process p = null;
            ProcessBuilder pb = new ProcessBuilder(". ./bin/setDomainEnv.sh && java weblogic.WLST info.py");
            pb.directory(new File (wls.getDomainLocation() ) );
            p = pb.start(); 
            
            
            InputStreamReader isr = new InputStreamReader(p.getInputStream());
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        }
    }    
    
    public static void main(String[] args) throws Exception {
        for ( int i=0; i< args.length; i++){
             ReadDir d = new ReadDir(args[i]);
             
             if ( d.isDirectory() ) {
                  WlsDomain wls = new WlsDomain(d.getDirName());
                            wls.setDomainLocation(d.getFQDNDirName());
                            
                  WlsDecrypt wd = new WlsDecrypt(wls);
                             wd.decrypt();
             }
             
        }
    }

    
    synchronized String getOutString(BufferedInputStream in) {
        final String func=getFunc("getOutString(BufferedInputStream in)");
        StringBuilder st = new StringBuilder();
        try {
            int c;  StringBuilder sw= new StringBuilder();
            while( (c=in.available()) >0 ) {
                byte[] b = new byte[c];
                c=in.read(b);
                if (sw.length() >0 ) { sw.delete(0, sw.capacity()); }
                for(int i=0; i<c; i++ ) {  sw.append( (char)b[i]  );    }
                st.append(sw.toString());
            }
        } catch(IOException io) {
            printf(func,1,"ERROR: resoucse could not loaded - error "+io.getMessage(), io);
        }  
        return st.toString();
    }

    
}
