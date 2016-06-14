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

package edu.iris.wss.framework;

import edu.iris.wss.provider.IrisProcessMarker;
import edu.iris.wss.provider.IrisProcessor;
import edu.iris.wss.provider.IrisSingleton;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;


import org.apache.log4j.Logger;

public class AppConfigurator {
	public static final Logger logger = Logger.getLogger(AppConfigurator.class);

	public static final String wssVersion = "2.2";

	public static final String wssDigestRealmnameSignature = "wss.digest.realmname";

	private static final String DEFAULT_SERVICE_FILE_NAME = "META-INF/service.cfg";
	public static final String SERVICE_CFG_NAME_SUFFIX = "-service.cfg";
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

    // store name for possible error message later in process
    private static String configFileName = null;

    public AppConfigurator() throws Exception {
        init();
    }
    
    private void init() throws Exception  {
        // set defaults, all parameters must be set here with
        // defaults of the correct (i.e. desired) object type.
        globals.put(GL_CFGS.appName.toString(), "notnamed");
        globals.put(GL_CFGS.version.toString(), "notversioned");
        globals.put(GL_CFGS.corsEnabled.toString(), true);
        globals.put(GL_CFGS.rootServiceDoc.toString(), null);
        globals.put(GL_CFGS.loggingMethod.toString(), LoggingMethod.LOG4J);
        globals.put(GL_CFGS.loggingConfig.toString(),
              "logging_config_not_specified");
        globals.put(GL_CFGS.sigkillDelay.toString(), 60); // kill delay in seconds
        globals.put(GL_CFGS.singletonClassName.toString(), null);

        // a slight dissonance, endpointClassName will appear as a
        // string externally, but an instatiated object internally
        ep_defaults.put(EP_CFGS.endpointClassName,
              getIrisProcessorInstance("edu.iris.wss.endpoints.CmdProcessor"));
        ep_defaults.put(EP_CFGS.handlerProgram, "nonespecified");
        ep_defaults.put(EP_CFGS.handlerTimeout, 30); // timeout in seconds
        ep_defaults.put(EP_CFGS.handlerWorkingDirectory, "/tmp");
        ep_defaults.put(EP_CFGS.formatTypes, createFormatTypes(""));
        ep_defaults.put(EP_CFGS.usageLog, true);
        ep_defaults.put(EP_CFGS.postEnabled, false);
        ep_defaults.put(EP_CFGS.use404For204, false);
        ep_defaults.put(EP_CFGS.proxyURL, "noproxyURL");
        ep_defaults.put(EP_CFGS.logMiniseedExtents, false);
        ep_defaults.put(EP_CFGS.formatDispositions, createFormatDispositions(""));
        ep_defaults.put(EP_CFGS.addHeaders, createCfgHeaders(""));
    }

    // InternalTypes is an enum of the types supported internally.
    // This is used in the code to
    // identify places in which the external typeKeys specified in the
    // service.cfg file must aggree with the respective items in this enum.
    // An operator must have typeKeys "miniseed" or "mseed" to enable access
    // to miniseed write code, otherwise data is written as binary.
    //
    // BINARY is defined as the default in the init method
    //
    // miniseed is an alias for mseed - to be consistent with FDSN standards
    //
	public static enum InternalTypes {
		MSEED, MINISEED, BINARY
	};
    
    // Make enum names the same as the names the user sees in the cfg file
    // global configuration parameter names
    public static enum GL_CFGS { appName, version, corsEnabled,
        rootServiceDoc, loggingMethod, loggingConfig, sigkillDelay,
        jndiUrl, singletonClassName};
    
    // endpoint configuration parameter names
    public static enum EP_CFGS { formatTypes, handlerTimeout,
        handlerProgram, handlerWorkingDirectory, usageLog, postEnabled, use404For204,
        endpointClassName, proxyURL, logMiniseedExtents, formatDispositions,
        addHeaders
    }

    // container for parameters that apply to all endpoints
    private final Map<String, Object> globals = new HashMap();

    // container for endpoints, an "endpoint" is defined in the cfg file
    // as being the string in front of ENDPOINT_TO_PROPERTIES_DELIMITER
    private final Map<String, Map<EP_CFGS, Object>> endpoints = new HashMap();
    
    // an endpoint, i.e. contains the parameters per endpoint
    private final Map<EP_CFGS, Object> ep_defaults = new HashMap();

