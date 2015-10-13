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

import edu.iris.wss.IrisStreamingOutput.IrisStreamingOutput;
import edu.iris.wss.utils.WebUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;

public class AppConfigurator {
	public static final Logger logger = Logger.getLogger(AppConfigurator.class);

	public static final String wssVersion = "mastr_v2x-SNAPSHOT";

	public static final String wssDigestRealmnameSignature = "wss.digest.realmname";

	private static final String DEFAULT_SERVICE_FILE_NAME = "META-INF/service.cfg";
	private static final String SERVICE_CFG_NAME_SUFFIX = "-service.cfg";
    public static final String ENDPOINT_TO_PROPERTIES_DELIMITER = ".";
    
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
        init();
    }
    
    private void init()  {
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
              getIrisSOInstance("edu.iris.wss.endpoints.CmdProcessorIrisEP"));
        ep_defaults.put(EP_CFGS.handlerProgram, "nonespecified");
        ep_defaults.put(EP_CFGS.handlerTimeout, 30); // timeout in seconds
        ep_defaults.put(EP_CFGS.handlerWorkingDirectory, "/tmp");
        try {
            //   default_outputTypes.put("BINARY", "application/octet-stream");
            ep_defaults.put(EP_CFGS.outputTypes, createOutputTypes(""));
        } catch (Exception ex) {
            System.out.println("************** ERR ERR ERR in init ");
            java.util.logging.Logger.getLogger(AppConfigurator.class.getName()).log(Level.SEVERE, null, ex);
        }
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

    // parameters that apply to all endpoints
    private Map<String, Object> globals = new HashMap();

    // container for endpoints where the key is defined in the cfg file
    // as being the string in front of ENDPOINT_TO_PROPERTIES_DELIMITER
    private Map<String, Map<EP_CFGS, Object>> endpoints = new HashMap();
    
    // an endpoint, i.e. contains the parameters per endpoint
    private Map<EP_CFGS, Object> ep_defaults = new HashMap();
    //private Map<EP_CFGS, Object> endpoint = new HashMap();
    //private Map<String, String> default_outputTypes = createOutputTypes();
    
//	private final InternalTypes outputType = InternalTypes.BINARY;
//    private String defaultOutputTypeKey = "BINARY";

	public static enum LoggingMethod {
		LOG4J, JMS
	};

    // newTypes may be null or empty, which means return the default list
    
    // must use LinkedHashMap or equivalent to preserve order of types
    // to enable using the first element as "the default type"
    
    // must always have at least one element, binary is the designated default
    // at this time.
    public Map<String, String> createOutputTypes(String newTypes)
          throws Exception
    {
        Map<String, String> types = new LinkedHashMap<>();
        
        // set newTypes first so as to preserve order from configuration file
        if (isOkString(newTypes)) {
            setOutputTypes(types, newTypes);
        }
        
        // set default last
        setOutputTypes(types, "BINARY: application/octet-stream");
        
        return types;
    }
//	private String catalogsHandlerProgram;
//	private String contributorsHandlerProgram;
//	private String countsHandlerProgram;

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
	//private String handlerProgram;
	//private Integer timeoutSeconds = 30;
	//private String workingDirectory = "/";
    //private Map<String, String> outputTypes = new HashMap<>();
	//private Boolean usageLog = true;
	//private Boolean postEnabled = false;
	//private Boolean use404For204 = false;





	// Required configuration entries. Failure to find these will result in an
	// exception.

	public String getAppName() { return (String)globals.get(GL_CFGS.appName.toString()); }
	public String getAppVersion() { return (String)globals.get(GL_CFGS.appVersion.toString()); }
	public boolean getCorsEnabled() { return ((Boolean)globals.get(GL_CFGS.corsEnabled.toString())); }
    public String getSwaggerV2URL() { return (String)globals.get(GL_CFGS.swaggerV2URL.toString()); }
    public String getWadlPath() { return (String)globals.get(GL_CFGS.wadlPath.toString()); }
    public String getRootServiceDoc() { return (String)globals.get(GL_CFGS.rootServiceDoc.toString()); }
    public LoggingMethod getLoggingType() { return (LoggingMethod)globals.get(GL_CFGS.loggingMethod.toString()); }
    public int getSigkillDelay() { return ((Integer)globals.get(GL_CFGS.sigkillDelay.toString())); }
    public String getSingletonClassName() { return (String)globals.get(GL_CFGS.singletonClassName.toString()); }
    
	public String getWssVersion() { return wssVersion; }
    
