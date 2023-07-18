package main;

import static com.macmario.io.lib.IOLib.execReadToString;
import java.io.IOException;
import java.util.HashMap;
import com.macmario.net.tcp.TcpHost;

public class Watcher extends TcpHost{

    private boolean   _os=false;
    
    private boolean  _mem=false;
    private boolean  _cpu=false;
    private boolean _port=false;

    private int tport=0;
    private int tcount=0;

    public Watcher(String[] args) {
        for(int i=0; i<args.length; i++){
            if ( args[i].matches("-d") ) { debug++; }
            if ( _os ) {
                if ( args[i].matches("-port") ) {  
                     _port=true;
                     if ( isPort(args[++i] )) {
                            tport = Integer.parseInt(args[i]);
                     } 
                }
                else if ( args[i].matches("-count") ) { 
                     int c= isInteger(args[++i]);
                     if ( c > 0 ) { tcount=c; }
                }
                else if ( args[i].matches("-mem") ) { _mem=true;}
                else if ( args[i].matches("-cpu") ) { _cpu=true;}
            }
            if ( args[i].matches("-os") ) { this._os=true; }
        }

    }


    public void testOSPort() throws IOException {
        String[] sp = execReadToString("netstat -an").split("\n");

        for (String s : sp) {
            if ( s.startsWith("tcp") ) {
                HashMap<String,String> ma = new HashMap<String,String>(); int i=0; String ip,po="";
                String[] tp = s.split("[\\t|\\ ]");
                for(String a:tp){if (!a.isEmpty() ){  ma.put(""+i++, a); } }
                System.out.println(ma);
                String[] at=splitIPPort(ma.get("3")); String[] at1=splitIPPort(ma.get("4"));
                System.out.println("get:"+ma.get("5")+":\t:"+at[0]+":-:"+at[1]+":\t:"+at1[0]+":-:"+at1[1]+":");
            }
        }
                
    }

    public void testOSCpu() {

    }

    public void testOSMem() {

    }

    public void test() throws IOException {
       if (_os ) { 
          if ( _port ) { testOSPort(); }
          if ( _cpu  ) { testOSCpu(); }
          if ( _mem  ) { testOSMem(); }
       }
    }

    private String[] splitIPPort(String inf) {
       String[] at = inf.split("[:,\\.]");
       StringBuilder ip=new StringBuilder(at[0]);
       if ( at.length>2){
          for(int i=1; i< at.length-2 ;i++) { ip.append(":").append(at[i]); }
       }
       
       return new String[]{ ip.toString(),(at[at.length-1].equals(at[0]))?"":at[at.length-1] };
    }

    public static String usage() {
        return "-os [ -port <portnumber>] [ -count <count>]";
    }

    public static void main(String[] args) throws Exception{
        if ( args.length > 0  ) {
            Watcher w = new Watcher(args);
                    w.test();
        } else {
            System.out.println("usage() java -cp "+jarfile+" main.Watcher "+usage());
        }
        
    }

    
}

