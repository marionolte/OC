/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.io.hcl;

import java.util.regex.Pattern;

/**
 *
 * @author SuMario
 */
public class HCLList  extends HCLMain {

    public HCLList(){
         _begin="{";
         _end="}";
         par = Pattern.compile(_begin+"|"+_end);
    }
    @Override
    public void parse(StringBuilder sw) {
    }
    
}
