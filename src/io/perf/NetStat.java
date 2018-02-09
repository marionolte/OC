/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.perf;

import general.Version;
import java.io.InputStream;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author SuMario
 */
class NetStat extends Version{
    
    
    static {
        
    }

    static StringBuilder outline(StringBuilder sw) {
        final String func="io.perf.NetStat::outline(StringBuilder sw)";
        StringBuilder ret = new StringBuilder();
        printf(func,4,"sw"+sw.toString());
        Matcher ma = Pattern.compile("tcp[4,6]|udp[4,6]|tcp|udp").matcher(sw.toString());
        printf(func,2,"matcher find:"+ma.find());
        int i = 0;
        while(ma.find(i)) {
            int j=(i>ma.group().length())?i-ma.group().length():i;
            for (String line: sw.substring(j,ma.start()).trim().split("\n") ) {
              printf(func,5,ma.group()+" find >|"+line+"|<");
              if ( line.startsWith(ma.group())) {
                printf(func,3,"line:"+line+":");
                String[] sp = Pattern.compile("\\s+").matcher(line).replaceAll(" ").split(" ");
                ConnStat cs = new ConnStat(sp);
                printf(func,2,"sp 3:"+sp[3]);
              }  
            }
            i=ma.end();
        }
    
        return ret;
    }
    
    static private String execReadToString(String execCommand) throws java.io.IOException {
            Process proc = Runtime.getRuntime().exec(execCommand);
            try (InputStream stream = proc.getInputStream()) {
                try (Scanner s = new Scanner(stream).useDelimiter("\\A")) {
                    return s.hasNext() ? s.next().trim() : "";
                }
            }
        }

    
    public static void main(String[] args) throws Exception{
        debug=3;
        System.out.println( outline( new StringBuilder(execReadToString("netstat -an"))));
    }
    
    
    static class ConnStat extends Version{

        private boolean tcp=false;
        private boolean udp=false;
        private boolean tcp6=false;
        private boolean tcp4=false;
        private boolean udp6=false;
        private boolean udp4=false;
        private long recvPackets=0L;
        private long sendPackets=0L;
        private String connstate="NOP";
        private String localIP;
        private String localPort;
        private String remIP;
        private String remPort;
        private boolean listener=false;
        
        ConnStat(String[] ar) {
            final String func="io.perf.NetStat::ConnStat(String[] ar)::ConnStat(String[] ar)";
            StringBuilder sw = new StringBuilder();
            for(String s : ar) { sw.append(s).append("|"); }
            printf(func,2, ar.toString()+" =>"+sw.toString());
            for ( int i=0; i<ar.length; i++ ) {
                if      (i == 0 &&  ar[i].toLowerCase().startsWith("tcp") ) { setTcp(ar[i]); }
                else if (i == 0 &&  ar[i].toLowerCase().startsWith("udp") ) { setUdp(ar[i]); }
                else if (i == 1                                         ) { this.recvPackets=Long.parseLong(ar[i]);}
                else if (i == 2                                         ) { this.sendPackets=Long.parseLong(ar[i]);}
                else if (i == 3                                         ) { String[] sp =getIP(ar[i]); this.localIP=sp[0]; this.localPort=sp[1];  }
                else if (i == 4                                         ) { String[] sp =getIP(ar[i]);   this.remIP=sp[0];   this.remPort=sp[1];  }
                else if (i == 5                                         ) { this.connstate=ar[i]; }
                else if (i == 6                                         ) { setConnTyp(ar[i]);}
                
            }
        }
    
        private String[] getIP(String inf) {
            String[] ret = new String[] { "", ""};
            String[] ip = inf.split("[:,\\.]");
            ret[0]=inf.substring(0, inf.length()-ip[ip.length-1].length()-1);
            ret[1]=ip[ip.length-1];
                  System.out.println("ip[0]="+ret[0]+"::"+ret[1]);
            return ret;
        }
        void setTcp(String inf) {
             this.tcp=true; this.udp=false;
             this.tcp6=(inf.contains("6")); this.tcp4=!this.tcp6;
        }
        
        void setUdp(String inf) {
             this.udp=true; this.tcp=false;
             this.udp6=(inf.contains("6")); this.udp4=!this.udp6;
        }
        
        void setConnTyp(String inf) {
            switch(inf) {
                case "LISTEN"       : this.listener=true; break;
                case "ESTABLISHED"  :  break;
                default : break;
            }
        }
    }
}
