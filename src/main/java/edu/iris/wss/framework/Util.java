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

import static edu.iris.wss.framework.WssSingleton.ACCESS_CONTROL_ALLOW_ORIGIN;
import static edu.iris.wss.framework.WssSingleton.CONTENT_DISPOSITION;
import edu.iris.wss.utils.LoggerUtils;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;
import javax.ws.rs.core.Response;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 * @author mike
 */
public class Util {
    public static final String WSS_OS_CONFIG_DIR = "wssConfigDir";
    private static final String LOG4J_CFG_NAME_SUFFIX = "-log4j.properties";

    private static final SimpleDateFormat ISO_8601_ZULU_FORMAT =
          new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    static {
        ISO_8601_ZULU_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

	public static boolean isOkString(String s) {
		return ((s != null) && !s.isEmpty());
	}

    public static String getWssFileNameBase(String serveletContextPath) {
        return serveletContextPath
              .replaceFirst(Pattern.quote("/"), "")
              .replaceAll(Pattern.quote("/"), ".");
    }

    public static String getUTCISO8601() {
        return ISO_8601_ZULU_FORMAT.format((new GregorianCalendar()).getTime());
    }

    public static void myNewInitLog4j(String configBase) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        String now = fmt.format((new GregorianCalendar()).getTime());

        String configDirName = System.getProperty(WSS_OS_CONFIG_DIR);
        System.out.println(now + " Info, myNewInitLog4j, property "
              + WSS_OS_CONFIG_DIR + ": " + configDirName);

        if (configDirName == null) {
            System.out.println(now + " *** Warning, myNewInitLog4j - system property, "
                  + WSS_OS_CONFIG_DIR + " is not found, log4j is not initialize here");
            System.out.println(now + " *** Warning, myNewInitLog4j - for tomcat, messages"
                  + " may be in files logs/wss.log and logs/wss_usage.log");
            return;
        }

        File configDir = new File(configDirName);

        if (!configDir.isDirectory()) {
            System.out.println(now + " *** Warning, myNewInitLog4j ,wssConfigDir path: "
                + configDir.getAbsolutePath() + " does not exist");
        }

        String fileName = configBase + LOG4J_CFG_NAME_SUFFIX;
        System.out.println(now + " Info, myNewInitLog4j, filename: " + fileName);

        File file = new File(configDir, fileName);

        if( !file.exists() ) {
            System.out.println(now + " *** Warning, myNewInitLog4j, configBase: " + configBase
                + " unable to locate log4j file: " + file.getAbsolutePath()
                + " check for logs/wss.log and logs/wss_usage.log");
            return;
        }
        System.out.println(now + " Info, myNewInitLog4j, configBase: " + configBase
            + "  log4j file: " + file.getAbsolutePath());

        PropertyConfigurator.configure(file.getAbsolutePath());
    }

    public static void updateWithCORSHeadersIfConfigured(RequestInfo ri, Map<String,
          String> headers) {
        if (ri.appConfig.isCorsEnabled()) {
            headers.put(ACCESS_CONTROL_ALLOW_ORIGIN.toLowerCase(), "*");

//            // some references show using some of the following, however...
//
//            // dont add this unless cookies are expected
//            headers.put("Access-Control-Allow-Credentials", "true");

//            // Not setting these at this time - 2015-08-12
//            headers.put("Access-Control-Allow-Methods", "HEAD, GET, POST");
//            headers.put("Access-Control-Allow-Headers", "Content-Type, Accept");

//            // not clear if needed now, 2015-08-12, but this is how to let client
//            // see what headers are available, although "...Allow-Headers" may be
//            // sufficient
//            headers.put("Access-Control-Expose-Headers",
//                  "X-mycustomheader1, X-mycustomheader2");
		}
    }
    
    public static void setResponseHeaders(Response.ResponseBuilder rb,
          Map<String, String> headersIn) {
        if (headersIn != null) {
            for (String headerKey : headersIn.keySet()) {
                rb.header(headerKey, headersIn.get(headerKey));
            }
        }
    }

	public static void logAndThrowException(RequestInfo ri,
          FdsnStatus.Status status, String message) {
		ServiceShellException.logAndThrowException(ri, status, message);       
	}
	
////	public static void newerShellException(FdsnStatus.Status status, RequestInfo ri,
////            IrisStreamingOutput iso) {
////		ServiceShellException.logAndThrowException(ri, status,
////                status.toString() + ". " + iso.getErrorString());
////	}

    public static FdsnStatus.Status adjustByCfg(FdsnStatus.Status trialStatus,
          RequestInfo ri) {
        if (trialStatus == FdsnStatus.Status.NO_CONTENT) {
            // override 204 if configured to do so
            if (ri.perRequestUse404for204) {
                return FdsnStatus.Status.NOT_FOUND;
            }
        }
        return trialStatus;
    }    

////	public static void newerShellException(FdsnStatus.Status status,
////          RequestInfo ri, IrisProcessor iso) {
////		ServiceShellException.logAndThrowException(ri, status,
////                status.toString() + ". " + iso.getErrorString());
////	}
	public static void logAndThrowException(RequestInfo ri,
          FdsnStatus.Status httpStatus, String briefMsg, String detailedMsg) {
		ServiceShellException.logAndThrowException(ri, httpStatus, briefMsg,
              detailedMsg);
	}

    public static void logUsageMessage(RequestInfo ri, String appSuffix,
          Long dataSize, Long processTime,
          String errorType, FdsnStatus.Status httpStatus, String extraText,
          String network, String station, String location, String channel,
          String quality, Date startTime, Date endTime, String duration) {

        LoggerUtils.logWssUsageMessage(ri, appSuffix, dataSize, processTime,
              errorType, httpStatus.getStatusCode(), extraText,
              network, station, location, channel, quality,
              startTime, endTime);
    }

	public static void logUsageMessage(RequestInfo ri, String appSuffix,
			Long dataSize, Long processTime,
			String errorType, FdsnStatus.Status httpStatus, String extraText) {

		LoggerUtils.logWssUsageMessage(ri, appSuffix, dataSize, processTime,
			errorType, httpStatus.getStatusCode(), extraText);
	}
}
