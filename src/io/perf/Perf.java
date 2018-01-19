package io.perf;


import io.thread.RunnableT;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Scanner;
import main.MainTask;

public class Perf extends MainTask{
    private final Properties args; 
    private final HashMap<String,PerfTask> map;
    private final PerfMonitor pm;

    public Perf(String[] ar) {
        args = parseArgs(ar);
        System.out.println("args hash:"+args);
        map=new HashMap<String,PerfTask> (); 
        if ( this.args.getProperty("-cpu") != null ) { map.put("cpu", new PerfTask("cpu",this.args.getProperty("-cpu"))); }
        if ( this.args.getProperty("-io")  != null ) { map.put("io",  new PerfTask("io", this.args.getProperty("-io" ) )); }
        if ( this.args.getProperty("-net") != null ) { map.put("net", new PerfTask("net",this.args.getProperty("-net"))); }
        
        Iterator<String> itter = map.keySet().iterator();
        if ( itter.hasNext() ) {
            while ( itter.hasNext() ) {
                String s = itter.next();
                System.out.println("map s:"+s+":");
                PerfTask pt = map.get(s);
                if ( pt != null ) {
                     pt.start();
                }
            }
        }
        
         pm = new PerfMonitor();
         pm.start();

    }
    
    public void test() {
        while ( ! pm.isCompleted() ) {            
            sleep(300);
        }
    }

    
    
    
    public static Perf getInstance(String[] args) {
         Perf p = new Perf(args);          
         return p;
    }
    
    public static void main(String[] args) {
        Perf p = getInstance(args);
             p.test();
    }

   private class PerfTask extends RunnableT{

        long time=15000L;
        int count=3;
        private final String command;

        PerfTask(String area, String ar) {
            System.out.println("area:"+area+": ar:"+ar+":");
            if ( ar != null && ! ar.isEmpty() ) {
                 for ( String s : ar.split(",") ) {
                     System.out.println("s:"+s+":");
                     if ( s.startsWith("time:") ) { 
                         time=Long.parseLong(s.substring("time:".length()))*1000L;
                     }else if ( s.startsWith("count:") ) { 
                         count=Integer.parseInt(s.substring("count:".length()));
                     }
                 }
            }
            switch(area) {
                case "net": this.command="netstat -an"; break;
                default: this.command="";
            }
            
        }
        
        private String execReadToString(String execCommand) throws java.io.IOException {
            Process proc = Runtime.getRuntime().exec(execCommand);
            try (InputStream stream = proc.getInputStream()) {
                try (Scanner s = new Scanner(stream).useDelimiter("\\A")) {
                    return s.hasNext() ? s.next().trim() : "";
                }
            }
        }

        @Override
        public void run() {
            setRunning();
            int ru=1;
            System.out.println("count:"+count+" time:"+time);
            while (ru<count) {
                System.out.println("ru:"+ru+" count:"+count);
                if ( ! this.command.isEmpty() ) {
                    System.out.println("command="+this.command+"");
                    //try { 

                         //System.out.println(execReadToString(this.command));

                    //}catch(java.io.IOException io) {
                    //}
                }
                ru++;
                if ( ru < count ) { 
                    System.out.println("go sleep");
                    sleep(time); }
            }    
            System.out.println("out while");
            setRunning();
            System.out.println("done");
        }

        boolean isPrintable() {
            return false;
        }
   }
   private class PerfMonitor extends RunnableT {

        boolean isCompleted(){ return ! this.isRunning(); }
       
        @Override
        public void run() {
            setRunning();
            System.out.println("Monitor starts");
            
            boolean loop=true;
            while(loop) {
                loop=false;
                Iterator<String> itter = map.keySet().iterator();
                if ( itter.hasNext() ) {
                    while ( itter.hasNext() ) {
                        PerfTask pt = map.get(itter.next());
                            if ( pt != null ) {
                                if ( pt.isRunning() ) {
                                    loop=true;
                                    if ( pt.isPrintable() ) {

                                    }
                                } 
                            }
                    }
                }
                sleep(300);
            }
            setRunning();
            System.out.println("Monitor ends");
        }
       
   }
}