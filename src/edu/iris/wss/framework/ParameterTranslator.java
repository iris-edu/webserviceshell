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


import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.log4j.Logger;

import edu.iris.wss.framework.ParamConfigurator.ConfigParam;
import edu.iris.wss.utils.WebUtils;

public class ParameterTranslator {
	
	// This rawKeySignature is the string that a URI query parameter must have for the 'name' 
	// portion when you don't wan the 'value' portion to be rendered into the command line 
	// command.  I.e. say ..?name1=val1&ARG=foo... to get -name1 val1 foo.  I.e., "foo" stands 
	// alone w/o a 'option'.
	
	public final static String rawKeySignature = "ARG";
	
	// Output is being deprecated, but will be allowable for now.
	// Since they perform the same function, the last seen entry in the param config
	// will be used.
	public final static String outputControlSignature = "output";

	public final static String nodataSignature = "nodata";
	public final static String usernameSignature = "username";
	public final static String postSignature = "STDIN";
	
	public static final Logger logger = Logger.getLogger(ParameterTranslator.class);

	public static  void parseQueryParams(ArrayList<String> cmd, RequestInfo ri) throws Exception {
		
		List<String> keys = new ArrayList<String>();
		List<String> keysWithNoValue = new ArrayList<String>();
		
		MultivaluedMap<String, String> qps = ri.uriInfo.getQueryParameters();
		
		// Special 'username' cli argument will be added if present
		
		String username = WebUtils.getAuthenticatedUsername(ri.requestHeaders);
		if (isOkString(username)) {
			cmd.add("--" + usernameSignature);
			cmd.add(username);
		}
		
		// Check for the nodata query parameter and the app config setting appropriately.
		String nodataVal = qps.getFirst(nodataSignature);
		if (isOkString(nodataVal)) { 
			qps.remove(nodataSignature);
			
			if (nodataVal.equals("204")) {
				ri.perRequestUse404for204 = false;
			} else if (nodataVal.equals("404")) {
				ri.perRequestUse404for204 = true;
			} else {
				throw new Exception("Invalid value for " + nodataSignature + ": " + nodataVal);
			}
		}
		
		if (ri.postBody != null) {
			cmd.add("--" + postSignature);
		}
		
		// Since the query parameters aren't going to come out of the Map structure in any meaningful 
		// way, we need some way to deal with non parameterized
		// Parse query parameter map, adding all key / value pairs to the command line and storing 
		// away any keys that don't have values to be appended later.
		
		if (qps.get(rawKeySignature) != null) {
			keysWithNoValue.addAll(qps.get(rawKeySignature)) ;
			
			while (qps.containsKey(rawKeySignature)) {
				qps.remove(rawKeySignature);
			}
		}
		
		// Iterate over all keys, checking for presence in the paramMap structure.  Throw an
		// exception if the param is not present in paramMap.
		
		keys.addAll(qps.keySet());
		for (String key: keys) {

			ConfigParam cp = ri.paramConfig.paramMap.get(key);
			if (cp == null) {
				throw new Exception("Unknown query parameter: " + key);
			}
			
			if (qps.get(key).size() > 1)
				throw new Exception("Duplicate query parameter: " + key);

			cp.value = qps.getFirst(key);
			
			// Test if param type is OK.  DATE, NUMBER, TEXT, BOOLEAN
			switch (cp.type) {
			case NONE:
				if (isOkString(cp.value)) {
					throw new Exception("No value permitted for " + key + " Found value: " + cp.value);
				}
				break;
				
			case DATE:
				if (!isValidFdsnDate(cp.value)) {
					throw new Exception("Bad date value for " + key + ": " + cp.value);
				}
				break;
				
			case NUMBER:
				if (!isValidFdsnDecimal(cp.value)) {
					throw new Exception("Bad numeric value for " + key + ": " + cp.value);
				}					
				break;
				
			case BOOLEAN:
				if (!cp.value.equalsIgnoreCase("true") && !cp.value.equalsIgnoreCase("false"))
					throw new Exception("Bad boolean value for " + key + ": " + cp.value);
				break;
			
			case TEXT:
				if (!isOkString(cp.value)) {
					throw new Exception("No valid value for " + key);
				}
				break;
			}		
			
			// Check to see if this was the 'format' parameter. If present AND valid, change the config class's 
			// output mime type so that the overall service's output format will change.  			
			if (key.equalsIgnoreCase(outputControlSignature)) {
				ri.setPerRequestOutputType(cp.value);
			}
			
			// Add key and also value if valid.
			cmd.add("--" + key);
			if (isOkString(cp.value)) {
				cmd.add(cp.value.replaceAll("\\p{Cntrl}", ""));
			}	
		}

		for (String rawArg: keysWithNoValue) {
			cmd.add(rawArg);
		}		
		
//		logger.info("CMD: " + cmd);
	}
	
	private static String[] regexpStrings = {
		"^\\d{4}-[01]\\d-[0-3]\\d[T ][0-2]\\d:[0-5]\\d:[0-5]\\d\\.\\d+(Z)?$",
		"^\\d{4}-[01]\\d-[0-3]\\d[T ][0-2]\\d:[0-5]\\d:[0-5]\\d(Z)?$",
		"^\\d{4}-[01]\\d-[0-3]\\d(Z)?$"
	};
	
	public static Boolean isValidFdsnDate(String dateString) {
		for (String regexp: regexpStrings) {
			if (dateString.trim().matches(regexp))
				return true;
		}
		return false;
	}
	
	private static Boolean isValidFdsnDecimal(String decString) {
		String regexp ="^[-+]?\\d*\\.?\\d*$";
		if (decString.trim().matches(regexp))
				return true;
		
		return false;
	}
	
	private static boolean isOkString(String s) {
		return ((s != null) && !s.isEmpty());
	}
}
