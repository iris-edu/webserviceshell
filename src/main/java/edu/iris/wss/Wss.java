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

package edu.iris.wss;

import java.io.*;
import java.net.URL;
import java.util.*;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;

import edu.iris.wss.framework.AppConfigurator.OutputType;
import edu.iris.wss.framework.FdsnStatus.Status;
import edu.iris.wss.framework.RequestInfo.CallType;

import org.apache.log4j.Logger;

import edu.iris.wss.IrisStreamingOutput.IrisStreamingOutput;
import edu.iris.wss.IrisStreamingOutput.ProcessStreamingOutput;
import edu.iris.wss.framework.*;
import edu.iris.wss.utils.WebUtils;

@Path ("/")
public class Wss {
	 
	@Context 	ServletContext context;
	@Context	javax.servlet.http.HttpServletRequest request;
    @Context 	UriInfo uriInfo;	
    @Context 	HttpHeaders requestHeaders;

    @Context 	SingletonWrapper sw;
    
    private RequestInfo ri;
    
	public static final Logger logger = Logger.getLogger(Wss.class);
	
	public Wss()  {
    	TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}	
	
	@Path("status")
	@GET
	public String cfg() {
    	ri = new RequestInfo(sw, uriInfo, request, requestHeaders);

    	StringBuilder sb = new StringBuilder();
		sb.append("<HTML><BODY>");
		sb.append("<TABLE BORDER=2 cellpadding='2' style='width: 600px'>");
		sb.append("<col style='width: 30%' />");
		sb.append("<TR><TH COLSPAN=\"2\">Servlet Environment</TH></TR>");
		sb.append("<TR><TD>" + "URL" + "</TD><TD>" + request.getRequestURI() + "</TD></TR>");
		sb.append("<TR><TD>" + "Host" + "</TD><TD>" + WebUtils.getHost(request) + "</TD></TR>");
		sb.append("<TR><TD>" + "Port" + "</TD><TD>" + WebUtils.getPort(request) + "</TD></TR>");
		sb.append("</TABLE>");

		sb.append("<br/>");
    	sb.append(ri.sw.statsKeeper.toHtmlString());

    	sb.append("<br/>");
    	sb.append(ri.appConfig.toHtmlString());
    	
    	sb.append("<br/>");
    	sb.append(ri.paramConfig.toHtmlString());

    	sb.append("</BODY></HTML>");

		return sb.toString();
	}
	
	// [region] Root path documentation handler, version handler and WADL


	private String defDoc() {
						
		return "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">" + 
				"<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=US-ASCII\">" +
				"<title>Welcome to the Web Service Shell</title></head>" +
				"<body><h2>Welcome to the Web Service Shell</h2>" + 
				"<p> Configure what to return here instead of this text by using the <b>rootServiceDoc</b> option." + 
				"</body></html>";
	}
	
	private static boolean isOkString(String s) {
		return ((s != null) && !s.isEmpty());
	}
	
    @GET
    public Response ok() {
    	ri = new RequestInfo(sw, uriInfo, request, requestHeaders);

    	String docUrl = ri.appConfig.getRootServiceDoc();
    	
    	// Dump some default text if no good Service Doc Config
    	if (!isOkString(docUrl))  
    		return  Response.status(Status.OK).entity(defDoc()).type("text/html").build(); 	
    	
      	InputStream is = null;
      	URL url = null;
    	try {    		
    		url = new URL(docUrl);    		
    		is = url.openStream();
    	} catch (Exception e) {
    		String err = "Can't find root documentation page: " + docUrl;
        	return  Response.status(Status.OK).entity(err).type("text/plain").build();
    	}
    	
    	final BufferedReader br = new BufferedReader( new InputStreamReader( is));

    	StreamingOutput so = new StreamingOutput() {
    		@Override
			public void write(OutputStream outputStream) throws IOException, WebApplicationException {
    			BufferedWriter writer = new BufferedWriter (new OutputStreamWriter(outputStream));
    			String inputLine = null ;
    			while ((inputLine = br.readLine()) != null) {
    				inputLine = inputLine.replace("VERSION",ri.appConfig.getVersion()); 
    				inputLine = inputLine.replace("HOST", WebUtils.getHostname()); 
    				inputLine = inputLine.replace("BASEURL", ri.appConfig.getRootServicePath());
    				writer.write(inputLine);
    				writer.newLine();
    			}
    			writer.flush();
    			br.close();
    			writer.close();
			}
    	};

    	ResponseBuilder builder = Response.status(Status.OK).entity(so).type("text/html");
		return builder.build();
    }
	
