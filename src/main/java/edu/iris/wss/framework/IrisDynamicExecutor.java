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

import edu.iris.wss.IrisStreamingOutput.IrisStreamingOutput;
import edu.iris.wss.endpoints.CmdProcessorIrisEP;
import edu.iris.wss.framework.FdsnStatus.Status;
import edu.iris.wss.framework.ParameterTranslator;
import edu.iris.wss.framework.RequestInfo;
import edu.iris.wss.framework.ServiceShellException;
import edu.iris.wss.framework.SingletonWrapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.apache.log4j.Logger;

public class IrisDynamicExecutor {
	public static final Logger logger = Logger.getLogger(IrisDynamicExecutor.class);

    @Context 	ServletContext context;
	@Context	javax.servlet.http.HttpServletRequest request;
    @Context 	UriInfo uriInfo;	
    @Context 	HttpHeaders requestHeaders;

    @Context 	SingletonWrapper sw;
	
	public IrisDynamicExecutor() {
        System.out.println("***************&&& IrisDynamicExecutor constr");
    }

    /**
     * This does execution for dynamic endpoints, it wraps what was
     * the in Wss.java as query.
     * 
     * The steps for running 
     * wss does setRequestInfo
     * wss does getResponse
     * framework does write
     * 
     * error handling will call getErrorString.
     * 
     * @return 
     */
    public Response doIrisStreaming() throws IOException {        
        // when run dynamically, this method does all the abstract methods,
        // so ri needs to be set here
        System.out.println("** doIrisStreaming ");
        RequestInfo ri = RequestInfo.createInstance(sw, uriInfo, request,
              requestHeaders);
    
        String requestedEpName = ri.getEndpointNameForThisRequest();
        System.out.println("** doIrisStreaming requestedEpName: " + requestedEpName);
        if (ri.isConfiguredForThisEndpoint()){
            // noop, continue with ;
        } else {
            shellException(Status.INTERNAL_SERVER_ERROR,
                  "Error, there is no configuration information for"
                        + " endpoint: " + requestedEpName, ri);
        }

		ArrayList<String> cmd = null;

        // No object existance check done here as it should have been
        // done when the configuration parameters were loaded
        IrisStreamingOutput iso = ri.appConfig.getIrisEndpointClass(requestedEpName);
    
        // until some other mechanism exist, use our command line processor
        // classname to determine if the handlerProgram name should be
        // pulled in to cmd
        System.out.println("** Iris SO class name: " + iso.getClass().getName());
        System.out.println("** Iris so test  name: " + edu.iris.wss.endpoints.CmdProcessorIrisEP.class.getName());
        if (iso.getClass().getName().equals(edu.iris.wss.endpoints.CmdProcessorIrisEP.class.getName())) {
            // i.e. if it is a command based class, use CmdProcessing class
            // TBD get handler name here
            String handlerName = ri.appConfig.getHandlerProgram(requestedEpName);
            System.out.println("***************** handlerPName: " + handlerName);
            cmd = new ArrayList<String>(Arrays.asList(handlerName.split(" ")));
        } else {
            cmd = new ArrayList<String>();
        }
        
		try {
			ParameterTranslator.parseQueryParams(cmd, ri, requestedEpName);
		} catch (Exception e) {
			shellException(Status.BAD_REQUEST, "Wss - " + e.getMessage(), ri);
		}
                
        
        System.out.println("************ja*after cmd.len: " + cmd.size());
        System.out.println("************ja*after cmd: " + cmd);
        if (cmd.size() > 0) {System.out.println("************ja*after cmd.get(0): " + cmd.get(0));}

    
                
            
        if (ri.request.getMethod().equals("HEAD")) {
            // return to Jersey before any more processing
            String noData = "";
            Response.ResponseBuilder builder = Response.status(Status.OK)
                  .type("text/plain")
                  .entity(noData);
            
            addCORSHeadersIfConfigured(builder, ri);
            return builder.build();
        }

                
        iso.setRequestInfo(ri);

		// Wait for an exit code, expecting the start of data transmission
        // or exception or timeout.
		Status status = iso.getResponse();
        System.out.println("** IrisDynamicExecutor after iso.getResponse, status: " + status);
    	if (status == null) {
            shellException(Status.INTERNAL_SERVER_ERROR,
                  "Null status from IrisStreamingOutput class", ri);
        }
        
        status = adjustByCfg(status, ri);
        if (status != Status.OK) {
            newerShellException(status, ri, iso);
		}

        String mediaType = null;
        String outputTypeKey = null;
        try {
            outputTypeKey = ri.getPerRequestOutputTypeKey(requestedEpName);
            mediaType = ri.getPerRequestMediaType(requestedEpName);
        } catch (Exception ex) {
            shellException(Status.INTERNAL_SERVER_ERROR, "Unknow mediaType for"
                    + " mediaTypeKey: " + outputTypeKey
                    + ServiceShellException.getErrorString(ex), ri);
        }

        Response.ResponseBuilder builder = Response.status(status)
              .type(mediaType)
              .entity(iso);

        try {
            builder.header("Content-Disposition", ri.createContentDisposition(requestedEpName));
        } catch (Exception ex) {
            shellException(Status.INTERNAL_SERVER_ERROR,
                  "Error creating Content-Disposition header value"
                        + " endpoint: " + requestedEpName
                        + ServiceShellException.getErrorString(ex), ri);
        }
System.out.println("** IrisDynamicExecutor almost end: ");
        addCORSHeadersIfConfigured(builder, ri);
		return builder.build();
    }
    
    private void addCORSHeadersIfConfigured(Response.ResponseBuilder rb, RequestInfo ri) {
		if (ri.appConfig.isCorsEnabled()) {
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
	
	private void shellException(Status status, String message, RequestInfo ri) {
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
