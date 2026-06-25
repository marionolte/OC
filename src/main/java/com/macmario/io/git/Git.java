package com.macmario.io.git;


import com.macmario.io.file.ReadDir;
import com.macmario.io.file.SecFile;
import com.macmario.io.thread.RunnableT;
import com.macmario.main.MainTask;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Properties;

/**
 *
 * @author SuMario
 */
public class Git extends MainTask{
    private SecFile  pwFile= null;
    private GitConfig conf = null;
    private ReadDir ind=null;
    private Properties map;
    
    public Git() { this( new String[]{} ); }
    public Git(String[] ar ) {
        map = parseArgs(ar);
    }
    
    public  void setPW(String f) { setPW(new SecFile(f)); }
    public  void setPW(SecFile f) {
        if ( f != null && f.isReadableFile() ) {
            this.pwFile = f; 
        }
    }
    String getPW() {         return (pwFile != null)? pwFile.readOut().toString():"";     }
    
    public  int init(ReadDir dir){
        this.ind=dir;
        
        int ret =runCommand(dir, "git", "init");
        System.out.println("INFO: init return "+ret);
        int a =runCommand(dir, "git", "config", "--global", "init.defaultBranch", "master");
        System.out.println("INFO: config return "+a);
        
        return ret;
    }
    
    public String[] allFilesInDirectory(ReadDir dir) {
        return dir.getFiles();
    }

    public int addRemote(String repo) { return addRemote(ind,repo); }
    public int addRemote(ReadDir dir,String repo) {
        int ret =runCommand(dir, "git", "remote", "add", "origin", repo);
        System.out.println("INFO: add repo "+repo+" return "+ret);
        return ret;
    }
    
    public String getRepo()  {return getRepo(ind, "master"); }
    public String getRepo(ReadDir dir) { return getRepo(dir, "master"); }
    public String getRepo(ReadDir dir, String branch) {
        if ( conf == null ) {
            final String f= File.separator;
            String _conf = f+".git"+f+"confg";
            conf=new GitConfig( dir.getAbsolutePath()+_conf );
        }
        if ( branch == null || branch.isEmpty() ) { 
             branch = "master";
        }
        return conf.getRepo(branch);
    }
    
    public String[] getBranches() { return getBranches(ind); }
    public String[] getBranches(ReadDir dir) {
        int ret =runCommand(dir, "git", "branch");
        String[] spr = new String[]{};
        if ( ret == 0 ) {
            
        } 
            
        return spr;
    }
    
    public int cloni(){ return clone(ind); }
    public int clone(ReadDir dir) {
        String repo = getRepo(dir);
        int ret =runCommand(dir, "git", "clone", repo);
        System.out.println("INFO: clone return "+ret);
        return ret;
    } 
    
    public int pull() { return pull(ind); }
    public int pull(ReadDir dir) {
        System.out.println("INFO: pull in "+dir.getFQDNDirName());
        int ret =runCommand(dir, "git", "pull");
        System.out.println("INFO: pull return "+ret);
        return ret;
    }
    
    public int commit(String message) { return commit(ind,message); }
    public int commit(ReadDir directory, String message) {
        return runCommand(directory, "git", "commit", "-m", message);
    }
    
    public int stage() throws IOException, InterruptedException { return stage(ind); }
    public int stage(ReadDir directory) throws IOException, InterruptedException {
        return runCommand(directory, "git", "add", "-A");
    }
    
    public int gitGc() { return gitGc(ind); }
    public int gitGc(ReadDir directory) {
        return runCommand(directory, "git", "gc");
    }

    
    private OutputStream in = null;
    synchronized private int runCommand(ReadDir dir, String... command) {
		Objects.requireNonNull(dir, "directory");
                
		if (! dir.isDirectory() ) {
		    throw new RuntimeException("ERROR: can't use directory  '" + dir + "'");
		}
                
		ProcessBuilder pb = new ProcessBuilder().command(command).directory(dir.getFile());
                               //pb.redirectErrorStream();
                int exit=-1;
                try {
                    msg = new StringBuilder();
                    Process p = pb.start();
                            
                    SGobbler errorGobbler  = new SGobbler(p.getErrorStream(), "ERROR", this);
                    SGobbler outputGobbler = new SGobbler(p.getInputStream(), "OUTPUT", this);
                    this.in =p.getOutputStream();
                    outputGobbler.start();
                    errorGobbler.start();
                    sleep(100);
                    exit = p.waitFor();
                    //errorGobbler.join();
                    //outputGobbler.join();
                }catch(java.io.IOException|java.lang.InterruptedException io ){ 
                    System.out.println("ERROR: "+io.getMessage());
                }    
		return exit;
    }
    
    public void response(String[] args) {
        System.out.println("git commands ? "+((args!=null)?"Yes="+args.length:"No"));
        if ( args.length > 1 ) {
            System.out.println("REPO: "+ind.getFQDNDirName() );
            for ( int i=1; i< args.length; i++ ) {
                if      ( args[i].equals("--pwFile") ) { setPW(args[++i]); }
            }
            for ( int i=1; i< args.length; i++ ) {
                System.out.println(i+"=>"+args[i]+"<=");
                if      ( args[i].equals("-repo")  ) { addRemote(ind, args[++i] ); }
                else if ( args[i].equals("-pull")  ) { pull(ind);  }
                else if ( args[i].equals("-clone") ) { clone(ind); }
                else if ( args[i].startsWith("--") ) { i++; }
                else {
                    System.out.println("unknown git command:"+args[i]);
                }
            }
        } else {
            System.out.println("no length");
        }
    }
    
    public static Git getInstance(String[] args ) {
         Git git = new Git();
            //System.out.println("args[0]:"+args[0]);
        String s = (args != null && args.length > 1)?args[0]:System.getProperty("user.dir");
        ReadDir ind = new ReadDir(s);
               
        
        int test= git.init(ind);
          
        return git;
    }
    public static void main(String[] args) {
        
        Git git = getInstance(args);
            
        git.response(args); 
             
    }
    
    StringBuilder msg = new StringBuilder();
    synchronized private void addLine(String line) {
       if ( ! msg.isEmpty() ) { msg.append("\n"); } 
       msg.append(line);
    }
    
    private class SGobbler extends RunnableT {

		private final InputStream is;
		private final String type;
                private final Git git;

		private SGobbler(InputStream is, String type, Git g) {
                        super();
			this.is = is;
			this.type = type;
                        //System.out.println("created "+type);
                        this.git = g;
		}

		@Override
		public void run() {
                        setRunning();
                        //System.out.println("running:"+type);
                    
			try (BufferedReader br = new BufferedReader(new InputStreamReader(is));) {
				String line;
				while ((line = br.readLine()) != null) {
                                        git.addLine(line);
					System.out.println(type + "> " + line);
                                        String l=line.toLowerCase().trim();
                                        if ( l.contains("passphrase") || l.contains("passwor") ) {
                                            this.git.in.write( (git.getPW()+"\n").getBytes() );
                                        }
				}
			} catch (NullPointerException | IOException ioe) {
                                System.out.println("ERROR - "+ioe.getMessage());
				ioe.printStackTrace();
			}
                        setClosed();
		}
	}
}
