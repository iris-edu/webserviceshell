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


import edu.iris.wss.framework.AppConfigurator.InternalTypes;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import edu.iris.wss.framework.FdsnStatus.Status;
import edu.iris.wss.utils.WebUtils;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;

public  class RequestInfo {

	public UriInfo uriInfo;
	public javax.servlet.http.HttpServletRequest request;
	public HttpHeaders requestHeaders;
	
	public static enum CallType { NORMAL, CATALOGS, CONTRIBUTORS, COUNTS };
	public CallType callType = CallType.NORMAL;
	
	public boolean perRequestUse404for204 = false;
    
    // Note: The setter should be validating this string, trim it
    //       and set to uppercase. This should enable any gets of
    //       this value to not need to trim, validate, etc.
	public String perRequestOutputTypeKey = null;
	
	public String postBody = null;
	
	public AppConfigurator appConfig;
	public ParamConfigurator paramConfig;
	public StatsKeeper statsKeeper;
	
	public SingletonWrapper sw;
	
	// Used (set) by ProcessStreamingOutput class on ZIP output
	public String workingSubdirectory = null;
    
    public boolean isWriteToMiniseed = false;
    
    // as per StackOverflow, make sure the object is fully created before
    // passing it to another constructor, use createInstance factory to create.
    private RequestInfo() {
    }
    
    public static RequestInfo createInstance(SingletonWrapper sw,
			UriInfo uriInfo, 
			javax.servlet.http.HttpServletRequest request,
			HttpHeaders requestHeaders) {
        
        RequestInfo ri = new RequestInfo();
        
        ri.sw = sw;
        ri.uriInfo = uriInfo;
        ri.request = request;
		ri.requestHeaders = requestHeaders;
		ri.appConfig = sw.appConfig;
		ri.paramConfig = sw.paramConfig;
		ri.statsKeeper = sw.statsKeeper;
        
        String contextPath = request.getSession().getServletContext().getContextPath();
        System.out.println("^^^^^^^^^^^^^ path: " +  contextPath);
        String requestURI = request.getRequestURI();
        System.out.println("^^^^^^^^^^^^^ getRequestURI: " +  requestURI);
        System.out.println("^^^^^^^^^^^^^ getRequestURL: " +  request.getRequestURL());
        System.out.println("^^^^^^^^^^^^^ overlap: " +  requestURI.substring(requestURI.indexOf(contextPath), contextPath.length()));
        String trialEndpoint = getEndpointNameForThisRequest(ri.request);
        System.out.println("^^^^^^^^^^^^^ trialEndpoint: " +  trialEndpoint);
        
        // need this to avoid checking for endpoint information when global
        // (i.e. non-endpoint) request are being handled
        if (isConfiguredForThisEndpoint(request, sw.appConfig)) {
            try {
                if (ri.isCurrentTypeKey(trialEndpoint, InternalTypes.MSEED)
                      | ri.isCurrentTypeKey(trialEndpoint, InternalTypes.MINISEED)) {
                    ri.isWriteToMiniseed = true;
                }
            } catch (Exception ex) {
                String msg = "Service configuration problem,"
                      + " endpointName or InternalTypes problem, endpointName: "
                      + trialEndpoint;
                System.out.println("^^^^^ msg: " + msg);
                System.out.println("^^^^^ msg ex: " + ex);
                ServiceShellException.logAndThrowException(ri, Status.INTERNAL_SERVER_ERROR,
                      msg,  ex);
            }


            // TBD since this is configurartion, look at doing this once at startup.
            if ((ri.appConfig.getHandlerProgram(trialEndpoint) == null) && 
                    (ri.appConfig.getIrisEndpointClass(trialEndpoint) == null)) {
                String msg = "Service configuration problem,"
                        + " handler program and StreamingOutputClassName not defined";
                System.out.println("^^^^^ msg: " + msg);
                ServiceShellException.logAndThrowException(ri,
                        Status.INTERNAL_SERVER_ERROR, msg);
            }
        }
        
        return ri;
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
        String contextPath = req.getSession().getServletContext().getContextPath();
        String requestURI = req.getRequestURI();
    
        // remove any leading /
        String epName = requestURI.substring(contextPath.length())
              .replaceFirst(java.util.regex.Pattern.quote("/"), "");
        
        return epName;
    }

    public boolean isConfiguredForThisEndpoint() {
        return isConfiguredForThisEndpoint(request, appConfig);
    }

    public static boolean isConfiguredForThisEndpoint(HttpServletRequest req,
          AppConfigurator appCfg) {
        String trialEpName = getEndpointNameForThisRequest(req);
        
        return trialEpName.length() > 0
              && appCfg.isConfiguredForEndpoint(trialEpName);
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
	public void setPerRequestOutputType(String epName, String trialKey) throws Exception {
        if (trialKey == null) {
            throw new Exception("format type requested is null");
        }
        // Validate of the value in query &format parameter
        String key = trialKey.trim().toUpperCase();
        
        if (appConfig.isConfiguredForTypeKey(epName, key)) {
            this.perRequestOutputTypeKey = key;
        } else {
            throw new Exception("Unrecognized format type requested: " + trialKey);
        }
	}
	
    /**
     * Note: Callers should expect the return value to be
     *       validated, trimmed, and uppercase
     * 
     * @return 
     */
	public String getPerRequestOutputTypeKey(String epName) throws Exception {
        // Note: Callers should expect the return value to be
        //       validated, trimmed, and uppercase
        String key = perRequestOutputTypeKey;
        if (key == null) {
            key = appConfig.getDefaultOutputTypeKey(epName);
		}
        return key;
	}
    
    private boolean isCurrentTypeKey(String epName, InternalTypes typeKey)
          throws Exception {
        return getPerRequestOutputTypeKey(epName).equals(typeKey.toString());
    }
    
    /**
     * Override configuration outputType with request outputType if the
     * request included &format.
     * 
     * @return 
     * @throws java.lang.Exception 
     */
    public String getPerRequestMediaType(String epName) throws Exception {
        return appConfig.getMediaType(epName, getPerRequestOutputTypeKey(epName));
    }

    /**
     * Create content disposition based on current request and configuration
     * information
     * 
     * @return 
     */
    public String createContentDisposition(String epName) throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
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
        sb.append(sdf.format(new Date()));
                
        if (! isCurrentTypeKey(epName, InternalTypes.BINARY)) {
            // no suffix for binary
            sb.append(".").append(getPerRequestOutputTypeKey(epName).toLowerCase());
        }
                
        return sb.toString();
    }
}