	public static enum LoggingMethod {
		LOG4J, JMS, RABBIT_ASYNC
	};

    /**
     *
     * @param newTypes - may be null or empty, if so, then return the default
     *                   list. There must always have at least one element,
     *                   binary is the designated default
     * @return
     * @throws Exception
     */
    public Map<String, String> createFormatTypes(String newTypes)
          throws Exception
    {
        // must use LinkedHashMap or equivalent to preserve order of types
        // to enable using the first element as "the default type"
        Map<String, String> types = new LinkedHashMap<>();

        // set newTypes first so as to preserve order from configuration file
        if (isOkString(newTypes)) {
            setKeyValueMap(types, newTypes, EP_CFGS.formatTypes.toString(), true);
        }

        // set default last
        setKeyValueMap(types, "BINARY: application/octet-stream",
              EP_CFGS.formatTypes.toString(), true);

        return types;
    }

    /**
     *
     * @param newDispositions - key, value pairs in the comma separated list
     * with colon separating key from value, same as createFormatTypes, except
     * the value here is an HTTP header Content-Disposition string.
     *
     * @return
     * @throws Exception
     */
    public Map<String, String> createFormatDispositions(String newDispositions)
          throws Exception
    {
        // I may not needed a linked version, but for now, implementing
        // the same way as formatTypes
        Map<String, String> dispositions = new LinkedHashMap<>();

        if (isOkString(newDispositions)) {
            setKeyValueMap(dispositions, newDispositions,
                 EP_CFGS.formatDispositions.toString(), true);
        }

        return dispositions;
    }

    /**
     * Create map for HTTP headers where the key is the desired HTTP header,
     * (i.e. header field) and the value is the value for that header.
     *
     * The headers map can be empty.
     *
     * @param newHeaders
     * @return
     * @throws Exception
     */
    public Map<String, String> createCfgHeaders(String newHeaders)
          throws Exception
    {
        // I may not needed a linked version, but for now, implementing
        // the same way as formatTypes
        Map<String, String> headers = new LinkedHashMap<>();

        if (isOkString(newHeaders)) {
            setKeyValueMap(headers, newHeaders, EP_CFGS.addHeaders.toString(),
                  false);
        }

        return headers;
    }

    public static String getConfigFileNamed() {
        return configFileName;
    }

	public String getAppName() {
        return (String) globals.get(GL_CFGS.appName.toString());
    }

    public String getAppVersion() {
        return (String) globals.get(GL_CFGS.version.toString());
    }

    public boolean isCorsEnabled() {
        return ((Boolean) globals.get(GL_CFGS.corsEnabled.toString()));
    }

    public String getRootServiceDoc() {
        return (String) globals.get(GL_CFGS.rootServiceDoc.toString());
    }

    public LoggingMethod getLoggingType() {
        return (LoggingMethod) globals.get(GL_CFGS.loggingMethod.toString());
    }

    public String getLoggingConfig() {
        return (String) globals.get(GL_CFGS.loggingConfig.toString());
    }

    public int getSigkillDelay() {
        return ((Integer) globals.get(GL_CFGS.sigkillDelay.toString()));
    }

    public String getSingletonClassName() {
        return (String) globals.get(GL_CFGS.singletonClassName.toString());
    }

    public String getWssVersion() {
        return wssVersion;
    }

    public String getProxyUrl(String epName) {
         return endpoints.get(epName).get(EP_CFGS.proxyURL).toString();
    }

    /**
     * Note: no parameter checking or configuration for endpoint checking
     *       done here, it should be done when parameters are loaded.
     * @param epName
     * @return 
     */
    public IrisProcessMarker getIrisEndpointClass(String epName) {
        return (IrisProcessMarker)endpoints.get(epName)
              .get(EP_CFGS.endpointClassName);
    }
    
    public String getHandlerProgram(String epName) {
        return endpoints.get(epName).get(EP_CFGS.handlerProgram).toString();
    }

	public int getTimeoutSeconds(String epName) {
		return (int)endpoints.get(epName).get(EP_CFGS.handlerTimeout);
	}

    public String getWorkingDirectory(String epName) {
        return endpoints.get(epName).get(EP_CFGS.handlerWorkingDirectory).toString();
    }

    // Note: this can throw NullPointerException and ClassCastException
    public boolean isThisEndpointConfigured(String epName) {
        return endpoints.containsKey(epName);
	}

    public Set<String> getEndpoints() {
        return endpoints.keySet();
	}

