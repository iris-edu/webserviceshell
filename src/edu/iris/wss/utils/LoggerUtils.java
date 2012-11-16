package edu.iris.wss.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import edu.iris.StatsWriter.WsStatsWriter;
import edu.iris.wss.framework.RequestInfo;
import edu.iris.wss.framework.AppConfigurator.LoggingType;

public class LoggerUtils {
	public static final Logger logger = Logger.getLogger(LoggerUtils.class);
	public static final Logger usageLogger = Logger.getLogger("UsageLogger");
	
	public static void logMessage(RequestInfo ri, Long dataSize, Long processTime,
			String errorType, Integer httpStatusCode, String extraText) {
		
		if (ri.appConfig.getLoggingType() == LoggingType.LOG4J) {
			usageLogger.info(makeUsageLogString(ri, dataSize, processTime,
					errorType, httpStatusCode, extraText));
			return;
		}
		
		if (ri.appConfig.getLoggingType() == LoggingType.JMS)
			try {
				WsStatsWriter wsw = new WsStatsWriter(
						ri.appConfig.getConnectionFactory(),
						ri.appConfig.getTopicDestination(),
						ri.appConfig.getJndiUrl());

				wsw.sendUsageMessage(
					ri.appConfig.getAppName(), 
					WebUtils.getHostname(),
					new Date(),  // Access time
					WebUtils.getClientName(ri.request),
					WebUtils.getClientIp(ri.request), 
					dataSize, 
					processTime,
					null, null, null, null, null, // NSCLQ 
					null, null, null, // Start time, end time, duration
					errorType, // Error Type
					WebUtils.getUserAgent(ri.request),
					Integer.toString(httpStatusCode),
					WebUtils.getAuthenticatedUsername(ri.requestHeaders),
					extraText);
			

			} catch (Exception e) {
				logger.error("Error while logging via JMS: " + e.getMessage());
			}
	}
	
	public static String makeUsageLogString(RequestInfo ri,  
			long dataLength, long processTime,
			String errorType, Integer httpStatus, String extra) {
	
//		ri.paramConfig.dump();
		String lat= ri.paramConfig.getValue("-lat");
		if (lat  != null) 
			logger.info("lat: "+ lat);
		
		return makeUsageLogString(ri, dataLength, processTime,
				errorType, httpStatus, extra,
				null, null, null, null, null, 
				null, null, null);
	}
	
	public static String makeUsageLogString(RequestInfo ri, 
			Long dataLength, Long processTime,
			String errorType, Integer httpStatus, String extra,
			String network, String station, String channel, String location, String quality,
			Date startTime, Date endTime, String duration) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

		StringBuffer sb = new StringBuffer();
		append(sb, ri.appConfig.getAppName());
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
