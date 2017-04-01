/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.wls;

import general.Version;
import io.file.ReadDir;
import java.io.File;
import java.util.ArrayList;

/**
 *
 * @author SuMario
 */
public class WlsToolConfig extends Version{

    public static void main(String[] args) {
        WlsToolConfig w = new WlsToolConfig();
                      w.getLocation();
        if ( args.length > 0 ) {
            for(int i=0; i< args.length; i++) {
                if ( args[i].matches("-location") ){ w.setLocation(args[++i]);} 
                else {
                    w.updateConfig(args[i]);
                }    
            }
        } else {
            System.out.println("ERROR: need domaindiras property");
            System.exit(-1);
        }
        
        System.exit(0);
    }

    private ReadDir loc = null;
    public  void setLocation(String dir) {
        if ( dir == null || dir.isEmpty() ) { loc = new ReadDir(System.getProperty("user.home")+File.separator+"bin"); }
    }
    public ReadDir getLocation(){
          if ( loc == null ) { setLocation(null);}
          return loc;
    }
    
    private ArrayList<WlsDomain> ar = new ArrayList();
    public void updateConfig(String dir) {
        final String func=getFunc("updateConfig(String dir)"); 
        if ( dir == null || dir.isEmpty() ) { return; }
        ReadDir d = new ReadDir(dir);
        if (d.isReadable()){
            printf(func,2,"Dir:"+d.getFQDNDirName()+":  domain:"+d.getDirName()+":");
            WlsDomain wd = new WlsDomain(d.getDirName());
                      wd.setDomainLocation(d.getFQDNDirName());
                      
                      ar.add(wd);
        }
    }
    
    
}
