/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.comm.ssh;

/**
 *
 * @author SuMario
 * 
         * The logic that one has to implement if "keyboard-interactive" autentication shall be
         * supported.
         
         
 */
public class InteractiveLogic {
                int promptCount = 0;
                String lastError;

                public InteractiveLogic(String lastError)
                {
                        this.lastError = lastError;
                }

                /* the callback may be invoked several times, depending on how many questions-sets the server sends */

                public String[] replyToChallenge(String name, String instruction, int numPrompts, String[] prompt,
                                boolean[] echo) throws java.io.IOException
                {
                        String[] result = new String[numPrompts];

                        for (int i = 0; i < numPrompts; i++)
                        {
                                /* Often, servers just send empty strings for "name" and "instruction" */

                                String[] content = new String[] { lastError, name, instruction, prompt[i] };

                                if (lastError != null)
                                {
                                        /* show lastError only once */
                                        lastError = null;
                                }

                                EnterSomethingDialog esd = new EnterSomethingDialog("Keyboard Interactive Authentication",
                                                content, !echo[i]);

                                esd.setVisible(true);

                                if (esd.answer == null)
                                        throw new java.io.IOException("Login aborted by user");

                                result[i] = esd.answer;
                                promptCount++;
                        }

                        return result;
                }
                /* We maintain a prompt counter - this enables the detection of situations where the ssh
                 * server is signaling "authentication failed" even though it did not send a single prompt.
                 */

                public int getPromptCount()
                {
                        return promptCount;
                }
}
