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
import edu.iris.dmc.logging.usage.WSUsageItem;
import edu.iris.wss.framework.AppConfigurator;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.iris.wss.framework.RequestInfo;
import edu.iris.wss.framework.AppConfigurator.LoggingMethod;
import edu.iris.wss.framework.Util;
import edu.iris.wss.framework.WssSingleton;

public class LoggerUtils {


	public static final Logger logger = Logger.getLogger(LoggerUtils.class);
	public static final Logger usageLogger = Logger.getLogger("UsageLogger");

	/**
     * Create and send usage message. The items with nulls are for
     * Miniseed channel information and are not needed here.
     *
     * The level passed in, e.g. ERROR for error messages and INFO for
     * messages is used by log4j.
     *
     * For values set to null, expecting the logging system to leave those
     * respective fields out of the delivered message
     */
    public static void logUsageMessage(RequestInfo ri, String appSuffix,
            Long dataSize, Long processTime,
            String errorType, Integer httpStatusCode, String extraText,
            Level level) {

        WSUsageItem wsuRabbit = new WSUsageItem();

        wsuRabbit.setMessagetype("usage");

        // note: application is now a simple pass through, it no longer
        //       appends a suffix, as in old code, e.g.
        //       wui.setApplication(makeFullAppName(ri, appSuffix));
        wsuRabbit.setApplication(    ri.appConfig.getAppName());
        // however, until feature is not needed, make the old application
        // name available (for JMS)
        String olderJMSApplciationName = makeFullAppName(ri, appSuffix);

        wsuRabbit.setHost(           WebUtils.getHostname());
        wsuRabbit.setAccessDate(     new Date());
        wsuRabbit.setClientName(     WebUtils.getClientName(ri.request));
        wsuRabbit.setClientIp(       WebUtils.getClientIp(ri.request));
        wsuRabbit.setDataSize(       dataSize);
        wsuRabbit.setProcessTimeMsec(processTime);
        wsuRabbit.setNetwork(        null);
        wsuRabbit.setStation(        null);
        wsuRabbit.setChannel(        null);
        wsuRabbit.setLocation(       null);
        wsuRabbit.setQuality(        null);
        wsuRabbit.setStartTime(      null);
        wsuRabbit.setEndTime(        null);
        wsuRabbit.setErrorType(      errorType);
        wsuRabbit.setUserAgent(      WebUtils.getUserAgent(ri.request));
        wsuRabbit.setHttpCode(       httpStatusCode);
        wsuRabbit.setUserName(       WebUtils.getAuthenticatedUsername(ri.requestHeaders));
        wsuRabbit.setExtra(          extraText);

		logWssUsageMessage(level, wsuRabbit, ri, olderJMSApplciationName);
	}
	
    /**
     * Create and send message for Miniseed channel information, it is
     * determined by media type of a request or default configuration.
     *
     * sets message type to wfstat as defined for downstream consumers
     *
     * appSuffix ignored
     *
     */
	public static void logWfstatMessage(RequestInfo ri, 
			String appSuffix, Long dataSize, Long processTime,
			String errorType, Integer httpStatusCode, String extraText,
			String network, String station, String location, String channel, String quality,
			Date startTime, Date endTime) {

        WSUsageItem wsuRabbit = new WSUsageItem();

        wsuRabbit.setMessagetype("wfstat");

        // note: application is now a simple pass through, it no longer
        //       appends a suffix, as in old code, e.g.
        //       wui.setApplication(makeFullAppName(ri, appSuffix));
        wsuRabbit.setApplication(    ri.appConfig.getAppName());
        // however, until feature is not needed, make the old application
        // name available (for JMS)
        String olderJMSApplciationName = makeFullAppName(ri, appSuffix);

        wsuRabbit.setHost(           WebUtils.getHostname());
        wsuRabbit.setAccessDate(     new Date());
        wsuRabbit.setClientName(     WebUtils.getClientName(ri.request));
        wsuRabbit.setClientIp(       WebUtils.getClientIp(ri.request));
        wsuRabbit.setDataSize(       dataSize);
        wsuRabbit.setProcessTimeMsec(processTime);
        wsuRabbit.setNetwork(        network);
        wsuRabbit.setStation(        station);
        wsuRabbit.setChannel(        channel);
        wsuRabbit.setLocation(       location);
        wsuRabbit.setQuality(        quality);
        wsuRabbit.setStartTime(      startTime);
        wsuRabbit.setEndTime(        endTime);
        wsuRabbit.setErrorType(      errorType);
        wsuRabbit.setUserAgent(      WebUtils.getUserAgent(ri.request));
        wsuRabbit.setHttpCode(       httpStatusCode);
        wsuRabbit.setUserName(       WebUtils.getAuthenticatedUsername(ri.requestHeaders));
        wsuRabbit.setExtra(          extraText);
        
		logWssUsageMessage(Level.INFO, wsuRabbit, ri, olderJMSApplciationName);
	}
	
