/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utils.io.files;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 *
 * @author SuMario
 */
public class ReadFile {
    
    private File filer;
    private String dir;
    private String file;
    private StringBuilder sb=null;

    public ReadFile(String dir, String file){
        this(dir+File.separator+file);
    }

    public ReadFile(String nfile){
        this( new File(nfile) );
    }

    public ReadFile(File file) {
         this.filer= file;
         this.file = file.getName();
         this.dir  = file.getParent();
    }


    public void checkLog(){
        if ( sb == null ) { this.sb=this.readOut(); }
    }

    public StringBuilder readOut() {
        try{
          String line;
          sb= new StringBuilder();
          BufferedReader rb = new java.io.BufferedReader( new java.io.FileReader(filer) );
          do {
                line=rb.readLine();
                if ( line != null ) {
                    sb.append(line).append("\n");
                }
          } while ( line != null );

          if( sb.length() > 0) { sb.setLength(sb.length()-1); }   // remove last \n

        } catch (Exception e){ }

        return sb;
    }
    
    public Pattern readPattern() {
         sb= new StringBuilder();
         String line;
       try {  
         BufferedReader rb = new java.io.BufferedReader( new java.io.FileReader(filer) );
         do {
                line=rb.readLine();
                if ( line != null && ! line.isEmpty() ) {
                   if ( sb.length() > 0 ) {
                       sb.append("|");
                   } 
                   sb.append(line);
                }
         } while ( line != null );
        } catch (Exception e){ }  
        return Pattern.compile(sb.toString());
    }
    

    public StringBuilder getStringBuilderRef ( ) { checkLog(); return this.sb; }

    public String getString() { checkLog(); return (sb == null )? "":sb.toString() ; }

    private ArrayList attr ;
    public ArrayList getMap(StringBuilder readOut) {
        final String meth="getMap(StringBuilder readOut)";
        attr = new ArrayList();
        if ( readOut != null ) {
                String[] sp = readOut.toString().split("\\n");
                for ( int i=0; i<sp.length; i++) {
                    if ( ! sp[i].isEmpty() && ! sp[i].startsWith("#")) {
                            String sf[] = sp[i].split("=");
                            String k=sf[0];
                            int    j=k.length();
                            if ( (k.length()+1) < sp[i].length() ) { j++; }

                            String v=sp[i].substring( j );
                            k=k.replaceAll(" ", "");
                            v=v.replaceFirst(" ", "");
                            attr.add( new String[] {k,v}  );

                    }
                }
        }
        return attr;
    }
    
    public boolean delete() {
        
        this.filer.delete();
        
        return this.filer.exists() ? false:true ;
        
    }
    
    public boolean exist() { return this.filer.exists();  }
    public boolean isExist() { return exist();  }
    public boolean isReadableFile()  {  return ( filer.isFile() && filer.canRead()    )? true:false; }
    public boolean isWriteableFile() {  return ( filer.isFile() && filer.canWrite()   )? true:false; }
    public boolean isExecutableFile(){  return ( filer.isFile() && filer.canExecute() )? true:false; }
    
    public boolean isReadableDirectory()  {  return ( filer.isDirectory() && filer.canRead() )? true:false; }
    public boolean isWriteableDirectory() {  return ( filer.isDirectory() && filer.canWrite())? true:false; }
    
    public long getLastModified() { return filer.lastModified(); }
    
    
    public synchronized boolean create() {
        try {
            this.filer.createNewFile();
        } catch(java.io.IOException io) {
        }
        return this.filer.exists() ? true:false ;
    }
    
    
    public synchronized boolean create(boolean b) {
        if ( b ) {
            ReadDir dir = new ReadDir ( filer.getParentFile() );
            if ( ! dir.isDirectory() ) {
                if ( ! dir.mkdirs() ) { 
                    return false;
                }
            }    
        }
        return create();
    }
    
    public synchronized boolean move(File moveFile){
        boolean r = filer.renameTo(moveFile);
        if (r) {
            this.file = filer.getName();
            this.dir  = filer.getParent();
        }
        return r;
    }
    
    private long size=0;
    private long dsize=0;
    public long getCopyState() { return (dsize==0)? (long) 0 : (long) size * 100 / dsize; }
    
    public synchronized boolean copy(File copyFile){
        boolean b=false; dsize=0;
        String meth="copy(File copyFile)";
        try {
            byte[] buf = new byte[ 64*1024 ];
            
            java.io.InputStream   in = new java.io.FileInputStream(filer);
  
            java.io.OutputStream out = new java.io.FileOutputStream(copyFile);

            int len;
            while ((len = in.read(buf)) > 0){ out.write(buf, 0, len); dsize=+buf.length; }
            in.close();
            out.flush();
            out.close();
            b=true;
        } catch (Exception e) {
            b=false;
        }
        return b;
    }
    
