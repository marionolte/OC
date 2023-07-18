/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.macmario.comm.checker.web;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author SuMario
 */
public class Header implements Response, Request, Runnable {
     public static int debug=0;
     public Header( ) {
          init();
          Cookie.debug=Header.debug;
     }

    String respProto=null;
    String respHost=null;
    String respPort=null;
    String respQuery=null;
    String reqProto=null;
    String reqHost=null;
    String reqPort=null;
    String reqQuery=null;
    
    ArrayList<Cookie> setcookie = new ArrayList<Cookie>();
    ArrayList<Cookie> getcookie = new ArrayList<Cookie>();
    ArrayList<String[]> postmsg   = new ArrayList<String[]>();
    
    String authkey=null;
    ArrayList<String> authval=null;
    String reqauthkey=null;
    ArrayList<String> reqauthval=null;
    
    @Override
    public void   setResponse(String key, String value) {
        boolean set=true;
        log(2,"set response key="+key+"| value="+value+"|");
        if ( value != null && (key.matches("Location") || key.matches("Referer")) ) {
            try { 
                if ( value.startsWith("/")) {
                  value=    ((respProto==null)?"":respProto+"://")
                          + ((respHost ==null)?"":respHost+"")
                          + ((respPort ==null)?"":":"+respPort)+value;
                  log(2,"update response key="+key+"| with value="+value+"|  from:"
                          +((respProto==null)?"":respProto+"://")+"<= =>"
                          +((respHost ==null)?"":respHost+"")+"<= =>"
                          +((respPort ==null)?"":""+respPort)+"<=");
                } 
                URL u = new URL(value);
                respProto=(  (respProto!=null)?respProto:u.getProtocol() ); 
                respHost =(  (respHost !=null)?respHost :u.getHost() ); 
                respPort =(  (respPort!=null && u.getPort() == -1 )?respPort:":"+u.getPort()  ); 
                respQuery=( u.getQuery() != null )?u.getQuery():"/";
            } catch(Exception e) {
                log(3, "response has no url value");
            }
        } else if ( key.toLowerCase().startsWith("set-cookie")) {
             set=false;  Cookie c = new Cookie(value);
             log(2,"set response cookie=>"+c);
        } else if ( key.toLowerCase().contains("authenticate") ) {
             set=false;
             if ( this.authkey == null ) {
                  authkey=key;  this.authval=new ArrayList<String>();
             }
             log(2, "Response Authentication key="+key+"  value=|"+value+"|");
             authval.add(value);
             
        }
        if ( set ) {
            response.put(key, value); 
        }    
    }

    @Override
    public String getResponse(String key) {
        final String f=response.get(key);
        log(2,"get response of key="+key+"| value="+((f==null)?"null":f)+"|");
        return f; 
    }
    
    private UserAgent myagent=null;
    
    @Override
    public void   setRequest(String key, String value) { 
        boolean addkey=true;
        log(2,"set request key="+key+"| value="+value+"|");
        if ( value != null && (key.matches("HttpRequestUrl") || key.matches("Referer")) ) {
            try { 
                if ( value.startsWith("/")) {
                  value=    ((reqProto==null)?"":reqProto+"://")
                          + ((reqHost ==null)?"":reqHost+"")
                          + ((reqPort ==null)?"":":"+reqPort)+value;
                  log(2,"update request key="+key+"| with value="+value+"| from =>"+((reqProto==null)?"":reqProto+"://")+"<= =>"
                          + ((reqHost ==null)?"":reqHost+"")+"<= =>"
                          + ((reqPort ==null)?"":""+reqPort)+value
                     );
                } 
                URL u = new URL(value);
                reqProto=(  (reqProto!=null)?reqProto:u.getProtocol() ); 
                reqHost =(  (reqHost !=null)?reqHost :u.getHost() ); 
                reqPort =(  (reqPort !=null && u.getPort() == -1 )?reqPort:""+u.getPort()  ); 
                reqQuery=( u.getQuery() != null )? u.getQuery():"/";
                log(2," request  proto=>"+reqProto+"<=  host=>"+reqHost+"<= port=>"+reqPort+"<= query=>"+reqQuery+"<= " );
            } catch(Exception e) {
                log(3, "request has no url value");
            }
        }
        else if ( value != null && key.toLowerCase().matches("user-agent") ) {
            myagent = UserAgent.check(value);
        }
        else if ( key.toLowerCase().contains("authenticate") ) {
             addkey=false;
             if ( this.reqauthkey == null ) {
                  reqauthkey=key;  this.reqauthval=new ArrayList<String>();
             }
             reqauthval.add(value);
             log(2, "Request Authentication key="+key+"  value=|"+value+"|");
        }
        if ( addkey ) request.put(key, value); 
    }