    // Note: this can throw NullPointerException and ClassCastException
    public boolean isConfiguredForTypeKey(String epName, String formatTypeKey) {
        Map<String, String> outTypes = (Map<String, String>)endpoints.get(epName)
              .get(EP_CFGS.formatTypes);
        return outTypes.containsKey(formatTypeKey);
	}

    public String getMediaType(String epName, String formatTypeKey)
          throws Exception {
        if (endpoints.containsKey(epName)) {
            Map<String, String> formatTypes = (Map<String, String>)endpoints
                  .get(epName).get(EP_CFGS.formatTypes);

            // Note: do the same operation on formatTypeKey as the setter, e.g. trim
            //       and toUpperCase
            String mediaType = formatTypes.get(formatTypeKey.trim().toUpperCase());
            if (mediaType == null) {
                throw new Exception("WebServiceShell getMediaType, no mediaType"
                      + " found for formatType: " + formatTypeKey
                      + "  on endpoint: " + epName);
            }
            return mediaType;
        }
        throw new Exception("WebServiceShell getMediaType, there is no endpoint"
                      + " configured for endpoint name: " + epName);
	}

    /**
     * There does not have to be a disposition parameter for a given endpoint,
     * nor does there have to be a disposition for a respective format
     *
     * @param epName
     * @param formatTypeKey
     * @return - the caller should skip use of this product when null is
     *           returned
     * @throws Exception
     */
    public String getDisposition(String epName, String formatTypeKey)
          throws Exception {
        if (endpoints.containsKey(epName)) {
            Map<String, String> dispositions = (Map<String, String>)endpoints
                  .get(epName).get(EP_CFGS.formatDispositions);

            // Note: do the same operation on formatTypeKey as the setter, e.g. trim
            //       and toUpperCase
            String disposition = dispositions.get(formatTypeKey.trim().toUpperCase());
            if (null == disposition) {
                // returning null is okay, there is no dispostion configured for
                // this format
            } else {
                disposition = replaceWellKnownNames(disposition);
            }
            return disposition;
        }
        throw new Exception("getDisposition, there is no endpoint"
                      + " configured for endpoint name: " + epName);
	}

    public String replaceWellKnownNames(String input) {
        String output = input.replaceAll("\\$\\{"
              + GL_CFGS.appName.toString() + "\\}", getAppName());
        output = output.replaceAll("\\$\\{"
              + "UTC" + "\\}", Util.getCurrentUTCTimeISO8601());

        return output;
    }

    /**
     * Theses are headers per endpoint from a cfg file. They apply to any
     * format type on the respective endpoint.
     *
     * @param epName
     * @return
     * @throws Exception
     */
    public Map<String, String> getEndpointHeaders(String epName)
          throws Exception {
        if (endpoints.containsKey(epName)) {
            Map<String, String> headers = (Map<String, String>)endpoints
                  .get(epName).get(EP_CFGS.addHeaders);

            Map<String, String> copy = createCfgHeaders("");
            copy.putAll(headers);
            for (String key : copy.keySet()) {
                String newVal = replaceWellKnownNames(copy.get(key));
                copy.put(key, newVal);
            }
            return copy;
        }
        throw new Exception("getCfgHeaders, there is no endpoint"
                      + " configured for endpoint name: " + epName);
	}

    public boolean isUsageLogEnabled(String epName) {
        return (boolean)endpoints.get(epName).get(EP_CFGS.usageLog);
	}

	public boolean isPostEnabled(String epName) {
		return (boolean)endpoints.get(epName).get(EP_CFGS.postEnabled);
	}

	public boolean isUse404For204Enabled(String epName) {
		return (boolean)endpoints.get(epName).get(EP_CFGS.use404For204);
	}

    public boolean isLogMiniseedExtents(String epName) {
        return (boolean)endpoints.get(epName).get(EP_CFGS.logMiniseedExtents);
	}

    // Note: this implements the rule that the first item in formatTypes
    //       is the default output type
    //       
    public String getDefaultFormatTypeKey(String epName) throws Exception {
        if (endpoints.containsKey(epName)) {
            Map<String, String> types = (Map<String, String>)endpoints
                  .get(epName).get(EP_CFGS.formatTypes);

            String defaultFormatTypeKey = (String)types.keySet().toArray()[0];

            return defaultFormatTypeKey;
        }
        throw new Exception(
              "WebServiceShell getDefaultFormatTypeKey, there is no endpoint"
                    + " configured for endpoint name: " + epName);
	}