	private static void logWssUsageMessage(Level level, WSUsageItem wsuRabbit,
          RequestInfo ri, String olderJMSApplciationName) {
		AppConfigurator.LoggingMethod loggingType = ri.appConfig.getLoggingType();
        
		if (loggingType == LoggingMethod.LOG4J) {
            String msg = makeUsageLogString(wsuRabbit);
            
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

            WebUsageItem wui = new WebUsageItem();

            wui.setApplication(  olderJMSApplciationName);
            wui.setHost(         wsuRabbit.getHost());
            wui.setAccessDate(   wsuRabbit.getAccessDate());
            wui.setClientName(   wsuRabbit.getClientName());
            wui.setClientIP(     wsuRabbit.getClientIp());
            wui.setDataSize(     wsuRabbit.getDataSize());
            wui.setProcessTime(  wsuRabbit.getProcessTimeMsec());
            wui.setNetwork(      wsuRabbit.getNetwork());
            wui.setStation(      wsuRabbit.getStation());
            wui.setChannel(      wsuRabbit.getChannel());
            wui.setLocation(     wsuRabbit.getLocation());
            wui.setQuality(      wsuRabbit.getQuality());
            wui.setStartTime(    wsuRabbit.getStartTime());
            wui.setEndTime(      wsuRabbit.getEndTime());
            wui.setErrorType(    wsuRabbit.getErrorType());
            wui.setUserAgent(    wsuRabbit.getUserAgent());
            wui.setHttpStatus(   wsuRabbit.getHttpCode());
            wui.setUserName(     wsuRabbit.getUserName());
            wui.setExtra(        wsuRabbit.getExtra());

            try {
                WssSingleton.webLogService.send(wui);
			} catch (Exception ex) {
				logger.error("Error while publishing via JMS ex: " + ex
                      + "  webLogService: " + WssSingleton.webLogService
                      + "  ex msg: " + ex.getMessage()
                      + "  application: " + wui.getApplication()
                      + "  host: " + wui.getHost()
                      + "  client IP: " + wui.getClientIP()
                      + "  ErrorType: " + wui.getErrorType());

//                logger.error("Error while publishing via JMS stack:", ex);
			}

		} else if (loggingType == LoggingMethod.RABBIT_ASYNC) {
            try {
                WssSingleton.rabbitAsyncPublisher.publish(wsuRabbit);
            } catch (Exception ex) {
                logger.error("Error while publishing via RABBIT_ASYNC ex: " + ex
                      + "  rabbitAsyncPublisher: " + WssSingleton.rabbitAsyncPublisher
                      + "  msg: " + ex.getMessage()
                      + "  application: " + wsuRabbit.getApplication()
                      + "  host: " + wsuRabbit.getHost()
                      + "  client IP: " + wsuRabbit.getClientIp()
                      + "  ErrorType: " + wsuRabbit.getErrorType());

//                logger.error("Error while publishing via RABBIT_ASYNC stack:", ex);
            }

		} else {
            logger.error("Error, unexpected loggingMethod configuration value: "
                    + loggingType + "  msg: " + makeUsageLogString(wsuRabbit));
        }
	}
	
    public static String makeFullAppName(RequestInfo ri, String appSuffix) {
        String fullAppName = ri.appConfig.getAppName();
        if (appSuffix != null) {
            fullAppName += appSuffix;
        }
        
        return fullAppName;
    }

	public static String makeUsageLogString(WSUsageItem wsu) {
		
		SimpleDateFormat sdf = new SimpleDateFormat(Util.ISO_8601_ZULU_FORMAT);
        sdf.setTimeZone(Util.UTZ_TZ);

		StringBuffer sb = new StringBuffer();
		
        // note, keep in the same order as getUsageLogHeader
		append(sb, wsu.getApplication());
		append(sb, wsu.getHost());
        if (wsu.getAccessDate() != null) {
            append(sb, sdf.format(wsu.getAccessDate()));
        } else {
            sb.append("|");
        }
		append(sb, wsu.getClientName());
		append(sb, wsu.getClientIp());
		append(sb, wsu.getDataSize().toString());
		append(sb, wsu.getProcessTimeMsec().toString());
		
		append(sb, wsu.getErrorType());
		append(sb, wsu.getUserAgent());
		append(sb, Integer.toString(wsu.getHttpCode()));
		append(sb, wsu.getUserName());
		
		append(sb, wsu.getNetwork());
		append(sb, wsu.getStation());
		append(sb, wsu.getChannel());
		append(sb, wsu.getLocation());
		append(sb, wsu.getQuality());
		if (wsu.getStartTime() != null) {
			append(sb, sdf.format(wsu.getStartTime()));
        } else {
            sb.append("|");
        }
		if (wsu.getEndTime() != null) {
			append(sb, sdf.format(wsu.getEndTime()));
        } else {
            sb.append("|");
        }
		append(sb, wsu.getExtra());
        //append(sb, wsu.getMessagetype());
        // on last one, leave off the delimiter
		if (AppConfigurator.isOkString(wsu.getMessagetype()))
			sb.append(wsu.getMessagetype());

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

        //append(sb, "Message Type");
        // on last one, leave off the delimiter
		if (AppConfigurator.isOkString("Message Type"))
			sb.append("Message Type");

		return sb.toString();
	}
	
	private static void append(StringBuffer sb, String s) {
		if (AppConfigurator.isOkString(s)) 
			sb.append(s);
		sb.append("|");
	}
}
