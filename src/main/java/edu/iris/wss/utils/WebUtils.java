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

package edu.iris.wss.utils;

import java.io.File;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.xml.bind.DatatypeConverter;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


public class WebUtils {
	public static final Logger logger = Logger.getLogger(WebUtils.class);

    public static final String wssConfigDirSignature = "wssConfigDir";
	
	public static String getHostname() {
		String hostname = "";
		try {
			hostname = java.net.InetAddress.getLocalHost().getCanonicalHostName();//.getHostName();	

		} catch (Exception e) {
			hostname = "unknown";
		}
		return hostname;
	}
	
	public static String getContextPath(HttpServletRequest request) {
		return request.getContextPath();
	}
	
//	// Returns only the last part after the '/' character of the context path
//	// which may include slash characters, etc.
//	public static String getWebAppName(HttpServletRequest request) {
//		String cp = getContextPath(request);
//		int index = cp.lastIndexOf('/');
//		return cp.substring(index + 1);
//	}
	
	public static String getConfigFileBase(ServletContext context) {
		String base = context.getContextPath();
		String version = null;
		// Looking for a 'Version' part of the context path. We're going to remove it if it exists
		// and use it later in constructing the  'base' of the config file names.
		
		// Look for final '/' a number and end of line.  If found, store the number as version
		// and remove the entire match to get the full context path, minus version.
		Pattern pat = Pattern.compile("/([0-9])$");
		Matcher mat = pat.matcher(base);
		if (mat.find()) {
			version = mat.group(1);
			base = base.substring(0, mat.start());
		}
				
		// Get everything at the end up to the last '/' character. I.e. the 'base'
		base = base.substring(base.lastIndexOf('/') + 1);
	
		
		if (version != null) {
			base += "-" + version;
		} 
		return base;
	}
  
    public static void myInitLog4j(ServletContext servletContext) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        String now = fmt.format((new GregorianCalendar()).getTime());
        
        String configDirName = System.getProperty(wssConfigDirSignature);
        System.out.println(now + " Info, myInitLog4j, property "
              + wssConfigDirSignature + ": " + configDirName);

        if (configDirName == null) {
            System.out.println(now + " *** Warning, myInitLog4j - system property, "
                  + wssConfigDirSignature + " is not found, log4j is not initialize here");
            System.out.println(now + " *** Warning, myInitLog4j - for tomcat, messages"
                  + " may be in files logs/wss.log and logs/wss_usage.log");
            return;
        }

        File configDir = new File(configDirName);

        if (!configDir.isDirectory()) {
            System.out.println(now + " *** Warning, myInitLog4j, wssConfigDir path: "
                + configDir.getAbsolutePath() + " does not exist");
        }
        

        String contextPath = servletContext.getContextPath();
        String split[] = contextPath.split("/");

        // use the same mechanism for building log4j properties file names
        // as service and param files names
        String configBase = WebUtils.getConfigFileBase(servletContext);
        
        String fileName = configBase + "-log4j.properties";
        System.out.println(now + " Info, myInitLog4j, filename: " + fileName);
        
        File file = new File(configDir, fileName);
        
        if( !file.exists() ) {
            System.out.println(now + " *** Warning, myInitLog4j, contextPath: " + contextPath
                + " unable to locate log4j file: " + file.getAbsolutePath()
                + " check for logs/wss.log and logs/wss_usage.log");
            return;
        }
        System.out.println(now + " Info, myInitLog4j, contextPath: " + contextPath
            + "  log4j file: " + file.getAbsolutePath());
        
        PropertyConfigurator.configure(file.getAbsolutePath());
    }
	
	public static String getUserAgent(HttpServletRequest request) {
		return request.getHeader("user-agent");
	}

	public static String getClientName(HttpServletRequest request) {
		return request.getRemoteHost();
	}
	
	public static String getClientIp(HttpServletRequest request) {
		String xfIp = getXForwardIp(request);
		if (xfIp == null) {
			return request.getRemoteAddr();
		} else {
			return xfIp;
		}
	}		
	
	public static String getUrl(HttpServletRequest request) {
		StringBuffer url = request.getRequestURL();
		if (request.getQueryString() != null) {
		    url.append("?").append(request.getQueryString());
		}
		return url.toString();
	}
	private static String getXForwardIp(HttpServletRequest request) {
		Enumeration<String> headers = request.getHeaders("x-forwarded-for");
		if (headers == null) return null;
		
		String xfIp = null;
		while (headers.hasMoreElements()) {
			String[] ips = headers.nextElement().split(",");
			if (ips.length > 0) {
				xfIp = ips[0];
			}
		}
		return xfIp;
	}
	
	public static String getAuthenticatedUsername(HttpHeaders requestHeaders) {
		
		final String authSig = "authorization";
		final String basicSig = "Basic ";
		final String digestSig = "Digest ";
		
		if (requestHeaders == null) return null;
		
		List <String> entries = null;
		try {
			entries = requestHeaders.getRequestHeader(authSig);
		} catch (Exception e) {
			logger.error("Failed to get request headers!");
			return null;
		}
		
        if ((entries == null) || (entries.isEmpty()))
	   		return null;

	   	String entry = entries.get(0);

	   	int index;
	   	if ((index = entry.indexOf(basicSig)) != -1)  {
			String userAndPassword;

		   	try {
		   		entry = entry.substring(index + basicSig.length());
		   		byte [] creds = DatatypeConverter.parseBase64Binary(entry);
		   	
		   		userAndPassword = new String(creds);
		   		return userAndPassword.substring(0, userAndPassword.indexOf(":"));
		   		
		   	} catch (Exception e) {
		   		return null;
		   	}
		   	
	   	} else if ((index = entry.indexOf(digestSig)) != -1) {
		   	try {
		   		String props = entry.substring(index + digestSig.length());
		   		String ps[] =  props.split(",");
		   		
	   			StringBuilder sb = new StringBuilder();
                for (String s: ps) sb.append(s).append("\n");
		   		
		   		Properties p = new Properties();
		   		p.load(new StringReader(sb.toString()));

		   		String quoted = (String) p.get("username");
		   		return quoted.substring(1, quoted.length() - 1);

		   	} catch (Exception e) {
		   		return null;
		   	}
	   	} 
	   	
	   	return null;
	}
	
	public static String getHost(HttpServletRequest request) {
    	String hostname = "";
		try {
			hostname = java.net.InetAddress.getLocalHost().getHostName();
			int index = hostname.indexOf('.');
			if (index != -1) 
				hostname = hostname.substring(0, index);

		} catch (Exception e) {
			hostname = "Unknown";
		}
		return hostname;
	}
	
	public static String getObfuscatedHost(HttpServletRequest request) {
    	String hostname = "";
		try {
			hostname = java.net.InetAddress.getLocalHost().getHostName();
			int index = hostname.indexOf('.');
			if (index != -1) 
				hostname = hostname.substring(0, index);

			if (hostname.length() > 4) {
				hostname = hostname.substring(2, 4) + hostname.substring(hostname.length() - 1);
			}

		} catch (Exception e) {
			hostname = "unk";
		}
		return hostname;
	}
	
	public static String getPort(HttpServletRequest request) {
    	int portNum = request.getServerPort();
    	String portStr = Integer.toString(portNum);
		
		return portStr;
	}
}