    @Path("wssversion")
	@GET @Produces("text/plain")
	public String getWssVersion() throws IOException {
    	return AppConfigurator.wssVersion;
	}
	
	@Path("version")
	@GET @Produces("text/plain")
	public String getVersion() {
    	ri = new RequestInfo(sw, uriInfo, request, requestHeaders);
    	return ri.appConfig.getVersion();
	}
	
	@Path("whoami")
	@GET @Produces("text/plain")
	public String getwho() {
    	ri = new RequestInfo(sw, uriInfo, request, requestHeaders);
    	return request.getRemoteAddr();
	}	
	
	@Path("application.wadl")
	@GET @Produces ("application/xml")
	public Response getWadl() {
		
    	ri = new RequestInfo(sw, uriInfo, request, requestHeaders);
    	
    	// First check if wadlPath configuration is set.  If so, then stream from that URL.
    	
    	String wadlPath = ri.appConfig.getWadlPath();
    	if ((wadlPath != null) && (!wadlPath.isEmpty())) {
        	InputStream is = null;
          	URL url = null;
        	try {    		
        		url = new URL(wadlPath);    		
        		is = url.openStream();
        	} catch (Exception e) {
        		String err = "Can't find root documentation page: " + wadlPath;
            	return  Response.status(Status.OK).entity(err).type("text/plain").build();
        	}
        	
        	final BufferedReader br = new BufferedReader( new InputStreamReader( is));

        	StreamingOutput so = new StreamingOutput() {
        		@Override
    			public void write(OutputStream outputStream) throws IOException, WebApplicationException {
        			BufferedWriter writer = new BufferedWriter (new OutputStreamWriter(outputStream));
        			String inputLine = null ;
        			while ((inputLine = br.readLine()) != null) {
        				writer.write(inputLine);
        				writer.newLine();
        			}
        			writer.flush();
        			br.close();
        			writer.close();
    			}
        	};

        	ResponseBuilder builder = Response.status(Status.OK).entity(so).type("application/xml");
    		return builder.build();
    	}

		// Try to read a user WADL file from the location specified by the
		// wssConfigDir property concatenated with the web application name (last part
		// of context path), e.g. 'station' or 'webserviceshell'
		String wadlFileName = null;
		String configBase = WebUtils.getConfigFileBase(context);
		FileInputStream wadlStream = null;
		try {
			String wssConfigDir = System.getProperty(AppConfigurator.wssConfigDirSignature);
			if (isOkString(wssConfigDir) && isOkString(configBase)) {
				if (!wssConfigDir.endsWith("/")) 
					wssConfigDir += "/";
				
				wadlFileName = wssConfigDir + configBase + "-application.wadl";		
			
				File wadl = new File(wadlFileName);
				if (wadl.exists()) {
					logger.info("Attempting to load wadl file from: " + wadlFileName);

					wadlStream = new FileInputStream(wadlFileName);
					return Response.status(Status.OK).entity(wadlStream).build();
				}
			}
		} catch (Exception e) {
			logger.error("Failure loading wadl file from: " + wadlFileName);
		}

		return Response.status(Status.NOT_FOUND).build();

	}
	
	// [end region]

	// [region] Main query entry points GET, POST
	
	@POST
	@Path("queryauth") 
	public Response postQueryAuth(String pb) {
    	ri = new RequestInfo(sw, uriInfo, request, requestHeaders);
    	ri.postBody = pb;
    	
		if (! ri.appConfig.getPostEnabled()) 
			shellException(Status.BAD_REQUEST, "POST Method not allowed");
		
		ri.statsKeeper.logAuthPost();
		return processQuery();
	}
	 
	@POST
	@Path("query") 
	public Response postQuery(String pb) {
    	ri = new RequestInfo(sw, uriInfo, request, requestHeaders);
    	ri.postBody = pb;
    	
		if (! ri.appConfig.getPostEnabled()) 
			shellException(Status.BAD_REQUEST, "POST Method not allowed");
		
		ri.statsKeeper.logPost();
		return processQuery();
	}

	@GET
	@Path("queryauth")
	public Response queryAuth() throws Exception {
    	ri = new RequestInfo(sw, uriInfo, request, requestHeaders);

    	ri.statsKeeper.logAuthGet();
		return processQuery();
	}
	
	@GET 
	@Path("query")
	public Response query() throws Exception {
    	ri = new RequestInfo(sw, uriInfo, request, requestHeaders);
    
    	ri.statsKeeper.logGet();
		return processQuery();
	}
	
