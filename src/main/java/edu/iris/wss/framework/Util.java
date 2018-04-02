/*******************************************************************************
 * Copyright (c) 2017 IRIS DMC supported by the National Science Foundation.
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
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;
import javax.ws.rs.core.Response;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 * @author mike
 */
public class Util {
    public static final Logger LOGGER = Logger.getLogger(Util.class);

    public static final String WSS_OS_CONFIG_DIR = "wssConfigDir";
    public static final String CONFIG_FILE_SYSTEM_PROPERTY_NAME =
          "edu.iris.rabbitmq.publisher.properties.file";
    private static final String LOG4J_CFG_NAME_SUFFIX = "-log4j.properties";
    public static final String RABBITMQ_CFG_NAME_SUFFIX
          = "-rabbitconfig-publisher.properties";

    public static final String ISO_8601_ZULU_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final TimeZone UTZ_TZ = TimeZone.getTimeZone("UTC");

    public static final String MEDIA_TYPE_CONTENT_TYPE = "Content-Type";

	public static boolean isOkString(String s) {
		return ((s != null) && !s.isEmpty());
	}

    public static String getWssFileNameBase(String serveletContextPath) {
        return serveletContextPath
              .replaceFirst(Pattern.quote("/"), "")
              .replaceAll(Pattern.quote("/"), ".");
    }

    public static String getCurrentUTCTimeISO8601() {
        SimpleDateFormat sdf = new SimpleDateFormat(ISO_8601_ZULU_FORMAT);
        sdf.setTimeZone(UTZ_TZ);
        return sdf.format((new GregorianCalendar()).getTime());
    }

    public static void myNewInitLog4j(String configBase) {
        String now = getCurrentUTCTimeISO8601();

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

    /**
     * Create a file name in the WebServiceShell configuration folder.
     * NOTE: Java System property Util.WSS_OS_CONFIG_DIR must be set before
     *       calling this method
     *
     * @param configBase - must be not empty, within a running container this
     *                     is normally derived from the web service context.
     * @param cfgNameSuffix - may be empty string, within a running container
     *                        this is normally a respective config file suffix.
     * @return
     */
    public static String createCfgFileName(String configBase, String cfgNameSuffix) {
        String wssConfigDir = System.getProperty(Util.WSS_OS_CONFIG_DIR);

        String warnMsg1 = "***** check for system property "
              + Util.WSS_OS_CONFIG_DIR
              + ", value found: " + wssConfigDir;
        String warnMsg2 = "***** or check webapp name on cfg files, value found: "
            + configBase;

        String configFileName = "not initialized";
        if (isOkString(wssConfigDir) && isOkString(configBase)) {
            if (!wssConfigDir.endsWith("/")) {
                // something to handle difference between incoming value of
                // wssConfigDir between starting a server and direct calls
                // in test code
                if (!configBase.startsWith("/")) {
                    wssConfigDir += "/";
                }
            }
            configFileName = wssConfigDir + configBase
                + cfgNameSuffix;
            LOGGER.info("Generated configuration file name: "+ configFileName);
        } else {
            LOGGER.warn("***** unexpected inputs for cfg file: " + cfgNameSuffix);
            LOGGER.warn(warnMsg1);
            LOGGER.warn(warnMsg2);
        }
        return configFileName;
    }

    public static Map<String, String> createDefaultContentDisposition(
          RequestInfo ri, String epName) throws Exception {
        Map<String, String> headersMap = new HashMap<>();
        try {
            // createContentDisposition is from earlier version of WSS
            // keep it as a default
            String value = ri.createDefaultContentDisposition(epName);
            headersMap.put(CONTENT_DISPOSITION.toLowerCase(), value);
        } catch (Exception ex) {
            Util.logAndThrowException(ri, FdsnStatus.Status.INTERNAL_SERVER_ERROR,
                  "Error creating Content-Disposition header value,"
                        + " endpoint: " + epName,
                  "Error, " + ServiceShellException.getErrorString(ex));
        }
        return headersMap;
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

    /**
     * Use Configured values to update response headers.
     *
     * @param ri
     * @param headers - headers to be updated
     * @param epName - endpoint name
     * @throws java.lang.Exception
     */
    public static void updateWithEndpointHeaders(RequestInfo ri, Map<String,
          String> headers, String epName) throws Exception {
        Map<String, String> epHeaders = ri.appConfig.getEndpointHeaders(epName);

        updateWithNewHeaders(headers, epHeaders);
    }

    /**
     * Do case insensitive key match to update old with new.
     *
     * @param headers
     * @param newHeaders
     */
    public static void updateWithNewHeaders(Map<String, String> headers,
          Map<String, String> newHeaders) {

        Map<String, String> upperToOriginal = new HashMap();
        for (String key : headers.keySet()) {
            upperToOriginal.put(key.toUpperCase(), key);
        }

        String updateKey;
        for (String key : newHeaders.keySet()) {
            if (upperToOriginal.containsKey(key.toUpperCase())) {
                // key matches one in exsting headers, but use original key
                updateKey = upperToOriginal.get(key.toUpperCase());
            } else {
                // key is new
                updateKey = key;
            }
            headers.put(updateKey, newHeaders.get(key));
        }
    }

    /**
     * Do special processing for Content-Type, Do case insensitive key match.
     * Remove the Content-Type entry if it exists.
     *
     * @param headers
     */
    public static String getContentTypeValueAndRemoveKey(Map<String, String> headers) {

        String ct_key = null;
        for (String key : headers.keySet()) {
            if (key.toUpperCase().equals(Util.MEDIA_TYPE_CONTENT_TYPE.toUpperCase())) {
                ct_key = key;
                break;
            }
        }

        if (ct_key == null) {
            return null;
        } else {
            String ct_val = headers.get(ct_key);
            headers.remove(ct_key);
            return ct_val;
        }
    }

    public static void updateDispositionPerFormatType(RequestInfo ri, Map<String,
          String> headers, String epName, String formatTypeKey) throws Exception {

        String value = ri.appConfig.getDisposition(epName, formatTypeKey);

        if (null != value) {
            Map<String, String> newMap = new HashMap();
            newMap.put(CONTENT_DISPOSITION.toString(), value);
            updateWithNewHeaders(headers, newMap);
        }
        // null is ok, i.e. no update
    }

    public static void updateWithApplicationHeaders(Map<String, String> headers,
          Map<String, String> appHeaders) {

        if (null != appHeaders) {
           updateWithNewHeaders(headers, appHeaders);
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

	public static void logAndThrowException(RequestInfo ri,
          FdsnStatus.Status httpStatus, String briefMsg, String detailedMsg) {
		ServiceShellException.logAndThrowException(ri, httpStatus, briefMsg,
              detailedMsg);
	}

    public static void logWfstatMessage(RequestInfo ri, String appSuffix,
          Long dataSize, Long processTime,
          String errorType, FdsnStatus.Status httpStatus, String extraText,
          String network, String station, String location, String channel,
          String quality, Date startTime, Date endTime, String duration) {

        LoggerUtils.logWfstatMessage(ri, appSuffix, dataSize, processTime,
              errorType, httpStatus.getStatusCode(), extraText,
              network, station, location, channel, quality,
              startTime, endTime);
    }

	public static void logUsageMessage(RequestInfo ri, String appSuffix,
			Long dataSize, Long processTime,
			String errorType, FdsnStatus.Status httpStatus, String extraText) {

        LoggerUtils.logUsageMessage(ri, appSuffix, dataSize,
              processTime, errorType, httpStatus.getStatusCode(), extraText,
              Level.INFO);
	}
}
