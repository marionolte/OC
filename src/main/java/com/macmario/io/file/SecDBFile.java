/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.macmario.io.file;

import com.macmario.general.Version;
import com.macmario.io.crypt.KeePass;

/**
 *
 * @author SuMario
 */
public class SecDBFile extends Version {
    
    private final SecFile sec;
    private final KeePass keepass;
    private final ReadFile db;
    private boolean updated=false;
    
    public SecDBFile(String db, SecFile sec) { this( new ReadFile(db), sec); }
    public SecDBFile(ReadFile db, SecFile sec) {
        this.sec=sec;
        this.db=db;
        this.keepass = KeePass.open(db.filer, sec.readOut().toString() );
    }
    
    
    public boolean save() {
        keepass.save(db.filer, sec.readOut().toString() );
        updated=false;
        return true;
    }
    
    public boolean save(String newPass) {
        if ( isUpdated() ) { save(); }
        keepass.save(db.filer, newPass);
        return sec.replace(newPass);
    }
    
    public boolean close() {
         if ( isUpdated() ) { save(); }
         return true;
    }
    
    public boolean isUpdated() { return this.updated; }
    
    public static void main(String[] args) throws Exception {
        SecFile s=null;
        String  db=null;
        
        for ( int i = 0; i<args.length; i++ ) {
            if ( args[i].equals("-d") ) { debug++; }
            else if ( args[i].equals("-j") ) { s=new SecFile(args[++i]); }
            else { db=args[i]; }
        }
        
        SecDBFile sdb = new SecDBFile(db,s);
        
                  sdb.close();
    }
}
