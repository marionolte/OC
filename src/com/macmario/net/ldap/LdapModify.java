/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.ldap;

import com.macmario.net.ldap.main.LdapMain;
import com.macmario.net.ldap.main.LdapException;
import com.macmario.net.ldap.main.LdapScope;
import static com.macmario.general.Version.printf;
import com.macmario.io.file.ReadFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;

/**
 *
 * @author SuMario
 */
public class LdapModify extends LdapMain {
 
    
    public static LdapModify getInstance( String protocol, String hostname, int port, String userDN, String userPWD, String filter , String auth , String basedn ) throws NamingException {
        LdapModify lm = new LdapModify();
                   lm.protocol=protocol;
                   lm.hostname=hostname;
                   lm.port=port;
                   lm.userdn=userDN;
                   lm.userpw=userPWD;
                   lm.filter=filter;
                   lm.auth=auth;
                   lm.baseDN=basedn;
                   lm.initialize(lm,  protocol,  hostname,  port,  userDN,  userPWD,  filter ,  auth);
        
                   lm.init();
        return lm;
        //return getInstance(new String[] { (getProtocol().equals("ldaps"))?"-ssl":"","-h",hostname,"-p",""+port,"-D",userdn,"-w",userpw,"-f",filter,auth,"-b",basedn});
    }
    
    public static LdapModify getInstance() throws NamingException{
        return getInstance(new String[]{});
    }
    static public LdapModify getInstance(String[] ar) throws NamingException {
        LdapModify lm = new LdapModify();
        lm.protocol="ldap";
        lm.hostname="localhost";
        lm.port=389;
        lm.userdn="cn=admin";
        lm.userpw="";
        lm.filter="objectclass=*";
        lm.auth="simple";
        lm.baseDN=lm.getDefaultBaseDN();
        lm.scanner(ar,myusage);
        return getInstance(lm.protocol,lm.hostname,lm.port,lm.userdn,lm.userpw,lm.filter,lm.auth,lm.baseDN);
    }
    
    private LdapModify() {
        name="LdapModify";
    }
    
    private String[] validate(String s) {
        String[] sp = s.split(";");
        if      ( sp[0].equalsIgnoreCase("add")    ){ sp[0]="add";    }
        else if ( sp[0].equalsIgnoreCase("delete") ){ sp[0]="delete"; }
        else if ( sp[0].equalsIgnoreCase("del")    ){ sp[0]="delete"; }
        else if ( sp[0].equalsIgnoreCase("insert") ){ sp[0]="add";    }
        else if ( sp[0].equalsIgnoreCase("mod")    ){ sp[0]="modify"; }
        else if ( sp[0].equalsIgnoreCase("modify") ){ sp[0]="modify"; }
        else { sp[0]="unknown"; }
        
        
        return new String[]{ sp[0], sp[1], sp[2], sp[3] };
    }
    
