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

import com.sun.jersey.api.uri.UriComponent;
import java.io.*;
import java.net.URL;
import java.util.*;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;

import edu.iris.wss.framework.FdsnStatus.Status;
import edu.iris.wss.framework.RequestInfo.CallType;

import org.apache.log4j.Logger;

import edu.iris.wss.IrisStreamingOutput.IrisStreamingOutput;
import edu.iris.wss.IrisStreamingOutput.ProcessStreamingOutput;
import edu.iris.wss.framework.*;
import edu.iris.wss.utils.WebUtils;
import java.util.logging.Level;

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
	public Response getStatus() {
        ri = RequestInfo.createInstance(sw, uriInfo, request, requestHeaders);

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

        ResponseBuilder builder = Response.status(Status.OK)
              .entity(sb.toString())
              .type("text/html");
        addCORSHeadersIfConfigured(builder, ri);
		return builder.build();
	}
	
	// [region] Root path documentation handler, version handler and WADL

	private String defDoc(String htmlMarkupMsg) {
        ri = RequestInfo.createInstance(sw, uriInfo, request, requestHeaders);
		return "<!DOCTYPE html>"
              + "<html><head>"
              + "<style>"
              + "#wssinfo {"
//              + "font-size: small;"
              + "background-color: #d0f0d0;"
              + "display: inline-block;"
              + "position: absolute;"
              + "top: 70px;"
              + "left: 10px;"
              + "}"
              + "#message {"
              + "background-color: #f0d0d0;"
              + "display: inline-block;"
              + "position: absolute;"
              + "top: 120px;"
              + "left: 10px;"
              + "}"
              + "</style>"
              + "<title>Welcome to the Web Service Shell</title>"
              + "</head>"
              + "<body>"
              + "<div id=\"container\">"
              + "<div id=\"welcome\"><h2>Welcome to the Web Service Shell</h2></div>"
              + "<div id=\"wssinfo\">"
              + "<div><b>Web Service Shell version:</b> "
              + ri.appConfig.getWssVersion() + "</div>"
              + "<div><b>appName:</b> " + ri.appConfig.getAppName()
              + "&nbsp&nbsp<b>version:</b> " + ri.appConfig.getVersion() + "</div>"
              + "</div>"

              + "<div id=\"message\">"
              + htmlMarkupMsg
              + "</div>"

              + "</div>"
              + "</body></html>";
	}
	
	private static boolean isOkString(String s) {
		return ((s != null) && !s.isEmpty());
	}
	
    @GET
    public Response rootpath() {
    	ri = RequestInfo.createInstance(sw, uriInfo, request, requestHeaders);

    	String docUrl = ri.appConfig.getRootServiceDoc();
    	
    	// Dump some default text if no good Service Doc Config
    	if (!isOkString(docUrl)) {
            String htmlMarkup ="<div>The <b>rootServiceDoc</b> parameter is not"
                  + " set in the service configuration file.</div>"
                  + "<div>You can configure documentation for this service by using"
                  + " the <b>rootServiceDoc</b> parameter in the *-service.cfg"
                  + " file.</div>";
            ResponseBuilder builder = Response.status(Status.OK)
                  .entity(defDoc(htmlMarkup))
                  .type("text/html");
            addCORSHeadersIfConfigured(builder, ri);
            return builder.build();
        }
    	
      	InputStream is = null;
      	URL url = null;
    	try {    		
    		url = new URL(docUrl);    		
    		is = url.openStream();
    	} catch (Exception e) {
            String htmlMarkup = "<div>An exception occurred while reading the"
                  + " contents of the <b>rootServiceDoc</b> parameter.</div>"
                  + "<div><b>rootServiceDoc value:</b> " + docUrl + "</div>"
                  + "<div><b>Exception:</b> " + e.toString() + "</div>"
                  + "<div>The <b>rootServiceDoc</b> parameter is in the"
                  + " *-service.cfg parameter file.</div>";
        	ResponseBuilder builder = Response.status(Status.OK)
                  .entity(defDoc(htmlMarkup))
                  .type("text/html");
            addCORSHeadersIfConfigured(builder, ri);
            return builder.build();
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

        ResponseBuilder builder = Response.status(Status.OK)
              .entity(so)
              .type("text/html");
        addCORSHeadersIfConfigured(builder, ri);
		return builder.build();
    }

    @Path("wssversion")
	@GET @Produces("text/plain")
	public Response getWssVersion() throws IOException {
        ri = RequestInfo.createInstance(sw, uriInfo, request, requestHeaders);

        ResponseBuilder builder = Response.status(Status.OK)
              .type(MediaType.TEXT_PLAIN)
              .entity(AppConfigurator.wssVersion);

        addCORSHeadersIfConfigured(builder, ri);

		return builder.build();
	}
	
    private void addCORSHeadersIfConfigured(ResponseBuilder rb, RequestInfo ri) {
		if (ri.appConfig.getCorsEnabled()) {
            // Insert CORS header elements.
		    rb.header("Access-Control-Allow-Origin", "*");

            // dont add this unless cookies are expected
//            rb.header("Access-Control-Allow-Credentials", "true");

            // Not setting these at this time - 2015-08-12
//            rb.header("Access-Control-Allow-Methods", "HEAD, GET, POST");
//            rb.header("Access-Control-Allow-Headers", "Content-Type, Accept");

            // not clear if needed now, 2015-08-12, but this is how to let client
            // see what headers are available, although "...Allow-Headers" may be
            // sufficient
//            rb.header("Access-Control-Expose-Headers", "X-mycustomheader1, X-mycustomheader2");
		}
    }

	@Path("version")
	@GET @Produces("text/plain")
	public Response getVersion() {
    	ri = RequestInfo.createInstance(sw, uriInfo, request, requestHeaders);

        ResponseBuilder builder = Response.status(Status.OK)
              .type(MediaType.TEXT_PLAIN)
              .entity(ri.appConfig.getVersion());

        addCORSHeadersIfConfigured(builder, ri);

		return builder.build();
	}
	
	@Path("whoami")
	@GET @Produces("text/plain")
	public Response getwho() {
    	ri = RequestInfo.createInstance(sw, uriInfo, request, requestHeaders);

        ResponseBuilder builder = Response.status(Status.OK)
              .type(MediaType.TEXT_PLAIN)
              .entity(request.getRemoteAddr());

        addCORSHeadersIfConfigured(builder, ri);

		return builder.build();
	}	
	
	@Path("application.wadl")
	@GET @Produces ("application/xml")
	public Response getWadl() {
		
    	ri = RequestInfo.createInstance(sw, uriInfo, request, requestHeaders);
    	
    	// First check if wadlPath configuration is set.  If so, then stream from that URL.
    	
    	String wadlPath = ri.appConfig.getWadlPath();
    	if ((wadlPath != null) && (!wadlPath.isEmpty())) {
            logger.info("Attempting to load WADL from: " + wadlPath);

        	InputStream is = null;
          	URL url = null;
        	try {    		
        		url = new URL(wadlPath);    		
        		is = url.openStream();
        	} catch (Exception ex) {
        		String errMsg = "Wss - Error getting WADL URL: " + wadlPath + "  ex: "
                      + ex;
                shellException(adjustByCfg(Status.NO_CONTENT, ri), errMsg);
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

            ResponseBuilder builder = Response.status(Status.OK)
                  .type("application/xml")
                  .entity(so);
            addCORSHeadersIfConfigured(builder, ri);
    		return builder.build();
    	}

		// Try to read a user WADL file from the location specified by the
		// wssConfigDir property concatenated with the web application name (last part
		// of context path), e.g. 'station' or 'webserviceshell'
		String wadlFileName = null;
		String configBase = WebUtils.getConfigFileBase(context);
		FileInputStream wadlStream = null;
        String errMsg = "Wss - Error getting default WADL file";
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

                    ResponseBuilder builder = Response.status(Status.OK)
                          .type("application/xml")
                          .entity(wadlStream);
                    addCORSHeadersIfConfigured(builder, ri);
                    return builder.build();
				} else {
                    errMsg = errMsg + "  wadlFileName: " + wadlFileName;
                }
			} else {
                errMsg = errMsg + "  wssConfigDir: " + wssConfigDir
                      + "  configBase: " + configBase;
            }
		} catch (Exception ex) {
            errMsg = errMsg + "  ex: " + ex;
		}

        Status status = adjustByCfg(Status.NO_CONTENT, ri);
        shellException(status, errMsg);

        ResponseBuilder builder = Response.status(status)
              .type(MediaType.TEXT_PLAIN);
        addCORSHeadersIfConfigured(builder, ri);
        return builder.build();
	}

	@Path("v2/swagger")
	@GET @Produces ({"application/json", "text/plain"})
	public Response getSwaggerV2Specification() {
		
    	ri = RequestInfo.createInstance(sw, uriInfo, request, requestHeaders);
    	
    	// First check if swaggerV2Spec configuration paramter is set.
        // If so, then return stream object from that URL.
    	
    	String resourceURLStr = ri.appConfig.getSwaggerV2URL();
    	if ((resourceURLStr != null) && (!resourceURLStr.isEmpty())) {
            logger.info("Attempting to load resource from: " + resourceURLStr);
            
        	InputStream is = null;
          	URL url = null;
        	try {    		
        		url = new URL(resourceURLStr);    		
        		is = url.openStream();
        	} catch (Exception ex) {
        		String errMsg = "Wss - Error on Swagger V2 URL: "
                      + resourceURLStr + "  ex: " + ex;
//                logger.error("Failure loading SwaggerV2 file from: " + url
//                + "  ex: " + ex);
//            	return  Response.status(Status.OK).entity(err).type("text/plain").build();

                shellException(adjustByCfg(Status.NO_CONTENT, ri), errMsg);
        	}
        	
        	final BufferedReader br = new BufferedReader( new InputStreamReader( is));

        	StreamingOutput so = new StreamingOutput() {
        		@Override
    			public void write(OutputStream outputStream) throws IOException,
                      WebApplicationException {
        			BufferedWriter writer =
                          new BufferedWriter (new OutputStreamWriter(outputStream));
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

            ResponseBuilder builder = Response.status(Status.OK)
                  .type("application/json")
                  .entity(so);
            addCORSHeadersIfConfigured(builder, ri);
    		return builder.build();
    	}

        // else - for resource URL not found, try with a default name
        //
		// Try to read the resource file from the webserviceshell configuration
        // file folder using the same naming conventions as regular
        // webserviceshell cfg files.
		String resourceFileName = null;
		String configBase = WebUtils.getConfigFileBase(context);
		FileInputStream fileInStream = null;
        String errMsg = "Wss - Error getting default resource file";
		try {
			String wssConfigDir = System.getProperty(AppConfigurator.wssConfigDirSignature);
			if (isOkString(wssConfigDir) && isOkString(configBase)) {
				if (!wssConfigDir.endsWith("/")) {
					wssConfigDir += "/";
                }
				
				resourceFileName = wssConfigDir + configBase + "-swagger.json";		
			
				File resourceFile = new File(resourceFileName);
				if (resourceFile.exists()) {
					logger.info("Attempting to load resource file from: "
                          + resourceFileName);

					fileInStream = new FileInputStream(resourceFileName);
                    ResponseBuilder builder = Response.status(Status.OK)
                          .type("application/json")
                          .entity(fileInStream);
                    addCORSHeadersIfConfigured(builder, ri);
                    return builder.build();
				} else {
                    errMsg = errMsg + "  resourceFileName: " + resourceFileName;
                }
			} else {
                errMsg = errMsg + "  wssConfigDir: " + wssConfigDir
                      + "  configBase: " + configBase;
            }
		} catch (Exception ex) {
            errMsg = errMsg + "  ex: " + ex;
		}

        Status status = adjustByCfg(Status.NO_CONTENT, ri);
        shellException(status, errMsg);

        ResponseBuilder builder = Response.status(status)
              .type(MediaType.TEXT_PLAIN);
        addCORSHeadersIfConfigured(builder, ri);
        return builder.build();
	}
	
	// [end region]

	// [region] Main query entry points GET, POST
	
	@POST
	@Path("queryauth") 
	public Response postQueryAuth(String pb) {
    	ri = RequestInfo.createInstance(sw, uriInfo, request, requestHeaders);
    	ri.postBody = pb;
    	
		if (! ri.appConfig.getPostEnabled()) 
			shellException(Status.BAD_REQUEST, "POST Method not allowed");
		
		ri.statsKeeper.logAuthPost();
		return processQuery();
	}
	 
	@POST
	@Path("query")
	public Response postQuery(String pb) {
    	ri = RequestInfo.createInstance(sw, uriInfo, request, requestHeaders);
    	ri.postBody = pb;
    	
		if (! ri.appConfig.getPostEnabled()) 
			shellException(Status.BAD_REQUEST, "POST Method not allowed");
        
		ri.statsKeeper.logPost();
		return processQuery();
	}

    @POST
    @Path("info") // on demand synthtics model information
    public Response postInfo(String pb) {
        ri = RequestInfo.createInstance(sw, uriInfo, request, requestHeaders);
        ri.postBody = pb;

        if (! ri.appConfig.getPostEnabled())
          shellException(Status.BAD_REQUEST, "POST Method not allowed");

        ri.statsKeeper.logPost();
        return processInfo();
    }

//	@POST
//	@Path("test")
//    @Produces("text/plain")
//    public String postTest(String pb) {
//        return postprocess(pb, false);
//    }
//    
//	@POST
//	@Path("test/usehandler")
//    @Produces("text/plain")
//    public String posTestUseHandler(String pb) {
//        return postprocess(pb, true);
//    }
//    
//	public String postprocess(String pb, Boolean useHandler) {
//    	ri = RequestInfo.createInstance(sw, uriInfo, request, requestHeaders);
//    	ri.postBody = pb;
//   	
//		if (! ri.appConfig.getPostEnabled()) {
//			shellException(Status.BAD_REQUEST, "POST Method not allowed");
//        }
// 		ri.statsKeeper.logPost();
//        
//        String cType = ri.request.getContentType();
//        Map pMap = ri.request.getParameterMap();
//        
//        System.out.println("********* post context path: "
//              + ri.request.getContextPath() + "  cType: " + cType);
//        
//        System.out.println("********* post parameter map: " + pMap);
//        System.out.println("********* post body: " + pb);
//        
//        String answer = "default";
//        
//        if (cType.equals("application/json")) {
//            answer = pb;
//        } else if (cType.equals("application/x-www-form-urlencoded")) {
//            System.out.println("********* post decoded  true: "
//                  + UriComponent.decodeQuery(pb, true));
//            System.out.println("********* post decoded false: "
//                  + UriComponent.decodeQuery(pb, false));
//            
//            answer = UriComponent.decodeQuery(pb, true).toString();
//        } else {
//            answer = pb;
//        }
//        
//        Response handlerResp = null;
//        if (useHandler) {
//            handlerResp = processQuery();
//            answer = handlerResp.toString();
//        }
//        
//        return answer;
//	}
	
	@GET 
	@Path("test")
    @Produces("text/plain")
	public Response getTest() throws Exception {
        ri = RequestInfo.createInstance(sw, uriInfo, request, requestHeaders);
        
    	ri.statsKeeper.logGet();
            
        System.out.println("********* get test type: " + ri.request.getContentType());
        System.out.println("********* get test context path: " + ri.request.getContextPath());
        System.out.println("********* get test parameter map: " + ri.request.getParameterMap());
        System.out.println("********* get test header accept: " + ri.request.getHeader("accept"));
        //return processQuery();

        ResponseBuilder builder = Response.status(Status.OK)
              .type(MediaType.TEXT_PLAIN)
              .entity("get test was called");

        addCORSHeadersIfConfigured(builder, ri);

		return builder.build();
	}
	 
//	@POST
//    @Consumes("application/x-www-form-urlencoded")
//    @Produces("text/plain")
//	@Path("query") 
//	public String postQuery(MultivaluedMap<String, String> postParams) {
//
//    	System.out.println("******* postParams:  " + postParams);
//        
//		if (! ri.appConfig.getPostEnabled()) 
//			shellException(Status.BAD_REQUEST, "POST Method not allowed");
//		
//		ri.statsKeeper.logPost();
//		return postParams.toString();
//	}

    @GET 
    @Path("info") // on demand synthtics model information
    public Response getInfo() throws Exception {
      ri = RequestInfo.createInstance(sw, uriInfo, request, requestHeaders);

      ri.statsKeeper.logGet();
      return processInfo();
    }

	@GET
	@Path("queryauth")
	public Response queryAuth() throws Exception {
    	ri = RequestInfo.createInstance(sw, uriInfo, request, requestHeaders);

    	ri.statsKeeper.logAuthGet();
		return processQuery();
	}
	
	@GET 
	@Path("query")
	public Response query() throws Exception {
        ri = RequestInfo.createInstance(sw, uriInfo, request, requestHeaders);
        
    	ri.statsKeeper.logGet();
		return processQuery();
	}
	
	@GET 
	@Path("catalogs")
	public Response catalogs() throws Exception {
    	ri = RequestInfo.createInstance(sw, uriInfo, request, requestHeaders);
    	ri.callType = CallType.CATALOGS;

    	ri.statsKeeper.logGet();
		return processQuery();
	}
	
	@GET 
	@Path("contributors")
	public Response contributors() throws Exception {
    	ri = RequestInfo.createInstance(sw, uriInfo, request, requestHeaders);
    	ri.callType = CallType.CONTRIBUTORS;

    	ri.statsKeeper.logGet();
		return processQuery();
	}	
	
	@GET 
	@Path("counts")
	public Response counts() throws Exception {
    	ri = RequestInfo.createInstance(sw, uriInfo, request, requestHeaders);
    	ri.callType = CallType.COUNTS;

    	ri.statsKeeper.logGet();
		return processQuery();
	}	

	private Response processInfo() {
		if (!ri.appConfig.isValid())
			shellException(Status.INTERNAL_SERVER_ERROR, "Application Configuration Issue");

		if (ri.appConfig.getStreamingOutputClassName() != null) {
			return runJava("info");
		} else {
			return runCommand("info");
		}
	}

	private Response processQuery() {
		if (!ri.appConfig.isValid())
			shellException(Status.INTERNAL_SERVER_ERROR, "Application Configuration Issue");

		if (ri.appConfig.getStreamingOutputClassName() != null) {
			return runJava("query");
		} else {
			return runCommand("query");
		}
	}
	
	// [end region]

	private Response runJava(String classChoice) {
		
		// Run the parameter translator to test consistency.  We need an arraylist, but it's not used.
		ArrayList<String> cmd = new ArrayList<String>();
		try {
			ParameterTranslator.parseQueryParams(cmd, ri);
		} catch (Exception e) {
			shellException(Status.BAD_REQUEST, "Wss - " + e.getMessage());
		}

        String className = null;
        if (classChoice.equals("info")) {
            className = ri.appConfig.getStreamingOutputInfoClassName();
        } else {
            className = ri.appConfig.getStreamingOutputClassName();
        }
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
        
        if (ri.request.getMethod().equals("HEAD")) {
            // return to Jersey before any more processing
            String noData = "";
            ResponseBuilder builder = Response.status(Status.OK)
                  .type("text/plain")
                  .entity(noData);
            addCORSHeadersIfConfigured(builder, ri);
            return builder.build();
        }
		
		iso.setRequestInfo(ri);
		
		// Wait for an exit code, expecting the start of data transmission
        // or exception or timeout.
		Status status = iso.getResponse();
        
		if (status == null) {
            shellException(Status.INTERNAL_SERVER_ERROR, "Null status from StreamingOutput class");
        }
        
        status = adjustByCfg(status, ri);
		
        if (status != Status.OK) {
            newerShellException(status, ri, iso);
		}

        String mediaType = null;
        try {
            mediaType = ri.getPerRequestMediaType();
        } catch (Exception ex) {
            shellException(Status.INTERNAL_SERVER_ERROR, "Unknow mediaType for"
                    + " mediaTypeKey: " + ri.getPerRequestOutputTypeKey()
                    + ServiceShellException.getErrorString(ex));
        }
        
        ResponseBuilder builder = Response.status(status)
              .type(mediaType)
              .entity(iso);
        builder.header("Content-Disposition", ri.createContentDisposition());
		
		addCORSHeadersIfConfigured(builder, ri);
	    
		return builder.build();
	}
		
	private Response runCommand(String cmdChoice)  {

    	// Create the 'command' array by first adding on the program to 
    	// be invoked.  Then parse the query parameters.  These are appended
    	// to the cmd argument.
    	
    	// The handler program string from the config file may contain multiple space-delimited
    	// text. These need to be split and added to the cmd collection
		ArrayList<String> cmd = null;

		switch (ri.callType) {
			case NORMAL:
				// Handler string can't be NULL per configuration requirements in AppConfigurator
                if (cmdChoice.equals("info")) {
                    cmd = new ArrayList<String>(Arrays.asList(
                          ri.appConfig.getInfoHandlerProgram().split(" ")));
                } else {
                    cmd = new ArrayList<String>(Arrays.asList(
                          ri.appConfig.getHandlerProgram().split(" ")));
                }
				try {
					ParameterTranslator.parseQueryParams(cmd, ri);
				} catch (Exception e) {
					shellException(Status.BAD_REQUEST, "Wss - " + e.getMessage());
				}
				break;
			case CATALOGS:
				String catalogsHandlerString = ri.appConfig.getCatalogsHandlerProgram();
				if (!isOkString(catalogsHandlerString))
					shellException(Status.NOT_FOUND,
                            "catalogHandler msg: " + catalogsHandlerString);

				cmd = new ArrayList<>(Arrays.asList(catalogsHandlerString.split(" ")));
				try {
					ri.setPerRequestOutputType("XML");
				} catch (Exception e) { ; }
				break;
				
			case CONTRIBUTORS:
				String contributorsHandlerString = ri.appConfig.getContributorsHandlerProgram();

				if (!isOkString(contributorsHandlerString))
					shellException(Status.NOT_FOUND,
                            "contributorsHandler msg: " + contributorsHandlerString);
				
				cmd = new ArrayList<>(Arrays.asList(contributorsHandlerString.split(" ")));
				try {
					ri.setPerRequestOutputType("XML");
				} catch (Exception e) { ; }
				break;
				
			case COUNTS:
				String countsHandlerString = ri.appConfig.getCountsHandlerProgram();

				if (!isOkString(countsHandlerString))
					shellException(Status.NOT_FOUND,
                            "countsHandler msg: " + countsHandlerString);
				
				cmd = new ArrayList<>(Arrays.asList(countsHandlerString.split(" ")));
				try {
					ri.setPerRequestOutputType("XML");
				} catch (Exception e) { ; }
				break;
		}
        
        if (ri.request.getMethod().equals("HEAD")) {
            // return to Jersey before any more processing
            String noData = "";
            ResponseBuilder builder = Response.status(Status.OK)
                  .type("text/plain")
                  .entity(noData);
            addCORSHeadersIfConfigured(builder, ri);
            return builder.build();
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
        pb.environment().put("CLIENTNAME", WebUtils.getClientName(request));
        pb.environment().put("HOSTNAME", WebUtils.getHostname());
        if (WebUtils.getAuthenticatedUsername(requestHeaders) != null) {
            pb.environment().put("AUTHENTICATEDUSERNAME",
                    WebUtils.getAuthenticatedUsername(requestHeaders));
        }
	   
		ProcessStreamingOutput iso = new ProcessStreamingOutput(pb, ri);      
		
		// Wait for an exit code, expecting the start of data transmission
        // or exception or timeout.
		Status status = iso.getResponse();
        
        status = adjustByCfg(status, ri);
		
        if (status != Status.OK) {
            newerShellException(status, ri, iso);
		}
		
        String mediaType = null;
        try {
            mediaType = ri.getPerRequestMediaType();
        } catch (Exception ex) {
            shellException(Status.INTERNAL_SERVER_ERROR, "Unknow mediaType for"
                    + " mediaTypeKey: " + ri.getPerRequestOutputTypeKey()
                    + ServiceShellException.getErrorString(ex));
        }
        
        ResponseBuilder builder = Response.status(status)
              .type(mediaType)
              .entity(iso);
        builder.header("Content-Disposition", ri.createContentDisposition());
		
		addCORSHeadersIfConfigured(builder, ri);
	    
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
	
	private void shellException(Status status, String message) {
		ServiceShellException.logAndThrowException(ri, status, message);       
	}
	
	private static void newerShellException(Status status, RequestInfo ri, 
            IrisStreamingOutput iso) {
		ServiceShellException.logAndThrowException(ri, status,
                status.toString() + iso.getErrorString());
	}

    private static Status adjustByCfg(Status trialStatus, RequestInfo ri) {
        if (trialStatus == Status.NO_CONTENT) {
            // override 204 if configured to do so
            if (ri.perRequestUse404for204) {
                return Status.NOT_FOUND;
            }
        }
        return trialStatus;
    }
}
