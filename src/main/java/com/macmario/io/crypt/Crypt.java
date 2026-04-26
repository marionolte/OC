/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.io.crypt;

import com.macmario.general.Version;
import com.macmario.io.file.WriteFile;
import java.util.UUID;
import com.macmario.main.Mos;
import com.macmario.net.tcp.Host;
import java.util.ArrayList;
import java.util.Base64;

/**
 *
 * @author SuMario
 */
public class Crypt extends Version {
    final private CryptHigh ch;
    final private byte[] base64Alphabet;
    final private int base64Length=255;
    private CryptHigh hostch;
    private CryptHigh custch=null;
    private CryptHigh userch;
    
          
    final private UUID uuid;
    private       String Ukey; //Ukey="5fa4a40a-53b4-4f7a-b132-61bd19b79a8e";
    private       String host=Host.getHostname();
    private       String user=getUserKey();
    int maxKeyLen;
    
    private int cryptLevel=0;
    public Crypt() {
        Ukey=(super.readPropertyFromRessource("/com/macmario/main/main.properties")).getProperty("UKEY", "");
        System.out.println("Ukey:"+Ukey+":  "+Ukey.length());
        uuid= UUID.fromString(Ukey);
        ch=new CryptHigh(uuid);
        base64Alphabet = new byte[base64Length];
        init();
    }
    public Crypt(Mos m) {
        this();
    }
    
    
    public void setHostKey(String info) {
        if ( info == null || info.isEmpty() ) { return; }
        host = getUUIDCode(info).toString();
        init();
    }
    
    public void setUserKey(String info) {
        if ( info == null || info.isEmpty() ) { return; }
        user = getUUIDCode(info).toString();
        init();
    }
    
    public void setCustomKey(String info) {
        if ( info == null || info.isEmpty() ) { return; }
        custch = new CryptHigh(getUUIDCode(info)) ;
    }
    
    public void setCryptLevel(int level) {
        this.cryptLevel=(level>0)?level:0;
    }
    
    public boolean updateUKey(UUID   u) { return updateUKey(u.toString()); }
    public boolean updateUKey(String u) { 
        boolean b=false;
                b=custch.updateUKey(u);
        if (b) { Ukey=u ;  }
        return b; 
    }
    
    private void init() {
        
            hostch=new CryptHigh(getUUIDCode(host));
            userch=new CryptHigh(getUUIDCode(user));
        
        for (int i=0;   i<base64Length; i++ ) { this.base64Alphabet[i]=(byte) -1; }
        for (int i='Z'; i>='A';         i-- ) { this.base64Alphabet[i]=(byte) (i-'A');    }
        for (int i='z'; i>='a';         i-- ) { this.base64Alphabet[i]=(byte) (i-'a'+26); }
        for (int i='9'; i>='0';         i-- ) { this.base64Alphabet[i]=(byte) (i-'0'+52); }
        this.base64Alphabet[62]=(byte) '+';
        this.base64Alphabet[63]=(byte) '/';
    }
    
    private UUID getUUIDCode(String info) {
        try { 
            return UUID.fromString(info);
        } catch(java.lang.IllegalArgumentException iae) { 
            StringBuilder sw = new StringBuilder();
                          
            byte[] b = getMD5(info).getBytes();  // [8]-[4]-[4]-[12]
            for ( int i=0; i<27; i++) {
                if (i==8 || i==12 || i==16 || i==20) { sw.append("-");}
                if (i< 27 ){
                    if ( i < b.length ) { sw.append( (char)b[i] ); }
                    else { sw.append("1"); }
                }
            }
            
            return UUID.fromString(sw.toString());
        }
    }
    
    public String getMD5(String info) {  return MD5.toMD5Hash(info);     }
    
    public boolean isBase64Regex(String txt) {
        try { 
            return txt.matches("^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Z]a-z0-9+/]{3}==)?$");
        }catch(NullPointerException io){            
        }   
        return false;
    }
    public boolean isCrypted(StringBuilder txt) {
        if ( isNotNullOrEmpty(txt) ) {
            return   isCrypted(txt.toString());
        }    
        return false;    
    }
    public boolean isCrypted(String txt) {
        try {
            if ( isBase64Regex(txt) ) {
                Base64.getDecoder().decode(txt);
                return true;
            }
        } catch( IllegalArgumentException|NullPointerException io) {
        }
        return false;
        //if ( txt == null || txt.isEmpty() ) return false;
        //return isCrypted(txt.getBytes());
    }
    
