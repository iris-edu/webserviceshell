/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.iris.wss.endpoints;

import edu.iris.wss.framework.RequestInfo;
import edu.iris.wss.provider.IrisProcessingResult;
import edu.iris.wss.provider.IrisProcessor;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 *
 * @author mike
 */
public class IncomingHeaders extends IrisProcessor {

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
