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

package edu.iris.wss.utils;

import edu.iris.dmc.jms.WebUsageItem;
import edu.iris.dmc.jms.service.WebLogService;
import edu.iris.wss.framework.AppConfigurator;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.iris.wss.framework.RequestInfo;
import edu.iris.wss.framework.AppConfigurator.LoggingMethod;
import edu.iris.wss.framework.WssSingleton;

public class LoggerUtils {


	public static final Logger logger = Logger.getLogger(LoggerUtils.class);
	public static final Logger usageLogger = Logger.getLogger("UsageLogger");
	
    /**
     * ERROR usage logging
     * 
     */
	public static void logWssUsageError(RequestInfo ri, String appSuffix,
			Long dataSize, Long processTime,
			String errorType, Integer httpStatusCode, String extraText) {

        WebUsageItem wui = new WebUsageItem();

        wui.setApplication(makeFullAppName(ri, appSuffix));
        wui.setHost(WebUtils.getHostname());
        wui.setAccessDate(new Date());
        wui.setClientName(WebUtils.getClientName(ri.request));
        wui.setClientIP(WebUtils.getClientIp(ri.request));
        wui.setDataSize(dataSize);
        wui.setProcessTime(processTime);
        wui.setNetwork(null);
        wui.setStation(null);
        wui.setChannel(null);
        wui.setLocation(null);
        wui.setQuality(null);
        wui.setStartTime(null);
        wui.setEndTime(null);
        wui.setErrorType(errorType);
        wui.setUserAgent(WebUtils.getUserAgent(ri.request));
        wui.setHttpStatus(httpStatusCode);
        wui.setUserName(WebUtils.getAuthenticatedUsername(ri.requestHeaders));
        wui.setExtra(extraText);

		logWssUsageMessage(Level.ERROR, wui, ri);
	}
	
	/**
     * INFO usage summary logging
     * 
     */
	public static void logWssUsageMessage(RequestInfo ri, String appSuffix,
			Long dataSize, Long processTime,
			String errorType, Integer httpStatusCode, String extraText) {

        WebUsageItem wui = new WebUsageItem();

        wui.setApplication(makeFullAppName(ri, appSuffix));
        wui.setHost(WebUtils.getHostname());
        wui.setAccessDate(new Date());
        wui.setClientName(WebUtils.getClientName(ri.request));
        wui.setClientIP(WebUtils.getClientIp(ri.request));
        wui.setDataSize(dataSize);
        wui.setProcessTime(processTime);
        wui.setNetwork(null);
        wui.setStation(null);
        wui.setChannel(null);
        wui.setLocation(null);
        wui.setQuality(null);
        wui.setStartTime(null);
        wui.setEndTime(null);
        wui.setErrorType(errorType);
        wui.setUserAgent(WebUtils.getUserAgent(ri.request));
        wui.setHttpStatus(httpStatusCode);
        wui.setUserName(WebUtils.getAuthenticatedUsername(ri.requestHeaders));
        wui.setExtra(extraText);
        
		logWssUsageMessage(Level.INFO, wui, ri);
	}
	
    /**
     * INFO usage detail logging
     *
     */
	public static void logWssUsageMessage(RequestInfo ri, 
			String appSuffix, Long dataSize, Long processTime,
			String errorType, Integer httpStatusCode, String extraText,
			String network, String station, String location, String channel, String quality,
			Date startTime, Date endTime) {

        WebUsageItem wui = new WebUsageItem();

        wui.setApplication(makeFullAppName(ri, appSuffix));
        wui.setHost(WebUtils.getHostname());
        wui.setAccessDate(new Date());
        wui.setClientName(WebUtils.getClientName(ri.request));
        wui.setClientIP(WebUtils.getClientIp(ri.request));
        wui.setDataSize(dataSize);
        wui.setProcessTime(processTime);
        wui.setNetwork(network);
        wui.setStation(station);
        wui.setChannel(channel);
        wui.setLocation(location);
        wui.setQuality(quality);
        wui.setStartTime(startTime);
        wui.setEndTime(endTime);
        wui.setErrorType(errorType);
        wui.setUserAgent(WebUtils.getUserAgent(ri.request));
        wui.setHttpStatus(httpStatusCode);
        wui.setUserName(WebUtils.getAuthenticatedUsername(ri.requestHeaders));
        wui.setExtra(extraText);
        
		logWssUsageMessage(Level.INFO, wui, ri);
	}
	
