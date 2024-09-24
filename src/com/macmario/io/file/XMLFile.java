/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.macmario.io.file;


import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import com.macmario.io.file.ReadFile;

/**
 *
 * @author SuMario
 */
public class XMLFile extends ReadFile {
    
    private Document doc=null;
    private DocumentBuilder docBuilder=null;
    private DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    

    public XMLFile(String dir, String file) {
        super(dir, file);
    }
    
    public XMLFile(String nfile){
        this( new File(nfile) );
        
    }

    public XMLFile(File file) {
         super(file);
         log(2, "XMLFile:"+file+"created");
    }

    public boolean isXML() {
        boolean b=false;
        try {
           if ( docBuilder==null) docBuilder= docBuilderFactory.newDocumentBuilder();
           doc = docBuilder.parse( this.getFile() );          
           b=true;
        } catch (Exception e) {
           log(3, this.getFileName()+" is not a XML file - reason:"+e.getMessage());  
        } finally {
            return b;
        }    
    }
    
    public NodeList getChildNodes() { return (doc!=null)?doc.getChildNodes():null; }
    
    public static int debug=0;
    private void log(final int level, String msg) {
       if ( debug >= level  ) {
           if ( level > 0 ) { msg="DEBUG("+level+"/"+ debug +") XMLFile:: =>"+msg; }
           System.out.println(msg);
       } 
    }

}
