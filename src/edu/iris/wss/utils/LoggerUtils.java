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

package edu.iris.wss.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.iris.StatsWriter.WsStatsWriter;
import edu.iris.wss.framework.RequestInfo;
import edu.iris.wss.framework.AppConfigurator.LoggingType;

public class LoggerUtils {


	public static final Logger logger = Logger.getLogger(LoggerUtils.class);
	public static final Logger usageLogger = Logger.getLogger("UsageLogger");
	
	public static void logWssUsageError(RequestInfo ri, String appSuffix,
			Long dataSize, Long processTime,
			String errorType, Integer httpStatusCode, String extraText) {

		logWssUsageMessage(Level.ERROR, ri, 
				appSuffix, dataSize, processTime,
				errorType, httpStatusCode, extraText,
				null, null, null, null, null,
				null, null, null);
	}
	
	// Usage helper functions for 'INFO' level messages.  I.e. normal ones.	
	
	public static void logWssUsageMessage(RequestInfo ri, String appSuffix,
			Long dataSize, Long processTime,
			String errorType, Integer httpStatusCode, String extraText) {
		logWssUsageMessage(ri, appSuffix, dataSize, processTime, errorType, httpStatusCode, extraText,
				null, null, null, null, null, null, null, null);
	}
	
	public static void logWssUsageMessage(RequestInfo ri, 
			String appSuffix, Long dataSize, Long processTime,
			String errorType, Integer httpStatusCode, String extraText,
			String network, String station, String location, String channel, String quality,
			Date startTime, Date endTime, String duration) {
		logWssUsageMessage(Level.INFO, ri, 
				appSuffix, dataSize, processTime,
				errorType, httpStatusCode, extraText,
				network, station, location, channel, quality,
				startTime, endTime, duration);
	}
	
		
	public static void logWssUsageMessage(Level level, RequestInfo ri, String appSuffix,
			Long dataSize, Long processTime,
			String errorType, Integer httpStatusCode, String extraText,
			String network, String station, String location, String channel, String quality,
			Date startTime, Date endTime, String duration) {
		
		if (ri.appConfig.getLoggingType() == LoggingType.LOG4J) {
			switch (level.toInt()) {
			case Level.ERROR_INT:
				usageLogger.error(makeUsageLogString(ri, appSuffix, 
						dataSize, processTime,
						errorType, httpStatusCode, extraText,
						network, station, location, channel, quality,
						startTime, endTime, duration));
				break;
			case Level.INFO_INT:
				usageLogger.info(makeUsageLogString(ri, appSuffix, 
						dataSize, processTime,
						errorType, httpStatusCode, extraText,
						network, station, location, channel, quality,
						startTime, endTime, duration));
				break;		
			default:
				usageLogger.debug(makeUsageLogString(ri, appSuffix, 
						dataSize, processTime,
						errorType, httpStatusCode, extraText,
						network, station, location, channel, quality,
						startTime, endTime, duration));
				break;	
			}

			return;
		}
		
		if (ri.appConfig.getLoggingType() == LoggingType.JMS) {
			
			String fullAppName = ri.appConfig.getAppName();
			if (appSuffix != null) fullAppName += appSuffix;
			
			try {
				WsStatsWriter wsw = new WsStatsWriter(
						ri.appConfig.getConnectionFactory(),
						ri.appConfig.getTopicDestination(),
						ri.appConfig.getJndiUrl());

				wsw.sendUsageMessage(
					fullAppName, 
					WebUtils.getHostname(),
					new Date(),  // Access time
					WebUtils.getClientName(ri.request),
					WebUtils.getClientIp(ri.request), 
					dataSize, 
					processTime,
					network, station, channel, location, quality, 
					startTime, endTime, duration, 
					errorType, // Error Type
					WebUtils.getUserAgent(ri.request),
					Integer.toString(httpStatusCode),
					WebUtils.getAuthenticatedUsername(ri.requestHeaders),
					extraText);
			

			} catch (Exception e) {
				logger.error("Error while logging via JMS: " + e.getMessage());
			}
		}
	}
	
	public static String makeUsageLogString(RequestInfo ri,  String appSuffix,
			long dataLength, long processTime,
			String errorType, Integer httpStatus, String extra) {
		
		return makeUsageLogString(ri, appSuffix, 
				dataLength, processTime,
				errorType, httpStatus, extra,
				null, null, null, null, null, 
				null, null, null);
	}
	
	public static String makeUsageLogString(RequestInfo ri, String appSuffix,
			Long dataLength, Long processTime,
			String errorType, Integer httpStatus, String extra,
			String network, String station, String channel, String location, String quality,
			Date startTime, Date endTime, String duration) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

		StringBuffer sb = new StringBuffer();
		
		String fullAppName = ri.appConfig.getAppName();
		if (appSuffix != null) fullAppName += appSuffix;
		
		append(sb, fullAppName);
		append(sb, WebUtils.getHostname());
		append(sb, sdf.format(new Date()));
		append(sb, WebUtils.getClientName(ri.request));
		append(sb, WebUtils.getClientIp(ri.request));
		append(sb, dataLength.toString());
		append(sb, processTime.toString());
		
		append(sb, errorType);
		append(sb, WebUtils.getUserAgent(ri.request));
		append(sb, Integer.toString(httpStatus));
		append(sb, WebUtils.getAuthenticatedUsername(ri.requestHeaders));
		
		append(sb, network);
		append(sb, station);
		append(sb, channel);
		append(sb, location);
		append(sb, quality);
		if (startTime != null)
			append(sb, sdf.format(startTime));		
		if (endTime != null) 
			append(sb, sdf.format(endTime));
		
		append(sb, extra);

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
		if ((s != null) && !s.isEmpty()) 
			sb.append(s);
		sb.append("|");
	}
}
