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

import edu.iris.wss.framework.FdsnStatus.Status;
import edu.iris.wss.framework.RequestInfo;
import edu.iris.wss.framework.ServiceShellException;
import edu.iris.wss.framework.SingletonWrapper;
import edu.iris.wss.utils.LoggerUtils;
import java.io.OutputStream;
import java.util.Date;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import org.apache.log4j.Logger;

public abstract class IrisStreamingOutput implements StreamingOutput {
	public static final Logger logger = Logger.getLogger(IrisStreamingOutput.class);

    @Context 	ServletContext context;
	@Context	javax.servlet.http.HttpServletRequest request;
    @Context 	UriInfo uriInfo;	
    @Context 	HttpHeaders requestHeaders;

    @Context 	SingletonWrapper sw;

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

    /**
     * This drives execution for dynamic endpoints, it wraps what was
     * the IRIS processing steps for webserviceshell query endpoint.
     * 
     * controls the life cycle of the implementing class, i.e.
     * wss does setRequestInfo
     * wss does getResponse
     * framework does write
     * 
     * error handling will call getErrorString.
     * 
     * @return 
     */
    public Response doIrisStreaming() {
        RequestInfo localRi = RequestInfo.createInstance(sw, uriInfo, request, requestHeaders);
        System.out.println("***************&&& doIrisStreaming");
        logger.info("doIrisStreaming" + "do I keep this message here");
        setRequestInfo(localRi);
		
		// Wait for an exit code, expecting the start of data transmission
        // or exception or timeout.
		Status status = getResponse();
    	if (status == null) {
            shellException(Status.INTERNAL_SERVER_ERROR, "Null status from StreamingOutput class");
        }
        
        status = adjustByCfg(status, ri);
        if (status != Status.OK) {
            newerShellException(status, ri, this);
		}

        String mediaType = null;
        try {
            mediaType = ri.getPerRequestMediaType();
        } catch (Exception ex) {
            shellException(Status.INTERNAL_SERVER_ERROR, "Unknow mediaType for"
                    + " mediaTypeKey: " + ri.getPerRequestOutputTypeKey()
                    + ServiceShellException.getErrorString(ex));
        }
    
        Response.ResponseBuilder builder = Response.status(status)
              .type(mediaType)
              .entity(this);
        builder.header("Content-Disposition", ri.createContentDisposition());

        addCORSHeadersIfConfigured(builder, ri);

		return builder.build();
    }
    
    private void addCORSHeadersIfConfigured(Response.ResponseBuilder rb, RequestInfo ri) {
		if (ri.appConfig.getCorsEnabled()) {
            // Insert CORS header elements.
		    rb.header("Access-Control-Allow-Origin", "*");

            // dont add this unless cookies are expected
//            rb.header("Access-Control-Allow-Credentials", "true");

            // Not setting these at this time - 2015-08-12
//            rb.header("Access-Control-Allow-Methods", "HEAD, GET, POST");
//            rb.header("Access-Control-Allow-Headers", "Content-Type, Accept");

            // not clear if needed now, 2015-08-12, but this is how to let client
            // see what headers are available, although "...Allow-Headers" may be
            // sufficient
//            rb.header("Access-Control-Expose-Headers", "X-mycustomheader1, X-mycustomheader2");
		}
    }
	
	private void shellException(Status status, String message) {
		ServiceShellException.logAndThrowException(ri, status, message);       
	}
	
	private static void newerShellException(Status status, RequestInfo ri, 
            IrisStreamingOutput iso) {
		ServiceShellException.logAndThrowException(ri, status,
                status.toString() + iso.getErrorString());
	}

    private static Status adjustByCfg(Status trialStatus, RequestInfo ri) {
        if (trialStatus == Status.NO_CONTENT) {
            // override 204 if configured to do so
            if (ri.perRequestUse404for204) {
                return Status.NOT_FOUND;
            }
        }
        return trialStatus;
    }
}