    public void setPostMsg(String f) { 
        int i =  f.indexOf("=");
        String k=f.substring(0, i); 
        String v=""; if (i+1 <=f.length() ) {v=f.substring(i+1);}
        log(2,"setPostMsg::  base =>"+f+"<= key="+k+"|<=  value="+v+"|<=");
        this.postmsg.add( new String[]{ k, v} ); 
    }
    
    public void setRequestCookie(String f) {
         String[] sp = f.split("; ");
         for ( int i=0; i<sp.length; i++ ) {
            log(2,"request Cookie =>"+sp[i]+"<=");
            Cookie c = new Cookie(sp[i]);
            this.getcookie.add(c);
         }
    }
    
    @Override
    public String getRequest(String key) {
        final String f= request.get(key); 
        log(2,"get request of key="+key+"| value="+((f==null)?"null":f)+"|");
        return f; 
    }
    
    private boolean head=false;
    public void setHeader(boolean b) { head=b; }
    public boolean isHeader(String val) {
        /*boolean b=false;
        if( request.containsKey(val)  || val.startsWith("Content") || val.startsWith("Accept")  || val.startsWith("Cookie") || val.startsWith("Connection")) { b=true;}
        log(2, "val =>"+val+"<= is header ? "+b);
        return b; */
        return head;
    }
    
    private void init() {
        init_response();  init_request(); 
    }
    private void init_response() {
        for (int i=0; i<responseHeaders.length; i++) {
            request.put(responseHeaders[i], null);
        }
    }

    private void init_request() {
        for (int i=0; i<requestHeaders.length; i++) {
            request.put(requestHeaders[i], null);
        }
        
    }
    
    private void log(final int level, String msg) {
       if ( debug >= level  ) {
           if ( level > 0 ) { msg="DEBUG("+level+"/"+ debug +") Header:: =>"+msg; }
           System.out.println(msg);
       } 
    }

    public synchronized void start() {
        if ( completed ) { return; }
        log(3, "start:: awaiting completion");
        while( ! this.configDone ) { try { Thread.sleep(300); } catch(InterruptedException ie){} }
        log(3, "start:: header completed ");
        th = new Thread(this, "Header testing thread ");
        th.start();
        while( ! validated ) { try { Thread.sleep(300); } catch(InterruptedException ie){} }
        log(3, "start:: done");
    }
    private boolean configDone=false;
    private boolean completed=false;
    public  void setCheck(boolean b) { this.configDone=b; completed=false; }
    private Thread th;
    @Override
    public void run() {
         log(2, "Header checker started");
         check_validate();
         log(2, "Header checker completed");
         completed=true;
    }

    private boolean validated=false;
    private boolean validate_result=false;
    public  boolean validate() { return validate_result;}
    private void check_validate() {
        log(2, "validate header :"+this.toString());
        boolean b = validate_request();
        if ( ! b ) {validate_response();}
        validated=true;
        validate_result=b;
    }

    private boolean validate_request(){
        boolean b = true;
        String url = this.getRequest("HttpRequestUrl");
        if ( url != null && ! url.isEmpty() ) { 
               url = ( url.startsWith("/") )?"http://localhost"+url:url ; 
               log(2, "validate_request:: requestURL =>|"+url+"|<= ");
               b = checkUrl(url);
        } else {
               log(2, "Request URL are null or empty");
               b=false;
        }    
        return b;
    }
    
    private boolean validate_response(){
        boolean b = true;
        String url = this.getResponse("HttpResponseUrl");
        if ( url != null && ! url.isEmpty() ) {
               url = ( url.startsWith("/") )?"http://localhost"+url:url ; 
               b = checkUrl(url);
        } else {
               log(2, "Response URL are null or empty");
               b=false;
        }         
        if ( b ) {
               url = this.getResponse("Location");
               if ( url != null ) {
                    url = ( url.startsWith("/") )?"http://localhost"+url:url ; 
                      b = checkUrl(url);
               }
        }       
        return b;
    }
    
