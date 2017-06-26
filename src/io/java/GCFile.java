/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.java;

import io.file.ReadFile;
import java.io.File;

/**
 *
 * @author SuMario
 */
public class GCFile extends ReadFile{
    public GCFile(String dir, String file) { super(dir,file); }
    public GCFile(            String file) { super(file); }
    public GCFile(            File   file) { super(file); }

    public void addLine(String s) {
        
    }
    
    
}
