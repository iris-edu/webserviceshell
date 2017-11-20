/*******************************************************************************
 * Copyright (c) 2017 IRIS DMC supported by the National Science Foundation.
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
import javax.ws.rs.core.MediaType;
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

    public String toString() {
        String delimiter = "  ";

        String s = String.join(delimiter,
            "wssMediaType: " + wssMediaType,
            "fdsnSS code: " + fdsnSS.getStatusCode(),
            "fdsnSS reason: " + fdsnSS.getReasonPhrase(),
            "briefErrMessage: " + briefErrMessage,
            "detailedErrMessage: " + detailedErrMessage,
            "headers: " + headers,
            "entity: " + entity);

        return s;
    }

////    // another version with StreamingOutput type for entity
////    public IrisProcessingResult(StreamingOutput so, String wssMediaType,
////          FdsnStatus.Status fdsnSS, Map<String, String> headers) {
////        this.entity = so;
////        this.wssMediaType = wssMediaType;
////        this.fdsnSS = fdsnSS;
////        this.headers = headers;
////    }

    /**
     * Method for standard FDSN error messages.
     *
     * @param fdsnStatus - Error status
     * @param message - A relatively short, user oriented  error message.
     * @return
     */
    public static IrisProcessingResult processError(FdsnStatus.Status fdsnStatus,
          String message) {
        IrisProcessingResult ipr = new IrisProcessingResult();

        ipr.fdsnSS = fdsnStatus;

        // keep entity type consistent with media type, don't need to set
        // entity content as WSS build new message in standard FDSN form.
        ipr.entity = "";
        ipr.wssMediaType = MediaType.TEXT_PLAIN;
        ipr.headers = null;

        ipr.briefErrMessage = message;
        ipr.detailedErrMessage = null;

        return ipr;
    }

    /**
     * Method for standard FDSN error message.
     *
     * @param fdsnStatus
     * @param headers - may be null
     * @param briefMessage - A relatively short, user oriented  error message.
     * @param detailedMessage - More detailed information, more system oriented
     *                          for identifying error location or WSS status.
     * @return
     */
    public static IrisProcessingResult processError(FdsnStatus.Status fdsnStatus,
          String briefMessage, String detailedMessage) {

        IrisProcessingResult ipr = new IrisProcessingResult();

        ipr.fdsnSS = fdsnStatus;

        // keep entity type consistent with media type, don't need to set
        // entity content as WSS build new message in standard FDSN form.
        ipr.entity = "";
        ipr.wssMediaType = MediaType.TEXT_PLAIN;
        ipr.headers = null;

        ipr.briefErrMessage = briefMessage;
        ipr.detailedErrMessage = detailedMessage;

        return ipr;
    }

    /**
     * Create result success result object for a streaming object.
     *
     * @param so - streaming object for data to be written to client
     * @param wssMediaType - corresponding media type for this streaming object
     * @return
     */
    public static IrisProcessingResult processStream(StreamingOutput so,
          String wssMediaType) {
        IrisProcessingResult ipr = new IrisProcessingResult();

        ipr.fdsnSS = FdsnStatus.Status.OK;

        ipr.entity = so;
        ipr.wssMediaType = wssMediaType;
        ipr.headers = null;

        ipr.briefErrMessage = null;
        ipr.detailedErrMessage = null;

        return ipr;
    }

    /**
     * Create result success result object for a streaming object.
     *
     * @param so - streaming object for data to be written to client
     * @param wssMediaType - corresponding media type for this streaming object
     * @param headers - corresponding headers that may override headers created
     *                  by WSS.
     * @return
     */
    public static IrisProcessingResult processStream(StreamingOutput so,
          String wssMediaType, Map<String, String> headers) {
        IrisProcessingResult ipr = new IrisProcessingResult();

        ipr.fdsnSS = FdsnStatus.Status.OK;

        ipr.entity = so;
        ipr.wssMediaType = wssMediaType;
        ipr.headers = headers;

        ipr.briefErrMessage = null;
        ipr.detailedErrMessage = null;

        return ipr;
    }

    /**
     * Create result success result object for a string object.
     *
     * @param str
     * @return
     */
    public static IrisProcessingResult processString(String str) {
        IrisProcessingResult ipr = new IrisProcessingResult();

        ipr.fdsnSS = FdsnStatus.Status.OK;

        ipr.entity = str;
        ipr.wssMediaType = MediaType.TEXT_PLAIN;
        ipr.headers = null;

        ipr.briefErrMessage = null;
        ipr.detailedErrMessage = null;

        return ipr;
    }
}
