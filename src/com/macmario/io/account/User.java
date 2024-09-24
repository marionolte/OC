/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.io.account;

import com.macmario.io.crypt.Crypt;

/**
 *
 * @author SuMario
 */
abstract public class User {
    private String user=null;
    private String pass=null;
    volatile private Crypt crypt=new Crypt();
    
    public User(String user, String pass){  
        //System.out.println("user:"+( (user == null)?"NULL":user )+": pass:"+( (pass == null)?"NULL":pass ) );
        this.user=getCrypted(user);
        this.pass=getCrypted(pass);
    }
    
    public synchronized String   getCrypted(String a) { return crypt.getCrypted(a);   }
    public synchronized String getUnCrypted(String a) { return crypt.getUnCrypted(a); }
    public String getUsername() { return getUnCrypted(user); }
    public String getPassword() { return getUnCrypted(pass); }
    public String genPassword() { return GenPassword.getPassword(PasswordTyp.MEDIUM); }
    public String genStrongPassword() { return GenPassword.getPassword(PasswordTyp.STRONG); }
    public String genEasyPassword() { return GenPassword.getPassword(PasswordTyp.EASY); }
    
    public void setUsername(String user) { this.user=getUnCrypted(user); }
    public void setPassword(String pass) { this.pass=getUnCrypted(pass); }
    
    public boolean isPasswordSet() { return (this.pass!=null);}
    public boolean isUsernameSet() { return (this.user!=null);}
    
}
