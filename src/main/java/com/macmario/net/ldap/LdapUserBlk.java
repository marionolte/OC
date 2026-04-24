package com.macmario.net.ldap;

import com.macmario.general.MyVersion;
import com.macmario.io.file.SecFile;
import com.macmario.io.thread.RunnableT;
import java.util.ArrayList;
import java.util.Hashtable;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;



/**
 *
 * @author SuMario / MarioHelp.de
 * @version  1.0
 * 
 */
public class LdapUserBlk extends RunnableT{
     private final String method;
     private final String attr;
     private final String val;
     private boolean started=false;
     private boolean finished=false;
     private boolean search=true;
     private MyVersion v = new MyVersion();

     
    
     
     public LdapUserBlk(          ) { this("main"); }
     public LdapUserBlk(String typ) { this(typ,null,null);}
     public LdapUserBlk(String method, String attr, String val ) {
            this.method=method;
            this.attr=attr;
            this.val=val;
     }
     
     private LdapUserBlk lbMain;
     public  void    setMain(LdapUserBlk lb) { this.lbMain=lb; }
     private LdapUserBlk lbMod;
     public  void    setLdapMod(LdapUserBlk lb) {this.lbMod=lb; }
     private boolean close=false;
     public  void    setClose() { 
         while( ( sr != null || lctx != null ) ) { try { Thread.sleep(50);}catch(Exception e){} }
         close=true; if(!this.started){ this.finished=true; } 
     }
     
     static public String myusage="usage():\noption: [-h hostname <def:localhost>] [-p port <def:389>] [-a adminDN <def:cn=orcladmin> "
                          + "[-j passwordfile] [-b baseDN ] [-f filter] [-ssl <set ldaps schema>] [-o ldapCheck ] [ -of ldapCheckFile ] [-create]"
                          + " [-modh <hostname for modificaction>] [-modp <port for ldapserver modification> ] [ -modssl <set ldaps for modification>]" 
                          + "| [-help <used per default>]";
                          
     public static void usage(boolean b){
         System.out.println("\n\n"
                          + "LdapUserBlk::"+myusage
                          + "˜n\t\t\t ldap     :\t"+proto+"://"+hostname+":"+port+"  \n"
                          + "\t\t\t account  :\t"+userDN+"/"+userPWD+" \n"
                          + "\t\t\t baseDN   :\t"+baseDN+"  \n"
                          + "\t\t\t filter   :\t"+filter+"  \n\n"
                          
                          + " special option :\n"
                          + "\t\t\t pwdreset :\t"+obPWD+"\n\n"
                          + "\t\t\t ldap operation \n"
                          + "\t\t\t   -of <filename> \t-\t profile an file for ldap operation check - format see -o\n"
                          + "\t\t\t   -o <add|del|mod>:attribute:value \n\n"
                          + "\t\t\t   -create  [creates an new user based on the provide baseDN with the objects you have provided]\n\n"
                 + " The script will perform - binding on the LDAP server with the provided crediantials and search for users in the baseDN. \n"
                 + " With that users it will add/modify/delete attributes like objectclass or sn on existing users.\n"
                 + " And could also create an user based on the baseDN and the provided attributes\n\n\n"
       
                          );
         if (b) { System.exit(0); }
     }
     
     private String getLdapContextFactory(){
         // sun "com.sun.jndi.lqqdap.LdapCtxFactory"
         // ibm "com.ibm.jndi.LDAPCtxFactory";
         
         String s="com.sun.jndi.ldap.LdapCtxFactory";
         
         if ( isAIX() ) {
             s="com.ibm.jndi.LDAPCtxFactory";
         }
         
         System.setProperty("java.naming.factory.initial", s);
	 return s;
     }
     
     private String getLdapNameingFactory() {
        //env.put("java.naming.factory.url.pkgs", "com.ibm.jndi"); 
        String s="com.sun.jndi";
        if ( isAIX() ) {
            s="com.ibm.jndi";
        }
        return s;
     }
     //connection related information
     static String proto  = "ldap";
     static String hostname = "localhost";
     static int    port = 389;
     static String protoMod  = "";
     static String hostnameMod = "";
     static int    portMod = -1;
     
