/*******************************************************************************
 * Copyright (c) 2017 IRIS DMC supported by the National Science Foundation.
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
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.message.internal.MediaTypes;

public class ParameterTranslator {

    // The rawKeySignature string can be used on a the URL request to bypass
    // regular parameter setup and checking. For instance, if this string
    // &ARG=someStr1&ARG=someStr2 is part of the request URL, then the resulting
    // command line will have this " someStr1 someStr2" appended to the end
    // of the line
	public final static String rawKeySignature = "ARG";

	public final static String NODATA_QUERY_PARAMETER = "nodata";
	public final static String usernameSignature = "username";
	public final static String postSignature = "STDIN";

	public static final Logger logger = Logger
			.getLogger(ParameterTranslator.class);

    /**
     *
     * @param cmd - The main output of this method other than exceptions. The given
     *              list is updated with items parsed from the request object.
     *              It was originally designed for command line processing, the
     *              list is key, listObject pairs.
     * @param ri
     * @param epName - In general, use the value returned from a call to
     *                 ri.getEndpointNameForThisRequest()
     * @throws Exception
     */
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

        // Look for any bypass parameters, put them in their own collection and
        // remove them from the well-defined parameter collection
		if (qps.get(rawKeySignature) != null) {
			keysWithNoValue.addAll(qps.get(rawKeySignature));

			while (qps.containsKey(rawKeySignature)) {
				qps.remove(rawKeySignature);
			}
		}

        // look in POST data for a mediaParameter name in order to set the HTTP
        // response media type later, aliases are not checked at this time, 2017-03-08
        if (ri.postBody != null) {
            String specifiedName = ri.appConfig.getMediaParameter(epName);
            if (ri.postBody.contains(specifiedName)) {
                String value = extractValueByKey(ri.postBody, specifiedName);
                if (value != null) {
                    // Note: null implies the specifiedName string is on a comment line
                    qps.add(specifiedName, value);
                }
            }
        }
        // not sure if a postBody and a multipart form can exist in the same
        // query, but if they do, the following multipart will override the
        // previous postBody processing.
        if (ri.postMultipart != null) {
            // where specifiedName is either the default "format" or some
            // other name defined with mediaParameter in respective cfg file
            String specifiedName = ri.appConfig.getMediaParameter(epName);
            Map<String, List<FormDataBodyPart>> fdmpMap = ri.postMultipart.getFields();
            for (String partName : fdmpMap.keySet()) {
                List<FormDataBodyPart> parts = fdmpMap.get(partName);
                for (FormDataBodyPart part : parts) {
                    MediaType partMt = part.getMediaType();
                    if (MediaTypes.typeEqual(MediaType.TEXT_PLAIN_TYPE, partMt)) {
                        if (part.getName().equals(specifiedName)) {
                            // pVal should be the media type of desired output format
                            String pVal = part.getValue();
                            // this should override any value set by simple postbody
                            // or the default formatType
                            qps.add(specifiedName, pVal);
                        }
                    }
                }
            }
        }

		// Iterate over all keys, checking for presence in the paramMap
		// structure. Throw an
		// exception if the param is not present in paramMap.

		keys.addAll(qps.keySet());

        for (String queryKey : keys) {
            // assign whatever is available from the request
            String nonAliasNameKey = queryKey;
            String value = qps.getFirst(queryKey);

            ConfigParam trialCp = ri.paramConfig.getConfigParamValue(epName, queryKey);
            if (trialCp == null && ri.appConfig.isRelaxedValidation(epName)) {
                // i.e. relaxedValidation property is true and a parameter
                // as defined by queryKey is not defined in param.cfg
                //
                // so use the contents of the URL request with no further
                // validation
                //
                // noop - drop and add to output cmd collection
            } else {
                if (qps.get(queryKey).size() > 1) {
                    throw new Exception("Multiple entries for query parameter: "
                          + queryKey);
                }

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
                value = qps.getFirst(queryKey);

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

                // Check to see if this was a 'format' or other equivalent name.
                //  If present AND valid, change the config class's
                // output mime type so that the overall service's output format will
                // change.
                if (nonAliasNameKey.equalsIgnoreCase(
                      ri.appConfig.getMediaParameter(epName))) {
                        ri.setPerRequestFormatType(epName, value);
                }
            }

            // Add key and also value if valid.
            cmd.add("--" + nonAliasNameKey);
            if (AppConfigurator.isOkString(value)) {
                // remove any control characters
                cmd.add(value.replaceAll("\\p{Cntrl}", ""));
            }
        }

        // add the bypass parameters back in for final output
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
     *           or, returns null if key not found, Note: key my be in comment
     *           or, returns not found message if key found but no value
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
            if (s.startsWith("#")) {
                // ignore comment lines
                continue;
            } else {
                if (s.contains(key)) {
                    String[] t = s.split("=");
                    if (t.length > 1) {
                        // take last item as value
                        value = t[t.length - 1];
                    } else if (t.length == 1){
                        value = "valueNotFoundForKey:"+key;
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
