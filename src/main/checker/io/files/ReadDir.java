/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utils.io.files;

import java.io.File;

/**
 *
 * @author SuMario
 */
public class ReadDir {
    private File readDir;
    private long modified=0;
    private String file;
    private String fileList="";
    private String dirList="";

    public ReadDir(String d){ this(new File(d)); }//this.readDir=new File(d); this.file=d; }

    public ReadDir(File dir) { readDir=dir; this.file=dir.toString(); }

    public boolean isDirectory() { return ( this.readDir.isDirectory() ) ? true : false;  }
    public boolean isFile()      { return ! isDirectory(); }
    
    public boolean isReadable()  { return  ( this.readDir.canRead()   )? true : false ;  }
    public boolean isWritable()  { return  ( this.readDir.canWrite()  )? true : false ;  }
    public boolean isExecutable(){ return  ( this.readDir.canExecute())? true : false ;  }
    public boolean isExist()     { return  this.readDir.exists(); }
    public boolean exists()       { return  isExist(); }

    public String[] getFiles(){ return loadDir(false); }
    public String[] getDirectories() { return loadDir(true); }

    public String[] loadDir(boolean dir){
        String[] s = new String[] {};

        if ( this.readDir.lastModified() > modified ) {
             fileList=""; dirList="";
             if (! this.file.equalsIgnoreCase(java.io.File.separator) ) { dirList=".."; }
             for ( String f : readDir.list() ) {
                 File io =new File(file+java.io.File.separator+f);
                 if ( io.isFile() ) {
                     if ( fileList.isEmpty() ) {
                          fileList=""+f;
                     } else {
                          fileList=fileList+"@"+f;
                     }
                 } else if ( io.isDirectory() ) {
                     if ( dirList.isEmpty() ) {
                          dirList=""+f;
                     } else {
                          dirList=dirList+"@"+f;
                     }
                 }
             }
        }
        if ( dir ) {
           s = dirList.split("@");
        } else {
           s = fileList.split("@");
        }
        return s;
    }

    public String getParent()     { return readDir.getParent(); }
    public File   getParentFile() { return readDir.getParentFile(); }

    public File getFile() { return this.readDir; }
    public String getFQDNDirName() { return this.readDir.toString(); }
    public String getDirName()  { return this.readDir.getName(); }

    public boolean create() {
        if ( ! readDir.exists() ) {
             readDir.mkdirs();
        }
        return ( readDir.exists() && readDir.isDirectory() )? true : false;
    }
    
    public boolean mkdir()  { return readDir.mkdir();  }
    public boolean mkdirs() { return readDir.mkdirs(); }
}
