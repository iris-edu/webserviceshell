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

package edu.iris.wss.IrisStreamingOutput;

import java.io.OutputStream;
import java.util.Date;

//import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import edu.iris.wss.framework.FdsnStatus.Status;
import edu.iris.wss.framework.RequestInfo;
import edu.iris.wss.framework.ServiceShellException;
import edu.iris.wss.utils.LoggerUtils;

public abstract class IrisStreamingOutput implements StreamingOutput {

	// These are helper routines as part of the basic interface IrisStreamingOutput
	public static void logUsageMessage(RequestInfo ri, String appSuffix,
			Long dataSize, Long processTime,
			String errorType, Status httpStatus, String extraText,
			String network, String station, String location, String channel, String quality,
			Date startTime, Date endTime, String duration) {
	
		LoggerUtils.logWssUsageMessage(ri, appSuffix, dataSize, processTime,
			errorType, httpStatus.getStatusCode(), extraText,
			network, station, location, channel, quality,
			startTime, endTime);
	}
	
	public static void logUsageMessage(RequestInfo ri, String appSuffix,
			Long dataSize, Long processTime,
			String errorType, Status httpStatus, String extraText) {
	
		LoggerUtils.logWssUsageMessage(ri, appSuffix, dataSize, processTime,
			errorType, httpStatus.getStatusCode(), extraText);
	}
	
	public static void logAndThrowException(RequestInfo ri, Status httpStatus, String message) {
		ServiceShellException.logAndThrowException(ri, httpStatus, message);
	}	
	
	public RequestInfo ri;
	
	public IrisStreamingOutput() { }

	@Override
	public abstract void write(OutputStream os);

	public abstract Status getResponse();
	public abstract String getErrorString();
	
	public abstract void setRequestInfo(RequestInfo ri);

}
