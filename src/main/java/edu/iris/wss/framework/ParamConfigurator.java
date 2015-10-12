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
import edu.iris.wss.utils.WebUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

public class ParamConfigurator {
	public static final Logger logger = Logger.getLogger(ParamConfigurator.class);
	
    private static final String DEFAULT_PARAM_FILE_NAME = "META-INF/param.cfg";
    private static final String PARAM_CFG_NAME_SUFFIX = "-param.cfg";
    private static final String ALIASES_KEY_NAME = "aliases";
    
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

	public HashMap<String, ConfigParam> paramMap = new HashMap<>();
    
    // A map of aliases pointing to their respective parameter name
    // need empty map in case there are no aliases
	public Map<String, String> aliasesMap = new HashMap<>();
	
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

        Class thisRunTimeClass = this.getClass();
        
        Properties configurationProps = AppConfigurator.loadPropertiesFile(
              configBase, thisRunTimeClass, PARAM_CFG_NAME_SUFFIX,
              DEFAULT_PARAM_FILE_NAME);

//		Properties configurationProps = new Properties();
//		Boolean userConfig = false;
//				
//		// Initially to read a user config file from the location specified by the
//		// wssConfigDir property concatenated with the web application name (last part
//		// of context path), e.g. 'station' or 'webserviceshell'
//		String configFileName = null;
//
//        String wssConfigDir = System.getProperty(WebUtils.wssConfigDirSignature);
// 
//        String warnMsg1 = "***** check system property for "
//              + WebUtils.wssConfigDirSignature + ", value found: " + wssConfigDir;
//        String warnMsg2 = "***** or check webapp name on cfg files, value found: "
//            + configBase;
//
//        if (isOkString(wssConfigDir) && isOkString(configBase)) {
//            if (!wssConfigDir.endsWith("/")) {
//                wssConfigDir += "/";
//            }
//
//            configFileName = wssConfigDir + configBase + PARAM_CFG_NAME_SUFFIX;
//            logger.info("Attempting to load parameter configuration file from: "
//                + configFileName);
//
//            try {
//                configurationProps.load(new FileInputStream(configFileName));
//                userConfig = true;
//            } catch (IOException ex) {
//                logger.warn("***** could not read param cfg file: " + configFileName);
//                logger.warn("***** ignoring exception: " + ex);
//                logger.warn(warnMsg1);
//                logger.warn(warnMsg2);
//            }
//        } else {
//            logger.warn("***** unexpected configuration for service cfg file");
//            logger.warn(warnMsg1);
//            logger.warn(warnMsg2);
//        }
//
//		// If no user config was successfully loaded, load the default config file
//        // Exception at this point should prop
//        if (!userConfig) {
//            InputStream inStream = this.getClass().getClassLoader()
//                .getResourceAsStream(DEFAULT_PARAM_FILE_NAME);
//            if (inStream == null) {
//                throw new Exception("Default parameter file was not"
//                    + " found for name: " + DEFAULT_PARAM_FILE_NAME);
//            }
//            logger.info("Attempting to load default parameter"
//                + " configuration from here: " + DEFAULT_PARAM_FILE_NAME);
//
//            configurationProps.load(inStream);
//            logger.info("Default parameter properties loaded, file: "
//                + DEFAULT_PARAM_FILE_NAME);
//        }
				
		@SuppressWarnings({ "unchecked", "rawtypes" })
		List<String> tmpKeys = new ArrayList(configurationProps.keySet());
		
		for (String key: tmpKeys) {
			if (paramMap.containsKey(key))
				throw new Exception("Duplicated config parameter: " + key);
			
			String type = (String) configurationProps.get(key);
			if (!isOkString(type)) {
                type = null;
            }
            
            if (key.equals(ALIASES_KEY_NAME)) {
                aliasesMap = createAliasesMap(type);
            } else {
                ParamType paramType;
                try {
                    paramType = ParamType.valueOf(type.toUpperCase());	
                } catch (Exception e) {
                    throw new Exception("Unrecognized param type: " + type);
                }

                paramMap.put(key, new ConfigParam(key, paramType));
            }
			configurationProps.remove(key);	
		}
		logger.info(this.toString());
	}
    
    public static Map<String, String> createAliasesMap(String aliasValues)
            throws Exception {
        Map<String, String> aliases = new HashMap<>();
        
        StringBuilder s2 = new StringBuilder(aliasValues.length());
        
        // simple parsing, rebuild string with commas inside perenthesis
        // replaced with |
        int openPerenCnt = 0;
        for (int i1 = 0; i1 < aliasValues.length(); i1++){
            char c = aliasValues.charAt(i1);
            if (c == '(') {
                openPerenCnt++;
                if (openPerenCnt > 1) 
                {
                    throw new Exception(
                        "WebserviceShell createAliasesMap too many open parenthesis"
                            + " no nesting expected, aliases input: " + aliasValues);
                }
            }
            if (c == ')') {
                openPerenCnt--;
                if (openPerenCnt < 0) 
                {
                    throw new Exception(
                        "WebserviceShell createAliasesMap too many closed parenthesis"
                            + " no nesting expected, aliases input: " + aliasValues);
                }
            }
            
            if (openPerenCnt == 1 && c == ',') {
                c = '|';
            }
            s2.append(c);
        }
        if (openPerenCnt != 0) {
            throw new Exception(
                    "WebserviceShell createAliasesMap unbalanced parenthesis"
                        + " aliases input: " + aliasValues);
        }
        
        String[] pairs = s2.toString().split(java.util.regex.Pattern.quote(","));
        
        for (String pair : pairs) {
            String[] oneKV = pair.split(java.util.regex.Pattern.quote(":"));
            if (oneKV.length != 2) {
                throw new Exception(
                        "WebserviceShell createAliasesMap is expecting 2 items in"
                        + " a comma separated list of pairs of parameter:"
                        + " aliases,"
                        + " instead item count is: " + oneKV.length
                        + (oneKV.length == 1 ? "  first item: " + oneKV[0] : "")
                        + "  input: " + aliasValues);
            }

            String respectiveParam = oneKV[0].trim();
            
            String[] aliasArray = oneKV[1].trim().replace("(", "")
                    .replace(")", "").split(java.util.regex.Pattern.quote("|"));
            
            for (String s: aliasArray) {
                aliases.put(s.trim(), respectiveParam);
            }
        }
        
        return aliases;
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
        sb.append(aliasesMap).append("\n");
              
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
        sb.append("<TR><TD>" + ALIASES_KEY_NAME + "</TD><TD>" + aliasesMap + "</TD></TR>");

		sb.append("</TABLE>");

		return sb.toString();
	}
}