     //credentials used for authentication
     static String userDN = "cn=admin";
     static String userPWD = "";
     static String userModDN = "";
     static String userModPWD = "";
     
          //generic search parameters 
     static String baseDN = "cn=Users,"+(LdapBind.getInstance()).getDefaultBaseDN();
     static String filter = "objectclass=*";
     static boolean obPWD = false;
     
     boolean bind=true;
     
     DirContext ctxmod = null;
     
     Hashtable env = new Hashtable();
     
     public void mod(SearchResult entry, ArrayList ar) {
        Name name=null;   
        try {
            // Create the initial context
            if ( ctxmod == null ) {
              if ( lbMain.debug ) System.out.println("LDAP MOD - connection to:"+ ((env.isEmpty())?lbMain.env:env).get(Context.PROVIDER_URL) );  
              ctxmod = new InitialDirContext( ( ( env.isEmpty() )?lbMain.env:env) ); 
            }
            name =  new CompositeName().add( ( (entry != null)? entry.getNameInNamespace():baseDN ) );
            
                       
            if ( lbMain.createDN ) {
                
                BasicAttribute  obj = new BasicAttribute("objectclass");
                ArrayList ua= new ArrayList();
                Hashtable map = new Hashtable();
                          map.put("objectclass", obj); ua.add("objectclass");
                for ( int i=0; i<ar.size(); i++ ) {
                    BasicAttribute ba = (BasicAttribute) ar.get(i);
                    String[] sp =  ba.toString().split(":");
                    BasicAttribute  ob = (BasicAttribute) map.get(sp[0]);
                    if ( lbMain.debug ) System.out.println("attribute |@|"+sp[0]+"|@|  value |@|"+ ba.toString().substring(sp[0].length()+2)+"|@|" );
                    if ( ob == null ) {
                        ua.add(sp[0]);
                        ob = new BasicAttribute(sp[0]);
                        map.put(sp[0], ob);
                    }
                    ob.add( ba.toString().substring(sp[0].length()+2));
                    
                }
                BasicAttributes ent = new BasicAttributes();
                
                for ( int i=0; i<ua.size(); i++ ) {
                    final String a = (String) ua.get(i);
                    if ( lbMain.debug ) System.out.println("add attribute :"+a+"="+( (BasicAttribute) map.get(a) ).toString() );
                    ent.put( (BasicAttribute) map.get(a) );
                }
                
                
                ctxmod.createSubcontext(name, ent);   
                
            } else {
                 // Specify the changes to make
                ModificationItem[] mods = new ModificationItem[ar.size()];
                for ( int i=0; i<ar.size(); i++ ) {
                //mods[i] = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("objectclass", ar.get(i)) );
                    mods[i] = ( ModificationItem ) ar.get(i);
                    if ( lbMain.debug ) System.out.println("Entry:"+name+" receive modification :"+mods[i].toString());
                }

                //Perform the requested modifications on the named object
                ctxmod.modifyAttributes(name, mods);
            }    
            
            lbMain.modified++;
            System.out.println("dn: "+name+" updated");
                  
        } catch (javax.naming.directory.SchemaViolationException ex) {
            if ( lbMain.createDN ) {
                System.out.println("ERROR: missing required entries for the user "+baseDN+" \nreason: "+ex.getMessage()); 
            } else {
                System.out.println("ERROR: Schema problem detected on the modification ldap server - needs to corrected first \nreason: "+ex.toString());
            }
            System.exit(-255);
        } catch ( javax.naming.AuthenticationException a) {
              System.out.println("ERROR: invalid credentials provided for ldap modification");
              System.exit(-2);
        } catch (javax.naming.directory.AttributeInUseException ea ) {
              System.out.println("INFO: "+name.toString()+" attribute duplicate - add error");
        } catch (Exception e) {
          if (name!=null ) System.out.println("ERROR: modification "+name.toString()+"\n");  
          e.printStackTrace();
          ctxmod=null;
        }
     }
     
     long count=0;
     long modified=0;
     long notModified=0;
     
     int pageSize = 10;
     
