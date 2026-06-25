/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.comm.ssh;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.InteractiveCallback;
import com.trilead.ssh2.Session;
import java.io.File;
import com.macmario.net.ssh.SSHshell;

/**
 *
 * @author SuMario
 * 
 *
         * The SSH-2 connection is established in this thread.
         * If we would not use a separate thread (e.g., put this code in
         * the event handler of the "Login" button) then the GUI would not
         * be responsive (missing window repaints if you move the window etc.)
 */
public class ConnectionThread  extends Thread {
        String hostname;
        String username;
        String password="";
        int    port=22;
        String host;
        TerminalDialog td=null;
        SSHshell ssh=null;
        public ConnectionThread(String hostname, String username, String password )
        {
                        this.hostname = hostname;
                        this.username = username;
                        this.password = password;
                        this.host=hostname;
        }
        public ConnectionThread(SSHshell ssh, String hostname, String username, String password, int port)
        {
            this(hostname,username,password);
            this.ssh=ssh;
            this.port=(port > 0 && port < 64*1024-1)?port:22;
            if (port != 22 ){ host=hostname+":"+port ;}
        }
        
                @Override
                public void run()
                {
                        Connection conn = new Connection(hostname,port);

                        try
                        {
                                /*
                                 * 
                                 * CONNECT AND VERIFY SERVER HOST KEY (with callback)
                                 * 
                                 */

                                String[] hostkeyAlgos = ssh.database.getPreferredServerHostkeyAlgorithmOrder(hostname);

                                if (hostkeyAlgos != null)
                                        conn.setServerHostKeyAlgorithms(hostkeyAlgos);

                                AdvancedVerifier.database=ssh.database; AdvancedVerifier.knownHostPath=ssh.knownHostPath;
                                conn.connect(new AdvancedVerifier());
                                    /*
                                 * 
                                 * AUTHENTICATION PHASE
                                 * 
                                 */

                                boolean enableKeyboardInteractive = true;
                                boolean enableDSA = true;
                                boolean enableRSA = true;

                                String lastError = null;

                                while (true)
                                {
                                     if ((enableDSA || enableRSA) && conn.isAuthMethodAvailable(username, "publickey"))
                                        {
                                                if (enableDSA)
                                                {
                                                        File key = new File(ssh.idDSAPath);

                                                        if (key.exists())
                                                        {
                                                                EnterSomethingDialog esd = new EnterSomethingDialog("DSA Authentication",
                                                                                new String[] { lastError, "Enter DSA private key password:" }, true);
                                                                esd.setVisible(true);

                                                                boolean res = conn.authenticateWithPublicKey(username, key, esd.answer);

                                                                if (res == true)
                                                                        break;

                                                                lastError = "DSA authentication failed.";
                                                        }
                                                        enableDSA = false; // do not try again
                                                
                                                }
                                                
                                                if (enableRSA) {
                                                        File key = new File(ssh.idRSAPath);
                                                        if (key.exists())
                                                        {
                                                                EnterSomethingDialog esd = new EnterSomethingDialog("RSA Authentication",
                                                                                new String[] { lastError, "Enter RSA private key password:" }, true);
                                                                esd.setVisible(true);

                                                                boolean res = conn.authenticateWithPublicKey(username, key, esd.answer);

                                                                if (res == true)
                                                                        break;

                                                                lastError = "RSA authentication failed.";
                                                        }
                                                        enableRSA = false; // do not try again
                                                }

                                                continue;
                                        }

                                        if (enableKeyboardInteractive && conn.isAuthMethodAvailable(username, "keyboard-interactive"))
                                        {
                                                InteractiveLogic il = new InteractiveLogic(lastError); 
                                                boolean res = conn.authenticateWithKeyboardInteractive(username, (InteractiveCallback) il);

                                                if (res == true)
                                                        break;

                                                if (il.getPromptCount() == 0)
                                                {
                                                        // aha. the server announced that it supports "keyboard-interactive", but when
                                                        // we asked for it, it just denied the request without sending us any prompt.
                                                        // That happens with some server versions/configurations.
                                                        // We just disable the "keyboard-interactive" method and notify the user.

                                                        lastError = "Keyboard-interactive does not work.";

                                                        enableKeyboardInteractive = false; // do not try this again
                                                }
                                                else
                                                {
                                                        lastError = "Keyboard-interactive auth failed."; // try again, if possible
                                                }

                                                continue;
                                        }
                                        if (conn.isAuthMethodAvailable(username, "password" ))
                                        {
                                                if ( password == null || password.isEmpty() ) {
                                                    
                                                    final EnterSomethingDialog esd = new EnterSomethingDialog("Password Authentication",
                                                                    new String[] { lastError, "Enter password for " + username }, true);

                                                    esd.setVisible(true);

                                                    if (esd.answer == null)
                                                            throw new java.io.IOException("Login aborted by user");

                                                    password=esd.answer;
                                                    
                                                }    

                                                boolean res = conn.authenticateWithPassword(username, password);

                                                if (res == true)
                                                        break;

                                                lastError = "Password authentication failed."; // try again, if possible

                                                continue;
                                        }

                                        throw new java.io.IOException("No supported authentication methods available.");
                                }
    
                                /*
                                 * 
                                 * AUTHENTICATION OK. DO SOMETHING.
                                 * 
                                 */

                                Session sess = conn.openSession();

                                int x_width = 90;
                                int y_width = 30;

                                sess.requestPTY("dumb", x_width, y_width, 0, 0, null);
                                sess.startShell();

                                td = new TerminalDialog(username + "@" + hostname, sess, x_width, y_width);

                                /* The following call blocks until the dialog has been closed */

                                td.setVisible(true);

                        }
                        catch (java.io.IOException e) {
                                //e.printStackTrace();
                                //JOptionPane.showMessageDialog(loginFrame, "Exception: " + e.getMessage());
                        }

            /*
            * 
            * CLOSE THE CONNECTION.
            * 
            */

            conn.close();

            /*
            * 
            * CLOSE THE LOGIN FRAME - APPLICATION WILL BE EXITED (no more frames)
            * 
            */

           /* Runnable r = new Runnable()
            {
                    private void sleep(long d) { try { Thread.sleep(d); } catch(Exception e){} }
                    public void run() { 
                        while(! td.isClosed() ) {  sleep(300); }
                        .dispose();     
                    }
            };

            SwingUtilities.invokeLater(r); */
        }
                
        public TerminalDialog getTerminalDialog() { return td; }        
}
