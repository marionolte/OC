/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.net.tcp;

//import java.security.KeyStore;
import com.macmario.general.Version;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;





/**
 *
 * @author SuMario
 */
public class MyTrustManager extends Version implements X509TrustManager {
    private static Version v = new Version(){};
    private static boolean trustAll=false;
    private static boolean trustMyDomain=false;
    //private static String  trustDomain="";
    private static String   ksType="JKS";
    private static String   passwd="changeit";
    private static String   trustStoreFile="mytruststore";
    private static String   trustType="SunX509";
    private static String   trustProvider="SunJSSE";
    private static HashMap  trustedDom = new HashMap();
    
    private X509TrustManager X509TM=null;
    private KeyStore         ks=null;
    private TrustManager     tms []=null;
    
    
    private MyTrustManager(boolean trustall) {
         this();
         this.trustAll=trustall;
         
         try { 
            ks = KeyStore.getInstance(ksType);
            // ks.load(new FileInputStream(trustStoreFile), passwd.toCharArray());  
            ks.load(true, trustStoreFile, passwd);
            
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(trustType, trustProvider);  
            tmf.init(ks.keys);  
   
            tms = tmf.getTrustManagers();  
   
            for (int i = 0; i < tms.length; i++) {  
                if (tms[i] instanceof X509TrustManager) {  
                    X509TM = (X509TrustManager) tms[i];  
                    i=tms.length;  
                }  
            }
         } catch (Exception e ) {
             log("MyTrustManager(boolean trustall)",1,"set trustall certificates, because problems to load trust store - reason:"+e.toString());
             this.trustAll=true;
         }   
    }
    
    public MyTrustManager() {
          super();  
    }
    
    public synchronized TrustManager[] getTrustManager() { if(tms==null){tms=getTrustManager(true);};return tms; }
    public static synchronized MyTrustManager[] getTrustManager(boolean trustAll) {
        return  new MyTrustManager[]{ new MyTrustManager(trustAll) };
    }
    
    public static synchronized MyTrustManager[] getTrustManager(HashMap map) {
        log("getTrustManager(HashMap map)",3,"create TrustManager now");
        MyTrustManager[] tms=getTrustManager(MyTrustManager.trustAll);
        if ( map != null && ! map.isEmpty() ) {
           Iterator i = map.entrySet().iterator();
           while(i.hasNext()) {
                Map.Entry me = (Map.Entry)i.next();
                String s = ((String) me.getKey()).toLowerCase();
                String v = (String) me.getValue();
                log("getTrustManager(HashMap map)",3,"use key:"+s+":  value:"+v);
                if      ( s.matches("provider")      ) { tms[0].trustProvider=  v; }
                else if ( s.matches("trusttype")     ) { tms[0].trustType=      v; }
                else if ( s.matches("truststore")    ) { tms[0].ksType=         v; }
                else if ( s.matches("truststorefile")) { tms[0].trustStoreFile= v; }
                else if ( s.matches("password")      ) { tms[0].passwd=         v; }
                else if ( s.matches("trustall")      ) { tms[0].trustAll=       v.toLowerCase().matches("true")?true:false; }
                else if ( s.matches("trustmydomain") ) { tms[0].trustMyDomain=  v.toLowerCase().matches("true")?true:false; }
                else if ( s.matches("domaincount")   ) { 
                        int j = Integer.parseInt( (String) map.get("domaincount") );
                        for ( int k=0; k<j; k++){
                            log("getTrustManager(HashMap map)",3,"add domain:"+k+":  value:"+(String) map.get("domain"+k));
                            tms[0].trustedDom.put( (String) map.get("domain"+k), "trusted");
                        }
                        if ( j > 0 ) { tms[0].trustMyDomain=true; }
                        
                }
                /*else if ( s.matches("domain")        ) { MyTrustManager.trustDomain=   ((String) me.getValue()).toLowerCase(); 
                                                         if ( MyTrustManager.trustDomain == null ) { MyTrustManager.trustDomain=""; }
                                                         if ( ! MyTrustManager.trustDomain.isEmpty() ) {
                                                             MyTrustManager.trustMyDomain=true;
                                                         }
                }*/
                
            } 
           
            log("getTrustManager(HashMap map)",2," TRUSTALL:"+tms[0].trustAll+":  DOMAINTRUST:"+tms[0].trustMyDomain+":");
        } else {
            log("getTrustManager(HashMap map)",3,"creating standard trustManager - map is null or empty");
        }
        return tms;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        log("checkClientTrusted(X509Certificate[] chain, String authType)",2,"Client Certificate Chain - trustAll :"+this.trustAll+":");
        if ( this.trustAll ) { return ; }
        log("checkClientTrusted(X509Certificate[] chain, String authType)",2,"Client Certificate Chain - trusted domain certificate");
        if ( isMyDomainTrusted(chain) ) { return ; }
        log("checkClientTrusted(X509Certificate[] chain, String authType)",2,"Client Certificate Chain - standard validation");
        try {  
             X509TM.checkClientTrusted(chain, authType);  
         } catch (CertificateException ex) {  
             // do any special handling here, or rethrow exception.  
             log("checkClientTrusted(X509Certificate[] chain, String authType)",1,"Client Certificate Chain brocken : "+ex.toString() );
             throw new CertificateException(ex.toString());
         }  
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        log("checkServerTrusted(X509Certificate[] chain, String authType)",2,"Server Certificate Chain - trustAll :"+this.trustAll+":");
        if ( this.trustAll ) { return ; }
        log("checkServerTrusted(X509Certificate[] chain, String authType)",2,"Server Certificate Chain - trusted domain certificate");
        if ( isMyDomainTrusted(chain) ) { return ; }
        log("checkServerTrusted(X509Certificate[] chain, String authType)",2,"Server Certificate Chain - standard validation");
        try {  
             X509TM.checkServerTrusted(chain, authType);
        
        } catch (CertificateException ex) {  
             /* 
              * Possibly pop up a dialog box asking whether to trust the 
              * cert chain. 
              */
             log("checkServerTrusted(X509Certificate[] chain, String authType)",1,"Server Certificate Chain brocken : "+ex.toString() );
             throw new CertificateException(ex.toString());
         }  
        
        
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        if ( this.trustAll ) { return null ; }
        return X509TM.getAcceptedIssuers();
    }
    
    
    
