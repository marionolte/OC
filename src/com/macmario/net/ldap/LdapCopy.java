/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.ldap;

import com.macmario.io.buffer.RingBuffer;
import com.macmario.net.ldap.main.LdapMain;
import com.macmario.io.file.ReadFile;
import com.macmario.io.file.SecFile;
import com.macmario.io.thread.RunnableT;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import javax.naming.CompositeName;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;
import com.macmario.net.tcp.Host;

/**
 *
 * @author SuMario
 */
public class LdapCopy extends LdapMain{

    private  String modprotocol;
    private  String host;
    private  String modhost;
    private  int    modport;
    private  String modbaseDN;
    private  String moduserdn;
    private  String moduserpw;
    private  String template;
    private boolean restiktCopy;
    private boolean originCopy;
    
    private LdapCopy() { this(new String[]{}); }
    private LdapCopy(String[] ar) {  
        scanner(ar,"ldapcopy "+myusage); 
        lcInit(); 
    }
    private void lcInit() {
        final String func=getFunc("lcInit()");
        printf(func,4,"created");
        
        if ( usage ) { throw new RuntimeException("property issue found"); }
        this.protocol   =(map.get("-ssl").equals("true"))?"ldaps":"ldap";
        this.modprotocol=(map.get("-modssl").equals("true"))?"ldaps":this.protocol;
        printf(func,3,"protocol:"+protocol+":  modprotocol:"+modprotocol+":");
        
        this.host=((map.get("-h").equals("hostname")?Host.getHostname():map.get("-h")));
        this.modhost=( (  map.get("-modh").equals(map.get("_default_-modh") )  )?this.host:map.get("-modh"));
        printf(func,3,"host:"+host+":  modhost:"+modhost+":");
        
        try {
            this.port=Integer.parseInt(map.get("-p"));
            if ( this.port < Host.getMinPort() || this.port > Host.getMaxPort() ) {
                throw new RuntimeException("not a port  min:"+(this.port < Host.getMinPort())+" max:"+(this.port > Host.getMaxPort()));
            }
        } catch (Exception e) {
            printf(func,3,"excetion :"+e.getMessage()+" for port:"+map.get("-p"));
            this.port=(this.protocol.equals("ldaps"))?636:389;
        }
        printf(func,3,"port:"+port+":  host:"+host+":");
        
        try {
            this.modport=Integer.parseInt(map.get("-modp"));
            if ( this.modport < Host.getMinPort() || this.modport > Host.getMaxPort() ) {
                throw new RuntimeException("not a port");
            }
        } catch (Exception e) {
            this.modport=(this.modprotocol.equals("ldaps"))?636:389;
        }
        printf(func,3,"modport:"+modport+":  modhost:"+modhost+":");
        
           this.baseDN=(map.get("-b" ).equals(map.get("_default_-b" )))?getDefaultBaseDN():map.get("-b" );
        this.modbaseDN=(map.get("-bc").equals(map.get("_default_-bc")))?getDefaultBaseDN():map.get("-bc");        
        printf(func,3,"baseDN:"+baseDN+":  modbaseDN:"+modbaseDN+":");
        
        
        //   this.userdn=(map.get("-D" ).equals("adminDN"))?"cn=admin":map.get("-D" );
        this.moduserdn=(map.get("-modD" ).equals(map.get("_default_-modD" )))?this.userdn:map.get("-modD" );
        printf(func,3,"userdn:"+userdn+":  moduserdn:"+moduserdn+":");
        
        filter=(! map.get("-f" ).equals(map.get("_default_-f")))?map.get("-f" ):"objectclass=*";
        
        //System.out.println("userpw1:"+userpw);
        /*this.userpw=( ! map.get("-j"    ).equals(map.get("_default_-j"))    )?  (new SecFile(  map.get("-j"   ) ).readOut().toString() ):"";
        if ( this.userpw.isEmpty() ) {
             System.out.println("userpw:"+userpw);
             this.userpw=( ! map.get("-w"    ).equals(map.get("_default_-w"))    )? map.get("-w") : "";
        }*/
        //System.out.println("userpw2:"+userpw);
        this.moduserpw=( ! map.get("-modj" ).equals(map.get("_default_-modj")) )?  (new SecFile(  map.get("-modj") ).readOut().toString() ):this.userpw;
        //System.out.println("moduserpw2:"+moduserpw);
        auth="simple";
        
        this.template= ( ! map.get("-t").equals(map.get("_default_-t")))? ( new ReadFile( map.get("-t") ).readOut().toString() ):"#template: all";
        for( String s: this.template.split("\n")) {
            if ( ! s.isEmpty() ) {
                    String[] sp = s.split(" ");
                    String attr = sp[0].substring(0,sp[0].length()-1);
                    printf(func,3,"attribute:"+attr);
                    if ( tmap.get(attr) == null ) {  tmap.put(attr.toLowerCase(), new HashMap<String,String>());}
                    HashMap<String, String> tp = tmap.get(attr.toLowerCase());
                    if ( sp.length < 1 ) {  tp.put(attr.toLowerCase(), ""); }
                    else {
                      for ( int i = 1; i< sp.length; i++ ) {
                        String[] at = sp[i].split("\\|");
                        tp.put(at[0].toLowerCase(), (at.length <= 1)?at[0]:sp[i].substring(at[0].length()+1));
                        if ( sp[i].indexOf('|') > 0 && tp.get(at[0].toLowerCase()).equals(at[0].toLowerCase()) ) {
                          tp.put(at[0].toLowerCase(),"");
                        }
                        printf(func,2,"attribute:"+attr+" "+at.length+" update with :"+sp[i]+":  =>"+at[0]+"="+tp.get(at[0])+"<= =>"
                                +at[0].toLowerCase()+"="+tp.get(at[0].toLowerCase()+"|<="));
                      }  
                    }
                    if ( debug  > 0 ) {
                        Iterator<String> itter = tp.keySet().iterator();
                        while(itter.hasNext()) {
                            String a=itter.next();
                            printf(func,2, "set for attribute:"+attr.toLowerCase()+":  the pair  =>|"+a+"="+tp.get(a)+"|<=");
                        }
                    }
            }
        }
        HashMap<String, String> tp = tmap.get("#template");
        this.restiktCopy=( tp.get("strict") != null && tp.get("strict").equals("strict"));
        this.originCopy =( tp.get("origin") != null && tp.get("origin").equals("origin"));
        
        printf(func,2,"tp:origin:"+tp.containsValue("origin")+":   :"+tp.get("origin")+":"+tp);
        
        int size=this.getPageSize();
        try {
            size=Integer.parseInt(map.get("-pg"));
        } catch (Exception e){}
        this.setPageSize(size);
        
        printf(func,2,"local:"+protocol+"://"+host+":"+port+"?"+userdn+"&"+userpw+"&"+baseDN+"&"+filter+"&\n"+
                      "remote:"+modprotocol+"://"+modhost+":"+modport+"?"+moduserdn+"&"+moduserpw+"&"+modbaseDN+"&\n"+
                      "filter:"+filter+":   objectlist:"+getAttrList());
    }
    
