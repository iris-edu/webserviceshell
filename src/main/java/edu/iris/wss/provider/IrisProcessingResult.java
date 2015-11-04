/*******************************************************************************
 * Copyright (c) 2015 IRIS DMC supported by the National Science Foundation.
 *  
 * This file is part of the Web Service Shell (WSS).
 *  
 * The WSS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * The WSS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * A copy of the GNU Lesser General Public License is available at
 * <http://www.gnu.org/licenses/>.
 ******************************************************************************/


package edu.iris.wss.provider;

import edu.iris.wss.framework.FdsnStatus;
import java.util.Map;

/**
 *
 * @author mike
 */
public class IrisProcessingResult {
    // Entities are objects that the Jersey framework can use to
    // write output, like String, StreamingOutput, FileInputStream, etc
    public Object entity = null;
    
    public String wssMediaType = null;
    public FdsnStatus.Status fdsnSS = null;
    
    // A store for incoming header, value pairs which may be provided
    // by handler, the header key should be trimmed and set to lowercase
    public Map<String, String> headers = null;
    
    /**
     * 
     * @param entity
     * @param wssMediaType
     * @param fdsnSS
     * @param headers - may be null
     */
    public IrisProcessingResult(Object entity, String wssMediaType,
          FdsnStatus.Status fdsnSS, Map<String, String> headers) {
        this.entity = entity;
        this.wssMediaType = wssMediaType;
        this.fdsnSS = fdsnSS;
        this.headers = headers;
    }
}
