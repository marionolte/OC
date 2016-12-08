/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.file;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 *
 * @author SuMario
 */
public class FileWalker {
    private File root;
    private File[] flist;
    public FileWalker(String dir) {
        setNewDirectory( dir );
    }
    
    public void setNewDirectory(String path) {
        root = new File( path );
        flist = root.listFiles();
        list();
    }

    private ArrayList<File> files;
    private ArrayList<File> dirs;
    private void list() {
        files=new ArrayList<File>();
        dirs=new ArrayList<File>();
        if (flist == null) return;
        
        for ( File f : flist ) {
            if ( f.isDirectory() ) {
                    list( f.getAbsoluteFile());
            } else {
                files.add(f.getAbsoluteFile());
                //System.out.println("add root file:"+f.getAbsoluteFile().toString());
            }
        }
    }
    private void list(File newpath) {
        File[] fl = newpath.listFiles();
        for ( File f : fl ) {
            if ( f.isDirectory() ) {
                if ( ! f.toString().matches(".") && ! f.toString().matches("..") ) {
                    dirs.add(f);
                    list( f.getAbsoluteFile());
                    //System.out.println("dir:"+f.getAbsoluteFile().toString()+":");
                }    
            } else {
                files.add(f.getAbsoluteFile());
                //System.out.println("add sub file:"+f.getAbsoluteFile().toString());
            }
        }
    }

    public String walkFiles(String pattern) {
         if ( files.isEmpty() ) { return ""; }
         Pattern pa = Pattern.compile(pattern);
         StringBuilder sw = new StringBuilder();
         for( File f : files) {
             if ( pa.matcher(f.toString()).find() ) {
                 if ( sw.length() > 0 ) {sw.append("\n"); }
                 sw.append(f.toString());
             }
         }
         return sw.toString();
    }
    
    public static void main(String[] args) {
        FileWalker fw = new FileWalker(args[0]);
        System.out.println(fw.walkFiles(args[1]));  // main stdout
    }

}