    private String[] getAuthHash(boolean b,String f) {
        
        ArrayList<String> a = new ArrayList();
             for ( int i=1; i<= debug; i++) { a.add("-d"); }
     if ( protocol.equals("ldaps") ){ a.add("-ssl"); }
             a.add("-h"); a.add(    (b)?hostname:modhost );
             a.add("-p"); a.add(""+((b)?port:modport)   );
             a.add("-D"); a.add(    (b)?userdn:moduserdn );
             a.add("-w"); a.add(    (b)?userpw:moduserpw );
     if(f.equals("search")){  
             a.add("-f"); a.add(filter); 
             a.add("-pg");a.add(""+this.getPageSize());
             a.add("-b"); a.add(    (b)?baseDN:modbaseDN );
     }
             a.add("-a"); a.add(    (b)?auth:auth);
             
             
          String[] ab = new String[ a.size() ];
             for ( int i=0; i< ab.length; i++ ) { ab[i]=a.get(i); }
          return ab;
    }
    
    LdapSearch ls  = null;
    LdapSearch lss = null;
    LdapSearch lms = null;
    LdapModify lmm = null;
    
    private SearchControls getNewSearchControl() {
        SearchControls tls = new SearchControls();
                       tls.setReturningAttributes(new String[]{"ALL"});
                       tls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        return tls;    
    }
    
