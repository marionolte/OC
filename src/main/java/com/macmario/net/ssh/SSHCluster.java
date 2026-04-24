/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.ssh;

import com.macmario.io.file.ReadDir;
import com.macmario.io.file.ReadFile;
import com.macmario.io.thread.RunnableT;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * @author SuMario
 */
public class SSHCluster extends RunnableT {

    private  ReadDir  userConndir;
    private  boolean  valid=true;
    private  ReadFile hostsfile;
    private  ArrayList<SSHshell> hostlist=new ArrayList();
    private  final HashMap<String, String> imap;
    private  final HashMap<String, HashMap<String, String>> hmap=new HashMap();
    
    private SSHCluster(String[] args) {
        final String func=getFunc("SSHCluster(String[] args)");
        imap = com.macmario.io.lib.IOLib.scanner(args, myusage);
        printf(func,3,"imap =>"+imap);
        this.userConndir=new ReadDir( System.getProperty("user.dir") );
        if (imap.containsKey("-conndir") && ! imap.get("-conndir").equals( imap.get("_default_-conndir")) ) {
            this.userConndir=new ReadDir( imap.get("conndir") ); 
        }
        if ( ! this.userConndir.isDirectory() ) { 
            System.out.println("ERROR: "+imap.get("-conndir")+" is not a directory for connecttion files");
            this.valid=false; 
        }
        
        this.hostsfile=new ReadFile( System.getProperty("user.dir")+File.separator+"hosts");
        if ( imap.containsKey("-hostfile") && ! imap.get("-hostfile").equals(imap.get("_default_-hostfile"))) {
            this.hostsfile=new ReadFile( imap.get("-hostfile") );
        }
        if ( ! this.hostsfile.isReadableFile() ) { 
            System.out.println("ERROR: "+this.hostsfile.getFQDNFileName()+" are not exist");
            this.valid=false; 
        }
        
        if (valid && imap.containsKey("-hosts") && ! imap.get("-hosts").equals(imap.get("_default_-hosts")) ) {
            valid=true;
        } else {
            System.out.println("ERROR: no hosts defined");
            valid=false;
        }
    }
    
    final public static String myusage="usage() sshcluster\n [-hosts <hostlist or or hostgrp>] [-hostfile <ansible similar host hostfile>] [-conndir <connection dirctory for connection files>] <command>\n";
    public void usage() {
        System.out.println(myusage+"");
    }
    public static SSHCluster getInstance(String[] args) {
        SSHCluster sc = new SSHCluster(args);
        
        return sc;
    }

    public boolean isValid() { return this.valid; }
    
    private void loadHosts() {
        final String func=getFunc("loadHosts()");
        ReadFile rf = new ReadFile(imap.get("-hosts") );
        String area="all";
        this.hmap.put(area, new HashMap<String,String>());
        for ( String s : rf.readOut().toString().split("\n") ) {
            s = s.trim();
            if ( s.isEmpty() || s.startsWith("#")) {}
            else if ( s.startsWith("[") ) { area=s.substring(1, s.length()-1);  
                                            printf(func,2,"new area are:"+area+":");
                                            if ( this.hmap.get(area) == null ) { this.hmap.put(area, new HashMap<String,String>());}
            }
            else {
                
            }
        }
        
        HashMap<String,String> hm = this.hmap.get("all");
        Iterator<String> itter = this.hmap.keySet().iterator();
        while ( itter.hasNext() ) {
            String ar = itter.next();
            if ( ar.equals("all") ) {} else {
                HashMap<String,String> hi = this.hmap.get(ar);
                
            }
        }
    }
    private void startConnection() {
        final String func=getFunc("startConnection()");
        String[] sp = imap.get("-hosts").split(",");
        for( String a : sp ) {
            if ( this.hmap.isEmpty() ) { loadHosts(); }
            printf(func,3,"host:"+a);
        }
    }
    
    @Override
    public void run() {
        startConnection();
        setRunning();
        while( ! isClosed() ) {
            if ( ! this.hostlist.isEmpty()  ) {
                sleep(100);   
            } else {
                sleep(300);
            }    
        }
        setRunning();
    }
    
}
