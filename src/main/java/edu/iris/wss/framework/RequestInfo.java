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

package edu.iris.wss.framework;


import edu.iris.wss.framework.AppConfigurator.InternalTypes;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import edu.iris.wss.framework.FdsnStatus.Status;
import javax.servlet.http.HttpServletRequest;

public  class RequestInfo {

	public UriInfo uriInfo;
	public javax.servlet.http.HttpServletRequest request;
	public HttpHeaders requestHeaders;

	public boolean perRequestUse404for204 = false;

    // Note: The setter should be validating this string, trim it
    //       and set to uppercase. This should enable any gets of
    //       this value to not need to trim, validate, etc.
	public String perRequestFormatTypeKey = null;

	public String postBody = null;

	public AppConfigurator appConfig;
	public ParamConfigurator paramConfig;
	public StatsKeeper statsKeeper;

	public WssSingleton sw;

    private boolean isWriteToMiniseed = false;

    public byte[] HEADER_START_IDENTIFIER_BYTES;
    public byte[] HEADER_END_IDENTIFIER_BYTES;

    // as per StackOverflow, make sure the object is fully created before
    // passing it to another constructor, use createInstance factory to create.
    private RequestInfo() {
    }

    public static RequestInfo createInstance(WssSingleton sw,
			UriInfo uriInfo,
			javax.servlet.http.HttpServletRequest request,
			HttpHeaders requestHeaders) {

        RequestInfo ri = new RequestInfo();

        ri.sw = sw;
        ri.HEADER_START_IDENTIFIER_BYTES = sw.HEADER_START_IDENTIFIER_BYTES;
        ri.HEADER_END_IDENTIFIER_BYTES = sw.HEADER_END_IDENTIFIER_BYTES;
        ri.uriInfo = uriInfo;
        ri.request = request;
		ri.requestHeaders = requestHeaders;
		ri.appConfig = sw.appConfig;
		ri.paramConfig = sw.paramConfig;
		ri.statsKeeper = sw.statsKeeper;

        String trialEndpoint = getEndpointNameForThisRequest(ri.request);

        // need this to avoid checking for endpoint information when global
        // (i.e. non-endpoint) request are being handled
        if (isThisEndpointConfigured(request, sw.appConfig)) {
            try {
                if (ri.isCurrentTypeKey(trialEndpoint, InternalTypes.MSEED)
                      || ri.isCurrentTypeKey(trialEndpoint, InternalTypes.MINISEED)) {
                    if (ri.appConfig.isLogMiniseedExtents(trialEndpoint)) {
                        ri.isWriteToMiniseed = true;
                    } else {
                        ri.isWriteToMiniseed = false;
                    }
                }
            } catch (Exception ex) {
                String msg = "Service configuration problem, possibly missing"
                      + " type definition for MINISEED for endpointName: "
                      + trialEndpoint;
                System.out.println("^^^^^ msg: " + msg);
                System.out.println("^^^^^ msg ex: " + ex);
                Util.logAndThrowException(ri, Status.INTERNAL_SERVER_ERROR, msg,
                      "Exception: " + ex.getMessage());
            }
        }

        return ri;
    }

    public boolean isWriteToMiniseed() {
        return isWriteToMiniseed;
    }
    /**
     * This method returns zero length string when the request is at root
     * on the base URL or or base URL minus a trailing /
     *
     * @return
     */
    public String getEndpointNameForThisRequest() {
        return getEndpointNameForThisRequest(request);
    }

    public static String getEndpointNameForThisRequest(HttpServletRequest req) {
//        Dont use req.getSession(), as it creates a session for every request
//        String contextPath = req.getSession().getServletContext().getContextPath();

String contextPath = req.getContextPath();
        String requestURI = req.getRequestURI();

        // remove any leading slash character
        String epName =  requestURI.substring(contextPath.length());
        if (epName.length() > 0 ) {
            if (epName.startsWith("/")) {
                epName = epName.substring(1);
            }
        }

        return epName;
    }

    public boolean isThisEndpointConfigured() {
        return isThisEndpointConfigured(request, appConfig);
    }

    public static boolean isThisEndpointConfigured(HttpServletRequest req,
          AppConfigurator appCfg) {
        String trialEpName = getEndpointNameForThisRequest(req);

        return trialEpName.length() > 0
              && appCfg.isThisEndpointConfigured(trialEpName);
    }

    // For testing only
    protected RequestInfo(AppConfigurator appConfig) {
        this.appConfig = appConfig;
    }

    /**
     * Validate and store the value from format parameter in query
     *
     * @param epName
     * @param trialKey
     * @throws Exception
     */
	public void setPerRequestFormatType(String epName, String trialKey) throws Exception {
        if (trialKey == null) {
            throw new Exception("format type requested is null");
        }
        // Validate of the value in query &format parameter
        String key = trialKey.trim().toUpperCase();

        if (appConfig.isConfiguredForTypeKey(epName, key)) {
            this.perRequestFormatTypeKey = key;
        } else {
            throw new Exception("Unrecognized format type requested: " + trialKey);
        }

        isWriteToMiniseed =
              (perRequestFormatTypeKey.equals(InternalTypes.MSEED.toString())
              || perRequestFormatTypeKey.equals(InternalTypes.MINISEED.toString()))
              && appConfig.isLogMiniseedExtents(epName);
	}

    /**
     * Note: Callers should expect the return value to be
     *       validated, trimmed, and uppercase
     *
     * @return
     */
	public String getPerRequestFormatTypeKey(String epName) throws Exception {
        // Note: Callers should expect the return value to be
        //       validated, trimmed, and uppercase
        String key = perRequestFormatTypeKey;
        if (key == null) {
            key = appConfig.getDefaultFormatTypeKey(epName);
		}
        return key;
	}

    private boolean isCurrentTypeKey(String epName, InternalTypes typeKey)
          throws Exception {
        return getPerRequestFormatTypeKey(epName).equals(typeKey.toString());
    }

    /**
     * Override configuration formatType with request formatType if the
     * request included &format.
     *
     * @return
     * @throws java.lang.Exception
     */
    public String getPerRequestMediaType(String epName) throws Exception {
        return appConfig.getMediaType(epName, getPerRequestFormatTypeKey(epName));
    }

    /**
     * Create content disposition based on current request and configuration
     * information
     *
     * @return
     */
    public String createDefaultContentDisposition(String epName) throws Exception {
        StringBuilder sb = new StringBuilder();

        if (isCurrentTypeKey(epName, InternalTypes.MSEED)
                || isCurrentTypeKey(epName, InternalTypes.MINISEED)
                || isCurrentTypeKey(epName, InternalTypes.BINARY)) {
            sb.append("attachment");
        } else {
            sb.append("inline");
        }

        sb.append("; filename=");
        sb.append(appConfig.getAppName());
        sb.append("_");
        sb.append(Util.getCurrentUTCTimeISO8601());

        if (! isCurrentTypeKey(epName, InternalTypes.BINARY)) {
            // put suffix when not binary
            sb.append(".").append(getPerRequestFormatTypeKey(epName).toLowerCase());
        }

        return sb.toString();
    }
}
