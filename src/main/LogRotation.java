/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import io.file.ReadDir;
import io.file.ReadFile;
import io.file.WriteFile;
import java.io.File;
import java.util.Calendar;
import java.util.Iterator;

/**
 *
 * @author SuMario
 */
public class LogRotation extends MainTask{
    
    private boolean truncate=true;
    public LogRotation(String[] args) {
           super();
           super.prop = parseArgs(args);
          
           if ( prop.getProperty("expr")      == null ) { prop.setProperty("expr",      "\\.log$|\\.out$"); }
           if ( prop.getProperty("workdir")   == null ) { prop.setProperty("workdir",   getReplaceSeparator(System.getProperty("user.dir"))); }
           if ( prop.getProperty("backupdir") == null ) { prop.setProperty("backupdir", prop.getProperty("workdir"));}
           if ( prop.getProperty("minsize")   == null ) { prop.setProperty("minsize",   (""+(500*1024L))    ); }
           if ( prop.getProperty("minold")    == null ) { prop.setProperty("minold",    (""+(60*60*1000L))  ); }
           if ( prop.getProperty("truncate")  != null ) { truncate=( prop.getProperty("truncate").toLowerCase().matches("true"))?true:false;}
           prop.setProperty("truncate",    (""+truncate)  );
           
           Long d=1L; String m=prop.getProperty("minsize").toLowerCase();
           if ( m.contains("k") ) {  d=d*1024;              m=m.replaceAll("k", ""); }
           if ( m.contains("m") ) {  d=d*1024*1024;         m=m.replaceAll("m", ""); }
           if ( m.contains("g") ) {  d=d*1024*1024*1024;    m=m.replaceAll("g", ""); }
           prop.setProperty("minsize", ""+d*Long.parseLong(m));
    }
    
    public void rotate() {
        final String func="rotate()";
        Calendar now = Calendar.getInstance();
        Long old = now.getTimeInMillis(); 
        try { old -= Long.parseLong(prop.getProperty("minold")); }catch(Exception e) {}
        ReadDir d  = new ReadDir( getReplaceSeparatorBack(prop.getProperty("workdir")  ) );
        ReadDir dTo= new ReadDir( getReplaceSeparatorBack(prop.getProperty("backupdir")) );
        
        System.out.println("Check Directory  "+d.getFQDNDirName()+"  writable:"+d.isWritable()+"  for files with expr:"+ prop.getProperty("expr")
                           +" to backup to "+dTo.getFQDNDirName()
                           );
        if ( dTo.isWritable() ) {
            printf(func,2,"Check for BACKUP Directory "+dTo.getFQDNDirName()+" OK");
        } else {
            if ( dTo.isDirectory() ) {
                printf(func,0,"ERROR: skipping rotation - backup directory "+dTo.getFQDNDirName()+" is not writable");
                return;
            } else {
                if ( dTo.mkdirs() ) {
                    printf(func,2,"Check for BACKUP Directory "+dTo.getFQDNDirName()+" OK (created)");
                } else {
                    printf(func,0,"ERROR: skipping rotation - creation of BACKUP Directory "+dTo.getFQDNDirName()+" Failed");
                    return;
                }
            }
        }
        for ( String f : d.getFiles(prop.getProperty("expr")) ) {
            WriteFile fn = new WriteFile(d.getFQDNDirName()+File.separator+f);
            printf(func,3,"check rotation for "+d.getFQDNDirName()+File.separator+f+" for minSize:"+prop.getProperty("minsize")+" lastMod:"+old);  
            
            
            if (    fn.rotate( dTo.getFQDNDirName()+File.separator+fn.getFileName(), 
                           true, 
                           Long.parseLong(prop.getProperty("minsize") ), 
                           old, 
                           truncate
                    )
                ) {
                System.out.println("INFO: File: "+fn.getFQDNFileName()+" is rotated "); 
            } else {
                System.out.println("ERROR: Filerotation of "+fn.getFQDNFileName()+" to "+dTo.getFQDNDirName()+File.separator);
            }    
        }
    }
    
    private  void usage(){
        StringBuilder sw = new StringBuilder();
        sw.append("usage()  <command> ");
        Iterator it = prop.keySet().iterator();
        while ( it.hasNext() ) { 
                String s=(String)it.next();  
                if ( ! s.matches("COMMAND"))
                    sw.append(" [-").append(s).append(" <value>").append("]");
        }
        
        System.out.println(sw.toString());
    }
    
    public static void main(String[] args) {        
        LogRotation lr = new LogRotation(args);
        if ( lr.isCommand("VERSION")   ) {  System.out.println("LogRotation v"+lr.getVersion()+" of "+lr.getFullInfo()); }
        else if ( lr.isCommand("USAGE")) { lr.usage(); }
        else {
            lr.rotate();
        }
    }

    
}
