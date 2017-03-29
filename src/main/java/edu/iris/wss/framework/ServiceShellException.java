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

package edu.iris.wss.framework;

import java.text.SimpleDateFormat;
import java.util.Date;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import edu.iris.wss.framework.FdsnStatus.Status;
import org.apache.log4j.Logger;
import edu.iris.wss.utils.LoggerUtils;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Level;

public class ServiceShellException extends WebApplicationException {

	private static final long serialVersionUID = 1L;
	public static final Logger logger = Logger.getLogger(ServiceShellException.class);
	public static final Logger usageLogger = Logger.getLogger("UsageLogger");
    public static final String usageDetailsSignature = "Usage Details ...";

    public ServiceShellException(final Status status, final String message,
          RequestInfo ri) {
        // Note: when running in junit with Grizzley server, the content in
        //       this message is not passed threw to the client
        //
        // In earlier versions of Jersey 1.x, a status code of 204 plus
        // content in errMsg would cause Jersey to convert the 204 to a 200
        // Now in Jersey 2.x, the errMsg is ignored and 204 remains a 204,
        // which is the desired behaviour
        super(new Throwable("throwable - " + message),
              preConstructorReponse(status, message, ri));
    }

    private static Response preConstructorReponse(final Status status,
          final String message, RequestInfo ri) {
        // Note: when running in junit with Grizzley server, the content in
        //       message is not passed through to the client

        Response.ResponseBuilder builder = Response.status(status).
           entity(message + "\n").type("text/plain");

        Map<String, String> headersMap = new HashMap<>();
        Util.updateWithCORSHeadersIfConfigured(ri, headersMap);
        Util.setResponseHeaders(builder, headersMap);

        Response response= builder.build();

        return response;
    }


    public static void logAndThrowException(RequestInfo ri, Status status, String message) {
    	if (message != null)
    		logAndThrowException(ri, status, message, null);
        else {
            // This probably should not happen, it implies a programmer error
            // either in the a WSS call sequence or from endpoint code created
            // external to WSS
            //
            // make a new message and explain the problem
            Thread.dumpStack();
            String newMsg = "*** note, WSS warning, a possible programing error"
                  + " has occurred - a null error message was received, along"
                  +  "with Status: " + status;
            logAndThrowException(ri, status, newMsg, null);
        }
    }

    public static void logAndThrowException(RequestInfo ri, Status status,
          String briefMsg, String detailedMsg) {

    	ri.statsKeeper.logError();

        // checking for null here as well as in createFdsnErrorMsg so the
        // logger message can be informed
        if (briefMsg == null) {
            briefMsg = "null message, possible coding error";
        }

////    	logger.error(briefMsg + getErrorString(e));
        logger.error(briefMsg + "  detailed: " + detailedMsg);

        LoggerUtils.logUsageMessage(ri, null, 0L, 0L, briefMsg,
              status.getStatusCode(), ri.getEndpointNameForThisRequest(),
              Level.ERROR);

        String errMsg = createFdsnErrorMsg(status, briefMsg, detailedMsg,
          ri.request.getRequestURL().toString(), ri.request.getQueryString(),
          ri.appConfig.getAppName(), ri.appConfig.getAppVersion());


        // In earlier versions of Jersey 1.x, a status code of 204 plus
        // content in errMsg would cause Jersey to convert the 204 to a 200
        // Now in Jersey 2.x, the errMsg is ignored and 204 remains a 204,
        // which is the desired behaviour
        throw new ServiceShellException(status, errMsg, ri);
    }

    public static String createFdsnErrorMsg(Status status, String briefMsg,
          String detailMsg, String requestURL, String requestQueryString,
          String appName, String appVersion) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd kk:mm:ss z");
        sdf.setTimeZone(Util.UTZ_TZ);
        StringBuilder sb = new StringBuilder();
        sb.append("Error ").append(status.getStatusCode());

        if (briefMsg == null) {
            briefMsg = "null message from caller, probable error in code";
        }
        int index = briefMsg.indexOf(usageDetailsSignature);
        if (index == -1) {
            sb.append(": ").append(briefMsg);
        } else {
            sb.append(": ").append(briefMsg.substring(0, index));
        }

        if (Util.isOkString(detailMsg)) {
        sb.append("\n\n")
              .append("More Details:\n")
              .append(detailMsg);
        }

        sb.append("\n\n")
              .append("Request:\n")
              .append(requestURL);
        if (Util.isOkString(requestQueryString)) {
            sb.append("?" + requestQueryString);
        }

        sb.append("\n\n")
              .append("Request Submitted:\n")
              .append(sdf.format(new Date()));

        sb.append("\n\n")
              .append("Service version:\n")
              .append("Service: ")
              .append(appName)
              .append("  version: ")
              .append(appVersion)
              .append("\n");

        return sb.toString();
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
