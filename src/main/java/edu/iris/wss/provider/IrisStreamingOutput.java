/*******************************************************************************
 * Copyright (c) 2013 IRIS DMC supported by the National Science Foundation.
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

import edu.iris.wss.framework.FdsnStatus.Status;
import edu.iris.wss.framework.RequestInfo;
import edu.iris.wss.framework.ServiceShellException;
import edu.iris.wss.utils.LoggerUtils;
import java.io.OutputStream;
import java.util.Date;
import javax.ws.rs.core.StreamingOutput;
import org.apache.log4j.Logger;

public abstract class IrisStreamingOutput implements StreamingOutput,
      IrisProcessMarker {
	public static final Logger logger = Logger.getLogger(IrisStreamingOutput.class);

//    @Context 	ServletContext context;
//	@Context	javax.servlet.http.HttpServletRequest request;
//    @Context 	UriInfo uriInfo;
//    @Context 	HttpHeaders requestHeaders;
//
//    @Context 	SingletonWrapper sw;
	
	protected RequestInfo ri;

	// These are helper routines as part of the basic interface IrisStreamingOutput
//    public static void logUsageMessage(RequestInfo ri, String appSuffix,
//          Long dataSize, Long processTime,
//          String errorType, Status httpStatus, String extraText,
//          String network, String station, String location, String channel,
//          String quality, Date startTime, Date endTime, String duration) {
//
//        LoggerUtils.logWssUsageMessage(ri, appSuffix, dataSize, processTime,
//              errorType, httpStatus.getStatusCode(), extraText,
//              network, station, location, channel, quality,
//              startTime, endTime);
//    }

//	public static void logUsageMessage(RequestInfo ri, String appSuffix,
//			Long dataSize, Long processTime,
//			String errorType, Status httpStatus, String extraText) {
//
//		LoggerUtils.logWssUsageMessage(ri, appSuffix, dataSize, processTime,
//			errorType, httpStatus.getStatusCode(), extraText);
//	}

//	public static void logAndThrowException(RequestInfo ri, Status httpStatus,
//          String message) {
//		ServiceShellException.logAndThrowException(ri, httpStatus, message);
//    }
//
//	public static void logAndThrowException(RequestInfo ri, Status httpStatus,
//          String message, Exception ex) {
//		ServiceShellException.logAndThrowException(ri, httpStatus, message, ex);
//	}	
	
	public IrisStreamingOutput() { }

    /**
     * Required by web framework to stream data out.
     * 
     * @param os 
     */
    @Override
	public abstract void write(OutputStream os);

    /**
     * Do main processing here before the framework does write.
     * @return 
     */
	public abstract Status getResponse();

    /**
     * Called by exception handlers, the returned string should be
     * respective messages for the end user but include enough details
     * to isolate the source of the error.
     * 
     * @return 
     */
	public abstract String getErrorString();
	
    /**
     * Somewhat equivalent to an initialization, will be called first by
     * topExec so as to make contextual information available to implementer
     * 
     * @param ri 
     */
	public abstract void setRequestInfo(RequestInfo ri);
}
