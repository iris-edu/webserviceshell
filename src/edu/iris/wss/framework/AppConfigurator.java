package edu.iris.wss.framework;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.apache.log4j.Logger;

public class AppConfigurator {

    private static final String configFilePath = "META-INF/service.cfg";
	public static final Logger logger = Logger.getLogger(AppConfigurator.class);

	public static enum OutputType { XML, JSON, TEXT, SEED, TEXTTREE };
	public static enum LoggingType { LOG4J, JMS };
	
    private OutputType outputType = OutputType.TEXT;
    
    private String rootServicePath;
    private String rootServiceDoc;
    
	private String workingDirectory = "/";
	private String handlerProgram;
	private String appName;
	private String version;
	
	private Integer timeoutSeconds = null;
    
	private LoggingType loggingType = LoggingType.LOG4J;
	private String connectionFactory = null;
	private String topicDestination = null;
	private String jndiUrl = null;
    
	private String streamingOutputClassName = null;
	private String singletonClassName = null;
	
	// Required configuration entries.  Failure to find these will result in an exception.
	public String getHandlerProgram() 					{ return handlerProgram; }
	public void setHandlerProgram(String s)				{ handlerProgram = s; }
	
	public String getRootServicePath() 					{ return rootServicePath; }
	public void setRootServicePath(String s)			{ rootServicePath = s; }
	
	public String getRootServiceDoc() 					{ return rootServiceDoc; }
	public void setRootServiceDoc(String s)				{ rootServiceDoc = s; }
	
	public String getAppName() 							{ return appName; }
	public void setAppName(String s)					{ appName = s; }
	
	public String getVersion() 							{ return version; }
	public void setVersion(String s)					{ version = s; }
	
	// These config entries have defaults from this class.
	public OutputType getOutputType() 					{ return outputType; }
	public void setOutputType(OutputType e)				{ outputType = e;  logger.info("OutputType set to " + e.toString()); }
	
	public void setOutputType(String s) throws Exception {
		try {
			this.outputType = OutputType.valueOf(s.toUpperCase());	
			logger.info("OutputType set to " + outputType.toString());
		} catch (Exception e) {
			throw new Exception("Unrecognized output format: " + s);
		}
	}
	
	public LoggingType getLoggingType() 				{ return loggingType; }
	public void setLoggingType(LoggingType e)			{ loggingType = e; }
	
	public void setLoggingType(String s) throws Exception {
		try {
			this.loggingType = LoggingType.valueOf(s.toUpperCase());	
		} catch (Exception e) {
			throw new Exception("Unrecognized logging type: " + s);
		}
	}
	
	public String getWorkingDirectory() 				{ return workingDirectory; }
	public void setWorkingDirectory(String s)			{ workingDirectory = s; }
	
	// Not required.  Might be defaulted elsewhere.
	public Integer getTimeoutSeconds() 					{ return timeoutSeconds; }
	public void setTimeoutSeconds(Integer i)			{ timeoutSeconds = i; }	
	
	public String getStreamingOutputClassName() 		{ return streamingOutputClassName; }
	public void setStreamingOutputClassName(String s)	{ streamingOutputClassName = s; }
	
	public String getSingletonClassName() 				{ return singletonClassName; }
	public void setSingletonClassName(String s)			{ singletonClassName = s; }	
	
	public String getConnectionFactory() 				{ return connectionFactory; }
	public void setConnectionFactory(String s)			{ connectionFactory = s; }
	
	public String getTopicDestination() 				{ return topicDestination; }
	public void setTopicDestination(String s)			{ topicDestination = s; }
	
	public String getJndiUrl() 							{ return jndiUrl; }
	public void setJndiUrl(String s)					{ jndiUrl = s; }
	

	// Other query parameters.
	
	public void loadConfigFile() throws Exception {
		
		Properties configurationProps = new Properties();

		// This configuration rather than .getResourceAsStream() avoids caching.
		configurationProps.load(this.getClass().getClassLoader().getResource(configFilePath).openStream());
		
		String configStr;		
		configStr = configurationProps.getProperty("handlerProgram");
		if (isOkString(configStr))
			this.handlerProgram = configStr;
		else 
			throw new Exception("Missing handler program configuration");
		
		configStr = configurationProps.getProperty("rootServicePath");
		if (isOkString(configStr))
			this.rootServicePath = configStr;
		else 
			throw new Exception("Missing rootServicePath configuration");
		
		configStr = configurationProps.getProperty("rootServiceDoc");
		if (isOkString(configStr))
			this.rootServiceDoc = configStr;
		else 
			throw new Exception("Missing rootServiceDoc configuration");
		
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
		
		//------------------------------------------------------------------
		
		configStr = configurationProps.getProperty("outputType");
		if (isOkString(configStr)) 
			this.setOutputType(configStr);
		
		configStr = configurationProps.getProperty("loggingMethod");
		if (isOkString(configStr)) 
			this.setLoggingType(configStr);
		
		configStr = configurationProps.getProperty("handlerWorkingDirectory");
		if (isOkString(configStr))
			this.workingDirectory = configStr;
		
		configStr = configurationProps.getProperty("handlerTimeout");
		if (isOkString(configStr)) 
			this.timeoutSeconds = Integer.parseInt(configStr);
				
		configStr = configurationProps.getProperty("connectionFactory");
		if (isOkString(configStr)) 
			this.connectionFactory = configStr;
		
		configStr = configurationProps.getProperty("topicDestination");
		if (isOkString(configStr)) 
			this.topicDestination = configStr;
		
		configStr = configurationProps.getProperty("jndiUrl");
		if (isOkString(configStr)) 
			this.jndiUrl = configStr;
		
		configStr = configurationProps.getProperty("streamingOutputClassName");
		if (isOkString(configStr)) 
			this.streamingOutputClassName = configStr;
		
		configStr = configurationProps.getProperty("singletonClassName");
		if (isOkString(configStr)) 
			this.singletonClassName = configStr;
	}
	
	public String getMimeType() {

		switch (this.getOutputType()) {
		case XML:
			return "application/xml";
		case TEXT:
			return "text/plain";
		case TEXTTREE:
			return "text/plain";
		case JSON:
			return "application/json";
		case SEED:
			return "application/vnd.fdsn.seed";
		default: 
			return "text/plain";
		}
	}
	
	public String getExtension() {
		switch (this.getOutputType()) {
		case XML:
			return ".xml";
		case TEXT:
			return ".txt";
		case TEXTTREE:
			return ".txt";
		case JSON:
			return ".json";
		case SEED:
			return ".seed";
		default: 
			return null;
		}
	}
	
	public String getOutputFilename() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		return this.appName + "_" + sdf.format(new Date()) + this.getExtension();
	}
	
	private static boolean isOkString(String s) {
		return ((s != null) && !s.isEmpty());
	}	
}
