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

import edu.iris.wss.framework.RequestInfo;
import edu.iris.wss.provider.IrisProcessingResult;
import edu.iris.wss.provider.IrisProcessor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.message.internal.MediaTypes;

/**
 *
 * @author mike
 */
public class PostEndpoint extends IrisProcessor {
    public static final String THIS_CLASS_NAME = PostEndpoint.class.getSimpleName();

    @Override
    public IrisProcessingResult getProcessingResults(RequestInfo ri, String wssMediaType) {
        IrisProcessingResult ipr = IrisProcessingResult.processString(
              "default string from " + THIS_CLASS_NAME);

        if (MediaTypes.typeEqual(MediaType.TEXT_PLAIN_TYPE, ri.requestMediaType)) {
            ipr = IrisProcessingResult.processString(ri.postBody);
        }

        // "multipart/form-data"
        if (MediaTypes.typeEqual(MediaType.MULTIPART_FORM_DATA_TYPE,
                        ri.requestMediaType)) {

            //review_multipart(ri.postMultipart);

            String jsonStr = simple_multipart_to_json(ri.postMultipart);

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

    private void review_multipart(FormDataMultiPart fdmp) {
        Map<String, List<FormDataBodyPart>> fdmpparts = fdmp.getFields();
        for (String pname : fdmpparts.keySet()) {
            System.out.println("------ pname: " + pname);
            List<FormDataBodyPart> parts = fdmpparts.get(pname);

            for (FormDataBodyPart part : parts) {
                System.out.println("-------------- part name: " + part.getName());
                System.out.println("-------------- part dispo: " + part.getContentDisposition());
                System.out.println("-------------- part fdisp: " + part.getFormDataContentDisposition());
                MediaType pMt = part.getMediaType();
                System.out.println("-------------- part mediT: " + pMt);
                if (MediaTypes.typeEqual(MediaType.TEXT_PLAIN_TYPE, pMt)) {
                    Object pVal = part.getValue();
                    System.out.println("-------------- part value str: " + pVal);
                } else {
                    InputStream instrm = part.getEntityAs(InputStream.class);
                    int bytesRead = 0;
                    int totalBytes = 0;
                    byte[] buffer = new byte[32768];
                    Charset charset = Charset.forName("ISO-8859-1");
                    while (true) {
                        try {
                            bytesRead = instrm.read(buffer, 0, buffer.length);
                        } catch (IOException ex) {
                            System.out.println("-------------- read exception: " + ex);
                        }

                        if (bytesRead < 0) {
                            break;
                        }
                        totalBytes += bytesRead;
                        //output.write(buffer, 0, bytesRead);
                        System.out.println("-----------_____--- part value tot: " + totalBytes);
                        ByteBuffer bb = ByteBuffer.wrap(buffer);
                        String text = charset.decode(bb).toString();
                        System.out.println("-----------_____--- part value buf: " + text);
                    }

                }

                //                                             System.out.println("-------------- part fdisp: " + part.);
                    //MediaType.
            System.out.println("************** form parts: " + fdmp.getBodyParts());

            }
        }
    }

    private String simple_multipart_to_json(FormDataMultiPart fdmp) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  [\n");
        Map<String, List<FormDataBodyPart>> fdmpMap = fdmp.getFields();

        boolean firstTime = true;
        for (String pname : fdmpMap.keySet()) {
            List<FormDataBodyPart> parts = fdmpMap.get(pname);
            if (!firstTime) {
                sb.append("    },\n");
            }
            for (FormDataBodyPart part : parts) {
                sb.append("    {\"name\": \"").append(part.getName()).append("\",\n");
                MediaType partMt = part.getMediaType();
                if (MediaTypes.typeEqual(MediaType.TEXT_PLAIN_TYPE, partMt)) {
                    Object pVal = part.getValue();
                    sb.append("     \"value\": \"").append(part.getValue()).append("\"\n");
                } else {
                    sb.append("     \"value\": \"").append("decoding skipped for mediaType: ")
                          .append(partMt).append("\"\n");
                }
            }
            firstTime = false;
        }
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n");

        return sb.toString();
    }
}
