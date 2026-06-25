/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.wls;

import com.macmario.general.Version;
import static com.macmario.general.Version.printf;
import com.macmario.io.file.ReadDir;
import com.macmario.io.file.WriteFile;
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

    public WlsDecrypt(WlsDomain wls) {
        this.wls=wls;
    }
    
    public void decrypt() throws IOException {
        final String func=getFunc("decrypt()");
        final String f="info.py";
        if ( wls != null ) {
             //System.out.println("read base script");
             String script = getOutString( new BufferedInputStream( WlsDecrypt.class.getResourceAsStream("/net/wls/scripts/decrypthash.py") ) )
                                 .replace("@@USER@@", wls.getAdminUser())
                                 .replace("@@PASS@@", wls.getAdminPassword())
                                 .replace("@@NMUS@@", wls.getNodeUser() )
                                 .replace("@@NMPA@@", wls.getNodePassword()   ); 
            //System.out.println("script:"+script+":");
            //System.out.println("create "+f); 
            WriteFile wt = new WriteFile(wls.getDomainLocation()+File.separator+f); wt.replace(script);
            Process p = null;
            ProcessBuilder pb = new ProcessBuilder("bash", "-c","( cd "+wls.getDomainLocation()+" &&  . ./bin/setDomainEnv.sh && java weblogic.WLST "+f+" 2>&1 )");
            //pb.directory(new File (wls.getDomainLocation() ) );
            p = pb.start(); 
            
            
            InputStreamReader isr = new InputStreamReader(p.getInputStream());
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                //System.out.println("=>"+line+"<=");
                if      ( line.startsWith("User:")   )  { String v = splitout(line);   _user=v;} 
                else if ( line.startsWith("Pass:")   )  { String v = splitout(line);   _pass=v;} 
                else if ( line.startsWith("NMUser:") )  { String v = splitout(line);  _nuser=v;} 
                else if ( line.startsWith("NMPass:") )  { String v = splitout(line);  _npass=v;} 
                
            }
            wt.delete();
            
            //System.out.println("User \t->"+getUser()+"<-\nPass \t->"+getPass()+"<-\nNMUser \t->"+getNMUser()+"<-\nNMPass \t->"+getNMPass()+"<-");
            
        }
    }

    private String _user="";
    private String _pass="";
    private String _nuser="";
    private String _npass="";
    
    public String getUser()   { return  _user; }
    public String getNMUser() { return _nuser; }
    public String getPass()   { return  _pass; }
    public String getNMPass() { return _npass; }
    
    public String setUser(  String u) {  _user=u; return getUser();   }
    public String setNMUser(String u) { _nuser=u; return getNMUser(); }
    public String setPass(  String p) {  _pass=p; return getPass();   }
    public String setNMPass(String p) { _npass=p; return getNMPass(); }
    
    private String splitout(String s             ) { return splitout(s,":"); }
    private String splitout(String s,String split) {
        String[] sp = s.split(split);
        if ( s.length() > sp[0].length()+1 ) { return s.trim().substring(sp[0].length()+1);}
        return "";
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
        //System.out.println("getOutString:"+st.toString()+":");
        return st.toString();
    }

    
}
