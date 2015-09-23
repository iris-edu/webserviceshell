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

import edu.iris.wss.utils.WebUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;

public class AppConfigurator {
	public static final Logger logger = Logger.getLogger(AppConfigurator.class);

	public static final String wssVersion = "mastr_v2x-SNAPSHOT";

	public static final String wssDigestRealmnameSignature = "wss.digest.realmname";

	private static final String defaultConfigFileName = "META-INF/service.cfg";
	private static final String userParamConfigSuffix = "-service.cfg";
    
    // this particular string is purposely matched is an error indicator
    // for timeout on miniseed data, although, unless changed, it will
    // also be used for writeNormal
    public static final String miniseedStreamInterruptionIndicator =
            "000000##ERROR#######ERROR##STREAMERROR##STREAMERROR#STREAMERROR\n" +
            "This data stream was interrupted and is likely incomplete.     \n" +
            "#STREAMERROR##STREAMERROR##STREAMERROR##STREAMERROR#STREAMERROR\n" +
            "#STREAMERROR##STREAMERROR##STREAMERROR##STREAMERROR#STREAMERROR\n";

	private Boolean isLoaded = false;
	private Boolean isValid = false;
    
    public AppConfigurator() {
        // set default type
        outputTypes.put("BINARY", "application/octet-stream");
        
        init();
    }
    
    private void init() {
        // set defaults, all parameters must be set here with
        // defaults of the correct (i.e. desired) object type.
        globals.put(GL_CFGS.appName.toString(), "notnamed");
        globals.put(GL_CFGS.appVersion.toString(), "notversioned");
        globals.put(GL_CFGS.corsEnabled.toString(), true);
        globals.put(GL_CFGS.swaggerV2URL.toString(), null);
        globals.put(GL_CFGS.wadlPath.toString(), null);
        globals.put(GL_CFGS.rootServiceDoc.toString(), null);
        globals.put(GL_CFGS.loggingMethod.toString(), LoggingMethod.LOG4J);
        globals.put(GL_CFGS.sigkillDelay.toString(), 100);
        globals.put(GL_CFGS.singletonClassName.toString(), null);

        ep_defaults.put(EP_CFGS.irisEndpointClassName,
              edu.iris.wss.endpoints.CmdProcessorIrisEP.class);
        ep_defaults.put(EP_CFGS.handlerProgram, null);
        ep_defaults.put(EP_CFGS.handlerTimeout, 30); // timeout in seconds
        ep_defaults.put(EP_CFGS.handlerWorkingDirectory, null);
        ep_defaults.put(EP_CFGS.outputTypes, null);
        ep_defaults.put(EP_CFGS.usageLog, true);
        ep_defaults.put(EP_CFGS.postEnabled, false);
        ep_defaults.put(EP_CFGS.use404For204, false);
    }

    // An enum of the types supported internally. This is used in the code to
    // identify places in which the external typeKeys specified in the
    // service.cfg file must aggree with the respective items in this enum.
    // An operator must have typeKeys "miniseed" to enable access
    // to respective code.
    //
    // BINARY is defined as the default
    //
    // miniseed is an alias for mseed - to be consistent with FDSN standards
    //
	public static enum InternalTypes {
		MSEED, MINISEED, BINARY
	};
    
    // Make enum names the same as the names the user sees in the cfg file
    // global configuration parameter names
    public static enum GL_CFGS { appName, appVersion, corsEnabled,
        swaggerV2URL, wadlPath, rootServiceDoc, loggingMethod, sigkillDelay,
        jndiUrl, singletonClassName, irisEndpointClassName};
    
    // endpoint configuration parameter names
    public static enum EP_CFGS { outputTypes, handlerTimeout,
        handlerProgram, handlerWorkingDirectory, usageLog, postEnabled, use404For204,
        irisEndpointClassName
    }

