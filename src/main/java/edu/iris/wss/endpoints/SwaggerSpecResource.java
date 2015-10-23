/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.iris.wss.endpoints;

import edu.iris.wss.provider.IrisProcessingResult;
import edu.iris.wss.provider.IrisProcessor;
import static edu.iris.wss.Wss.logger;
import edu.iris.wss.framework.AppConfigurator;
import edu.iris.wss.framework.FdsnStatus;
import edu.iris.wss.framework.RequestInfo;
import edu.iris.wss.framework.Util;
import edu.iris.wss.utils.WebUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
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
public class SwaggerSpecResource extends IrisProcessor {

    String globalErrMsg = "no globalErrMessage";

    @Override
// deprecated    public IrisProcessingResult getProcessingResults(RequestInfo ri, String configBase) {
    public IrisProcessingResult getProcessingResults(RequestInfo ri) {
        String configBase = null;
    	// First check if swaggerV2Spec configuration paramter is set.
        // If so, then return stream object from that URL.
    	
    	String resourceURLStr = ri.appConfig.getSwaggerV2URL();
    	if ((resourceURLStr != null) && (!resourceURLStr.isEmpty())) {
            logger.info("Attempting to load resource from: " + resourceURLStr);
            
        	InputStream is = null;
          	URL url = null;
        	try {    		
        		url = new URL(resourceURLStr);    		
        		is = url.openStream();
        	} catch (Exception ex) {
        		String errMsg = "SwaggerSpecResource - Error on Swagger V2 URL: "
                      + resourceURLStr + "  ex: " + ex;
//                logger.error("Failure loading SwaggerV2 file from: " + url
//                + "  ex: " + ex);
//            	return  Response.status(Status.OK).entity(err).type("text/plain").build();

                Util.logAndThrowException(ri, Util.adjustByCfg(FdsnStatus.Status.NO_CONTENT, ri),
                      errMsg);
        	}
        	
        	final BufferedReader br = new BufferedReader( new InputStreamReader( is));

        	StreamingOutput so = new StreamingOutput() {
        		@Override
    			public void write(OutputStream outputStream) throws IOException,
                      WebApplicationException {
        			BufferedWriter writer =
                          new BufferedWriter (new OutputStreamWriter(outputStream));
        			String inputLine = null ;
        			while ((inputLine = br.readLine()) != null) {
        				writer.write(inputLine);
        				writer.newLine();
        			}
        			writer.flush();
        			br.close();
        			writer.close();
    			}
        	};
            
            IrisProcessingResult ipr = new IrisProcessingResult(so,
                  MediaType.APPLICATION_JSON_TYPE, FdsnStatus.Status.OK);
            Response.ResponseBuilder builder = Response.status(FdsnStatus.Status.OK)
                  .type("application/json")
                  .entity(so);
            Util.addCORSHeadersIfConfigured(builder, ri);
    		return ipr;
    	}

        // else - for resource URL not found, try with a default name
        //
		// Try to read the resource file from the webserviceshell configuration
        // file folder using the same naming conventions as regular
        // webserviceshell cfg files.
		String resourceFileName = null;

		FileInputStream fileInStream = null;
        String errMsg = "SwaggerSpecResource - Error getting default resource file,";
		try {
			String wssConfigDir = System.getProperty(WebUtils.wssConfigDirSignature);
			if (AppConfigurator.isOkString(wssConfigDir)
                  && AppConfigurator.isOkString(configBase)) {
				if (!wssConfigDir.endsWith("/")) {
					wssConfigDir += "/";
                }
				
				resourceFileName = wssConfigDir + configBase + "-swagger.json";		
			
				File resourceFile = new File(resourceFileName);
				if (resourceFile.exists()) {
					logger.info("Attempting to load resource file from: "
                          + resourceFileName);

					fileInStream = new FileInputStream(resourceFileName);
                    IrisProcessingResult ipr = new IrisProcessingResult(
                          fileInStream, MediaType.APPLICATION_JSON_TYPE,
                          FdsnStatus.Status.OK);
                    Response.ResponseBuilder builder = Response.status(FdsnStatus.Status.OK)
                          .type("application/json")
                          .entity(fileInStream);
                    Util.addCORSHeadersIfConfigured(builder, ri);
                    return ipr;
				} else {
                    errMsg = errMsg + "  resourceFileName: " + resourceFileName;
                }
			} else {
                errMsg = errMsg + "  wssConfigDir: " + wssConfigDir
                      + "  configBase: " + configBase;
            }
		} catch (Exception ex) {
            errMsg = errMsg + "  ex: " + ex;
		}

                    IrisProcessingResult ipr = new IrisProcessingResult(null,
                          MediaType.TEXT_PLAIN_TYPE,
                          FdsnStatus.Status.NO_CONTENT);

        FdsnStatus.Status status = Util.adjustByCfg(FdsnStatus.Status.NO_CONTENT, ri);
        Util.logAndThrowException(ri, status, errMsg);

        Response.ResponseBuilder builder = Response.status(status)
              .type(MediaType.TEXT_PLAIN);
        Util.addCORSHeadersIfConfigured(builder, ri);
        return ipr;
    }

    @Override
    public String getErrorString() {
        return globalErrMsg;
    }
    
}
