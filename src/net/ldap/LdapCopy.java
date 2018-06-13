/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.ldap;

import io.file.ReadFile;
import io.file.SecFile;
import java.io.IOException;
import javax.naming.CompositeName;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchResult;
import net.tcp.Host;

/**
 *
 * @author SuMario
 */
public class LdapCopy extends LdapMain{

    private final String modprotocol;
    private final String host;
    private final String modhost;
    private int modport;
    private final String modbaseDN;
    private final String moduserdn;
    private final String moduserpw;
    private final String template;
    
    private LdapCopy() {
        final String func="LdapCopy::LdapCopy()";
        printf(func,4,"created");
        this.protocol   =(map.get("-ssl").equals("true"))?"ldaps":"ldap";
        this.modprotocol=(map.get("-modssl").equals("true"))?"ldaps":"ldap";
        printf(func,3,"protocol:"+protocol+":  modprotocol:"+modprotocol+":");
        
        this.host=((map.get("-h").equals("hostname")?Host.getHostname():map.get("-h")));
        this.modhost=(map.get("-modh").equals("modifyHost")?this.host:map.get("-modh"));
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
        
        this.baseDN=(map.get("-b" ).equals(map.get("_default_-b"))    )?getDefaultBaseDN():map.get("-b" );
        this.modbaseDN=(map.get("-bc").equals(map.get("_default_-bc")))?getDefaultBaseDN():map.get("-bc");        
        printf(func,3,"baseDN:"+baseDN+":  modbaseDN:"+modbaseDN+":");
        
        
           this.userdn=(map.get("-D" ).equals("adminDN"))?"cn=admin":map.get("-D" );
        this.moduserdn=(map.get("-modD" ).equals(map.get("_default_-modD" )))?"cn=admin":map.get("-modD" );
        printf(func,3,"userdn:"+userdn+":  moduserdn:"+moduserdn+":");
        
        filter=(! map.get("-f" ).equals(map.get("_default_-f")))?map.get("-f" ):"objectclass=*";
        
        
           this.userpw=( ! map.get("-j"    ).equals(map.get("_default_-j"))    )?  (new SecFile(  map.get("-j"   ) ).readOut().toString() ):"";
        if ( this.userpw.isEmpty() ) {
            this.userpw=( ! map.get("-w"    ).equals(map.get("_default_-w"))    )? map.get("-w") : "";
        }   
        this.moduserpw=( ! map.get("-modj" ).equals(map.get("_default_-modj")) )?  (new SecFile(  map.get("-modj") ).readOut().toString() ):"";
        
        auth="simple";
        
        this.template= ( ! map.get("-t").equals(map.get("_default_-t")))? ( new ReadFile( map.get("-t") ).readOut().toString() ):"";
        
        printf(func,0,"local:"+protocol+"://"+host+":"+port+"?"+userdn+"&"+userpw+"&"+baseDN+"&"+filter+"&\n"+
                      "remote:"+modprotocol+"://"+modhost+":"+modport+"?"+moduserdn+"&"+moduserpw+"&"+modbaseDN+"&\n"+
                      "filter:"+filter+":   objectlist:"+getAttrList());
    }
    
    LdapSearch ls  = null;
    LdapSearch lms = null;
    LdapModify lmm = null;
    synchronized public void copy() throws NamingException, IOException {
        final String func=getFunc("copy()");
        printf(func,0,"copy dn's  from "+map.get("-b")+" to "+map.get("-bc"));
        ls = LdapSearch.getInstance(new String[]{ (   protocol.equals("ldaps"))?"-ssl":"", "-h",hostname, "-p",""+port,    "-D",   userdn, "-w",   userpw, "-f",filter, "-a", auth, "-b", baseDN});
        
        NamingEnumeration find = ls.search();
        if ( find != null ) {
             while( find.hasMore() ) {
                 SearchResult entry = (SearchResult) find.next();
                 printf(func,3,"find entry:"+entry);
                 transport(entry);
             }
        }
    }
    private void transport(SearchResult entry) throws NamingException {
        if ( lmm == null ) {
           lmm = LdapModify.getInstance(new String[]{ (modprotocol.equals("ldaps"))?"-ssl":"", "-h", modhost, "-p",""+modport, "-D",moduserdn, "-w",moduserpw, "-f",filter, "-a", auth, "-b", modbaseDN} );
           lms = LdapSearch.getInstance(new String[]{ (modprotocol.equals("ldaps"))?"-ssl":"", "-h", modhost, "-p",""+modport, "-D",moduserdn, "-w",moduserpw, "-f",filter, "-a", auth, "-b", modbaseDN} );
        }
        Name ename = new CompositeName().add( ( (entry != null)? entry.getNameInNamespace().replaceAll(map.get("-b"), map.get("-bc")):map.get("-bc")) );
        
        System.out.println("ename:"+ename+"   entry:"+getNewDN(entry.getNameInNamespace(),map.get("-b"), map.get("-bc")) +"  base:"+map.get("-b")+":  newbase:"+map.get("-bc")+":");
        
    }
    
    private String getNewDN(String old,String base, String newbase) {
        System.out.println("old:"+old+" =>"+old.substring(0,old.length()-base.length()-1)+"<=");
        String[] sp = old.substring(0,old.length()-base.length()-1).split(",");
        StringBuilder sw = new StringBuilder();
        for(String s:sp) {
            if ( sw.length() >0 ) {sw.append(","); }
            
            sw.append(s);
        }
        
        return sw.toString()+newbase;
    }

    static private String myusage="\nusage():\noption: [-h hostname] [-p port] [-ssl] [-D adminDN] [-j passwordfile] [-modh modifyHost] [-modp port] [-modD adminDN] [-modj passwordfile] [-modssl] [-b baseDN] [-bc copyBaseDN] [-f filter] [-t copytemplate] [objectlist]\n";
    
    public static LdapCopy getInstance(String[] ar) {
        scanner(ar,myusage);
        return new LdapCopy();  
    }
    
    public static LdapCopy getInstance() {
        scanner(new String[]{},myusage);
        return new LdapCopy();
    }
    
    
    public static void main(String[] args) {
         LdapCopy lc = getInstance(args);    
    }
}
