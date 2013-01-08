package edu.iris.wss.framework;


import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import edu.iris.wss.framework.FdsnStatus.Status;

public  class RequestInfo {

	public UriInfo uriInfo;
	public javax.servlet.http.HttpServletRequest request;
	public HttpHeaders requestHeaders;
	
	public String postBody = null;
	
	public AppConfigurator appConfig;
	public ParamConfigurator paramConfig;
	public StatsKeeper statsKeeper;
	
	public SingletonWrapper sw;
	
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
			ServiceShellException.logAndThrowException(this, Status.INTERNAL_SERVER_ERROR, 
					"Service Configuration Problem");

		}
	}	
}