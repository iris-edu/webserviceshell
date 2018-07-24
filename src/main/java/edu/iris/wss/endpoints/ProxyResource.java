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

import edu.iris.wss.framework.AppConfigurator;
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
import javax.ws.rs.core.StreamingOutput;

/**
 *
 * @author mike
 */
public class ProxyResource extends IrisProcessor {

    @Override
    public IrisProcessingResult getProcessingResults(RequestInfo ri,
          String wssMediaType) {
    	// First check for the resource
        // If found, then return an entity to read and write the content
        // of the URL.

        String epName = ri.getEndpointNameForThisRequest();
        String proxyURLSource = ri.appConfig.getProxyUrl(epName);

        if (Util.isOkString(proxyURLSource)) {
            logger.info("Attempting to load resource from: " + proxyURLSource);

            InputStream is = null;
            URL url = null;
            try {
                url = new URL(proxyURLSource);
        		is = url.openStream();
        	} catch (Exception ex) {
                String briefMsg = this.getClass().getName()
                      + " could not open URL: " + proxyURLSource;
                String detailedMsg = "resource is not available or possibley"
                      + " the cfg file is incorrect, method: "
                      + this.getClass().getName()
                      + ".getProcessingResults - exception: " + ex;

                IrisProcessingResult ipr =
                      IrisProcessingResult.processError(
                            FdsnStatus.Status.NOT_FOUND, briefMsg, detailedMsg);
				return ipr;
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

            IrisProcessingResult ipr = IrisProcessingResult.processStream(so,
                  wssMediaType, null);

    		return ipr;
    	}

        // if we are here, then proxyURLSource is null or empty,
        // this should have been checked when parameters were loaded

        String briefMsg = this.getClass().getName()
                      + " found proxy URL to be null or empty";
        String detailedMsg = "Unexpected error in " + this.getClass().getName()
              + " class, also check parameter "
              + AppConfigurator.EP_CFGS.proxyURL.toString()
              + " in -service.cfg file";

        IrisProcessingResult ipr = IrisProcessingResult.processError(
              FdsnStatus.Status.INTERNAL_SERVER_ERROR, briefMsg, detailedMsg);

        return ipr;
    }
}
