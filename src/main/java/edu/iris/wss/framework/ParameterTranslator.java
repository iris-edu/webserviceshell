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

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.log4j.Logger;

import edu.iris.wss.framework.ParamConfigurator.ConfigParam;
import edu.iris.wss.utils.WebUtils;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import javax.ws.rs.core.MultivaluedHashMap;

public class ParameterTranslator {

	// This rawKeySignature is the string that a URI query parameter must have
	// for the 'name'
	// portion when you don't wan the 'value' portion to be rendered into the
	// command line
	// command. I.e. say ..?name1=val1&ARG=foo... to get -name1 val1 foo. I.e.,
	// "foo" stands
	// alone w/o a 'option'.

	public final static String rawKeySignature = "ARG";

	// Output is being deprecated, but will be allowable for now.
	// Since they perform the same function, the last seen entry in the param
	// config
	// will be used.
	public final static String outputControlSignature1 = "output";
	public final static String outputControlSignature2 = "format";

	public final static String NODATA_QUERY_PARAMETER = "nodata";
	public final static String usernameSignature = "username";
	public final static String postSignature = "STDIN";

	public static final Logger logger = Logger
			.getLogger(ParameterTranslator.class);

	public static void parseQueryParams(ArrayList<String> cmd, RequestInfo ri,
          String epName)
			throws Exception {

		List<String> keys = new ArrayList<>();
		List<String> keysWithNoValue = new ArrayList<>();

        // for Jersey 2.x, the structure returned from getQueryParameters()
        // is now immutable, so make a local copy
        MultivaluedMap<String, String> qps_immutable = ri.uriInfo.getQueryParameters();
        MultivaluedMap<String, String> qps = new MultivaluedHashMap<>();
        Set<String> mmKeys = qps_immutable.keySet();
        for (String mmkey : mmKeys) {
            qps.addAll(mmkey, qps_immutable.get(mmkey));
        }

		// Special 'username' cli argument will be added if present

		String username = WebUtils.getAuthenticatedUsername(ri.requestHeaders);
		if (AppConfigurator.isOkString(username)) {
			cmd.add("--" + usernameSignature);
			cmd.add(username);
		}

		// Check for the nodata query parameter and the app config setting
		// appropriately.
		String nodataVal = qps.getFirst(NODATA_QUERY_PARAMETER);
		if (AppConfigurator.isOkString(nodataVal)) {
			qps.remove(NODATA_QUERY_PARAMETER);

			if (nodataVal.equals("204")) {
				ri.perRequestUse404for204 = false;
			} else if (nodataVal.equals("404")) {
				ri.perRequestUse404for204 = true;
			} else {
				throw new Exception("Invalid value for " + NODATA_QUERY_PARAMETER
						+ ": " + nodataVal);
			}
		}

		if (ri.postBody != null) {
			cmd.add("--" + postSignature);
		}

		// Since the query parameters aren't going to come out of the Map
		// structure in any meaningful
		// way, we need some way to deal with non parameterized
		// Parse query parameter map, adding all key / value pairs to the
		// command line and storing
		// away any keys that don't have values to be appended later.

		if (qps.get(rawKeySignature) != null) {
			keysWithNoValue.addAll(qps.get(rawKeySignature));

			while (qps.containsKey(rawKeySignature)) {
				qps.remove(rawKeySignature);
			}
		}
        
        // look in post data for parameters that affect HTTP response header type,
        // at this time, 2014-06-20, "format" and "output"
        if (ri.postBody != null) {
            String key = outputControlSignature1;
            if (ri.postBody.contains(key)) {
                String value = extractValueByKey(ri.postBody, key);
                qps.add(key, value);
            }

            key = outputControlSignature2;
            if (ri.postBody.contains(key)) {
                String value = extractValueByKey(ri.postBody, key);
                qps.add(key, value);
            }
        }

		// Iterate over all keys, checking for presence in the paramMap
		// structure. Throw an
		// exception if the param is not present in paramMap.

		keys.addAll(qps.keySet());

        for (String queryKey : keys) {

			if (qps.get(queryKey).size() > 1) {
				throw new Exception("Multiple entries for query parameter: "
                      + queryKey);
            }
            
            String nonAliasNameKey;
            if (ri.paramConfig.containsParamAlias(epName, queryKey)) {
                nonAliasNameKey = ri.paramConfig.getParamFromAlias(epName, queryKey);
                if (nonAliasNameKey != null) {
                    if (qps.containsKey(nonAliasNameKey)) {
                        throw new Exception(
                              "Multiple parameters found from alias parameter: "
                              + queryKey + "  an alias for parameter: " + nonAliasNameKey
                              + "  on endpoint: " + epName);
                    }
                } else {
                    throw new Exception("undefined alias parameter: "
                            + queryKey + "  on endpoint: "+ epName);
                }
            } else {
                nonAliasNameKey = queryKey;
            }

			ConfigParam cp = ri.paramConfig.getConfigParamValue(epName, nonAliasNameKey);
			if (cp == null) {
				throw new Exception("No type defined or unknown query parameter: "
                      + queryKey);
			}

			// note: should only be one value, duplicate checks should be
            //       performed before this assignment
			String value = qps.getFirst(queryKey);

			// Test if param type is OK. DATE, NUMBER, TEXT, BOOLEAN
			switch (cp.type) {
			case NONE:
				if (AppConfigurator.isOkString(value)) {
					throw new Exception("No value permitted for " + queryKey
							+ " Found value: " + value);
				}
				break;

			case DATE:
				if (!isValidFdsnDate(value)) {
					throw new Exception("Bad date value for " + queryKey + ": "
							+ value);
				}
				break;

			case NUMBER:
				if (!isValidFdsnDecimal(value)) {
					throw new Exception("Bad numeric value for " + queryKey + ": "
							+ value);
				}
				break;

			case BOOLEAN:
				if (!value.equalsIgnoreCase("true")
						&& !value.equalsIgnoreCase("false"))
					throw new Exception("Bad boolean value for " + queryKey + ": "
							+ value);
				break;

			case TEXT:
				if (!AppConfigurator.isOkString(value)) {
					throw new Exception("Invalid value for parameter: " + queryKey);
				}
				break;
			}

			// Check to see if this was the 'format' parameter. If present AND
			// valid, change the config class's
			// output mime type so that the overall service's output format will
			// change.
			if (nonAliasNameKey.equalsIgnoreCase(outputControlSignature1)
					|| nonAliasNameKey.equalsIgnoreCase(outputControlSignature2)) {
                
				ri.setPerRequestFormatType(epName, value);
			}

			// Add key and also value if valid.
			cmd.add("--" + nonAliasNameKey);
			if (AppConfigurator.isOkString(value)) {
                // remove any control characters
				cmd.add(value.replaceAll("\\p{Cntrl}", ""));
			}
		}

		for (String rawArg : keysWithNoValue) {
			cmd.add(rawArg);
		}

		// logger.info("CMD: " + cmd);
	}
    

