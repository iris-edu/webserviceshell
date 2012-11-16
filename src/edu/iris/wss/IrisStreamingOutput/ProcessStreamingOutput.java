package edu.iris.wss.IrisStreamingOutput;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Date;


import org.apache.log4j.Logger;

import edu.iris.wss.StreamEater;
import edu.iris.wss.framework.RequestInfo;

import javax.ws.rs.core.Response.Status;


public class ProcessStreamingOutput extends IrisStreamingOutput {
	
	private static final int responseThreadDelayMsec = 50;
	public static final Logger logger = Logger.getLogger(ProcessStreamingOutput.class);

	private Date startTime;
	
	private ProcessBuilder processBuilder = null;
	private Process process;

	private int exitVal;
	
	private InputStream is = null;
	private StreamEater se = null;
	
	public ProcessStreamingOutput() {}
	
	public ProcessStreamingOutput(RequestInfo ri) {
		this();
		this.ri = ri;
	}
	
	public ProcessStreamingOutput(ProcessBuilder pb, RequestInfo ri) {
		this();
		this.initialize(pb, ri);
	}
	
	public void initialize(ProcessBuilder pb, RequestInfo ri) {
		this.processBuilder = pb;
		this.ri = ri;
	}
	
	@Override
	public void setRequestInfo(RequestInfo ri) {
		this.ri = ri;
	}
	
	public Integer getExitVal() {
		return exitVal;
	}
	
	public String getErrorString() {
		if (se == null) return null;

		try {
			return se.getOutputString();
		} catch (IOException ioe) {
			return "No valid error text";
		}
	}
	
	public Status getResponse()  {
		
		startTime = new Date();

		if (processBuilder == null) {
			logMessage(ri, Status.INTERNAL_SERVER_ERROR, "No valid process found.");
		}

		try {
			process = processBuilder.start();
		} catch (IOException ioe) {
			logMessage(ri, Status.INTERNAL_SERVER_ERROR, 
					"Failure starting process\n" + ioe.getMessage());
		}

		ReschedulableTimer rt = new ReschedulableTimer(ri.appConfig.getTimeoutSeconds() * 1000);
		rt.schedule(killIt);
		
		try {
			se = new StreamEater(process, process.getErrorStream());
		} catch (Exception e) {
			logMessage(ri, Status.INTERNAL_SERVER_ERROR, 
					e.getMessage());
		}
	    is = process.getInputStream();
	    
	    // Wait for data, error or timeout.
	    while (true) {
	    	
	    	// Check for process finished.  If error (exitVal != 0), exit with an error.
	    	try {
	    		exitVal = process.exitValue();
//	    		logger.info("Got exitval : " + exitVal);
	    		rt.cancel();
    			return processExitVal(exitVal, ri);		    		
	    	} catch (IllegalThreadStateException itse) {
	    		// Nothing to catch here.  When the process isn't done, this call always
	    		// throws this type of exception.
	    	}
	   	 		    	
	    	try {    		
		    	if (is.available() > 0) {
//		    		logger.info("Got data");
		    		rt.cancel();
		    		return Status.OK;
		    	} else {
		    		// No data available yet.  Just continue looping, waiting for data.
		    		// The timer will kill the process after the defined interval.
		    	}
	    	} catch (IOException ioe) {
	    		// This means the process died or timed out and that the InputStream object
	    		// is no longer valid.  The exit value check above will take care of this 
	    		// during the next cycle through the loop.
	    		logger.error("IO Exception while waiting for data: " + ioe.getMessage());
	    	}
	    		    	
	    	// Sleep for a little while.
	    	try {
	    		Thread.sleep(responseThreadDelayMsec);
	    	} catch (InterruptedException ie) {
	    		;
	    	}
	    }		
	}
	
	private static Status processExitVal(Integer exitVal, RequestInfo ri) {
		
		if (exitVal == 0) {
			return Status.OK;
		}
		
		if (exitVal == 9 + 128) {
			// SIGKILL
			logMessage(ri, Status.INTERNAL_SERVER_ERROR, 
					"Timeout or unexpected termination with prejudice of worker");
		}
		else if (exitVal == 15 + 128) {
			// SIGTERM
			logMessage(ri, Status.INTERNAL_SERVER_ERROR, 
					"Timeout or unexpected termination of worker");
		}
		else {
			logMessage(ri, Status.INTERNAL_SERVER_ERROR, 
					"Termination of workeer with exit code: " + exitVal);
		}
		return Status.OK;		// Won't get here.
	}
	
	@Override
	public void write(OutputStream output) {
		
		long totalBytesTransmitted = 0L;
		int bytesRead;
		byte [] buffer = new byte[1024];		
		
		ReschedulableTimer rt = new ReschedulableTimer(ri.appConfig.getTimeoutSeconds() * 1000);
		rt.schedule(killIt);
		
		try {
			while (true) {
				bytesRead = is.read(buffer, 0, buffer.length);
				if (bytesRead < 0) {
					break;
				}
				totalBytesTransmitted += bytesRead;
				output.write(buffer, 0, bytesRead);
				output.flush();
				rt.reschedule();
			}
		}

		catch (IOException ioe) {			
			logger.error("Got IOE: " + ioe.getMessage());
		}
		catch (Exception e) {
			logger.error("Got Generic Exception: " + e.getMessage());
		
		} finally {
			logger.info("Done:  Wrote " + totalBytesTransmitted + " bytes\n");

			logUsageMessage(ri, totalBytesTransmitted, (new Date()).getTime()- startTime.getTime(),
					null, Status.OK, null);
    		rt.cancel();
    		
    		try {
    			output.close();
    			is.close();
    		} catch (IOException ioe) {
    			// What can one do?
    			;
    		}
		}	
	}

	private Runnable killIt = new Runnable() {
		public void run() {
			stopProcess(process);
		}
	};
	
	private static void stopProcess(Process process)  {
    	// Send a SIGTERM (friendly-like) via the destroy() method.  
    	// Wait 100msec, then terminate with SIGKILL.
    	try {
    		process.destroy();
    		Thread.sleep(100);
    	} catch (InterruptedException ie) {
    		logger.error("TimeoutTask thread got interrrupted.");
    	}
    	
    	// See if the process is still running by getting its exit value.
    	// If still running, it'll throw an IllegaThreadStateException
    	try {
    		int exitVal = process.exitValue();
    		logger.info("Process terminated w/ SIGTERM: " + exitVal);
    	} catch (IllegalThreadStateException itse) {
    		killUnixProcess(process);
    		logger.info("Process recalcitrant.  Killing w/ SIGKILL");
    	}
    }
    
    // Obv. only works w/ Unix processes.
	private static int getUnixPID(Process process) throws Exception
	{
	    if (process.getClass().getName().equals("java.lang.UNIXProcess")) {
	        Class<? extends Process> cl = process.getClass();
	        Field field = cl.getDeclaredField("pid");
	        field.setAccessible(true);
	        Object pidObject = field.get(process);
	        return (Integer) pidObject;
	    } else {
	        throw new IllegalArgumentException("Needs to be a UNIXProcess");
	    }
	}

	private static void killUnixProcess(Process process)
	{
		Integer pid = null;
		try {
			pid = getUnixPID(process);
		} catch (Exception e) {
			logger.error("Couldn't get PID of process: " + e.getMessage());
			return;
		}
		
		try {
			Runtime.getRuntime().exec("kill -9 " + pid).waitFor();
		} catch (Exception e) {
			logger.error("Couldn't kill process w/ PID: " + pid + ": " + e.getMessage());
		}		
	}
}
