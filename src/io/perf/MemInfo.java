/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.perf;

import io.buffer.RingBuffer;
import io.thread.RunnableT;
import java.util.HashMap;

/**
 *
 * @author SuMario
 */
class MemInfo {
    private static Runtime rt;
    private static MemRuntime mr;
    static {
        rt = Runtime.getRuntime();
        mr = new MemRuntime();
        mr.start();
    }
    
    static void outLine() {
    
    }
    
    static private class MemRuntime extends RunnableT{
        long prevTotal = 0L;
        long prevFree  = 0L;
        long total     = 0L;
        long used      = 0L;
        long prevUsed  = 0L;
        long free      = 0L;
        HashMap<String, String> map = new HashMap<String,String>();
        RingBuffer<String> rbuf = new RingBuffer<String>(10);

        MemRuntime() {
             prevFree = rt.freeMemory();
             total    = rt.totalMemory();
             
        }
        
        @Override
        public void run() {
            setRunning();
            final String lim="_@_";
            while( isRunning() ) {
                    free = getFreeMemory();
                    String i=""+System.currentTimeMillis();
                    if (total != prevTotal || free != prevFree) {
                        used = total - free;
                        prevUsed = (prevTotal - prevFree);
                        System.out.println(
                            "#" + i +
                            ", Total: " + total +
                            ", Used: " + getUsedMemory() +
                            ", ∆Used: " + (used - prevUsed) +
                            ", Free: " + free +
                            ", ∆Free: " + (free - prevFree));
                     
                        map.put(i, total+lim+used+lim+ (used - prevUsed)+lim+free+lim+(free-prevFree) );
                        rbuf.push(i+lim+total+lim+used+lim+ (used - prevUsed)+lim+free+lim+(free-prevFree));
                    
                        prevTotal = total;
                        prevFree = free;
                    
                    }    
            }
            setRunning();
        }
        
        public  long getMaxMemory() {
            return rt.maxMemory();
        }

        public  long getUsedMemory() {
            return getMaxMemory() - getFreeMemory();
        }
        public  long getDeltaUsedMemory(){
            long used=getUsedMemory();
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


    }
}
