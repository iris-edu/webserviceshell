package edu.iris.wss.IrisStreamingOutput;


import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.Ostermiller.util.CircularByteBuffer;

import edu.iris.wss.StreamEater;
import edu.iris.wss.framework.AppConfigurator.OutputType;
import edu.iris.wss.framework.FdsnStatus.Status;
import edu.iris.wss.framework.RequestInfo;
import edu.sc.seis.seisFile.mseed.*;

//import javax.ws.rs.core.Response.Status;


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
	    
	    if (ri.postBody != null) {
	    	logger.info("PB not null");
	    	try{ 
	    		process.getOutputStream().write(ri.postBody.getBytes());
	    		process.getOutputStream().close();
			} catch (IOException ioe) {
				logMessage(ri, Status.INTERNAL_SERVER_ERROR, 
					"Failure writing POST body\n" + ioe.getMessage());
			}
		}
	    
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
	
	public static Status processExitVal(Integer exitVal, RequestInfo ri) {
		
		if (exitVal == 0) {
			return Status.OK;
		} else if (exitVal == 1) {
			if (ri.appConfig.getUse404For204())
				return Status.NOT_FOUND;
			else
				return Status.NO_CONTENT;
		} else if (exitVal == 2) {
			return Status.BAD_REQUEST;
		} else if (exitVal == 3) {
			return Status.REQUEST_ENTITY_TOO_LARGE;
		} else if (exitVal == 4) {
			return Status.INTERNAL_SERVER_ERROR;
		}
		
		
		// These below are for timeouts and other weird errors that shouldn't 
		// really generate errors through this mechanism.
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
					"Termination of worker with unknown exit code: " + exitVal);
		}
		return Status.OK;		// Won't get here.
	}
	
	@Override
	public void write(OutputStream output) {
		if (this.ri.appConfig.getOutputType() == OutputType.SEED) {
			writeSeed(output);
		} else {
			writeNonSeed(output);
		}
	}
	
	public void writeSeed(OutputStream output) {

		long totalBytesTransmitted = 0L;
		int bytesRead;
		
		// Hopefully this is the largest logical record size we'll see.  Used in terminating
		// SeedRecord processing when the circular buffer holds less than this many bytes.
		final int maxSeedRecordSize = 4096;
		
		// Must be bigger (and a multiple) of maxSeedRecordSize 
		byte [] buffer = new byte[32768];	 
		
		HashMap<String, Long> logHash = new HashMap<String, Long>();	

		ReschedulableTimer rt = new ReschedulableTimer(ri.appConfig.getTimeoutSeconds() * 1000);
		rt.schedule(killIt);
		
		CircularByteBuffer cbb = new CircularByteBuffer(CircularByteBuffer.INFINITE_SIZE, false);
		DataInputStream dis = new DataInputStream(cbb.getInputStream());

		SeedRecord sr = null;

		try {
			while (true) {
				
				// Read bytes, keep a running total and write them to the output.
				bytesRead = is.read(buffer, 0, buffer.length);
				if (bytesRead < 0) break;
				
				totalBytesTransmitted += bytesRead;
				output.write(buffer, 0, bytesRead);
				output.flush();
				
				// All the below is only for logging.
				
				// Write the newly read data into the circular buffer.
				cbb.getOutputStream().write(buffer, 0, bytesRead);				
				
				// We're going to parse SEED records out of the circular buffer until the remaining
				// bytes in the buffer get below maxSeedBlockSize (max seed record size).
				while (cbb.getAvailable() >= maxSeedRecordSize) {
					try {						
						sr = SeedRecord.read(dis);
					} catch (Exception e) {
						logger.error("Caught exception in seed parse: " + e.getMessage());
						break;
					}
					if (sr == null) break;
					
					// Parse and log the data header for logging.
					this.processRecord(sr, cbb, logHash);			
				}	
				
				// Reset the timeout timer
				rt.reschedule();
			}
			
			// Finally clear anything out in the buffer.  There might be data left in the circular
			// buffer whose length will be less than maxSeedRecordSize.
			while (cbb.getAvailable() > 0) {
				try {
					sr = SeedRecord.read(dis);
				} catch (Exception e) {
					logger.error("Caught exception in seed parse: " + e.getMessage());
					break;
				}
			
				if (sr != null) {
					this.processRecord(sr, cbb, logHash);					
				}
			}
		}
		catch (IOException ioe) {			
			logger.error("Got IOE: " + ioe.getMessage());
		}
		catch (Exception e) {
			logger.error("Got Generic Exception: " + e.getMessage());
		
		} finally {	
			logger.info("Done:  Wrote direct " + totalBytesTransmitted + " bytes\n");			

			long processingTime = (new Date()).getTime() - startTime.getTime();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

			String oldAppName = ri.appConfig.getAppName();	
			ri.appConfig.setAppName(oldAppName + "_summary");
			Date startDate = null, endDate = null;
			String quality = null;
			
			try {
				quality =   ri.paramConfig.getValue("quality");
				startDate = sdf.parse(ri.paramConfig.getValue("starttime"));
				endDate =   sdf.parse(ri.paramConfig.getValue("endtime"));
			} catch (Exception e) {
				; // Do nothing
			}
			
			logUsageMessage(ri, totalBytesTransmitted, processingTime,
					null, Status.OK, null);
			
			ri.appConfig.setAppName(oldAppName);

			long total = 0;
			for (String key: logHash.keySet()) {
					
				logUsageMessage(ri, logHash.get(key), processingTime,
						null, Status.OK, null,
						LogKey.getNetwork(key), LogKey.getStation(key), LogKey.getLocation(key),
						LogKey.getChannel(key), LogKey.getQuality(key), 
						startDate, endDate, quality);
							
						total += logHash.get(key);		
				logger.info ("Key: " + key + " Bytes: " + logHash.get(key));
			
			}
			logger.info("Hash total: :" + total);
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
	
	private int processRecord(SeedRecord sr, CircularByteBuffer cbb, HashMap<String, Long> logHash) {
		if (sr  instanceof DataRecord) {
			
			DataRecord dr = (DataRecord) sr;
			DataHeader dh = dr.getHeader();
//			logger.info("Read data record: " + dr.getRecordSize());
//			logger.info("Remaining in CBB: " + cbb.getAvailable());
			
			String key = LogKey.makeKey(dh.getNetworkCode().trim(), dh.getStationIdentifier().trim(),
					dh.getLocationIdentifier().trim(), dh.getChannelIdentifier().trim(),
					dh.getQualityIndicator());
		
			if (logHash.containsKey(key)) {
				logHash.put(key, dr.getRecordSize() + logHash.get(key));
			} else {
				logHash.put(key, (long) dr.getRecordSize());
			}
			return dr.getRecordSize();
		}
		return 0;
	}
	
	public void writeNonSeed(OutputStream output) {
		
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
	
	private static class LogKey {
		
		static String makeKey(String n, String s, String l, String c, Character q) {
			return n + "_" + s + "_" + l + "_" + c + "_" + 	q;
		}
		
		static String getNetwork(String key) {
			return key.split("_")[0];
		}
		static String getStation(String key) {
			return key.split("_")[1];
		}
		static String getLocation(String key) {
			return key.split("_")[2];
		}
		static String getChannel(String key) {
			return key.split("_")[3];
		}
		static String getQuality(String key) {
			return key.split("_")[4];
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