     private boolean check4User(SearchResult entry) throws NamingException{
         return check4User( entry.getAttributes() );
     }    
     private boolean check4User(Attributes a) throws NamingException {
         return check4User( a.get("objectclass") );
     }
     private boolean check4User(Attribute a) throws NamingException {
         boolean b=false;               
           
         for (int i=0; i<a.size(); i++ ) {
                final String f=(String) a.get(i);
                
                if      ( f.toLowerCase().matches("orcluser")               ) { b=true; i=a.size(); }
                else if ( f.toLowerCase().matches("orcluserv2")             ) { b=true; i=a.size(); }
                else if ( f.toLowerCase().matches("organizationalperson")   ) { b=true; i=a.size(); }
                else if ( f.toLowerCase().matches("inetorgperson")          ) { b=true; i=a.size(); }
                else if ( f.toLowerCase().matches("person")                 ) { b=true; i=a.size(); }
                else if ( f.toLowerCase().matches("oblixorgperson")         ) { b=true; i=a.size(); }
         }
        
         return b;
     }
     
     
     
     public void search() {
         //JNDI context declaration
         LdapContext ctx = null;
     
         
         byte[] cookie = null;
         
         //startup
	 try {
              //preparing the hashtable for initializing the context
              //Hashtable env = new Hashtable();
              String ldapURL = proto+"://" + hostname + ":" + port;
              env.put(Context.INITIAL_CONTEXT_FACTORY, getLdapContextFactory() );
              env.put(Context.URL_PKG_PREFIXES,        getLdapNameingFactory() );
              env.put(Context.SECURITY_AUTHENTICATION, "simple");
	      if ( bind ) {
              	env.put(Context.SECURITY_PRINCIPAL, userDN);
              	env.put(Context.SECURITY_CREDENTIALS, userPWD);
	      }
              env.put("java.naming.ldap.attributes.binary", filter);
              env.put(Context.PROVIDER_URL, ldapURL); 

              //creating the JNDI context
              ctx = new InitialLdapContext(env, null);  

              //creating the PagedResultsControl and add it to the context
              ctx.setRequestControls(new Control[] {new PagedResultsControl( pageSize, Control.CRITICAL) }); 

              long loop=0;
              do {
                //performing the search
                NamingEnumeration results = (!createDN) ?ctx.search( baseDN, filter, new SearchControls()) : null;
                
                while (results != null && results.hasMore()) {
                        if ( lbMain.debug ) System.out.println("result has an new entry");
                        SearchResult entry = (SearchResult) results.next();
                        count++;
                        
                        if (! check4User(entry) ) { 
                            if ( lbMain.debug ) System.out.println("skip entry "+entry.getNameInNamespace()+" - not a person ");
                        }else {
                           ((LdapUserBlk) lbList.get(0)).checkEntry(entry, new ArrayList() ); 
                        }
                        
                }
                if ( results != null && count == 0) {
                    if ( lbMain.debug ) System.out.println("check base entry as user entry");
                    lattr = ctx.getAttributes(baseDN);
                    if ( check4User( lattr  ) ) {
                        if ( lbMain.debug ) System.out.println("base entry "+baseDN+" is a user entry -  create "+createDN);
                        count++;
                        ((LdapUserBlk) lbList.get(0)).checkEntry(ctx, new ArrayList(), lattr ); 
                    }
                } else {
                    if ( createDN ) {
                        if ( lbMain.debug ) System.out.println("base entry "+baseDN+" creating ");
                        count++;
                        ((LdapUserBlk) lbList.get(0)).checkEntry(ctx, new ArrayList(), lattr );
                    }
                }
                //process the returned controls to get the cookie 
                Control[] controls = ctx.getResponseControls();
                if (controls != null) {
                     for (int i = 0; i < controls.length; i++) {
                           if (controls[i] instanceof PagedResultsResponseControl) {
                              PagedResultsResponseControl prrc = (PagedResultsResponseControl) controls[i];
                              cookie = prrc.getCookie();
                           }
                      }
                 }  
                //setting the cookie on the context for the next page search 
                ctx.setRequestControls(
                         new Control[] { 
                             new PagedResultsControl( pageSize, cookie, Control.CRITICAL) });
                //System.out.println("cookie:" + cookie);
                if ( loop >0 && forcedDown ) { cookie=null; }
                loop++;
              } while (cookie != null);
                     
        } catch ( javax.naming.AuthenticationException a) {
              System.out.println("ERROR: invalid credentials provided");
              System.exit(-2);
        } catch ( javax.naming.NameNotFoundException af) {
              System.out.println("ERROR: baseDN "+baseDN+" not found");
              System.exit(-3);
        } catch (Exception e) {
              e.printStackTrace(); 
        }
        
        try {    ctx.close(); } catch(Exception e){} 
        try { ctxmod.close(); } catch(Exception e){} 
     }
    
