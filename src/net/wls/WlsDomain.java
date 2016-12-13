/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.wls;

import io.file.ReadDir;
import io.file.XMLReadFile;
import java.io.File;
import main.MainTask;
/**
 *
 * @author SuMario
 */
public class WlsDomain extends MainTask{
    private ReadDir confdir;
    private XMLReadFile conf;
    public WlsDomain(String[] args) {
       super(args);
       confdir = new ReadDir(".");
       String te = getProperty("CONF");
       if (te != null && ! te.isEmpty() ) { confdir=new ReadDir(te); }
       if ( confdir.isDirectory() ) {
            conf=new XMLReadFile(confdir.getFQDNDirName()+File.separator+"config"+File.separator+"config.xml");
            if ( ! conf.isReadableFile() ) {
                throw new RuntimeException("ERROR: "+conf.getFQDNFileName()+" is not a readable config.xml");
            }
                 
       } else {
           throw new RuntimeException("ERROR: "+confdir.getFQDNDirName()+" is not a directory ");
       }
       
       
    }
    
    public static void main(String[] args) {
        WlsDomain wl = getInstance(args);
    }
    
    public static WlsDomain getInstance(String[] args) {
        WlsDomain wl = new WlsDomain(args);
        return wl;
    }
}
