/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package general;

import io.thread.RunnableT;

/**
 *
 * @author SuMario
 */
public abstract class Updater extends RunnableT {
    
    private static String serviceHost="www.macmario.de";
    private static int    servicePort=443;
    public  static String userver="https://"+serviceHost+":"+servicePort;
    public  static String updateUrl=userver+"/OCservice/";
    public  static String updateScript="OCUp2Date.php";
    
}
