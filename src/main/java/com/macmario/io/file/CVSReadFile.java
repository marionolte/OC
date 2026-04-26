/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.io.file;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * @author SuMario
 */
public class CVSReadFile extends ReadFile {
    private final String SEPA;
    private final String LF;
    
    public CVSReadFile(String dir, String file, String delimit, String LF){
        this(new File(dir+File.separator+file), delimit, LF);
    }
    public CVSReadFile(String dir, String file){
        this(dir,file,",","\n");
    }

    public CVSReadFile(String nfile){
        this(new File(nfile),",","\n");
    }

    public CVSReadFile(File file) {
        this(file,",","\n");
    }
    
    public CVSReadFile(File file, String delimit, String LF) {
        super(file);
        this.SEPA=delimit;
        this.LF=LF;
    }
    

    public ArrayList<HashMap<String,String>> getCSV(boolean withHeader) {
        ArrayList<HashMap<String,String>> ar = new ArrayList();
        
        String[] sp = readOut().toString().split(LF);
        String[] mp = sp[0].split(SEPA);
        HashMap<String,String> header = new HashMap();
        int len=mp.length;
        for ( int i=0; i<mp.length; i++ ) {
             header.put(mp[i], mp[i]);
        }
        if (withHeader) ar.add(header);
        for( int i=1; i<sp.length; i++) {
            mp = sp[i].split(SEPA);
            if ( mp.length < len ) {
                mp = (sp[i]+sp[++i]).split(SEPA);
            }
            HashMap<String,String> val = new HashMap();
            Iterator<String> it=header.keySet().iterator();
            int c = 0;
            while(it.hasNext()) {
                String k=it.next();
                if ( mp.length> c ) {
                    val.put(k, mp[c]);
                } else {
                    val.put(k, "NULL");
                }
                c++;
            }
            ar.add(val);
        }       
        
        return ar;
    }
    
    public ArrayList<HashMap<String,String>> getCSV(){ return getCSV(true); }
    public ArrayList<HashMap<String,String>> getCSVWithoutHeader(){ return getCSV(false); }
        
    
    public static void main(String[] args) {
        ArrayList<HashMap<String,String>> mp=(new CVSReadFile(args[0])).getCSV(true);
        while( mp.size() > 0 ){
            HashMap<String,String> ha = mp.remove(0);
            
        }
        
    }
    
}
