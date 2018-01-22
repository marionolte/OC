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
        final String func=getFunc("Perf(String[] ar)");
        printf(func,3,"args hash:"+args);
        map=new HashMap<String,PerfTask> (); 
        if ( this.args.getProperty("-cpu") != null ) { map.put("cpu", new PerfTask("cpu",this.args.getProperty("-cpu"))); }
        if ( this.args.getProperty("-io")  != null ) { map.put("io",  new PerfTask("io", this.args.getProperty("-io" ) )); }
        if ( this.args.getProperty("-net") != null ) { map.put("net", new PerfTask("net",this.args.getProperty("-net"))); }
        
        Iterator<String> itter = map.keySet().iterator();
        if ( itter.hasNext() ) {
            while ( itter.hasNext() ) {
                String s = itter.next();
                printf(func,3,"map s:"+s+":");
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
        final String func=getFunc("test()");
        printf(func,4,"run test()");
        while ( ! pm.isClosed() ) {            
            sleep(300);
        }
        printf(func,4,"complete test()");
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
            final String func=getFunc("run()");
            setRunning();
            int ru=1;
            System.out.println("count:"+count+" time:"+time);
            while (ru<=count) {
                printf(func,3,"ru:"+ru+" count:"+count);
                if ( ! this.command.isEmpty() ) {
                    printf(func,3,"command="+this.command+"");
                    //try { 

                         //System.out.println(execReadToString(this.command));

                    //}catch(java.io.IOException io) {
                    //}
                }
                ru++;
                if ( ru < count ) { 
                    printf(func,4,"go sleep");
                    sleep(time); 
                    printf(func,4,"waitup");
                }
            }    
            printf(func,4,"out while");
            setRunning();
            printf(func,4,"done");
        }

        boolean isPrintable() {
            return false;
        }
   }
   private class PerfMonitor extends RunnableT {

        boolean isCompleted(){ return ! this.isRunning(); }
       
        @Override
        public void run() {
            final String func=getFunc("run()");
            setRunning();
            printf(func,4,"Monitor starts");
            
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
            printf(func,4,"Monitor ends");
            setClosed();
        }
       
   }
}