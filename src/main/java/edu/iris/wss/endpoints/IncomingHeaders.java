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

package edu.iris.wss.endpoints;

import edu.iris.wss.framework.FdsnStatus;
import edu.iris.wss.framework.RequestInfo;
import edu.iris.wss.framework.Util;
import edu.iris.wss.provider.IrisProcessingResult;
import edu.iris.wss.provider.IrisProcessor;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Set;
import javax.ws.rs.core.MultivaluedMap;

/**
 *
 * @author mike
 */
public class IncomingHeaders extends IrisProcessor {
    public static final String THIS_CLASS_NAME = IncomingHeaders.class.getSimpleName();

    // concerning proxy informatino from apache to tomcat
    // https://httpd.apache.org/docs/2.4/mod/mod_proxy.html#x-headers
    // from stackoverflow
    // https://stackoverflow.com/questions/16558869/getting-ip-address-of-client#answer-21884642
    private static final String[] HEADERS_TO_TRY = {
    "X-Forwarded-For",
    "Proxy-Client-IP",
    "WL-Proxy-Client-IP",
    "HTTP_X_FORWARDED_FOR",
    "HTTP_X_FORWARDED",
    "HTTP_X_CLUSTER_CLIENT_IP",
    "HTTP_CLIENT_IP",
    "HTTP_FORWARDED_FOR",
    "HTTP_FORWARDED",
    "HTTP_VIA",
    "REMOTE_ADDR" };

    @Override
    public IrisProcessingResult getProcessingResults(RequestInfo ri,
          String wssMediaType) {
        IrisProcessingResult ipr = IrisProcessingResult.processString(
              "is String, as a default IrisProcessingResult from " + THIS_CLASS_NAME);

        // for Jersey 2.x, the structure returned from getQueryParameters()
        // is now immutable, so make a local copy
        MultivaluedMap<String, String> qps_immutable = ri.uriInfo.getQueryParameters();
        Set<String> mmKeys = qps_immutable.keySet();

        if (mmKeys.size() == 0) {
            // handle base path
            ipr = getHeaders(ri, wssMediaType);
        } else {
            for (String mmkey : mmKeys) {
                String value = qps_immutable.get(mmkey).get(0);
                if (mmkey.equals("setlogandthrow")) {
                    ipr = handle_setlogandthrow(ri, mmkey, value);
                }
            }
        }

        return ipr;
    }

    private IrisProcessingResult handle_setlogandthrow(RequestInfo ri, String key,
          String value) {
        String input = "param: " + key + "  value: " + value;
        FdsnStatus.Status status = FdsnStatus.Status.NOT_IMPLEMENTED;
        if (value.equals("204")) {
            status = FdsnStatus.Status.NO_CONTENT;
        } else if ((value.equals("404"))) {
            status = FdsnStatus.Status.NOT_FOUND;
        } else {
            Util.logAndThrowException(ri, FdsnStatus.Status.OK,
                  "return 200 for -- " + input);
        }
        Util.logAndThrowException(ri, status,
              "msg: log and throw executed with code: " + status.getStatusCode()
              + ", reason: " + status.getReasonPhrase()
              + "  based on input of " + input);

        // should never get here
        IrisProcessingResult ipr = IrisProcessingResult.processString(
              "is String, from class: " + THIS_CLASS_NAME
              + " method: handle_setlogandthrow, this should never execute");

        return ipr;
    }

    private IrisProcessingResult getHeaders(RequestInfo ri,
          String wssMediaType) {

        String wssIP = "";
        StringBuilder sb = new StringBuilder(1024);

        try {
            wssIP = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            wssIP = "UnknownHostException";
            sb.append("*** wssIP ex: ").append(ex).append("\n");
        }
        sb.append("*** wssMediaType ex: ").append(wssMediaType)
              .append("  wssIP: ").append(wssIP).append("\n");
        sb.append("\n");

        // check headers for original requesting IP - from stackoverflow example method
        for (String header : HEADERS_TO_TRY) {
            String ip = ri.request.getHeader(header);
            if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
                sb.append("*** ip_header: ").append(header)
                      .append("  IP: ").append(ip)
                      .append("\n");
            }
        }
        sb.append("\n");

        // get all the names and values
        Enumeration names = ri.request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            Enumeration values = ri.request.getHeaders(name); // support multiple values
            if (values != null) {
                while (values.hasMoreElements()) {
                    String value = (String) values.nextElement();
                    sb.append("*** all_headers: ").append(name)
                          .append("  value: ").append(value)
                          .append("\n");
                }
            } else {
                sb.append("*** all_headers: ").append(name)
                      .append("  value: ").append("null")
                      .append("\n");
            }
        }

        IrisProcessingResult ipr = IrisProcessingResult.processString(
              sb.toString());

        return ipr;
    }
}
