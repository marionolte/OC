/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.wls;

import main.MainTask;

/**
 *
 * @author SuMario
 */
public class WlsNodeManager extends MainTask {
      private boolean bosted;
      private String  domain;
      public WlsNodeManager(String[] args, String dom) {
          super(args, "WlsNodeManager");
          this.bosted=( dom == null || dom.isEmpty() ); 
          this.domain=dom;
      }
    
      public WlsNodeManager(String dom) {
          this(new String[]{}, dom);
      }
}