    /**
     *
     * @param outTypes
     * @param s
     * @param paramName
     * @param setToUpper - Invented this parameter so that this method can be
     *        shared. Only addHeaders should be false. In the case of
     *        addHeaders, the header name file is defined by the user in the
     *        .cfg file, so the idea is to keep the case the same as the user definition.
     *
     * @throws Exception
     */
	public void setKeyValueMap(Map<String, String> outTypes, String s,
          String paramName, boolean setToUpper)
          throws Exception {
        if (!isOkString(s)) {
			throw new Exception("setKeyValueMap, cfg parameter: "
                  + paramName + " pair values are null or empty string.");
        }

        String[] pairs = s.split(java.util.regex.Pattern.quote(","));

        for (String pair : pairs) {
            String[] oneKV = pair.split(java.util.regex.Pattern.quote(":"));
            if (oneKV.length != 2) {
                throw new Exception(
                        "setKeyValueMap could not parse a"
                      + " <key: value> pair for parameter: " + paramName
                      + "  Pairs should be separated by commas."
                      + " Items found: " + oneKV.length
                      + (oneKV.length == 1 ? "  first item: " + oneKV[0] : "")
                      + "  input string in question: " + s);
            }

            String key;
            if (setToUpper) {
                key = oneKV[0].trim().toUpperCase();
            } else {
                key = oneKV[0].trim();
            }
            outTypes.put(key, oneKV[1].trim());
        }
    }

	public Boolean isValid() {
		return isValid;
	}

    // split opening of properties file from parameter parsing to
    // make it easier for testing
	public void loadConfigFile(String configBase)
			throws Exception {

		// Depending on the way the servlet context starts, this can be called
		// multiple times via SingletonWrapper class.
		if (isLoaded) {
			return;
        }
		isLoaded = true;

        Class thisRunTimeClass = this.getClass();
        
        Properties configurationProps = loadPropertiesFile(configBase,
              thisRunTimeClass, SERVICE_CFG_NAME_SUFFIX,
              DEFAULT_SERVICE_FILE_NAME);
        
        if (configurationProps != null) {
            loadConfigurationParameters(configurationProps);
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

        String wssConfigDir = System.getProperty(Util.WSS_OS_CONFIG_DIR);
        
        String warnMsg1 = "***** check for system property "
              + Util.WSS_OS_CONFIG_DIR
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

	public void loadConfigurationParameters(Properties inputProps)
			throws Exception {

		// ------------------------------------------------------

        loadGlobalParameter(inputProps, globals, GL_CFGS.appName);
        loadGlobalParameter(inputProps, globals, GL_CFGS.version);
        loadGlobalParameter(inputProps, globals, GL_CFGS.corsEnabled);
        loadGlobalParameter(inputProps, globals, GL_CFGS.rootServiceDoc);
        loadGlobalParameter(inputProps, globals, GL_CFGS.loggingMethod);
        loadGlobalParameter(inputProps, globals, GL_CFGS.loggingConfig);
        loadGlobalParameter(inputProps, globals, GL_CFGS.sigkillDelay);
        loadGlobalParameter(inputProps, globals, GL_CFGS.singletonClassName);

        Enumeration keys = inputProps.propertyNames();
        while (keys.hasMoreElements()) {
            String propName = (String)keys.nextElement();

            // by design, in this version of WSS, a global parameter is 
            // defined as a string with no epname. decoration.
            // An endpoint parameter must have a epname. in front of the 
            // WSS to designate endpoint and endpoint parameters
            String[] withDots = propName.split(java.util.regex.Pattern.quote(
                  ENDPOINT_TO_PROPERTIES_DELIMITER));
            if (withDots.length == 1) {
                // should be already loaded, noop
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
                        endpoint = new HashMap<EP_CFGS, Object>();
                        endpoint.putAll(ep_defaults);
                        endpoints.put(epName, endpoint);
                    }

                    loadEndpointParameter(inputProps, ep_defaults, endpoint,
                          inputParm, propName);
                } catch (IllegalArgumentException ex) {
                    System.out.println("****** ignoring ex: " + ex);
                    //throw new Exception("Unrecognized paramater: " + withDots[1], ex);
                    logger.warn("unexpected exception: " + ex
                      + "  propName: " + propName);
                }
            } else if (withDots.length > 2) {
                System.out.println("*** ERR *** multiple dots not allowed, key: "
                + propName);
                logger.warn("more than 2 dot delimeters found in endpoint name,"
                      + " found: " + withDots.length + " dots, propName: " + propName);
            }
        }

		// ------------------------------------------------------------------;
        // do additional validation

        for (String epName : endpoints.keySet()) {
            IrisProcessMarker iso = getIrisEndpointClass(epName);

//          cannot do this (for now) because it forces an endpoint to always
//          point to a runnable file, even if the endpoint does not
//          use a handler.
////            if (iso instanceof edu.iris.wss.endpoints.CmdProcessor) {
////                String handlerName = getHandlerProgram(epName);
////                try {
////                    if (isOkString(handlerName)) {
////                        if (isExecutableAndExists(handlerName)) {
////                            continue;
////                        }
////                    }
////                } catch(Exception ex) {
////                    String msg = "Error getting handlerProgram for endpoint: "
////                          + epName + "  ex: " + ex.toString();
////                    logger.error(msg);
////                    throw new Exception(msg, ex);
////                }
////            } else if (iso instanceof edu.iris.wss.endpoints.ProxyResource) {
            if (iso instanceof edu.iris.wss.endpoints.ProxyResource) {
                String resoureToProxyURL = getProxyUrl(epName);
                try {
                    if (isOkString(resoureToProxyURL)) {
                        URL url = new URL(resoureToProxyURL);
                        InputStream is = url.openStream();
                    }
                } catch(Exception ex) {
                    String msg = EP_CFGS.proxyURL.toString()
                          + " error for endpoint: " + epName
                          + "  ex: " + ex.toString();
                    logger.error(msg);
                    throw new Exception(msg, ex);
                }
            }
        }

		// Finished without problems.
		this.isValid = true;
		logger.info(this.toString());
	}

