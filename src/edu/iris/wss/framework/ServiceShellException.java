package edu.iris.wss.framework;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;

import edu.iris.wss.utils.LoggerUtils;
import edu.iris.wss.utils.WebUtils;


public class ServiceShellException extends WebApplicationException {
	
	private static final long serialVersionUID = 1L;
	public static final Logger logger = Logger.getLogger(ServiceShellException.class);
	public static final Logger usageLogger = Logger.getLogger("UsageLogger");

    public ServiceShellException(final Status status, final String message) {
        super(Response.status(status).
          entity(message + "\n").type("text/plain").build());
    }
    
    public ServiceShellException(WebApplicationException wae, String message) {
    	super(Response.status(wae.getResponse().getStatus()).
    	          entity(message + "\n").type("text/plain").build());
    }

    public static void logAndThrowException(RequestInfo ri, WebApplicationException wae, String message) {
        logAndThrowException(ri, Status.fromStatusCode(wae.getResponse().getStatus()), message, null);
    }
    
    public static void logAndThrowException(RequestInfo ri, Status status, String message) {
       logAndThrowException(ri, status, message, null);
    }

    public static void logAndThrowException(RequestInfo ri, Status status, String message, Exception e) {
   		logger.error(message + ": " + getErrorString(e));
   		LoggerUtils.logMessage(ri, 0L, 0L, message, status.getStatusCode(), null);
		
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd kk:mm:ss z");
    	StringBuilder sb = new StringBuilder();
    	sb.append("Error " + status.getStatusCode());
    	sb.append(": " + message);
    	if (e != null) sb.append("\n" + getErrorString(e));
    	
    	sb.append("\n\n" + "Request:\n");
    	sb.append(ri.request.getRequestURL());
    	String qs = ri.request.getQueryString();
    	if ((qs != null) && (!qs.equals("")))
    		sb.append("?" + qs);
    	
    	sb.append("\n\n" + "Request Submitted:\n");
    	sb.append(sdf.format(new Date()));
    	
   	
    	sb.append("\n\nService version:\n");
    	sb.append(ri.appConfig.getAppName() + ": v " + ri.appConfig.getVersion() + "\n");
    	
    	sb.append(WebUtils.getCrazyHostPort(ri.request));
//    	logger.error(sb.toString());
    	throw new ServiceShellException(status, sb.toString());
    }
    
    public static String getErrorString(Throwable e) {
    	StringBuilder sb = new StringBuilder();
    	
    	if (e == null)  return "";
    	
    	if (e.getMessage() != null) {
    		sb.append(e.getMessage() + "\n");
    	}

		if (e.getCause() != null) {
			sb.append(getErrorString(e.getCause()));
		}
		return sb.toString();
    }
}
