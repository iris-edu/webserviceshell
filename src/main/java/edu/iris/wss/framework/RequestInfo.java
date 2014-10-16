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
import java.text.SimpleDateFormat;
import java.util.Date;

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
    
    // as per StackOverflow, make sure the object is fully created before
    // passing it to another constructor, use createInstance factory to create.
    private RequestInfo() {
        // noop
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
		
        // TBD since this is configurartion, look at doing this once at startup.
		if ((ri.appConfig.getHandlerProgram() == null) && 
				(ri.appConfig.getStreamingOutputClassName() == null)) {
			ServiceShellException.logAndThrowException(ri,
                    Status.INTERNAL_SERVER_ERROR, 
					"Service configuration problem,"
                    + " handler program and StreamingOutputClassName not defined");
		}
        
        return ri;
    }
    
    // For testing only
    protected RequestInfo(AppConfigurator appConfig) {
        this.appConfig = appConfig;
    }
	
    /**
     * Validate and store the value from format parameter in query
     * 
     * @param trialKey
     * @throws Exception 
     */
	public void setPerRequestOutputType(String trialKey) throws Exception {
        if (trialKey == null) {
            throw new Exception("WebServiceShell, output type key is null, it"
                    + " must be key in outputTypes list in ...service.cfg");
        }
        // Validate of the value in query &format parameter
        String key = trialKey.trim().toUpperCase();
        
        if (appConfig.isConfiguredForTypeKey(key)) {
            this.perRequestOutputTypeKey = key;
        } else {
            throw new Exception("WebServiceShell, unrecognized outpTtype key: "
                    + key + "  from input: " + trialKey
                    + "  must be a type in outputTypes in ...service.cfg");
        }
	}
	
    /**
     * Note: Callers should expect the return value to be
     *       validated, trimmed, and uppercase
     * 
     * @return 
     */
	public String getPerRequestOutputTypeKey() {
        // Note: Callers should expect the return value to be
        //       validated, trimmed, and uppercase
        String key = perRequestOutputTypeKey;
        if (key == null) {
            key = appConfig.defaultOutputTypeKey();
		}
        return key;
	}
    
    public boolean isCurrentTypeKey(InternalTypes typeKey) {
        return getPerRequestOutputTypeKey().equals(typeKey.toString());
    }
    
    /**
     * Override configuration outputType with request outputType if the
     * request included &format.
     * 
     * @return 
     * @throws java.lang.Exception 
     */
    public String getPerRequestMediaType() throws Exception {
        return appConfig.getMediaType(getPerRequestOutputTypeKey());
    }

    /**
     * Create content disposition based on current request and configuration
     * information
     * 
     * @return 
     */
    public String createContentDisposition() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        StringBuilder sb = new StringBuilder();
        
        if (isCurrentTypeKey(InternalTypes.MSEED)
                || isCurrentTypeKey(InternalTypes.MINISEED)
                || isCurrentTypeKey(InternalTypes.BINARY)) {
            sb.append("attachment");
        } else {
            sb.append("inline");
        }
        
        sb.append("; filename=");
        sb.append(appConfig.getAppName());
        sb.append("_");
        sb.append(sdf.format(new Date()));
                
        if (! isCurrentTypeKey(InternalTypes.BINARY)) {
            // no suffix for binary
            sb.append(".").append(getPerRequestOutputTypeKey().toLowerCase());
        }
                
        return sb.toString();
    }
}
