/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.macmario.io.buffer;

import com.macmario.general.Version;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * @author SuMario
 */
public class MapList extends Version{
    final private String _mkey;
    private MapList master=null; 
    public MapList() {
        this.declare = new HashMap<>();
        this.mapval = new HashMap<>();
        this._mkey=toString();        
        maplist.put(_mkey, ""+0);
        this.name="unknown"+com.macmario.io.crypt.Crypt.getRandomID();
        init();
    }
    
    public MapList(MapList ml) {
        this();
        this.master=ml;
    }
    
    private void init(){
        map.add(this);
    }
    
    private final HashMap<String,String>  mapval;
    private final HashMap<String,String> maplist  = new HashMap<>();
    private final ArrayList<MapList>      map     = new ArrayList<>();
    
    public MapList getMapList() { return this; }
    public MapList getMapList(int i) { return (i>=0 && i< map.size())?map.get(i):map.get(0);  }
    public MapList getMapList(String val) { return map.get( Integer.parseInt( maplist.get(val) ) ); }
    
    public String getValue(String name) { return getValue(name, getFirst() ); }
    public String getValue(String name, MapList list) {
        if ( isNullOrEmpty(list) || list.isValueEmpty() )  { return ""; }
        String s=list.mapval.get(name);
        
        return (s!=null)?s:"";
    }
    
    private MapList getFirst() { return map.get(0); }
    private MapList getLast(){        
        return map.get( (map.size()-1) );
    }
    
    public void setValue(String name, String value) { setValue(name,value, getFirst() ); }
    public void setValueOnLast(String name, String value) {
        
    }
    public void setValue(String name, String value, MapList list) { 
        list.mapval.put(name, value);
    }
    
    private String name="unknown";
    public String setName(String name) { this.name=name; return getName(); }
    public String getName()            { return this.name; }
    
    public MapList getNewList(MapList ml) { map.add(new MapList(ml)); return getLast(); }
    public MapList getMaster()  { return this.master; }
    
    public boolean isValueEmpty() {  return   mapval.isEmpty(); }
    public boolean isListEmpty()  {  return  maplist.size()>1; }
    public boolean isEmpty()      {  return ( isValueEmpty() && isListEmpty() ); }
    
    private final StringBuilder _com = new StringBuilder();
    public void   setComment(String s) {   _com.append(s.trim()).append("\n"); }
    public String getComment()         { return _com.toString(); }
    
    private final HashMap<String, String> declare;
    public void setValueType(String name, String typ) {
        declare.put(name, typ.toLowerCase());
    }
    public boolean isFunction(String name){ return testVal(name,"func"); }
    public boolean isValue(String name){ return testVal(name,"val"); }
    
    
    private boolean testVal(String name, String typ){
        String f=declare.get(name);
        return (f != null && typ!=null && f.equals(typ.toLowerCase()));
    }
    
    public String getInfo(String tab){
        StringBuilder sw = new StringBuilder();
        sw.append(tab).append(getName()).append( (tab.isEmpty())?"":" ="   ).append(" { \n");
                String l = getComment();
                if ( l.length() > 0 ) sw.append(tab+"\t"+l);
                Iterator<String> itter = mapval.keySet().iterator();
                while( itter.hasNext() ) {
                     String k=itter.next();
                     
                     sw.append(tab).append("\t").append(k).append(" = ")
                                   .append( (isValue(k))?"\"":"") .append( mapval.get(k) ).append( (isValue(k))?"\"":""  )
                                   .append( (!tab.isEmpty() && itter.hasNext() )?",":""  ).append("\n");
                }
                if ( map.size() > 1 ) {
                    for ( int i=1; i<map.size(); i++ ) {
                        sw.append( (map.get(i)).getInfo(tab+"\t") );
                    }
                }
        sw.append(tab).append("}\n");
        return sw.toString();
    }
    
}
