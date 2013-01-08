package edu.iris.wss;

import java.io.*;
import java.net.URL;
import java.util.*;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;

import edu.iris.wss.framework.FdsnStatus.Status;

import org.apache.log4j.Logger;

import edu.iris.wss.IrisStreamingOutput.IrisStreamingOutput;
import edu.iris.wss.IrisStreamingOutput.ProcessStreamingOutput;
import edu.iris.wss.framework.*;
import edu.iris.wss.utils.WebUtils;

@Path ("/")
public class Wedge {
	 
	@Context 	ServletContext context;
	@Context	javax.servlet.http.HttpServletRequest request;
    @Context 	UriInfo uriInfo;	
    @Context 	HttpHeaders requestHeaders;

    @Context 	SingletonWrapper sw;
    
    private RequestInfo ri;
    
	public static final Logger logger = Logger.getLogger(Wedge.class);
	
	public Wedge()  {
    	TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}	
	
	@Path("status")
	@GET
	public String cfg() {
    	ri = new RequestInfo(sw, uriInfo, request, requestHeaders);

    	StringBuilder sb = new StringBuilder();
		sb.append("<HTML><BODY>");
		sb.append("<TABLE BORDER=2 cellpadding='2' style='width: 600px'>");
		sb.append("<TR><TH COLSPAN=\"2\">Servlet Environment</TH></TR>");
		sb.append("<TR><TD>" + "URL" + "</TD><TD>" + request.getRequestURI() + "</TD></TR>");
		sb.append("<TR><TD>" + "Host" + "</TD><TD>" + WebUtils.getHost(request) + "</TD></TR>");
		sb.append("<TR><TD>" + "Port" + "</TD><TD>" + WebUtils.getPort(request) + "</TD></TR>");
		sb.append("</TABLE>");

		sb.append("<br/>");
    	sb.append(ri.sw.statsKeeper.toHtmlString());

    	sb.append("<br/>");
    	sb.append(ri.appConfig.toHtmlString());

    	sb.append("</BODY></HTML>");

		return sb.toString();
	}
	
	@Path("204")
	@GET
	public Response foo() {
    	ri = new RequestInfo(sw, uriInfo, request, requestHeaders);
    	
		ArrayList<String> cmd = new ArrayList<String>();
		try {
			ParameterTranslator.parseQueryParams(cmd, ri);
		} catch (Exception e) {
			shellException(Status.BAD_REQUEST, e.getMessage());
		}

		return Response.status(ProcessStreamingOutput.processExitVal(1,  ri)).build();
	}
	
	// [region] Root path documentation handler and version handler
	
    @Path("/")
    @GET
    public Response ok() {
    	if (sw == null) logger.info("It's null");
    	ri = new RequestInfo(sw, uriInfo, request, requestHeaders);

      	InputStream is = null;
      	URL url = null;
    	try {    		
    		url = new URL(ri.appConfig.getRootServiceDoc());    		
    		is = url.openStream();
    	} catch (Exception e) {
    		String err = "Can't find root documentation page: " + url.toString();
        	return  Response.status(Status.OK).entity(err).type("text/plain").build();
    	}
    	
    	final BufferedReader br = new BufferedReader( new InputStreamReader( is));

    	StreamingOutput so = new StreamingOutput() {
    		@Override
			public void write(OutputStream outputStream) throws IOException, WebApplicationException {
    			BufferedWriter writer = new BufferedWriter (new OutputStreamWriter(outputStream));
    			String inputLine = null ;
    			while ((inputLine = br.readLine()) != null) {
    				inputLine = inputLine.replace("VERSIONHOST", 
    							"<div id=\"version\"><pre>Version: " + ri.appConfig.getVersion() 
    							+ "</pre></div>");
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
	@GET
	public String getWssVersion() throws IOException {
    	return "0.5.4";
	}
	
	@Path("version")
	@GET
	public String getVersion() {
    	ri = new RequestInfo(sw, uriInfo, request, requestHeaders);
    	return ri.appConfig.getVersion();
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

	// [end region]

	private Response processQuery() {
    	
		if (ri.appConfig.getStreamingOutputClassName() != null) {
			return runJava();
		} else {
			return runCommand();
		}
	}
	
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
			if (status != Status.OK) {
				
				if (status == null) 
					shellException(Status.INTERNAL_SERVER_ERROR, "Null status from StreamingOutput class");
				else
					shellException(status, status.toString() + ": " +  iso.getErrorString());
			}
			
			ResponseBuilder builder = Response.status(status)
					.type(ri.appConfig.getMimeType()).entity(iso);
			builder.header("Content-Disposition", "inline; filename=" + ri.appConfig.getOutputFilename());	
			return builder.build();
	}
		
	private Response runCommand() {

    	// Create the 'command' array by first adding on the program to 
    	// be invoked.  Then parse the query parameters.  These are appended
    	// to the cmd argument.
    	
    	// The handler program string from the config file may contain multiple space-delimited
    	// text. These need to be split and added to the cmd collection
		logger.info("CMD: " + ri.appConfig.getHandlerProgram());
		ArrayList<String> cmd = new ArrayList<String>(Arrays.asList(ri.appConfig.getHandlerProgram().split(" ")));
		
		try {
			ParameterTranslator.parseQueryParams(cmd, ri);
		} catch (Exception e) {
			shellException(Status.BAD_REQUEST, e.getMessage());
		}
		
//		logger.info("CMD: " + cmd);
//		for (String key: ri.request.getParameterMap().keySet()) {
//			logger.info("PM; Key: " + key + " Val: " + ri.request.getParameter(key));
//		}
		
				
	    ProcessBuilder pb = new ProcessBuilder(cmd);
	    pb.directory(new File(context.getRealPath(ri.appConfig.getWorkingDirectory())));    
	   
	    pb.environment().put("REQUESTURL", WebUtils.getUrl(request));
	    pb.environment().put("USERAGENT", WebUtils.getUserAgent(request));
	    pb.environment().put("IPADDRESS", WebUtils.getClientIp(request));
	    pb.environment().put("APPNAME", ri.appConfig.getAppName());
	    pb.environment().put("VERSION", ri.appConfig.getVersion());
	   
		ProcessStreamingOutput iso = new ProcessStreamingOutput(pb, ri);      
		
		// Wait for an exit code, the start of data transmission or a timeout.
		Status status = iso.getResponse();
		if (status == Status.NO_CONTENT) {
			logger.info("Exit val = " + iso.getExitVal());
			ServiceShellException.logAndThrowException(ri, status, null);
		} else if (status != Status.OK) {
			logger.info("Exit val = " + iso.getExitVal());
			shellException(status, "Command exit code: " + iso.getExitVal() + "  " + iso.getErrorString());
		}
		
		ResponseBuilder builder = Response.status(status)
				.type(ri.appConfig.getMimeType()).entity(iso);
		builder.header("Content-Disposition", "inline; filename=" + ri.appConfig.getOutputFilename());	
		return builder.build();
	}

	
	private void shellException(Status badRequest, String s) {
		ServiceShellException.logAndThrowException(ri, badRequest, s);
	}
}
