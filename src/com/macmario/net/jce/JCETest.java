/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.jce;

import java.security.NoSuchAlgorithmException;

/**
 *
 * @author SuMario
 */
public class JCETest {
    
    public static boolean jceUnLimited() throws NoSuchAlgorithmException {
       return ( javax.crypto.Cipher.getMaxAllowedKeyLength("AES") == Integer.MAX_VALUE );
    }
    
    public static void main(String[] args) throws Exception {
         System.out.println("JCE unlimited strengths: "+jceUnLimited() );
    }
    
}
