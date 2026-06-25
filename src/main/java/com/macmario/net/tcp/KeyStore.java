/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.macmario.net.tcp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;



/**
 *
 * @author SuMario
 */
public class KeyStore extends Store {
    
    

    private String keystoreFilename = "mykeystore";
    private StoreType certType = StoreType.JKS ;   // JKS 
    
    java.security.KeyStore keys;
    
    public KeyStore(){ super(); }
    public KeyStore(StoreType ct, String[] key) {
        this();
        if ( key != null ) {
            if ( key.length >= 1 )
                this.setPassword(key[0]);
            if ( key.length >= 3 )
                this.setAlias(key[2]);
            if ( key.length >= 2 )
                this.setKeyStoreFile(key[1]);
        }
        this.certType=ct;
        try {
            keys = java.security.KeyStore.getInstance(ct.name());
        } catch(Exception e) {
            printf("KeyStore(StoreType ct, String[] key)",1,"ERROR: create KeyStore with exception : "+e.toString(),e);
        }    
    }
    
    
    public static KeyStore getInstance(String typ) throws KeyStoreException {
           KeyStore ks = new KeyStore( StoreType.getInstance(typ) , new String[] { } );
           
           return ks;
    }
    
    public void load(boolean createOnFail, String fname, String passwd){
        try {
            keys.load(new FileInputStream(fname), passwd.toCharArray() );
        } catch(Exception e ) {
            if ( createOnFail ) {
                this.setPassword(passwd);
                this.setKeyStoreFile(fname);                
            }
        }    
    }
    
    public void load(FileInputStream fileIO, char[] passwd) throws Exception {
            keys.load(fileIO, passwd);
    }
    
    public String getKeyStoreFile(){return this.keystoreFilename; }
    public void   setKeyStoreFile(String fname){ 
        File f = new File(fname);
        if ( ! f.exists() ) { createKeyStoreFile(certType, fname);}
        if ( f.canRead() ) {
            this.keystoreFilename=fname;
        } else {
            throw new StoreException("keystore loading file breaks "+fname+" no readable");
        }
        
    }
    
    
    private boolean loadStore(){
        try {
            loadStore();
            return true;
        }catch(Exception e) {
            printf("loadStore()",1,"ERROR: load Store with exception - "+e.toString(),e);
            return false;
        }
    }
    
    private void createKeyStoreFile(StoreType certType, String fname) {
        try {
            java.security.KeyStore ks = java.security.KeyStore.getInstance(certType.name()); //"JKS");
                                   ks.load(null, null);
                                   
            java.io.FileOutputStream output = new java.io.FileOutputStream(fname);
                                   
                                   ks.store(output,  this.getPassword() );
                                   
                                   output.close();

                                   
        } catch (Exception ex) {
            printf("createKeyStoreFile(CertificateType certType, String fname)",2,"ERROR: KeyStore create failed - "+ex.toString() );
        }
        
    }
    
    private String testKeyFile ="";
    public  void   setTestKeyStoreFile(String fname) throws FileNotFoundException{ setTestKeyStoreFile(new FileInputStream(fname));}
    public  void   setTestKeyStoreFile(FileInputStream in){
        try {
            Certificate cert = (getCertificateFactory()).generateCertificate(in);
        } catch (CertificateException ex) {
        }
        finally{
            try {
                in.close();
            } catch (IOException ex) {
            }
        }
    }

    private String alias = "test";
    public String  getAlias(){ return alias; }
    public String  setAlias(String alias){ this.alias=alias; return getAlias(); }


    //FileInputStream fIn = new FileInputStream(keystoreFilename);
    //KeyStore keystore = KeyStore.getInstance("JKS");

    //keystore.load(fIn, password);
    private java.security.KeyStore keystore=null;
    public Certificate getCertificate() {
        try {
            return keystore.getCertificate(alias);
        } catch (KeyStoreException ex) {
            throw new StoreException("no certificate over alias "+alias);
        }
        
    }
    
}
