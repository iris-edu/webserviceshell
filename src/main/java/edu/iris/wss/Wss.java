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

package edu.iris.wss;

import edu.iris.wss.framework.AppConfigurator;
import java.io.*;
import java.net.URL;
import java.util.*;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;

import edu.iris.wss.framework.FdsnStatus.Status;
import edu.iris.wss.framework.RequestInfo;
import edu.iris.wss.framework.WssSingleton;
import edu.iris.wss.framework.Util;
import org.apache.log4j.Logger;
import edu.iris.wss.utils.WebUtils;

@Path ("/")
public class Wss {
	@Context 	ServletContext context;
	@Context	javax.servlet.http.HttpServletRequest request;
    @Context 	UriInfo uriInfo;	
    @Context 	HttpHeaders requestHeaders;

    @Context 	WssSingleton sw;

    //private RequestInfo ri;
    
	public static final Logger logger = Logger.getLogger(Wss.class);
	
	public Wss()  {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	@Path("wssstatus")
	@GET
	public Response getStatus() {
        RequestInfo ri = RequestInfo.createInstance(sw, uriInfo, request, requestHeaders);

        StringBuilder sb = new StringBuilder();
		sb.append("<HTML><BODY>");
		sb.append("<TABLE BORDER=2 cellpadding='2' style='width: 600px'>");
		sb.append("<col style='width: 30%' />");
		sb.append("<TR><TH COLSPAN=\"2\">Servlet Environment</TH></TR>");
		sb.append("<TR><TD>").append("URL").append("</TD><TD>")
              .append(request.getRequestURI())
              .append("</TD></TR>");

        sb.append("<TR><TD>").append("Web Service Shell Version").append("</TD><TD>")
              .append(sw.appConfig.getWssVersion()).append("</TD></TR>");

		sb.append("<TR><TD>" + "Host" + "</TD><TD>")
              .append(WebUtils.getHost(request))
              .append("</TD></TR>");
		sb.append("<TR><TD>" + "Port" + "</TD><TD>")
              .append(WebUtils.getPort(request))
              .append("</TD></TR>");
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

        Map<String, String> headersMap = new HashMap<>();
        Util.updateWithCORSHeadersIfConfigured(ri, headersMap);
        Util.setResponseHeaders(builder, headersMap);

		return builder.build();
	}

	private String defDoc(String htmlMarkupMsg) {
        RequestInfo ri = RequestInfo.createInstance(sw, uriInfo, request, requestHeaders);
		return "<!DOCTYPE html>"
              + "<html><head>"
              + "<style>"
              + "#wssinfo {"
//              + "font-size: small;"
//              + "background-color: #d0f0d0;"
              + "display: inline-block;"
              + "position: absolute;"
              + "top: 70px;"
              + "left: 10px;"
              + "}"
              + "#message {"
//              + "background-color: #f0d0d0;"
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
              + "&nbsp&nbsp<b>version:</b> " + ri.appConfig.getAppVersion() + "</div>"
              + "</div>"

              + "<div id=\"message\">"
              + htmlMarkupMsg
              + "</div>"

              + "</div>"
              + "</body></html>";
	}

    @GET
    public Response rootpath() {
        RequestInfo ri = RequestInfo.createInstance(sw, uriInfo, request, requestHeaders);
        ri.statsKeeper.logGet();

    	String docUrl = ri.appConfig.getRootServiceDoc();
    	
    	// Dump some default text if no good Service Doc Config
    	if (!AppConfigurator.isOkString(docUrl)) {
            String htmlMarkup ="<div>The <b>rootServiceDoc</b> parameter is not"
                  + " set in the service configuration file.</div>"
                  + "<div>You can configure documentation for this service by using"
                  + " the <b>rootServiceDoc</b> parameter in the *-service.cfg"
                  + " file.</div>";
            ResponseBuilder builder = Response.status(Status.OK)
                  .entity(defDoc(htmlMarkup))
                  .type("text/html");

            Map<String, String> headersMap = new HashMap<>();
            Util.updateWithCORSHeadersIfConfigured(ri, headersMap);
            Util.setResponseHeaders(builder, headersMap);

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

            Map<String, String> headersMap = new HashMap<>();
            Util.updateWithCORSHeadersIfConfigured(ri, headersMap);
            Util.setResponseHeaders(builder, headersMap);

            return builder.build();
    	}
    	
    	final BufferedReader br = new BufferedReader( new InputStreamReader( is));

    	StreamingOutput so = new StreamingOutput() {
    		@Override
			public void write(OutputStream outputStream) throws IOException, WebApplicationException {
    			BufferedWriter writer = new BufferedWriter (new OutputStreamWriter(outputStream));
    			String inputLine = null ;
    			while ((inputLine = br.readLine()) != null) {
                    inputLine = inputLine.replace("WSSBASEURL",
                          Util.getWssFileNameBase(context.getContextPath()));
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

        Map<String, String> headersMap = new HashMap<>();
        Util.updateWithCORSHeadersIfConfigured(ri, headersMap);
        Util.setResponseHeaders(builder, headersMap);

		return builder.build();
   }
    
    @Path("wssversion")
	@GET @Produces("text/plain")
	public Response getWssVersion() throws IOException {
        RequestInfo ri = RequestInfo.createInstance(sw, uriInfo, request, requestHeaders);
        ri.statsKeeper.logGet();

        ResponseBuilder builder = Response.status(Status.OK)
              .type(MediaType.TEXT_PLAIN)
              .entity(AppConfigurator.wssVersion);

        Map<String, String> headersMap = new HashMap<>();
        Util.updateWithCORSHeadersIfConfigured(ri, headersMap);
        Util.setResponseHeaders(builder, headersMap);

		return builder.build();
	}
	
	@Path("version")
	@GET @Produces("text/plain")
	public Response getAppVersion() {
    	RequestInfo ri = RequestInfo.createInstance(sw, uriInfo, request, requestHeaders);
        ri.statsKeeper.logGet();

        ResponseBuilder builder = Response.status(Status.OK)
              .type(MediaType.TEXT_PLAIN)
              .entity(ri.appConfig.getAppVersion());

        Map<String, String> headersMap = new HashMap<>();
        Util.updateWithCORSHeadersIfConfigured(ri, headersMap);
        Util.setResponseHeaders(builder, headersMap);

		return builder.build();
	}
	
	@Path("whoami")
	@GET @Produces("text/plain")
	public Response getwho() {
        RequestInfo ri = RequestInfo.createInstance(sw, uriInfo, request, requestHeaders);
        ri.statsKeeper.logGet();

        ResponseBuilder builder = Response.status(Status.OK)
              .type(MediaType.TEXT_PLAIN)
              .entity(request.getRemoteAddr());

        Map<String, String> headersMap = new HashMap<>();
        Util.updateWithCORSHeadersIfConfigured(ri, headersMap);
        Util.setResponseHeaders(builder, headersMap);

		return builder.build();
	}
}
