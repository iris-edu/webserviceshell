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

import edu.iris.wss.framework.FdsnStatus.Status;
import edu.iris.wss.framework.ParameterTranslator;
import edu.iris.wss.framework.RequestInfo;
import edu.iris.wss.framework.ServiceShellException;
import edu.iris.wss.framework.WssSingleton;
import static edu.iris.wss.framework.WssSingleton.ACCESS_CONTROL_ALLOW_ORIGIN;
import static edu.iris.wss.framework.WssSingleton.CONTENT_DISPOSITION;
import edu.iris.wss.framework.Util;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.apache.log4j.Logger;
import org.glassfish.jersey.server.ContainerRequest;

public class IrisDynamicProvider {
	public static final Logger logger = Logger.getLogger(IrisDynamicProvider.class);

    @Context 	ServletContext context;
	@Context	javax.servlet.http.HttpServletRequest request;
    @Context 	UriInfo uriInfo;	
    @Context 	HttpHeaders requestHeaders;

    @Context 	WssSingleton sw;

    @Context    ContainerRequestContext containerRequestContext;

	public IrisDynamicProvider() {
        //System.out.println("***************&&& IrisDynamicExecutor constr");
    }

    /**
     * A test function, which when called, should echo back the contents
     * of a post request.
     * @return
     * @throws IOException
     */
    public Response echoPostString() throws IOException {
        // when run dynamically, this method does all the abstract methods,
        // so ri needs to be set here and past in.
        RequestInfo ri = RequestInfo.createInstance(sw, uriInfo, request,
              requestHeaders);

        String requestedEpName = ri.getEndpointNameForThisRequest();

        if (!ri.isThisEndpointConfigured()) {
//            Util.shellException(Status.INTERNAL_SERVER_ERROR,
//                  "Error, there is no configuration information for"
//                        + " endpoint: " + requestedEpName, ri);
            System.out.println("* echoPostString warning, no configuration for endpoint: "
                  + requestedEpName);
        }

        System.out.println("* echoPostString method: " + containerRequestContext.getMethod());
        System.out.println("* echoPostString toString: " + containerRequestContext.toString());
        System.out.println("* echoPostString getLength: " + containerRequestContext.getLength());

//        String postContent = ((ContainerRequest) containerRequestContext).readEntity(String.class);
//        System.out.println("* echoPostString readEntity: " + postContent);

        // cannot do both readEntity and read InputStream
		StringBuilder sb = new StringBuilder(2048);

        InputStream is = containerRequestContext.getEntityStream();
		byte [] buffer = new byte[1024];

		try {
			int nRead;
			while ((nRead = is.read(buffer, 0, buffer.length)) != -1) {
				sb.append(new String(buffer, 0, nRead));
			}
		} catch(IOException ex) {
			System.out.println("Got IO exception in doIrisStreamin2, ex: " + ex);
		} finally {
			try{ is.close(); } catch( Exception ex) {;}
		}

        System.out.println("* echoPostString entityStream: " + sb.toString());

        Response.ResponseBuilder builder = Response.status(Status.OK)
                  .type("text/plain")
                  .entity(sb.toString());

        return builder.build();
    }

