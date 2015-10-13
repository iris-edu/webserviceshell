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

import java.util.HashMap;
import java.util.Properties;


import org.apache.log4j.Logger;

import edu.iris.wss.framework.ParamConfigurator.ConfigParam.ParamType;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;

public class ParamConfigurator {
	public static final Logger logger = Logger.getLogger(ParamConfigurator.class);
	
    private static final String DEFAULT_PARAM_FILE_NAME = "META-INF/param.cfg";
    private static final String PARAM_CFG_NAME_SUFFIX = "-param.cfg";
    private static final String ALIASES_KEY_NAME = "aliases";

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

    public ParamConfigurator(Set<String> epNames) {
        for (String epName : epNames) {
            // create an empty param map per endpoint found in AppConfigurator
            epParams.put(epName, new HashMap<String, ConfigParam>());

            // create an empty Aliases map per endpoint found in AppConfigurator
            epAliases.put(epName, new HashMap<String, String>());
        }
    }

	private Boolean isLoaded = false;

    private Map<String, Map<String, ConfigParam>> epParams = new HashMap<>();;

    // A map of aliases pointing to their respective parameter name
    // need empty map in case there are no aliases
    private Map<String, Map<String, String>> epAliases = new HashMap<>();

	public ConfigParam getConfigParamValue(String epName, String key) {
        if (epParams.containsKey(epName)) {
            return epParams.get(epName).get(key);
        } else {
            return null;
        }
	}

    public boolean containsParamAlias(String epName, String inputName) {
        if (epAliases.containsKey(epName)) {
            return epAliases.get(epName).containsKey(inputName);
        }
        return false;
    }

    public String getParamFromAlias(String epName, String inputName) {
        return epAliases.get(epName).get(inputName);
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

        if (configurationProps != null) {
            loadConfigurationParameters(configurationProps);
        }
    }

	public void loadConfigurationParameters(Properties inputProps)
			throws Exception {

        Enumeration keys = inputProps.propertyNames();
        while (keys.hasMoreElements()) {
            String propName = (String)keys.nextElement();

            String[] withDots = propName.split(java.util.regex.Pattern.quote(
                  AppConfigurator.ENDPOINT_TO_PROPERTIES_DELIMITER));
            if (withDots.length == 2) {
                String epName = withDots[0];
                String paramName = withDots[1];

                if (!epParams.containsKey(epName)) {
                    logger.warn("Ignoring property when endpoint name is not"
                          + " previously defined in the service cfg parameters,"
                          + " ignored epName: " + epName + "  propName: " + propName);
                    continue;
                }

                // evedently this can never be triggered as loading Properties
                // appears to only keep the last entry when multiple entires
                // occurr
                if (epParams.get(epName).containsKey(paramName)) {
                    throw new Exception("Duplicated config parameter: "
                          + paramName + "  on endpoint: " + epName);
                }

                // paramValue should be a type of representation, unless
                // it is an alias definition property
                String paramValue = (String) inputProps.get(propName);
                if (!AppConfigurator.isOkString(paramValue)) {
                    paramValue = null;
                }

                if (paramName.equals(ALIASES_KEY_NAME)) {
                    // replace empty map rather than update
                    epAliases.put(epName, createAliasesMap(paramValue));
                } else {
                    ParamType paramType;
                    try {
                        paramType = ParamType.valueOf(paramValue.toUpperCase());
                    } catch (Exception e) {
                        throw new Exception("Unrecognized param type: "
                              + paramValue + "  on paramName: " + paramName
                              + "  on endpoint: " + epName);
                    }

                    epParams.get(epName).put(paramName,
                          new ConfigParam(paramName, paramType));
                }
            } else {
                logger.warn("Ignoring property with no endpoint defined,"
                      + " input: " + propName);
            }
        }
        logger.info("parameters loaded - \n" + this.toString());
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

    @Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("WSS Parameter Configuration\n");

        for (String epName : epParams.keySet()) {
            Map<String, ConfigParam> params = epParams.get(epName);
            for (String key: params.keySet()) {
                ConfigParam cp = params.get(key);
                sb.append(epName)
                      .append(AppConfigurator.ENDPOINT_TO_PROPERTIES_DELIMITER)
                      .append(strAppend(cp.name))
                      .append(cp.type)
                      .append("\n");
            }

            Map aliases = epAliases.get(epName);
            if (aliases.isEmpty()) {
                // noop, skip
            } else {
                sb.append(epName)
                      .append(AppConfigurator.ENDPOINT_TO_PROPERTIES_DELIMITER)
                      .append(strAppend(ALIASES_KEY_NAME))
                      .append(aliases)
                      .append("\n");
            }
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

        for (String epName : epParams.keySet()) {
            Map<String, ConfigParam> params = epParams.get(epName);
            for (String key: params.keySet()) {
                ConfigParam cp = params.get(key);
                sb.append("<TR><TD>")
                      .append(epName)
                      .append(AppConfigurator.ENDPOINT_TO_PROPERTIES_DELIMITER)
                      .append(strAppend(cp.name))
                      .append("</TD><TD>")
                      .append(cp.type)
                      .append("</TD></TR>");
            }

            Map aliases = epAliases.get(epName);
            sb.append("<TR><TD>")
                  .append(epName)
                  .append(AppConfigurator.ENDPOINT_TO_PROPERTIES_DELIMITER)
                  .append(ALIASES_KEY_NAME )
                  .append("</TD><TD>")
                  .append(aliases)
                  .append("</TD></TR>");
        }

		sb.append("</TABLE>");

		return sb.toString();
	}
}
