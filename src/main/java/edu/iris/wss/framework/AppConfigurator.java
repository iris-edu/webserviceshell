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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;

public class AppConfigurator {

	public static final String wssVersion = "1.1.7-SNAPSHOT";
	public static final String wssConfigDirSignature = "wssConfigDir";

	public static final String wssDigestRealmnameSignature = "wss.digest.realmname";

	private static final String defaultConfigFileName = "META-INF/service.cfg";
	private static final String userParamConfigSuffix = "-service.cfg";

	public static final Logger logger = Logger.getLogger(AppConfigurator.class);

	private Boolean isLoaded = false;
	private Boolean isValid = false;

    // add miniseed as alias for mseed to stay consistent with FDSN standards
	public static enum OutputType {
		XML, JSON, TEXT, MSEED, MINISEED, TEXTTREE, ZIP
	};

	public static enum LoggingType {
		LOG4J, JMS
	};

	private OutputType outputType = OutputType.TEXT;

	private String rootServicePath;
	private String rootServiceDoc;
	private String wadlPath;

	private String workingDirectory = "/";
	private String handlerProgram;
	private String catalogsHandlerProgram;
	private String contributorsHandlerProgram;
	private String countsHandlerProgram;

	private String appName;
	private String version;

	private Boolean postEnabled = false;
	private Boolean use404For204 = false;
	private Boolean allowCors = false;

	private Integer sigkillDelay = 100; // 100 msec delay from SIGTERM to SIGKILL

	private Integer timeoutSeconds = null;

	private LoggingType loggingType = LoggingType.LOG4J;
	private String connectionFactory = null;
	private String topicDestination = null;
	private String jndiUrl = null;

	private String streamingOutputClassName = null;
	private String singletonClassName = null;
	
	private String argPreProcessorClassName = null;

	// Either a handler program or a Streaming Output Class is required.

	public String getHandlerProgram() {
		return handlerProgram;
	}

	public void setHandlerProgram(String s) {
		handlerProgram = s;
	}

	public String getStreamingOutputClassName() {
		return streamingOutputClassName;
	}

	public void setStreamingOutputClassName(String s) {
		streamingOutputClassName = s;
	}

	// Required configuration entries. Failure to find these will result in an
	// exception.

	public String getRootServicePath() {
		return rootServicePath;
	}

	public void setRootServicePath(String s) {
		rootServicePath = s;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String s) {
		appName = s;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String s) {
		version = s;
	}

	// These config entries have defaults from this class.
	public OutputType getOutputType() {
		return outputType;
	}

	public void setOutputType(OutputType e) {
		outputType = e;
	}

	public void setOutputType(String s) throws Exception {
		try {
			this.outputType = OutputType.valueOf(s.toUpperCase());
		} catch (Exception e) {
			throw new Exception("Unrecognized output format: " + s);
		}
	}

	public LoggingType getLoggingType() {
		return loggingType;
	}

	public void setLoggingType(LoggingType e) {
		loggingType = e;
	}

	public void setLoggingType(String s) throws Exception {
		try {
			this.loggingType = LoggingType.valueOf(s.toUpperCase());
		} catch (Exception e) {
			throw new Exception("Unrecognized logging type: " + s);
		}
	}

	public String getWorkingDirectory() {
		return workingDirectory;
	}

	public void setWorkingDirectory(String s) {
		workingDirectory = s;
	}

	public Boolean getPostEnabled() {
		return postEnabled;
	}

	public void setPostEnabled(Boolean b) {
		postEnabled = b;
	}

	public Boolean getUse404For204() {
		return use404For204;
	}

	public void setUse404For204(Boolean b) {
		use404For204 = b;
	}
	
	public Boolean getAllowCors() {
		return allowCors;
	}

	public void setAllowCors(Boolean b) {
		allowCors = b;
	}

	public Integer getSigkillDelay() {
		return sigkillDelay;
	}

	public void setSigkillDelay(Integer i) {
		sigkillDelay = i;
	}

