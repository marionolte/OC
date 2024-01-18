/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.macmario.general;

//import com.oracle.OraConst;
import com.macmario.io.file.ReadFile;
import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.util.logging.Logger;
import com.macmario.net.tcp.Host;

/**
 *
 * @author SuMario
 */
public abstract class Version  { //extends OraConst {
    final public static String mhfile="OC.jar";
    final public static String mh="MarioHelpService";
    final public static String mhservice="MHService - "+mhfile;
    final public static String prodauthor="Mario Nolte";
    final public static int majorVersion=0;
    final public static int minorVersion=0;
    final public static int patchVersion=5;
    final public static int fixedVersion=4;
    final public static int   libVersion=0;
    final public static int  betaVersion=1;
    
    static {
      
       try { 
        jarfile = URLDecoder.decode(Version.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8");
       } catch(Exception e) {} 
       JAVAHOME    = System.getProperty("java.home");
       OSNAME      = System.getProperty("os.name").toLowerCase();
       JAVAVERSION = System.getProperty("java.version");
       USERKEY     = System.getProperty("user.name");
       HOSTKEY     = Host.getHostname(); //System.getProperty("os.name")+System.getProperty("os.version");
       TEMPDIR     = System.getProperty("java.io.tmpdir");
       
        //System.out.println("java:"+getJavaMainVersion()+":"+getJavaMinVersion()+":");
    }
    
    /**
     *
     * @return
     */
    final public static String  getVersion()       { return ""+majorVersion+"."+minorVersion+((patchVersion==0)?"":"."+patchVersion); }
    final public static String  getFullVersion()   { return getVersion()+((fixedVersion==0)?"":"."+fixedVersion); }
    final public static String  getDebugVersion()  { return getFullVersion()+((libVersion==0)?"":"."+libVersion); }
    final public static String  getFullInfo()      { return mhservice+"/"+getFullVersion()+" - "+getProductAuthor();}
    final public static String  getProductAuthor() { return prodauthor; }
    final public static int     getLibVersion()    { return libVersion;}
    final public static String  getJavaHome()      { return JAVAHOME; }
    final public static File    getJavaCacerts()   { 
        File f=new File(JAVAHOME+File.separator+"lib"+File.separator+"security"+File.separator+"lib"+File.separator+"cacerts");
        if ( ! f.canRead() ) {
             f=new File(JAVAHOME+File.separator+"jre"+File.separator+"lib"+File.separator+"security"+File.separator+"lib"+File.separator+"cacerts");
        }
        return f; 
    }
    final public static boolean testLibVersion(String a) {
        int b =0; try { b=Integer.parseInt(a); }catch(Exception e) {}
        return (getLibVersion() == b );
    }
    
    final public static int getJavaMainVersion(){
        //System.out.println("JAVAVERSION:"+JAVAVERSION+":");
        String[] sp = JAVAVERSION.split("\\.");
        if ( sp[0].equals("1") && sp.length > 1 ) { sp[0]=sp[1]; }
        return Integer.parseInt(sp[0]);
    }
    
    final public static int getJavaMinVersion(){
        String[] sp = JAVAVERSION.split("_");
        return Integer.parseInt( ((sp.length > 1)?sp[ sp.length-1 ]:"0")   ) ;
    }
    
    final public static String getBetaVersion(){
                        if (betaVersion == 0 ) { return getFullVersion(); } 
                        return getDebugVersion()+"."+betaVersion;
    }
    final public static boolean isBeta() { return ! ( getBetaVersion().matches(getFullVersion())); }
    final public static String getBetaInfo(){
         if (betaVersion == 0) { return ""; }
         return "Beta "+betaVersion;
    }
    final public static String getJarMD5() {
        ReadFile fa = new ReadFile(jarfile);
        return fa.getMD5();
    }
    
    final public static String getLocationMD5() {
        ReadFile fa = new ReadFile(jarfile);
        return fa.getLocalMD5();
    }
    
    
    static private boolean runsleep=false;
    final public static void sleep(long l) {
        try { runsleep=true; Thread.sleep(l); } catch(Exception e){}
        runsleep=false;
    }
    final public static void wakeup() {
        try { if (runsleep){ Thread.interrupted(); } } catch(Exception e){}
    }
    
    
    public static String debugTrace=null;
    public static Logger logger = null;
    //private static FileHandler fh;  
    final public static void log(String s) {
         //if ( debugTrace == null ) {
              System.out.println(s);  // log stdout
         /*} else {
              if ( logger == null ) {
                  try {
                    logger = Logger.getLogger("debug");;     
                    fh = new FileHandler(debugTrace); 
                    SimpleFormatter formatter = new SimpleFormatter();  
                    fh.setFormatter(formatter);
                    logger.addHandler(fh); 
                  } catch( java.io.IOException io ) { 
                      logger=null; debugTrace=null; 
                      System.out.println(s); return;  //log stdout
                  }  
              }
              logger.info(s);
         }*/
    }
    
    final public static void printUsage(String s) {
        log(mhservice+"/"+(isBeta()?getBetaVersion():getVersion())+" - "+prodauthor+" "+s);
    }
    private String releaseVersion="";
    private String releaseMD5="";
    private String betaFile="";
    private String betaMD5="";
    private static       String TEMPDIR  ;
    private static final String OSNAME   ;
    private static final String JAVAHOME ;
    private static final String JAVAVERSION;
    private static final String USERKEY;
    private static final String HOSTKEY;
    public static int debug=0;
    
    public final String getTempDir() { return TEMPDIR; }
    public final void   setTempDir(String f) { setTempDir(new File(f) );}
    public final void   setTempDir(File f) { 
         if ( f != null ) {
              if ( ! f.isDirectory() ) {
                   f.mkdirs();
              }
              System.setProperty("java.io.tempdir", f.getAbsolutePath() );
         }
    }
    public final int getJavaMajor() { return Integer.parseInt( ( JAVAVERSION.split("\\.") )[1] ); }
    public final int getJavaMinor() { return Integer.parseInt( ( JAVAVERSION.split("[\\.|_]") )[2] ); }
    public final int getJavaMinorPatch() { return Integer.parseInt(  ( JAVAVERSION.split("_") )[1] ); }
    public final String getJavaVersion() { return JAVAVERSION; }
    public final String getUserKey()     { return USERKEY; }
    public final String getHostKey()     { return HOSTKEY; }
    
    public final void updateReleaseVersion(String v) { if (v != null) this.releaseVersion = v; }
    public final void updateReleaseMD5(String     v) { if (v != null) this.releaseMD5 = v;  } 
    public final void updateBetaFile(String       v) { if (v != null) this.betaFile = v;  }
    public final void updateBetaMD5(String        v) { if (v != null) this.betaMD5 = v; }
    
    public final String getRelaseFileMD5(){ return this.releaseMD5; }
    public final String getBetaFileName() { return this.betaFile; }
    public final String getBetaFileMD5()  { return this.betaMD5;  }
    public final boolean compareBetaMD5(String md5) { return (this.betaMD5.matches(md5)); }
    public final boolean compateReleaseMD5(String md5) { return (this.releaseMD5.matches(md5)); }
    
    static public final boolean isWindows() { return OSNAME.contains("win"); } 
    static public final boolean isUnix()    { return !isWindows(); } 
    static public final boolean isMac()     { return (OSNAME.contains("mac")) || (OSNAME.contains("mac")); } 
    static public final boolean isSolaris() { return (OSNAME.contains("sunos")) || (OSNAME.contains("solaris")); } 
    static public final boolean isAIX()     { return OSNAME.contains("aix");}
    static public final boolean isLinux()   { return OSNAME.contains("linux"); }
    
    
    public static String jarfile;
    
    public static PrintStream ps=null;
    public static PrintStream pserr=null;
    public synchronized static void println(String msg) { print(msg+"\n"); }
    public synchronized static void println(int lev, String msg ) { 
        if ( lev == 0 ) { 
            println(msg); 
        } else {
            if ( debug >= lev ) println("DEBUG["+lev+"/"+debug+"] "+msg);
        }
    }
    public synchronized static void print(String msg) {
        if ( ps == null ) { System.out.print(msg); } else { ps.print(msg); ps.flush(); }
    }
    public synchronized static void printerr(String msg) {
        if ( pserr == null ) { System.err.println(msg); } else { pserr.print(msg); pserr.flush(); }
    }
    public synchronized static void printf(String msg, String[] sp) {
        if ( ps == null ) {
           if ( sp.length == 0 ) { print(msg); }
           else if ( sp.length == 1 ) { System.out.printf(msg, sp[0]); }
           else if ( sp.length == 2 ) { System.out.printf(msg, sp[0], sp[1]); }
           else if ( sp.length == 3 ) { System.out.printf(msg, sp[0], sp[1], sp[2]); }
           else if ( sp.length == 4 ) { System.out.printf(msg, sp[0], sp[1], sp[2], sp[3]); }
           else if ( sp.length >= 5 ) { System.out.printf(msg, sp[0], sp[1], sp[2], sp[3], sp[4]); }
                
        } else { pserr.print(msg); pserr.flush(); }
    }
    
    
    
    public synchronized static void printf(String cName, String meth, int level, String msg ) { printf(cName+"::"+meth,level,msg); }
    public synchronized static void printf(String cName, int level, String msg ) { 
        //println(level, cName+" - "+msg);
        //println(level,cName+" - "+msg+"debug["+level+"/"+debug+"]");
        if ( level <= debug ) {
            if ( msg.contains("\n") ) {
                boolean t=false;
                for(String m: msg.split("\n")) {
                    String n=(t)?"\t":"";
                    println(level, cName+" - "+n+m);
                    t=true;
                }
            } else {
                println(level, cName+" - "+msg);
            }    
        }
    }
    
    public synchronized static void printf(String cName, int level, String msg , Exception e) { 
        if ( level >= debug ) {
             StringWriter sw = new StringWriter();
             PrintWriter  pw = new PrintWriter(sw);
             e.printStackTrace(pw);
             printf(cName, level, msg+"\n"+sw.toString());
        }
    }
    
    final public synchronized static String replacePass(String s) {
        StringBuilder sw=new StringBuilder();
        if ( s != null ) {
            for(String f : s.split("\n")) {
                if (sw.length() >0 ) { sw.append("\n"); }
                if      ( f.startsWith("PASSWORD=")       ) { f="PASSWORD=(are set)"; }
                else if ( f.startsWith("SERVERPASSWORD=") ) { f="SERVERPASSWORD=(are set)"; } 
                sw.append(f);
            }
        }
        return sw.toString();
    }
    
    
    final        public String getFunc(String func){ return this.getClass().getName()+"::"+func; }
    final static public String getFuncStatic(String func){ return Version.class.getName()+"::"+func; }
    
    
    final static public boolean getBooleanValue(String key) { return ( key!=null && ( key.equals("1") ||key.toLowerCase().equals("true") ) )?true:false; }
}
