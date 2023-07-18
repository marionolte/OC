/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.comm.ssh;

/**
 *
 * @author SuMario
 */
public class RemoteConsumer  extends Thread
{
    int posy = 0;
    int posx = 0;
    int x, y;
    char[][] lines = new char[y][];
    
    TerminalDialog dia;
    
    RemoteConsumer(int x, int y, TerminalDialog dia) {
        this.x=x;
        this.y=y;
        this.dia=dia;
    }
                        
    
    private void addText(byte[] data, int len)
    {
        for (int i = 0; i < len; i++) {
            char c = (char) (data[i] & 0xff);

            if (c == 8) // Backspace, VERASE
            {
                    if (posx < 0) continue;
                    posx--;
                    continue;
            }

            if (c == '\r') {
                posx = 0;
                continue;
            }

            if (c == '\n') {
                posy++;
                if (posy >= y) {
                        for (int k = 1; k < y; k++)  lines[k - 1] = lines[k];
                        posy--;
                        lines[y - 1] = new char[x];
                        for (int k = 0; k < x; k++) lines[y - 1][k] = ' ';
                }
                continue;
            }

            if (c < 32) { continue; }

            if (posx >= x) {
                    posx = 0;
                    posy++;
                    if (posy >= y) {
                            posy--;
                            for (int k = 1; k < y; k++)  lines[k - 1] = lines[k];
                            lines[y - 1] = new char[x];
                            for (int k = 0; k < x; k++)  lines[y - 1][k] = ' ';
                    }
            }

            if (lines[posy] == null) {
                    lines[posy] = new char[x];
                    for (int k = 0; k < x; k++) lines[posy][k] = ' ';
            }

            lines[posy][posx] = c;
            posx++;
        }

        StringBuffer sb = new StringBuffer(x * y);

        for (int i = 0; i < lines.length; i++) {
            if (i != 0) sb.append('\n');

            if (lines[i] != null) {  sb.append(lines[i]); }

        }
        
        dia.setContent(sb.toString());
    }

    @Override
    public void run() {
        byte[] buff = new byte[8192];

        try {
            while (true) {
                int len = dia.in.read(buff);
                if (len == -1) return;
                
                addText(buff, len);
            }
        } catch (Exception e) { }
    }

    
}