	// Not required. Might be defaulted elsewhere.

	public String getRootServiceDoc() {
		return rootServiceDoc;
	}

	public void setRootServiceDoc(String s) {
		rootServiceDoc = s;
	}
	
	public String getWadlPath() {
		return wadlPath;
	}

	public void setWadlPath(String s) {
		wadlPath = s;
	}

	public Integer getTimeoutSeconds() {
		return timeoutSeconds;
	}

	public void setTimeoutSeconds(Integer i) {
		timeoutSeconds = i;
	}

	public String getSingletonClassName() {
		return singletonClassName;
	}

	public void setSingletonClassName(String s) {
		singletonClassName = s;
	}

	
	public String getArgPreProcessorClassName() {
		return argPreProcessorClassName;
	}

	public void setArgPreProcessorClassName(String s) {
		argPreProcessorClassName = s;
	}
	
	public String getConnectionFactory() {
		return connectionFactory;
	}

	public void setConnectionFactory(String s) {
		connectionFactory = s;
	}

	public String getTopicDestination() {
		return topicDestination;
	}

	public void setTopicDestination(String s) {
		topicDestination = s;
	}

	public String getJndiUrl() {
		return jndiUrl;
	}

	public void setJndiUrl(String s) {
		jndiUrl = s;
	}

	// Other Getters. Not defaulted

	public String getCatalogsHandlerProgram() {
		return catalogsHandlerProgram;
	}

	public void setCatalogsHandlerProgram(String s) {
		catalogsHandlerProgram = s;
	}

	public String getContributorsHandlerProgram() {
		return contributorsHandlerProgram;
	}

	public void setContributorsHandlerProgram(String s) {
		contributorsHandlerProgram = s;
	}

	public String getCountsHandlerProgram() {
		return countsHandlerProgram;
	}

	public void setCountsHandlerProgram(String s) {
		countsHandlerProgram = s;
	}

	
	public Boolean isValid() {
		return isValid;
	}

