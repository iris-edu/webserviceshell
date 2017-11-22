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

import edu.iris.wss.framework.AppConfigurator;
import edu.iris.wss.framework.FdsnStatus.Status;
import edu.iris.wss.framework.ParameterTranslator;
import edu.iris.wss.framework.RequestInfo;
import edu.iris.wss.framework.ServiceShellException;
import edu.iris.wss.framework.WssSingleton;
import edu.iris.wss.framework.Util;
import edu.iris.wss.utils.WebUtils;
import java.util.ArrayList;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.apache.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.message.internal.MediaTypes;
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

////    /**
////     * A test function, which echos back the contents of a post request.
////     *
////     * @return
////     * @throws IOException
////     */
////    public Response echoPostString() throws IOException {
////        // when run dynamically, this method does all the abstract methods,
////        // so ri needs to be set here and past in.
////        RequestInfo ri = RequestInfo.createInstance(sw, uriInfo, request,
////              requestHeaders);
////
////        String requestedEpName = ri.getEndpointNameForThisRequest();
////
////        if (!ri.isThisEndpointConfigured()) {
//////            Util.shellException(Status.INTERNAL_SERVER_ERROR,
//////                  "Error, there is no configuration information for"
//////                        + " endpoint: " + requestedEpName, ri);
////            System.out.println("* echoPostString warning, no configuration for endpoint: "
////                  + requestedEpName);
////        }
////
////        System.out.println("* echoPostString method: " + containerRequestContext.getMethod());
////        System.out.println("* echoPostString toString: " + containerRequestContext.toString());
////        System.out.println("* echoPostString getLength: " + containerRequestContext.getLength());
////
//////        String postContent = ((ContainerRequest) containerRequestContext).readEntity(String.class);
//////        System.out.println("* echoPostString readEntity: " + postContent);
////
////        // cannot do both readEntity and read InputStream
////		StringBuilder sb = new StringBuilder(2048);
////
////        InputStream is = containerRequestContext.getEntityStream();
////		byte [] buffer = new byte[1024];
////
////		try {
////			int nRead;
////			while ((nRead = is.read(buffer, 0, buffer.length)) != -1) {
////				sb.append(new String(buffer, 0, nRead));
////			}
////		} catch(IOException ex) {
////			System.out.println("Got IO exception in doIrisStreamin2, ex: " + ex);
////		} finally {s
////			try{ is.close(); } catch( Exception ex) {;}
////		}
////
////        System.out.println("* echoPostString entityStream: " + sb.toString());
////
////        Response.ResponseBuilder builder = Response.status(Status.OK)
////                  .type("text/plain")
////                  .entity(sb.toString());
////
////
////        Map<String, String> headersMap = new HashMap<>();
////        Util.updateWithCORSHeadersIfConfigured(ri, headersMap);
////        Util.setResponseHeaders(builder, headersMap);
////
////        return builder.build();
////    }

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
            Util.logAndThrowException(ri, Status.NOT_FOUND,
                  "Error, this endpoint is not configured, or it is mistyped, or"
                        + " there is a trailing slash in path: "
                        + ri.request.getRequestURI());
        }

        ri.requestMediaType = containerRequestContext.getMediaType();
        if (containerRequestContext.getMethod().equals("POST")) {
            if (containerRequestContext != null) {
                if (MediaTypes.typeEqual(MediaType.MULTIPART_FORM_DATA_TYPE,
                        ri.requestMediaType)) {
                    ri.postMultipart = ((ContainerRequest) containerRequestContext)
                          .readEntity(FormDataMultiPart.class);
                } else {
                    ri.postBody = ((ContainerRequest) containerRequestContext)
                          .readEntity(String.class);
                }
            }

            String username = WebUtils.getAuthenticatedUsername(ri.requestHeaders);
		    if (AppConfigurator.isOkString(username)) {
                ri.statsKeeper.logAuthPost();
            } else {
                ri.statsKeeper.logPost();
            }
        } else {
            // NOTE: HEAD is counted here as well as GET (and PUT and DELETE, etc)
            String username = WebUtils.getAuthenticatedUsername(ri.requestHeaders);
		    if (AppConfigurator.isOkString(username)) {
                ri.statsKeeper.logAuthGet();
            } else {
                ri.statsKeeper.logGet();
            }
        }

        IrisProcessor isdo = null;
        if (sw.appConfig.getIrisEndpointClass(requestedEpName) instanceof
              edu.iris.wss.provider.IrisProcessor) {
            // Instantiate a new object everytime to avoid latent memory or
            // threading problems if endpoint class is not specifically coded for
            // repeated calls
            // AppConfigurator stores the first instantiates and verifie an
            // object as defined by the config file. Get the classname of
            // that object and re-instantiate - DON'T reuse it!
            isdo = (IrisProcessor)sw.appConfig.getIrisEndpointClass(requestedEpName);
            isdo = (IrisProcessor)AppConfigurator.getClassInstance(
                  isdo.getClass().getName(), IrisProcessMarker.class);
        } else {
            // this might happen if the service config has been set with
            // some other valid IRIS class, since the one parameter is now
            // used with more than one class type.

            String briefMsg = "An IrisProcessor object was not found, "
                  + " this class was found: "
                  + sw.appConfig.getIrisEndpointClass(requestedEpName).getClass();

            String moreDetails = "Check the setup in service config or dynamic"
                  + " class assignment in MyApplication";

            Util.logAndThrowException(ri, Status.INTERNAL_SERVER_ERROR,
                  briefMsg, moreDetails);
        }

        // check for parameter errors before trying to do any endpoint
        // processing, endpoints may/will do this again, but tranlator
        // errors should be determined here first.
        ArrayList<String> cmd = new ArrayList<>();
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
        //
        // "wss"MediaType denotes the media type as determined by Web Service
        // Shell, that is the valuse as determined from the initial configuration
        // and any override from a respective query parameter. wssMediaType is
        // provided to the application in the isdo.getProcessingResults call
        // and my be overwritten as needed.
        // The ipr returned from isdo.getProcessingResults was a field named
        // wssMediaType -- this is poorly named and should be changed some day.
        // The media type within the ipr structure might be the same as the
        // input wssMediaType, but is could be different, and if so, it should
        // replacde the local copy of wssMediaType assigned here.
        String wssMediaType = null;
        String formatTypeKey = null;
        try {
            // Note: formatType is a name assigned by a user in a service.cfg
            //       file. It is used as a key to lookup a mediaType, so rather
            //       than two calls, this should probably be one call sinse they
            //       should normally come in pairs when used for output control.
            formatTypeKey = ri.getPerRequestFormatTypeKey(requestedEpName);
            wssMediaType = ri.getPerRequestMediaType(requestedEpName);
        } catch (Exception ex) {
            Util.logAndThrowException(ri, Status.INTERNAL_SERVER_ERROR,
                  "Unknown mediaType from mediaTypeKey, endpoint: "
                        + requestedEpName,
                  "Error, mediaTypeKey: " + formatTypeKey +
                        ServiceShellException.getErrorString(ex));
        }

        if (ri.request.getMethod().equals("HEAD")) {

            // return to Jersey before any more processing
            Response.ResponseBuilder builder = Response.status(Status.OK)
                  .type("text/plain");

            Map<String, String> headersMap = Util.createDefaultContentDisposition(
              ri, requestedEpName);
            Util.updateWithCORSHeadersIfConfigured(ri, headersMap);
            Util.updateWithEndpointHeaders(ri, headersMap, requestedEpName);
            Util.updateDispositionPerFormatType(ri, headersMap, requestedEpName,
                  formatTypeKey);
            Util.setResponseHeaders(builder, headersMap);

            return builder.build();
        }

		// Wait for an exit code, expecting the start of data transmission
        // or exception or timeout.
        // provide incoming media type to isdo in case the it is needed for
        // processing, the isdo can return the same value or possible a new
        // value.
		IrisProcessingResult ipr = isdo.getProcessingResults(ri, wssMediaType);

        // check for programming error
        boolean isStatusNull = ipr.fdsnSS == null;
        boolean isMediaTypeNull = ipr.wssMediaType == null;
        if (isStatusNull || isMediaTypeNull) {
            String phrase = "";
            if (isStatusNull && isMediaTypeNull) {
                phrase = "FDSN Status and mediaType are";
            } else if (isStatusNull && ! isMediaTypeNull) {
                phrase = "FDSN Status is";
            } else if (! isStatusNull && isMediaTypeNull) {
                phrase = "mediaType is";
            }

            String briefMsg = isdo.getClass().getName()
                  + " class programming error, " + phrase + " null";
            String detailedMsg = "Reported brief message: " + ipr.briefErrMessage
                  + "  detailed message: " + ipr.detailedErrMessage;

            Util.logAndThrowException(ri, Status.INTERNAL_SERVER_ERROR,
                  briefMsg, detailedMsg);
        }

        Status status = Util.adjustByCfg(ipr.fdsnSS, ri);
        if (status != Status.OK) {
            Util.logAndThrowException(ri, status, ipr.briefErrMessage,
                  ipr.detailedErrMessage);
		}

        // TBD - look for an occurrances of irr.wssMediaType in formatTypes
        // values, if not there, meaning this type is not configured, give
        // a warning.
