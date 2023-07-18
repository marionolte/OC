/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.macmario.io.db;

import com.macmario.general.Version;
import com.macmario.io.account.User;
import com.macmario.io.crypt.Crypt;
import com.macmario.io.file.ReadFile;
import com.macmario.io.file.SecFile;
import de.slackspace.openkeepass.KeePassDatabase;
import de.slackspace.openkeepass.domain.Entry;
import de.slackspace.openkeepass.domain.EntryBuilder;
import de.slackspace.openkeepass.domain.Group;
import de.slackspace.openkeepass.domain.GroupBuilder;
import de.slackspace.openkeepass.domain.KeePassFile;
import de.slackspace.openkeepass.domain.KeePassFileBuilder;
import java.io.File;
import java.util.HashMap;
import java.util.Properties;

/**
 *
 * @author SuMario
 */
    public class SecDb extends Version{
    final HashMap<String, String> map;
    final User user;
    private final Crypt  crypt;
    final private String use="usage: [<-create <file>> |<-update <file>> |<-read <file>>] [-masterpw <master password file>] [-force]";
    
    public SecDb(String[] ar) {
        
        crypt = new Crypt();
        map = com.macmario.io.lib.IOLib.scanner(ar, use);
        
        Properties p=new Properties();
        String v = getValue("-masterpw");  
        if ( v == null ) { v="masterpass"; }
        SecFile sec = new SecFile(v);
        if ( sec.isReadableFile()) { 
            p=sec.getProperties();
        }    
        if ( p.getProperty("USER")     == null ) p.setProperty("USER", getUserKey()); 
        if ( p.getProperty("PASSWORD") == null ) p.setProperty("PASSWORD",getJarMD5());            
        
        user = new User(p.getProperty("USER",getUserKey()), p.getProperty("PASSWORD",getJarMD5())){};

        //System.out.println("map =>"+map.toString());
    }
    private String getValue(String k) {
        String kdef="_default_"+k;
        String vdef=map.get(kdef);
        String v   =map.get(k);
        return ( v != null && ! v.equals(vdef))?v:"";
    }
    public void run() {
        System.out.println("create? =>"+getValue("-create")+"<- ("+(! getValue("-create").isEmpty())+")");
        System.out.println("read?   =>"+getValue("-read"  )+"<- ("+(! getValue("-read"  ).isEmpty())+")");
        System.out.println("update? =>"+getValue("-update")+"<- ("+(! getValue("-update").isEmpty())+")");
        if      ( ! getValue("-create").isEmpty()  ) { runCreate(); }
        else if ( ! getValue("-update").isEmpty()  ) { runUpdate(); }
        else if ( ! getValue("-read").isEmpty()  ) { runRead(); }
        else {
            System.out.println(use);
        }
    }
    
    private void runCreate(){
        System.out.println("masterkey:"+user.getPassword()+":");
        boolean b = ( ! getValue("-force").isEmpty() && getValue("-force").equals("true") );
        
        ReadFile f = new ReadFile( getValue("-create") );
        if ( f.isReadableFile() ) {
            if ( ! b ) {
              System.out.println("ERROR: DB "+f.getFQDNFileName()+" already exist - use option -force to complete");
              throw new RuntimeException("DB already exist - use option -force ");
            }
            System.out.println("INFO: recreate "+f.getFQDNFileName());
            f.getWriteFile().delete();
        }
        
        System.out.println("INFO: like create kdbx "+f.getFQDNFileName());
        //KeePassFile kdb = getOpenSecDB(map.get("-create"), user.getPassword(), map.get("-keyFile") );
        GroupBuilder root = new GroupBuilder();
        EntryBuilder et = new EntryBuilder("TopEntry").username(user.getUsername()).password(user.getPassword());
        root.addGroup(new GroupBuilder("Top").addEntry( et.build() ).build() );
        KeePassFile kdb = new KeePassFileBuilder("writingDB").addTopGroups(root.build()).build();
        System.out.println("INFO: kdb "+kdb.toString()+"||"+kdb.getMeta().toString());
        
        KeePassDatabase.write(kdb, user.getPassword() , f.getFQDNFileName());
        
        //KeePassDatabase.write(kdb, user.getPassword() , f.getWriteFile().getFileOutStream());
      
    }
    
    public void runUpdate(){
        
    }
    public void runRead(){
        ReadFile f = new ReadFile( getValue("-read") );
        System.out.println("masterkey:"+user.getPassword()+":");
        KeePassFile db = getOpenSecDB(getValue("-read"), user.getPassword(), f.getFQDNFileName() );
        Group g = db.getGroupByName(map.get("group") );
        if ( g != null ) {
            boolean  k=( map.get("key") != null );
            String key=((k)?(map.get("key")):(map.get("value")));
            for ( Entry e : g.getEntries() ) {
                if ( k && e.getTitle().equals(key) ) { System.out.println(e.getPassword()); }
                else if ( ! k ) {
                    System.out.println("e:"+e);
                }
            }
        }
    }
    
    private KeePassFile getOpenSecDB(String fname, String mpw, String keyFile) {
        if ( keyFile != null ) {
            ReadFile fa = new ReadFile(keyFile);
            if ( fa.isReadableFile() ) {
                KeePassFile db = KeePassDatabase.getInstance(fname).openDatabase(new File(keyFile));
                return db;
            }
        }
        KeePassFile db = KeePassDatabase.getInstance(fname).openDatabase(mpw);
        return db;
    }
    
    public static void main(String[] args) {
        SecDb sdb = new SecDb(args);
              sdb.run();
    }
    
}
