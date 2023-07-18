/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.io.crypt;

import com.macmario.general.Version;
import static java.lang.Character.digit;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.UUID;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author SuMario
 */
class CryptHigh extends Version {
    private UUID uuid;
    private String pass;
    private String Ukey="5fa4a40a-53b4-4f7a-b132-61bd19b79a8e";
    int maxKeyLen;
    final private String secCipher="AES";
    
    CryptHigh(UUID u) {
        this.uuid=u;
        this.pass=getPass(uuid.toString());
        init();
    }
    
    private CryptHigh() {
        uuid= UUID.fromString(Ukey);
        pass=getPass(uuid.toString());
        init();
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
    
    boolean updateUKey(UUID   u) { return updateUKey(u.toString()); }
    boolean updateUKey(String u) { 
        boolean b = ( u!= null && ! u.isEmpty() ); 
        if ( b ) {
            Ukey=u; 
            uuid=UUID.fromString(Ukey);
            pass=getPass(uuid.toString());
            init();
        }
        return b; 
    }
    
    public boolean doing;
    private Cipher cipher=null;
    private Field field=null;
    private void init() { 
        final String func="CryptHigh::init() - ";
        try {
          int v=getJavaMainVersion();
          //  System.out.println("Java Main Version:"+v);
          if ( v <= 7 || (v==8 && getJavaMinVersion() < 152 )) {  
            field = Class.forName("javax.crypto.JceSecurity").getDeclaredField("isRestricted");
            int modify = field.getModifiers();
            if ( Modifier.isFinal(modify) && Modifier.isStatic(modify) && Modifier.isPrivate(modify) ) {
                field.setAccessible(true);
                field.setBoolean(null, java.lang.Boolean.FALSE);
                field.setAccessible(false);
            } else {
                throw new RuntimeException("newer JRE/JDK used");
            } 
          }  
            
        } catch (ClassNotFoundException cnf ) { printf(func,1,"strength isRestricted class set error  : "+cnf.getMessage(),cnf); }
          catch (IllegalAccessException iae ) { printf(func,1,"strength isRestricted access set error : "+iae.getMessage(),iae); }
          catch (NoSuchFieldException   nsfe) {  printf(func,1,"strength isRestricted field error : "    +nsfe.getMessage(),nsfe); }
          catch (RuntimeException       re)   { printf(func,1,"strength isRestricted set error : "       +re.getMessage(),re); }
        
        try { 
                //cipher= Cipher.getInstance("AES/ECB/NoPadding", "SunJCE");
                cipher = Cipher.getInstance(secCipher, "SunJCE");
        }
        catch( NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException ne ){
            printf(func,1,"ERROR: "+ne.getMessage(),ne);
            cipher=null;
        }
        if ( cipher==null ) { doing=false; } else { doing=true; }   
        
        try {
            maxKeyLen = Cipher.getMaxAllowedKeyLength(secCipher);
            printf(func,2,"maxKeyLen:"+maxKeyLen);
                    
            if (maxKeyLen <= 128 ) {  
               try { 
                    cipher= Cipher.getInstance(secCipher); 
               }
               catch(NoSuchAlgorithmException nsa) { cipher=null;}
               catch(NoSuchPaddingException   nse) { cipher=null;}
            }
        } catch(NoSuchAlgorithmException ne) {}    
        println(1,func+"max length = "+maxKeyLen+" cipher are:"+cipher+" ("+doing+")");
        
       
    }
    
    
    public String getCrypted(String txt) {
        final String func="getCrypted(String txt)";
        printf(func,1,"doing:"+doing+":");
        
         if ( ! doing ) { return new String(Base64.encode(txt.getBytes())); }
         byte[] b=encrypt(txt,uuid.toString(),pass);
         
        if ( b==null ) { 
            printf(func,1,"return:: - NULL");
            return ""; 
        }
        
        printf(func,1,"return:"+b.length+":"); 
        return new String(Base64.encode(b));
    }
    
    public String getUnCrypted(String info) {
        final String func="CryptHigh::getUnCrypted(String info)";
        if ( ! doing ) { 
            if ( info != null ) {
                byte[] b=Base64.decodeBase64(info); 
                StringBuilder sw= new StringBuilder(); 
                for (int i=0; i<b.length; i++){ sw.append( (char) b[i] ); }
                printf(func, 4, "return sw |"+sw.toString()+"|");
                return sw.toString();
            } 
            printf(func, 4, "return info [!doing] |"+info+"|");
            return info; 
                
        }
        byte[] b =null;
        try {
            b=Base64.decodeBase64(info);
        }catch ( NullPointerException | IllegalArgumentException   np ){
            b=null;
        }
        
        if ( b != null ) {
            String s=new String( decrypt(b,uuid.toString(),pass) ); 
            if ( s == null ) { return info; }
            
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
        printf(func, 4, "return info |"+info+"|");
        return info;
    }
    
    public byte[] getUnCryptedByte(String info) {
        if ( info != null && info.endsWith("=")) {
            byte[] b=Base64.decodeBase64(info);
                   if ( ! doing ) { return b; }
                   b=decryptByte(b,uuid.toString(),pass);
            return b;       
        }
        return new byte[0];
    }
    
    private byte[] enBase64(byte[] b   ) { return Base64.encodeBase64(b); }

    
    private  byte[] encrypt(String plainText, String enkey, String pw) {
        final String func="encrypt(String plainText, String enkey, String pw)";
        try {
            printf(func,2,"enkey:"+enkey+": pw:"+pw+":");
            SecretKeySpec skeySpec = new SecretKeySpec((pw).getBytes(), secCipher);
            printf(func,4,"spec completed");
            Cipher ci = Cipher.getInstance(secCipher);
            printf(func,4,"init start "+skeySpec+" "+ci);
            ci.init(Cipher.ENCRYPT_MODE, skeySpec);

            printf(func,4,"init completed");
        //System.out.println("Base64 encoded: "+ java.util.Base64.getEncoder().encode(data.getBytes()).length);

            byte[] original = java.util.Base64.getEncoder().encode(ci.doFinal(plainText.getBytes()));
            return original;
        }
        catch( java.security.NoSuchAlgorithmException 
                | java.security.InvalidKeyException 
                | javax.crypto.NoSuchPaddingException 
                | javax.crypto.BadPaddingException 
                | javax.crypto.IllegalBlockSizeException e){
             printf(func,1,"ERROR: message - "+e.getMessage(),e);   
        }
        catch ( Exception ef) {
            printf(func,1,"ERROR: message - "+ef.getMessage(),ef);
        }
        printf(func,4,"return blank");
        return "".getBytes();
    }
    
    private byte[] encrypt1(String plainText, String enkey, String pw)  {
          final String func="encrypt(String plainText, String encryptionKey, String pass)";
          SecretKeySpec key ;
          IvParameterSpec iv ;
	  try {	
                log(func,1,"enKey:"+enkey.length()+"(64)   pw:"+pw.length()+"(16)");
                 key = new SecretKeySpec(getBytes(updateLength(enkey,64)), secCipher);
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
    private static byte[] getBytes(byte[] b) {
        byte[] a = b;
        if ( b.length%16 != 0 ) {
             a=new byte[ a.length+(16-a.length+1)  ];
             for( int i=0;        i<b.length; i++) { a[i]=b[i]; }
             for( int i=b.length; i<a.length; i++) { a[i]=0;    }
        }
        return a;
    }

    private byte[] decrypt(byte[] encData, String encryptionKey, String pass) {
        final String func="decrypt(byte[] encData, String encryptionKey, String pass)";
        try {
            printf(func,1,"init skeySpek with "+secCipher );
            SecretKeySpec skeySpec = new SecretKeySpec( (pass).getBytes(), secCipher);
            printf(func,1,"init cipher ci with "+secCipher );
            Cipher ci = Cipher.getInstance(secCipher);

            ci.init(Cipher.DECRYPT_MODE, skeySpec);

            printf(func,1,"init cipher ci complete");
            
            //System.out.println("Base64 decoded: "+java.util.Base64.getDecoder().decode( encData ).length );
            byte[] original = ci.doFinal(java.util.Base64.getDecoder().decode(encData));
            printf(func,1,"return "+( (original!=null)?original.length:"NULL" ) );
            return original;
        } 
        catch ( NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            printf(func,1,"ERROR: "+e.getMessage(),e);
        }
        return "".getBytes();
    }
    
    private byte[] deBase64(String text) { return  Base64.decodeBase64(text); }
    private String decrypt1(byte[] cipherText, String encryptionKey, String pass) {
         final String func="decrypt(byte[] cipherText, String encryptionKey, String pass)";
         SecretKeySpec key;IvParameterSpec iv;
         try {
		 key = new SecretKeySpec(getBytes(updateLength(encryptionKey,64)), secCipher);
                  iv = new IvParameterSpec(getBytes(updateLength(pass, 16)));
		cipher.init(Cipher.DECRYPT_MODE, key,iv);
                int mod = cipherText.length%16;
                printf(func,3,"cipherText len:"+cipherText.length+":  mod:"+mod );
                if ( mod != 0  ) {
                     byte[] b = new byte[ cipherText.length+mod ];
                     for ( int i=0; i<cipherText.length; i++) { b[i]=cipherText[i]; }
                     for ( int i=cipherText.length; i< b.length ; i++) { b[i]=0; }
                     cipherText=b;
                     printf(func,3,"cipherText new len:"+cipherText.length+":  mod:"+(cipherText.length%16) );
                } 
		String s=new String(cipher.doFinal(cipherText),"UTF-8");
                //String s=new String(cipher.doFinal(getBytes(cipherText)),"UTF-8");
                printf(func,3,"s:"+s+":  len:"+s.length()+"  mod:"+(s.length()%16));
                return s;
         } catch (Exception e) {
             log(func, 1, "encrption error (with unlimited size)- "+e.getMessage(),e);
         }
         
         return null;       
    } 
    
    private byte[] decryptByte(byte[] cipherText, String encryptionKey, String pass) {
            return decrypt( cipherText,  encryptionKey, pass);
    }
    private byte[] decryptByte1(byte[] cipherText, String encryptionKey, String pass) {
         final String func="decrypt(byte[] cipherText, String encryptionKey, String pass)";
         SecretKeySpec key;IvParameterSpec iv;
         try {
              
		 key = new SecretKeySpec(getBytes(updateLength(encryptionKey,64)), secCipher);
                  iv = new IvParameterSpec(getBytes(updateLength(pass, 16)));
		cipher.init(Cipher.DECRYPT_MODE, key,iv);
		return cipher.doFinal(cipherText);
         } catch (Exception e) {
             log(func, 1, "encrption error (with unlimited size)- "+e.getMessage(),e);
         }
         
         return cipherText;       
    } 
    
    public static void main(String[] args) throws Exception {
         CryptHigh c = new CryptHigh();
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
