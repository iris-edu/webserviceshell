/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.iris.wss.provider;

import edu.iris.wss.framework.FdsnStatus;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author mike
 */
public class IrisProcessingResult {
//    public static StreamingOutput writer = null;
    public Object entity = null;
    public String wssMediaType = null;
    public FdsnStatus.Status fdsnSS = null;
    
    public IrisProcessingResult(Object entity, String wssMediaType,
          FdsnStatus.Status fdsnSS) {
        this.entity = entity;
        this.wssMediaType = wssMediaType;
        this.fdsnSS = fdsnSS;
    }
}
