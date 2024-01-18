/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.io.crypt;

/**
 *
 * @author SuMario
 */
public enum PasswordTyp {
    
        EASY, MEDIUM, STRONG;
        
        public static PasswordTyp fromString(String text){
            text=(text!=null)?text.toUpperCase():"";
            switch(text) {
                case "EASY"  : { return EASY;   }
                case "MEDIUM": { return MEDIUM; }
            }            
            return STRONG;
        }
    
}
