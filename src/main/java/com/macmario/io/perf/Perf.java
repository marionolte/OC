package com.macmario.io.perf;


import static com.macmario.io.lib.IOLib.execReadToString;
import com.macmario.io.thread.RunnableT;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import com.macmario.main.MainTask;

public class Perf extends MainTask{
    private final Properties args; 
    private final HashMap<String,String> ops;
    private final HashMap<String,PerfTask> map;
    private final PerfMonitor pm;

    public Perf(String[] ar) {
        final String func=getFunc("Perf(String[] ar)");
        
        args = parseArgs(ar);
        ops  = com.macmario.io.lib.IOLib.scanner(ar, myusage);
        debug=Integer.parseInt(ops.get("_debug_"));
        printf(func,3,"usage:"+ops.get("_usage_")+"  args size:"+args.size());
        if ( ops.get("_usage_").equals("true") && ar.length > 0 ) { 
            printUsage(myusage); 
            System.exit(-1);
        }
        
        printf(func,3,"args hash:"+args);
        map=new HashMap<String,PerfTask> (); 
        if ( this.args.getProperty("-cpu") != null ) { map.put("cpu", new PerfTask("cpu",this.args.getProperty("-cpu"))); }
        if ( this.args.getProperty("-io")  != null ) { map.put("io",  new PerfTask("io", this.args.getProperty("-io" ) )); }
        if ( this.args.getProperty("-net") != null ) { map.put("net", new PerfTask("net",this.args.getProperty("-net"))); }
        if ( this.args.getProperty("-mem") != null ) { map.put("mem", new PerfTask("mem",this.args.getProperty("-mem"))); }
        if ( map.isEmpty() ) {
            //{ map.put("cpu", new PerfTask("cpu",this.args.getProperty("-cpu")));  }
            //{ map.put("io",  new PerfTask("io", this.args.getProperty("-io" ) )); }
            //{ map.put("net", new PerfTask("net",this.args.getProperty("-net")));  }
            { map.put("mem", new PerfTask("mem",this.args.getProperty("-mem")));  }
        }
        
        printf(func,2,"hash:"+map+":");
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
        if ( ! map.isEmpty() ) pm.start();
        //else printUsage=true;
    }
    
    //public boolean printUsage=false;
    
    public void test() {
        //debug=4;
        final String func=getFunc("test()");
        printf(func,4,"run test()");
        //if ( printUsage ) {  return; }
        while(! pm.isRunning()) { sleep(300);}
        printf(func,4,"pm running");
        while ( ! pm.isClosed() ) {  
            sleep(300);
        }
        printf(func,4,"pm complete test()");
    }

    final public static String myusage="\nusage()\n[-cpu=time=XX,count=XX] [-mem=time=XX,count=XX] [-io=time=XX,count=XX] [-net=time=XX,count=XX] ";
    
    
    public static Perf getInstance(String[] args) {
         Perf p = new Perf(args);          
         return p;
    }
    
    public static void main(String[] args) {
        Perf p = getInstance(args);
             p.test();
    }

   private class PerfTask extends RunnableT{
        private long defTime=15000L;
        long time=defTime;
        int count=3;
        private final String command;
        private final String area;
        HashMap<String, String> imap = new HashMap<String, String>();

        PerfTask(String area, String ar) {
            final String func=getFunc("PerfTask(String area, String ar)");
            printf(func,3,"area:"+area+": ar:"+ar+":");
            this.area=area;
            if ( ar != null && ! ar.isEmpty() ) {
                 for ( String s : ar.split(",") ) {
                     printf(func,3,"s:"+s+":");
                     if ( s.startsWith("time:") ) { 
                         time=Long.parseLong(s.substring("time:".length()))*1000L;
                         if ( time < 1000L ) { time=defTime; }
                     }else if ( s.startsWith("count:") ) { 
                         count=Integer.parseInt(s.substring("count:".length()));
                     } else {
                         String[] sp = s.split(":");
                         String    a = (s.length()>sp[0].length()+1)?s.substring(sp[0].length()+1):"true";
                         imap.put(sp[0], a);
                     }
                     
                 }
            }
            
            switch(area) {
                case "net": this.command="netstat -an"; break;
                case "mem": this.command="buildin";     break;
                case "cpu": this.command="ps -e -o uid,gid,pid,ppid,cpu,%cpu,%mem,rss,vsz,wchan,time,command=cmd ; uptime"; break;
                default: this.command="";
            }
            printf(func,3,"area:"+area+":  command:"+this.command+":");
            
        }
        
        
        @Override
        public void run() {
            final String func=getFunc("run()");
            setRunning();
            int ru=1;
            printf(func,3,"count:"+count+" time:"+time);
            while (ru<=count) {
                printf(func,3,"ru:"+ru+" count:"+count);
                if ( ! this.command.isEmpty() && ! this.command.equals("buildin") ) {
                    printf(func,3,"command="+this.command+"");
                    try { 
                         StringBuilder sw = new StringBuilder(execReadToString(this.command));
                         printf(func,2,"command produce "+sw.capacity()+" char outcome");
                         
                         switch(area) {
                             case "net": NetStat.setPort(imap.get("port"));sw.replace(0, sw.capacity(), NetStat.outline(sw).toString()); break;
                             default: ;
                        }
            

                    }catch(java.io.IOException io) {
                        printf(func,1,"execReadToString end with exeception "+io.getMessage(),io);
                    }
                }
                else if ( ! this.command.isEmpty() &&  this.command.equals("buildin")  ) {
                        printResults();
                }
                ru++;
                if ( ru <= count ) { 
                    printf(func,4,"go sleep");
                    sleep(time); 
                    printf(func,4,"wakeup");
                }
            }    
            printf(func,4,"out while");
            setRunning();
            printf(func,4,"done");
            
            //printResults();
        }
        
        private void printResults() {
            switch (area) {
                            case "mem":  MemInfo.outLine(); break;
                            case "cpu":  CpuInfo.outLine(); break;
                            default: ;
            }
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