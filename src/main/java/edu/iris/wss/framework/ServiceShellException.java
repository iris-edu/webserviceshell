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

import java.text.SimpleDateFormat;
import java.util.Date;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import edu.iris.wss.framework.FdsnStatus.Status;
import org.apache.log4j.Logger;
import edu.iris.wss.utils.LoggerUtils;

public class ServiceShellException extends WebApplicationException {

	private static final long serialVersionUID = 1L;
	public static final Logger logger = Logger.getLogger(ServiceShellException.class);
	public static final Logger usageLogger = Logger.getLogger("UsageLogger");
    public static final String usageDetailsSignature = "Usage Details ...";

	public ServiceShellException(final Status status) {
        // need this constructor for 204, otherwise, if entity or type is
        // used in builder, Jersey appears to convert the status to 200
		super(Response.status(status).build());
	}

    public ServiceShellException(final Status status, final String message) {
        super(new Throwable("throwable - " + message), Response.status(status).
           entity(message + "\n").type("text/plain").build());
    }

    public static void logAndThrowException(RequestInfo ri, Status status, String message) {
    	if (message != null)
    		logAndThrowException(ri, status, message, null);
        else {
            // This should not happen, it means WSS is inconsistent about setting
            // values for message earlier in the call stack
            Thread.dumpStack();
            String newMsg = "*** note, WSS warning, received unexpected null message";
            logAndThrowException(ri, status, newMsg, null);
        }
    }

    public static void logAndThrowException(RequestInfo ri, Status status,
          String message, Exception e) {

    	ri.statsKeeper.logError();

    	logger.error(message + getErrorString(e));

        LoggerUtils.logWssUsageError(ri, null, 0L, 0L, message,
              status.getStatusCode(), ri.getEndpointNameForThisRequest());

    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd kk:mm:ss z");
    	StringBuilder sb = new StringBuilder();
        sb.append("Error ").append(status.getStatusCode());

        int index = message.indexOf(usageDetailsSignature);
        if (index == -1) {
            sb.append(": ").append(message);
        } else {
            sb.append(": ").append(message.substring(0, index));
        }

        if (e != null) {
            sb.append("\n").append(getErrorString(e));
        }

        sb.append("\n\n")
              .append("Request:\n")
              .append(ri.request.getRequestURL());

        String qs = ri.request.getQueryString();

        if ((qs != null) && (!qs.equals(""))) {
            sb.append("?" + qs);
        }

        sb.append("\n\n")
              .append("Request Submitted:\n")
              .append(sdf.format(new Date()));

        sb.append("\n\n")
              .append("Service version:\n")
              .append(ri.appConfig.getAppName())
              .append(": v ")
              .append(ri.appConfig.getAppVersion())
              .append("\n");

//    	sb.append(WebUtils.getCrazyHostPort(ri.request));
//    	logger.error(sb.toString());
        if (status == status.NO_CONTENT) {
            // for 204, need different constructor with no message
            // otherwise Jersey changes 204 to 200
            throw new ServiceShellException(status);
        } else {
            throw new ServiceShellException(status, sb.toString());
        }
    }

    public static String getErrorString(Throwable e) {
        StringBuilder sb = new StringBuilder();

        if (e == null) {
            return "  exception: is null";
        }

        if (e.getMessage() != null) {
            sb.append("  exception: ")
                  .append(e.getMessage())
                  .append("\n");
        }

        if (e.getCause() != null) {
            sb.append("  exception cause: ");
            sb.append(getErrorString(e.getCause()));
        }
        return sb.toString();
    }
}
