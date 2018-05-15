package io.perf;


import static io.lib.IOLib.execReadToString;
import io.thread.RunnableT;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
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
        if ( this.args.getProperty("-mem") != null ) { map.put("mem", new PerfTask("mem",this.args.getProperty("-mem"))); }
        
        
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
        else printUsage=true;
    }
    
    public boolean printUsage=false;
    
    public void test() {
        debug=4;
        final String func=getFunc("test()");
        printf(func,4,"run test()");
        if ( printUsage ) {  return; }
        while(! pm.isRunning()) { sleep(300);}
        printf(func,4,"pm running");
        while ( ! pm.isClosed() ) {            
            sleep(300);
        }
        printf(func,4,"pm complete test()");
    }

    
    public String usage() {
        printUsage=true;
        StringBuilder sw = new StringBuilder();
        sw.append("< [-cpu|-mem|-io|-net]=time=XX,count=XX  ..>");
        return sw.toString();
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
                        switch (area) {
                            case "mem":  MemInfo.outLine(); break;
                            default: ;
                        }
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