/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.file;

import java.io.ByteArrayInputStream;
import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author SuMario
 */
public class XMLReadFile extends ReadFile{
    
    private Document doc=null;
    private DocumentBuilder docBuilder=null;
    private DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    
    public XMLReadFile(String dir, String file) { super(dir, file); parse(); }        
    public XMLReadFile(String nfile){             super( nfile );   parse(); }
    public XMLReadFile(File file) {               super(file);      parse(); }
    
    private boolean __OK__=false; 
    
    private void parse() {
      if ( ! __OK__ ) {  
        try {   
          if ( docBuilder == null ) { docBuilder = docBuilderFactory.newDocumentBuilder(); }
          ByteArrayInputStream input =  new ByteArrayInputStream( readOut().toString().getBytes("UTF-8"));
          doc = docBuilder.parse(input);
          __OK__=true;
        }catch(Exception e) { __OK__=false;}  
      }
    }
    
    public void printOut() {
        
        try { 
         System.out.println("Root element :" 
            + doc.getDocumentElement().getNodeName());
         NodeList nList = doc.getElementsByTagName("student");
         System.out.println("----------------------------");
         for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);
            System.out.println("\nCurrent Element :" 
               + nNode.getNodeName());
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
               Element eElement = (Element) nNode;
                System.out.println("element:"+eElement);
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
    }
    
    
    public NodeList getNodeList(String name){
        NodeList nList = doc.getElementsByTagName(name);
        return nList;
    }
    public String getContext(String eName,String node) {
        NodeList nList = getNodeList(node);
         
         for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);
            System.out.println("\nCurrent Element :"  + nNode.getNodeName()+" NODE:"+nNode);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) nNode;
                System.out.println(temp+":"+eElement);
                if ( eElement != null ) {
                    NodeList m = eElement.getElementsByTagName(eName);
                    if ( m != null && m.getLength()>0) {
                        String t = m.item(0).getTextContent();
                        System.out.println(eName+" : " +t);
                    }    
                } else {
                    System.out.println(eName+" : NULL");
                }     
            }
        }
        return "";
    }
    
    public static void main(String[] args) {
        if ( args.length == 0 ) { throw new RuntimeException("ERROR: missing file propertie");}
        for(int i=0; i< args.length; i++) {
             XMLReadFile f = (new XMLReadFile(args[i]));
             f.printOut(); 
             
             f.getContext("NAME", "HOME");
        }
    }
}
