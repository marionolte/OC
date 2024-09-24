/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.io.hcl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 *
 * @author SuMario
 */
public class HCLArray extends HCLMain {
    private String _begin="[";
    private String _end="]";
            
    private ArrayList<HCLList> list = new ArrayList(); 
    private HashMap<String,HCLMap>     map = new HashMap();
    
            
    public HCLArray() {
      
        _begin="[";
          _end="]";
          par = Pattern.compile(_begin+"|"+_end);
        //list.add(new HCLList());
        //list.get(0).setName("HCLArray"+io.crypt.Crypt.getRandomID());
    }  
    
    @Override
    public void parse(StringBuilder sw) {
        
    }
}