    private boolean checkUrl(String url) {
        boolean b=false;
        final String func="checkUrl(String url):: ";
        try {
            log(2, func+" test =>"+url+"<=");
            URL u = new URL(url);
            b=true;
        } catch(Exception e) {
            log(2, func+"error "+e.getMessage()+"  for url =>"+url+"<=");
            errmsg.append("EROROR: url "+url+" generates error "+e.getMessage());
        } finally { return b;}
    }
    
    public boolean isResponseAuthentication() { return (   this.authkey!=null)?true:false; }
    public boolean isRequestAuthentication()  { return (this.reqauthkey!=null)?true:false; }
    
    private StringBuilder errmsg=new StringBuilder();
    public String getErrorMsg() { return errmsg.toString(); }
    
    private StringBuilder body=new StringBuilder();
    
    public String getHeaderRequestMsg(){ 
        StringBuilder sw=new StringBuilder();
        Iterator<String> itter = request.keySet().iterator();
        sw.append(request.get("HttpRequestMethod") ).append(" ").append(request.get("HttpRequestUrl")).append(" ").append(request.get("HttpRequestVersion")).append("\n");
        while ( itter.hasNext() ) { String a=itter.next(); final String t=request.get(a);
              if ( t != null && ! a.startsWith("HttpRe"))
                sw.append(a).append(": ").append(t).append("\n"); 
        }
        if ( isRequestAuthentication() ) {
             for ( int i=0; i< this.authval.size() ;  i++ ) {
                 sw.append(reqauthkey).append(": ").append( reqauthval.get(i) ).append("\n");
             }
        }
        return sw.toString();
    }
    
    public String getHeaderResponseBody(){ return body.toString(); }
    public String getHeaderResponseMsg(){
        StringBuilder sw=new StringBuilder();
        Iterator<String> itter = response.keySet().iterator();
        sw.append(response.get("HttpResponseMethod") ).append(" ").append(response.get("HttpReturnCode")).append(" ").append(response.get("HttpReturnDescription")).append("\n");
                         itter = response.keySet().iterator();
        while ( itter.hasNext() ) { String a=itter.next(); final String t=response.get(a);
              if ( t != null && ! a.startsWith("HttpRe")) 
                sw.append(a).append(": ").append(t).append("\n"); 
        } 
        if ( isResponseAuthentication() ) {
             for ( int i=0; i< this.authval.size() ;  i++ ) {
                 sw.append(authkey).append(": ").append( authval.get(i) ).append("\n");
             }
        }
        return (sw.append("\n")).toString();
    }
    @Override
    public String toString() {
        StringBuilder sw=new StringBuilder();
        Iterator<String> itter = request.keySet().iterator();
        sw.append("__@@REQUEST@@_BEGIN\n").append(getHeaderRequestMsg()).append("\n");
        sw.append("__@@RESPONSE_HEAD@@__BEGIN\n").append(getHeaderResponseMsg()).append("__@@RESPONSE_HEAD@@__END\n");
        sw.append("__@@RESPONSE_BODY@@__BEGIN\n").append(getHeaderResponseBody()).append("__@@RESPONSE_BODY@@__END\n");
        /*sw.append(response.get("HttpResponseMethod") ).append(" ").append(response.get("HttpReturnCode")).append(" ").append(response.get("HttpReturnDescription")).append("\n");
                         itter = response.keySet().iterator();
        while ( itter.hasNext() ) { String a=itter.next(); final String t=response.get(a);
              if ( t != null && ! a.startsWith("HttpRe")) 
                sw.append(a).append(": ").append(t).append("\n"); 
        } 
        if ( isResponseAuthentication() ) {
             for ( int i=0; i< this.authval.size() ;  i++ ) {
                 sw.append(authkey).append(": ").append( authval.get(i) ).append("\n");
             }
        }
        sw.append("\n");*/
        
        if ( errmsg.length() > 0) { sw.append("\nERRORS:\n").append(getErrorMsg()).append("\n").append("errorlength:"+errmsg.length()+"\n"); }
        return sw.toString();
    } 
}
