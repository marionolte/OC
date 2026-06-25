/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.io.logs;

import com.macmario.io.file.ReadDir;
import com.macmario.io.file.WriteFile;
import java.io.File;
import java.util.Calendar;
import java.util.Iterator;
import com.macmario.main.MainTask;
import java.util.Properties;

/**
 *
 * @author SuMario
 */
public class LogRotation extends MainTask{
    
    private boolean truncate=true;
    public LogRotation(String[] args) {
           super();
           setProperties(parseArgs(args));
           
           Properties p = getProperties();
          
           if ( p.getProperty("expr")      == null ) { p.setProperty("expr",      "\\.log$|\\.out$"); }
           if ( p.getProperty("workdir")   == null ) { p.setProperty("workdir",   getReplaceSeparator(System.getProperty("user.dir"))); }
           if ( p.getProperty("backupdir") == null ) { p.setProperty("backupdir", p.getProperty("workdir"));}
           if ( p.getProperty("minsize")   == null ) { p.setProperty("minsize",   (""+(500*1024L))    ); }
           if ( p.getProperty("minold")    == null ) { p.setProperty("minold",    (""+(60*60*1000L))  ); }
           if ( p.getProperty("truncate")  != null ) { truncate=( p.getProperty("truncate").toLowerCase().matches("true"))?true:false;}
           p.setProperty("truncate",    (""+truncate)  );
           
           Long d=1L; String m=p.getProperty("minsize").toLowerCase();
           if ( m.contains("k") ) {  d=d*1024;              m=m.replaceAll("k", ""); }
           if ( m.contains("m") ) {  d=d*1024*1024;         m=m.replaceAll("m", ""); }
           if ( m.contains("g") ) {  d=d*1024*1024*1024;    m=m.replaceAll("g", ""); }
           p.setProperty("minsize", ""+d*Long.parseLong(m));
    }
    
    public void rotate() {
        final String func=getFunc("rotate()");
        Calendar now = Calendar.getInstance();
        Long old = now.getTimeInMillis(); 
        try { old -= Long.parseLong(getProperties().getProperty("minold")); }catch(Exception e) {}
        ReadDir d  = new ReadDir( getReplaceSeparatorBack(getProperties().getProperty("workdir")  ) );
        ReadDir dTo= new ReadDir( getReplaceSeparatorBack(getProperties().getProperty("backupdir")) );
        
        System.out.println("Check Directory  "+d.getFQDNDirName()+"  writable:"+d.isWritable()+"  for files with expr:"+ getProperties().getProperty("expr")
                           +" to backup to "+dTo.getFQDNDirName()
                           );
        if ( dTo.isWritable() ) {
            printf(func,2,"Check for BACKUP Directory "+dTo.getFQDNDirName()+" OK");
        } else {
            if ( dTo.isDirectory() ) {
                printf(func,1,"ERROR: skipping rotation - backup directory "+dTo.getFQDNDirName()+" is not writable");
                return;
            } else {
                if ( dTo.mkdirs() ) {
                    printf(func,2,"Check for BACKUP Directory "+dTo.getFQDNDirName()+" OK (created)");
                } else {
                    printf(func,1,"ERROR: skipping rotation - creation of BACKUP Directory "+dTo.getFQDNDirName()+" Failed");
                    return;
                }
            }
        }
        for ( String f : d.getFiles(getProperties().getProperty("expr")) ) {
            WriteFile fn = new WriteFile(d.getFQDNDirName()+File.separator+f);
            printf(func,3,"check rotation for "+d.getFQDNDirName()+File.separator+f+" for minSize:"+getProperties().getProperty("minsize")+" lastMod:"+old);  
            
            
            if (    fn.rotate( dTo.getFQDNDirName()+File.separator+fn.getFileName(), 
                           true, 
                           Long.parseLong(getProperties().getProperty("minsize") ), 
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
    
    public String usage(boolean b){
        StringBuilder sw = new StringBuilder();
        //sw.append();
        Iterator it = getProperties().keySet().iterator();
        while ( it.hasNext() ) { 
                String s=(String)it.next();  
                if ( ! s.matches("COMMAND"))
                    sw.append(" [-").append(s)
                                    .append(" <value [")
                                    .append(getReplaceSeparatorBack(getProperties().getProperty(s)))
                                    .append("]> ]");
        }
        if (b)
            System.out.println("usage()  <command> "+sw.toString());
        return sw.toString();
    }
    
    public static void main(String[] args) {        
        LogRotation lr = new LogRotation(args);
        if ( lr.isCommand("VERSION")   ) {  System.out.println("LogRotation v"+lr.getVersion()+" of "+lr.getFullInfo()); }
        else if ( lr.isCommand("ROTATE") ) { lr.rotate();    }
        else  { 
             lr.usage(true); 
        }
    }

    
}