    /**
     * This does execution for dynamic endpoints, it wraps what was
     * the in Wss.java as query.
     * 
     * Concept is the following steps are done
     * - this methid is loaded as an endpoint in MyApplication based on
     *   configuration information in service.cfg
     * - when a resepective endpoint is called, the framework calls
     *   this method
     * - this method captures useful information in RequestInfo
     * - this method checks parameters and parameter type
     * - this method calls respective methods in IrisStreamingOutput
     * - this method may customize the response, then returns a
     *   response to the framework
     *
     * for error handling, error messages should be available in the
     * user code via a call to getErrorString.
     * 
     * @return 
     * @throws java.io.IOException
     */
    public Response doIrisStreaming() throws IOException {
        // when run dynamically, this method does all the abstract methods,
        // so ri needs to be set here, e.e first
        RequestInfo ri = RequestInfo.createInstance(sw, uriInfo, request,
              requestHeaders);

        String requestedEpName = ri.getEndpointNameForThisRequest();

        if (!ri.isThisEndpointConfigured()) {
            Util.logAndThrowException(ri, Status.INTERNAL_SERVER_ERROR,
                  "Error, there is no configuration information for"
                        + " endpoint: " + requestedEpName);
        }

        if (containerRequestContext.getMethod().equals("POST")) {
            if (containerRequestContext != null) {
                ri.postBody = ((ContainerRequest) containerRequestContext)
                      .readEntity(String.class);
            } else {
                ri.postBody = "";
            }
        }

		ArrayList<String> cmd = null;

        // No object existance check done here as it should have been
        // done when the configuration parameters were loaded
        IrisStreamingOutput iso =
              (IrisStreamingOutput)ri.appConfig.getIrisEndpointClass(requestedEpName);

        // until some other mechanism exist, use our command line processor
        // classname to determine if the handlerProgram name should be
        // put into cmd array
        if (iso.getClass().getName().equals(
              edu.iris.wss.endpoints.CmdProcessorIrisEP.class.getName())) {
            // i.e. if it is a command processing class, there must be
            // a command handler
            // Note: not error checking file handler existance or if
            //       executable, this should be done when the configuration
            //       is loaded at startup.
            String handlerName = ri.appConfig.getHandlerProgram(requestedEpName);
            cmd = new ArrayList<>(Arrays.asList(handlerName.split(
                  Pattern.quote(" "))));
        } else {
            cmd = new ArrayList<>();
        }

		try {
			ParameterTranslator.parseQueryParams(cmd, ri, requestedEpName);
		} catch (Exception e) {
			Util.logAndThrowException(ri, Status.BAD_REQUEST,
                  "doIrisStreaming - " + e.getMessage());
		}
            
        if (ri.request.getMethod().equals("HEAD")) {
            System.out.println("** doIrisStreaming, returning head request: "
                  + " cmd: " + cmd);
            // return to Jersey before any more processing
            String noData = "";
            Response.ResponseBuilder builder = Response.status(Status.OK)
                  .type("text/plain")
                  .entity(noData);
            
            Util.addCORSHeadersIfConfigured(builder, ri, null);
            return builder.build();
        }

        iso.setRequestInfo(ri);
		// Wait for an exit code, expecting the start of data transmission
        // or exception or timeout.
		Status status = iso.getResponse();
        System.out.println("** doIrisStreaming after iso.getResponse, status: "
              + status);
    	if (status == null) {
            Util.logAndThrowException(ri, Status.INTERNAL_SERVER_ERROR,
                  "Null status from IrisStreamingOutput class");
        }

        status = Util.adjustByCfg(status, ri);
        if (status != Status.OK) {
            Util.newerShellException(status, ri, iso);
		}

        // TBD - check to see if this test is done up front and
        //       this code can be removed?
        String mediaType = null;
        String outputTypeKey = null;
        try {
            outputTypeKey = ri.getPerRequestOutputTypeKey(requestedEpName);
            mediaType = ri.getPerRequestMediaType(requestedEpName);
        } catch (Exception ex) {
            Util.logAndThrowException(ri, Status.INTERNAL_SERVER_ERROR,
                  "Unknow mediaType for" + " mediaTypeKey: " + outputTypeKey
                    + ServiceShellException.getErrorString(ex));
        }

        Response.ResponseBuilder builder = Response.status(status)
              .type(mediaType)
              .entity(iso);

        try {
            builder.header(CONTENT_DISPOSITION,
                  ri.createContentDisposition(requestedEpName));
        } catch (Exception ex) {
            Util.logAndThrowException(ri, Status.INTERNAL_SERVER_ERROR,
                  "Error creating Content-Disposition header value"
                        + " endpoint: " + requestedEpName
                        + ServiceShellException.getErrorString(ex));
        }

        Util.addCORSHeadersIfConfigured(builder, ri, null);
		return builder.build();
    }

