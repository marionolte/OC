/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.account;

import io.crypt.Crypt;

/**
 *
 * @author SuMario
 */
abstract public class User {
    private String user;
    private String pass;
    volatile private Crypt crypt=new Crypt();
    
    public User(String user, String pass){
        
        this.user=user;
        this.pass=pass;
    }
    
    public synchronized String   getCrypted(String a) { return crypt.getCrypted(a);   }
    public synchronized String getUnCrypted(String a) { return crypt.getUnCrypted(a); }
    public String getUsername() { return getUnCrypted(user); }
    public String getPassword() { return getUnCrypted(pass); }
    
    public void setUsername(String user) { this.user=getUnCrypted(user); }
    public void setPassword(String pass) { this.pass=getUnCrypted(pass); }
    
}
