/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.macmario.io.file;

import com.macmario.general.Version;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 *
 * @author SuMario
 */
public class ReadDir extends Version{
    
    private File readDir;
    private long modified=0;
    private String file;
    private String fileList="";
    private String dirList="";

    public ReadDir(String d){ this(new File(d)); }//this.readDir=new File(d); this.file=d; }

    public ReadDir(File dir) { 
        readDir = getCanonical(dir);
        this.file=readDir.toString(); 
        //System.out.println("file:"+file+":");
    }
    
    private File getCanonical(File d) {
        final String sepa="__@@__";
        final ArrayList<String> ar = new ArrayList<>();
        String _userd = System.getProperty("user.dir");
        
        String[] sp= ((isUnix())? d.getAbsolutePath().replaceAll("^~", System.getProperty("user.home"))
                                        .replaceAll("^.$", System.getProperty("user.dir")+_FS)
                                        .replaceAll("^."+_FS, _userd+_FS)
                                : d.getAbsolutePath()
                      ).split(_FS);
        for(String s : sp) {
            if ( ! s.isEmpty() ) {
                if ( s.equals("..") ) { ar.remove(ar.size()-1); }
                else if ( s.equals(".") ) {}
                else { ar.add(s); }
            }
        }
        
        StringBuilder sw = new StringBuilder(sepa);
        while( ! ar.isEmpty() ) {sw.append(ar.remove(0)).append(sepa); }
        
        return new File(sw.toString().replaceAll(sepa, _FS));
    }

    public boolean isDirectory() { return ( this.readDir.isDirectory() ) ? true : false;  }
    public boolean isFile()      { return ! isDirectory(); }
    
    public boolean isReadable()  { return  ( this.readDir.canRead()   )? true : false ;  }
    public boolean isWritable()  { return  ( this.readDir.canWrite()  )? true : false ;  }
    public boolean isExecutable(){ return  ( this.readDir.canExecute())? true : false ;  }
    public boolean isExist()     { return  this.readDir.exists(); }
    public boolean exists()       { return  isExist(); }

    public String[] getFiles(){ return loadDir(false); }
    public String[] getDirectories() { return loadDir(true); }
    