    /**
     * An updated version of doIrisStreaming which only needs two user
     * methods and returns more information in the user response for
     * better control of application output.
     *
     * @return
     * @throws Exception
     */
    public Response doIrisProcessing() throws Exception {
        // when run dynamically, this method does all the abstract methods,
        // so ri needs to be set here, e.e first
        RequestInfo ri = RequestInfo.createInstance(sw, uriInfo, request,
              requestHeaders);

        String requestedEpName = ri.getEndpointNameForThisRequest();

        if (!ri.isThisEndpointConfigured()) {
            Util.logAndThrowException(ri, Status.INTERNAL_SERVER_ERROR,
                  "Error, there is no configuration information for"
                        + " endpoint: " + requestedEpName);
        }

        if (containerRequestContext.getMethod().equals("POST")) {
            if (containerRequestContext != null) {
                ri.postBody = ((ContainerRequest) containerRequestContext)
                      .readEntity(String.class);
            } else {
                ri.postBody = "";
            }
        }

		ArrayList<String> cmd = null;

        // No object existance check done here as it should have been
        // done when the configuration parameters were loaded
        IrisProcessor isdo = null;
        if (sw.appConfig.getIrisEndpointClass(requestedEpName) instanceof
              edu.iris.wss.provider.IrisProcessor) {
            isdo = (IrisProcessor)sw.appConfig.getIrisEndpointClass(requestedEpName);
        } else {
            // this might happen if the service config has been set with
            // some other valid IRIS class, since the one parameter is now
            // used with more than one class type.
            Util.logAndThrowException(ri, Status.INTERNAL_SERVER_ERROR,
                  "An IrisProcessor object could not be found, "
                        + " class found: "
                        + sw.appConfig.getIrisEndpointClass(requestedEpName).getClass()
                        + "  MyApplication or the service.cfg did not setup"
                        + " the correct class for method doIrisProcessing");
        }

        cmd = new ArrayList<>();

		try {
			ParameterTranslator.parseQueryParams(cmd, ri, requestedEpName);
		} catch (Exception e) {
			Util.logAndThrowException(ri, Status.BAD_REQUEST,
                  "doIrisProcessing - " + e.getMessage());
		}

        // The value for media type, i.e. the value for parameters "format"
        // or "output" must have been specified in the configuration file
        // in the parameter outputs.
        // the string here should be of the form "type/subtype"
        String wssMediaType = null;
        String outputTypeKey = null;
        try {
            outputTypeKey = ri.getPerRequestOutputTypeKey(requestedEpName);
            wssMediaType = ri.getPerRequestMediaType(requestedEpName);
        } catch (Exception ex) {
            Util.logAndThrowException(ri, Status.INTERNAL_SERVER_ERROR,
                  "Unknown mediaType for" + " type parameter: " + outputTypeKey
                    + ServiceShellException.getErrorString(ex));
        }

        if (ri.request.getMethod().equals("HEAD")) {
            System.out.println("** doIrisProcessing, returning head request: "
                  + " cmd: " + cmd);
            // return to Jersey before any more processing
            Response.ResponseBuilder builder = Response.status(Status.OK)
                  .type("text/plain");

            Util.addCORSHeadersIfConfigured(builder, ri, null);
            return builder.build();
        }

		// Wait for an exit code, expecting the start of data transmission
        // or exception or timeout.
		IrisProcessingResult irr = isdo.getProcessingResults(ri, wssMediaType);
        if (irr.fdsnSS == null) {
            Util.logAndThrowException(ri, Status.INTERNAL_SERVER_ERROR,
                  "Null status from IrisStreamingOutput class");
        }

        Status status = Util.adjustByCfg(irr.fdsnSS, ri);
        if (status != Status.OK) {
            Util.newerShellException(status, ri, isdo);
		}

        Response.ResponseBuilder builder = Response.status(status)
              .type(irr.wssMediaType)
              .entity(irr.entity);

        try {
            // from previous way code worked
            String value = ri.createContentDisposition(requestedEpName);
            if (irr.headers != null) {
                if (irr.headers.containsKey(
                      CONTENT_DISPOSITION.toLowerCase())) {
                    // this is for new capability where handler can override
                    // response headers
                    value = irr.headers.get(CONTENT_DISPOSITION.toLowerCase());
                }
            }
            builder.header(CONTENT_DISPOSITION, value);
        } catch (Exception ex) {
            Util.logAndThrowException(ri, Status.INTERNAL_SERVER_ERROR,
                  "Error creating Content-Disposition header value"
                        + " endpoint: " + requestedEpName
                        + ServiceShellException.getErrorString(ex));
        }

        Util.addCORSHeadersIfConfigured(builder, ri, irr.headers);
        Util.addOtherHeadersIfAvailable(builder, irr.headers);

		return builder.build();
    }
}