    public static boolean isExecutableAndExists(String filename) throws Exception {
        File f = new File(filename);
        if (!f.exists()) {
            throw new Exception("file: " + filename + " does not exist");
        }

        if (!f.canExecute()) {
            throw new Exception("file: " + filename + " is not executable");
        }

       return true;
    }

    public void loadGlobalParameter(Properties input, Map cfgs, GL_CFGS eKey)
          throws Exception {
        
        String key = eKey.toString();
		String newVal = input.getProperty(key);
        
		if (isOkString(newVal)) {
            // use type of object set in default map to choose processing
            Object currentVal = cfgs.get(key);
            
//            I am currently allowing null values for some things and assume
//            that the else clause will handle input from properties file
//            NOTE: this only works for Strings
//            if (currentVal != null) {
                if (currentVal instanceof Boolean) {
                    cfgs.put(key, Boolean.valueOf(newVal));
                } else if (currentVal instanceof Integer) {
                    try {
                        cfgs.put(key, Integer.valueOf(newVal));
                    } catch (NumberFormatException ex) {
                        throw new Exception("Unrecognized value for paramater: " + key
                              + "  value found: " + newVal
                              + "  it should be an integer");
                    }
                } else if(currentVal instanceof LoggingMethod) {
                    try {
                        LoggingMethod trial = LoggingMethod.valueOf(newVal.toUpperCase());
                        cfgs.put(key, trial);
                    } catch (Exception ex) {
                        throw new Exception("Unrecognized value for paramater: " + key
                              + "  value found: " + newVal
                              + "  should be one of " + LoggingMethod.LOG4J.toString()
                              + " or " + LoggingMethod.JMS.toString(), ex);
                    }
                } else if(currentVal instanceof URL) {
                    try {
                        URL trial = new URL(newVal);
                        System.out.println("------------------------000--- newVal: " + newVal);
                        System.out.println("------------------------000--- trial: " + trial);
                        cfgs.put(key, trial);
                    } catch (Exception ex) {
                        throw new Exception("Error for URL paramater: " + key
                              + "  value found: " + newVal
                              + "  it must be a well formed URL", ex);
                    }
                } else {
                    // should be String type if here in else

                    cfgs.put(key, newVal);
                }
//            } else {
//                // TBD add logging
//                System.out.println("*** *** *** default global value not defined for key: " + key);
//                //throw new Exception("Missing required default for parameter: " + key);
//            }
        } else {
            String msg =
                  "The service cfg file did not contain a valid value for parameter: "
                  + key + "  value: " + newVal;
            logger.info(msg);
//            if (eKey.equals(GL_CFGS.appName) | eKey.equals(GL_CFGS.version)) {
//                System.out.println("*** *** *** TBD - is this needed and for what - Missing required global parameter: " + key);
//                //throw new Exception("Missing required global parameter: " + key );
//            }
        }
	}

