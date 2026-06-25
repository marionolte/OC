/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.comm.oci.config;

import com.macmario.general.Version;
import java.io.File;

/**
 *
 * @author SuMario
 */
public class OciConfig extends Version{
    
    static public String  ociConfig=System.getProperty("user.dir")+File.separator+".oci"+File.separator+"config";
    static public String ociProfile="Default";
    
}
