package edu.iris.wss.IrisStreamingOutput;

import java.io.OutputStream;
import java.util.Date;

//import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import edu.iris.wss.framework.FdsnStatus.Status;
import edu.iris.wss.framework.RequestInfo;
import edu.iris.wss.framework.ServiceShellException;
import edu.iris.wss.utils.LoggerUtils;

public abstract class IrisStreamingOutput implements StreamingOutput {

	public static void logUsageMessage(RequestInfo ri, String appSuffix,
			Long dataSize, Long processTime,
			String errorType, Status httpStatus, String extraText,
			String network, String station, String location, String channel, String quality,
			Date startTime, Date endTime, String duration) {
	
		LoggerUtils.logMessage(ri, appSuffix, dataSize, processTime,
			errorType, httpStatus.getStatusCode(), extraText,
			network, station, location, channel, quality,
			startTime, endTime, duration);
	}
	
	public static void logUsageMessage(RequestInfo ri, String appSuffix,
			Long dataSize, Long processTime,
			String errorType, Status httpStatus, String extraText) {
	
		LoggerUtils.logMessage(ri, appSuffix, dataSize, processTime,
			errorType, httpStatus.getStatusCode(), extraText);
	}
	
	public static void logAndThrowException(RequestInfo ri, Status httpStatus, String message) {
		ServiceShellException.logAndThrowException(ri, httpStatus, message);
	}	
	
	public RequestInfo ri;
	
	public IrisStreamingOutput() { }

	@Override
	public abstract void write(OutputStream os);

	public abstract Status getResponse();
	public abstract String getErrorString();
	
	public abstract void setRequestInfo(RequestInfo ri);

}