     private SearchResult  sr=null;
     private ArrayList  modAr=null;
     private LdapContext lctx=null;
     private Attributes lattr=null;
     
     public  void checkEntry(LdapContext entry, ArrayList checkAr, Attributes at) {
         if( entry == null ) {return;}
         while ( ! getLock() ) { sleep(); }
         if ( lbMain.debug ) System.out.println(getThread().getName()+" lock free ");
         synchronized( lock ) {
             if ( lbMain.debug ) System.out.println(getThread().getName()+" work provided for "+baseDN);
             sr=null; modAr=checkAr;  lctx=entry; lattr=at;
             if ( ! this.started ){ this.start(); }
         }
     }
     public  void checkEntry(SearchResult entry, ArrayList checkAr) {
         if( entry == null ) {return;}
         if ( lbMain.debug ) System.out.println(getThread().getName()+" wait to get the lock ");
         while ( ! getLock() ) { sleep(); }
         if ( lbMain.debug ) System.out.println(getThread().getName()+" lock free ");
         synchronized( lock ) {
             if ( lbMain.debug ) System.out.println(getThread().getName()+" work provided for "+entry.getName());
             sr=entry; modAr=checkAr;  lctx=null; lattr=null;
             if ( ! this.started ){ this.start(); }
         }
     }
     
     public void sleep() {
             try {
                Thread.sleep(300);
             } catch(Exception e) {}  
     }
     
     final private String lock =" lock";
     
     public boolean getLock() {
        synchronized ( lock ) { 
            return ( sr==null && lctx==null ) ? true : false; 
        }    
     }
     
