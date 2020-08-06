package com.oc;

import com.oc.io.RunnableT;

public class Server extends RunnableT {
    boolean started;
    boolean proto;
    String host;
    int port;
    StringBuilder msg;

    Server(boolean proto, String host, int port, StringBuilder msg) {
        this.proto= proto;
        this.host = host;
        this.port = port;
        this.msg  = msg;
        this.started=false;
    }


    @Override
    public void run() {
         started=true;


         started=false;
    }


    private class TCPServer extends RunnableT{
        @Override
        public void run() {
            while ( isRunning() ) {

            }
        }
    }

    private class UDPServer extends RunnableT {

        @Override
        public void run() {
            while ( isRunning() ) {

            }
        }
    }
}
