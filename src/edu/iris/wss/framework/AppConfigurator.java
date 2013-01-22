package edu.iris.wss.framework;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;

public class AppConfigurator {
	private static final String wssConfigDirSignature = "wssConfigDir";

    private static final String defaultConfigFileName = "META-INF/service.cfg";
    private static final String userParamConfigSuffix = "-service.cfg";
    
	public static final Logger logger = Logger.getLogger(AppConfigurator.class);

	private Boolean isLoaded = false;
	private Boolean isValid = false;
	
	public static enum OutputType { XML, JSON, TEXT, SEED, TEXTTREE };
	public static enum LoggingType { LOG4J, JMS };
	
    private OutputType outputType = OutputType.TEXT;
    
    private String rootServicePath;
    private String rootServiceDoc;
    
	private String workingDirectory = "/";
	private String handlerProgram;
	private String appName;
	private String version;
	
	private Boolean postEnabled = false;
	private Boolean use404For204 = false;
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
	
	public Boolean getPostEnabled()						{ return postEnabled; }
	public void setPostEnabled(Boolean b) 				{ postEnabled = b; }
	
	public Boolean getUse404For204()					{ return use404For204; }
	public void setUse404For204(Boolean b) 				{ use404For204 = b; }
	
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
	
	// Other Getters.
	
	public Boolean isValid() 							{ return isValid; }
	
	public void loadConfigFile(String configBase, ServletContext context) throws Exception {
		
		// Depending on the way the servlet context starts, this can be called multiple
		// times via SingletonWrapper class.
		if (isLoaded) return;
		isLoaded = true;
		
		Properties configurationProps = new Properties();
		Boolean userConfig = false;

		// Initially to read a user config file from the location specified by the
		// wssConfigDir property concatenated with the web application name (last part
		// of context path), e.g. 'station' or 'webserviceshell'
		String configFileName = null;
		try {
			String wssConfigDir = System.getProperty(wssConfigDirSignature);
			if (isOkString(wssConfigDir) && isOkString(appName)) {
				if (!wssConfigDir.endsWith("/")) 
					wssConfigDir += "/";
				
				configFileName = wssConfigDir + appName + userParamConfigSuffix;		
	    		logger.info("Attempting to load application configuration file from: " + configFileName);

				configurationProps.load(new FileInputStream(configFileName));
	    		userConfig = true;
			}
		} catch (Exception e) {
			logger.info("Failure loading application config file from: " + configFileName);
		}
		
		// If no user config was successfully loaded, load the default config file
		// Exception at this point should propagate up.
		if (!userConfig) {
			configurationProps.load(this.getClass().getClassLoader().getResourceAsStream(defaultConfigFileName));
		}
		
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
		
		configStr = configurationProps.getProperty("handlerTimeout");
		if (isOkString(configStr)) 
			this.timeoutSeconds = Integer.parseInt(configStr);
		
		configStr = configurationProps.getProperty("postEnabled");
		if (isOkString(configStr)) 
			this.postEnabled = Boolean.parseBoolean(configStr);
		
		configStr = configurationProps.getProperty("use404For204");
		if (isOkString(configStr)) 
			this.use404For204 = Boolean.parseBoolean(configStr);
				
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
		
		// Load the configuration for the working directory and substiute 
		// System properties and environment properties.
		configStr = configurationProps.getProperty("handlerWorkingDirectory");
		if (isOkString(configStr)) {
			
			File f = null;
			if (!configStr.matches("/.*|.*\\$\\{.*\\}.*")){
				this.workingDirectory = configStr;
			} else {
			    Properties props = System.getProperties();
			    for(Object key: props.keySet()) {
			        this.workingDirectory = configStr.replaceAll("\\$\\{"+key+"\\}", props.getProperty(key.toString()));
			    }
			    Map<String, String>map = System.getenv();
			    for(String key: map.keySet()) {
			        this.workingDirectory = configStr.replaceAll("\\$\\{"+key+"\\}", map.get(key));
			    }    
			}
			
			// If the working directory is and absolute path then just use it
			// If it's relative, then reference it to the servlet context.  
			if (this.workingDirectory.matches("/.*")) {
				f = new File(this.workingDirectory);
			} else {
				f = new File(context.getRealPath(this.workingDirectory));
			}

			logger.info("WD: " + f.getAbsolutePath());
			if (!f.exists()) 
				throw new Exception("Working Directory: " + this.workingDirectory + " does not exist");
			
			if (!f.canWrite() || !f.canRead()) 
				throw new Exception("Improper permissions on working Directory: " + this.workingDirectory );
		}
		
		// Finished w/o problems.
		this.isValid = true;
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
	
	public String getContentDispositionType() {
		switch (this.getOutputType()) {
		case SEED:
			return "attachment"; 
		default: 
			return "inline";
		}
	}
	
	
	public String getOutputFilename() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		return this.appName + "_" + sdf.format(new Date()) + this.getExtension();
	}
	
	private static boolean isOkString(String s) {
		return ((s != null) && !s.isEmpty());
	}	
	
	public String toHtmlString() {
		StringBuilder sb = new StringBuilder();
		sb.append("<TABLE border=2 style='width: 600px'>");

		sb.append("<TR><TH colspan=\"2\" >" + "Web Service Shell Configuration" + "</TH></TR>");
		
		sb.append("<TR><TD>" + "Root Service Path" + "</TD><TD>" + rootServicePath + "</TD></TR>");
		sb.append("<TR><TD>" + "Root Service Doc" + "</TD><TD>" + rootServiceDoc + "</TD></TR>");
		
		sb.append("<TR><TD>" + "Application Name" + "</TD><TD>" + appName + "</TD></TR>");
		sb.append("<TR><TD>" + "Version" + "</TD><TD>" +  version + "</TD></TR>");

		sb.append("<TR><TD>" + "Handler Working Directory" + "</TD><TD>" + workingDirectory + "</TD></TR>");
		sb.append("<TR><TD>" + "Handler Program" + "</TD><TD>" + handlerProgram + "</TD></TR>");
		sb.append("<TR><TD>" + "Handler Timeout" + "</TD><TD>" + timeoutSeconds + "</TD></TR>");
		
		sb.append("<TR><TD>" + "Post Enabled" + "</TD><TD>" + postEnabled + "</TD></TR>");
		sb.append("<TR><TD>" + "Use 404 for 204" + "</TD><TD>" + use404For204 + "</TD></TR>");
		
		sb.append("<TR><TD>" + "Output Type" + "</TD><TD>" + outputType + "</TD></TR>");
		    
		sb.append("<TR><TD>" + "Logging Method" + "</TD><TD>" + loggingType + "</TD></TR>");

		if (jndiUrl != null) 
			sb.append("<TR><TD>" + "JNDI URL" + "</TD><TD>" + jndiUrl + "</TD></TR>");

		if (connectionFactory != null) 
			sb.append("<TR><TD>" + "Connection Factory" + "</TD><TD>" + connectionFactory + "</TD></TR>");
		
		if (topicDestination != null) 
			sb.append("<TR><TD>" + "Topic Destination" + "</TD><TD>" + topicDestination + "</TD></TR>");
		
		if (singletonClassName != null) 
			sb.append("<TR><TD>" + "Singleton ClassName" + "</TD><TD>" + singletonClassName + "</TD></TR>");
		
		if (streamingOutputClassName != null) 
			sb.append("<TR><TD>" + "Streaming Output Class " + "</TD><TD>"  + streamingOutputClassName + "</TD></TR>");

		sb.append("</TABLE>");

		return sb.toString();
	}
}
