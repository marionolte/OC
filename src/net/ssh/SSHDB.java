/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.ssh;

import general.Version;
import io.file.ReadDir;
import io.file.ReadFile;
import io.file.SecDBFile;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author SuMario
 */
public class SSHDB extends Version{
    private String sshdir=System.getProperty("user.home")+File.separator+".ssh";
    SSHDB() {
         ReadDir d = new ReadDir(sshdir);
         if ( ! d.isDirectory() ) {
                 d = new ReadDir(getTempDir()+File.separator+".ssh");
                 if ( ! d.isDirectory() ) { d.mkdirs(); }
         }
         SecDBFile f = new SecDBFile(getDBFile(d));
    }
    
    private ReadFile getDBFile(ReadDir d) {
        ReadFile f = null ;
                String[] sp = d.getFiles("sshdb-*.sdb");
                if ( sp != null & sp.length >=1 ) {  f = new ReadFile(d.getFQDNDirName()+File.separator+sp[0]); }
                if ( f == null || ! f.isReadableFile() ) {
                    try {
                        f = new ReadFile( File.createTempFile("sshdb-", ".sdb", d.getFile() ) );
                    } catch(IOException io) {
                        f = new ReadFile(sshdir+"sshdb-master.sdb");
                    } 
                }
        return f;
    }
}