	@GET 
	@Path("catalogs")
	public Response catalogs() throws Exception {
    	ri = new RequestInfo(sw, uriInfo, request, requestHeaders);
    	ri.callType = CallType.CATALOGS;

    	ri.statsKeeper.logGet();
		return processQuery();
	}
	
	@GET 
	@Path("contributors")
	public Response contributors() throws Exception {
    	ri = new RequestInfo(sw, uriInfo, request, requestHeaders);
    	ri.callType = CallType.CONTRIBUTORS;

    	ri.statsKeeper.logGet();
		return processQuery();
	}	
	
	@GET 
	@Path("counts")
	public Response counts() throws Exception {
    	ri = new RequestInfo(sw, uriInfo, request, requestHeaders);
    	ri.callType = CallType.COUNTS;

    	ri.statsKeeper.logGet();
		return processQuery();
	}	

	private Response processQuery() {
		if (!ri.appConfig.isValid())
			shellException(Status.INTERNAL_SERVER_ERROR, "Application Configuration Issue");

		if (ri.appConfig.getStreamingOutputClassName() != null) {
			return runJava();
		} else {
			return runCommand();
		}
	}
	
	// [end region]

	private Response runJava() {
		
		// Run the parameter translator to test consistency.  We need an arraylist, but it's not used.
		ArrayList<String> cmd = new ArrayList<String>();
		try {
			ParameterTranslator.parseQueryParams(cmd, ri);
		} catch (Exception e) {
			shellException(Status.BAD_REQUEST, e.getMessage());
		}
				
		String className = ri.appConfig.getStreamingOutputClassName();
		IrisStreamingOutput iso = null;
		
		try {
    		Class<?> soClass;
    		soClass = Class.forName(className);
			iso = (IrisStreamingOutput) soClass.newInstance();
		} catch (ClassNotFoundException e) {
			String err = "Could not find class with name: " + className;
			logger.fatal(err);
			throw new RuntimeException(err);
		} catch (InstantiationException e) {
			String err = "Could not instantiate class: " + className;
			logger.fatal(err);
			throw new RuntimeException(err);
		} catch (IllegalAccessException e) {
			String err = "Illegal access while instantiating class: " + className;
			logger.fatal(err);
			throw new RuntimeException(err);
		}
		
		iso.setRequestInfo(ri);
		
		// Wait for an exit code, the start of data transmission or a timeout.
		Status status = iso.getResponse();
		if (status == null) {
			shellException(Status.INTERNAL_SERVER_ERROR, "Null status from StreamingOutput class");
		} else if (status == Status.NO_CONTENT) {
			if (ri.perRequestUse404for204) {
				status = Status.NOT_FOUND;
				shellException(status, status.toString() + ": " +  iso.getErrorString());
			} else {
				shellException(status, null);
			}
		} else if (status != Status.OK) {
			shellException(status, status.toString() + ": " +  iso.getErrorString());
		}
		
		OutputType outputType = ri.appConfig.getOutputType();
		
		if (ri.perRequestOutputType != null) {
			outputType = ri.perRequestOutputType;
		}
		
		ResponseBuilder builder = Response.status(status)
				.type(AppConfigurator.getMimeType(outputType)).entity(iso);    
		builder.header("Content-Disposition", AppConfigurator.getContentDispositionType(outputType) + 
				"; filename=" + ri.appConfig.getOutputFilename(outputType));			
		
		// Insert CORS header elements. 
		if (ri.appConfig.getAllowCors()) {
		    builder.header("Access-Control-Allow-Origin", "*");
		    builder.header("Access-Control-Allow-Credentials", "true");
		    builder.header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT");
		    builder.header("Access-Control-Allow-Headers", "Content-Type, Accept");
		}
	    
		return builder.build();
	}
		
