/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.macmario.general;

//import com.oracle.OraConst;
import com.macmario.io.file.ReadFile;
import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.util.logging.Logger;
import com.macmario.net.tcp.Host;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

/**
 *
 * @author SuMario
 */
public abstract class Version  extends Functions { //extends OraConst {
    final public static String mhfile="OC.jar";
    final public static String mh="MarioHelpService";
    final public static String mhservice="MHService - "+mhfile;
    final public static String prodauthor="Mario Nolte";
    final public static int majorVersion=0;
    final public static int minorVersion=0;
    final public static int patchVersion=8;
    final public static int fixedVersion=1;
    final public static int   libVersion=0;
    final public static int  betaVersion=1;
    
    
}
