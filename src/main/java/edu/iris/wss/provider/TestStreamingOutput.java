/*******************************************************************************
 * Copyright (c) 2013 IRIS DMC supported by the National Science Foundation.
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

package edu.iris.wss.provider;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;


import org.apache.log4j.Logger;
import edu.iris.wss.framework.RequestInfo;
import edu.iris.wss.framework.FdsnStatus.Status;

public class TestStreamingOutput extends IrisStreamingOutput {
	
	public static final Logger logger = Logger.getLogger(TestStreamingOutput.class);
	private Date startTime;
	
	public TestStreamingOutput() {}
	
	public TestStreamingOutput(RequestInfo ri) {
		this();
		this.ri = ri;
	}
	
	@Override
	public void setRequestInfo(RequestInfo ri) {
		this.ri = ri;
	}
	
	public String getErrorString() {
		return "Seriously, an error occurred";
	}
	
	public Status getResponse()  {
		
		startTime = new Date();
		return Status.OK;
//		return Status.NO_CONTENT;
//		return Status.BAD_REQUEST;		
	}
	
	@Override
	public void write(OutputStream output) {

		long totalBytesTransmitted = 0L;
		
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
		sb.append("<data>My favorite things</data>");
		
		String testData = sb.toString();
		try {
			output.write(testData.getBytes());
			totalBytesTransmitted += testData.getBytes().length;
			output.flush();
		} catch (IOException ioe) {			
			logger.error("Got IOE: " + ioe.getMessage());
		}
		catch (Exception e) {
			logger.error("Got Generic Exception: " + e.getMessage());
		
		} finally {
			logger.info("Done:  Wrote " + totalBytesTransmitted + " bytes\n");

			logUsageMessage(ri, null, totalBytesTransmitted, (new Date()).getTime() - startTime.getTime(),
					null, Status.OK, null);
    		ri.statsKeeper.logShippedBytes(totalBytesTransmitted);
    		try {
    			output.close();
    		} catch (IOException ioe) {
    			// What can one do?
    			;
    		}
		}	
	}
}
