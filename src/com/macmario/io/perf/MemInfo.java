/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.io.perf;

import com.macmario.io.buffer.RingBuffer;
import com.macmario.io.thread.RunnableT;

/**
 *
 * @author SuMario
 */
class MemInfo {
    private static final Runtime rt;
    private static final MemRuntime mr;
    static {
        rt = Runtime.getRuntime();
        mr = new MemRuntime();
        mr.start();
    }
    
    static void outLine(){
        System.out.println(mr.outLine());
    }
    
    static private class MemRuntime extends RunnableT{
        long prevTotal = 0L;
        long prevFree  = 0L;
        long total     = 0L;
        long used      = 0L;
        long prevUsed  = 0L;
        long free      = 0L;
        //HashMap<String, String> map = new HashMap<String,String>();
        RingBuffer<String> rbuf = new RingBuffer<String>(100);

        MemRuntime() {
             prevFree = rt.freeMemory();
             total    = rt.totalMemory();
             
        }
        
        final private String lim="__--__";
        @Override
        public void run() {
            final String func=getFunc("run()");
            setRunning();
            printf(func,0,"start");
            long d=System.currentTimeMillis();
            int c =0;
            while( isRunning() ) {
                    //free = getFreeMemory();
                    free = getMem(); 
                    String i=""+d;
                    if ( rbuf.getSize() != c || System.currentTimeMillis() > (d+1000L) ) {
                        /*used = total - free;
                        prevUsed = (prevTotal - prevFree);*/
                        printf(func,0,
                            "#" + i +
                            ", Total: " + total +
                            ", Used: " + getUsedMemory() +
                            ", ∆Used: " + (used - prevUsed) +
                            ", Free: " + free +
                            ", ∆Free: " + getDeltaUsedMemory()+" "+(free - prevFree));
                        /*
                        //map.put(i, total+lim+used+lim+ (used - prevUsed)+lim+free+lim+(free-prevFree) );
                        rbuf.push(i+lim+total+lim+used+lim+ (used - prevUsed)+lim+free+lim+(free-prevFree));
                        last=i; if( first.isEmpty() ) { first=i;};
                        prevTotal = total;
                        prevFree = free;*/
                        d=System.currentTimeMillis();
                        c= rbuf.getSize();
                    }  
                    sleep(100);
            }
            setRunning();
            printf(func,0,"closed");
        }
        private String first="";
        private String  last="";
        public String outLine(){
            StringBuilder sw = new StringBuilder();
            //while ( map.get(first) == null ) { sleep(300); }
            while ( rbuf.isEmpty() ) { sleep(300);}
            //String[] fp=map.get(first).split(lim);  
            String[] fp=rbuf.getFirst().split(lim); 
            //String[] ep=map.get(last).split(lim);   
            String es=rbuf.getLast();
            if ( es == null ) { return ""; }
            String[] ep=es.split(lim); 
            last=ep[0];
            sw.append("#" + last +
                            ", Total: " + ep[0] +
                            ", Used: "  + ep[1] +
                            ", ∆Used: " + ep[2] +//getMathLongBack(ep[1],fp[1]) +
                            ", Free: "  + ep[3] +
                            ", ∆Free: " + ep[4] //getMathLongBack(ep[4],fp[4]));
                      );
            first=last;
            rbuf.clear(); rbuf.push(es);
            
            return sw.toString();
        }
        
        private String getMathLongBack(String a, String b) {
            long ad = Long.parseLong(a);
            long bd = Long.parseLong(b);
            return ""+(ad-bd);
        }
        
        public  long getMaxMemory() {
            return rt.maxMemory();
        }

        public  long getUsedMemory() {
            return getMaxMemory() - getFreeMemory();
        }
        public  long getDeltaUsedMemory(){
            used=getUsedMemory();
            long ret = used - prevUsed; 
            prevUsed=used;
            return ret;
        }

        public  long getTotalMemory() {
            return rt.totalMemory();
        }

        public  long getFreeMemory() {
            return rt.getRuntime().freeMemory();
        }
        
        long getMem() {
             final String func=getFunc("getMem()");
             long t = getTotalMemory();
             long f = getFreeMemory();
             used = t-f;
             if ( used != prevUsed ) {
                  prevUsed = (prevTotal - prevFree);
                  String i = ""+System.currentTimeMillis();
                        printf(func,0,
                            "#" + i +
                            ", Total: " + t +
                            ", Used: " + used +
                            ", ∆Used: " + (used - prevUsed) +
                            ", Free: " + free +
                            ", ∆Free: " + (used-prevUsed)+" "+(free - prevFree));
                     
                        //map.put(i, total+lim+used+lim+ (used - prevUsed)+lim+free+lim+(free-prevFree) );
                        rbuf.push(i+lim+t+lim+used+lim+ (used - prevUsed)+lim+f+lim+(f-prevFree));
                        last=i; if( first.isEmpty() ) { first=i;};
                  prevTotal = t;
                  prevFree = f;
                        
             } 
             return f;
        }


    }
}
