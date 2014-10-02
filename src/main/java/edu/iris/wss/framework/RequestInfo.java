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


import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import edu.iris.wss.framework.AppConfigurator.OutputType;
import edu.iris.wss.framework.FdsnStatus.Status;

public  class RequestInfo {

	public UriInfo uriInfo;
	public javax.servlet.http.HttpServletRequest request;
	public HttpHeaders requestHeaders;
	
	public static enum CallType { NORMAL, CATALOGS, CONTRIBUTORS, COUNTS };
	public CallType callType = CallType.NORMAL;
	
	public boolean perRequestUse404for204 = false;
	public AppConfigurator.OutputType perRequestOutputType = null;
	
	public String postBody = null;
	
	public AppConfigurator appConfig;
	public ParamConfigurator paramConfig;
	public StatsKeeper statsKeeper;
	
	public SingletonWrapper sw;
	
	// Used (set) by ProcessStreamingOutput class on ZIP output
	public String workingSubdirectory = null;
	
	public RequestInfo(SingletonWrapper sw,
			UriInfo uriInfo, 
			javax.servlet.http.HttpServletRequest request,
			HttpHeaders requestHeaders) {
		this.sw = sw;
		this.uriInfo = uriInfo;
		this.request = request;
		this.requestHeaders = requestHeaders;
		this.appConfig = sw.appConfig;
		this.paramConfig = sw.paramConfig;
		this.statsKeeper = sw.statsKeeper;
		
		if ((this.appConfig.getHandlerProgram() == null) && 
				(this.appConfig.getStreamingOutputClassName() == null)) {
			ServiceShellException.logAndThrowException(this,
                    Status.INTERNAL_SERVER_ERROR, 
					"Service configuration problem,"
                    + " handler program and StreamingOutputClassName not defined");

		}
	}	
	
	public void setPerRequestOutputType(String s) throws Exception {
		try {
			this.perRequestOutputType = OutputType.valueOf(s.toUpperCase());
		} catch (Exception e) {
			throw new Exception("Unrecognized output format: " + s);
		}
	}
}
