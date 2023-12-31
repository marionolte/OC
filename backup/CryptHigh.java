/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.crypt;

import general.Version;
import static java.lang.Character.digit;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author SuMario
 */
class CryptHigh extends Version {

    final private UUID uuid;
    final private String pass;
    private static String Ukey="5fa4a40a-53b4-4f7a-b132-61bd19b79a8e";
    int maxKeyLen;
    
    final private HashMap<String, String> pamap;
    
    CryptHigh(UUID u) {
        this.uuid=u;
        this.pass=getPass(uuid.toString());
        pamap = new HashMap<String, String>();
        pamap.put("usrId",  Crypt.usrId  );
        pamap.put("sysId",  Crypt.sysId  );
        pamap.put("hostId", Crypt.hostId );
        pamap.put(Crypt.usrId, getPass( Crypt.usrId ));
        pamap.put(Crypt.sysId, getPass( Crypt.sysId ));
        pamap.put(Crypt.hostId, getPass( Crypt.hostId ));
        
        init();
    }
    
    private CryptHigh() {
        this(UUID.fromString( Ukey )  );
    }
    
    public void setPassword(String key, String val) {
        if ( key == null ) { return; }
        String k = key.toLowerCase();
            if      ( k.matches("hostid") ) { pamap.put(Crypt.hostId, (val!=null && ! val.isEmpty())?val:getPass( Crypt.hostId ) ); }
            else if ( k.matches("usrid")  ) { pamap.put(Crypt.usrId,  (val!=null && ! val.isEmpty())?val:getPass( Crypt.usrId  ) ); }
            else if ( k.matches("sysid")  ) { pamap.put(Crypt.sysId,  (val!=null && ! val.isEmpty())?val:getPass( Crypt.sysId  ) ); }
            else {
                                                                   if (val!=null && ! val.isEmpty()){ 
                                                                       pamap.put(key,  val); 
                                                                   } else { 
                                                                       pamap.remove(key); 
                                                                   }
            }
    }
    
