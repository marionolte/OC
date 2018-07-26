/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.ldap;

import io.buffer.RingBuffer;
import net.ldap.main.LdapMain;
import io.file.ReadFile;
import io.file.SecFile;
import io.thread.RunnableT;
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
    private LdapCopy(String[] ar) {  scanner(ar,"ldapcopy "+myusage); lcInit(); }
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
             for ( int i=1; i< debug; i++) { a.add("-d"); }
             if ( protocol.equals("ldaps") ){a.add("-ssl"); }
             a.add("-h"); a.add(    (b)?hostname:modhost );
             a.add("-p"); a.add(""+((b)?port:modport)   );
             a.add("-D"); a.add(    (b)?userdn:moduserdn );
             a.add("-w"); a.add(    (b)?userpw:moduserpw );
     if(f.equals("search")){  
             a.add("-f"); a.add(filter); 
             a.add("-pg");a.add(""+this.getPageSize());
     }
             a.add("-a"); a.add(    (b)?auth:auth);
             a.add("-b"); a.add(    (b)?baseDN:modbaseDN );
             
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
        lcs = new LdapCopySearch(this); lcs.start();
        lcm = new LdapCopyModify(this); lcm.start();
        printf(func,3,"start LdapCopySearch completed");
        byte[] cookie = null;
        try {
            //ls = LdapSearch.getInstance(new String[]{ (   protocol.equals("ldaps"))?"-ssl":"", "-h",hostname, "-p",""+port,    "-D",   userdn, "-w", userpw, "-f",filter, "-a", auth, "-b", baseDN});
            ls = LdapSearch.getInstance( getAuthHash(true, "search") );
            ls.getLdapContext().setRequestControls(new Control[] {new PagedResultsControl( getPageSize(), Control.CRITICAL) });
            
            lss = LdapSearch.getInstance( getAuthHash(true, "search") );
            lss.getLdapContext().setRequestControls(new Control[] {new PagedResultsControl( getPageSize(), Control.CRITICAL) });
            
            
            Name baseName = new CompositeName().add( map.get("-b") );
            Name copyName = new CompositeName().add( map.get("-bc") );
           
            /*SearchControls ctls = new SearchControls();
                           ctls.setReturningAttributes(new String[]{"ALL"});
                           ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            */
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
                            Name ename = new CompositeName().add( (ent.getNameInNamespace() ) );
                            printf(func,3,"user entries for dn:"+ename+"   =>"); 
                            rbuf.push(ename);
                            
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
        } catch ( NamingException ne ) {
          printf(func,1,"transport entries to ends with naming error "+ne.getMessage());        
        }    
        
        this.searchRun=false;
        printf(func,4, "wait until rbuf is not empty");
        while( ! rbuf.isEmpty() || ! mbuf.isEmpty() ) { sleep(300);}
        printf(func,4,"copy complete");
    }
    
    synchronized public void copy1() throws NamingException, IOException {
        final String func=getFunc("copy()");
        printf(func,4,"copy dn's  from "+map.get("-b")+" to "+map.get("-bc"));
        
        byte[] cookie = null;
        try {
            //ls = LdapSearch.getInstance(new String[]{ (   protocol.equals("ldaps"))?"-ssl":"", "-h",hostname, "-p",""+port,    "-D",   userdn, "-w", userpw, "-f",filter, "-a", auth, "-b", baseDN});
            ls = LdapSearch.getInstance( getAuthHash(true, "search") );
            
            ls.getLdapContext().setRequestControls(new Control[] {new PagedResultsControl( getPageSize(), Control.CRITICAL) });
            Name baseName = new CompositeName().add( map.get("-b") );
            Name copyName = new CompositeName().add( map.get("-bc") );
           
            SearchControls ctls = new SearchControls();
                           ctls.setReturningAttributes(new String[]{"ALL"});
                           ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            
            long loop=0;
            do {
                printf(func,3,"perform search to "+getBaseDN()+" with :"+getFilter()+":");
                //performing the search
                NamingEnumeration results = ls.getLdapContext().search( getBaseDN(), getFilter(), ctls);
                                            
                printf(func,3,"result from search:"+results);
                
                checkedDN.put(map.get("-bc").toLowerCase(), "false");
                boolean b=true;        
                while (results != null && results.hasMore()) {
                        
                        
                        SearchResult ent = (SearchResult) results.next();
                        printf(func,3,"read entry "+ent);
                        
                        if ( ent != null ) {
                            printf(func,3,"found entry "+ent+"   dn:"+ent.getName()+","+map.get("-b"));
                            Name ename = new CompositeName().add( (ent.getName()+","+map.get("-b")) );
                            printf(func,2,"found entry "+ent+"  goes to name:"+ename );
                            if ( ename != null && ! ename.startsWith(baseName) && ! ename.startsWith(copyName) ) {
                                transport(ent);
                            } else {
                               printf(func,3,"do not copy base "); 
                            }
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
          printf(func,1,"transport entries to ends with "+io.getMessage());
        }
        
        printf(func,4,"copy complete");
    }
    private void transport(SearchResult entry) throws NamingException, IOException {
        final String func=getFunc("transport(SearchResult entry)");
        printf(func,4,"transport start");
        ArrayList<String> atlist= new ArrayList<String>(); atlist.add("ALL");
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
        
        
        NamingEnumeration fnew = lms.search(ename.toString(), "objectclass=*", atlist );
        NamingEnumeration fold = lms.search(entry.getNameInNamespace(), "objectclass=*", atlist );
        
        while (fold.hasMore() ) {
            System.out.println("fold next:"+fold.next()+":");
        }
        printf(func,3,"check old:"+fold+":  new:"+fnew);
        
        if ( entry != null ) {
            printf(func,3,"entry "+ename+" has attributes: "+entry.getAttributes()+" entry:"+entry);
            HashMap<String, ArrayList<String>> ip = new HashMap();
            Iterator<String> itter = tmap.keySet().iterator();
            while(itter.hasNext() ) {
                String f = itter.next();
                if ( ! f.startsWith("#")) {
                    HashMap<String, String> h = tmap.get(f);
                    ArrayList<String> mp = new ArrayList();
                    Iterator<String> it = h.keySet().iterator();
                    while(it.hasNext()) { mp.add( h.get(it.next())); }
                    ip.put(f.toLowerCase(), mp);
                    printf(func,3,"add attribute(strict:"+this.restiktCopy+") k="+f+":    value:"+tmap.get(f));
                }    
            }
            if( ename != null ){ //ip.get("dn") == null ) {
                ArrayList<String> mp = new ArrayList(); mp.add(ename.toString());
                ip.put("dn", mp);
            }
            if( ip.get("objectclass") == null ) {
                ArrayList<String> mp = new ArrayList(); mp.add("top");
                ip.put("objectclass", mp);
            }
            printf(func,3,"entry check start  ");
            Attributes at = entry.getAttributes();
            printf(func,3,"attributes are :"+at);
            NamingEnumeration<? extends Attribute> attrs = at.getAll();
            if ( attrs != null ) {
                while ( attrs.hasMore() ) {
                    Attribute f = attrs.next();
                    NamingEnumeration<?> c = f.getAll();
                    printf(func,3,"mod attr  f=>"+f+"<=  id:"+((f!=null)?f.getID():"NULL")+":");
                    ArrayList<String> mp = modAttr(f.getID(), c);
                    if ( ! mp.isEmpty() ) {
                        ip.put(f.getID(),mp);
                    }
                }
            }
            printf(func,3,"entry check completed ");
            if ( debug >= 3 ) { printEntry(ip); }
        }
        printf(func,4,"transport done");
        
    }
    
    private void printEntry(HashMap<String, ArrayList<String>> ip) {
        final String func=getFunc("printEntry(HashMap<String, ArrayList<String>> ip)");
        printf(func,3,"dn: "+ip.get("dn").get(0));
        
        String k = "objectclass";
        ArrayList<String> v = ip.get(k);
        for ( int i=0; i< v.size(); i++) {
            printf(func,3,k+": "+v.get(i));
        }
        Iterator<String> itter = ip.keySet().iterator();
        while ( itter.hasNext() ) {
            String a = itter.next();
            if ( ! a.equals("dn") && ! a.equals(k) ) {
                v = ip.get(a);
                for ( int i=0; i< v.size(); i++) {
                    printf(func,3,a+": "+v.get(i));
                }
            }    
        }
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
             boolean b=false;
             Iterator<String> itter = tp.keySet().iterator();
             while ( itter.hasNext() ) {
                String v=itter.next();
                if ( ! v.equals("*")) {
                    mp.add(v);
                    printf(func,3,"attr:"+attr+":  v:"+mp.get(mp.size()-1)+": ");
                } else { b=true; }    
             }
             if ( b ) {
                printf(func,3,"attr:"+attr+":  from ar:"+ar);
                while( ar.hasMore() ) {
                    String f=(String)ar.next();
                    if (  ! mp.contains(f) ) {
                        mp.add(f);
                        printf(func,2,"add with    modification :"+mp.get(mp.size()-1)+":");
                    }    
                }
             }   
               
         } else {
             while(ar.hasMore()) {
                String f = (String)ar.next(); 
                if ( ! this.restiktCopy ) {
                    mp.add(f);
                    printf(func,2,"add without modification :"+mp.get(mp.size()-1)+":");
                }    
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
            ArrayList ar = new ArrayList(); ar.add("ALL");
            while(itter.hasNext() ) {
                  String k = itter.next();
                  printf(func,4,"check for "+k+":  have:"+checkedDN.get(k).equals("true"));
                  if ( checkedDN.get(k).equals("true") ) { b=true; ; dns.put(k, ""+b ); }
                  else {
                    String v= "false";  
                    try {  
                        NamingEnumeration r = lms.search(checkedDN.get(k), "objectclas=*", ar);
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

    final static public String myusage="\nusage():\noption: [-h hostname] [-p port] [-ssl] [-D adminDN] [-j passwordfile] [-modh modifyHost] [-modp port] [-modD adminDN] [-modj passwordfile] [-modssl] [-b baseDN] [-bc copyBaseDN] [-f filter] [-t copytemplate] [-pg <page-sizelimit>] [objectlist]\n";
    final static public String free="false";
    
    public static LdapCopy getInstance(String[] ar) {
       return new LdapCopy(ar); 
    }
    
    public static LdapCopy getInstance() { return new LdapCopy(); }
    
    public static void main(String[] args) throws Exception {
         LdapCopy lc = getInstance(args);
                  lc.copy();
    }
    
    private RingBuffer<Name> rbuf = new RingBuffer(this.getPageSize());
    private RingBuffer<HashMap<String, ArrayList<String>>> mbuf = new RingBuffer<HashMap<String, ArrayList<String>>>(this.getPageSize());
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
                               HashMap<String, ArrayList<String>> imp = new HashMap<String, ArrayList<String>>();
                        
                               SearchResult entry = (SearchResult) namEnum.next();
                               ArrayList<String> ar = new ArrayList<String>(); ar.add(entry.getNameInNamespace());
                               imp.put("dn", ar);
                               printf(func,3,"dn: "+entry.getNameInNamespace() +" entry:"+entry); 
                               

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
                               mbuf.push(imp);
                        }    

        
                        
                    } catch(NamingException ne) {
                        printf(func,1,"lookup error for "+o+" - reason "+ne.getMessage(),ne);
                    } catch(IOException io) {
                            printf(func,1,"lookup error for "+o+" - reason "+io.getMessage(),io);
                    
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
                    printHash(imp);
                    modifyHash(imp);
                    //printHash(imp);
                    
                } else {
                    sleep(300);
                }
            }
        
            printf(func,4,"closing "+this+"   "+lc.mbuf.getSize());
            setRunning();
            printf(func,4, "closed "+this+" - done");
        }
        
        
        private boolean modifyHash(HashMap<String, ArrayList<String>> imp) {
            final String func=getFunc("modifyHash(HashMap<String, ArrayList<String>> imp)"); 
            HashMap<String, ArrayList<String>> mp = new HashMap<String, ArrayList<String>>();
            ArrayList<String> ar = new ArrayList();
                              ar.add( lc.getNewDN( imp.get("dn").get(0), lc.map.get("-b"), lc.map.get("-bc")) );
                              mp.put("dn", ar);
            
                               ar = new ArrayList();
            ArrayList<String> iar = imp.get("objectclass");
                              if ( iar == null ) { 
                                  printf(func,1,"iar map missing ->"+mp );
                                  return false; 
                              }
                              String f = iar.toString().toLowerCase();
                              if ( f.contains("organization")) {
                                  for ( String s: iar ) {
                                      if (! s.isEmpty()) ar.add(s);
                                  }
                              } else {
                                  for ( String s: iar ) {
                                      if (! s.isEmpty()) { 
                                          if ( f.contains("*") || f.contains(s.toLowerCase()) ) {
                                              ar.add(s);
                                          }
                                      }
                                  }
                              }
                              
                              
                              mp.put("objectclass", ar);
            //Iterator<String> itter = lc.attrList.keySet().iterator();
            
            printHash(mp);
            
            return true;
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
        
    }
}
