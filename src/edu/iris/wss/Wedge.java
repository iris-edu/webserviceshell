package edu.iris.wss;

import java.io.*;
import java.net.URL;
import java.util.*;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.*;

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
	
	// [region] Root path documentation handler and version handler
    @Path("/")
    @GET
    public Response ok() {
    	if (sw == null) logger.info("It's null");
    	ri = new RequestInfo(sw, uriInfo, request, requestHeaders);

      	InputStream is = null;
    	try {
    		
    		// RKRK Fix this when we have a real context path
//    		String serviceName = context.getContextPath();
//    		URL l_URL = new URL(ri.config.getRootServiceDoc() + "/" + serviceName + "_root.htm");
    		
    		logger.info(ri.appConfig.getRootServiceDoc() + "/" + "flinnengdahl" + "_root.htm");
    		URL l_URL = new URL(ri.appConfig.getRootServiceDoc() + "/" + "flinnengdahl" + "_root.htm");
    		is = l_URL.openStream();

    	} catch (Exception e) {
    		String err = "Can't find root documentation page";
        	ResponseBuilder builder = Response.status(Status.OK).entity(err).type("text/plain");
    		return builder.build();
    	}
    	
    	final BufferedReader br = new BufferedReader( new InputStreamReader( is));
    	final String crazyHostPort = WebUtils.getCrazyHostPort(request); 	

    	StreamingOutput so = new StreamingOutput() {
    		@Override
			public void write(OutputStream outputStream) throws IOException, WebApplicationException {
    			BufferedWriter writer = new BufferedWriter (new OutputStreamWriter(outputStream));
    			String inputLine = null ;
    			while ((inputLine = br.readLine()) != null) {
    				inputLine = inputLine.replace("VERSIONHOST", 
    							"<div id=\"version\"><pre>Version: " + ri.appConfig.getVersion() + "<BR/>" + 
    							crazyHostPort + "</pre></div>");
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
	
	@Path("version")
	@GET
	public String getVersion() {
    	ri = new RequestInfo(sw, uriInfo, request, requestHeaders);
    	return ri.appConfig.getVersion();
	}
	
	// [end region]

	
	@GET
	@Path("tt")
	public String tt() {
    	ri = new RequestInfo(sw, uriInfo, request, requestHeaders);

		WebApplicationException wae = new WebApplicationException(Status.OK);
		ServiceShellException.logAndThrowException(ri, wae, "fongy");
return "neverland";
	}
	
	@POST
	@Path("posty") 
	public String toast(String txt) {

		return "toast: " + txt;
	}
	
	@GET
	@Path("queryauth")
	public Response queryauth() throws Exception {
		return processQuery();
	}

	
	@GET 
	@Path("query")
	public Response query() throws Exception {
		return processQuery();
	}
	
	private Response processQuery() {
    	ri = new RequestInfo(sw, uriInfo, request, requestHeaders);
		if (ri.appConfig.getStreamingOutputClassName() != null) {
			return runJava();
		} else {
			return runCommand();
		}
	}
	
	private Response runJava() {
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
    			logger.fatal("Could not instantiate class: " + className);
    		} catch (IllegalAccessException e) {
    			logger.fatal("Illegal access while instantiating class: " + className);
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
		ArrayList<String> cmd = new ArrayList<String>(Arrays.asList(ri.appConfig.getHandlerProgram().split(" ")));
		
		try {
			ParameterTranslator.parseQueryParams(cmd, ri);
		} catch (Exception e) {
			shellException(Status.BAD_REQUEST, e.getMessage());
		}
		
		for (String key: ri.request.getParameterMap().keySet()) {
			logger.info("PM; Key: " + key + " Val: " + ri.request.getParameter(key));
		}
		
				
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
		if (status != Status.OK) {
			logger.info("Exit val = " + iso.getExitVal());
			shellException(status, "Command exit code: " + iso.getExitVal() + "  " + iso.getErrorString());
		}
		
		ResponseBuilder builder = Response.status(status)
				.type(ri.appConfig.getMimeType()).entity(iso);
		builder.header("Content-Disposition", "inline; filename=" + ri.appConfig.getOutputFilename());	
		return builder.build();
	}
	
//	@GET
//	@Path("vingy")
//	public Response getStuffed() throws Exception {
//    	ri = WebUtils.makeRequestInfo(uriInfo, request, requestHeaders);
//
//		ArrayList<String> cmd = new ArrayList<String>();
//	    cmd.add("/Users/rich/Documents/IndigoWorkspace/Woof/Release/Woof");
//	    cmd.add("-d2000");	
//	    cmd.add("-s2000");
////	    cmd.add("-e2");
////	    cmd.add("-D1500");
//	    cmd.add("-K");
////	    cmd.add("-f/tmp/alm.log");
////	    cmd.add("-EOws12345678901234567890");
//	   
//   
//	    logger.info(cmd.toString());
//	    
//	    ProcessBuilder pb = new ProcessBuilder();
//	    pb.command(cmd);
//	    pb.directory(new File(context.getRealPath("/") ));
//	    
//	    
//		IrisStreamingOutput iso = new IrisStreamingOutput(pb, ri);      
//		
//		Status status = iso.getResponse();
//		if (status != Status.OK) {
//			logger.info("Exit val = " + iso.exitVal);
//			return Response.status(status).entity(iso.getErrorString()).build();
//		}
//		
//		ResponseBuilder builder = Response.status(status).entity(iso);
//		return builder.build();	  		
//	}

	
	private void shellException(Response.Status status, String s) {
		ServiceShellException.logAndThrowException(ri, status, s);
	}
}
