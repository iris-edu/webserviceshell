package edu.iris.wss.utils;

import java.io.StringReader;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.xml.bind.DatatypeConverter;

import org.apache.log4j.Logger;


public class WebUtils {
	public static final Logger logger = Logger.getLogger(WebUtils.class);
	
	public static String getHostname() {
		String hostname = "";
		try {
			hostname = java.net.InetAddress.getLocalHost().getHostName();		
		} catch (Exception e) {
			hostname = "unknown";
		}
		return hostname;
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
	   	List <String> entries = requestHeaders.getRequestHeader(authSig);
	   	
	   	if ((entries == null) || (entries.size() == 0))
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
		   		for (String s: ps) sb.append(s + "\n");
		   		
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
	
	public static String getCrazyHostPort(HttpServletRequest request) {
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
		
    	int portNum = request.getServerPort();
    	String portStr = Integer.toString(portNum);
    	if (portStr.length() >= 4) {
    		portStr = portStr.substring(2, 4);
    	}
		
		return hostname + ":" + portStr;
	}
}