    /**
     * Method to extract the value of the input key in a string where
     * at least the value of interest is in a key=value form. It is expected
     * to work on plain text or application/x-www-form-urlencoded text.
     * Also expects newline separators between key-value pairs and network,
     * station, etc lines.
     * 
     * @param postBody - the text to parse
     * @param key - the text to search for
     * @return - the value for the respective key,
     *           or, returns null if key not found,
     *           or, reeturns empty string if parameter found but no value
     * @throws Exception 
     */
    static String extractValueByKey(String postBody, String key)
        throws UnsupportedEncodingException, IllegalArgumentException {
        String value = null;
        String urlDecoded = null;
        try {
            urlDecoded = java.net.URLDecoder.decode(postBody, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new UnsupportedEncodingException("ParameterTranslator.extractValueByKey"
                + " unable to URLDecode postBody: "
                + postBody);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("ParameterTranslator.extractValueByKey"
                + " unable to URLDecode postBody: "
                + postBody, ex);
        }

        // assumes properties may be separated by carriage return newline,
        //after URLDecoding
        
        // ignore carriage return so as to work with either plain text or url decoded
        urlDecoded = urlDecoded.replaceAll("\\r", "");
            
        String[] itemsByLine = urlDecoded.split("\\n");
        for (String s : itemsByLine) {
            if (s.contains(key)) {
                String[] t = s.split("=");
                if (t.length > 1) {
                    // take last item as value
                    value = t[t.length - 1];
                } else if (t.length == 1){
                    value = "";
                    logger.warn("In parsing POST body for key: " + key
                        + "  a value was not found, postBody: " + postBody);
                } else {
                    // maybe never get here, but just in case
                     throw new IllegalArgumentException("ParameterTranslator.extractValueByKey"
                        + " parameter list postBody: "
                        + postBody);
               }

                break;
            }
        }

        return value;
    }

	private static String[] regexpStrings = {
			"^\\d{4}-[01]\\d-[0-3]\\d[T ][0-2]\\d:[0-5]\\d:[0-5]\\d\\.\\d+(Z)?$",
			"^\\d{4}-[01]\\d-[0-3]\\d[T ][0-2]\\d:[0-5]\\d:[0-5]\\d(Z)?$",
			"^\\d{4}-[01]\\d-[0-3]\\d(Z)?$" };

	public static Boolean isValidFdsnDate(String dateString) {
		for (String regexp : regexpStrings) {
			if (dateString.trim().matches(regexp))
				return true;
		}
		return false;
	}

	private static Boolean isValidFdsnDecimal(String decString) {
		String regexp = "^[-+]?\\d*\\.?\\d*$";
		if (decString.trim().matches(regexp))
			return true;

		return false;
	}
}
