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
