/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.macmario.io.crypt;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 *
 * @author MNO
 */
public class TestAES {
    
    
    private static String decrypt_data(String encData) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
    String key = "bad8deadcafef00d";
    SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes(), "AES");
    Cipher cipher = Cipher.getInstance("AES");

    cipher.init(Cipher.DECRYPT_MODE, skeySpec);

    System.out.println("Base64 decoded: "+ 
            Base64.getDecoder().decode( encData.getBytes() ).length );
    byte[] original = cipher.doFinal(Base64.getDecoder().decode(encData.getBytes()));
    return new String(original).trim();
}

  private static String encrypt_data(String data) throws Exception {
    String key = "bad8deadcafef00d";
    SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes(), "AES");
    Cipher cipher = Cipher.getInstance("AES");

    cipher.init(Cipher.ENCRYPT_MODE, skeySpec);

    System.out.println("Base64 encoded: "
            + Base64.getEncoder().encode(data.getBytes()).length);

    byte[] original = Base64.getEncoder().encode(cipher.doFinal(data.getBytes()));
    return new String(original);
  }
  
    public static void main(String[] args) throws Exception {
        String a = encrypt_data(args[0]);
        String b = decrypt_data(a);
        System.out.println(args[0]+"\n"+a+"\n"+b );
    }
}
