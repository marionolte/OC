/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

/**
 *
 * @author SuMario
 */
public class Wls extends MainTask{
    public Wls(String[] args) {
        super();
        super.prop = parseArgs(args);
    }
    
    public static void main(String[] args) {
       Wls w = new Wls(args);   
    }

    
}