    public void modify(String op, String dn, String attr, String val) {
        final String func=getFunc("modify(String op, String dn, String attr, String val)");
        NamingEnumeration<SearchResult> entry = null;
        boolean opdone=false;
        printf(func,4,"income");
        try { 
            
            entry = search(dn);
            if ( entry == null ) { throw new NamingException("dn: "+dn+" not found" ); }
            printf(func,3,"search dn:"+dn+": results with entry:"+entry);
            
            while(entry.hasMore()) {
                  SearchResult sr = entry.next();
                  printf(func,2," find entry to modify "+sr.getNameInNamespace()+" "+attr+"="+val+"|");
                  ModificationItem[] mods = new ModificationItem[1];
                  Name nam = new CompositeName().add( sr.getNameInNamespace() ); 
                  if ( op.matches("delete")) {
                     mods[0] =new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute(attr) );
                  } else if ( op.matches("insert") ) {
                        insert=true;
                        BasicAttributes ent = imap.get(dn);
                        if ( ent == null ) {
                            imap.put(dn, new BasicAttributes());
                            ent = imap.get(dn);
                        }        
                        ent.put( new  BasicAttribute(attr,val) ); 
                        mods[0]=null;  
                  } else {
                     mods[0] =new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(attr, val) );
                  }   
                  
                  printf(func,2,"perform entry modification to "+nam);
                  //Perform the requested modifications on the named object
                  if (mods[0] != null ) { getLdapContext().modifyAttributes(nam, mods); }
            }
            opdone=true;
        } 
        catch( NamingException ne ) {
            
            printf(func,1,"ERROR: message "+ne.getMessage(),ne);
            if ( ! op.matches("insert") ) { 
                System.out.println("ERROR: operation on dn "+dn+" not possible - "+ne.getExplanation());
                throw new LdapException(ne.getMessage()); 
            }
        } 
        catch(IOException io ) {
            printf(func,1,"ERROR: message "+io.getMessage(),io);
        }
        
        
        
        if ( ! opdone ) { }
        
        printf(func,4,"outgoing");
    }
    
    public  String  getLdifFile() { return ( map.get("-lf").equals( map.get("_default_lf") ) )?null:map.get("-lf"); }
    private boolean insert = false;
    private HashMap<String, BasicAttributes> imap =  new HashMap();
    private void insertMod() {
        final String func=getFunc("insertMod()"); 
        Iterator<String> itter = imap.keySet().iterator();
        while( itter.hasNext() ) {
            try {
                String dn = itter.next();
                
                Name nam = new CompositeName().add( dn ); 
                BasicAttributes ent = imap.get(dn);
                getLdapContext().createSubcontext(nam, ent);  
            } catch(NamingException ne) {
                printf(func,1,"ERROR: message "+ne.getMessage(),ne);
            }    
        }        
    }
    
    private NamingEnumeration<SearchResult> search(String dn) throws NamingException, IOException {
        final String func=getFunc("search(String dn)");
        printf(func,3,"run search against :"+dn);
        
        
        getLdapContext().setRequestControls(new Control[] {new PagedResultsControl( getPageSize(), Control.CRITICAL) }); 
        
        printf(func,3,"set search  controls :"+dn);
        
        SearchControls ctls = new SearchControls();
                       ctls.setReturningAttributes(new String[]{"objectclass"});
        if ( getSearchTimeout() > 0)
                       ctls.setTimeLimit(getSearchTimeout());
        
        printf(func,3,"set search scope :"+dn);
        
        
        if ( getMyScope().equals(LdapScope.sub))
           ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        if ( getMyScope().equals(LdapScope.one))
           ctls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
        if ( getMyScope().equals(LdapScope.base))
           ctls.setSearchScope(SearchControls.OBJECT_SCOPE);
        
        printf(func,3,"search now :"+dn);
        
        Name nam =  new CompositeName().add( dn ) ;
        //NamingEnumeration<SearchResult> results = getLdapContext().search( dn, getEnv("java.naming.ldap.attributes.binary") , ctls);
       
        NamingEnumeration<SearchResult> results = getLdapContext().search( nam, getFilter() , ctls);

        printf(func,3,"search done for :"+dn+":   result:"+((results!=null)?results:"NULL" ));

        return results;
        
    }
    
    synchronized public void modify(String file) {
        final String func=getFunc("modify(String file)");
        ReadFile f = new ReadFile(file);
        Name nam = null;  ArrayList<ModificationItem> ar=null;
        int op=DirContext.REPLACE_ATTRIBUTE;
        String dn="";
               if ( f.isReadableFile() ) {
                    printf(func,2,"file "+file+" is readable");
                    boolean _ldif=false; String ops="";
                    for ( String s: f.readOut().toString().split("\n") ) {
                        if ( s.startsWith("dn:")) { _ldif=true; }
                        if ( ! _ldif ) {
                            if ( ! s.isEmpty()  && ! s.startsWith("\\#") ) {
                               printf(func,2,"unkown operation :"+s+":");
                               String[] sp = validate(s);
                               if ( sp[0].matches("unknown") ) {
                                   
                                   if ( ! _ldif ) {
                                      printf(func,2,"unkown operation :"+sp[0]+":");
                    
                                      throw new LdapException("unkown ldapmodify operation provided");
                                   }   
                               }
                               printf(func,2,"operation :"+sp[0]+":");
                               if ( sp.length == 4) {
                                       modify(sp[0],sp[1],sp[2], sp[3]);
                               } else {
                                       modify(sp[0],sp[1],null,null); 
                               }  
                            }
                        } else {
                            printf(func,3," ldif operation  for:"+s);
                            try {
                                String[] sp = s.split(" ");
                                if ( sp[0].equals("dn:") ) {
                                     dn  = sp[ sp.length -1 ];
                                     nam = new CompositeName().add( dn );
                                     ar = new ArrayList(); ops=""; 
                                     op = DirContext.REPLACE_ATTRIBUTE;
                                } else if ( sp[0].equals("") || sp[0].equals("-")) {
                                    if ( nam != null ) {
                                        modify(dn,ar);
                                    }
                                } else if ( sp[0].toLowerCase().equals("changetype")) {
                                      String[] at = s.split(":");
                                      at[1]=at[1].replaceAll(" ", "").toLowerCase();
                                      switch(at[1]) {
                                          case "add"    : op=DirContext.ADD_ATTRIBUTE    ; break;
                                          case "delete" : op=DirContext.REMOVE_ATTRIBUTE ; break;
                                          default       : op=DirContext.REPLACE_ATTRIBUTE; break;
                                      }
                                } else if ( sp[0].toLowerCase().equals("replace") || sp[0].toLowerCase().equals("add") ||  sp[0].toLowerCase().equals("delete") ){
                                      String[] at = s.split(":");
                                      at[0]=at[0].replaceAll(" ", "").toLowerCase();
                                      switch(at[0]) {
                                          case "add"    : op=DirContext.ADD_ATTRIBUTE    ; break;
                                          case "delete" : op=DirContext.REMOVE_ATTRIBUTE ; break;
                                          default       : op=DirContext.REPLACE_ATTRIBUTE; break;
                                      }      
                                } else {
                                    String[] at = s.split(":");
                                    String attr = at[0]; 
                                    String val=s.substring(at[0].length()+2);
                                    ar.add(new ModificationItem(op, new BasicAttribute(attr, val) ) );
                                }
                            } catch(NamingException ne){
                                printf(func,1,"ERROR: message "+ne.getMessage()+" ",ne);
                    
                            }    
                        }    
                    } 
                    if ( ar.size() > 0 ) {
                        try {
                            modify(dn,ar);
                        } catch(NamingException ne) {
                            
                        }    
                    }
               }else {
                   System.out.println("ERROR: file "+f.getFQDNName()+" is not a readable file");
               }
               if ( insert ) { this.insertMod(); }
    }
    
    private void modify(String dn, ArrayList<ModificationItem> ar) throws InvalidNameException, NamingException {
        final String func=getFunc("modify(String dn, ArrayList<ModificationItem> ar)");
        Name nam = new CompositeName().add( dn );
        ModificationItem[] mods = new ModificationItem[ar.size()];
        for ( int i=0; i<ar.size(); i++ ) {mods[i] = ar.get(i); }
        
        //getLdapContext().modifyAttributes(nam, mods);
    }
    
    LdapContext ctx=null; 
    synchronized private SearchResult getDN(String dn) throws NamingException, IOException {
              String ldapURL = protocol+"://" + hostname + ":" + port;
              getEnv().put(Context.INITIAL_CONTEXT_FACTORY, getLdapContextFactory() );
              getEnv().put(Context.URL_PKG_PREFIXES,        getLdapNameingFactory() );
              getEnv().put(Context.SECURITY_AUTHENTICATION, "simple");
	      if ( userdn != null ) {
                getEnv().put(Context.SECURITY_PRINCIPAL, userdn);
              	getEnv().put(Context.SECURITY_CREDENTIALS, userpw);
	      }
              getEnv().put("java.naming.ldap.attributes.binary", filter);
              getEnv().put(Context.PROVIDER_URL, ldapURL); 

              //creating the JNDI context
              ctx = new InitialLdapContext(getEnv(), null);  

              //creating the PagedResultsControl and add it to the context
              ctx.setRequestControls(new Control[] {new PagedResultsControl( getPageSize(), Control.CRITICAL) }); 

              NamingEnumeration results = ctx.search( baseDN, filter, new SearchControls());
              
             
     
        return (SearchResult) ((results !=null)?results.next():null);
    }
    synchronized private void modifyDN(String dn,ArrayList ar) throws InvalidNameException, NamingException, IOException {
        SearchResult entry = getDN(dn);
        
        Name name =  new CompositeName().add( ( (entry != null)? entry.getNameInNamespace():dn ) );
        
        BasicAttribute  obj = new BasicAttribute("objectclass");
        ArrayList ua  = new ArrayList();
        Hashtable map = new Hashtable();
        if ( entry == null ) {
             map.put("objectclass", obj); ua.add("objectclass");
        
            for ( int i=0; i<ar.size(); i++ ) {
                    BasicAttribute ba = (BasicAttribute) ar.get(i);
                    String[] sp =  ba.toString().split(":");
                    BasicAttribute  ob = (BasicAttribute) map.get(sp[0]);
                                    ua.add(sp[0]);
                                    ob = new BasicAttribute(sp[0]);
                                    map.put(sp[0], ob);
                                    ob.add( ba.toString().substring(sp[0].length()+2));
                    
            }
            BasicAttributes ent = new BasicAttributes();

            for ( int i=0; i<ua.size(); i++ ) {
                        final String a = (String) ua.get(i);
                        ent.put( (BasicAttribute) map.get(a) );
            }
            ctx.createSubcontext(name, ent);
        } else {
            ModificationItem[] mods = new ModificationItem[ar.size()];
                for ( int i=0; i<ar.size(); i++ ) {
                //mods[i] = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("objectclass", ar.get(i)) );
                    mods[i] = ( ModificationItem ) ar.get(i);
                    
            }

            //Perform the requested modifications on the named object
            ctx.modifyAttributes(name, mods);
        }    
    }
    
    synchronized public void operate() {
        final String func=getFunc("operate()");
        //System.out.println("attrLIst:"+lm.attrList);
        int sav=debug;
        //debug=4;
        if ( attrList != null ) {
           printf(func,3,"entries to modify exist :"+attrList);
           Iterator<String> itter = attrList.keySet().iterator();
           while(itter.hasNext()) {
                 String dn = itter.next(); 
                 printf(func,2,"like to modify dn:"+dn);
                 HashMap<String,String> m = attrList.get(dn);
                 printf(func,2,"found for modify on:"+dn+":  hash =>"+m);
                 
                 int j = (m!=null && m.size()>0)?m.size()/2+1:0;
                 printf(func,2,"found for modify on:"+dn+":  hash =>"+m+" j:"+j);
                 
                 for ( int i=0; i<=j ; i++) {
                    if ( m.get("op"+i+"attr") != null ) { 
                        String[] sp = m.get("op"+i+"attr").split(": ");
                        printf(func,1,"call modify :"+m.get("op"+i)+": :"+dn+": :"+sp[0]+": :"+m.get("op"+i+"attr").substring(sp[0].length()+2)+":");

                        modify(m.get("op"+i),dn,sp[0],m.get("op"+i+"attr").substring(sp[0].length()+2));
                    } 
                 }
           }
        } else {
            printf(func,3,"no Attributelist exist");
        } 
        if ( insert ) { this.insertMod(); }
        debug=sav;
    }
    
    //synchronized public void singlemodify(String op, String dn, String attr, String val) {
    //    
    //}
    
    static public String myusage="\nusage():\noption: [-h hostname] [-p port] [-D adminDN ] [-j passwordfile] [[-f <file for operation of -o>] [-o <add|del|mod>:dn:attribute:value>] [-lf ldiffile]\n";
    public static void main(String[] args) throws Exception {
        LdapModify ls = LdapModify.getInstance(args);
                   //ls.scanner(args,myusage);
        
        if ( ! ls.usage ) { 
            // LdapModify ls = getInstance(); //getInstance(protocol,hostname,port,userdn,userpw,filter,auth);
            //LdapModify ls = getInstance(protocol,hostname,port,userdn,userpw,filter,auth,baseDN);

            if ( ls == null ) {
                System.out.println("ERROR: doesn't create an LdapModify object");
                ls.error_code=-1;
            } else {
               if ( ls.operationfile != null && (new ReadFile(ls.operationfile)).isReadableFile() ) { 
                    ls.modify(ls.operationfile);
               } else {
                    ls.operate();
               }
            }
        }    
        System.exit(ls.error_code);
    }
    
}
