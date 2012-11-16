package edu.iris.wss.framework;

import java.text.SimpleDateFormat;
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
	public final static String outputControlSignature = "output";
	public final static String usernameSignature = "username";
	public static final Logger logger = Logger.getLogger(ParameterTranslator.class);

	public static  void parseQueryParams(ArrayList<String> cmd, RequestInfo ri) throws Exception {
		
		List<String> keys = new ArrayList<String>();
		List<String> keysWithNoValue = new ArrayList<String>();
		
		MultivaluedMap<String, String> qps = ri.uriInfo.getQueryParameters();
		
		// Special 'query parameter', output.  This is treated completely separately
		
		// Search for the output format.  If present AND valid, change the config class's mime type
		// so that the overall service's output format will change.  Don't remove this parameter; it
		// will be used by the called routine.
		
		String outputVal = qps.getFirst(outputControlSignature);
		if (isOkString(outputVal)) { 
			ri.appConfig.setOutputType(outputVal);

			cmd.add("--" + outputControlSignature);
			cmd.add(outputVal);

			qps.remove(outputControlSignature);
		}			
		
		// Special 'username' cli argument will be added if present
		
		String username = WebUtils.getAuthenticatedUsername(ri.requestHeaders);
		if (isOkString(username)) {
			cmd.add("--" + usernameSignature);
			cmd.add(username);
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
				throw new Exception("Unknown query key: " + key);
			}
			cp.value = qps.getFirst(key);
			
			// Test if param type is OK.  DATE, NUMBERS, LATs and LONs
			switch (cp.type) {
			case DATE:
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
				sdf.setLenient(false);

				try {
					sdf.parse(cp.value);
				} catch (Exception e) {
					throw new Exception("Bad date value for " + key + ": " + cp.value);
				}
				break;
				
			case NUMBER:
				try {
					Float.parseFloat(cp.value);
				} catch (Exception e) {
					throw new Exception("Bad numeric value for " + key + ": " + cp.value);
				}					
				break;
				
//			case LATITUDE:
//				try {
//					f = Float.parseFloat(cp.value);
//				} catch (Exception e) {
//					throw new Exception("Bad numeric value for " + key + ": " + cp.value);
//				}
//				if  ((f > 90) || (f < -90))
//					throw new Exception("Out of range value for " + key + ": " + cp.value);
//				break;
//				
//			case LONGITUDE:
//				try {
//					f = Float.parseFloat(cp.value);
//				} catch (Exception e) {
//					throw new Exception("Bad numeric value for " + key + ": " + cp.value);
//				}
//				if  ((f > 180) || (f < -180))
//					throw new Exception("Out of range value for " + key + ": " + cp.value);
//				break;
			
			case TEXT:
				// Text is, well, just text
				break;
			}		
			
			// Add key and also value if valid.
			cmd.add("--" + key);
			if (isOkString(cp.value)) 
				cmd.add(cp.value);
		}

		for (String rawArg: keysWithNoValue) {
			cmd.add(rawArg);
		}		
		
//		logger.info("CMD: " + cmd);
	}
	
	private static boolean isOkString(String s) {
		return ((s != null) && !s.isEmpty());
	}
}