    public String[] getInfo() {
        String[] info= { "execute:"+filer.canExecute(),  
                         "write:"+filer.canWrite(),
                         "read:"+filer.canRead(),
                         "exist:"+filer.exists(),
                         "directory:"+filer.isDirectory(),
                         "file:"+filer.isFile(),
                         "hidden:"+filer.isHidden(),
                         "size:"+filer.length(),
                         "lastmodified:"+filer.lastModified()
        };
        
        return info;
    }

    public File   getFile()         { return this.filer; }
    public String getFQDNFileName() { return this.filer.toString();}
    public String getFileName()     { return this.filer.getName(); }
    public String getDirName()      { return this.filer.getPath(); }
    public Long   getSize()         { return this.filer.length();  }
    
   
    public void save(StringBuilder sb) {
        final String meth="save(StringBuilder sb)";
        try {
            java.io.OutputStream out = new java.io.FileOutputStream(filer);
            out.write(sb.toString().getBytes());
            out.flush();
            out.close();
        } catch (Exception ex) {
        }
          
    }
    
    public StringBuilder read() {
        final String meth="read()";
        StringBuilder ab=new StringBuilder();
        try {
            java.io.BufferedReader is = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(filer)));
            String inputLine;
            while(  ( inputLine = is.readLine() ) != null ) {
                ab.append(inputLine).append("\n");          
            }
            is.close();
        }catch (Exception ex) {
        } finally {
            return ab;
        }
        
    }
    
    StringBuilder errorMsg=new StringBuilder();
    StringBuilder stdoutMsg=new StringBuilder();
    public String getStdError() { return errorMsg.toString();  }
    public String getStdOut(){ return stdoutMsg.toString(); }
    
    
    private ObjectInputStream oin=null;
    public Iterator loadObjects(Object o) {
        final String func="loadObjects(Object o)";
        java.util.HashMap m = new java.util.HashMap();
        try {
           if ( oin == null ) oin= new ObjectInputStream( new BufferedInputStream( new FileInputStream(this.filer) ) );
           Object obj;
           while ( (obj=oin.readObject() ) != null ) {
                 m.put(obj, obj);
           }
        } catch (ClassNotFoundException ex) {
        } catch(IOException io) {
        } 
        return m.values().iterator();
    }
    
    private ObjectOutputStream oout=null;
    public void storeObjects(Object f) {
        final String func="storeObjects(Object f)";
        try {
            if (oout == null) oout = new ObjectOutputStream( new BufferedOutputStream( new FileOutputStream( this.filer ) ) ); 
            if ( f == null ) { oout.flush(); oout.close(); oout=null; return ; }
            oout.writeObject(f);
        } catch(IOException io) {
        }    
    }
    
    public boolean Execute() { return Execute(null); }
    public boolean Execute(String[] evp) {
        final String func="Exceute(PrintStream ps)";
        boolean b=false;
        try  {  
            String[] cmd = { this.getFQDNFileName() };
            Runtime rt = Runtime.getRuntime();
            Process p;
            if (evp == null ) {
                p = rt.exec(cmd);
            } else {
                p = rt.exec(cmd,evp);
            }     
            BufferedReader br = new BufferedReader ( new InputStreamReader( p.getInputStream() ) );
            InputStream er = p.getErrorStream();
            while( ! br.ready() ) {
                try { Thread.sleep(1000); } catch(Exception io) {}
            }
            errorMsg=new StringBuilder();
            stdoutMsg=new StringBuilder();
             String l;
             while( ( l = br.readLine() ) != null) { 
                stdoutMsg.append(l).append("\n");
             }
             try { br.close(); }catch(IOException iom) {}
             
             if ( er.available() == 0 ) { b=true; } else {
                while( er.available() >0 ) { 
                    errorMsg.append( (char)er.read() );
                }
                if ( errorMsg.length() > 0 ) { 
                } else {
                    b=true;
                }
             }
             try { er.close(); }catch(IOException iom) {}
             
             p.destroy(); 
        } catch(Exception io) {
        }    
            
        return b;
        
    }

    public boolean append(byte[] buf) {
       try {  
        OutputStream os = new FileOutputStream(filer, true);
        os.write(buf, 0, buf.length); 
        os.close();
        return true;
       }catch(Exception e) { return false; } 
    }

    public PrintStream getPrintStream() {
        try {
            create(true);
            OutputStream os = new FileOutputStream(filer, true);
            return new PrintStream( os);
        } catch(FileNotFoundException fn) {
            fn.printStackTrace();
            return null;
        }    
    }

    
}
