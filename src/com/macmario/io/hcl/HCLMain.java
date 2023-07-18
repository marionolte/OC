/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.io.hcl;

import com.macmario.io.buffer.MapList;
import java.util.regex.Pattern;

/**
 *
 * @author SuMario
 */
abstract class HCLMain extends MapList{
     Pattern par ; 
     
     HCLMain(){
         par=null;
         _begin=null;
         _end=null;
     }
     
     String _begin;
     String _end;
     
     
     abstract public void parse(StringBuilder sw);
     
}