    private LdapCopySearch lcs = null;
    private LdapCopyModify lcm = null;
    private boolean searchRun=false;
    synchronized public void copy()  {
        final String func=getFunc("copy()");
        printf(func,4,"copy dn's  from "+map.get("-b")+" to "+map.get("-bc"));
        this.searchRun=true;
        printf(func,3,"start LdapCopySearch");
        ReadOK.clear(); 
        ReadNOK.clear();
        lcs = new LdapCopySearch(this); lcs.start();
        lcm = new LdapCopyModify(this); lcm.start();
        printf(func,3,"start LdapCopySearch completed");
        byte[] cookie = null;
        Name ename=null;    
        try {
            //ls = LdapSearch.getInstance(new String[]{ (   protocol.equals("ldaps"))?"-ssl":"", "-h",hostname, "-p",""+port,    "-D",   userdn, "-w", userpw, "-f",filter, "-a", auth, "-b", baseDN});
            ls = LdapSearch.getInstance( getAuthHash(true, "search") );
            ls.getLdapContext().setRequestControls(new Control[] {new PagedResultsControl( getPageSize(), Control.CRITICAL) });
            
            lss = LdapSearch.getInstance( getAuthHash(true, "search") );
            lss.getLdapContext().setRequestControls(new Control[] {new PagedResultsControl( getPageSize(), Control.CRITICAL) });
            
                     
             
            long loop=0;
            do {
                printf(func,3,"perform search to "+getBaseDN()+" with :"+getFilter()+":");
                //performing the search
                NamingEnumeration results = ls.getLdapContext().search( getBaseDN(), getFilter(), getNewSearchControl() );
                                            
                printf(func,3,"result from search:"+results);
                
                checkedDN.put(map.get("-bc").toLowerCase(), "false");
                boolean b=true; 
                while (results != null && results.hasMore()) {
                        
                        
                        SearchResult ent = (SearchResult) results.next();
                        
                        if ( ent.getNameInNamespace().equals( map.get("-b") ) || ent.getNameInNamespace().indexOf(",") <= map.get("-b").indexOf(",") ) {
                           continue;
                        }
                        
                        printf(func,3,"read entry "+ent);
                        
                        if ( ent != null ) {
                            
                            printf(func,3,"found entry "+ent+"   dn:"+ent.getNameInNamespace()+":");
                            ename = new CompositeName().add( (ent.getNameInNamespace() ) );
                            printf(func,3,"user entries for dn:"+ename+"   =>"); 
                            rbuf.push(ename);
                            LookupOK.add(ename);
                            
                        } else {
                          printf(func,1,"break - not found");  
                          b=false;
                        }
                        
                        if (b) {
                            Control[] controls = ls.getLdapContext().getResponseControls();
                            if (controls != null) {
                                for (int i = 0; i < controls.length; i++) {
                                      if (controls[i] instanceof PagedResultsResponseControl) {
                                         PagedResultsResponseControl prrc = (PagedResultsResponseControl) controls[i];
                                         cookie = prrc.getCookie();
                                      }
                                 }
                            }  
                        }      
                }  
            } while (cookie != null);   
            
        } catch (IOException io ) {
          printf(func,1,"transport entries to ends with io error "+io.getMessage());
          LookupNOK.add(ename);
        } catch ( NamingException ne ) {
          printf(func,1,"transport entries to ends with naming error "+ne.getMessage());   
          LookupNOK.add(ename);
        }    
        
        this.searchRun=false;
        printf(func,4, "wait until rbuf is not empty");
        while( ! rbuf.isEmpty() || ! mbuf.isEmpty() ) { sleep(300);}
        System.out.println("Copy Update status");
          System.out.println("\tLookUp      \t\tOK:"+LookupOK.size()+"\t\tNOT OK:"+LookupNOK.size());
        if ( LookupNOK.size() > 0) {  
          System.out.println("\t\tLookup Errors:");
         for(int i=0; i< LookupNOK.size();i++) {
          System.out.println("\t\t\t"+LookupNOK.get(i));    
         }
        }            
          System.out.println("\tRead for Update\t\tOK:"+  ReadOK.size()+"\t\tNOT OK:"+  ReadNOK.size());
        if ( ReadNOK.size() > 0) {  
          System.out.println("\t\tRead Errors:");
         for(int i=0; i< ReadNOK.size();i++) {
          System.out.println("\t\t\t"+ReadNOK.get(i));    
         }
        }  
          System.out.println("\tModification\t\tOK:"+   ModOK.size()+"\t\tNOT OK:"+   ModNOK.size());
        if ( ModNOK.size() > 0) {  
          System.out.println("\t\tModification Errors:");
         for(int i=0; i< ModNOK.size();i++) {
          System.out.println("\t\t\t"+ModNOK.get(i));    
         }
        }
        printf(func,4,"copy complete");
    }
    
    private ArrayList<Name> ReadOK    = new ArrayList();
    private ArrayList<Name> ReadNOK   = new ArrayList();
    private ArrayList<Name> ModOK     = new ArrayList();
    private ArrayList<Name> ModNOK    = new ArrayList();
    private ArrayList<Name> LookupOK  = new ArrayList();
    private ArrayList<Name> LookupNOK = new ArrayList();
    
    private HashMap<String,HashMap<String,String>> tmap = new HashMap<String,HashMap<String,String>>(); 
    
