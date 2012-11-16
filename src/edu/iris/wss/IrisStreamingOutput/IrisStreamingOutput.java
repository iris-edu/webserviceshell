package edu.iris.wss.IrisStreamingOutput;

import java.io.OutputStream;

import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.Response.Status;

import edu.iris.wss.framework.RequestInfo;
import edu.iris.wss.framework.ServiceShellException;
import edu.iris.wss.utils.LoggerUtils;

public abstract class IrisStreamingOutput implements StreamingOutput {

	public static void logUsageMessage(RequestInfo ri, Long dataSize, Long processTime,
			String errorType, Status httpStatus, String extraText) {
	
		LoggerUtils.logMessage(ri, dataSize, processTime,
			errorType, httpStatus.getStatusCode(), extraText);
	}
	
	public static void logMessage(RequestInfo ri, Status httpStatus, String message) {
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
