package edu.iris.wss.framework;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;


import org.apache.log4j.Logger;

import edu.iris.wss.framework.ParamConfigurator.ConfigParam.ParamType;

public class ParamConfigurator {

	private static final String wssConfigDirSignature = "wssConfigDir";
	
    private static final String defaultConfigFileName = "META-INF/param.cfg";
    private static final String userParamConfigSuffix = "Param.cfg";
    
	public static final Logger logger = Logger.getLogger(ParamConfigurator.class);

	private Boolean isLoaded = false;

	public static class ConfigParam {
		public static enum ParamType { TEXT, DATE, NUMBER, BOOLEAN, NONE };

		public String name;
		public ParamType type;
		public String value = null;

		public ConfigParam() {
		}
		
		public ConfigParam (String name, ParamType type) {
			this.name = name;
			this.type = type;
		}
		
		public ConfigParam (String name, ParamType type, String value) {
			this.name = name;
			this.type = type;
			this.value = value;
		}
	}

	public HashMap<String, ConfigParam> paramMap = new HashMap<String, ConfigParam>();
	
	public String getValue(String key) {
		ConfigParam cp = paramMap.get(key);
		if (cp == null) return null;
		else return cp.value;
	}
	
	public void loadConfigFile(String appName) throws Exception {		
		
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
	    		configurationProps.load(new FileInputStream(configFileName));
	    		userConfig = true;
	    		logger.info("Loaded Parameter configuration file from: " + configFileName);
			}
		} catch (Exception e) {
//			logger.info("Failed to load service config file from: " + configFileName);
		}

		// If no user config was successfully loaded, load the default config file
		// Exception at this point should prop
		if (!userConfig) {
			configurationProps.load(this.getClass().getClassLoader().getResourceAsStream(defaultConfigFileName));
		}
				
		@SuppressWarnings({ "unchecked", "rawtypes" })
		List<String> tmpKeys = new ArrayList(configurationProps.keySet());
		
		for (String key: tmpKeys) {
			if (paramMap.containsKey(key))
				throw new Exception("Duplicated config parameter: " + key);
			
			String type = (String) configurationProps.get(key);
			if (!isOkString(type) )  type = null;
			ParamType paramType;
			try {
				paramType = ParamType.valueOf(type.toUpperCase());	
			} catch (Exception e) {
				throw new Exception("Unrecognized param type: " + type);
			}
			
			paramMap.put(key, new ConfigParam(key, paramType)); 
			configurationProps.remove(key);	
		}
	}

	private static boolean isOkString(String s) {
		return ((s != null) && !s.isEmpty());
	}		
}