    private String getNewDN(String old,String base, String newbase) {
        final String func=getFunc("getNewDN(String old,String base, String newbase)");
        int savdebug=debug; 
        //debug=3;        
        HashMap<String,String> tp = tmap.get("dn");
        if ( tp == null ) { tp= new HashMap<String,String>(); tmap.put("dn", tp); }
        if ( ! tp.isEmpty() ) {
            Iterator<String> itter = tp.keySet().iterator();
            while(itter.hasNext()) {
                String a = itter.next();
                if ( a.equals("{replace")) {
                    String v = tp.get(a).replace("(","").replace(")}", "").toLowerCase();
                    printf(func,3,"itter:"+a+":  has value:"+v+":");
                    for ( String s : v.split(";")) {
                        if ( ! s.isEmpty() ) {
                          printf(func,3," s =>"+s+"<=");  
                          String[] at = s.split("\\|");
                                   at = new String[] { at[0], ((at.length>1)?at[1]:"") };
                          printf(func,3, "replace :"+at[0]+":  with:"+at[1]+":  old=>"+old+"<=");
                          old=old.toLowerCase().replaceAll(at[0], at[1]);
                          printf(func,3, "after replace =>"+old+"<=");
                          
                        }  
                    }
                }    
                
            }
        }
        
        String[] sp = old.substring(0, old.length()-base.length() ).split(",");  String[] at=base.split(",");
        StringBuilder sw = new StringBuilder();
        
        printf(func,3,"check:"+old+":  base:"+base+":  newbase:"+newbase+":" + " tp =>"+tp);
            
        for( int i=0; i<(sp.length-(at.length-1));i++ ) {
            if ( sw.length() >0 ) {sw.append(","); }
            printf(func,3,"check:"+sp[i]+" against : "+tp.get(sp[i].toLowerCase()));
            if ( tp.get(sp[i].toLowerCase()) != null ) { sp[i]=tp.get(sp[i].toLowerCase()); }
            sw.append(sp[i]);
        }
        final String f=sw.toString()+(( sw.length() > 0)?",":"")+newbase;
        printf(func,3,"from:"+old+":  moved to:"+f+":");
        debug=savdebug;
        return f;
    }
    
    final private HashMap<String,String> checkedDN=new HashMap<String,String>();
    
    final static public String myusage="\nusage():\noption: [-h hostname] [-p port] [-ssl] [-D adminDN] [-j passwordfile] [-modh modifyHost] [-modp port] [-modD adminDN] [-modj passwordfile] [-modssl] [-b baseDN] [-bc copyBaseDN] [-f filter] [-t copytemplate] [-pg <page-sizelimit>] [objectlist]\n"
            + "\n\t\texample template file:\n"
            + "\t\t\t\t#template: strict\n" 
            + "\t\t\t\tdn: {replace(ou=People|ou=Users;ou=subtree|)\n" 
            + "\t\t\t\tobjectClass: top person posixAccount inetOrgPerson\n" 
            + "\t\t\t\tgidNumber: 1000\n" 
            + "\t\t\t\tsn:\n" 
            + "\t\t\t\tgivenname:\n" 
            + "\t\t\t\tmail: __givenname__.__sn__@example.com\n" 
            + "\t\t\t\tuid: __givenname(0,1)__sn(0,5)__\n" 
            + "\t\t\t\tuidnumber: {autoinc(1000)}\n" 
            + "\t\t\t\tloginShell: /bin/bash\n" 
            + "\t\t\t\thomeDirectory: /home/__uid__\n" 
            + "\t\t\t\tcn: __uid__\n" 
            + "\t\t\t\tuserpassword: 1234\n"
            + "\n"
            + " \tInformation to the template:\n"
            + " \t\t__ \t\t\tbefore and after will take a value from a ldap attribute\n"
            + " \t\t(0,5)  \t\t\ttake a substring from 0 to 5 \n"
            + " \t\tou=People|ou=Users \treplace  in the dn the ou=People part with ou=Users\n"
            + " \t\t{replace(ou=People|ou=Users;ou=subtree|) replace ou=People to ou=Users and removes ou=subtree\n"
            + " \t\tautoinc(1000) \twill generate an auto integer incrimental value starting with the base 1000\n"
            + " \t\tstrict in the #template\tthis with take over only the names attributes\n"
            + "\n";
    final static public String free="true";
    
    public static LdapCopy getInstance(String[] ar) {
       return new LdapCopy(ar); 
    }
    
    public static LdapCopy getInstance() { return new LdapCopy(); }
    
    public static void main(String[] args) throws Exception {
         LdapCopy lc = getInstance(args);
         if ( lc != null && ! lc.usage ) {
                  lc.debug=debug;
                  lc.copy();
         }
    }
    