	public void loadConfigFile(String configBase, ServletContext context)
			throws Exception {

		// Depending on the way the servlet context starts, this can be called
		// multiple
		// times via SingletonWrapper class.
		if (isLoaded)
			return;
		isLoaded = true;

		Properties configurationProps = new Properties();
		Boolean userConfig = false;

		// Now try to read a user config file from the location specified by the
		// wssConfigDir property concatenated with the web application name
		// (last part
		// of context path), e.g. 'station' or 'webserviceshell'
		String configFileName = null;

        String wssConfigDir = System.getProperty(wssConfigDirSignature);
        
        String warnMsg1 = "***** check system property for " + wssConfigDirSignature
            + ", value found: " + wssConfigDir;
        String warnMsg2 = "***** or check webapp name on cfg files, value found: "
            + configBase;
        
        if (isOkString(wssConfigDir) && isOkString(configBase)) {
            if (!wssConfigDir.endsWith("/")) {
                wssConfigDir += "/";
            }

            configFileName = wssConfigDir + configBase
                + userParamConfigSuffix;
            logger.info("Attempting to load application configuration file from: "
                + configFileName);

            try {
                configurationProps.load(new FileInputStream(configFileName));
                userConfig = true;
            } catch (IOException ex) {
                logger.warn("***** could not read service cfg file: " + configFileName);
                logger.warn("***** ignoring exception: " + ex);
                logger.warn(warnMsg1);
                logger.warn(warnMsg2);
            }
        } else {
            logger.warn("***** unexpected configuration for service cfg file");
            logger.warn(warnMsg1);
            logger.warn(warnMsg2);
        }

		// If no user config was successfully loaded, load the default config file
        // Exception at this point should propagate up.
        if (!userConfig) {
            InputStream inStream = this.getClass().getClassLoader()
                .getResourceAsStream(defaultConfigFileName);
            if (inStream == null) {
                throw new Exception("Default configuration file was not"
                    + " found for name: " + defaultConfigFileName);
            }
            logger.info("Attempting to load default application"
                + " configuration from here: " + defaultConfigFileName);

            configurationProps.load(inStream);
            logger.info("Default application properties loaded, file: "
                + defaultConfigFileName);
        }

		// Only allow one of handler program or streaming output class
		String handlerStr = configurationProps.getProperty("handlerProgram");
		String soStr = configurationProps
				.getProperty("streamingOutputClassName");

		if (!isOkString(handlerStr) && !isOkString(soStr))
			throw new Exception("Missing handler program configuration");

		if (isOkString(handlerStr) && isOkString(soStr))
			throw new Exception(
					"Handler program _AND_ StreamingOutput class specified.  Only one allowed.");

		if (isOkString(handlerStr))
			this.handlerProgram = handlerStr;

		if (isOkString(soStr))
			this.streamingOutputClassName = soStr;
		// ------------------------------------------------------

		String configStr;

		configStr = configurationProps.getProperty("rootServicePath");
		if (isOkString(configStr))
			this.rootServicePath = configStr;
		else
			throw new Exception("Missing rootServicePath configuration");

		configStr = configurationProps.getProperty("appName");
		if (isOkString(configStr))
			this.appName = configStr;
		else
			throw new Exception("Missing appName configuration");

		configStr = configurationProps.getProperty("version");
		if (isOkString(configStr))
			this.version = configStr;
		else
			throw new Exception("Missing version configuration");

		// ------------------------------------------------------------------
		configStr = configurationProps.getProperty("rootServiceDoc");
		if (isOkString(configStr))
			this.rootServiceDoc = configStr;

		configStr = configurationProps.getProperty("wadlPath");
		if (isOkString(configStr))
			this.wadlPath = configStr;
		
		configStr = configurationProps.getProperty("outputType");
		if (isOkString(configStr))
			this.setOutputType(configStr);

		configStr = configurationProps.getProperty("loggingMethod");
		if (isOkString(configStr))
			this.setLoggingType(configStr);

		configStr = configurationProps.getProperty("handlerTimeout");
		if (isOkString(configStr))
			this.timeoutSeconds = Integer.parseInt(configStr);

		configStr = configurationProps.getProperty("postEnabled");
		if (isOkString(configStr))
			this.postEnabled = Boolean.parseBoolean(configStr);

		configStr = configurationProps.getProperty("use404For204");
		if (isOkString(configStr))
			this.use404For204 = Boolean.parseBoolean(configStr);

		configStr = configurationProps.getProperty("allowCors");
		if (isOkString(configStr))
			this.allowCors = Boolean.parseBoolean(configStr);
		
		configStr = configurationProps.getProperty("sigkillDelay");
		if (isOkString(configStr))
			this.sigkillDelay = Integer.parseInt(configStr);

		configStr = configurationProps.getProperty("connectionFactory");
		if (isOkString(configStr))
			this.connectionFactory = configStr;

		configStr = configurationProps.getProperty("topicDestination");
		if (isOkString(configStr))
			this.topicDestination = configStr;

		configStr = configurationProps.getProperty("jndiUrl");
		if (isOkString(configStr))
			this.jndiUrl = configStr;

		configStr = configurationProps.getProperty("singletonClassName");
		if (isOkString(configStr))
			this.singletonClassName = configStr;
		
		configStr = configurationProps.getProperty("argPreProcessorClassName");
		if (isOkString(configStr))
			this.argPreProcessorClassName = configStr;

		configStr = configurationProps.getProperty("catalogsHandlerProgram");
		if (isOkString(configStr))
			this.catalogsHandlerProgram = configStr;

		configStr = configurationProps.getProperty("contributorsHandlerProgram");
		if (isOkString(configStr))
			this.contributorsHandlerProgram = configStr;
		
		configStr = configurationProps.getProperty("countsHandlerProgram");
		if (isOkString(configStr))
			this.countsHandlerProgram = configStr;

		// Load the configuration for the working directory and substitute
		// System properties and environment properties.
		configStr = configurationProps.getProperty("handlerWorkingDirectory");
		if (isOkString(configStr)) {

			if (!configStr.matches("/.*|.*\\$\\{.*\\}.*")) {
				this.workingDirectory = configStr;
			} else {
				Properties props = System.getProperties();
				for (Object key : props.keySet()) {
					this.workingDirectory = configStr.replaceAll("\\$\\{" + key
							+ "\\}", props.getProperty(key.toString()));
				}
				Map<String, String> map = System.getenv();
				for (String key : map.keySet()) {
					this.workingDirectory = configStr.replaceAll("\\$\\{" + key
							+ "\\}", map.get(key));
				}
			}

			// If the working directory is and absolute path then just use it
			// If it's relative, then reference it to the servlet context.
			if (!this.workingDirectory.matches("/.*")) {
				this.workingDirectory = context
						.getRealPath(this.workingDirectory);
			}

			File f = new File(this.workingDirectory);
			if (!f.exists())
				throw new Exception("Working Directory: "
						+ this.workingDirectory + " does not exist");

			if (!f.canWrite() || !f.canRead())
				throw new Exception(
						"Improper permissions on working Directory: "
								+ this.workingDirectory);
		}

		// Finished w/o problems.
		this.isValid = true;
		logger.info(this.toString());
	}

