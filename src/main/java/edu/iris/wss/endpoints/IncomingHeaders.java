/*******************************************************************************
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
 ******************************************************************************/

package edu.iris.wss.endpoints;

import edu.iris.wss.framework.FdsnStatus;
import edu.iris.wss.framework.RequestInfo;
import edu.iris.wss.framework.Util;
import edu.iris.wss.provider.IrisProcessingResult;
import edu.iris.wss.provider.IrisProcessor;
import edu.iris.wss.utils.WebUtils;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.StreamingOutput;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.message.internal.MediaTypes;
import org.json.simple.JSONObject;

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
        System.out.println("************** incoming now: " + Util.getCurrentUTCTimeISO8601_MS());
        System.out.println("************** incoming keysize: " + mmKeys.size());
        System.out.println("************** incoming ri mediaType: " + ri.requestMediaType);
        System.out.println("************** incoming wss mediaType: " + wssMediaType);
        System.out.println("************** user-agent: " + WebUtils.getUserAgent(ri.request));

        if (mmKeys.size() == 0) {
            // handle base path
            ipr = getHeaders(ri, wssMediaType);
        } else {
            for (String mmkey : mmKeys) {
                String value = qps_immutable.get(mmkey).get(0);
                if (mmkey.equals("setlogandthrow")) {
                    ipr = handle_setlogandthrow(ri, mmkey, value);
                } else if (mmkey.equals("try_setlogandthrow")) {
                    ipr = handle_try_setlogandthrow(ri, mmkey, value);
                }
            }
        }
        // "multipart/form-data"
        if (MediaTypes.typeEqual(MediaType.MULTIPART_FORM_DATA_TYPE,
                        ri.requestMediaType)) {
            JSONObject jo = simple_multipart_to_json(ri.postMultipart);
            String jsonStr = jo.toJSONString();

            StreamingOutput so = new StreamingOutput() {
                @Override
                public void write(OutputStream output) {
                    try {
                        output.write(jsonStr.getBytes());
                    } catch (IOException ex) {
                        throw new RuntimeException(THIS_CLASS_NAME + MediaType.MULTIPART_FORM_DATA
                              +" test code"
                              + " failed to do streaming output, ex: " + ex);
                    }
                }
            };

            ipr = IrisProcessingResult.processStream(so, MediaType.APPLICATION_JSON);
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
                  "return 200 in handle_setlogandthrow for input -- " + input);
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

    private IrisProcessingResult handle_try_setlogandthrow(RequestInfo ri, String key,
          String value) {
        String input = "param: " + key + "  value: " + value;
        FdsnStatus.Status status = FdsnStatus.Status.NOT_IMPLEMENTED;
        if (value.equals("204")) {
            status = FdsnStatus.Status.NO_CONTENT;
            try {
                Util.logAndThrowException(ri, status, "No content.  yoyo");
            } catch (Exception e) {
                // At a breakpoint here, 'e' is of type ServiceShellException & detailedMessage = "HTTP 204 No Content"
                // After this throw the program finishes with the user seeing "Error 500: HTTP 204 No Content"
                System.out.println("***********************1 e: " + e);
                System.out.println("***********************1 e: " + e.getMessage());
                System.out.println("***********************1 etm: " + e.getCause().getMessage());

                throw e;
            }
        } else if ((value.equals("404"))) {
            status = FdsnStatus.Status.NOT_FOUND;
            try {
                Util.logAndThrowException(ri, status, "No content.  updown");
            } catch (Exception e) {
                // At a breakpoint here, 'e' is of type ServiceShellException & detailedMessage = "HTTP 204 No Content"
                // After this throw the program finishes with the user seeing "Error 500: HTTP 204 No Content"
                System.out.println("***********************2 e: " + e);
                System.out.println("***********************2 e: " + e.getMessage());
                System.out.println("***********************2 etm: " + e.getCause().getMessage());

                //throw e;
                Util.logAndThrowException(ri, status, e.getCause().getMessage());
            }
        } else {
            Util.logAndThrowException(ri, FdsnStatus.Status.OK,
                  "return 200 in handle_try_setlogandthrow for input -- " + input);
        }
        // should never get here
        Util.logAndThrowException(ri, status,
              "msg: try log and throw executed with code: " + status.getStatusCode()
              + ", reason: " + status.getReasonPhrase()
              + "  based on input of " + input);

        // should never get here
        IrisProcessingResult ipr = IrisProcessingResult.processString(
              "is String, from class: " + THIS_CLASS_NAME
              + " method: handle_try_setlogandthrow, this should never execute");

        return ipr;
    }

    private IrisProcessingResult getHeaders(RequestInfo ri,
          String wssMediaType) {

        String wssIP = "not_set";
        StringBuilder sb = new StringBuilder(1024);

        sb.append("*** now: " + Util.getCurrentUTCTimeISO8601_MS()).append("\n");
        sb.append("*** wssMediaType: ").append(wssMediaType).append("\n");
        sb.append("*** wss.getUserAgent: ").append(WebUtils.getUserAgent(ri.request))
              .append("\n");
        try {
            wssIP = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            wssIP = "UnknownHostException";
            sb.append("*** exception getting wssIP: ").append(ex).append("\n");
        }
        sb.append("*** wssIP: ").append(wssIP).append("\n");
        sb.append("\n");

        // check headers for original requesting IP - from stackoverflow example method
        for (String header : HEADERS_TO_TRY) {
            String value = ri.request.getHeader(header);
            if (value != null && value.length() != 0 && !"unknown".equalsIgnoreCase(value)) {
                sb.append("*** original requesting IP header: ").append(header)
                      .append("  value: ").append(value)
                      .append("\n");
            }
        }
        sb.append("\n");

        // get all the names and values
        Enumeration names = ri.request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            Enumeration values = ri.request.getHeaders(name);
            if (values != null) {
                // support multiple values
                while (values.hasMoreElements()) {
                    String value = (String) values.nextElement();
                    sb.append("*** list all, header: ").append(name)
                          .append("  value: ").append(value)
                          .append("\n");
                }
            } else {
                sb.append("*** list all, header: ").append(name)
                      .append("  value: ").append("null")
                      .append("\n");
            }
        }

        IrisProcessingResult ipr = IrisProcessingResult.processString(
              sb.toString());

        return ipr;
    }

    private JSONObject simple_multipart_to_json(FormDataMultiPart fdmp) {
        JSONObject jo = new JSONObject();
        Map<String, List<FormDataBodyPart>> fdmpMap = fdmp.getFields();

        for (String pname : fdmpMap.keySet()) {
            List<FormDataBodyPart> parts = fdmpMap.get(pname);
            for (FormDataBodyPart part : parts) {
                MediaType partMt = part.getMediaType();
                if (MediaTypes.typeEqual(MediaType.TEXT_PLAIN_TYPE, partMt)) {
                    Object pVal = part.getValue();
                    jo.put(pname, pVal);
                } else {
                    jo.put(pname, "decoding skipped for mediaType: " + partMt);
                }
            }
        }

        return jo;
    }
}
