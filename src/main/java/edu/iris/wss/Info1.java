package edu.iris.wss;


import edu.iris.wss.framework.AppConfigurator;
import edu.iris.wss.framework.FdsnStatus.Status;
import edu.iris.wss.framework.RequestInfo;
import edu.iris.wss.framework.WssSingleton;
import edu.iris.wss.framework.Util;
import java.io.IOException;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import org.apache.log4j.Logger;

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

        Util.addCORSHeadersIfConfigured(builder, ri, null);

		return builder.build();
	}

//    private void addCORSHeadersIfConfigured(ResponseBuilder rb, RequestInfo ri) {
//		if (ri.appConfig.isCorsEnabled()) {
//            // Insert CORS header elements.
//		    rb.header("Access-Control-Allow-Origin", "*");
//
//            // dont add this unless cookies are expected
////            rb.header("Access-Control-Allow-Credentials", "true");
//
//            // Not setting these at this time - 2015-08-12
////            rb.header("Access-Control-Allow-Methods", "HEAD, GET, POST");
////            rb.header("Access-Control-Allow-Headers", "Content-Type, Accept");
//
//            // not clear if needed now, 2015-08-12, but this is how to let client
//            // see what headers are available, although "...Allow-Headers" may be
//            // sufficient
////            rb.header("Access-Control-Expose-Headers", "X-mycustomheader1, X-mycustomheader2");
//		}
//    }
}