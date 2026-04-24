/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.io.sys;

import static com.macmario.general.Version.log;
import com.macmario.io.file.ReadFile;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 *
 * @author SuMario
 */
public class MyClassLoader extends ClassLoader {
    public int debug=0; 
    
    public MyClassLoader(){
        super(MyClassLoader.class.getClassLoader());
    }

    @Override
    public Class loadClass(String className) throws ClassNotFoundException {
         return findClass(className);
    }
    
    public HashMap jarmap=new HashMap();
    public void addJar(String jar) throws IOException { if( jar != null && ! jar.isEmpty() ) addJar(new ReadFile(jar)); }
    public void addJar(ReadFile jar) throws IOException, FileNotFoundException {
        final String func="MyClassLoader::addJar(ReadFile jar)";
        ZipInputStream zis = new ZipInputStream(new FileInputStream(jar.getFile()));
        ZipEntry ze;  byte[] buf = new byte[1024];
        while( (ze=zis.getNextEntry()) != null){
                String fn = ze.getName();
                if (debug > 3 ) { log(func+" get entry :"+fn+":");}
                if ( fn.endsWith(".class")  && fn.length() > 5 ) {
                     String n=fn.substring(0, fn.length()-".class".length());
                     if (debug > 0 ) { log(func+" found class entry :"+n+": "); }
                     jarmap.put(n,jar.getFQDNFileName() );
                } else {
                    if (debug > 3 ) { log(func+" not a class entry "+fn+":"); }
                }
        }
        zis.close();
        
    }
    
    @Override
    public Class findClass(String className){
        final String func="MyClassLoader::findClass(String className)";
        byte classByte[];
        Class result=null;
        result = (Class)classes.get(className);
        if(result != null){
            return result;
        }
        
        try{
            return findSystemClass(className);
        }catch(Exception e){ 
           if ( debug > 0 ) {log(func+" system resource for "+className+" with exception "+e.getMessage() ); }
        }
        if ( debug > 1 ) {log(func+" system resource - resource not found for "+className); }
        
        String f= className.replace('.',File.separatorChar); // +".class"; 
        String classPath = (String)jarmap.get(f);
        if ( debug > 0 ) { log(func+" get classjar for |"+f+"| from: =>"+classPath+"<="); }
        
        try{
            
            if ( debug > 0 ) { log(func+" get classPath: =>"+classPath+"<= (URL) for class:"+f  );} 
            URLClassLoader clsLoader = URLClassLoader.newInstance(  new URL[]{ new URL("file://"+classPath) } );
            Class cls = clsLoader.loadClass(className);
            if ( cls != null ) { 
                if ( debug > 0 ) { log(func+" found class "+className+" with URLClassLoader classPath: =>"+classPath+"<= (URL)"  );} 
                classes.put(className, cls);
                return cls; 
            } else { 
               if ( debug > 0 ) { log(func+" not found with URLClassLoader classPath: =>"+classPath+"<= (URL)"  );} 
            }
        }catch(Exception e){
           if (debug > 0 ) { log(func+"could not get my resource from url - "+e.getMessage() );  e.printStackTrace(); }
        } 
        
        if ( debug > 1 ) {log(func+" myresource loader -  resource not found for "+className); }
        try {    
            if ( debug > 1 ) { log(func+" get classPath: =>"+classPath+"<= (local)"  );} 
            classByte = loadClassJarData(classPath, f);
            
            if ( debug >1 ) { log(func+" classByte : "+((classByte==null)?"NULL":""+classByte.length)+" f:"+f);}
            result = defineClass(f.replaceAll(File.separator, "\\."),classByte,0,classByte.length,null);
            if ( result != null ) {
               classes.put(className,result);
               return result;
            }
        }catch(Exception e){
           if (debug > 0 ) log(func+"could not get my resource "+e.getMessage() );
           //throw new RuntimeException("could not get my resource "+e.getMessage());
        } 
        if ( debug > 1 ) {log(func+" myresource loader -  resource not found for "+className); }
        
        
        
        try { 
            classPath =    ((String)ClassLoader.getSystemResource(className.replace('.',File.separatorChar)+".class").getFile()).substring(1);
            if ( debug > 0 ) { log(func+" get classPath: =>"+classPath+"<= (local)"  ); } 
            
            classByte = loadClassData(classPath);
            result = defineClass(className,classByte,0,classByte.length,null);
            classes.put(className,result);
            return result;
        }catch(Exception e){
           if (debug > 0 ) log(func+"could no get system resource "+e.getMessage() );
        } 
        
        return null;
    }

    public byte[] loadClassJarData(String jarFile, String className) {
        try {  
            JarFile jar = new JarFile(jarFile);  
            JarEntry entry = jar.getJarEntry(className + ".class");  
            InputStream is = jar.getInputStream(entry);  
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();  
            int nextValue = is.read();  
            while (-1 != nextValue) {  
                byteStream.write(nextValue);  
                nextValue = is.read();  
            }  
  
            return byteStream.toByteArray();  
            //result = defineClass(className, classByte, 0, classByte.length, null);  
            //classes.put(className, result);  
            //return result;  
        } catch (Exception e) {  
            return null;  
        }  
    }
    
    public byte[] loadClassData(String className) throws IOException{
 
        File f ;
        f = new File(className);
        int size = (int)f.length();
        byte buff[] = new byte[size];
        FileInputStream fis = new FileInputStream(f);
        DataInputStream dis = new DataInputStream(fis);
        dis.readFully(buff);
        dis.close();
        return buff;
    }
 
    public Hashtable classes = new Hashtable();

    /*final private void log(String s) {
         //if ( debugTrace == null ) {
              System.out.println(s);
    }*/
}