    private boolean isMyDomainTrusted(X509Certificate[] chain) {
        if (trustAll) { return true; }
        String meth="isMyDomainTrusted(X509Certificate[] chain)";
        
        boolean b=false;
        log(meth,3,"validate IsMyDoamin certified domains:"+this.trustedDom.size()+" certificate:"+chain[0]);
        if ( MyTrustManager.trustMyDomain && MyTrustManager.trustedDom.size() > 0  && chain != null && chain.length >0 ) {
          try {
            Iterator i;
            log(meth,2,"like to test SubjectDN :"+chain[0].getSubjectDN().getName().toLowerCase()+": ");
          
            /*b=chain[0].getSubjectDN().getName().toLowerCase().contains( trustedDom[0]);
            if ( ! b && chain[0].getSubjectAlternativeNames() != null ) {
                 log(meth,2,"like to test SubjectAlternativeNames :"+chain[0].getSubjectAlternativeNames()+":");
                 i=chain[0].getSubjectAlternativeNames().iterator();
                 while(i.hasNext() ) {
                    List li = (List) i.next();
                    if ( li.get(0).toString().toLowerCase().contains(trustDomain) ) { b=true; break; }
                 }
            }
            
            if ( ! b ) {   
                log(meth,2,"like to test IssuerDN :"+chain[0].getIssuerDN()+":");
                b=chain[0].getIssuerDN().getName().toLowerCase().contains(trustDomain);
            }
            if ( ! b  && chain[0].getIssuerAlternativeNames() != null ) {
                log(meth,2,"like to test IssuerAlternativeNames :"+chain[0].getIssuerAlternativeNames()+":");
                i= chain[0].getIssuerAlternativeNames().iterator();
                while(i.hasNext()) {
                    List li = (List) i.next();
                    if ( li.get(0).toString().toLowerCase().contains(trustDomain) ) { b=true; break; }
                }
            }  */  
           } catch(Exception ex) {
              log(meth,1,"could not validate the first certificate chain[0]:"+chain[0].toString()+"\nexception :"+ex.toString());
           } 
        }
        //log(meth,2,"return "+b+" with domain:"+trustDomain+": and  with my domain trust:"+trustMyDomain+": ");
        return b;
    }
    
    
    private static void log(String method, int level, String msg ) { 
        printf("MyTrustManager", level, method+" - "+msg);
    }

}