    private RingBuffer<Name> rbuf = new RingBuffer(this.getPageSize());
    private RingBuffer<HashMap<String, ArrayList<String>>> mbuf = new RingBuffer<HashMap<String, ArrayList<String>>>(this.getPageSize());
    
    private HashMap<String, ArrayList<String>> getLdapHash(SearchResult entry) {
       final String func=getFunc("getLdapHash(SearchResult entry)"); 
       printf(func,4,"incoming");
       HashMap<String, ArrayList<String>> imp = new HashMap<String, ArrayList<String>>();
                        
       ArrayList<String> ar = new ArrayList<String>(); ar.add(entry.getNameInNamespace());
       imp.put("dn", ar);
       printf(func,3,"dn: "+entry.getNameInNamespace() +" entry:"+entry); 
                               
       try {
            Attributes attr = entry.getAttributes();
            printf(func,3,"Attributes:"+attr);
            NamingEnumeration en = attr.getAll();
            while(en!= null && en.hasMore() ) {
                 Attribute at = (Attribute) en.next();
                 ar = new ArrayList<String>();
                 printf(func,3,"Attribute:"+at);
                 String id=at.getID().toLowerCase();
                 printf(func,3,"id:"+id+":  :"+at.toString().substring(at.getID().length()+2)+":");
                 String sp[]  = at.toString().substring(at.getID().length()+2).split(",");
                 for (int i=0; i<sp.length; i++) {
                          printf(func,4,"i="+i+":   sp[]=:"+sp[i]+":");
                          Object ob=at.get();

                          String v= (( ob instanceof byte[] )? new String((byte[]) ob ):sp[i]).replaceAll("^ ", "");
                          printf(func,4,id+": "+v);
                          ar.add(v);
                 }
                 printf(func,3,"put list  for:"+id);
                 imp.put(id, ar);
            }
       } catch(NamingException ne) {
           printf(func,1,"readout entry ends with error "+ne.getMessage(),ne);
       }
       printf(func,4,"closing");
       return imp;
    } 
    
    private class LdapCopySearch extends RunnableT{

        private final LdapCopy lc;
        
        LdapCopySearch(LdapCopy lc) {
            this.lc=lc;
        }
        
        
        @Override
        public void run() {
            final String func=getFunc("run()");
            printf(func,4,"starting "+this);
            setRunning(); lc.rbuf.debug=lc.debug;
        
            ArrayList<String> a = new ArrayList<String>();  a.add("ALL");
            while( this.lc.searchRun || ! lc.rbuf.isEmpty() ) {
                if ( ! lc.rbuf.isEmpty() ) {
                    Name o = (Name) lc.rbuf.popObject();
                    try {
                        NamingEnumeration namEnum = lc.lss.search(o.toString(), "objectclass=*", a );
                        printf(func,3,"res:"+namEnum);
                        
                        while (namEnum != null && namEnum.hasMore()) {
                              final SearchResult entry = (SearchResult) namEnum.next();
                              final String n = entry.getNameInNamespace();
                              printf(func,3,"dn: "+n+" entry:"+entry); 
                              final HashMap<String, ArrayList<String>> imp = lc.getLdapHash(entry);
                              printf(func,3,"dn: "+n+" ldap has generated"); 
                              ReadOK.add(o);
                              mbuf.push(imp);
                        }    
                        
                    } catch(NamingException ne) {
                        printf(func,1,"lookup error for "+o+" - reason "+ne.getMessage(),ne);
                        ReadNOK.add(o);
                    } catch(IOException io) {
                        printf(func,1,"lookup error for "+o+" - reason "+io.getMessage(),io);
                        ReadNOK.add(o);
                    }   
             
                    printf(func,3,"pop name:"+o+":");
                } else {    
                   sleep(300);
                }    
            }
            printf(func,4,"closing "+this+"   "+lc.rbuf.getSize()+"  check mbuf:");
            
            setRunning();
            printf(func,4, "closed "+this+" - done");
        }
        
    }
    private class LdapCopyModify extends RunnableT {

        private final LdapCopy lc;
        
        LdapCopyModify(LdapCopy lc) {
            this.lc=lc;
        }