    public ReadFile getFile(String file) {  return new ReadFile( this.getFQDNDirName()+_FS+file); }

    
    public File[] loadDirDirectory(){ return loadDirFiles(true); }
    public File[] loadDirFiles(boolean dir){  
        File[] files = this.readDir.listFiles();
        ArrayList<File> ar = new ArrayList<>();
        
        if ( isNotNullOrEmpty(files) ) {
            for ( File f : files ) {
                if ( dir ) {
                    if ( f.isDirectory() ) { ar.add(f); }
                } else {
                    if ( f.isFile()      ) { ar.add(f); }
                }
            }
        }
        File[] ret = new File[ar.size()];
        for (int i=0; i<ar.size(); i++) { ret[i]=ar.get(i); }
        return ret ; 
    }
    public String[] loadDir(boolean dir){
        String[] s; // = new String[] {};
        fileList=""; dirList="";

        //System.out.println("readDir.lastModified():"+readDir.lastModified()+":  mod:"+modified+":");
        if ( this.readDir.lastModified() == 0 ||this.readDir.lastModified() > modified ) {
             for ( File f : loadDirFiles(dir) ) {
                 if ( dir ) {
                    dirList=(dirList.isEmpty()  )?f.getName():dirList+"@"+f.getName(); 
                 } else {
                    fileList=(fileList.isEmpty())?f.getName():fileList+"@"+f.getName();
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

    public String[] getFiles(String filter) {
        ArrayList<String> ar = new ArrayList<String>();
        Pattern pa = Pattern.compile(filter);
        for ( String s : getFiles() ) {
            if ( pa.matcher(s).find()) { 
                //System.out.println("found =>"+s+"<=");
                ar.add(s); 
            }
        }
        
        String[] sp = new String[ ar.size() ];
        if ( ar.size() > 0 ) {
            for (int i=0; i<sp.length; i++ ) {  sp[i]=ar.get(i); }
        }
        return sp;
    }
    public ArrayList<ReadFile> getReadFiles(){
        ArrayList<ReadFile> ar = new ArrayList<ReadFile>();
        for ( String f : getFiles() ) {
             if (! f.isEmpty())  ar.add(new ReadFile(f));
        }
        return ar;
    }
    
    public String[] getFiles(String filter, boolean subDir) {
        final String func=getFunc("getFiles(String filter, boolean subDir)");
        String baseD=this.getDirName();
        String[] myfiles = getFiles(filter);
        printf(func,3,"find "+myfiles.length+" Files in Directories in "+baseD);
        ArrayList<String> ar = new ArrayList<String>();
        for ( String d : myfiles ) {
            if ( ! d.isEmpty() ) {
                printf(func,3,"add file "+d+" to Directories of "+baseD);
                ar.add(baseD+_FS+d);
            }
        }
        printf(func,3,"find "+ar.size()+" files in Directory "+baseD+ " check SubDir:"+subDir);
        if ( subDir ) {
            String[] ap = getDirectories();
            printf(func,3,"find "+ap.length+" Directories in "+baseD);
            if ( ap.length > 0 ) {
                String df = this.getFQDNDirName();
                for( String d : ap ) {
                    if ( ! d.isEmpty() && ! d.matches(".") && ! d.matches("..") ) {
                        ReadDir rd = new ReadDir(df+_FS+d);
                        if ( rd.isDirectory() ) {
                            printf(func,3,"run Directories in "+rd.getFQDNDirName() );
                            String[] mp = rd.getFiles(filter, subDir );
                            for ( String f : mp ) {
                                if ( ! f.isEmpty() ) {
                                    printf(func,3,"add File "+f+" from directories "+baseD);
                                    ar.add(baseD+_FS+f);
                                }
                            }
                            printf(func,2,"run complete in directory "+rd.getFQDNDirName() +" -  find "+mp.length+" files");
                        }
                    }
                }
            }
            
        }
        printf(func,3,"have found "+ar.size()+" files in "+baseD);
        String[] sp = new String[ ar.size() ];
        int i=0; 
        while( ar.size() > 0 ) {
            String l = ar.remove(0);
            printf(func,3,"update return ["+i+"/"+(sp.length-1)+"]  add "+l);
            sp[i]=l; i++; 
        }
        printf(func,3,"return "+ar.size()+"/"+sp.length+" from "+baseD+" =>"+sp.toString());
        return sp;
    }
    
    public String getParent()     { return readDir.getParent(); }
    public File   getParentFile() { return readDir.getParentFile(); }

    public File getFile() { return this.readDir; }
    public String getFQDNDirName()  { return this.readDir.toString(); }
    public String getAbsolutePath() { return this.readDir.getAbsolutePath(); }
    public String getDirName()  { return this.readDir.getName(); }

    public boolean create() {
        if ( ! readDir.exists() ) {
             readDir.mkdirs();
        }
        return ( readDir.exists() && readDir.isDirectory() )? true : false;
    }
    
    public boolean mkdir()  { return readDir.mkdir();  }
    public boolean mkdirs() { return readDir.mkdirs(); }
    
    private String backDir=System.getProperty("user.dir");
    public boolean setCurrentDirectory(String directory_name,boolean create)
    {
        backDir = System.getProperty("user.dir");
        boolean result = false;  
        
        readDir = new File(directory_name).getAbsoluteFile();
        if( ! this.isDirectory() && create ) { mkdirs(); }
        if(   this.isDirectory()           ) {
           result = (System.setProperty("user.dir", readDir.getAbsolutePath()) != null);
        }

        return result;
    }
    
    public boolean setCurrentDirectoryBack() { return setCurrentDirectory(backDir,false);  }
    
    
    public void deleteFiles() {
        String base=readDir.getAbsolutePath()+_FS;
        for ( String sp : getFiles()) {
             if ( ! sp.equals("\\.") && ! sp.equals("\\.\\.") ) {
                 WriteFile fd = new WriteFile(base+sp);
                          fd.delete();
             }
        }
    }
    
    public boolean rmdir(boolean b) {
        if (b) { deleteFiles(); }
        return readDir.delete();
    }

    public boolean isSubDirectory(String f) { return isSubDirectory(new File(f));  }
    public boolean isSubDirectory(File f) { 
        
        String o = readDir.getAbsolutePath().replaceAll(_FS, ":");
        String a = f.getAbsolutePath().replaceAll(_FS, ":");
        boolean b=( a.startsWith(o) || o.startsWith(a));
        return b;
    }
    
    public static ReadDir getTempDirectory(String fname){
        if ( fname == null || fname.isEmpty() ) { fname=".tempDir"; }
        try {
            return new ReadDir( Files.createTempDirectory(fname).toFile());
        }catch(java.io.IOException io ) {
            
        }   
        
        return new ReadDir( System.getProperty("java.io.tmpdir") );
    }
    
    public String getPath(){
        return readDir.getAbsolutePath();
    }
    
    public boolean createDirectoryIfNotExists() {
        if ( ! readDir.isDirectory() ) {
            return readDir.mkdirs();
        }
        return true;
    }
}