    private String getPass(String info){
        String ret="blank"+getVersion();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(  (info.getBytes()));
            byte[] mdbytes = md.digest();

            //convert the byte to hex format method 1
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mdbytes.length; i++) {
              sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString(); //(sb.length()<16)?sb.toString():sb.substring(0, 15);
        } catch(Exception e){}
        return ret;
    }
    
    boolean getHighAllow(){  return ( this.maxKeyLen > 128 ); }
    
    
    public boolean doing;
    private Cipher cipher=null;
    private Field field=null;
    private void init() { 
        final String func="CryptHigh::init() - ";
        try {
            field = Class.forName("javax.crypto.JceSecurity").getDeclaredField("isRestricted");
            field.setAccessible(true);
            field.set(null, java.lang.Boolean.FALSE);
        } catch (Exception ex) {
            log("init()",1,"strength isRestricted set error : "+ex.getMessage());
            if ( debug > 0 ) ex.printStackTrace();
        }
        try { 
                cipher= Cipher.getInstance("AES/CBC/NoPadding", "SunJCE");
        }
        catch(NoSuchAlgorithmException nsa) { cipher=null;}
        catch(NoSuchProviderException  nsp) { cipher=null;}
        catch(NoSuchPaddingException   nse) { cipher=null;}
        if ( cipher==null ) { doing=false; } else { doing=true; }   
        
        if ( ! doing )
            try {
                maxKeyLen = Cipher.getMaxAllowedKeyLength("AES");
                if (maxKeyLen <= 128 ) {  
                   try { 
                    cipher= Cipher.getInstance("AES"); 
                   }
                   catch(NoSuchAlgorithmException nsa) { cipher=null;}
                   catch(NoSuchPaddingException   nse) { cipher=null;}
                }
            } catch(NoSuchAlgorithmException ne) {}    
        println(2,func+"max length = "+maxKeyLen+" cipher are:"+cipher+" ("+doing+")");
        
       
    }
    
    
    private String getHostCrypted(String txt ) {
    byte[] b=encrypt(txt,pamap.get("hostId"),pamap.get("hostIdPass"));
         if ( b==null ) { return txt; }
         return new String(Base64.encode(b));    
    }
    private String getUserCrypted(String txt) {
        byte[] b=encrypt(txt,pamap.get("usrId"),pamap.get("usrIdPass"));
         if ( b==null ) { return txt; }
         return new String(Base64.encode(b));
    }
    public String getCrypted(String txt) {
         if ( ! doing ) { return new String(Base64.encode(txt.getBytes())); }
         byte[] b=encrypt(txt,uuid.toString(),pass);
         //byte[] b=encrypt(txt,Ukey,pass);
         if ( b==null ) { return ""; }
         return new String(Base64.encode(b));
    }
    
    public String getUnCrypted(String info) {
        if ( ! doing ) { 
            if ( info != null ) {
                byte[] b=Base64.decodeBase64(info); 
                StringBuilder sw= new StringBuilder(); 
                for (int i=0; i<b.length; i++){ sw.append( (char) b[i] ); }
                return sw.toString();
            } 
            return info; 
                
        }
        final String func="CryptHigh::getUnCrypted(String info) - ";
        if ( info != null && info.endsWith("=")) {
            byte[] b=Base64.decodeBase64(info);
            String s=decrypt(b,uuid.toString(),pass); 
            //String s=decrypt(b,Ukey,pass); 
            StringBuilder sw= new StringBuilder();
            int c=0;
            checkOutter:
            while( c<s.length() ) {
                char ch = s.charAt(c);
                if ( ch != 0 ) { sw.append(ch); } else { break checkOutter; }
                c++;
            }
            println(6,func+"return |"+replacePass(sw.toString())+"|");
            return sw.toString();
        }
        log(func, 6, "return info |"+info+"|");
        return info;
    }
    public byte[] getUnCryptedByte(String info) {
        if ( ! doing ) {
            return Base64.decodeBase64(info);
        }
        return decryptByte(Base64.decodeBase64(info),uuid.toString(),pass);
    }
    
    private byte[] enBase64(byte[] b   ) { return Base64.encodeBase64(b); }
    private byte[] encrypt(String plainText, String enkey, String pw)  {
          final String func="encrypt(String plainText, String encryptionKey, String pass)";
          SecretKeySpec key ;
          IvParameterSpec iv ;
	  try {	
                log(func,1,"enKey:"+enkey.length()+"(64)   pw:"+pw.length()+"(16)");
                 key = new SecretKeySpec(getBytes(updateLength(enkey,64)), "AES");
                  iv = new IvParameterSpec(getBytes(updateLength(pw, 16)));
                //log(func,1, key.
		cipher.init(Cipher.ENCRYPT_MODE, key, iv);
                log(func, 1, "encrption text length="+plainText.length()+"  mod="+(plainText.length()%16));
		return cipher.doFinal(updateLength(plainText,64).getBytes("UTF-8"));
          } catch (Exception e) {
              doing=false;
              log(func, 1, "encrption error unlimited strength - "+e.getMessage(),e);
              
          }  
          
          return plainText.getBytes();
          //return null;
    }
    
    private String updateLength(String str, int len){
        final String func="updateLength(String str, int len)";
        int size=(str.length() % len);
        if ( size == 0 ) { return str; }
        StringBuilder sw=new StringBuilder(); sw.append(str);
        log(func, 3, "size:"+size+" base are :"+len);
        int c=0;
        if (size != 0 ) {
            while( (size+c) < len ) {
                sw.append("\0"); c++;
            }
        }    
        log(func, 3, "new size "+sw.length()+" test:"+(sw.length()%len)+" have added "+c+" null" );
        return sw.toString();
    }
    
    private static byte[] getBytes(String input) {
        int length = input.length();
        byte[] output = new byte[length / 2];

        for (int i = 0; i < length; i += 2) {
            output[i / 2] = (byte) ((digit(input.charAt(i), 16) << 4) | digit(input.charAt(i+1), 16));
        }
        return output;
    }
    
    private byte[] deBase64(String text) { return  Base64.decodeBase64(text); }
    private String decrypt(byte[] cipherText, String encryptionKey, String pass) {
         final String func="decrypt(byte[] cipherText, String encryptionKey, String pass)";
         /*SecretKeySpec key;IvParameterSpec iv;
         try {
              
		 key = new SecretKeySpec(getBytes(updateLength(encryptionKey,64)), "AES");
                  iv = new IvParameterSpec(getBytes(updateLength(pass, 16)));
		cipher.init(Cipher.DECRYPT_MODE, key,iv);
		return new String(cipher.doFinal(cipherText),"UTF-8");
         } catch (Exception e) {
             log(func, 1, "encrption error (with unlimited size)- "+e.getMessage(),e);
         }*/
         try {
            return new String( decryptByte(cipherText,  encryptionKey,  pass) );
         } catch(Exception e) {}           
         return null;       
    } 
    
    private byte[] decryptByte(byte[] cipherText, String encryptionKey, String pass) {
        final String func="decrypt(byte[] cipherText, String encryptionKey, String pass)";
         SecretKeySpec key;IvParameterSpec iv;
         try {
              
		 key = new SecretKeySpec(getBytes(updateLength(encryptionKey,64)), "AES");
                  iv = new IvParameterSpec(getBytes(updateLength(pass, 16)));
		cipher.init(Cipher.DECRYPT_MODE, key,iv);
		return cipher.doFinal(cipherText);
         } catch (Exception e) {
             log(func, 1, "encrption error (with unlimited size)- "+e.getMessage(),e);
         }
         
         return null; 
    }
    
    public static void main(String[] args) throws Exception {
        
        CryptHigh c = new CryptHigh( );
        for(String s: args) {
            if ( s.matches("\\-d") ){ c.debug++; } else { 
              String en = c.getCrypted(s);
              String de = c.getUnCrypted(en);
              String ma = ( s.equals(de) )?"YES":"NO";
              c.log("main(String[] args)",0,"TESTING:"+s+":\nENCODED :"+en+":\nDECODED :"+de+":\nMATCHING:"+ma+"\n");
            }  
        }
    }
    
    
    private void log(String func, int level, String msg) {
        println(level,func+" - "+msg);
    }
    private void log(String func, int level, String msg, Exception e) {
         log(func,level,msg);
         log(func,level, "Exception trown with "+e.getMessage());
         e.printStackTrace();
    }
}