        @Override
        public void run() {
            final String func=getFunc("run()");
            printf(func,4,"starting "+this);
            setRunning(); lc.mbuf.debug=lc.debug;
        
            while( this.lc.searchRun || ! lc.rbuf.isEmpty() || ! lc.mbuf.isEmpty() ) {
                
                if ( ! lc.mbuf.isEmpty() ) {
                    HashMap<String, ArrayList<String>> imp = (HashMap<String, ArrayList<String>>) lc.mbuf.popObject();
                    if ( debug > 2 ) printHash(imp);
                    try { modifyHash(imp); } catch(InvalidNameException ine) {}
                } else {
                    sleep(300);
                }
            }
        
            printf(func,4,"closing "+this+"   "+lc.mbuf.getSize());
            setRunning();
            printf(func,4, "closed "+this+" - done");
        }
        
        
       
        
        private boolean modifyHash(HashMap<String, ArrayList<String>> imp) throws InvalidNameException {
            final String func=getFunc("modifyHash(HashMap<String, ArrayList<String>> imp)"); 
            HashMap<String, ArrayList<String>> mp = new HashMap<String, ArrayList<String>>();
            ArrayList<String> ar = new ArrayList();
                              ar.add( lc.getNewDN( imp.get("dn").get(0), lc.map.get("-b"), lc.map.get("-bc")) );
                              mp.put("dn", ar);
            
                               ar = new ArrayList();
            ArrayList<String> iar = imp.get("objectclass");
                              if ( iar == null ) { 
                                  printf(func,1,"iar map fpr objectclass missing ->"+mp );
                                  return false; 
                              }
                              String f = iar.toString().toLowerCase();
                              if ( f.contains("organization") && ! f.contains("person") && ! f.contains("account") ) {
                                  printf(func,3," organization found - do not modify - add direct");
                                  for ( String s: iar ) {
                                      if (! s.isEmpty()) ar.add(s);
                                  }
                              } else {
                                  printf(func,1," people other entry found - add after modify");
                                  for ( String s: iar ) {
                                      if (! s.isEmpty()) { 
                                          if ( f.contains("\\*") || f.contains(s.toLowerCase()) ) {
                                              ar.add(s);
                                          } else {
                                              printf(func,3,"objectclass "+s+"not needed");
                                          }
                                      }
                                  }
                                  
                                  HashMap<String, String> oc = lc.tmap.get("objectclass");
                                  if ( oc != null ) {
                                        Iterator<String> itter = oc.keySet().iterator();
                                        while ( itter.hasNext() ) {
                                             String sf = itter.next();
                                             if ( ! sf.isEmpty() ) {
                                                printf(func,3,"check object :"+sf+":");
                                                boolean found=true;
                                                for(int i=0; i< ar.size(); i++) {
                                                    String m = ar.get(i);
                                                    printf(func,3,"check ar["+i+"]="+m+"| with value s:"+sf+":");
                                                    if ( m.equals(sf.toLowerCase()) ) { 
                                                        printf(func,3,"found ar["+i+"]="+m+"| with value s:"+sf+":");
                                                        found=false; i=ar.size(); 
                                                    }
                                                }
                                                if ( found ) { 
                                                    printf(func,2,"add value s:"+sf+":");
                                                    ar.add(sf); 
                                                }
                                             }  
                                        }
                                        if ( lc.restiktCopy ) {
                                            ArrayList<String> arm = new ArrayList();
                                            while ( ar.size() > 0 ) {
                                                String sf = ar.remove(0);
                                                if ( oc.get(sf) != null) { arm.add(sf); }
                                            }
                                            ar=arm;
                                        }
                                  }      
                              }
                              mp.put("objectclass", ar);
                              
                              Iterator<String> itter = lc.tmap.keySet().iterator();
                              while (itter.hasNext() ) {
                                    String o = itter.next();
                                    if ( ! o.isEmpty() && ! o.equals("dn") && ! o.equals("objectclass") ) {
                                        ArrayList<String> v = imp.get(o.toLowerCase());
                                        ArrayList<String> n = new ArrayList<String>();
                                        if ( v != null ) {
                                            for ( int i=0; i<v.size(); i++) {
                                                String a = this.updateValue(v.get(i),lc.tmap.get(o),imp,mp,o);

                                                n.add(a);
                                            }
                                        } else {
                                            if ( ! o.startsWith("#")) {
                                                printf(func,3,"new attribute :"+o+":");
                                                HashMap<String, String> mm = lc.tmap.get(o);
                                                String a ="";
                                                       Iterator<String> itt = mm.keySet().iterator();
                                                       while(itt.hasNext()) { String s=itt.next(); if (!s.isEmpty()){ a=s;}}
                                                       a = this.updateValue(a,mm,imp,mp,o);
                                                       
                                                n.add(a);
                                            }    
                                        }   
                                        mp.put(o.toLowerCase(), n);
                                    }
                              }
            
                              if ( imp.get("uid") != null && mp.get("uid") !=null && imp.get("uid").get(0) != mp.get("uid").get(0) ) {
                                  ar = mp.get("dn");
                                  ar.add( ar.remove(0).replaceAll(imp.get("uid").get(0), mp.get("uid").get(0)) );
                                  mp.put("dn", ar);
                              }
                              Name e = new CompositeName().add( mp.get("dn").get(0) );
                              printHash(mp);
                              sendUpdate(mp);
                              
                              ModOK.add(e);
            
            return true;
        }
        