     public void run() {
         if ( lbMain.debug ) System.out.println(getThread().getName()+" started");
         //while ( ! close ) {
         do {
             if ( this.search ) {
                // for the ldap verify threads
                if ( ! getLock() ) {
                   // synchronized( lock ) {
                   if (lbMain.createDN ) {
                      if      ( this.method.matches("add") ) {
                           if ( lbMain.debug ) System.out.println(getThread().getName()+"add on "+baseDN+" "+this.attr+"="+this.val+" ");
                           modAr.add( new  BasicAttribute(this.attr,this.val) );
                      } else {
                          if ( lbMain.debug ) System.out.println(getThread().getName()+" only add operation on "+baseDN+ "  possible with the attribute :"+this.attr+":");
                      } 
                   } else {    
                      final String name = (sr==null)?baseDN:sr.getName();
                            Attributes at;
                            if ( lbMain.debug ) System.out.println(getThread().getName()+" working for "+name+ "  with the attribute :"+this.attr+":");
                            if (sr!=null){ 
                                if ( lbMain.debug ) System.out.println(getThread().getName()+"sr  getAttributes()");
                                at = sr.getAttributes();
                            } else { 
                              //try {
                                  if ( lbMain.debug ) System.out.println(getThread().getName()+"lctx getAttributes() for "+this.attr+"("+lattr.size()+") on "+baseDN);
                                  //at=lctx.getAttributes( baseDN );
                                  at=lattr;
                              //} catch (NamingException ex) {
                              //    if ( lbMain.debug ) System.out.println(getThread().getName()+"lctx  NamingException "+ex.getMessage());
                              //    at=null;
                              //}
                            }
                            Attribute   a = (at!=null)?at.get(this.attr):null;

                            boolean exist=false;
                            boolean match=false;
                            if ( a!=null && a.size() > 0 ) {
                              if ( lbMain.debug ) System.out.println("user "+name+" has attributes for check :"+a.size()+" ");
                              exist=true;
                              for (int i=0; i<a.size(); i++ ) {
                                 try {
                                     final String f=(String) a.get(i);
                                     if ( lbMain.debug ) System.out.println("user "+name+"  ("+i+") has attribute check :"+f+":" );
                                     if ( f.toLowerCase().matches(this.val.toLowerCase()) ) { match=true; i=a.size(); }
                                 }catch (Exception ex) { 
                                     if ( lbMain.debug ) System.out.println("Exception in attribute check :"+ex.getMessage()+" ");
                                 }
                              }
                            } else  {
                                if ( lbMain.debug ) System.out.println("ERROR: user "+name+"  has no attributes");
                            }    
                            if ( lbMain.debug ) System.out.println(getThread().getName()+" entry "+name+" has atrribute "+this.attr+" ?  "+exist+"");  


                            if      ( this.method.matches("add") ) { 
                                    if ( ! exist ) {
                                       if ( lbMain.debug ) System.out.println(getThread().getName() +"  add "+this.attr+"="+this.val+" to "+name ); 
                                       modAr.add( new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute(this.attr, this.val )));
                                    } else {
                                        if ( lbMain.debug ) System.out.println(getThread().getName()+" entry exist modification  for "+this.attr+"="+this.val+" not provide on "+name );
                                    }
                            } 
                            else if ( this.method.matches("mod")) {
                                    if ( lbMain.debug ) System.out.println(getThread().getName()+" "+method+": (mod) "+this.attr+"="+this.val+" on "+name );
                                    modAr.add( new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(this.attr, this.val) ) );
                            }
                            else if ( this.method.matches("del")) {
                                    if ( exist ) {
                                       if ( lbMain.debug ) System.out.println(getThread().getName()+" del: "+this.attr +" on "+name );  
                                       modAr.add( new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute(this.attr)));
                                    } else {
                                       if ( lbMain.debug ) System.out.println(getThread().getName()+" del: not need - not exist  on "+name);
                                        System.out.println("User entry "+name+" did not has the attribute "+this.attr);
                                    }
                            } else {
                                    if ( lbMain.debug ) System.out.println(getThread().getName()+"unknown modification typ provided :"+this.method+":");
                            }
                      }

                      if  ( order+1 < lbList.size() ) {
                          if ( lbMain.debug ) System.out.println(getThread().getName()+" provide entry to thread "+(order+1));
                          if (sr != null ) {
                              ( (LdapUserBlk) lbList.get(order+1) ).checkEntry(sr,modAr);
                          } else {
                              ( (LdapUserBlk) lbList.get(order+1) ).checkEntry(lctx,modAr,lattr);
                          }    
                      } else {
                          if ( lbMain.debug ) System.out.println(getThread().getName()+"  mod:"+modAr.size()+"  (last thread)");
                          if ( modAr.size() > 0 ) {
                              if (sr != null ) {
                                lbMod.checkEntry(sr, modAr);
                              } else {
                                lbMod.checkEntry(lctx, modAr,lattr);  
                              }  
                              lbMod.start();
                          } else {
                              lbMain.notModified++;
                          }
                      }
                      if ( lbMain.debug ) System.out.println(getThread().getName()+" work completed");
                      sr=null; modAr=null; lctx=null;
                      
                   //}
                }
             } else {
                 // for the ldap modify thread
                 if ( ! getLock() ) {
                   final String name = (sr==null)?baseDN:sr.getName();  
                   synchronized( lock ) {
                      if ( lbMain.debug ) System.out.println(getThread().getName()+" working - ldapmodify for "+name );
                      mod(sr, modAr);
                      if ( lbMain.debug ) System.out.println(getThread().getName()+" work modification completed for "+name);
                      sr=null; modAr=null; lctx=null;
                     
                   }
                 }  
             }   
             
             sleep(); 
         } while(! close );
         if ( lbMain.debug ) System.out.println(getThread().getName()+" finished");
         finished=true;
     }
     
     private Thread thread=null;
     public void setThread(Thread th ){ this.thread=th;}
     
     ArrayList lbList = new ArrayList();
     private int order=0;
     public void setOrder(int ord) { this.order=ord; }
     
     public void addCheckObj(String s) throws Exception {
         String[] sp = s.split(":");
         sp[0]=sp[0].toLowerCase();
         String val = s.substring( sp[0].length()+sp[1].length()+2);
         if ( lbMain.debug ) System.out.println("Ldap method:"+sp[0]+":  |@|"+sp[1]+"="+val+"|@|");
         
         LdapUserBlk lb= new LdapUserBlk(sp[0],sp[1],val);
                     lb.setMain(this);
                     lb.setThread( new Thread((Runnable) lb, "checker Thread "+lbList.size()+" Ldap method:"+sp[0]+":  |@|"+sp[1]+"="+val+"|@|") );
                     lb.setOrder(lbList.size());
                     lbList.add(lb);
                     lb.lbList=this.lbList;
                     lb.lbMain=this;
         if ( lbMod == null ) {
                     lbMod = new LdapUserBlk("ldapmod");
                     lbMod.search=false;
                     lbMod.setThread( new Thread((Runnable) lbMod, "ldap modification thread") );
                     lbMod.lbMain=this;
                     if ( modEnv ) {
                         String ldapURL = ( (   protoMod.isEmpty())?   proto:protoMod   ) +"://" 
                                         +( (hostnameMod.isEmpty())?hostname:hostnameMod) + ":" 
                                         +( (portMod>0            )? portMod:port );
                        lbMod.env.put(Context.INITIAL_CONTEXT_FACTORY,getLdapContextFactory() );
                        lbMod.env.put(Context.SECURITY_AUTHENTICATION, "simple");
	                if ( bind ) {
              	             lbMod.env.put(Context.SECURITY_PRINCIPAL,   ( ( userModDN.isEmpty())? userDN:userModDN ) );
              	             lbMod.env.put(Context.SECURITY_CREDENTIALS, ( (userModPWD.isEmpty())?userPWD:userModPWD) );
	                }
                        //env.put("java.naming.ldap.attributes.binary", filter);
                        lbMod.env.put(Context.PROVIDER_URL, ldapURL); 

                     }
         } 
                     lb.setLdapMod(lbMod);
                     lb.start();
                 
     }
     
     public void finishing() {
         if ( lbMain.debug ) System.out.println("send close to ldap checker thread");
         //for ( int i=0; i<lbList.size(); i++ ) {  ( (LdapUserBlk) lbList.get(i) ).setClose();  }
         //System.out.println("wait for ldap check thread closing ");
         for ( int i=0; i<lbList.size(); i++ ) {  
             
              LdapUserBlk lb=(LdapUserBlk) lbList.get(i);  
              if ( lb != null) {
                  if ( lbMain.debug ) System.out.println("wait for ldap check thread "+i+" closing ");
                  lb.setClose();
                  do {
                     try { Thread.sleep(50); } catch(Exception e) {}
                  }while( ! lb.isClosed() );
                  if ( lbMain.debug ) System.out.println("ldap check thread "+i+" are closed ");
              }
         }
         if ( lbMain.debug ) System.out.println("send close to ldapmodify thread");
         lbMod.setClose();
         do {
             try { Thread.sleep(50); } catch(Exception e) {}
         }while( ! lbMod.isClosed() );
     }
     
     static boolean forcedDown=false;
     static boolean debug=false;
     static boolean createDN=false;
     
     boolean modEnv=false;
     
     public void scanArgs(String[] args)  {
         
       try {
           
            String pwfile=""; String modpwfile=""; String ofile="";
            for (int i=0; i< args.length; i++ ) {
              if      ( args[i].matches("-h") && args.length>i+1) { hostname=args[++i];}
              else if ( args[i].matches("-p") && args.length>i+1) { port= Integer.parseInt(args[++i]);}
              else if ( args[i].matches("-a") && args.length>i+1) { userDN=args[++i];}
              else if ( args[i].matches("-D") && args.length>i+1) { userDN=args[++i];}
              else if ( args[i].matches("-P") && args.length>i+1) { userPWD=args[++i]; bind=true; }
              else if ( args[i].matches("-w") && args.length>i+1) { userPWD=args[++i]; bind=true; }
              else if ( args[i].matches("-j") && args.length>i+1) { pwfile=args[++i];}
              else if ( args[i].matches("-b") && args.length>i+1) { baseDN=args[++i];  bind=true; }
              else if ( args[i].matches("-f") && args.length>i+1) { filter=args[++i];}
	      else if ( args[i].matches("-B")                   ) { bind=true; }
              else if ( args[i].matches("-modh") && args.length>i+1) { hostnameMod=args[++i];                 modEnv=true;}
              else if ( args[i].matches("-modp") && args.length>i+1) { portMod= Integer.parseInt(args[++i]);  modEnv=true;}
              else if ( args[i].matches("-moda") && args.length>i+1) { userModDN=args[++i];                   modEnv=true;}
              else if ( args[i].matches("-modP") && args.length>i+1) { userModPWD=args[++i];    bind=true;    modEnv=true;}
              else if ( args[i].matches("-modj") && args.length>i+1) { modpwfile=args[++i];                   modEnv=true;}
              else if ( args[i].matches("-modssl")                 ) { protoMod="ldaps"; }
              else if ( args[i].matches("-o")       && args.length>i+1 ) { addCheckObj(args[++i]);}
              else if ( args[i].matches("-of")      && args.length>i+1 ) { ofile=args[++i];}
	      else if ( args[i].matches("-ssl")                        ) { proto="ldaps"; }
              else if ( args[i].matches("-size")    && args.length>i+1 ) { pageSize = Integer.parseInt(args[++i]); }
              else if ( args[i].matches("-help")                       ) { usage(true);}
              else if ( args[i].matches("-fdown")                      ) { forcedDown=true; }
              else if ( args[i].matches("-create")                     ) { createDN=true;   }
              else if ( args[i].matches("-d")                          ) { debug=true; }
              else { usage(true); }
            }
            
            String line; StringBuilder sb;
            if ( ! pwfile.isEmpty() ) {
                userPWD=getPW(pwfile);
            }
            if (! modpwfile.isEmpty()) {
                userModPWD=getPW(modpwfile);
            }
            if ( ! ofile.isEmpty() ) {
                ArrayList a = getSW(ofile); 
                for ( int i=0; i<a.size(); i++ ) { addCheckObj( ((String) a.get(i) ) );}
            }
            if ( lbList.size() == 0 ) {
                throw new Exception("missing value to check - see option -o or -of");
            }
            
          } catch (Exception e) {
              System.out.println("ERROR: parsing "+e.toString());
              usage(false);
              System.exit(-1);
          }  
       
         
     }
     
     public long getCount() { return count; } 
     public long getModified() { return modified; }
     
     public static LdapUserBlk getInstance(String[] ar) {
         LdapUserBlk ob = new LdapUserBlk();
                     if (ar.length == 0 ) { usage(true); }
                     ob.scanArgs(ar);
                     
                     return ob;
     }
     
     synchronized public void runSearch() {
         search();
         finishing();
     } 
     
     public static void main(String[] args) {
          LdapUserBlk ob = getInstance(args); 
                      ob.runSearch();
          System.out.println(ob.count+" ldap entries found \tmodified:"+ob.modified);
        
    }

    static ArrayList getSW(String fname) {
        String line; 
        ArrayList a = new ArrayList();
        if ( ! fname.isEmpty() ) {
            try {
                //System.out.println("verify file:"+fname);
                java.io.BufferedReader rb = new java.io.BufferedReader( new java.io.FileReader(fname) );
                do {
                    line=rb.readLine();
                    if ( line != null ) {  a.add(line); }
                } while ( line != null );
            } catch (Exception e){
                System.out.println("ERROR: problems to read from file "+fname+" \nreason:"+e.toString());
            }    
        }
        
        return a;
    }
    
    //static private Crypt crypt=new Crypt();
    
    static String getPW(String fname) {
        SecFile fa = new SecFile(fname);
        if ( fa.isReadableFile() ) {
             return fa.readOut().toString();
        }
        
        throw new RuntimeException("Error - not a readable file "+fname);
    }
    
    //private void log(String method, int level, String msg ) { log("LdapUserBlk", level, method+" - "+msg); }
} 

