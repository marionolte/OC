/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.buffer;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author SuMario
 */
public class MapList {
    final private String _mkey;
    public MapList() {
        this._mkey=this.toString();
        map.add(this);
        maplist.put(_mkey, ""+0);
    }
    
    private HashMap<String,String>  mapval  = new HashMap<String,String>();
    private HashMap<String,String> maplist  = new HashMap<String,String>();
    private ArrayList<MapList>      map     = new ArrayList();
    
    public MapList getMapList() { return this; }
    public MapList getMapList(int i) { return (i>=0 && i< map.size())?map.get(i):map.get(0);  }
    public MapList getMapList(String val) { return map.get( Integer.parseInt( maplist.get(val) ) ); }
    
    public String getValue(String name) { return getValue(name, getFirst() ); }
    public String getValue(String name, MapList list) {
        if ( list != null || list.isValueEmpty() )  { return ""; }
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
    
    public boolean isValueEmpty() {  return   mapval.isEmpty(); }
    public boolean isListEmpty()  {  return  maplist.size()>1; }
    public boolean isEmpty()      {  return ( isValueEmpty() && isListEmpty() ); }
}