        private HashMap<String,String> auto = new HashMap();
        
        private String updateValue(String val, HashMap<String, String> tmap, HashMap<String, ArrayList<String>> imp,HashMap<String, ArrayList<String>> mp,String attr) {
            final String func=getFunc("updateValue(String val, HashMap<String, String> tmap, HashMap<String, ArrayList<String>> imp,HashMap<String, ArrayList<String>> mp,String attr)");
            String sf="";
            Iterator<String> itter =tmap.keySet().iterator();
            while( itter.hasNext() ) {
                String s = itter.next();
                if (! s.isEmpty() ) { sf=s; }
            }
            printf(func,3,"like to modify :"+val+":  based on ->|"+sf+"|<-");
            if ( ! sf.isEmpty() ) {
                if ( ! sf.contains("{") && ! sf.contains("__") ){
                    val=sf;
                } else  if ( sf.startsWith("{") ) {
                    int i = val.indexOf("{"); int j=val.indexOf("}");
                    printf(func,3,"df:"+sf+":  index j("+j+")>i+1("+(i+1)+")");
                    if ( j>i+1 ) {
                        String v = val.substring(i+1, j);
                        printf(func,3,"need to modify value:"+val+" based:"+sf+":  with {} =>"+v+"<=  for attribute:"+attr+":");
                        if ( v.startsWith("autoinc") ) {
                            String a = auto.get(attr); 
                            int base = 500;
                            if ( a == null ) {
                                 try {
                                    a = sf.substring(  sf.indexOf("(")+1, sf.indexOf(")") ); 
                                    // System.out.println("parse:"+a+":");
                                    base=Integer.parseInt(a);
                                 } catch(Exception e) { base=500;}
                            } else {
                                try{ base=Integer.parseInt(a); }catch(java.lang.NumberFormatException nf){ base=500; }
                            }
                            base++;
                            auto.put(attr, ""+base);
                            val=auto.get(attr);
                            
                            printf(func,2,"new value for attribute "+attr+" are now:"+val+":");
                        } else if (  v.startsWith("replace") ) {
                            String a = auto.get(attr);
                            if ( a != null ) {
                                 a = sf.substring(  sf.indexOf("(")+1, sf.indexOf(")") ); 
                                 printf(func,3,"a:"+a+":");
                                 for ( String s : v.split(";")) {
                                    if ( ! s.isEmpty() ) {
                                        printf(func,3," s =>"+s+"<=");  
                                        String[] at = s.split("\\|");
                                                 at = new String[] { at[0], ((at.length>1)?at[1]:"") };
                                                 printf(func,3, "replace :"+at[0]+":  with:"+at[1]+":  old=>"+val+"<=");
                                        val=val.replaceAll(at[0], at[1]);
                                        printf(func,3, "after replace =>"+val+"<=");
                          
                                    }  
                                  }                              
                            }
                        }
                    }
                } else {
                    printf(func,3,"need to modify value:"+val+" based:"+sf+":");
                    StringBuilder sw = new StringBuilder();
                    for (String s : sf.split("__") ) {
                        int start=0; int stop=-1;
                        String[] sp = s.split("\\(");
                        if ( sp.length > 1 ) {
                                     s=sp[0];
                                     sp = sp[sp.length-1].split(",");  
                                     sp[1]= sp[1].replaceAll("\\)", "");
                                     if ( sp[1].isEmpty() || sp[1].equals("$") ) { sp[1]=""+val.length(); }
                                     try { start=Integer.parseInt(sp[0]); }catch(java.lang.NumberFormatException nf) { start=0; }
                                     try { stop =Integer.parseInt(sp[1]); }catch(java.lang.NumberFormatException nf) { stop=-1; }
                        }
                        printf(func,3,"like to take:"+s+":   start:"+start+":  stop:"+stop+":");
                        ArrayList<String> ar = imp.get(s);
                        printf(func,3,"found ar:"+(ar!=null));
                        if ( mp.get(s.toLowerCase()) != null ) { ar= mp.get(s); }
                        printf(func,3,"found with mp ar:"+(ar!=null));
                        if ( ar != null ) {
                            s=ar.get(0);
                            if      ( stop == -1        ) { s=s.substring(start);       } 
                            else if ( stop > s.length() ) { s=s.substring(start);       }
                            else                          { s=s.substring(start, stop); }
                            sw.append(s);
                        } else {
                            sw.append(s);
                        }
                    }
                    printf(func,2,"new value :"+sw.toString()+":");
                    val=sw.toString();
                }
            }
            return val;
        }
        