    public boolean isCrypted(byte[] b) {
        boolean br=false;
        for (int i=0; i<b.length; i++) {
            if ( ! isBase64(b[i]) ) {
                return false;
            } 
        } 
        //System.out.println("br last:"+b[ b.length-1 ]);
        if ( b[ b.length-1 ] == 61 ) { br=true; }
        return br;
    }
    
    final private byte PAD = (byte)'=';
    public boolean isBase64(byte oct) {
        boolean ret=false;
        if      ( oct == PAD                ) { ret=true; }
        else if ( oct < 0                   ) { ret=false;}
        else if ( base64Alphabet[oct] == -1 ) { ret=false;}
        else                                  { ret=true; }
        return ret;
    }
    
    public boolean isBase64(byte[] b) {
        try {
            Base64.getDecoder().decode(b);
            return true;
        } catch (IllegalArgumentException|NullPointerException io){            
        }   
        return false;
    }
    
    public String getCrypted(String txt) {
         String out = getUserCrypted(txt);
                out = getHostCrypted(out);
                out = getCustCrypted(out);
         return ch.getCrypted(out);
    }
        
    private String getUserCrypted(String txt) {
        if( cryptLevel > 0 &&  userch!=null  ) {
        
            StringBuilder sw = new StringBuilder();
                          sw.append(userch.getCrypted(getHostCrypted(txt)));
                          //if ( sw.length() > 4 && ! sw.toString().endsWith("=") ) { sw.append("="); }
            return "<"+user+" md5=\""+getMD5(sw.toString())+"\">"+sw.toString()+"</"+user+">";
        }
        return txt;
    }
    
    private String getHostCrypted(String txt) {
        if( cryptLevel > 1 ) {
            StringBuilder sw = new StringBuilder();
                          sw.append(hostch.getCrypted(txt));
                          //if ( ! sw.substring(sw.capacity()-1).equals("=") ) { sw.append("="); }
            return "<"+host+">"+sw.toString()+"</"+host+">";
        }
        return txt;
    }
    
    private String getCustCrypted(String txt) {
        if(  ( custch!=null ) ) { //|| ( custcl!=null )  ) {
            StringBuilder sw = new StringBuilder();
                          sw.append(custch.getCrypted(txt));
                          //if ( ! sw.substring(sw.length()-1).equals("=") ) { sw.append("="); }
            return sw.toString();
        }
        return txt;
    }
    
    public String getUnCrypted(String info) {
        String out= ch.getUnCrypted(info);         
               out=getUnCryptedCust(out);        
               out=getUnCryptedHost(out);        
               out=getUnCryptedUser(out);        
               out=getUnCryptedHost(out);        
        return out;
    }
    private String getUnCryptedHost(String info) {
        final String a = "<"+host; final String e="</"+host+">";
        if ( info.startsWith(a) && info.endsWith(e) ) {
                      String[] ab = info.split(">"); 
           String f = info.substring(ab[0].length()+1,info.length()-e.length());
           String md5 = getCryptedMD5(ab[0]);
           if ( md5.isEmpty()  || ( ! md5.isEmpty() && md5.matches(getMD5(f)) )    )  {
                return hostch.getUnCrypted(f);
           
           }
        }
        return info;
    }
    private String getUnCryptedUser(String info) {
        final String a = "<"+user; final String e="</"+user+">";
        if ( info.startsWith(a) && info.endsWith(e) ) {
           String[] ab = info.split(">"); 
           String f = info.substring(ab[0].length()+1,info.length()-e.length());
           String md5 = getCryptedMD5(ab[0]);
           if ( md5.isEmpty()  || ( ! md5.isEmpty() && md5.matches(getMD5(f)) )    )  {
                return userch.getUnCrypted(f) ;
           }
        }
        return info;
    }
    
    synchronized private String getCryptedMD5(String info) {
        String[] sp = info.split("\"");
        for(int i=0; i< sp.length;i++) {
            if ( sp[i].endsWith("md5=") && i+i < sp.length ) { return sp[++i]; }
        }
        return "";
    }
    
