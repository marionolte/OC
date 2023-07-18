/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.macmario.net.tcp;

import com.macmario.general.Version;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.util.ArrayList;


/**
 *
 * @author SuMario
 */
public class Store extends Version{

    public Store() {
        try {
            cf = CertificateFactory.getInstance("X.509");
        } catch (CertificateException ex) {
            throw new StoreException("could not get instance of a X509 CertificateFactory ");
        }
    }
    private ArrayList mylist = null;
    private FileInputStream is = null;
    private java.security.KeyStore keystore = null ;

    private String  storeFile=System.getProperty("java.home") + "/lib/security/cacerts".replace('/', File.separatorChar);
    public  String  whichStoreFile(){ return storeFile; }
    public  void    setStoreFile(String fname){
        if ( fname.isEmpty() ) {
            throw new StoreException("storepath could not empty");
        }
        File f = new File (fname);
        if ( ! f.canRead() || f.isDirectory() ){
            throw new StoreException("storepath file need to readable - leaving "+storeFile);
        }
        this.storeFile=fname;
        if ( is != null ) {
            try { is.close(); }
            catch(java.io.IOException ie){}
        }
        try {
            is = new FileInputStream(storeFile);
        } catch( java.io.FileNotFoundException ie){}
        if ( !  setKeyStore() ) {
            throw new StoreException("keystore problem:"+keystoreerr);
        }
    }
    private PKIXParameters params=null;
    public  PKIXParameters getPKIXParam() { return params; }
    
    private boolean storeLoaded=false;
    public boolean isStoreLoaded(){ return storeLoaded; }
    public void    disable(){ storeLoaded=false; }
    public void    enable() { storeLoaded=true; }

    public void readCertificate(String fname){
        try {
            readCertificate(new FileInputStream(fname));
        } catch (FileNotFoundException ex) {
            throw new StoreException("file not exist : "+fname);
        }
    }
    public void readCertificate(FileInputStream in){
        try {
            mylist = new ArrayList(); 
            Certificate c = cf.generateCertificate(in);
            mylist.add(c);
            CertPath cp = cf.generateCertPath(mylist);
        } catch (CertificateException ex) { }
    }
    public  ArrayList  getCertList(){ return mylist; }

    public  CertificateFactory getCertificateFactory() {return cf; }
    
    private CertificateFactory cf=null;
    private CertPath           certPath=null;
    public  CertPath getCertPath() { return certPath; }
    
    public  Certificate   getCertificate(String alias){ return null; }
    public  Certificate[] getCertificates() { 
            Certificate[] cr = new Certificate[mylist.size() ];
            for ( int i=0; i<mylist.size(); i++) { cr[i] = (Certificate) mylist.get(i); }
            return cr;
    }
    
    public void loadKeyStore() {
        disable();
        if ( setKeyStore() ) {
            try {

             keystore.load(is, password.toCharArray());
             params = new PKIXParameters(keystore);
             params.setRevocationEnabled(false);

             enable();
            }
            catch(java.io.IOException                     ie){ throw new StoreException("keystore loading breaks with "  +ie.getMessage()); }
            catch(java.security.NoSuchAlgorithmException  ie){ throw new StoreException("keystore load breaks with "     +ie.getMessage()); }
            catch(java.security.cert.CertificateException ie){ throw new StoreException("keystore breaks on certificate "+ie.getMessage()); }
            catch(java.security.KeyStoreException         ie){ throw new StoreException("keystore breaks on store "      +ie.getMessage()); }
            catch(java.security.InvalidAlgorithmParameterException ie){ throw new StoreException("keystore breaks with alg"   +ie.getMessage());}
            
        }
    }


    private String keystoreerr="";
    public boolean setKeyStore(){
        if ( keystore == null ) {
           try {
            keystoreerr="";
            keystore = KeyStore.getInstance(KeyStore.getDefaultType());
           }catch(java.security.KeyStoreException ie){ keystoreerr=ie.getMessage();}
        }
        return ( keystore == null )? false:true;
    }
    public void setKeyStore(String typ){
         if ( typ.isEmpty() ){ typ="JKS";}
         try {
            keystore = KeyStore.getInstance(typ);
         }catch(java.security.KeyStoreException ie){ keystoreerr=ie.getMessage(); }
    }

    public java.security.KeyStore getKeyStore(){ return keystore; }


    private String password="changeit";
    public  void  setPassword(String pw) {
        if ( pw == null || pw.isEmpty() ) {
            this.password="changeit";
            throw new RuntimeException("store password cound not empty - set back to default");
        }
        this.password = pw;
    }
    public char[] getPassword() { return password.toCharArray(); }

    //FileInputStream f = new FileInputStream("CertPath.dat");
    //ObjectInputStream b = new ObjectInputStream(f);
    //CertPath cp = (CertPath) b.readObject();
    public Object readFile(String         fname) throws IOException, ClassNotFoundException{ return readFile( new FileInputStream(fname));}
    public Object readStream( InputStream    in) throws IOException, ClassNotFoundException{ return readFile( createObjectStream(in) );}
    public Object readFile(FileInputStream   in) throws IOException, ClassNotFoundException{ return readFile( createObjectStream(in) );}
    public Object readFile(ObjectInputStream in) throws IOException, ClassNotFoundException{ return in.readObject(); }

    public void   createFile(String fname) throws FileNotFoundException, IOException { createFile( new File(fname)); }
    public void   createFile(File    file) throws FileNotFoundException, IOException { createFile( new FileOutputStream(file)); }
    public void   createFile(FileOutputStream out) throws IOException                { out.flush(); }
    public ObjectInputStream  createObjectStream(InputStream in) throws IOException{ return new ObjectInputStream(in); }


    public void   writeFile(OutputStream out){ }

}
