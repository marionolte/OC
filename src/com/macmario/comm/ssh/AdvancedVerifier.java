/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.comm.ssh;

import com.trilead.ssh2.KnownHosts;
import com.trilead.ssh2.ServerHostKeyVerifier;
import java.io.File;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 *
 * @author SuMario
 *       * This ServerHostKeyVerifier asks the user on how to proceed if a key cannot be found
         * in the in-memory database.
         *
 */
public class AdvancedVerifier implements ServerHostKeyVerifier {
        static public KnownHosts database; 
        static String knownHostPath;
        
        @Override
        public boolean verifyServerHostKey( String hostname, int port, String serverHostKeyAlgorithm, byte[] serverHostKey) throws Exception
        {
            final String host = hostname;
            final String algo = serverHostKeyAlgorithm;

            String message;

            /* Check database */

            int result = database.verifyHostkey(hostname, serverHostKeyAlgorithm, serverHostKey);

            switch (result) {
                case KnownHosts.HOSTKEY_IS_OK: return true;

                case KnownHosts.HOSTKEY_IS_NEW:
                                message = "Do you want to accept the hostkey (type " + algo + ") from " + host + " ?\n";
                                break;

                case KnownHosts.HOSTKEY_HAS_CHANGED:
                                message = "WARNING! Hostkey for " + host + " has changed!\nAccept anyway?\n";
                                break;

                default:   throw new IllegalStateException();
            }

            /* Include the fingerprints in the message */

            String hexFingerprint = KnownHosts.createHexFingerprint(serverHostKeyAlgorithm, serverHostKey);
            String bubblebabbleFingerprint = KnownHosts.createBubblebabbleFingerprint(serverHostKeyAlgorithm, serverHostKey);

            message += "Hex Fingerprint: " + hexFingerprint + "\nBubblebabble Fingerprint: " + bubblebabbleFingerprint;

            /* Now ask the user */
            int choice=-1;
            //if ( loginFrame != null ) {
            //     choice = JOptionPane.showConfirmDialog(loginFrame, message);
            //}else {
            //     System.out.println("confirm host");
            //}  
            choice = JOptionPane.showConfirmDialog(new JFrame(), message);
            if (choice == JOptionPane.YES_OPTION) { // 0
                System.out.println("YES_OPTION:"+JOptionPane.YES_OPTION);
                /* Be really paranoid. We use a hashed hostname entry */

                String hashedHostname = KnownHosts.createHashedHostname(hostname);

                /* Add the hostkey to the in-memory database */

                database.addHostkey(new String[] { hashedHostname }, serverHostKeyAlgorithm, serverHostKey);

                /* Also try to add the key to a known_host file */

                try {
                    KnownHosts.addHostkeyToFile(new File(knownHostPath), new String[] { hashedHostname }, serverHostKeyAlgorithm, serverHostKey);
                } catch (java.io.IOException ignore) { }

                return true;

            } 
            if (choice == JOptionPane.CANCEL_OPTION) {
                                throw new Exception("The user aborted the server hostkey verification.");
            }

            //System.out.println("return false");
            return false;
        }  
}
