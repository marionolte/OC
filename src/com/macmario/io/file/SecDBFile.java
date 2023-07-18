/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.io.file;

import com.macmario.general.Version;
import com.macmario.io.crypt.Crypt;
import java.io.ByteArrayInputStream;
import java.util.HashMap;

/**
 *
 * @author SuMario
 */
public class SecDBFile extends Version{
    final private WriteFile db; 
    private final Crypt  crypt;
    
    final private HashMap<String, HashMap<String,HashMap<String,String>>> ind = new HashMap();

    public SecDBFile(ReadFile dbFile) {
         db= dbFile.getWriteFile();
         
         crypt=new Crypt();
         crypt.setCustomKey(db.getFQDNFileName());
         setCryptLevel(2);
         
         for (String s: db.getZipIndex()) {
             //System.out.println("s:"+s+":");
             String[] sp = s.split("/");
             //System.out.println("s:"+sp[0]+":"+sp[1]+":"+sp[sp.length-1]+":");
                      sp = getIndex( new String[]{ sp[0],sp[1],sp[sp.length-1] } );
                      HashMap<String,String> ar = getArray(sp[0],sp[1]);
                                ar.put(sp[sp.length-1], "");
         }
    }
    
    
    public void setCryptLevel(int level) {
        this.crypt.setCryptLevel( (level>0)?level:0 );
    }
    
    public  String[] getIndex(String s ) {  return getIndex( new String[]{ "","",s} ); }
    private String[] getIndex(String[] sp) {
        String md = crypt.getMD5(sp[sp.length-1]);
        String aa = md.substring(0, 2).toUpperCase();
        String ee = md.substring(md.length()-2).toUpperCase();
        if ( ! sp[0].isEmpty() ) { aa=sp[0]; }
        if ( ! sp[1].isEmpty() ) { ee=sp[1]; }
        //System.out.println(":"+aa+"<->"+ee+"<->"+md+":");
        return new String[]{ aa,ee,md };
    }
    
    private HashMap<String,String> getArray(String ind1, String ind2 ) {
         HashMap<String, HashMap<String,String>> map = ind.get(ind1);
         if ( map == null ) {  map=new HashMap();  ind.put(ind1, map); }
         HashMap<String,String> ar = map.get(ind2);
         if (  ar == null ) { 
                ar=new HashMap<String,String>(); 
                map.put(ind2, ar); 
         }
         return ar;
    }
    
    synchronized private String getSecKey(String key) {      
        return this.crypt.getCrypted(key);
    }
    
    synchronized public ByteArrayInputStream getUnSecure(String val) {
        return new ByteArrayInputStream(this.crypt.getUnCryptedByte(val));
    }
    
    synchronized public ByteArrayInputStream getSecure(String val) {
        return new ByteArrayInputStream(this.crypt.getCryptedByte(val));
    }
    synchronized private String getUnSecKey(String key) {      
        return this.crypt.getUnCrypted(key);
    }
    
    public void update(String key, String value) { add(key,value); }
    public void add(String key,String value) {
         if ( key   == null ) { return; }
         if ( value == null ) { value=""; }
         String[] fp = key.split("/");
         if ( fp.length <= 2 ) {
              fp = new String[]{ "","", key };
         }
         String[] sp = getIndex( new String[]{ fp[0],fp[1], getSecKey(fp[ 2 ]) } );
         HashMap<String,String> ar = getArray(sp[0],sp[1]);
         String s = this.crypt.getCrypted(value);
         ar.put(sp[2],s);
         db.addToZip(sp[0]+"/"+sp[1]+"/"+sp[2], getSecure(value));
    }
    
    
    public String getShortValue(String key) {
        ByteArrayInputStream ar = get(key);
        byte[] b = new byte[ar.available()];
        ar.read(b,0,b.length);
        return new String(b);
    }
    
    public ByteArrayInputStream get(String key) {
        String[] fp = key.split("/");
         if ( fp.length <= 2 ) {
              fp = new String[]{ "","", key };
         }
        String[] sp = getIndex(new String[]{ fp[0],fp[1], getSecKey(fp[2] ) });
        HashMap<String,String> ar = getArray(sp[0],sp[1]);
        //System.out.println(ar);
        return (ByteArrayInputStream) db.getFileFromZip(sp[0]+"/"+sp[1]+"/"+sp[2]);
    }
    
    public boolean remove(String key) {
        String[] fp = key.split("/");
         if ( fp.length <= 2 ) {
              fp = new String[]{ "","", key };
         }
        String[] sp = getIndex(new String[]{ fp[0],fp[1], getSecKey(fp[2] ) });
        HashMap<String,String> ar = getArray(sp[0],sp[1]);
                               ar.remove(sp[2]);
        return db.removeFromZip(sp[0]+"/"+sp[1]+"/"+sp[2]);
    } 
    
    public static void main(String[] args) {
        SecDBFile sdb=new SecDBFile(new ReadFile(args[0]));
        
        sdb.add("myhost", "abcd1234!#%");
        sdb.add("nohost", "fdb17");
        sdb.add("33/AD/std","nixda7");
        System.out.println(":"+sdb.getUnSecKey(sdb.getShortValue("myhost"))+":");
        System.out.println(":"+sdb.getUnSecKey(sdb.getShortValue("33/AD/std"))+":");
        sdb.remove("nohost");
    }
    
}
