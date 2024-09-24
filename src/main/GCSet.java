/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import main.gc.GCSetDefaults;

/**
 *
 * @author SuMario
 */
public class GCSet extends GCSetDefaults{
    
    private GCSet(String[] args) {
        super();
        final String func=getFunc("GCSet(String[] args)");
        for(int i=0; i< args.length; i++ ) {
            if ( args[i].startsWith("-") ) {
                String[] sp = args[i].split("[:,=]");
                if ( isBaseProperty(sp[0].substring(1))  ) {
                   String k= getBaseKey(  args[i].substring(1));   //getBaseKey(sp[0].replaceAll("-", ""));
                   String v= getBaseValue(args[i].substring(1)); // args[i].substring(sp[0].length()) );
                   printf(func,2, "setBaseProperty :"+k+":  with value:"+v+":");
                   setBaseValue(k, v);
                } else if ( isXXProperty(sp[0].replaceAll("-", ""))) {
                    sp = args[i].substring(4).split("=");
                    String k = (sp[0]);  
                    String v = (args[i].substring(4+sp[0].length()+1));
                    printf(func,0, "setXXProperty :"+k+":  with value:"+v+":");
                    setXXProperty(k,v);
                } else {
                    System.out.println("not found :"+sp[0].replaceAll("-", ""));
                }
            } else {
                System.out.println("not handled :"+args[i]);
            }    
        }
    }
    
    @Override
    public String toString(){
        StringBuilder sw = new StringBuilder();
        sw.append(getBaseSet());
        sw.append(getXXSet());
        sw.append(getGCSet());
        return sw.toString();
    }
    public static void main(String[] args) {
        GCSet gc = new GCSet(args);
        System.out.println(gc.toString());
    }
}
