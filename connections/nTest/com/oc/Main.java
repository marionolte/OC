package com.oc;

import com.oc.io.RunnableT;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Properties;


public class Main extends RunnableT {
    int    port=6789;
    String host="localhost";
    boolean rule=   false;   // "false:client; true:server";
    boolean proto= false;    // "false:tcp true:udp";
    StringBuilder msg = new StringBuilder();
    Properties prop  = new Properties();
    void parseArgs(String[] ar) throws IOException {
        if ( ar != null ) {
           for ( int i=0; i<ar.length; i++ ) {
               if      ( ar[i].equals("-server")  ) { rule=true;    }
               else if ( ar[i].equals("-server")  ) { rule=false;   }
               else if ( ar[i].equals("-tcp")     ) { proto=false;  }
               else if ( ar[i].equals("-udp")     ) { proto=true;   }
               else if ( ar[i].equals("-h")       ) { host=ar[++i]; }
               else if ( ar[i].equals("-p")       ) { port=Integer.parseInt(ar[++i]); }
               else if ( ar[i].equals("-conf")    ) { prop.load(new java.io.FileReader(ar[++i])); }
               else {
                   if (msg.length()>0 ) { msg.append("\t"); }
                   msg.append(ar[i]);
               }
           }
        }
    }

    void runAsServer() {
       Server s;

        try {
            s = new Server(proto, host, port, msg);
            s.start();
            while( s.isRunning() ) {
                sleep(300L);
            }
        } catch (Exception e) {
            throw new RuntimeException( "run server fail "+e.getMessage(),e);
        }
    }

    void runAsClient() {

    }

    public static void main(String[] args) throws Exception{
        Main m = new Main();
             m.parseArgs(args);
             if ( m.rule ) { m.runAsServer(); }  else { m.runAsClient(); }

	// write your code here
    }

    public StringBuilder readOut(String file) {
        StringBuilder sb= new StringBuilder();
        try{
            String line;
            sb= new StringBuilder();
            BufferedReader rb = new java.io.BufferedReader( new java.io.FileReader(file) );
            do {
                line=rb.readLine();
                if ( line != null ) {
                    sb.append(line).append("\n");
                }
            } while ( line != null );

            if( sb.length() > 0) { sb.setLength(sb.length()-1); }   // remove last \n

        } catch (Exception e){ }

        return sb;
    }

    @Override
    public void run() {

    }
}
