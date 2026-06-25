/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.wls;

import com.macmario.general.Version;
import com.macmario.io.file.ReadDir;
import com.macmario.io.file.WriteFile;
import java.io.File;
import java.util.Calendar;

/**
 *
 * @author SuMario
 */
public class WlsDomainLogRotation extends Version{

    private final   WlsDomain   dom;
    public static long        minsize = (4 * 1024L);
    public static int         minold  = 3;
    public static int         maxold  = 30;
    public static String      savefile="\\.pid|\\.lck";
    
    public WlsDomainLogRotation(WlsDomain d) {
        this.dom=d;
    }
    
    
    public void rotate() {
        ReadDir di = new ReadDir(this.dom.getDomainLocation()+File.separator+"servers");
        for ( String srv : di.getDirectories() ) {
            if ( ! srv.matches("domain_bak") && ! srv.matches("\\.\\.") && ! srv.matches("\\.") ) {
                //System.out.println("srv:"+srv+":");
                ReadDir dir = new ReadDir( di.getFQDNDirName()+File.separator+srv+File.separator+"logs");
                if ( dir.isDirectory() ) { 
                    System.out.println("INFO: rotate "+dir.getFQDNDirName());
                    rotateDir(dir); 
                }
            }
        }
       
        di = new ReadDir(this.dom.getDomainLocation());
        for(String f : di.getFiles("hprof$")) {
            if ( ! f.isEmpty() ) {
                System.out.println("INFO: gzip file "+f);
                WriteFile fn = new WriteFile(this.dom.getDomainLocation()+File.separator+f);
                          fn.gzip();
            }
        }
        for(String f : di.getFiles("gc*log$")) {
            if ( ! f.isEmpty() ) {
                System.out.println("INFO: truncate + gzip file "+f);
                WriteFile fn = new WriteFile(this.dom.getDomainLocation()+File.separator+f);
                          fn.rotate(f+"-"+fn.getTime()+".gz", true, true);
            }
        }
        
    }
    
    private void rotateDir(ReadDir dir) {
        for (String f : dir.getFiles() ) {
            WriteFile wf = new WriteFile(dir.getFQDNDirName()+File.separator+f);
            checkFile(wf);
        }
        
    }
    
    
    
    private void checkFile(WriteFile wf) {
        final String func=getFunc("checkFile(WriteFile wf)");
        printf(func,2,wf.getFQDNFileName()
                +" \n\tsize:"+wf.isBiggerThan(minsize)
                +": \n\tcompress:"+wf.isOlderThanXDays(minold)+":  is compressed:"+wf.isCompresssed()
                +": \n\tdelete alter:"+wf.isOlderThanXDays(maxold) 
                +": \n\tsav File "+wf.isMatching(savefile)
        );
        
        if ( ! wf.isMatching(savefile) ) {
          if ( wf.isOlderThanXDays(maxold)) {
               System.out.println("INFO: file "+wf.getFQDNFileName()+" are deleted  now:"+wf.delete());
          } else {
              if ( ! wf.isCompresssed() ){  
                if ( wf.isOlderThanXDays(minold) || wf.isBiggerThan(minsize) ) {
                   boolean trunc = wf.isMatching("log$|out$");
                   System.out.println("INFO: file "+wf.getFQDNFileName()+" rotate (truncate:"+trunc+")");
                   //WriteFile::public boolean rotate(String fn, boolean gzip, long minSize, long old, boolean truncate)
                   wf.rotate(wf.getFQDNFileName()+"."+getTime()+".gz", true, trunc );
                }   
              } else {
                  printf(func,3, wf.getFQDNFileName()+" are compressed");
              }
          } 
        } else {
            printf(func,3, wf.getFQDNFileName()+" is a save file");
        }
    }
    
    
    private String getTime() {
        StringBuilder sw =new StringBuilder();
        Calendar now = Calendar.getInstance();
                 now.setTimeInMillis(System.currentTimeMillis());
        
        sw.append(  getAlign(  now.get(Calendar.YEAR)        ,4)   
                   +getAlign( (now.get(Calendar.MONTH)+1)    ,2) 
                   +getAlign(  now.get(Calendar.DAY_OF_MONTH),2)
                 +"-"
                   +getAlign(  now.get(Calendar.HOUR_OF_DAY), 2)
                   +getAlign(  now.get(Calendar.MINUTE),      2)    
                   +getAlign(  now.get(Calendar.SECOND),      2)
         );

        return sw.toString();
    }
    
    private String getAlign(int i, int c) {
        if ( c == 2 ) {
            return ((i<10)?"0":"")+i;
        }
        StringBuilder sw = new StringBuilder(""+i);
        while (sw.length() < c ) { sw.insert(0, "0"); }
        
        return sw.toString();
    }
    
    public static String usage() {
        StringBuilder sw = new StringBuilder();
        sw.append(" [-minsize <size 4k>]");
        sw.append(" [-minold <days 3d>]");
        sw.append(" [-maxold <days 30d>]");
        sw.append(" [-savefile <pattern>]");
        sw.append(" <domain dir> [<domain dir1> ..]");
        return sw.toString();
    }    
    
    
    public static final String sepa="__@@__";
    public static       String dirs="";    
    public static void parseArgs(String[] args) {
        StringBuilder sw = new StringBuilder();
        
        for ( int i=0; i< args.length; i++ ) {
            if        ( args[i].matches("-minsize") ){ WlsDomainLogRotation.minsize  = Long.parseLong(args[++i]); 
            } else if ( args[i].matches("-minold")  ){ WlsDomainLogRotation.minold   = Integer.parseInt(args[++i]);
            } else if ( args[i].matches("-maxold")  ){ WlsDomainLogRotation.maxold   = Integer.parseInt(args[++i]);
            } else if ( args[i].matches("-savefile")){ WlsDomainLogRotation.savefile = args[++i];
            } else if ( args[i].matches("-usage")   ){ System.out.println("usage() - "+usage()); 
                                                       System.exit(1);
            } else {
                sw.append(sepa).append(args[i]);
            }        
        }
        dirs=sw.toString();
    }
    
    public static void main(String[] args) throws Exception {
        parseArgs(args);
        
        for ( String s : dirs.split(sepa) ) {
            if ( ! s.isEmpty() ) {
                ReadDir di = new ReadDir(s);
                WlsDomain d = new WlsDomain(di.getDirName());
                          d.setDomainLocation(di.getFQDNDirName());
                WlsDomainLogRotation wlog = new WlsDomainLogRotation(d);
                                     wlog.rotate();
            }
        }
        
    }

    
}
