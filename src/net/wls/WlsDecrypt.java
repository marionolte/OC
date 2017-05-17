/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.wls;

import io.file.ReadDir;

/**
 *
 * @author SuMario
 */
public class WlsDecrypt {

    private WlsDecrypt(WlsDomain wls) {
        
    }
    

    public static void main(String[] args) {
        for ( int i=0; i< args.length; i++){
             ReadDir d = new ReadDir(args[i]);
             
             if ( d.isDirectory() ) {
                  WlsDomain wls = new WlsDomain(d.getDirName());
                            wls.setDomainLocation(d.getFQDNDirName());
                            
                  WlsDecrypt wd = new WlsDecrypt(wls);          
             }
             
        }
    }

    
    
}
