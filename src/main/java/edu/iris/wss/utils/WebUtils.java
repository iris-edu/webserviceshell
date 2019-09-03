/** *****************************************************************************
 * Copyright (c) 2018 IRIS DMC supported by the National Science Foundation.
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
 *****************************************************************************
 */
package edu.iris.wss.utils;

import java.io.IOException;
import java.io.StringReader;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.xml.bind.DatatypeConverter;

import org.apache.log4j.Logger;

public class WebUtils {

    public static final Logger LOGGER = Logger.getLogger(WebUtils.class);

    public static String getHostname() {
        String hostname;
        try {
            hostname = java.net.InetAddress.getLocalHost().getCanonicalHostName();//.getHostName();

        } catch (UnknownHostException e) {
            hostname = "unknown";
        }
        return hostname;
    }

    public static String getUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("user-agent");
        if (userAgent == null) {
            userAgent = "NOT_SET_by_client";
        }
        return userAgent;
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
        if (headers == null) {
            return null;
        }

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

        if (requestHeaders == null) {
            return null;
        }

        List<String> entries;
        try {
            entries = requestHeaders.getRequestHeader(authSig);
        } catch (Exception e) {
            LOGGER.error("Failed to get request headers!");
            return null;
        }

        if ((entries == null) || (entries.isEmpty())) {
            return null;
        }

        String entry = entries.get(0);

        int index;
        if ((index = entry.indexOf(basicSig)) != -1) {
            String userAndPassword;

            try {
                entry = entry.substring(index + basicSig.length());
                byte[] creds = DatatypeConverter.parseBase64Binary(entry);

                userAndPassword = new String(creds);
                return userAndPassword.substring(0, userAndPassword.indexOf(":"));

            } catch (Exception e) {
                return null;
            }

        } else if ((index = entry.indexOf(digestSig)) != -1) {
            try {
                String props = entry.substring(index + digestSig.length());
                String ps[] = props.split(",");

                StringBuilder sb = new StringBuilder();
                for (String s : ps) {
                    sb.append(s).append("\n");
                }

                Properties p = new Properties();
                p.load(new StringReader(sb.toString()));

                String quoted = (String) p.get("username");
                return quoted.substring(1, quoted.length() - 1);

            } catch (IOException e) {
                return null;
            }
        }

        return null;
    }

    public static String getHost(HttpServletRequest request) {
        String hostname;
        try {
            hostname = java.net.InetAddress.getLocalHost().getHostName();
            int index = hostname.indexOf('.');
            if (index != -1) {
                hostname = hostname.substring(0, index);
            }

        } catch (UnknownHostException e) {
            hostname = "Unknown";
        }
        return hostname;
    }

    public static String getObfuscatedHost(HttpServletRequest request) {
        String hostname;
        try {
            hostname = java.net.InetAddress.getLocalHost().getHostName();
            int index = hostname.indexOf('.');
            if (index != -1) {
                hostname = hostname.substring(0, index);
            }

            if (hostname.length() > 4) {
                hostname = hostname.substring(2, 4) + hostname.substring(hostname.length() - 1);
            }

        } catch (UnknownHostException e) {
            hostname = "unk";
        }
        return hostname;
    }

    public static String getPort(HttpServletRequest request) {
        int portNum = request.getServerPort();
        String portStr = Integer.toString(portNum);

        return portStr;
    }

    public static String getTomcatLogDir() {
        String rtnDir;
        try {
            String dir = System.getProperty("catalina.base");
            rtnDir = String.join("/", dir, "logs");
        } catch (Exception e) {
            rtnDir = "Unknown";
        }
        return rtnDir;
    }
}