        private void printHash(HashMap<String, ArrayList<String>> imp) {
            final String func=getFunc("printHash(HashMap<String, ArrayList<String>> imp)");
            int d=1;
            printf(func,d,"dn: "+(imp.get("dn")).get(0)  );
            
            ArrayList<String> a = objList;
               if ( debug >= 0 ) {
                   printf(func,1,"objList is Empty ? :"+a.isEmpty()+":  ");
                   for(String s: a ) {
                       printf(func,d,"print attribut :"+s+":  "+a.contains(s));
                   }
               }
            ArrayList<String> ar = imp.get("objectclass");
               for ( int i=0; i<ar.size() ; i++ ) {
                   String o = ar.get(i);
                   if ( a.isEmpty() || a.contains("objectclass") ) {
                       printf(func,d,"objectclass: "+o);
                   }
               }
               //System.out.println("a");
               Iterator<String> itter = imp.keySet().iterator(); 
               while ( itter.hasNext() ) {
                   String f = itter.next();
                   if ( ! f.isEmpty() && ! f.equals("dn") && ! f.equals("objectclass")) {
                       ar = imp.get(f);
                       for(int i=0; i<ar.size(); i++) {
                           if ( a.isEmpty() || a.contains(f) ) { 
                               printf(func,d,f+": "+ar.get(i)); 
                           }
                       }

                   }
               }
               
               printf(func,d,"");
        }

        private void sendUpdate(HashMap<String, ArrayList<String>> mp) {
           final String func=getFunc("sendUpdate(HashMap<String, ArrayList<String>> mp)");
           printf(func,4,"start update");
           Name nam=null;
           try { 
                 if ( lc.lmm == null ) {
                     lc.lmm = LdapModify.getInstance( lc.getAuthHash(false, "modify"));
                 }
                 nam  = new CompositeName().add( mp.get("dn").get(0) );
                 
                 ArrayList<BasicAttribute> ar = new ArrayList();
                 Iterator<String> itter = mp.keySet().iterator();
                 while(itter.hasNext()) {
                     String k = itter.next();
                     if ( ! k.equals("dn") && ! k.startsWith("#") ) {
                         ArrayList<String> a = mp.get(k);
                         BasicAttribute ba = new BasicAttribute(k);
                         int i=0;
                         for(String s : a) {
                             if ( !s.isEmpty()) {
                                printf(func,1, "add k:"+k+":  v:"+s+":"); 
                                //ar.add(  new BasicAttribute(k,s));
                                ba.add(s);
                             }   
                         }
                         ar.add(ba);
                      }    
                 }
                 
                 if ( needToCreate(nam) ) {
                     
                     BasicAttributes ent = new BasicAttributes();
                     for ( int i=0; i<ar.size(); i++ ) {
                        ent.put( ar.get(i) );
                     }
                     
                     lc.lmm.getLdapContext().createSubcontext(nam, ent);
                     
                 } else {
                     
                     ModificationItem[] mods = new ModificationItem[ar.size()];
                     
                     for ( int i=0; i<ar.size(); i++ ) {
                            //mods[i] = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("objectclass", ar.get(i)) );
                            mods[i] = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, ar.get(i) );
                            //mods[i] = ( ModificationItem ) ar.get(i);
                     }
                     
                     lc.lmm.getLdapContext().modifyAttributes(nam, mods);
                 }
                 ModOK.add(nam);
           } catch(NullPointerException npe) {
                printf(func,1,"NullPointer error ",npe);
                
           } catch(NamingException ne) {
                printf(func,1,"Naming error "+ne.getMessage(),ne);
                if ( nam != null ) ModNOK.add(nam); 
           }
           
           printf(func,4,"close update");
        
        }

        private boolean needToCreate(Name nam) {
            final String func=getFunc("needToCreate(Name nam)");
            BasicAttributes matchingAttributes = new BasicAttributes();
                            matchingAttributes.put( new BasicAttribute("dn") );
            try {
                NamingEnumeration<SearchResult> s = lc.lmm.getLdapContext().search(nam, matchingAttributes);
                if ( s == null ) { return true;}
                printf(func,3,nam+" exist? "+ ( ! s.hasMore()) );
                return ( s.hasMore());
            } 
            catch (NamingException ex) { 
                printf(func,1," lookup runs in error "+ex.getMessage(),ex);
                
                return false; 
            }
        }
        
        
    }
}
