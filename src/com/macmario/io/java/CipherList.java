/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.io.java;

import java.security.Provider;
import java.security.Provider.Service;
import java.security.Security;
import java.util.TreeSet;

/**
 *
 * @author SuMario
 */
public class CipherList {

    public String getAlgorithmList() {
            TreeSet<String> tree = new TreeSet();
            for ( Provider provider : Security.getProviders() ) {
                for ( Service serv : provider.getServices() ) {
                    tree.add(serv.getAlgorithm());
                }
            }
            StringBuilder sw = new StringBuilder();
            for ( String alg : tree ) {
                sw.append(alg).append("\n");
            }
            return sw.toString();
    }
    public static void main(String[] args) {
          System.out.println( (new CipherList()).getAlgorithmList() );
    }
}
