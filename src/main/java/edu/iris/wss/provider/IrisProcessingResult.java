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
    public MediaType mediaType = null;
    public FdsnStatus.Status fdsnSS = null;
    
    public IrisProcessingResult(Object entity, MediaType mediaType,
          FdsnStatus.Status fdsnSS) {
        this.entity = entity;
        this.mediaType = mediaType;
        this.fdsnSS = fdsnSS;
    }
}
