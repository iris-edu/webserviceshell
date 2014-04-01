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

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;


import org.apache.log4j.Logger;

import edu.iris.wss.framework.ParamConfigurator.ConfigParam.ParamType;
import java.io.InputStream;

public class ParamConfigurator {

	private static final String wssConfigDirSignature = "wssConfigDir";
	
    private static final String defaultConfigFileName = "META-INF/param.cfg";
    private static final String userParamConfigSuffix = "-param.cfg";
    
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
	
	public void loadConfigFile(String configBase) throws Exception {		
		
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
			if (isOkString(wssConfigDir) && isOkString(configBase)) {
				if (!wssConfigDir.endsWith("/")) 
					wssConfigDir += "/";
				
				configFileName = wssConfigDir + configBase + userParamConfigSuffix;			
	    		logger.info("Attempting to load parameter configuration file from: " + configFileName);
	    		
	    		configurationProps.load(new FileInputStream(configFileName));
	    		userConfig = true;
			}
		} catch (Exception e) {
                    logger.warn("Failed to load parameter config file from: "
                        + configFileName);
		}

		// If no user config was successfully loaded, load the default config file
		// Exception at this point should prop
                if (!userConfig) {
                    InputStream inStream = this.getClass().getClassLoader()
                        .getResourceAsStream(defaultConfigFileName);
                    if (inStream == null) {
                        throw new Exception("Default parameter file was not"
                            + " found for name: " + defaultConfigFileName);
                    }
                    logger.info("Attempting to load default parameter"
                        + " configuration from here: " + defaultConfigFileName);
                    
                    configurationProps.load(inStream);
                    logger.info("Default parameter properties loaded, file: "
                        + defaultConfigFileName);
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
		logger.info(this.toString());
	}

	private static boolean isOkString(String s) {
		return ((s != null) && !s.isEmpty());
	}		
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("WSS Parameter Configuration\n");

		for (String key: paramMap.keySet()) {	
			ConfigParam cp = paramMap.get(key);
			sb.append(strAppend(cp.name) + cp.type + "\n");
		}
		return sb.toString();
	}
	
	private final int colSize = 30;
	private String strAppend(String s) {
		int len = s.length();
		for (int i=0; i < colSize - len; i++) {
			s += " ";
		}
		return s;
	}
	
	public String toHtmlString() {
		StringBuilder sb = new StringBuilder();
		sb.append("<TABLE border=2 style='width: 600px'>");
		sb.append("<col style='width: 30%' />");
		sb.append("<TR><TH colspan=\"2\" >" + "WSS Parameter Configuration" + "</TH></TR>");

		for (String key: paramMap.keySet()) {	
			ConfigParam cp = paramMap.get(key);
			sb.append("<TR><TD>" + cp.name + "</TD><TD>" + cp.type + "</TD></TR>");
		}

		sb.append("</TABLE>");

		return sb.toString();
	}
}
