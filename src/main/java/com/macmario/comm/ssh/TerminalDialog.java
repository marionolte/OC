/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.comm.ssh;

import com.trilead.ssh2.Session;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.InputStream;
import java.io.OutputStream;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import com.macmario.net.ssh.SSHshell;

/**
 *
 * @author SuMario
 * 
    * TerminalDialog is probably the worst terminal emulator ever written - implementing
    * a real vt100 is left as an exercise to the reader, i.e., to you =)
    *
    
 */
public class TerminalDialog extends JPanel{
                private static final long serialVersionUID = 1L;
                private RemoteConsumer consumer=null;
                private final String title;
                private SSHshell ssh=null;
                
                JPanel botPanel;
                JButton logoffButton;
                JTextArea terminalArea;
                JPanel terminalPanel=null;

                Session sess;
                InputStream in;
                OutputStream out;

                int x, y;

                /**
                 * This thread consumes output from the remote server and displays it in
                 * the terminal window.
                 *
                 */

                public TerminalDialog(String title, SSHshell ssh, int x, int y) {
                    this.title=title;
                    this.ssh=ssh;
                    
                    this.x=x;
                    this.y=y;
                    
                    this.sess=ssh.getSession();
                    this.in=ssh.getOutput();
                    this.out=ssh.getInput();
                    
                    init();
                }

                public TerminalDialog(String title, Session sess, int x, int y) throws java.io.IOException
                {
                        //super(mainFrame, title, true);
                        this.title=title;
                        this.sess = sess;

                        in = sess.getStdout();
                        out = sess.getStdin();

                        this.x = x;
                        this.y = y;
                        
                        init();

                }
                
                private void init() {
                        this.setName(title);
                        
                        consumer = new RemoteConsumer(x,y,this);
                        terminalPanel=new JPanel(new FlowLayout(FlowLayout.LEFT));
                        terminalPanel.setName(title);
                        
                        botPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

                        logoffButton = new JButton("Logout");
                        botPanel.add(logoffButton);

                        logoffButton.addActionListener(new ActionListener()
                        {
                                @Override
                                public void actionPerformed(ActionEvent e)
                                {
                                        /* Dispose the dialog, "setVisible(true)" method will return */
                                    setClosed();    
                                    //dispose();
                                }
                        });

                        Font f = new Font("Monospaced", Font.PLAIN, 16);

                        terminalArea = new JTextArea(y, x);
                        terminalArea.setFont(f);
                        terminalArea.setBackground(Color.BLACK);
                        terminalArea.setForeground(Color.ORANGE);
                        /* This is a hack. We cannot disable the caret,
                         * since setting editable to false also changes
                         * the meaning of the TAB key - and I want to use it in bash.
                         * Again - this is a simple DEMO terminal =)
                         */
                        terminalArea.setCaretColor(Color.BLACK);

                        KeyAdapter kl = new KeyAdapter()
                        {
                                public void keyTyped(KeyEvent e)
                                {
                                        int c = e.getKeyChar();

                                        try
                                        {
                                                out.write(c);
                                        }
                                        catch (java.io.IOException e1)
                                        {
                                        }
                                        e.consume();
                                }
                        };

                        terminalArea.addKeyListener(kl);

                        /*getContentPane().add(terminalArea, BorderLayout.CENTER);
                        getContentPane().add(botPanel, BorderLayout.PAGE_END);

                        setResizable(false);
                        pack();
                        setLocationRelativeTo(parent);*/

                        consumer.start();
                    
                }

                public void setContent(String lines)
                {
                        // setText is thread safe, it does not have to be called from
                        // the Swing GUI thread.
                        terminalArea.setText(lines);
                }
                
                public JPanel getTerminal() { return terminalPanel; }
                
    boolean closed=false;
    public void   setClosed(){  closed=true; 
            terminalArea.setEditable(false); 
            if(consumer != null ) { consumer.stop(); }
            if(ssh      != null ) { ssh.setClosed(); }
    } 
    public boolean isClosed() { return closed; }

    @Override
    public void setVisible(boolean b) { terminalPanel.setVisible(b); super.setVisible(b);}
                
}
