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

package edu.iris.wss.endpoints;

import edu.iris.wss.provider.IrisProcessingResult;
import edu.iris.wss.provider.IrisProcessor;
import edu.iris.wss.framework.FdsnStatus;
import edu.iris.wss.framework.RequestInfo;
import edu.iris.wss.framework.Util;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

/**
 *
 * @author mike
 */
public class ProxyResource extends IrisProcessor {

    String globalErrMsg = "no globalErrMessage";

    @Override
    public IrisProcessingResult getProcessingResults(RequestInfo ri,
          String wssMediaType) {
    	// First check for the resource
        // If found, then return an entity to read and write the content
        // of the URL.

        String epName = ri.getEndpointNameForThisRequest();
        String proxyURLSource = ri.appConfig.getProxyUrl(epName);

        if ((proxyURLSource != null) && (!proxyURLSource.isEmpty())) {
            logger.info("Attempting to load resource from: " + proxyURLSource);

            InputStream is = null;
          	URL url = null;
        	try { 
                url = new URL(proxyURLSource);
        		is = url.openStream();
        	} catch (Exception ex) {
                globalErrMsg = this.getClass().getName()
                      + ".getProcessingResults - Error resolving proxy URL: "
                      + proxyURLSource + "  ex: " + ex;
//                logger.error("Failure loading SwaggerV2 file from: " + url
//                + "  ex: " + ex);
//            	return  Response.status(Status.OK).entity(err).type("text/plain").build();

                Util.logAndThrowException(ri,
                      Util.adjustByCfg(FdsnStatus.Status.NO_CONTENT, ri),
                      globalErrMsg, ex);
        	}
        	
            final InputStreamReader inputSR = new InputStreamReader(is);

        	StreamingOutput so = new StreamingOutput() {
        		@Override
    			public void write(OutputStream outputStream) throws IOException,
                      WebApplicationException {
                    try (BufferedWriter writer = new BufferedWriter(
                          new OutputStreamWriter(outputStream));
                          BufferedReader br = new BufferedReader(inputSR)) {
                        String inputLine = null ;
                        while ((inputLine = br.readLine()) != null) {
                            writer.write(inputLine);
                            writer.newLine();
                        }
                        writer.flush();
                    }
    			}
        	};
            
            IrisProcessingResult ipr = new IrisProcessingResult(so,
                  wssMediaType, FdsnStatus.Status.OK, null);

    		return ipr;
    	}

        // else - null or empty URL string - may have been checked
        //        when paramters were loaded.

        FdsnStatus.Status status = FdsnStatus.Status.NO_CONTENT;
        String entity = null; // for 204, no_content, should return nothing
        
        if (ri.perRequestUse404for204) {
            // use 404 instead
            status =  FdsnStatus.Status.NOT_FOUND;
            globalErrMsg = "input proxy URL was null or empty string";
            entity = "entity is string of " + globalErrMsg;
        }

        IrisProcessingResult ipr = new IrisProcessingResult(entity,
              MediaType.TEXT_PLAIN, status, null);

        // goahead and call exception handling directly, ipr is never used.
        Util.logAndThrowException(ri, status, globalErrMsg);

        return ipr;
    }

    @Override
    public String getErrorString() {
        return globalErrMsg;
    }
}
