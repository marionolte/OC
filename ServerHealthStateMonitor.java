package net.wls; 

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;
//import weblogic.management.jmx.MBeanServerInvocationHandler;
import java.util.Hashtable;
import java.io.IOException;
import java.net.MalformedURLException;

public class ServerHealthStateMonitor {
    private static MBeanServerConnection connection;
    private static JMXConnector connector;
    private static final ObjectName service;
    private static String combea = "com.bea:Name=";
    private static String service1 = "DomainRuntimeService,Type=weblogic.management.mbeanservers.domainruntime.DomainRuntimeServiceMBean";
    private static String service2 = "RuntimeService,Type=weblogic.management.mbeanservers.runtime.RuntimeServiceMBean";

    static {
        try {
            service = new ObjectName(combea + service1);
        } catch (MalformedObjectNameException e) {
            throw new AssertionError(e.getMessage());
        }
    }

    public static void initConnection (String host, String portS, String username, String password) throws IOException,MalformedURLException {
        String protocol = "t3";
        Integer portInteger = Integer.valueOf(portS);
        int port = portInteger.intValue();
        String jndiroot = "/jndi/";
        String mserver = "weblogic.management.mbeanservers.domainruntime";
        System.out.println("config service url");
        //JMXServiceURL serviceURL = new JMXServiceURL(protocol, hostname, port, jndiroot + mserver);
        JMXServiceURL serviceURL = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi");
        Hashtable h = new Hashtable();
        h.put(Context.SECURITY_PRINCIPAL, username);
        h.put(Context.SECURITY_CREDENTIALS, password);
        h.put(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES,"weblogic.management.remote");
        h.put("jmx.remote.x.request.waiting.timeout", new Long(1000));
        connector = JMXConnectorFactory.connect(serviceURL, h);
        System.out.println("conn create");
        connection = connector.getMBeanServerConnection();
    }

    public static ObjectName[] getServerRuntimes() throws Exception {
        return (ObjectName[]) connection.getAttribute(service,"ServerRuntimes");
    }

    public void printNameAndState() throws Exception
    {
        ObjectName arr[]=getServerRuntimes();
        for(ObjectName temp : arr)
        System.out.println("nt servers: "+temp);
        ObjectName domain = (ObjectName) connection.getAttribute(service,"DomainConfiguration");
        System.out.println("Domain: " + domain.toString());
        ObjectName[] servers = (ObjectName[]) connection.getAttribute(domain,"Servers");
        for (ObjectName server : servers)
        {
            String aName = (String) connection.getAttribute(server,"Name");
            try{
                ObjectName ser= new ObjectName("com.bea:Name="+aName+",Location="+aName+",Type=ServerRuntime");
                String serverState=(String) connection.getAttribute(ser,"State");
                System.out.println("nt Server: "+aName+"t State: "+serverState);
                weblogic.health.HealthState serverHealthState=( weblogic.health.HealthState) connection.getAttribute(ser,"HealthState");
                int hState=serverHealthState.getState();
                if(hState==weblogic.health.HealthState.HEALTH_OK)
                System.out.println("t Server: "+aName+"t State Health: HEALTH_OK");
                if(hState==weblogic.health.HealthState.HEALTH_WARN)
                System.out.println("t Server: "+aName+"t State Health: HEALTH_WARN");
                if(hState==weblogic.health.HealthState.HEALTH_CRITICAL)
                System.out.println("t Server: "+aName+"t State Health: HEALTH_CRITICAL");
                if(hState==weblogic.health.HealthState.HEALTH_FAILED)
                System.out.println("t Server: "+aName+"t State Health: HEALTH_FAILED");
                if(hState==weblogic.health.HealthState. HEALTH_OVERLOADED)
                System.out.println("t Server: "+aName+"t State Health: HEALTH_OVERLOADED");
            }
            catch(javax.management.InstanceNotFoundException e)
            {
                System.out.println("nt Server: "+aName+"t State: SHUTDOWN (or Not Reachable)");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if ( args.length < 4 ) {
            System.out.println("usage() - missing parameter:  <host> <port> <user> <userpw> ");
            System.exit(-1);
        }
        String hostname = args[0];
        String portString = args[1];
        String username = args[2];
        String password = args[3];
        ServerHealthStateMonitor s = new ServerHealthStateMonitor();
        System.out.println("init connect");
        initConnection(hostname, portString, username, password);
        System.out.println("connect complete");
        s.printNameAndState();
        try{ Thread.sleep(1000); } catch(Exception e) {}
        System.out.println("close");
        connector.close();
    }

}

