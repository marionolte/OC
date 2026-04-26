/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.tcp;

/**
 *
 * @author SuMario
 */
public enum StoreType {
    /**
     * No certificate to be used (and so no SSL and no Start TLS).
     */
    NO_CERTIFICATE,
    /**
     * Use a newly created Self Signed Certificate.
     */
    SELF_SIGNED_CERTIFICATE,
    /**
     * Use an existing JKS key store.
     */
    JKS,
    /**
     * Use an existing JCEKS key store.
     */
    JCEKS,
    /**
     * Use an existing PKCS#11 key store.
     */
    PKCS11,
    /**
     * Use an existing PKCS#12 key store.
     */
    PKCS12;

    public static StoreType getInstance(String typ) {
        if      ( typ.toLowerCase().matches("jks")                     ) { return StoreType.JKS;                     }
        else if ( typ.toLowerCase().matches("jceks")                   ) { return StoreType.JCEKS;                   }
        else if ( typ.toLowerCase().matches("pkcs11")                  ) { return StoreType.PKCS11;                  }
        else if ( typ.toLowerCase().matches("pkcs12")                  ) { return StoreType.PKCS12;                  }
        else if ( typ.toLowerCase().matches("self_signed_certificate") ) { return StoreType.SELF_SIGNED_CERTIFICATE; }
        else if ( typ.toLowerCase().matches("no_certificate")          ) { return StoreType.NO_CERTIFICATE;          }
        
        return StoreType.JKS;
    }

    static boolean isValidTyp(String typ) {
        typ=typ.toUpperCase();
        if ( typ.matches("JKS") || typ.matches("JCEKS") || typ.matches("PKCS11") || typ.matches("PKCS12") ) { return true;}
        return false;
    }
}