//    public String getIrisEndpointClassName(String epName) throws Exception {
//        if (endpoints.containsKey(epName)) {
//            return endpoints.get(epName).get(EP_CFGS.irisEndpointClassName).toString();
//        }
//        throw new Exception("WebServiceShell getIrisEndpointClassName,"
//              + " there is no endpoint configured for endpoint name: "
//              + epName);
//    }

    /**
     * Note: no parameter checking or configuration for endpoint checking
     *       done here, it should be done when parameters are loaded.
     * @param epName
     * @return 
     */
    public IrisStreamingOutput getIrisEndpointClass(String epName) {
        return (IrisStreamingOutput)endpoints.get(epName)
              .get(EP_CFGS.irisEndpointClassName);
    }
    
    public String getHandlerProgram(String epName) {
        System.out.println("---- **** epName: " + epName);
        System.out.println("---- **** get(epName)): " + endpoints.get(epName));
        System.out.println("---- **** get(epName).get(EP_CFGS.handlerProgram): " + endpoints.get(epName).get(EP_CFGS.handlerProgram));
        return endpoints.get(epName).get(EP_CFGS.handlerProgram).toString();
    }
	public int getTimeoutSeconds(String epName) {
		return (int)endpoints.get(epName).get(EP_CFGS.handlerTimeout);
	}
    public String getWorkingDirectory(String epName) {
        return endpoints.get(epName).get(EP_CFGS.handlerWorkingDirectory).toString();
    }
    
    // Note: this can throw NullPointerException and ClassCastException
    public boolean isConfiguredForEndpoint(String epName) {
        return endpoints.containsKey(epName);
	}
    public Set<String> getEndpoints() {
        return endpoints.keySet();
	}
    
    // Note: this can throw NullPointerException and ClassCastException
    public boolean isConfiguredForTypeKey(String epName, String outputTypeKey) {
        Map<String, String> outTypes = (Map<String, String>)endpoints.get(epName)
              .get(EP_CFGS.outputTypes);
        return outTypes.containsKey(outputTypeKey);
	}
    public String getMediaType(String epName, String outputTypeKey) throws Exception {
        if (endpoints.containsKey(epName)) {
            Map<String, String> outputTypes = (Map<String, String>)endpoints
                  .get(epName).get(EP_CFGS.outputTypes);

            // Note: do the same operation on outputTypeKey as the setter, e.g. trim
            //       and toUpperCase
            String mediaType = outputTypes.get(outputTypeKey.trim().toUpperCase());
            if (mediaType == null) {
                throw new Exception("WebServiceShell getMediaType, no mediaType"
                      + " found for outputType: " + outputTypeKey
                      + "  on endpoint:" + epName);
            }
            return mediaType;
        }
        throw new Exception("WebServiceShell getMediaType, there is no endpoint"
                      + " configured for endpoint name: " + epName);
	}
	public boolean getUsageLog(String epName) {
        return (boolean)endpoints.get(epName).get(EP_CFGS.usageLog);
	}
	public Boolean getPostEnabled(String epName) {
		return (boolean)endpoints.get(epName).get(EP_CFGS.postEnabled);
	}
	public Boolean getUse404For204(String epName) {
		return (boolean)endpoints.get(epName).get(EP_CFGS.usageLog);
	}
    
    // Note: this implements the rule that the first item in outputTypes
    //       is the default output type
    //       
    public String getDefaultOutputTypeKey(String epName) throws Exception {
        if (endpoints.containsKey(epName)) {
            Map<String, String> types = (Map<String, String>)endpoints
                  .get(epName).get(EP_CFGS.outputTypes);
            
            String defaultOutputTypeKey = (String)types.keySet().toArray()[0];
            
            return defaultOutputTypeKey;
        }
        throw new Exception("WebServiceShell getDefaultOutputTypeKey, there is no endpoint"
                      + " configured for endpoint name: " + epName);
	}

    // ---------------------------------
	public void setOutputTypes(Map<String, String> outTypes, String s)
          throws Exception {
        if (!isOkString(s)) {
			throw new Exception("WebServiceShell setOutputTypes, outputTypes"
                  + " pair values are null or empty string.");
        }
        
        String[] pairs = s.split(java.util.regex.Pattern.quote(","));

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
            outTypes.put(key, oneKV[1].trim());
        }
    }


	// Not required. Might be defaulted elsewhere.


	// Other Getters. Not defaulted

	
	public Boolean isValid() {
		return isValid;
	}

    // split opening of properties file from parameter parsing to
    // make it easier for testing
	public void loadConfigFile(String configBase, ServletContext context)
			throws Exception {

		// Depending on the way the servlet context starts, this can be called
		// multiple
		// times via SingletonWrapper class.
		if (isLoaded) {
			return;
        }
		isLoaded = true;

        Class thisRunTimeClass = this.getClass();
        
        Properties configurationProps = loadPropertiesFile(configBase,
              thisRunTimeClass, SERVICE_CFG_NAME_SUFFIX,
              DEFAULT_SERVICE_FILE_NAME);
        
        if (configurationProps != null) {
            loadConfigurationParameters(configurationProps, context);
        }
    }

	public static Properties loadPropertiesFile(String configBase,
          Class runtTimeClass, String cfgNameSuffix, String defaultCfgName)
			throws Exception {

		Properties configurationProps = new Properties();
		Boolean userConfig = false;

		// Now try to read a user config file from the location specified by the
		// wssConfigDir property concatenated with the web application name
		// (last part of context path), e.g. 'station' or 'dataselect'
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
                + cfgNameSuffix;
            logger.info("Attempting to load application configuration file from: "
                + configFileName);

            try {
                configurationProps.load(new FileInputStream(configFileName));
                userConfig = true;
            } catch (IOException ex) {
                logger.warn("***** could not read cfg file: " + configFileName);
                logger.warn("***** ignoring exception: " + ex);
                logger.warn(warnMsg1);
                logger.warn(warnMsg2);
            }
        } else {
            logger.warn("***** unexpected inputs for cfg file: " + cfgNameSuffix);
            logger.warn(warnMsg1);
            logger.warn(warnMsg2);
        }

		// If no user config was successfully loaded, load the default config file
        // Exception at this point should propagate up.
        if (!userConfig) {
            InputStream inStream = runtTimeClass.getClassLoader()
                .getResourceAsStream(defaultCfgName);
            if (inStream == null) {
                throw new Exception("Default configuration file was not"
                    + " found for name: " + defaultCfgName);
            }
            logger.info("Attempting to load default application"
                + " configuration from here: " + defaultCfgName);

            configurationProps.load(inStream);
            logger.info("Default application properties loaded, file: "
                + defaultCfgName);
        }
        return configurationProps;
    }

	public void loadConfigurationParameters(Properties inputProps,
          ServletContext context)
			throws Exception {

		// ------------------------------------------------------

        loadGlobalParameter(inputProps, globals, GL_CFGS.appName);
        loadGlobalParameter(inputProps, globals, GL_CFGS.appVersion);
        loadGlobalParameter(inputProps, globals, GL_CFGS.corsEnabled);
        loadGlobalParameter(inputProps, globals, GL_CFGS.swaggerV2URL);
        loadGlobalParameter(inputProps, globals, GL_CFGS.wadlPath);
        loadGlobalParameter(inputProps, globals, GL_CFGS.rootServiceDoc);
        loadGlobalParameter(inputProps, globals, GL_CFGS.loggingMethod);
        loadGlobalParameter(inputProps, globals, GL_CFGS.sigkillDelay);
        loadGlobalParameter(inputProps, globals, GL_CFGS.singletonClassName);

        Enumeration keys = inputProps.propertyNames();
        while (keys.hasMoreElements()) {
            String propName = (String)keys.nextElement();      
            System.out.println("**** ---- **** service propName: " + propName
                + "  value: " + inputProps.getProperty(propName));
            
            // by design, in this version of WSS, a global parameter is 
            // defined as a string with no epname. decoration.
            // An endpoint parameter must have a epname. in front of the 
            // WSS to designate endpoint and endpoint parameters
            String[] withDots = propName.split(java.util.regex.Pattern.quote(
                  ENDPOINT_TO_PROPERTIES_DELIMITER));
            if (withDots.length == 1) {
                // should be already loaded, noop
                //System.out.println("*** glb: " + withDots[0]);
            } else if (withDots.length == 2) {
                try {
                    String epName = withDots[0];
                    String inputParmStr = withDots[1];
                    
                    // relying on throwing IllegalArgumentException if it is
                    // not a defined WSS endpoint configuration parameter
                    EP_CFGS inputParm = EP_CFGS.valueOf(inputParmStr);

                    Map<EP_CFGS, Object> endpoint = null;
                    if (endpoints.containsKey(epName)) {
                        endpoint = endpoints.get(epName);
                    } else {
                        endpoint = new HashMap();
                        endpoint.putAll(ep_defaults);
                        endpoints.put(epName, endpoint);
                    }
                    
                    loadEndpointParameter(inputProps, ep_defaults, endpoint,
                          inputParm, propName, context);
                } catch (IllegalArgumentException ex) {
                    System.out.println("****** ignoring ex: " + ex);
                    //throw new Exception("Unrecognized paramater: " + withDots[1], ex);
                }
            } else if (withDots.length > 2) {
                System.out.println("*** ERR *** multiple dots not allowed, key: "
                + propName);
            }
        }


		// ------------------------------------------------------------------;


////		// Load the configuration for the working directory and substitute
////		// System properties and environment properties.
////		valueStr = inputProps.getProperty("handlerWorkingDirectory");
////		if (isOkString(valueStr)) {
////
////			if (!valueStr.matches("/.*|.*\\$\\{.*\\}.*")) {
////				this.workingDirectory = valueStr;
////			} else {
////				Properties props = System.getProperties();
////				for (Object key : props.keySet()) {
////					this.workingDirectory = valueStr.replaceAll("\\$\\{" + key
////							+ "\\}", props.getProperty(key.toString()));
////				}
////				Map<String, String> map = System.getenv();
////				for (String key : map.keySet()) {
////					this.workingDirectory = valueStr.replaceAll("\\$\\{" + key
////							+ "\\}", map.get(key));
////				}
////			}
////
////			// If the working directory is and absolute path then just use it
////			// If it's relative, then reference it to the servlet context.
////			if (!this.workingDirectory.matches("/.*")) {
////				this.workingDirectory = context
////						.getRealPath(this.workingDirectory);
////			}
////
////			File f = new File(this.workingDirectory);
////			if (!f.exists())
////				throw new Exception("Working Directory: "
////						+ this.workingDirectory + " does not exist");
////
////			if (!f.canWrite() || !f.canRead())
////				throw new Exception(
////						"Improper permissions on working Directory: "
////								+ this.workingDirectory);
////		}

		// Finished w/o problems.
		this.isValid = true;
		logger.info(this.toString());
	}
    
	public void loadGlobalParameter(Properties input, Map cfgs, GL_CFGS eKey)
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
                System.out.println("*** *** *** default global value not defined for key: " + key);
                //throw new Exception("Missing required default for parameter: " + key);
            }
        } else {
            // TBD add logging
            if (eKey.equals(GL_CFGS.appName) | eKey.equals(GL_CFGS.appVersion)) {
                System.out.println("*** *** *** Missing required global parameter: " + key);
                //throw new Exception("Missing required global parameter: " + key );
            }
        }
	}

    
	public void loadEndpointParameter(Properties input, Map epDefaults,
          Map endPt, EP_CFGS epParm, String propName, ServletContext context)
          throws Exception {
        
		String newVal = input.getProperty(propName);
        
		if (isOkString(newVal)) {
            // use type defined in a default object to do additional processing
            Object defaultz = epDefaults.get(epParm);
            
            if (defaultz != null) {
                if (defaultz instanceof Boolean) {
                    endPt.put(epParm, Boolean.parseBoolean(newVal));
                } if (defaultz instanceof Integer) {
                    try {
                        endPt.put(epParm, Integer.parseInt(newVal));
                    } catch (NumberFormatException ex) {
                        throw new Exception("Unrecognized Integer for paramater: " + propName
                              + "  value found: " + newVal
                              + "  it should be an integer");
                    }
                } else if(defaultz instanceof IrisStreamingOutput) {
                    endPt.put(epParm, getIrisSOInstance(newVal));
                } else if(defaultz instanceof Map) {
                    if (epParm.equals(EP_CFGS.outputTypes)) {
                        // note: for references to mutable objects,
                        //       dont get the previous value as this
                        //       might look like a concatenation of values
                        //       if there is more than one entry in the
                        //       config file, instead use only this newVal
                        Map<String, String> new_outputTypes = createOutputTypes(newVal);

//                        new_outputTypes.putAll(default_outputTypes);
                        endPt.put(EP_CFGS.outputTypes, new_outputTypes);
                    } else {
                        String msg = "Unexpected Map type for paramater: "
                              + propName + "  value found: " + newVal
                              + "  only handling outputTypes";
                        logger.fatal(msg);
                        throw new Exception(msg);
                    }
                } else {
                    // should be String type if here
                    
                    if (epParm.equals(EP_CFGS.handlerWorkingDirectory)) {
                        newVal = getValidatedWorkingDir(newVal, context);
                    } else {
                        // noop, use newVal as is
                    }
                    endPt.put(epParm, newVal);
                }
            } else {
                // TBD add logging
                System.out.println("*** default value not defined for key: " + epParm + "  input property: " + propName);
                if (epParm.equals(EP_CFGS.irisEndpointClassName) | epParm.equals(EP_CFGS.handlerWorkingDirectory)) {
                    System.out.println("Missing required default for endpoint parameter: " + epParm + "  input property: " + propName);
                    //throw new Exception("Missing required default for endpoint parameter: " + epParm + "  input property: " + propName);
                }
            }
        } else {
            // TBD add logging
            System.out.println("*** property is null or empty for key: " + epParm + "  input property: " + propName);
            if (epParm.equals(EP_CFGS.irisEndpointClassName) | epParm.equals(EP_CFGS.handlerWorkingDirectory)) {
                System.out.println("*** *** *** Missing required endpoint parameter: " + epParm + "  input property: " + propName);
                //throw new Exception("Missing required endpoint parameter: " + epParm + "  input property: " + propName);
            }
        }
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
    
    private String getValidatedWorkingDir(String newVal,
          ServletContext context) throws Exception {
        
        String validVal = newVal;
        if (!validVal.matches("/.*|.*\\$\\{.*\\}.*")) {
                            //this.workingDirectory = valueStr;
            //noop, fall through and put newVal
        } else {
            Properties props = System.getProperties();
            System.out.println("------- ------ --- WD newVal1: " + validVal);
            for (Object key : props.keySet()) {
                //this.workingDirectory = valueStr.replaceAll("\\$\\{" + key
                validVal = validVal.replaceAll("\\$\\{" + key
                      + "\\}", props.getProperty(key.toString()));
            }
            System.out.println("------- ------ --- WD newVal2: " + validVal);
            Map<String, String> map = System.getenv();
            for (String key : map.keySet()) {
                //this.workingDirectory = valueStr.replaceAll("\\$\\{" + key
                validVal = validVal.replaceAll("\\$\\{" + key
                      + "\\}", map.get(key));
            }
            System.out.println("------- ------ --- WD newVal3: " + validVal);
        }

        // If the working directory is an absolute path then just use it
        // If it's relative, then reference it to the servlet context.
//                        if (!newVal.matches("/.*")) {
//                            newVal = context.getRealPath(newVal);
//                        }
// TBD, if not absolute, throw exception
        if (context != null) {System.out.println("------- WARNING --- context realPath: " + context.getRealPath(validVal));}
        else {System.out.println("------- WARNING --- context is null" );}
        System.out.println("------- WARNING --- may need context");
        System.out.println("------- ------ --- WD newVal4: " + validVal);

        File f = new File(validVal);
        if (!f.exists()) {
            throw new Exception("Working Directory: "
                  + validVal + " does not exist");
        }

        if (!f.canWrite() || !f.canRead()) {
            throw new Exception(
                  "Improper permissions on working Directory: "
                  + validVal);
        }

        return validVal;
    }

    private IrisStreamingOutput getIrisSOInstance(String className) {
        Class<?> irisClass = null;
        IrisStreamingOutput iso = null;
        try {
            irisClass = Class.forName(className);
            iso = (IrisStreamingOutput) irisClass.newInstance();
        } catch (ClassNotFoundException ex) {
            String msg = "Could not find "
                  + EP_CFGS.irisEndpointClassName + ": " + className;
            logger.fatal(msg);
            throw new RuntimeException(msg, ex);
        } catch (InstantiationException ex) {
            String msg = "Could not instantiate "
                  + EP_CFGS.irisEndpointClassName + ": " + className;
            logger.fatal(msg);
            throw new RuntimeException(msg, ex);
        } catch (IllegalAccessException ex) {
            String msg = "Illegal access while instantiating "
                  + EP_CFGS.irisEndpointClassName + ": " + className;
            logger.fatal(msg);
            throw new RuntimeException(msg, ex);
        }
        return iso;
    }
	public static boolean isOkString(String s) {
		return ((s != null) && !s.isEmpty());
	}
    
    public static String createEPPropertiesName(String epName, EP_CFGS cfgName) {
        return epName + ENDPOINT_TO_PROPERTIES_DELIMITER + cfgName.toString();
    }

    @Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
        String line = "----------------------";
        
        sb.append("\n");
		sb.append(line).append(" WebServiceShell Configuration").append("\n");

		sb.append(strAppend("WSS Version")).append(wssVersion).append("\n");

        sb.append(line).append(" globals\n");
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
            Object value = globals.get(key) != null ? globals.get(key) : "null";
            sb.append(strAppend(key)).append(value.toString()).append("\n");
        }

        sb.append(line).append(" endpoints\n");
        for (String epName : endpoints.keySet()) {
            Map endpoint = endpoints.get(epName);
            for (EP_CFGS cfgName: (Set<EP_CFGS>)endpoint.keySet()) {
                Object value = endpoint.get(cfgName);
                if (value == null) {
                    value = "null";
                } else if(value instanceof IrisStreamingOutput) {
                    value = value.getClass().getName();
                } else if (value instanceof Map && cfgName.equals(EP_CFGS.outputTypes)) {
                    value = formatOutputTypes((Map<String, String>)value);
                }
                
                sb.append(strAppend(createEPPropertiesName(epName, cfgName)))
                      .append(value).append("\n");
            }
            sb.append("\n");
            
            try {
                sb.append(strAppend(epName + " - default output type"))
                      .append(getDefaultOutputTypeKey(epName)).append("\n");
            } catch (Exception ex) {
                // ignore this, it should have been tested in the testcode
            }
            sb.append("\n");
        }
        sb.append(line).append(" endpoints end\n");


		

		return sb.toString();
	}

	private final int colSize = 40;

	private String strAppend(String s) {
        int colWidth = s.length() >= colSize ? 1 : colSize - s.length();
		for (int i = 0; i < colWidth; i++) {
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

        for (String epName : endpoints.keySet()) {
            Map endpoint = endpoints.get(epName);
            for (EP_CFGS cfgName: (Set<EP_CFGS>)endpoint.keySet()) {
                Object value = endpoint.get(cfgName);
                if (value == null) {
                    value = "null";
                } else if(value instanceof Class) {
                    value = ((Class)value).getName();
                } else if (value instanceof Map && cfgName.equals(EP_CFGS.outputTypes)) {
                    value = formatOutputTypes((Map<String, String>)value);
                }
                
                sb.append("<TR><TD>").append(createEPPropertiesName(epName, cfgName))
                      .append("</TD><TD>").append(value).append("</TD></TR>");
            }
            sb.append("\n");
            
            try {
                sb.append("<TR><TD>" + "Default Output Type Key" + "</TD><TD>")
                      .append(getDefaultOutputTypeKey(epName)).append("</TD></TR>");
            } catch (Exception ex) {
                // ignore this, it should have been tested in the testcode
            }
            sb.append("\n");
        };


		sb.append("</TABLE>");

		return sb.toString();
	}
}