	public void loadEndpointParameter(Properties input, Map epDefaults,
          Map<EP_CFGS, Object> endPt, EP_CFGS epParm, String propName)
          throws Exception {
        
		String newVal = input.getProperty(propName);
        
		if (isOkString(newVal)) {
            // use type defined in a default object to do additional processing
            Object defaultz = epDefaults.get(epParm);

            if (defaultz != null) {
                if (defaultz instanceof Boolean) {
                    endPt.put(epParm, Boolean.valueOf(newVal));
                } else if (defaultz instanceof Integer) {
                    try {
                        endPt.put(epParm, Integer.valueOf(newVal));
                    } catch (NumberFormatException ex) {
                        throw new Exception("Unrecognized Integer for paramater: "
                              + propName + "  value found: " + newVal
                              + "  it should be an integer");
                    }
                } else if(defaultz instanceof IrisProcessMarker) {
                    try {
                        Object ipm = getClassInstance(newVal, IrisProcessMarker.class);
                        endPt.put(epParm, ipm);
                    } catch (Exception ex) {
                        String msg = "loadEndpointParameter error, ex: "
                              + ex.getMessage();
                        logger.fatal(msg);
                        throw new Exception(msg, ex);
                    }
                } else if(defaultz instanceof Map) {
                    if (epParm.equals(EP_CFGS.formatTypes)) {
                        // note: Don't use the map from from the default object,
                        //       create a new one.
                        Map<String, String> newmap = createFormatTypes(newVal);
                        endPt.put(EP_CFGS.formatTypes, newmap);
                    } else if (epParm.equals(EP_CFGS.formatDispositions)) {
                        // note: Don't use the map from from the default object,
                        //       create a new one.
                        Map<String, String> newmap = createFormatDispositions(newVal);
                        endPt.put(EP_CFGS.formatDispositions, newmap);
                    } else if (epParm.equals(EP_CFGS.addHeaders)) {
                        // note: Don't use the map from from the default object,
                        //       create a new one.
                        Map<String, String> newmap = createCfgHeaders(newVal);
                        endPt.put(EP_CFGS.addHeaders, newmap);
                    } else {
                        String msg = "Unexpected Map type for paramater: "
                              + propName + "  value found: " + newVal
                              + "  only handling formatTypes";
                        logger.fatal(msg);
                        throw new Exception(msg);
                    }
                } else {
                    // should be String type if here
                    if (epParm.equals(EP_CFGS.handlerWorkingDirectory)) {
                        newVal = getValidatedWorkingDir(newVal);
                    } else {
                        // noop, use newVal as is
                    }
                    endPt.put(epParm, newVal);
                }
            } else {
                String msg = "*** default value not defined for key: "
                      + epParm + "  input property: " + propName;
                System.out.println(msg);
                logger.warn(msg);

                // code for required parameters?
                if (epParm.equals(EP_CFGS.endpointClassName)
                      | epParm.equals(EP_CFGS.handlerWorkingDirectory)) {
                    System.out.println("Missing required default for endpoint parameter: "
                          + epParm + "  input property: " + propName);
                    //throw new Exception("Missing required default for endpoint parameter: "
                    //+ epParm + "  input property: " + propName);
                }
            }
        } else {
            String msg = "*** property is null or empty for key: "
                  + epParm + "  input property: " + propName;
            System.out.println(msg);
            logger.warn(msg);

            // code for required parameters?
            if (epParm.equals(EP_CFGS.endpointClassName)
                  | epParm.equals(EP_CFGS.handlerWorkingDirectory)) {
                System.out.println("*** *** *** Missing required endpoint parameter: "
                      + epParm + "  input property: " + propName);
                //throw new Exception("Missing required endpoint parameter: " + epParm
                //+ "  input property: " + propName);
            }
        }
	}

    private static String toStringMapStringTypes(Map<String, String> stringMaps) {
        StringBuilder s = new StringBuilder();
        
        if (null != stringMaps) {
            if (stringMaps.isEmpty()) {
                s.append("\"\"");
            } else {
                Iterator<String> keyIt = stringMaps.keySet().iterator();
                while(keyIt.hasNext()) {
                    String key = keyIt.next();
                    s.append(key).append(": ").append(stringMaps.get(key));
                    if (keyIt.hasNext()) {
                        s.append(", ");
                    }
                }
            }
        } else {
            s.append("null");
        }
        
        return s.toString();
    }

