/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.comm.checker;


import com.macmario.general.Version;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.util.ArrayList;

/**
 *
 * @author SuMario
 */
public class Checker extends Version {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        int ex=0;
        
        if ( args.length < 1 ) { 
            System.err.println("ERROR: missing file or directory as option");
            usage();
            ex=1;
        } else {
            if ( args[0].matches("ENV") ) {
                try {
                    int m=Integer.parseInt( System.getenv("ENV_COUNT") );
                    if ( m == 0 ) { throw new RuntimeException("no options"); }
                    args = new String[m];
                    for ( int i=0; i<m ; i++) {
                        args[i]=System.getenv("ENVOPT"+i);
                    }
                    
                } catch (Exception e) {
                    usage(); 
                    System.exit(ex);
                }    
            }
            Checker ch=new Checker(args);
                    ch.verify();
                    ex=ch.getResult();
        }
        
        System.exit(ex);
    }

    private static void usage1() {
        System.out.println("usage: java -cp OC.jar main.checker.Checker "+usage());
    }
    public static String usage(){
        return (" [-f <pattern file>] [-i <include pattern>] [-b <begin time>] [-e <end time>] <FILE|Directory>\n"
               +"       format time: DAY-MONTH-YEAR HH:MIN:SEC - example to use '01-MAR-2013 10:12:45' ");
    }
    
    public Checker(String[] args) {
        boolean run=true;
        for (int i=0; i< args.length ; i++ ) {
            if ( args[i].matches("-d") ) { debug++; }
            else if ( args[i].matches("-f") ) { patterFile = new File(args[++i]); }
            else if ( args[i].matches("-i") ) { pattern.append(args[++i]).append("\n"); }
            else if ( args[i].matches("-b") ) { timebegin=addTimer(args[++i]); }
            else if ( args[i].matches("-e") ) { timeend=addTimer(args[++i]); }
            else if ( args[i].matches("-h") ||  args[i].matches("--help") ) { usage(); System.exit(-1); }
            else {
                File n = new File(args[i]);
                if      ( n.isDirectory() && n.canRead() ) { readDir(n); } 
                else if ( n.isFile()      && n.canRead() ) { ar.add(n); }
                run=false;
            }
        }
        if ( run ) { readDir( new File(".")); }
        
    }
    
    
    private String[] addTimer(String stime) {
        log(1,"addTimer(String stime) - set |@|"+stime+"|@|");
        String[] sp=stime.split(" ");
        String[] date = new String[3];
        String[] time = new String[3];
        if ( sp[0] != null ) {
            if ( sp[0].indexOf("-")>0 ) { date=sp[0].split("-"); }
            if ( sp[0].indexOf(":")>0 ) { time=sp[0].split(":"); date=new String[3]; }
        }
        if ( sp.length> 1 && sp[1] != null && sp[1].indexOf(":")>0) { time=sp[1].split(":"); }
        
        
        String[] ret=new String[] {
                                    (date.length >0 )?date[0]:null ,
                                    (date.length >1 )?date[1]:null ,
                                    (date.length >2 )?date[2]:null ,
                                    (time.length >0 )?time[0]:null ,
                                    (time.length >1 )?time[1]:null ,
                                    (time.length >2 )?time[2]:null ,
                                  };
        
        log(2,"addTimer(String stime): date:"+ret[0]+"-"+ret[1]+"-"+ret[2]+"  time:"+ret[3]+":"+ret[4]+":"+ret[5]+":");
        
        return ret;
    }

    private String[] timebegin=new String[6];
    private String[] timeend=new String[6];
    private File patterFile;
    private ArrayList ar=new ArrayList();
    private static int debug=0; 
    private int exit=0;
    public int getResult() { return exit; }

    private void readDir(File folder) {
         for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                readDir(fileEntry);
            } else {
                ar.add(fileEntry);
            }
        }
    }
    
    private StringBuilder readFile(File n) {
        StringBuilder sw = new StringBuilder();
        log(2, "readout now :"+n.toString());
        BufferedReader br=null;
        try {
            br = new BufferedReader( new FileReader( n )  );
            String str; boolean b=false;
            while( (str=br.readLine()) !=null  ) {  
                if( b ) { sw.append("\n"); }
                sw.append(str); 
                b=true;  
            }
        }catch(Exception ex) {
            log(2,"FILE:"+n.toString()+" readout with Exception"+ex.toString());
        } finally {
            try  { br.close(); } catch(Exception e) {}
        }
        return sw;
    }

    private ArrayList plist=new ArrayList();
    private PatternTest last=null;
    private PatternTest first=null;
    
    StringBuilder pattern=new StringBuilder();
    CheckTimer ckt=null;
    
    public void verify() {
        
        ArrayList pa = new ArrayList();
        String[] sp = null ;
        if ( this.patterFile== null ) {
            InputStream is = this.getClass().getResourceAsStream("/main/checker/defpattern.properties");
            StringBuilder sw=new StringBuilder();
            byte[] buf = new byte[32*1024];
            int read;
            try {
                while( (read = is.read(buf) ) >= 0) {
                    sw.append(new String(buf));
                }
            } catch(Exception e) {
                log(1,"pattern read from jar with Exception"+e.toString());
            }
            sp=sw.toString().split("\n");
        }else {
           sp= readFile(this.patterFile).toString().split("\n");
        }
        if ( sp != null && sp.length > 0 ) {
          for ( int i=0; i<sp.length-1; i++) {
              if ( sp[i] != null && ! sp[i].isEmpty() ) {
                log(2,"add pattern:"+sp[i]);
                pa.add(sp[i]); 
              }  
          }
        }
        if ( pa.size() > 0 ) {
            
            
            log(3,pa.size()+" pattern received");
            for ( int i=0; i<pa.size(); i++ ) {
                log(3, "add:"+i);
                String m=(String) pa.get(i);
                log(2,"start PatternTest "+i+" for :"+m+":");
                PatternTest an = new PatternTest(m,debug);
                            if (last != null ) { last.setPatternTest(an); }
                            last=an;
                            if ( first == null ) { first=an;}
                (new Thread(an, m)).start();
                log(2, "start completed for pattern:"+m+":");
            }

            ckt = new CheckTimer(timebegin,timeend,debug);
            for ( int i=0; i< ar.size(); i++ ) {
                File f = (File) ar.get(i) ;
                ckt.reset();
                log(2,"MAIN("+i+"): setTest on "+first.getName()+" for file:"+f.toString()+"  size:"+f.length() );
                if ( f.length() < 32*1024 ) {
                    first.setTest( readFile ( f ),f ,ckt);
                } else {
                    log(2,"MAIN("+i+"): handle large size file "+f.toString() ); 
                    InputStream ios=null;
                    try {
                        byte[] buf = new byte[32*1024];
                        int read;
                        StringBuilder sw=new StringBuilder();
                        ios = new FileInputStream(f);  
                        int d=0; boolean b=false;
                        while( (read = ios.read(buf) ) >= 0) {
                            final String m = new String(buf);
                            sw.append(m); b=true;
                            if ( (d%100) == 0 ) {
                                log(3,"provide now |"+sw.toString()+"|");
                                first.setTest(sw,f,ckt);
                                sw = new StringBuilder();
                                sw.append(m);  // overlapped
                                b=false;
                            }    
                            d++;
                        }
                        log(2,"MAIN("+i+"): verify not provided update if true="+b);
                        if (b) { first.setTest(sw,f,ckt);}
                    } catch (Exception e) {
                        log(2,"MAIN("+i+"): handle file "+f.toString()+" runs in exception:"+e.toString() ); 
                    }finally {  
                      try { ios.close(); } catch(Exception e) {}
                    }   
                }    
            }
            sleep(10000);
        } else {
            log(0,"no pattern too check");
        }    
    }
    
    private static void log(final int level, final String msg) {
       if ( debug >= level  ) 
        System.out.println(msg);
    }
    
    
}
