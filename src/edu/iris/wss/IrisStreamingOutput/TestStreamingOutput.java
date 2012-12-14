package edu.iris.wss.IrisStreamingOutput;

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

			logUsageMessage(ri, totalBytesTransmitted, (new Date()).getTime() - startTime.getTime(),
					null, Status.OK, null);
    		
    		try {
    			output.close();
    		} catch (IOException ioe) {
    			// What can one do?
    			;
    		}
		}	
	}
}