    private String getUnCryptedCust(String info) {
        if( ( custch !=null ) ) { //|| ( custcl!=null )  && info.endsWith("=") ) {
            String out = custch.getUnCrypted(info);            
            return out;
        }
        return info;
    }
    
    public byte[] getUnCryptedByte(String info) {
        return ch.getUnCryptedByte(info);
    }
    
    public byte[] getCryptedByte(String info) {
        return ch.getCrypted(info).getBytes();
    }
    
    public void runArgs(String[] args) {
        boolean test = false;  String cust=Host.getHostname()+"1234@456789-0";
         ArrayList<String> ar=new ArrayList<>();
         for ( int i=0; i<args.length; i++ ) {
             if ( args[i].equals("-d") ){ debug++;}  else { ar.add(args[i]); }
         }
         ch.debug=debug;
         if ( ! ar.isEmpty() ) {
             args=new String[ar.size()];
             for ( int i=0; i<ar.size(); i++ ) { args[i]=ar.get(i); }
         }
         for ( int i=0; i<args.length; i++ ) {
             if ( args[i].matches("-max") ) {
                    setCryptLevel(Integer.parseInt(args[++i]));
                    
             }
             else if ( args[i].matches("-custkey")   ) { cust=args[++i];     }
             else if ( args[i].matches("-usecustkey")) { setCustomKey(cust); }
             else if ( args[i].matches("-test") ) {
                 int j=args.length;
                 for( j=++i; j<args.length; j++) {
                   String s=args[j];
                   if ( s.equals("-d") ){  } else { //if(cl!=null){cl.debug++;} } else { 
                     String en = getCrypted(s);
                     String de = getUnCrypted(en);
                     String ma = ( s.equals(de) )?"YES":"NO";
                     log(0,"main(String[] args) TESTING:"+s+":\nENCODED :"+en+":\nDECODED :"+de+":\nDECODED :"+getUnCrypted(s)+": (income)\nMATCHING:"+ma+"\n");
                   }  
                }
                i=j;
                test=true;
             } 
             else if (args[i].matches("-version")     && ! test ) { System.out.println("Crypt v"+this.getVersion()+" of "+this.getFullInfo());  }
             else if (args[i].matches("-crypt") && ! test ) { 
                WriteFile fa = new WriteFile(args[++i]);
                if ( ! fa.isReadableFile() ) {
                    String s= getCrypted(args[i]); //.replaceAll("==$", "="));
                    System.out.println(s);
                 } else {
                    if ( ! fa.isBinaryFile() )  {
                       String s= fa.readOut().toString();
                              s= getCrypted(s); //.replaceAll("==$", "="));
                       fa.replace( s ); //+((s.endsWith("="))?"":"=") );
                    } else {
                       System.out.println("WARNING:  do not handle binary files ");
                    }
                }    
             }
             else if (args[i].matches("-uncrypt") && ! test ) { 
                WriteFile fa = new WriteFile(args[++i]);
                if ( ! fa.isReadableFile() ) {
                    //System.out.println("incoming:"+args[i]);
                    String s= getUnCrypted(args[i]);
                    //System.out.println("outgoing:"+s);
                 } else {
                    if ( ! fa.isBinaryFile() )  {
                       String s= fa.readOut().toString();
                              s=getUnCrypted(s);
                       fa.replace(s);
                    } else {
                        System.out.println("WARNING:  do not handle binary files ");
                    }
                }    
             } else {
                 System.out.println("i="+i+": |"+args[i]+"|");
                 System.out.println(usage(true));
             }
         }
         
    }
    
    public static String getRandomID() {
        String x=(""+Math.random());
        return x.substring(2);
    }
    
    public static void main(String[] args) throws Exception {
         Crypt c = new Crypt();         
               c.runArgs(args);
    }

    public String usage(boolean b) {
        StringBuilder sw=new StringBuilder();
        if (b) sw.append("usage() : ");
        sw.append("[-max <0..2>] [-custkey <custom key> -usecustkey] <-crypt|-uncrypt> <String|File>");
        return sw.toString();
    }
    
    /*private void log(String func, int level, String msg) {
        if ( level == 0 ) {
            System.out.println(msg);
        } else {
            if ( level <= debug ) {
                System.out.println("DEBUG["+level+"/"+debug+"] Crypt::"+func+" - "+msg);
            }
        }    
    }*/
    
}
