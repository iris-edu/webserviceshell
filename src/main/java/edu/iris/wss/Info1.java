package edu.iris.wss;


import edu.iris.wss.framework.AppConfigurator;
import edu.iris.wss.framework.FdsnStatus.Status;
import edu.iris.wss.framework.RequestInfo;
import edu.iris.wss.framework.WssSingleton;
import edu.iris.wss.framework.Util;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import org.apache.log4j.Logger;

/**
 *
 * @author mike
 *
 * Keeping this class around to illustrate a simple way to add functionality
 * using the addEndpoint method in MyApplication.java.
 *
 * @Context variables are populated with every request. The remainder of the
 * of the class can be pojo methods that are configured by addEndpoint.
 */
public class Info1 {
	@Context 	ServletContext context;
	@Context	javax.servlet.http.HttpServletRequest request;
    @Context 	UriInfo uriInfo;	
    @Context 	HttpHeaders requestHeaders;

    @Context 	WssSingleton sw;
    
	public static final Logger logger = Logger.getLogger(Info1.class);
	
	public Info1()  {
        System.out.println("***************&&& Info1 constr");
	}	
	public Response getDyWssVersion() throws IOException {
        RequestInfo ri = RequestInfo.createInstance(sw, uriInfo, request, requestHeaders);

        ResponseBuilder builder = Response.status(Status.OK)
              .type(MediaType.TEXT_PLAIN)
              .entity("Dynamiclly added, wssVersion: " + AppConfigurator.wssVersion);

        Map<String, String> headersMap = new HashMap<>();
        Util.updateWithCORSHeadersIfConfigured(ri, headersMap);
        Util.setResponseHeaders(builder, headersMap);

		return builder.build();
	}
}