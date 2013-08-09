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

package edu.iris.wss.framework;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import edu.iris.wss.IrisStreamingOutput.ProcessStreamingOutput;
import edu.iris.wss.framework.FdsnStatus.Status;

import org.apache.log4j.Logger;

import edu.iris.wss.utils.LoggerUtils;

public class ServiceShellException extends WebApplicationException {
	
	private static final long serialVersionUID = 1L;
	public static final Logger logger = Logger.getLogger(ServiceShellException.class);
	public static final Logger usageLogger = Logger.getLogger("UsageLogger");

	// [region] Constructors
	public ServiceShellException(final Status status) {
		super(Response.status(status).build());
	}
	
    public ServiceShellException(final Status status, final String message) {
        super(Response.status(status).
          entity(message + "\n").type("text/plain").build());
    }
    
    public ServiceShellException(WebApplicationException wae, String message) {
    	super(Response.status(wae.getResponse().getStatus()).
    	          entity(message + "\n").type("text/plain").build());
    }
    
    // [end region] 
    
    public static void logAndThrowException(RequestInfo ri, Status status) {
   		LoggerUtils.logWssUsageError(ri, null, 0L, 0L, null, status.getStatusCode(), null);
   		
		throw new ServiceShellException(status);
	}
    
    public static void logAndThrowException(RequestInfo ri, Status status, String message) {
    	if (message != null)
    		logAndThrowException(ri, status, message, null);
    	else
    		logAndThrowException(ri, status);
    }
    
    public static void logAndThrowException(RequestInfo ri, WebApplicationException wae, String message) {
        logAndThrowException(ri, Status.fromStatusCode(wae.getResponse().getStatus()), message, null);
    }

    public static final String usageDetailsSignature = "Usage Details ...";
    
    public static void logAndThrowException(RequestInfo ri, Status status, String message, Exception e) {
    	
    	if (ri.workingSubdirectory != null) {
    		ProcessStreamingOutput.deleteTempDirectory(new File(ri.workingSubdirectory));
    	}
    	
    	ri.statsKeeper.logError();
    	
    	logger.error(message + ": " + getErrorString(e));
    	
   		LoggerUtils.logWssUsageError(ri, null, 0L, 0L, message, status.getStatusCode(), null);
		
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd kk:mm:ss z");
    	StringBuilder sb = new StringBuilder();
    	sb.append("Error " + status.getStatusCode());
    	
    	int index = message.indexOf(usageDetailsSignature);
    	if (index == -1)
   			sb.append(": " + message);
    	else 
    		sb.append(": " + message.substring(0, index));
    	
    	if (e != null) sb.append("\n" + getErrorString(e));
    	
    	sb.append("\n\n" + "Request:\n");
    	sb.append(ri.request.getRequestURL());
    	String qs = ri.request.getQueryString();
    	if ((qs != null) && (!qs.equals("")))
    		sb.append("?" + qs);
    	
    	sb.append("\n\n" + "Request Submitted:\n");
    	sb.append(sdf.format(new Date()));
    	
   	
    	sb.append("\n\nService version:\n");
    	sb.append(ri.appConfig.getAppName() + ": v " + ri.appConfig.getVersion() + "\n");
    	
//    	sb.append(WebUtils.getCrazyHostPort(ri.request));
//    	logger.error(sb.toString());
    	throw new ServiceShellException(status, sb.toString());
    }
    
    public static String getErrorString(Throwable e) {
    	StringBuilder sb = new StringBuilder();
    	
    	if (e == null)  return "";
    	
    	if (e.getMessage() != null) {
    		sb.append(e.getMessage() + "\n");
    	}

		if (e.getCause() != null) {
			sb.append(getErrorString(e.getCause()));
		}
		return sb.toString();
    }
}