    protected String getValidatedWorkingDir(String newVal) throws Exception {
        
        String validVal = newVal;
        if (!validVal.matches("/.*")) {
            throw new Exception(
                  "Working Directory must be an absolute path, value found: "
                  + validVal);
        }

        // for backward compatability, support embedded ${key} values
        // from Java system properties or environment variables
        Properties props = System.getProperties();
        for (Object key : props.keySet()) {
            //this.workingDirectory = valueStr.replaceAll("\\$\\{" + key
            validVal = validVal.replaceAll("\\$\\{" + key
                  + "\\}", props.getProperty(key.toString()));
        }
        Map<String, String> map = System.getenv();
        for (String key : map.keySet()) {
            //this.workingDirectory = valueStr.replaceAll("\\$\\{" + key
            validVal = validVal.replaceAll("\\$\\{" + key
                  + "\\}", map.get(key));
        }

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

    public static Object getClassInstance(String className,
          Class expectedClass) {
        Object iso = null;

        // The exception objects don't include the exception name in the
        // message, so get the class type with this.
        Exception exObj = null;

        try {
            iso = expectedClass.cast(Class.forName(className).newInstance());
        } catch (ClassNotFoundException ex) {
            exObj = ex;
        } catch (InstantiationException ex) {
            exObj = ex;
        } catch (IllegalAccessException ex) {
            exObj = ex;
        } catch (ClassCastException ex) {
            exObj = ex;
        }

        if (null == iso) {
            String msg = exObj.getClass() + ", className: " + className
                  + "  expected Class type: " + expectedClass.getName();
            throw new RuntimeException(msg, exObj);
        }

        return iso;
    }

    public static IrisProcessMarker getIrisProcessorInstance(String className) {
        Class<?> irisClass = null;
        IrisProcessor iso = null;
        try {
            irisClass = Class.forName(className);
            iso = (IrisProcessor) irisClass.newInstance();
        } catch (ClassNotFoundException ex) {
            String msg = "getIrisProcessorInstance could not find "
                  + EP_CFGS.endpointClassName + ": " + className;
            logger.fatal(msg);
            throw new RuntimeException(msg, ex);
        } catch (InstantiationException ex) {
            String msg = "getIrisProcessorInstance could not instantiate "
                  + EP_CFGS.endpointClassName + ": " + className;
            logger.fatal(msg);
            throw new RuntimeException(msg, ex);
        } catch (IllegalAccessException ex) {
            String msg = "getIrisProcessorInstance illegal access while instantiating "
                  + EP_CFGS.endpointClassName + ": " + className;
            logger.fatal(msg);
            throw new RuntimeException(msg, ex);
        } catch (ClassCastException ex) {
            String msg = "getIrisProcessorInstance ClassCastException while instantiating "
                  + EP_CFGS.endpointClassName + ": " + className;
            logger.fatal(msg);
            throw new RuntimeException(msg, ex);
        }
        return iso;
    }

    public static IrisSingleton getIrisSingletonInstance(String className) {
        Class<?> irisClass = null;
        IrisSingleton is = null;
        try {
            irisClass = Class.forName(className);
            logger.info("--- Create new instance of class: " + irisClass.getCanonicalName());
            is = (IrisSingleton) irisClass.newInstance();
            logger.info("--- End create of new instance of class: " + irisClass.getCanonicalName());
        } catch (ClassNotFoundException ex) {
            String msg = "getIrisSingletonInstance could not find "
                  + GL_CFGS.singletonClassName + ": " + className;
            logger.fatal(msg);
            throw new RuntimeException(msg, ex);
        } catch (InstantiationException ex) {
            String msg = "getIrisSingletonInstance could not instantiate "
                  + GL_CFGS.singletonClassName + ": " + className;
            logger.fatal(msg);
            throw new RuntimeException(msg, ex);
        } catch (IllegalAccessException ex) {
            String msg = "getIrisSingletonInstance illegal access while instantiating "
                  + GL_CFGS.singletonClassName + ": " + className;
            logger.fatal(msg);
            throw new RuntimeException(msg, ex);
        } catch (ClassCastException ex) {
            String msg = "getIrisSingletonInstance ClassCastException while instantiating "
                  + EP_CFGS.endpointClassName + ": " + className;
            logger.fatal(msg);
            throw new RuntimeException(msg, ex);
        }
        return is;
    }

	public static boolean isOkString(String s) {
		return ((s != null) && !s.isEmpty());
	}
    
    public static String createEPdotPropertyName(String epName, EP_CFGS cfgName) {
        return epName + ENDPOINT_TO_PROPERTIES_DELIMITER + cfgName.toString();
    }

    @Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
        String line = "----------------------";
        
        sb.append("\n");
		sb.append(line).append(" Web Service Shell Service Configuration").append("\n");

		sb.append(strAppend("Web Service Shell Version")).append(wssVersion).append("\n");

        sb.append(line).append(" globals\n");
        List<String> keyList = new ArrayList();
        keyList.add(GL_CFGS.appName.toString());
        keyList.add(GL_CFGS.version.toString());
        keyList.add(GL_CFGS.corsEnabled.toString());
        keyList.add(GL_CFGS.rootServiceDoc.toString());
        keyList.add(GL_CFGS.loggingMethod.toString());
        keyList.add(GL_CFGS.loggingConfig.toString());
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
                } else if(value instanceof IrisProcessMarker) {
                    value = value.getClass().getName();
                } else if (value instanceof Map &&
                      (cfgName.toString().equals(EP_CFGS.formatTypes.toString())
                      || cfgName.toString().equals(EP_CFGS.formatDispositions.toString())
                      || cfgName.toString().equals(EP_CFGS.addHeaders.toString())
                      )) {
                    value = toStringMapStringTypes((Map<String, String>)value);
                }
                