	public static String getMimeType(OutputType type) {

		switch (type) {
		case XML:
			return "application/xml";
		case TEXT:
			return "text/plain";
		case TEXTTREE:
			return "text/plain";
		case JSON:
			return "application/json";
		case MSEED:
		case MINISEED:
			return "application/vnd.fdsn.mseed";
		case ZIP:
			return "application/zip";
		default:
			return "text/plain";
		}
	}

	public static String getExtension(OutputType type) {
		switch (type) {
		case XML:
			return ".xml";
		case TEXT:
			return ".txt";
		case TEXTTREE:
			return ".txt";
		case JSON:
			return ".json";
		case MSEED:
		case MINISEED:
			return ".mseed";
		case ZIP:
			return ".zip";
		default:
			return null;
		}
	}

	public static String getContentDispositionType(OutputType type) {
		switch (type) {
		case MSEED:
		case MINISEED:
			return "attachment";
		default:
			return "inline";
		}
	}

	public String getOutputFilename(OutputType type) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		return this.appName + "_" + sdf.format(new Date())
				+ getExtension(type);
	}

	private static boolean isOkString(String s) {
		return ((s != null) && !s.isEmpty());
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("WSS Service Configuration" + "\n");

		sb.append(strAppend("WSS Version") + wssVersion + "\n");

		sb.append(strAppend("Root Service Path") + rootServicePath + "\n");
		sb.append(strAppend("Root Service Doc") + rootServiceDoc + "\n");
		sb.append(strAppend("WADL Path") + wadlPath + "\n");

		sb.append(strAppend("Application Name") + appName + "\n");
		sb.append(strAppend("Version") + version + "\n");

		sb.append(strAppend("Handler Working Directory") + workingDirectory
				+ "\n");
		sb.append(strAppend("Handler Program") + handlerProgram + "\n");
		sb.append(strAppend("Handler Timeout") + timeoutSeconds + "\n");

		sb.append(strAppend("Catalog Handler Program") + catalogsHandlerProgram
				+ "\n");
		sb.append(strAppend("Contributor Handler Program")
				+ contributorsHandlerProgram + "\n");

		sb.append(strAppend("Post Enabled") + postEnabled + "\n");
		sb.append(strAppend("Use 404 for 204") + use404For204 + "\n");

		sb.append(strAppend("Output Type") + outputType + "\n");

		sb.append(strAppend("Logging Method") + loggingType + "\n");

		if (jndiUrl != null)
			sb.append(strAppend("JNDI URL") + jndiUrl + "\n");

		if (connectionFactory != null)
			sb.append(strAppend("Connection Factory") + connectionFactory
					+ "\n");

		if (topicDestination != null)
			sb.append(strAppend("Topic Destination") + topicDestination + "\n");

		if (singletonClassName != null)
			sb.append(strAppend("Singleton ClassName") + singletonClassName
					+ "\n");

		if (streamingOutputClassName != null)
			sb.append(strAppend("Streaming Output Class ")
					+ streamingOutputClassName + "\n");

		return sb.toString();
	}

	private final int colSize = 30;

	private String strAppend(String s) {
		int len = s.length();
		for (int i = 0; i < colSize - len; i++) {
			s += " ";
		}
		return s;
	}

	public String toHtmlString() {
		StringBuilder sb = new StringBuilder();
		sb.append("<TABLE border=2 style='width: 600px'>");
		sb.append("<col style='width: 30%' />");

		sb.append("<TR><TH colspan=\"2\" >" + "WSS Service Configuration"
				+ "</TH></TR>");

		sb.append("<TR><TD>" + "WSS Version" + "</TD><TD>" + wssVersion
				+ "</TD></TR>");

		sb.append("<TR><TD>" + "Root Service Path" + "</TD><TD>"
				+ rootServicePath + "</TD></TR>");
		sb.append("<TR><TD>" + "Root Service Doc" + "</TD><TD>"
				+ rootServiceDoc + "</TD></TR>");
		sb.append("<TR><TD>" + "WADL Path" + "</TD><TD>"
				+ wadlPath + "</TD></TR>");

		sb.append("<TR><TD>" + "Application Name" + "</TD><TD>" + appName
				+ "</TD></TR>");
		sb.append("<TR><TD>" + "Version" + "</TD><TD>" + version + "</TD></TR>");

		sb.append("<TR><TD>" + "Handler Working Directory" + "</TD><TD>"
				+ workingDirectory + "</TD></TR>");
		sb.append("<TR><TD>" + "Handler Program" + "</TD><TD>" + handlerProgram
				+ "</TD></TR>");
		sb.append("<TR><TD>" + "Handler Timeout" + "</TD><TD>" + timeoutSeconds
				+ "</TD></TR>");

		sb.append("<TR><TD>" + "Catalogs Handler Program" + "</TD><TD>"
				+ catalogsHandlerProgram + "</TD></TR>");
		sb.append("<TR><TD>" + "Contributors Handler Program" + "</TD><TD>"
				+ contributorsHandlerProgram + "</TD></TR>");
		sb.append("<TR><TD>" + "Counts Handler Program" + "</TD><TD>"
				+ countsHandlerProgram + "</TD></TR>");
		sb.append("<TR><TD>" + "Post Enabled" + "</TD><TD>" + postEnabled
				+ "</TD></TR>");
		sb.append("<TR><TD>" + "Use 404 for 204" + "</TD><TD>" + use404For204
				+ "</TD></TR>");

		sb.append("<TR><TD>" + "Output Type" + "</TD><TD>" + outputType
				+ "</TD></TR>");

		sb.append("<TR><TD>" + "Logging Method" + "</TD><TD>" + loggingType
				+ "</TD></TR>");

		if (jndiUrl != null)
			sb.append("<TR><TD>" + "JNDI URL" + "</TD><TD>" + jndiUrl
					+ "</TD></TR>");

		if (connectionFactory != null)
			sb.append("<TR><TD>" + "Connection Factory" + "</TD><TD>"
					+ connectionFactory + "</TD></TR>");

		if (topicDestination != null)
			sb.append("<TR><TD>" + "Topic Destination" + "</TD><TD>"
					+ topicDestination + "</TD></TR>");

		if (singletonClassName != null)
			sb.append("<TR><TD>" + "Singleton ClassName" + "</TD><TD>"
					+ singletonClassName + "</TD></TR>");

		if (streamingOutputClassName != null)
			sb.append("<TR><TD>" + "Streaming Output Class " + "</TD><TD>"
					+ streamingOutputClassName + "</TD></TR>");

		sb.append("</TABLE>");

		return sb.toString();
	}
}