    Set<String> endpointNames = new HashSet();
    HashMap<String, Object> globals = new HashMap();
    HashMap<EP_CFGS, Object> ep_defaults = new HashMap();
    HashMap<EP_CFGS, Object> endpoints = new HashMap();
    
    
	private final InternalTypes outputType = InternalTypes.BINARY;
    private String defaultOutputTypeKey = "BINARY";
    public String defaultOutputTypeKey() {
		return defaultOutputTypeKey;
	}
    private Map<String, String> outputTypes = new HashMap<>();

	public static enum LoggingMethod {
		LOG4J, JMS
	};


	private String workingDirectory = "/";
	private String handlerProgram;
	private String catalogsHandlerProgram;
	private String contributorsHandlerProgram;
	private String countsHandlerProgram;

	//private String appName;
	//private String appVersion;
	//private Boolean corsEnabled = true;
    //private String swaggerV2URL;
	//private String wadlPath;
	//private String rootServiceDoc;
	//private LoggingType loggingType = LoggingType.LOG4J;
	//private Integer sigkillDelay = 100; // 100 msec delay from SIGTERM to SIGKILL
	//private String singletonClassName = null;

    //private String irisEndpointClassName = null;

	private Boolean usageLog = true;
	private Boolean postEnabled = false;
	private Boolean use404For204 = false;


	//private Integer timeoutSeconds = 30;


	// Either a handler program or a Streaming Output Class is required.

	public String getHandlerProgram() {
		return handlerProgram;
	}


	// Required configuration entries. Failure to find these will result in an
	// exception.

	public String getAppName() { return (String)globals.get(GL_CFGS.appName.toString()); }
	public String getAppVersion() { return (String)globals.get(GL_CFGS.appVersion.toString()); }
	public boolean getCorsEnabled() { return ((Boolean)globals.get(GL_CFGS.corsEnabled.toString())).booleanValue(); }
    public String getSwaggerV2URL() { return (String)globals.get(GL_CFGS.swaggerV2URL.toString()); }
    public String getWadlPath() { return (String)globals.get(GL_CFGS.wadlPath.toString()); }
    public String getRootServiceDoc() { return (String)globals.get(GL_CFGS.rootServiceDoc.toString()); }
    public LoggingMethod getLoggingType() { return (LoggingMethod)globals.get(GL_CFGS.rootServiceDoc.toString()); }
    public int getSigkillDelay() { return ((Integer)globals.get(GL_CFGS.sigkillDelay.toString())).intValue(); }
    public String getSingletonClassName() { return (String)globals.get(GL_CFGS.singletonClassName.toString()); }
    
	public String getWssVersion() {
		return wssVersion;
	}
    
    public String getIrisEndpointClassName() { return endpoints.get(EP_CFGS.irisEndpointClassName).toString(); }


    public boolean isConfiguredForTypeKey(String outputTypeKey) throws Exception {
        return outputTypes.containsKey(outputTypeKey);
	}

    public String getMediaType(String outputTypeKey) throws Exception {
        // Note: do the same operation on outputTypeKey as the setter, e.g. trim
        //       and toUpperCase
        String mediaType = outputTypes.get(outputTypeKey.trim().toUpperCase());
        if (mediaType == null) {
            throw new Exception("WebserviceShell getOutputTypes, no Content-Type"
                    + " found for outputType: " + outputTypeKey);
        }
		return mediaType;
	}
    
	public void setOutputTypes(String s) throws Exception {
        if (!isOkString(s)) {
			throw new Exception("Missing outputTypes, at least one pair must"
                    + " be set");
        }
        
        String[] pairs = s.split(java.util.regex.Pattern.quote(","));

        int count = 0;
        for (String pair : pairs) {
            String[] oneKV = pair.split(java.util.regex.Pattern.quote(":"));
            if (oneKV.length != 2) {
                throw new Exception(
                        "WebserviceShell setOutputTypes is expecting 2 items in"
                        + " a comma separated list of pairs of output type"
                        + " and HTTP Content-Type,"
                        + " instead item count: " + oneKV.length
                        + (oneKV.length == 1 ? "  first item: " + oneKV[0] : "")
                        + "  input: " + s);
            }

            String key = oneKV[0].trim().toUpperCase();
            outputTypes.put(key, oneKV[1].trim());
            
            // the first item in the list shall be the new default
            count++;
            if (count == 1) {
                defaultOutputTypeKey = key;
            }
        }
    }