	private Response runCommand()  {

    	// Create the 'command' array by first adding on the program to 
    	// be invoked.  Then parse the query parameters.  These are appended
    	// to the cmd argument.
    	
    	// The handler program string from the config file may contain multiple space-delimited
    	// text. These need to be split and added to the cmd collection
		ArrayList<String> cmd = null;
		switch (ri.callType) {
			case NORMAL:
				// Handler string can't be NULL per configuration requirements in AppConfigurator
				cmd = new ArrayList<String>(Arrays.asList(ri.appConfig.getHandlerProgram().split(" ")));			
				try {
					ParameterTranslator.parseQueryParams(cmd, ri);
				} catch (Exception e) {
					shellException(Status.BAD_REQUEST, e.getMessage());
				}
				break;
			case CATALOGS:
				String catalogsHandlerString = ri.appConfig.getCatalogsHandlerProgram();
				if (!isOkString(catalogsHandlerString))
					shellException(Status.NOT_FOUND, null);

				cmd = new ArrayList<String>(Arrays.asList(catalogsHandlerString.split(" ")));
				try {
					ri.appConfig.setOutputType("XML");
				} catch (Exception e) { ; }
				break;
				
			case CONTRIBUTORS:
				String contributorsHandlerString = ri.appConfig.getContributorsHandlerProgram();

				if (!isOkString(contributorsHandlerString))
					shellException(Status.NOT_FOUND, null);
				
				cmd = new ArrayList<String>(Arrays.asList(contributorsHandlerString.split(" ")));
				try {
					ri.appConfig.setOutputType("XML");
				} catch (Exception e) { ; }
				break;
				
			case COUNTS:
				String countsHandlerString = ri.appConfig.getCountsHandlerProgram();

				if (!isOkString(countsHandlerString))
					shellException(Status.NOT_FOUND, null);
				
				cmd = new ArrayList<String>(Arrays.asList(countsHandlerString.split(" ")));
				try {
					ri.appConfig.setOutputType("XML");
				} catch (Exception e) { ; }
				break;
		}

		// Now run any Argument Preprocessor class if set.
		if (ri.appConfig.getArgPreProcessorClassName() != null) {
			try {
				preProcess(ri, cmd);
			} catch (Exception e) {
				shellException(Status.BAD_REQUEST, e.getMessage());
			}
		}
		//	logger.info("CMD array: " + cmd);	

	    ProcessBuilder pb = new ProcessBuilder(cmd);
	    pb.directory(new File(ri.appConfig.getWorkingDirectory()));    
	   
	    pb.environment().put("REQUESTURL", WebUtils.getUrl(request));
	    pb.environment().put("USERAGENT", WebUtils.getUserAgent(request));
	    pb.environment().put("IPADDRESS", WebUtils.getClientIp(request));
	    pb.environment().put("APPNAME", ri.appConfig.getAppName());
	    pb.environment().put("VERSION", ri.appConfig.getVersion());
	   
		ProcessStreamingOutput iso = new ProcessStreamingOutput(pb, ri);      
		
		// Wait for an exit code, the start of data transmission or a timeout.
		Status status = iso.getResponse();
		if (status == Status.NO_CONTENT) {
			if (ri.perRequestUse404for204) {
				status = Status.NOT_FOUND;
				shellException(status, status.toString() + ": " +  iso.getErrorString());
			} else {
				shellException(status, null);
			}
		} else if (status != Status.OK) {
			shellException(status, "handlerProgram exit code: " + iso.getExitVal()
                + "  handlerProgram message: " + iso.getErrorString());
		}
		
		OutputType outputType = ri.appConfig.getOutputType();
		
		if (ri.perRequestOutputType != null) {
			outputType = ri.perRequestOutputType;
		}
		
		ResponseBuilder builder = Response.status(status)
				.type(AppConfigurator.getMimeType(outputType)).entity(iso);    
		builder.header("Content-Disposition", AppConfigurator.getContentDispositionType(outputType) + 
				"; filename=" + ri.appConfig.getOutputFilename(outputType));	
		
		// Insert CORS header elements. 
		if (ri.appConfig.getAllowCors()) {
		    builder.header("Access-Control-Allow-Origin", "*");
		    builder.header("Access-Control-Allow-Credentials", "true");
		    builder.header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT");
		    builder.header("Access-Control-Allow-Headers", "Content-Type, Accept");
		}
	    
		return builder.build();
	}
	
	private void preProcess(RequestInfo ri, List<String> cmd) throws Exception {
		
		String className = ri.appConfig.getArgPreProcessorClassName();
		ArgPreProcessor argpp = null;
		
		try {
    		Class<?> argppClass;
    		argppClass = Class.forName(className);
    		argpp = (ArgPreProcessor) argppClass.newInstance();
		} catch (ClassNotFoundException e) {
			String err = "Could not find class with name: " + className;
			logger.fatal(err);
			throw new RuntimeException(err);
		} catch (InstantiationException e) {
			String err = "Could not instantiate class: " + className;
			logger.fatal(err);
			throw new RuntimeException(err);
		} catch (IllegalAccessException e) {
			String err = "Illegal access while instantiating class: " + className;
			logger.fatal(err);
			throw new RuntimeException(err);
		}
		
		argpp.process(ri,  cmd);
	}
	
	private void shellException(Status badRequest, String s) {
		ServiceShellException.logAndThrowException(ri, badRequest, s);
	}
}