	public static void logWssUsageMessage(Level level, WebUsageItem wui, RequestInfo ri) {
		AppConfigurator.LoggingMethod loggingType = ri.appConfig.getLoggingType();
        
		if (loggingType == LoggingMethod.LOG4J) {
            String msg = makeUsageLogString(wui);
            
			switch (level.toInt()) {
			case Level.ERROR_INT:
				usageLogger.error(msg);
				break;
			case Level.INFO_INT:
				usageLogger.info(msg);
				break;		
			default:
				usageLogger.debug(msg);
				break;	
			}
		} else if (loggingType == LoggingMethod.JMS) {
            // for now, webLogService startup is here as in the original code.
            // It could be placed in AppContextListener, or possibly
            // MyApplication. If placed in AppContextListener, the
            // startup timing between components needs to be checked.
            try {
                if (WssSingleton.webLogService == null) {
                    WssSingleton.webLogService = new WebLogService();
                    try {
                        WssSingleton.webLogService.init();
                        logger.info("webLogService init succeeded");
                    } catch (Exception ex) {
                        System.out.println("webLogService init exception: "
                                + ex + "  msg: " + ex.getMessage());
                        logger.error("webLogService init exception: ", ex);
                    }
                }
                
//                // check output
//                System.out.println("*** logWssUsageMessage \n"
//                    + getUsageLogHeader() + "\n"
//                    + makeUsageLogString(wui)
//                );
                
                WssSingleton.webLogService.send(wui);

			} catch (Exception ex) {
				logger.error("Error while logging via JMS ex: " + ex
                        + "  msg: " + ex.getMessage());
                logger.error("Error while logging via JMS stack:", ex);
			}
		} else {
            logger.error("Error, unexpected loggingType configuration value: "
                    + loggingType + "  msg: " + makeUsageLogString(wui));
        }
	}
	
    public static String makeFullAppName(RequestInfo ri, String appSuffix) {
        String fullAppName = ri.appConfig.getAppName();
        if (appSuffix != null) {
            fullAppName += appSuffix;
        }
        
        return fullAppName;
    }

	public static String makeUsageLogString(WebUsageItem wui) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        System.out.println("---------------------------------- timezone: " + sdf.getTimeZone());
        System.out.println("---------------------------------- sys props user.timezone: " + System.getProperty("user.timezone"));

		StringBuffer sb = new StringBuffer();
		
        // note, keep in the same order as getUsageLogHeader
		append(sb, wui.getApplication());
		append(sb, wui.getHost());
        if (wui.getAccessDate() != null) {
            append(sb, sdf.format(wui.getAccessDate()));
        } else {
            sb.append("|");
        }
		append(sb, wui.getClientName());
		append(sb, wui.getClientIP());
		append(sb, wui.getDataSize().toString());
		append(sb, wui.getProcessTime().toString());
		
		append(sb, wui.getErrorType());
		append(sb, wui.getUserAgent());
		append(sb, Integer.toString(wui.getHttpStatus()));
		append(sb, wui.getUserName());
		
		append(sb, wui.getNetwork());
		append(sb, wui.getStation());
		append(sb, wui.getChannel());
		append(sb, wui.getLocation());
		append(sb, wui.getQuality());
		if (wui.getStartTime() != null) {
			append(sb, sdf.format(wui.getStartTime()));
        } else {
            sb.append("|");
        }
		if (wui.getEndTime() != null) {
			append(sb, sdf.format(wui.getEndTime()));
        } else {
            sb.append("|");
        }
		append(sb, wui.getExtra());

		return sb.toString();
	}
	
	public static String getUsageLogHeader() {
		StringBuffer sb = new StringBuffer();
		sb.append("# ");
		append(sb, "Application");
		append(sb, "Host Name");
		append(sb, "Access Date");
		append(sb, "Client Name");
		append(sb, "Client IP");
		append(sb, "Data Length");
		append(sb, "Processing Time (ms)");
		
		append(sb, "Error Type");
		append(sb, "User Agent");
		append(sb, "HTTP Status");
		append(sb, "User");
		
		append(sb, "Network");
		append(sb, "Station");
		append(sb, "Channel");
		append(sb, "Location");
		append(sb, "Quality");

		append(sb, "Start Time");		
		append(sb, "End Time");
		
		append(sb, "Extra");

		return sb.toString();
	}
	
	private static void append(StringBuffer sb, String s) {
		if (AppConfigurator.isOkString(s)) 
			sb.append(s);
		sb.append("|");
	}
}
