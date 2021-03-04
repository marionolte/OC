/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.file;

import general.Version;
import io.crypt.Crypt;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author SuMario
 */
public class SecDBFile extends Version{
    final private WriteFile db; 
    private final Crypt  crypt;
    
    final private HashMap<String, HashMap<String,ArrayList>> ind = new HashMap();

    public SecDBFile(ReadFile dbFile) {
         db= dbFile.getWriteFile();
         
         crypt=new Crypt();
         crypt.setCustomKey(db.getFQDNFileName());
         setCryptLevel(1);
    
         for (String s: db.getZipIndex()) {
             //System.out.println("s:"+s+":");
             String[] sp = s.split("/");
             System.out.println("s:"+sp[0]+":"+sp[1]+":"+sp[sp.length-1]+":");
                      sp = getIndex( new String[]{ sp[0],sp[1],sp[sp.length-1] } );
                      ArrayList ar = getArray(sp[0],sp[1]);
                                ar.add(sp[sp.length-1]);
         }
    }
    
    
    public void setCryptLevel(int level) {
        this.crypt.setCryptLevel( (level>0)?level:0 );
    }
    
    private String[] getIndex(String[] sp) {
        String md = crypt.getMD5(sp[sp.length-1]);
        String aa = md.substring(0, 2).toUpperCase();
        String ee = md.substring(md.length()-2).toUpperCase();
        if ( ! sp[0].isEmpty() ) { aa=sp[0]; }
        if ( ! sp[1].isEmpty() ) { ee=sp[1]; }
        System.out.println(":"+aa+"<->"+ee+"<->"+md+":");
        return new String[]{ aa,ee,md };
    }
    
    private ArrayList getArray(String ind1, String ind2 ) {
         HashMap<String, ArrayList> map = ind.get(ind1);
         if ( map == null ) {  map=new HashMap();  ind.put(ind1, map); }
         ArrayList<String> ar = map.get(ind2);
         if (  ar == null ) { ar=new ArrayList(); map.put(ind2, ar); }
         return ar;
    }
    
    
    public static void main(String[] args) {
        SecDBFile sdb=new SecDBFile(new ReadFile(args[0]));
    }
    
}
