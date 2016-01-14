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
import javax.ws.rs.core.StreamingOutput;

/**
 *
 * @author mike
 */
public class IrisProcessingResult {
    // Entities are objects that the Jersey framework can use to
    // write output, like String, StreamingOutput, FileInputStream, etc
    public Object entity = null;
    
    // can set mediaType, otherwise pass through media type from framework,
    // - null or zero length string means ignore this value.
    public String wssMediaType = null;

    public FdsnStatus.Status fdsnSS = null;

    // Incoming header - value pairs, which may be provided
    // by the caller.
    // - may be null, may be empty
    public Map<String, String> headers = null;

    String briefErrMessage = null;
    String detailedErrMessage = null;

    private IrisProcessingResult() {
        //noop
    }

////    // another version with StreamingOutput type for entity
////    public IrisProcessingResult(StreamingOutput so, String wssMediaType,
////          FdsnStatus.Status fdsnSS, Map<String, String> headers) {
////        this.entity = so;
////        this.wssMediaType = wssMediaType;
////        this.fdsnSS = fdsnSS;
////        this.headers = headers;
////    }

    public static IrisProcessingResult createSuccessfulResult(
          StreamingOutput so, String wssMediaType, Map<String, String> headers) {

        IrisProcessingResult ipr = new IrisProcessingResult();
        ipr.fdsnSS = FdsnStatus.Status.OK;

        ipr.entity = so;
        ipr.wssMediaType = wssMediaType;
        ipr.headers = headers;

        return ipr;
    }

    public static IrisProcessingResult createErrorResult(
          FdsnStatus.Status fdsnSS, String wssMediaType,
          Map<String, String> headers, String briefMessage,
          String detailedMessage) {

        IrisProcessingResult ipr = new IrisProcessingResult();

        ipr.fdsnSS = fdsnSS;
        ipr.entity = "";
        ipr.wssMediaType = wssMediaType;
        ipr.headers = headers;

        ipr.briefErrMessage = briefMessage;
        ipr.detailedErrMessage = detailedMessage;

        return ipr;
    }

    public static IrisProcessingResult createSuccessfulResult(String str) {

        IrisProcessingResult ipr = new IrisProcessingResult();

        ipr.fdsnSS = FdsnStatus.Status.OK;
        ipr.entity = str;
        ipr.wssMediaType = "text/plain";
        ipr.headers = null;

        return ipr;
    }
}