//        logger.warn("An inconsistency has occured between endpoint code and "
//            + "...");

        Response.ResponseBuilder builder = Response.status(status)
              .type(ipr.wssMediaType)
              .entity(ipr.entity);

        // establish headers for this request, update map in order of
        // of precedence, starting with defaults.
        //
        // default CONTENT_DISPOSITION
        Map<String, String> headersMap = Util.createDefaultContentDisposition(
              ri, requestedEpName);
        Util.updateWithCORSHeadersIfConfigured(ri, headersMap);

        // update headers in order
        Util.updateWithEndpointHeaders(ri, headersMap, requestedEpName);
        Util.updateDispositionPerFormatType(ri, headersMap, requestedEpName,
              formatTypeKey);
        // highest priority, use any headers from user processes
        Util.updateWithApplicationHeaders(headersMap, ipr.headers);


        Util.setResponseHeaders(builder, headersMap);

////        // manual test code
////        Response response = builder.build();
////        MultivaluedMap<String, Object> mm = response.getHeaders();
////        Set<String> mmKeys = mm.keySet();
////        for (String mmKey : mmKeys) {
////            System.out.println("******************** www output headers mmKey: " + mmKey
////                  + "         value: " + mm.get(mmKey));
////        }
////        System.out.println("------ return response");
////        return response;

		return builder.build();
    }
}
