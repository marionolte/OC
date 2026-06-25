/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.wls;

import com.macmario.io.account.User;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author SuMario
 */
public class WlsUser extends User {

      private final URL url;
     
      public WlsUser(String url, String user, String pass) throws MalformedURLException{
          this( new URL(url), user,pass);
      }
      public WlsUser(URL url, String user, String pass){
          super(user,pass);
          this.url=url;
      }
    
      @Override
      public void setUsername(String a) { throw new RuntimeException("username could not modified after creation"); }
      @Override
      public void setPassword(String a) { throw new RuntimeException("password could not modified after creation"); }
      
      public URL getUrl() { return this.url; }
      
      
}
