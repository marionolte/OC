/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.ldap;

import net.ldap.main.LdapMain;
import io.file.ReadFile;
import io.file.SecFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import javax.naming.CompositeName;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;
import net.tcp.Host;

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
    
    private LdapCopy() { this(new String[]{}); }
    private LdapCopy(String[] ar) {  scanner(ar,myusage); lcInit(); }
    private void lcInit() {
        final String func=getFunc("lcInit()");
        printf(func,4,"created");
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
            this.modport=Integer.parseInt(map.get("-p"));
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
        
        this.template= ( ! map.get("-t").equals(map.get("_default_-t")))? ( new ReadFile( map.get("-t") ).readOut().toString() ):"";
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
        
        
        printf(func,2,"local:"+protocol+"://"+host+":"+port+"?"+userdn+"&"+userpw+"&"+baseDN+"&"+filter+"&\n"+
                      "remote:"+modprotocol+"://"+modhost+":"+modport+"?"+moduserdn+"&"+moduserpw+"&"+modbaseDN+"&\n"+
                      "filter:"+filter+":   objectlist:"+getAttrList());
    }
    
    private String[] getAuthHash(boolean b,String f) {
        
        ArrayList<String> a = new ArrayList();
             for ( int i=1; i< debug; i++) { a.add("-d"); }
             if ( protocol.equals("ldaps") ){a.add("-ssl"); }
             a.add("-h"); a.add(    (b)?hostname:modhost );
             a.add("-p"); a.add(""+((b)?port:modport)   );
             a.add("-D"); a.add(    (b)?userdn:moduserdn );
             a.add("-w"); a.add(    (b)?userpw:moduserpw );
     if(f.equals("search")){  
             a.add("-f"); a.add(filter); 
     }
             a.add("-a"); a.add(    (b)?auth:auth);
             a.add("-b"); a.add(    (b)?baseDN:modbaseDN );
             
          String[] ab = new String[ a.size() ];
             for ( int i=0; i< ab.length; i++ ) { ab[i]=a.get(i); }
          return ab;
    }
    
    LdapSearch ls  = null;
    LdapSearch lms = null;
    LdapModify lmm = null;
    synchronized public void copy() throws NamingException, IOException {
        final String func=getFunc("copy()");
        printf(func,4,"copy dn's  from "+map.get("-b")+" to "+map.get("-bc"));
        
        byte[] cookie = null;
        try {
            //ls = LdapSearch.getInstance(new String[]{ (   protocol.equals("ldaps"))?"-ssl":"", "-h",hostname, "-p",""+port,    "-D",   userdn, "-w", userpw, "-f",filter, "-a", auth, "-b", baseDN});
            ls = LdapSearch.getInstance( getAuthHash(true, "search") );         
            ls.getLdapContext().setRequestControls(new Control[] {new PagedResultsControl( getPageSize(), Control.CRITICAL) });
            long loop=0;
            do {
                //performing the search
                NamingEnumeration results = ls.getLdapContext().search( getBaseDN(), getFilter(), new SearchControls());
                
                checkedDN.put(map.get("-bc").toLowerCase(), "false");
                        
                while (results != null && results.hasMore()) {
                        
                        
                        SearchResult ent = (SearchResult) results.next();
                        if ( ent != null ) {
                            transport(ent);
                        }
                        
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
            } while (cookie != null);   
            
        } catch (IOException io ) {
          printf(func,1,"transport entries to ends with "+io.getMessage());
        }
        
        printf(func,4,"copy complete");
    }
    private void transport(SearchResult entry) throws NamingException {
        final String func=getFunc("transport(SearchResult entry)");
        printf(func,4,"transport start");
        if ( lmm == null ) {
            //lmm = LdapModify.getInstance(new String[]{ (modprotocol.equals("ldaps"))?"-ssl":"", "-h", modhost, "-p",""+modport, "-D",moduserdn, "-w",moduserpw, "-f",filter, "-a", auth, "-b", map.get("-bc") } );
            lmm = LdapModify.getInstance( getAuthHash(false, "modify") );
            //lms = LdapSearch.getInstance(new String[]{ (modprotocol.equals("ldaps"))?"-ssl":"", "-h", modhost, "-p",""+modport, "-D",moduserdn, "-w",moduserpw, "-f",filter, "-a", auth, "-b", map.get("-bc") } );
            lms = LdapSearch.getInstance( getAuthHash(false, "search") );
        }
        Name ename = new CompositeName().add( ( (entry != null)? getNewDN(entry.getNameInNamespace(),map.get("-b"), map.get("-bc")):map.get("-bc")) );
        
        printf(func,3,"ename:"+ename+"   entry:"+getNewDN(entry.getNameInNamespace(),map.get("-b"), map.get("-bc")) +"  base:"+map.get("-b")+":  newbase:"+map.get("-bc")+":");
        
        checkAllBaseDN(ename);
        
        printf(func,3,"checkAllBaseDN completed ");
        
        if ( entry != null ) {
            printf(func,3,"entry check start  ");
            NamingEnumeration<? extends Attribute> attrs = entry.getAttributes().getAll();
            if ( attrs != null ) {
                while ( attrs.hasMore() ) {
                    Attribute f = attrs.next();
                    NamingEnumeration<?> c = f.getAll();
                    printf(func,3,"mod attr  f=>"+f+"<=  id:"+((f!=null)?f.getID():"NULL")+":");
                    modAttr(f.getID(), c);
                }
            }
            printf(func,3,"entry check completed ");
        }
        printf(func,4,"transport done");
        
    }
    private HashMap<String,HashMap<String,String>> tmap = new HashMap<String,HashMap<String,String>>(); 
    
    private String getNewDN(String old,String base, String newbase) {
        final String func=getFunc("getNewDN(String old,String base, String newbase)");
        String[] sp = old.substring(0, old.length()-base.length() ).split(",");  String[] at=base.split(",");
        StringBuilder sw = new StringBuilder();
        HashMap<String,String> tp = tmap.get("dn");
        if ( tp == null ) { tp= new HashMap<String,String>(); }
        
        printf(func,3,"check:"+old+":  base:"+base+":  newbase:"+newbase+":");
            
        for( int i=0; i<(sp.length-(at.length-1));i++ ) {
            if ( sw.length() >0 ) {sw.append(","); }
            printf(func,3,"check:"+sp[i]+" against : "+tp.get(sp[i].toLowerCase()));
            if ( tp.get(sp[i].toLowerCase()) != null ) { sp[i]=tp.get(sp[i].toLowerCase()); }
            sw.append(sp[i]);
        }
        final String f=sw.toString()+","+newbase;
        printf(func,3,"from:"+old+":  moved to:"+f+":");
        
        return f;
    }
    
    private HashMap<String,String> checkedDN=new HashMap<String,String>();
    private void checkAllBaseDN(Name dn) {
        if ( dn == null ) { return; }
        final String func=getFunc("checkAllBaseDN(Name dn)");
        String[] sp = dn.toString().split(",");
        StringBuilder sw=new StringBuilder(map.get("-bc"));
        for ( int i=sp.length-1; i>0; i--) {
            if ( ! sp[i].toLowerCase().startsWith("dc=") && ! sp[i].toLowerCase().startsWith("o=") ) {
                    String s = (sp[i]+","+sw.toString()).toLowerCase();
                    if ( checkedDN.get(s) == null ) {
                         printf(func,2,"have to check sp["+i+"]="+sp[i]+"     =>"+s);
                         checkedDN.put(s, "false");
                    }
                    
            }
        }
        printf(func,3,"have to check some "+sp.length);
                         
        if ( ! checkBaseAreExist() ) { 
            
            Iterator<String> itter = checkedDN.keySet().iterator();
            while ( itter.hasNext() ) {
               String s= itter.next();
               printf(func,2,"have to check =>"+s+"<=");
                         
               if ( s != null && ! s.isEmpty() && checkedDN.get(s).equals("false") ) {
                   try { 
                        Name nam = new CompositeName().add( s );
                        printf(func,1,"like to create for =>"+s+"<= ");
                        //createDN(nam); 
                   } catch(NamingException ne) {
                       printf(func,1,"ERROR: for =>"+s+"<=  "+ne.getMessage());
                         
                   }     
               }     
            }   
        } else {
          printf(func,2,"INFO: all base entries are exist");
        }
    }
    
    
    private boolean createDN(Name dn, HashMap<String,String> mp) {
        final String func=getFunc("createDN(Name dn)");
        ArrayList<String> ar = new ArrayList(); ar.add("ALL");
        try {
            NamingEnumeration sr = ls.search(dn.toString(), "obajectclass=*", ar);
            while ( sr.hasMore() ) {
                 Object o = sr.next();
                 printf(func,0,"have for =>"+dn.toString()+"<=  object ->|"+o+"]<-");
            }
            
            return true;
        } catch(NamingException ne) {
            printf(func,1,"ERROR: for =>"+dn.toString()+"<=  "+ne.getMessage());
        } catch (IOException io ) {
            printf(func,1,"ERROR: for =>"+dn.toString()+"<=  "+io.getMessage());
        }  
        return false;
    }
    
    private ArrayList<String> modAttr(String attr, NamingEnumeration<?> ar) throws NamingException {
         final String func=getFunc("modAttr(String attr, NamingEnumeration<?> ar)");
         final ArrayList<String> mp = new ArrayList();
         HashMap<String,String> tp = tmap.get(attr.toLowerCase());
         if ( this.restiktCopy && tp == null ) {
             printf(func,3,"strict copy are set and attribute :"+attr+": not found in template");
             return mp;
         }
         printf(func,3,"like to check "+((attr==null)?"NULL":attr)+" for mod "+((tp==null)?"NULL":tp));
         if ( tp != null  ) {
             String v = tp.get( attr.toLowerCase() );
             printf(func,3,"attr:"+attr+":  v:"+v+": ");
             while( ar.hasMore() ) {
                 String f=(String)ar.next();
                 if ( v != null && ! v.isEmpty() ) {
                      //String[] sp = v.split(" ");
                      for ( String s : v.split(" ")  ) {
                          printf(func,2,"f:"+f+":   s:"+s+":");
                      }
                 }
                 mp.add(f);
                 printf(func,2,"add with    modification :"+mp.get(mp.size()-1)+":");
             }
         } else {
             while(ar.hasMore()) {
                mp.add((String)ar.next());
                printf(func,2,"add without modification :"+mp.get(mp.size()-1)+":");
             }
         }
         printf(func,4,"return");
         return mp;
    }
    
    private boolean checkBaseAreExist(){
        final String func=getFunc("checkBaseAreExist()");
        boolean b=false;
        HashMap<String,String> dns=new HashMap<String,String>();
        try {
            Iterator<String> itter = checkedDN.keySet().iterator();
            ArrayList ar = new ArrayList(); ar.add("dn");
            while(itter.hasNext() ) {
                  String k = itter.next();
                  printf(func,4,"check for "+k+":  have:"+checkedDN.get(k).equals("true"));
                  if ( checkedDN.get(k).equals("true") ) { b=true; ; dns.put(k, ""+b ); }
                  else {
                    String v= "false";  
                    try {  
                        NamingEnumeration r = ls.search(checkedDN.get(k), "objectclas=*", ar);
                        printf(func,3,"ldapsearch return:"+r);
                        if ( r.hasMore() ) { b=true; v=""+b;}
                        printf(func,2,"checked for :"+k+":  getting:"+v);
                        if ( ! b ) { return b; }
                    } catch(Exception e) {
                        b=false;
                    }
                    dns.put(k, v);
                  }
            }
        } catch(Exception e ) {
            printf(func,1,"checked bases runs in error :"+e.getMessage(),e);
        }    
        checkedDN=dns;
        printf(func,4,"return with "+b);
        return b;
    }

    final static public String myusage="\nusage():\noption: [-h hostname] [-p port] [-ssl] [-D adminDN] [-j passwordfile] [-modh modifyHost] [-modp port] [-modD adminDN] [-modj passwordfile] [-modssl] [-b baseDN] [-bc copyBaseDN] [-f filter] [-t copytemplate] [objectlist]\n";
    final static public String free="false";
    
    public static LdapCopy getInstance(String[] ar) {
       return new LdapCopy(ar); 
    }
    
    public static LdapCopy getInstance() { return new LdapCopy(); }
    
    public static void main(String[] args) throws Exception {
         LdapCopy lc = getInstance(args);
                  lc.copy();
    }
}