                sb.append(strAppend(createEPdotPropertyName(epName, cfgName)))
                      .append(value).append("\n");
            }
            sb.append("\n");
            
            try {
                sb.append(strAppend(epName + " - default output type"))
                      .append(getDefaultFormatTypeKey(epName)).append("\n");
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

		sb.append("<TR><TH colspan=\"2\" >")
              .append("Web Service Shell Configuration")
              .append("</TH></TR>");

        sb.append("<TR><TH colspan=\"2\" >")
              .append("global paramaters")
              .append("</TH></TR>");

        List<String> keyList = new ArrayList();
        keyList.add(GL_CFGS.appName.toString());
        keyList.add(GL_CFGS.version.toString());
        keyList.add(GL_CFGS.corsEnabled.toString());
        keyList.add(GL_CFGS.rootServiceDoc.toString());
        keyList.add(GL_CFGS.loggingMethod.toString());
        keyList.add(GL_CFGS.loggingConfig.toString());
        keyList.add(GL_CFGS.sigkillDelay.toString());
        keyList.add(GL_CFGS.singletonClassName.toString());
        
        for (String key: keyList) {
            Object value = globals.get(key) != null ? globals.get(key) : "null";
            sb.append("<TR><TD>").append(key).append("</TD><TD>")
                    .append(value.toString()).append("</TD></TR>");
        }

        for (String epName : endpoints.keySet()) {
            sb.append("<TR><TH colspan=\"2\" >")
                  .append("endpoint: ")
                  .append(epName)
                  .append("</TH></TR>");
            
            Map endpoint = endpoints.get(epName);
            for (EP_CFGS cfgName: (Set<EP_CFGS>)endpoint.keySet()) {
                Object value = endpoint.get(cfgName);
                if (value == null) {
                    value = "null";
                } else if(value instanceof IrisProcessMarker) {
                    value = value.getClass().getName();
                } else if (value instanceof Map &&
                      (cfgName.toString().equals(EP_CFGS.formatTypes.toString())
                      || cfgName.toString().equals(EP_CFGS.formatDispositions.toString())
                      || cfgName.toString().equals(EP_CFGS.addHeaders.toString())
                      )) {
                    value = toStringMapStringTypes((Map<String, String>)value);
                }

                sb.append("<TR><TD>")
                      .append(createEPdotPropertyName(epName, cfgName))
                      .append("</TD><TD>")
                      .append(value)
                      .append("</TD></TR>");
            }

            try {
                sb.append("<TR><TD>")
                      .append("Default Output Type Key")
                      .append("</TD><TD>")
                      .append(getDefaultFormatTypeKey(epName))
                      .append("</TD></TR>");
            } catch (Exception ex) {
                // ignore this, it should have been tested in the testcode
            }
        }

		sb.append("</TABLE>");

		return sb.toString();
	}
}