	public String getWorkingDirectory() {
		return workingDirectory;
	}

	public void setWorkingDirectory(String s) {
		workingDirectory = s;
	}

	public Boolean getUsageLog() {
		return usageLog;
	}

	public void setUsageLog(Boolean b) {
		usageLog = b;
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

	// Not required. Might be defaulted elsewhere.

	public int getTimeoutSeconds() {
		return (int)globals.get(EP_CFGS.handlerTimeout.toString());
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
        Properties configurationProps = loadPropertiesFile(configBase, context);
        if (configurationProps != null) {
            loadConfigurationParameters(configurationProps, context);
        }
    }

	public Properties loadPropertiesFile(String configBase, ServletContext context)
			throws Exception {

		// Depending on the way the servlet context starts, this can be called
		// multiple
		// times via SingletonWrapper class.
		if (isLoaded) {
			return null;
        }
		isLoaded = true;

		Properties configurationProps = new Properties();
		Boolean userConfig = false;

		// Now try to read a user config file from the location specified by the
		// wssConfigDir property concatenated with the web application name
		// (last part
		// of context path), e.g. 'station' or 'webserviceshell'
		String configFileName = null;

        String wssConfigDir = System.getProperty(WebUtils.wssConfigDirSignature);
        
        String warnMsg1 = "***** check for system property "
              + WebUtils.wssConfigDirSignature
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
        return configurationProps;
    }

	public void loadConfigurationParameters(Properties inputProps,
          ServletContext context)
			throws Exception {
		// Only allow one of handler program or streaming output class
		String handlerStr = inputProps.getProperty("handlerProgram");
		String soStr = inputProps
				.getProperty("irisEndpointClassName");

		if (!isOkString(handlerStr) && !isOkString(soStr))
			throw new Exception("Missing handlerProgram parameter");

		if (isOkString(handlerStr) && isOkString(soStr)) {
//			throw new Exception(
//					"Both handlerProgram and irisEndpointClassName are specified.  Only one allowed.");
            System.out.println("*** *** *** Both handlerProgram and irisEndpointClassName are specified.  Only one allowed.");
        }

		if (isOkString(handlerStr))
			this.handlerProgram = handlerStr;

		// ------------------------------------------------------

		String valueStr;

        loadGlobalParameter(inputProps, globals, GL_CFGS.appName);
        loadGlobalParameter(inputProps, globals, GL_CFGS.appVersion);
        loadGlobalParameter(inputProps, globals, GL_CFGS.corsEnabled);
        loadGlobalParameter(inputProps, globals, GL_CFGS.swaggerV2URL);
        loadGlobalParameter(inputProps, globals, GL_CFGS.wadlPath);
        loadGlobalParameter(inputProps, globals, GL_CFGS.rootServiceDoc);
        loadGlobalParameter(inputProps, globals, GL_CFGS.loggingMethod);
        loadGlobalParameter(inputProps, globals, GL_CFGS.sigkillDelay);
        loadGlobalParameter(inputProps, globals, GL_CFGS.singletonClassName);

        loadEndpointParameter(inputProps, ep_defaults, endpoints, EP_CFGS.irisEndpointClassName);

        Set<String> endpointText = new HashSet();
        Enumeration keys = inputProps.propertyNames();
        while (keys.hasMoreElements()) {
            String name = (String)keys.nextElement();      
            System.out.println("**** name: " + name
                + "  value: " + inputProps.getProperty(name));
            
            String[] withDots = name.split(java.util.regex.Pattern.quote("."));
            if (withDots.length == 1) {
                // should be already loaded, noop
                System.out.println("*** glb: " + withDots[0]);
            } else if (withDots.length == 2) {
                try {
                        EP_CFGS trial = EP_CFGS.valueOf(withDots[1]);
                        // add endpoint only if there is a valid parameter
                        endpointText.add(withDots[0]);
                    } catch (IllegalArgumentException ex) {
                        System.out.println("****** ignoring ex: " + ex);
                        //throw new Exception("Unrecognized paramater: " + withDots[1], ex);
                    }
            } else if (withDots.length > 2) {
                System.out.println("*** ERR *** multiple dots not allowed, key: "
                + name);
            }
        }
        
        System.out.println("-------------------------- epTexts");
        Iterator<String> endpointsIter = endpointText.iterator();
        while (endpointsIter.hasNext()) {
            String epText = endpointsIter.next();
            System.out.println("******* epText: " + epText);
        }
        System.out.println("-------------------------- epTexts2");


		// ------------------------------------------------------------------
		
		valueStr = inputProps.getProperty("outputTypes");
		if (isOkString(valueStr))
			this.setOutputTypes(valueStr);

        String keyStr = EP_CFGS.handlerTimeout.toString();
		valueStr = inputProps.getProperty(keyStr);
		if (isOkString(valueStr)) {
            globals.put(keyStr, Integer.parseInt(valueStr));
        }

		valueStr = inputProps.getProperty("usageLog");
		if (isOkString(valueStr))
			this.usageLog = Boolean.parseBoolean(valueStr);

		valueStr = inputProps.getProperty("postEnabled");
		if (isOkString(valueStr))
			this.postEnabled = Boolean.parseBoolean(valueStr);

		valueStr = inputProps.getProperty("use404For204");
		if (isOkString(valueStr))
			this.use404For204 = Boolean.parseBoolean(valueStr);

		valueStr = inputProps.getProperty("catalogsHandlerProgram");
		if (isOkString(valueStr))
			this.catalogsHandlerProgram = valueStr;

		valueStr = inputProps.getProperty("contributorsHandlerProgram");
		if (isOkString(valueStr))
			this.contributorsHandlerProgram = valueStr;
		
		valueStr = inputProps.getProperty("countsHandlerProgram");
		if (isOkString(valueStr))
			this.countsHandlerProgram = valueStr;

		// Load the configuration for the working directory and substitute
		// System properties and environment properties.
		valueStr = inputProps.getProperty("handlerWorkingDirectory");
		if (isOkString(valueStr)) {

			if (!valueStr.matches("/.*|.*\\$\\{.*\\}.*")) {
				this.workingDirectory = valueStr;
			} else {
				Properties props = System.getProperties();
				for (Object key : props.keySet()) {
					this.workingDirectory = valueStr.replaceAll("\\$\\{" + key
							+ "\\}", props.getProperty(key.toString()));
				}
				Map<String, String> map = System.getenv();
				for (String key : map.keySet()) {
					this.workingDirectory = valueStr.replaceAll("\\$\\{" + key
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
    
	public void loadGlobalParameter(Properties input, HashMap cfgs, GL_CFGS eKey)
          throws Exception {
        
        String key = eKey.toString();
		String newVal = input.getProperty(key);
        
		if (isOkString(newVal)) {
            // use type set for default value to do additional processing
            Object previous = cfgs.get(key);
            
            if (previous != null) {
                if (previous instanceof Boolean) {
                    cfgs.put(key, Boolean.parseBoolean(newVal));
                } if (previous instanceof Integer) {
                    try {
                        cfgs.put(key, Integer.parseInt(newVal));
                    } catch (NumberFormatException ex) {
                        throw new Exception("Unrecognized value for paramater: " + key
                              + "  value found: " + newVal
                              + "  it should be an integer");
                    }
                } else if(previous instanceof LoggingMethod) {
                    try {
                        LoggingMethod trial = LoggingMethod.valueOf(newVal.toUpperCase());
                        cfgs.put(key, trial);
                    } catch (Exception ex) {
                        throw new Exception("Unrecognized value for paramater: " + key
                              + "  value found: " + newVal
                              + "  should be one of " + LoggingMethod.LOG4J.toString()
                              + " or " + LoggingMethod.JMS.toString(), ex);
                    }
                } else {
                    // should be String type if here
                    cfgs.put(key, newVal);
                }
            } else {
                // TBD add logging
                System.out.println("*** *** *** default value not defined for key: " + key);
                //throw new Exception("Missing required default for parameter: " + key);
            }
        } else {
            // TBD add logging
            System.out.println("*** property is null or empty for key: " + key);
            if (eKey.equals(GL_CFGS.appName) | eKey.equals(GL_CFGS.appVersion)) {
                throw new Exception("Missing required parameter: " + key );
            }
        }
	}

    
	public void loadEndpointParameter(Properties input, HashMap epDefaults,
          HashMap endPts, EP_CFGS eKey)
          throws Exception {
        
        String keyStr = eKey.toString();
		String newVal = input.getProperty(keyStr);
        
		if (isOkString(newVal)) {
            // use type set for default value to do additional processing
            Object defaultz = epDefaults.get(eKey);
            
            if (defaultz != null) {
                if (defaultz instanceof Boolean) {
                    endPts.put(eKey, Boolean.parseBoolean(newVal));
                } if (defaultz instanceof Integer) {
                    try {
                        endPts.put(eKey, Integer.parseInt(newVal));
                    } catch (NumberFormatException ex) {
                        throw new Exception("Unrecognized value for paramater: " + eKey
                              + "  value found: " + newVal
                              + "  it should be an integer");
                    }
                } else if(defaultz instanceof Class) {
                    try {
                        Class<?> irisClass = Class.forName(newVal);
                        endPts.put(eKey, irisClass);
                    } catch (Exception ex) {
                        throw new Exception("Unrecognized value for paramater: " + eKey
                              + "  value found: " + newVal
                              + "  should be a valid class name", ex);
                    }
                } else {
                    // should be String type if here
                    endPts.put(eKey, newVal);
                }
            } else {
                // TBD add logging
                System.out.println("*** default value not defined for key: " + eKey);
                throw new Exception("Missing required default for parameter: " + eKey);
            }
        } else {
            // TBD add logging
            System.out.println("*** property is null or empty for key: " + eKey);
            if (eKey.equals(EP_CFGS.irisEndpointClassName) | eKey.equals(EP_CFGS.handlerWorkingDirectory)) {
                throw new Exception("Missing required parameter: " + eKey );
            }
        }
	}

    public String formatOutputTypes() {
        return formatOutputTypes(outputTypes);
    }

    private static String formatOutputTypes(Map<String, String> outputTypes) {
        StringBuilder s = new StringBuilder();
        s.append("outputTypes = ");
        
        Iterator<String> keyIt = outputTypes.keySet().iterator();
        while(keyIt.hasNext()) {
            String key = keyIt.next();
            s.append(key).append(": ").append(outputTypes.get(key));
            if (keyIt.hasNext()) {
                s.append(", ");
            }
        }
        
        return s.toString();
    }

	private static boolean isOkString(String s) {
		return ((s != null) && !s.isEmpty());
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("WSS Service Configuration" + "\n");

		sb.append(strAppend("WSS Version") + wssVersion + "\n");

        List<String> keyList = new ArrayList();
        keyList.add(GL_CFGS.appName.toString());
        keyList.add(GL_CFGS.appVersion.toString());
        keyList.add(GL_CFGS.corsEnabled.toString());
        keyList.add(GL_CFGS.swaggerV2URL.toString());
        keyList.add(GL_CFGS.wadlPath.toString());
        keyList.add(GL_CFGS.rootServiceDoc.toString());
        keyList.add(GL_CFGS.loggingMethod.toString());
        keyList.add(GL_CFGS.sigkillDelay.toString());
        keyList.add(GL_CFGS.singletonClassName.toString());
        
        for (String key: keyList) {
            if (globals.get(key) != null) {
                sb.append(strAppend(key)).append(globals.get(key).toString()).append("\n");
            }
        }
        
        List<EP_CFGS> keyLis2 = new ArrayList();
        keyLis2.add(EP_CFGS.irisEndpointClassName);
        
        for (EP_CFGS key: keyLis2) {
            if (endpoints.get(key) != null) {
                sb.append(strAppend(key.toString())).append(endpoints.get(key).toString()).append("\n");
            }
        }

		sb.append(strAppend("Handler Working Directory") + workingDirectory
				+ "\n");
		sb.append(strAppend("Handler Program") + handlerProgram + "\n");
        String keyStr = EP_CFGS.handlerTimeout.toString();
		sb.append(strAppend("Handler Timeout") + globals.get(keyStr) + "\n");

		sb.append(strAppend("Catalog Handler Program") + catalogsHandlerProgram
				+ "\n");
		sb.append(strAppend("Contributor Handler Program")
				+ contributorsHandlerProgram + "\n");

		sb.append(strAppend("Usage Log") + usageLog + "\n");
		sb.append(strAppend("Post Enabled") + postEnabled + "\n");
		sb.append(strAppend("Use 404 for 204") + use404For204 + "\n");

		sb.append(strAppend("Default Output Type Key") + defaultOutputTypeKey + "\n");

		sb.append(strAppend("Output Types") + formatOutputTypes(outputTypes) + "\n");

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

        List<String> keyList = new ArrayList();
        keyList.add(GL_CFGS.appName.toString());
        keyList.add(GL_CFGS.appVersion.toString());
        keyList.add(GL_CFGS.corsEnabled.toString());
        keyList.add(GL_CFGS.swaggerV2URL.toString());
        keyList.add(GL_CFGS.wadlPath.toString());
        keyList.add(GL_CFGS.rootServiceDoc.toString());
        keyList.add(GL_CFGS.loggingMethod.toString());
        keyList.add(GL_CFGS.sigkillDelay.toString());
        keyList.add(GL_CFGS.singletonClassName.toString());
        
        for (String key: keyList) {
            if (globals.get(key) != null) {
                sb.append("<TR><TD>").append(key).append("</TD><TD>")
                      .append(globals.get(key).toString()).append("</TD></TR>");
            }
        }

        
        List<EP_CFGS> keyLis2 = new ArrayList();
        keyLis2.add(EP_CFGS.irisEndpointClassName);
        
        for (EP_CFGS key: keyLis2) {
            if (endpoints.get(key) != null) {
                sb.append("<TR><TD>").append(key.toString()).append("</TD><TD>")
                      .append(endpoints.get(key).toString()).append("</TD></TR>");
            }
        }

		sb.append("<TR><TD>" + "Handler Working Directory" + "</TD><TD>"
				+ workingDirectory + "</TD></TR>");
		sb.append("<TR><TD>" + "Handler Program" + "</TD><TD>" + handlerProgram
				+ "</TD></TR>");
        String keyStr = EP_CFGS.handlerTimeout.toString();
		sb.append("<TR><TD>" + "Handler Timeout" + "</TD><TD>" + globals.get(keyStr)
				+ "</TD></TR>");

		sb.append("<TR><TD>" + "Catalogs Handler Program" + "</TD><TD>"
				+ catalogsHandlerProgram + "</TD></TR>");
		sb.append("<TR><TD>" + "Contributors Handler Program" + "</TD><TD>"
				+ contributorsHandlerProgram + "</TD></TR>");
		sb.append("<TR><TD>" + "Counts Handler Program" + "</TD><TD>"
				+ countsHandlerProgram + "</TD></TR>");
		sb.append("<TR><TD>" + "Usage Log" + "</TD><TD>" + usageLog
				+ "</TD></TR>");
		sb.append("<TR><TD>" + "Post Enabled" + "</TD><TD>" + postEnabled
				+ "</TD></TR>");
		sb.append("<TR><TD>" + "Use 404 for 204" + "</TD><TD>" + use404For204
				+ "</TD></TR>");

		sb.append("<TR><TD>" + "Default Output Type Key" + "</TD><TD>"
                + defaultOutputTypeKey + "</TD></TR>");
        
		sb.append("<TR><TD>").append("Output Types").append("</TD><TD>")
                .append(formatOutputTypes(outputTypes)).append("</TD></TR>");

		sb.append("</TABLE>");

		return sb.toString();
	}
}
