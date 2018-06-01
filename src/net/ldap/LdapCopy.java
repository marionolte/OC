/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.ldap;

import io.file.SecFile;
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
            if ( this.port > Host.getMinPort() || this.port > Host.getMaxPort() ) {
                throw new RuntimeException("not a port");
            }
        } catch (Exception e) {
            this.port=(this.protocol.equals("ldaps"))?636:389;
        }
        printf(func,3,"port:"+port+":  host:"+host+":");
        
        try {
            this.modport=Integer.parseInt(map.get("-p"));
            if ( this.modport > Host.getMinPort() || this.modport > Host.getMaxPort() ) {
                throw new RuntimeException("not a port");
            }
        } catch (Exception e) {
            this.modport=(this.modprotocol.equals("ldaps"))?636:389;
        }
        printf(func,3,"modport:"+modport+":  modhost:"+modhost+":");
        
        System.out.println("b :"+map.get("-b" ));
        System.out.println("bc:"+map.get("-bc" ));
        System.out.println("getDefaultBaseDN:"+getDefaultBaseDN());
        this.baseDN=(map.get("-b" ).equals("baseDN")    )?getDefaultBaseDN():map.get("-b" );
        this.modbaseDN=(map.get("-bc").equals("copyBaseDN"))?getDefaultBaseDN():map.get("-bc");        
        printf(func,0,"baseDN:"+baseDN+":  modbaseDN:"+modbaseDN+":");
        
        
           this.userdn=(map.get("-D" ).equals("adminDN"))?map.get("-D" ):"cn=admin";
        this.moduserdn=(map.get("-modD" ).equals("copyBaseDN"))?map.get("-modD" ):"cn=admin";
        printf(func,0,"userdn:"+userdn+":  moduserdn:"+moduserdn+":");
        
        filter=(map.get("-f" ).equals("filter"))?map.get("-f" ):"objectclass=*";
        
        
           this.userpw=( map.get("-j"    ).equals("passwordfile")  )?  (new SecFile(  map.get("-j"   ) ).readOut().toString() ):"";
        this.moduserpw=( map.get("-modj" ).equals("passwordfile")  )?  (new SecFile(  map.get("-modj") ).readOut().toString() ):"";
        auth="simple";
        
        printf(func,0,"local:"+protocol+"://"+host+":"+port+"?"+userdn+"&"+userpw+"&"+baseDN+"&"+filter+"&\n"+
                      "remote:"+modprotocol+"://"+modhost+":"+modport+"?"+moduserdn+"&"+moduserpw+"&"+modbaseDN+"&\n"+
                      "filter:"+filter+":   objectlist:..");
    }

    static private String myusage="\nusage():\noption: [-h hostname] [-p port] [-ssl] [-D adminDN] [-j passwordfile] [-modh modifyHost] [-modp port] [-modD adminDN] [-modj passwordfile] [-modssl] [-b baseDN] [-bc copyBaseDN] [-f filter] [objectlist]\n";
    
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
