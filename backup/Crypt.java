/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.crypt;

import general.HWMac;
import general.Version;
import io.file.ReadFile;
import io.file.WriteFile;
import java.io.File;
import java.util.UUID;
import main.Mos;


/**
 *
 * @author SuMario
 */
public class Crypt extends Version {

    static String sysId ="";
    static String usrId ="";
    static String hostId="";
    static ReadFile secfile=new ReadFile(System.getProperty("user.home")+File.separator+".ssh"+File.separator+"mysec");
    
    final private CryptHigh ch;
    final private CryptLow  cl;
    final private UUID uuid;
    private final String Ukey="5fa4a40a-53b4-4f7a-b132-61bd19b79a8e";
    int maxKeyLen;
    
    public Crypt() {
        uuid= UUID.fromString(Ukey);        
        Crypt.sysId  = HWMac.getSystemId();
        Crypt.usrId  = HWMac.getUserInfo();
        Crypt.hostId = HWMac.getHostName();
        
        ch=new CryptHigh(uuid);
        if (ch.getHighAllow()) { cl=null; }else{ cl=new CryptLow(uuid);}
        
        if ( secfile.isReadableFile() ) {
             String a = this.getUnCrypted(secfile.readOut().toString()) ;
             printf(getFunc("Crypt()"),0,"readout secfile:"+a);
        }
        
    }
    public Crypt(Mos m) {
        this();
    }
    
    public String getCrypted(String txt) {
         return ( cl == null )? ch.getCrypted(txt) : cl.getCrypted(txt);
    }
    
    public String getUnCrypted(String info) {
        return ( cl == null )? ch.getUnCrypted(info) : cl.getUnCrypted(info);
    }
    
    public byte[] getUnCryptedByte(String info) {
        return ( cl == null )? ch.getUnCryptedByte(info) : cl.getUnCryptedByte(info);
    }
    
    public void runArgs(String[] args) {
        boolean test = false; 
         
         for ( int i=0; i<args.length; i++ ) {
             if ( args[i].matches("-test") ) {
                 int j=args.length;
                 for( j=++i; j<args.length; j++) {
                   String s=args[j];
                   if ( s.matches("\\-d") ){ debug++;  ch.debug++; if(cl!=null){cl.debug++;} } else { 
                     String en = getCrypted(s);
                     String de = getUnCrypted(en);
                     String ma = ( s.equals(de) )?"YES":"NO";
                     log("main(String[] args)",0,"TESTING:"+s+":\nENCODED :"+en+":\nDECODED :"+de+":\nDECODED :"+getUnCrypted(s)+": (income)\nMATCHING:"+ma+"\n");
                   }  
                }
                i=j;
                test=true;
             } 
             else if (args[i].matches("-version")     && ! test ) { System.out.println("Crypt v"+this.getVersion()+" of "+this.getFullInfo());  }
             else if (args[i].matches("-crypt") && ! test ) { 
                WriteFile fa = new WriteFile(args[++i]);
                if ( ! fa.isReadableFile() ) {
                    String s= getCrypted(args[i].replaceAll("==$", "="));
                    System.out.println(s);
                 } else {
                    if ( ! fa.isBinaryFile() )  {
                       String s= fa.readOut().toString();
                              s= getCrypted(s.replaceAll("==$", "="));
                       fa.replace( s+((s.endsWith("="))?"":"=") );
                    } else {
                       System.out.println("WARNING:  do not handle binary files ");
                    }
                }    
             }
             else if (args[i].matches("-uncrypt") && ! test ) { 
                WriteFile fa = new WriteFile(args[++i]);
                if ( ! fa.isReadableFile() ) {
                    String s= getUnCrypted(args[i]);
                    System.out.println(s);
                 } else {
                    if ( ! fa.isBinaryFile() )  {
                       String s= fa.readOut().toString();
                              s=getUnCrypted(s);
                       fa.replace(s);
                    } else {
                        System.out.println("WARNING:  do not handle binary files ");
                    }
                }    
             }
         }
         
    }
    
    public static void main(String[] args) throws Exception {
         Crypt c = new Crypt();
               c.runArgs(args);
    }
    
    private void log(String func, int level, String msg) {
        if ( level == 0 ) {
            System.out.println(msg);
        } else {
            if ( level <= debug ) {
                System.out.println("DEBUG["+level+"/"+debug+"] Crypt::"+func+" - "+msg);
            }
        }    
    }
    private void log(String func, int level, String msg, Exception e) {
         log(func,level,msg);
         log(func,level, "Exception trown with "+e.getMessage());
         e.printStackTrace();
    }
}
