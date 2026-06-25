/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.general;

/**
 *
 * @author SuMario
 */
public class MyVersion extends Version {
    
     public static void main(String[] args) {
         StringBuilder sw=new StringBuilder(Version.mhfile.substring(0, Version.mhfile.length()-4));
                       sw.append("-").append(Version.getFullVersion());
                 if ( Version.isBeta() ) {
                       sw.append(".beta").append(Version.betaVersion);
                       
                 }      
         System.out.println(sw.toString());
    }
}
