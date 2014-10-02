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

package edu.iris.wss.IrisStreamingOutput;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;

import com.Ostermiller.util.CircularByteBuffer;

import edu.iris.wss.StreamEater;
import edu.iris.wss.framework.AppConfigurator;
import edu.iris.wss.framework.AppConfigurator.OutputType;
import edu.iris.wss.framework.FdsnStatus.Status;
import edu.iris.wss.framework.RequestInfo;
import edu.sc.seis.seisFile.mseed.DataHeader;
import edu.sc.seis.seisFile.mseed.DataRecord;
import edu.sc.seis.seisFile.mseed.SeedRecord;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProcessStreamingOutput extends IrisStreamingOutput {

	public static final String outputDirSignature = "outputdir";

	private static final int responseThreadDelayMsec = 50;
	public static final Logger logger = Logger
			.getLogger(ProcessStreamingOutput.class);

	private Date startTime;

	private ProcessBuilder processBuilder = null;
	private Process process;

	private int exitVal;

	private InputStream is = null;
	private StreamEater se = null;
    
    private AtomicBoolean isKillingProcess = new AtomicBoolean(false);

	// [region] Constructors and Getters

	public ProcessStreamingOutput() {
	}

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

    // Note: this is broken if ever threaded, exitVal should be
    // passed in, not a global, but it means a change to the
    // interface
    @Override
	public String getErrorString() {
        return getErrorString(exitVal);
	}
    
	public String getErrorString(int exitCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("  exit code: ").append(exitCode);

		if (se == null) {
            sb.append("  error: none, stderr is null");
        } else {

            try {
                if (se.getOutputString().length() <= 0) {
                    sb.append("  error: none, message is zero length");
                } else {
                    sb.append("  error: ").append(se.getOutputString());
                }
            } catch (IOException ioe) {
                sb.append("  error: none, IOException reading stderr");
            }
        }
        
        return sb.toString();
	}

	// [end region]

	// [region] Response Method

    @Override
	public Status getResponse() {
		startTime = new Date();

		if (processBuilder == null) {
			logAndThrowException(ri, Status.INTERNAL_SERVER_ERROR,
					"No valid process found.");
		}

		if (ri.appConfig.getOutputType() == OutputType.ZIP) {
			// Create a sub-directory for the results based off of the working
			// Directory
			String wd = ri.appConfig.getWorkingDirectory();
			try {
				Path p = Paths.get(wd);
				Path tempDir = Files.createTempDirectory(p, "wsszip");
				ri.workingSubdirectory = tempDir.toString();

				processBuilder.command().add("--" + outputDirSignature);
				processBuilder.command().add(ri.workingSubdirectory);

			} catch (IOException ioe) {
				logAndThrowException(ri, Status.INTERNAL_SERVER_ERROR,
						"Could not create temp directory in: " + wd);
			}
		}

		logger.info("NEW CMD" + processBuilder.command());

		try {
			process = processBuilder.start();
		} catch (IOException ioe) {
            logger.error("getResponse processBuilder.start ex: ", ioe);

			logAndThrowException(ri, Status.INTERNAL_SERVER_ERROR,
					"IOException when starting handler");
			// + ioe.getMessage());
		}

		ReschedulableTimer rt = new ReschedulableTimer(
				ri.appConfig.getTimeoutSeconds() * 1000);
		rt.schedule(new killIt(null));

		try {
			se = new StreamEater(process, process.getErrorStream());
		} catch (Exception e) {
            logger.error("getResponse StreamEater exception: ", e);
			logAndThrowException(ri, Status.INTERNAL_SERVER_ERROR,
					("Ex msg: " + e.getMessage()) );
		}
		is = process.getInputStream();
 
        if (ri.postBody != null) {
            try {

// mls 2014-06-19 - code to make post from html forms work... worked once with curl as well
// not implementing until time to test for side affects related to url encoding or not.
// CONTENT_TYPE may not match encoding from curl, depending on the combination
//  of curl options. e.g. curl does not set "query=" like 
// htmls forms, but CONTENT_TYPE is set to application/x-www-form-urlencoded
// even when the body was not encoded. so curl does not control CONTENT_TYPE
// versus what is sent using the --data-binary option.
// another approach, check USER-AGENT for "curl" and only do this code for
//  non-curl agents
//
//        System.out.println("****************** CONTENT_TYPE: "
//            + ri.requestHeaders.getRequestHeader(HttpHeaders.CONTENT_TYPE)
//            + "  USER_AGENT: " + ri.requestHeaders.getRequestHeader(HttpHeaders.USER_AGENT));
//
//        System.out.println("***************** postBody: " + ri.postBody);
//                
//        String urlDecoded = null;
//        try {
//            // code to make queries work from browsers, via html forms
//            urlDecoded = java.net.URLDecoder.decode(ri.postBody, "UTF-8");
//            urlDecoded = urlDecoded.replace("query=", "");
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//        System.out.println("***************** postBoUD: " + urlDecoded);
// 		  process.getOutputStream().write(urlDecoded.getBytes());
//                               
				process.getOutputStream().write(ri.postBody.getBytes());
				process.getOutputStream().close();
			} catch (IOException ioe) {
                logger.error("getResponse post to output stream ex: ", ioe);
				logAndThrowException(ri, Status.INTERNAL_SERVER_ERROR,
						"Failure writing POST body\n" + ioe.getMessage());
			}
		}

		// Wait for data, error or timeout.
		while (true) {
			Boolean gotExitValue = false;

			// Check for process finished. If error (exitVal != 0), exit with an
			// error.
			try {
				exitVal = process.exitValue();
				gotExitValue = true;
				rt.cancel();
			} catch (IllegalThreadStateException itse) {
				// Nothing to do here. IllegalThreadStateException is thrown
                // when process is not yet terminated
			}

			try {
				if (is.available() > 0) {
                    rt.cancel();
                    return Status.OK;
				} else {
					// No data available yet. Just continue looping, waiting for
					// data.
					// The timer will kill the process after the defined
					// interval.
				}
			} catch (IOException ioe) {
				// This means the process died or timed out and that the
				// InputStream object
				// is no longer valid. The exit value check above will take care
				// of this
				// during the next cycle through the loop.
                // note: may loop in here more than once if responseThreadDelayMsec
                //       is less than ri.appConfig.getSigkillDelay()
				logger.error("IO Exception while waiting for data: "
						+ ioe.getMessage());
			}

			// Exit here on getting an exit value so that any data that is in
			// the system
			// which would change the response from 'NO_DATA' to 'OK' is read in
			// the section above.
			if (gotExitValue) {
				return processExitVal(exitVal, ri);
            }

			// Sleep for a little while.
			try {
				Thread.sleep(responseThreadDelayMsec);
			} catch (InterruptedException ie) {
				;
			}
		}
	}

	public Status processExitVal(Integer exitVal, RequestInfo ri) {

		// An exit value of '0' here indicates an 'OK' return, but obv.
		// with no data. Therefore, interpret it as 204.
		if ((exitVal == 0) || (exitVal == 2)) {
			if (ri.appConfig.getUse404For204())
				return Status.NOT_FOUND;
			else
				return Status.NO_CONTENT;
		} else if (exitVal == 1) {
            logger.error("Handler exited," + this.getErrorString(exitVal));
			return Status.INTERNAL_SERVER_ERROR;
		} else if (exitVal == 3) {
			return Status.BAD_REQUEST;
		} else if (exitVal == 4) {
			return Status.REQUEST_ENTITY_TOO_LARGE;
		}

		if (exitVal == 9 + 128) {
			// SIGKILL
			logAndThrowException(ri, Status.INTERNAL_SERVER_ERROR,
					"Enforced timeout or unexpected termination of handler,"
                            + this.getErrorString(exitVal));
		} else if (exitVal == 15 + 128) {
			// SIGTERM
			logAndThrowException(ri, Status.INTERNAL_SERVER_ERROR,
					"Timeout or unexpected termination of handler,"
                            + this.getErrorString(exitVal));
		} else {
			logAndThrowException(ri, Status.INTERNAL_SERVER_ERROR,
					"Unexpected termination of handler,"
                            + this.getErrorString(exitVal));;
		}

		return Status.OK; // Won't get here.
	}

	// [end region]

	@Override
	public void write(OutputStream output) {
		if (this.ri.appConfig.getOutputType() == OutputType.MSEED
            || this.ri.appConfig.getOutputType() == OutputType.MINISEED) {
			writeMiniSeed(output);
		} else if (this.ri.appConfig.getOutputType() == OutputType.ZIP) {
			writeZip(output);
		} else {
			writeNormal(output);
		}
	}

	// [region] Seed writer

    /**
     * Reads stdin and writes to stdout, To capture processing statistics, the
     * data is also parsed as miniseed formated data. Primarily to get the 
     * number of bytes per channel.
     * 
     * @param output 
     */
	public void writeMiniSeed(OutputStream output) {

		long totalBytesTransmitted = 0L;
		int bytesRead;

		// Hopefully this is the largest logical record size we'll see. Used in
		// terminating
		// SeedRecord processing when the circular buffer holds less than this
		// many bytes.
		final int maxSeedRecordSize = 4096;
		final int minSeedRecordSize = 256;
		final int defaultSeedRecordSize = 4096;

		// Must be bigger (and a multiple) of maxSeedRecordSize
		byte[] buffer = new byte[32768];

		HashMap<String, RecordMetaData> logHash = new HashMap<>();

		ReschedulableTimer rt = new ReschedulableTimer(
				ri.appConfig.getTimeoutSeconds() * 1000);
		rt.schedule(new killIt(output));

		CircularByteBuffer cbb = new CircularByteBuffer(
				CircularByteBuffer.INFINITE_SIZE, false);
		DataInputStream dis = new DataInputStream(cbb.getInputStream());

		SeedRecord sr = null;
        
        // processing time, but excluding while read is blocking
        long timeNonBlockingStart = 0L;
        long timeNonBlockingTotal = 0L;

		boolean badSeedParsingLogged = false;

		try {
			while (true) {

				// Read bytes, keep a running total and write them to the
				// output.
				bytesRead = is.read(buffer, 0, buffer.length);
                timeNonBlockingStart = System.currentTimeMillis();
				if (bytesRead < 0) {
					break;
				}

				totalBytesTransmitted += bytesRead;
				output.write(buffer, 0, bytesRead);
				output.flush();

                if (ri.appConfig.getUsageLog()) {
                    // All the below is only for usage logging.

                    // Write the newly read data into the circular buffer.
                    cbb.getOutputStream().write(buffer, 0, bytesRead);

				// We're going to parse miniSEED records out of the circular buffer
                    // until the remaining
                    // bytes in the buffer get below maxSeedBlockSize (max seed
                    // record size).
				// IMPORTANT: The SeedRecord.read() call blocks, so we def. do
                    // _NOT_ want to allow that to
                    // happen. Therefore we _only_ call this when we have at least 1
                    // block of miniSeed.
                    while (cbb.getAvailable() >= maxSeedRecordSize) {
                        try {
                            sr = SeedRecord.read(dis, defaultSeedRecordSize);
                        } catch (Exception e) {
                            if (!badSeedParsingLogged) {
                                badSeedParsingLogged = true;
                                logger.error("MiniSEED format exception in SeedRecord.read"
                                        + "  bytes transmitted so far: " + totalBytesTransmitted
                                        + "  exception: " + e);
                            }

						// The parser barfed. Skip ahead through the remainder
                            // of this buffer.
                            byte[] trash = new byte[cbb.getAvailable()
                                    - maxSeedRecordSize];
                            dis.read(trash, 0, cbb.getAvailable()
                                    - maxSeedRecordSize);
                            break;
                        }

                        // Parse and log the data header for logging.
                        this.processRecord(sr, logHash);
                    }
                }

				// Reset the timeout timer;
				rt.reschedule();
                
                timeNonBlockingTotal += System.currentTimeMillis()
                        - timeNonBlockingStart;
			}

            if (ri.appConfig.getUsageLog()) {
			// Finally clear anything out in the buffer. There might be data
                // left in the circular
                // buffer whose length will be less than maxSeedRecordSize.
                // SeedRecord.read will block with a broken SEED record and will
                // reset the stream forever
                // So it's important to only try to decode the remaining data if it
                // could be a full SEED
                // record.

			// That said, a corrupt or truncated piece of SEED will hang the
                // SeisFile decoder and a
                // time out will occur.
                long lastAvailable = cbb.getAvailable();
                while (cbb.getAvailable() >= minSeedRecordSize) {
                    try {
                        sr = null;
                        sr = SeedRecord.read(dis);
                        this.processRecord(sr, logHash);
                    } catch (Exception e) {
                        logger.error("SeedRecord.read or processRecord ex: ", e);
                        break;
                    } finally {
                        if (cbb.getAvailable() == lastAvailable) {
                            // Nothing got parsed. Get the hell out of here.
                            logger.info("Unexpected miniseed data found while"
                                    + " getting usage statistics");
                            break;
                        } else {
                            lastAvailable = cbb.getAvailable();
                        }
                    }
                }
            }
            
            timeNonBlockingTotal += System.currentTimeMillis()
                    - timeNonBlockingStart;
		} catch (IOException ioe) {
			logger.error("Got IOE (probable client disconnect): ", ioe);
			stopProcess(process, ri.appConfig.getSigkillDelay(), output);
		} catch (Exception e) {
			logger.error("Miniseed parse error or process record exception: ", e);
			stopProcess(process, ri.appConfig.getSigkillDelay(), output);
		} finally {
            long processingTime = (new Date()).getTime() - startTime.getTime();
            logger.info("writeMiniSeed done:  Wrote " + totalBytesTransmitted + " bytes"
                    + "  processingTime: " + processingTime
                    + "  timeNotBlocking: " + timeNonBlockingTotal);
            ri.statsKeeper.logShippedBytes(totalBytesTransmitted);

            if (ri.appConfig.getUsageLog()) {
                try {
                    if (isKillingProcess.get()) {
                        logUsageMessage(ri, "_KillitInWriteMiniSeed", 0L,
                                processingTime,
                                "killit was called, possible timeout waiting"
                                + " for data after intial data flow started",
                                Status.INTERNAL_SERVER_ERROR, null);
                    } else {
                        logUsageMessage(ri, "_summary", totalBytesTransmitted,
                                processingTime, null, Status.OK, null);
                    }
                } catch (Exception ex) {
                    logger.error("Error logging MiniSEED response, ex: " + ex);
                }

                for (String key : logHash.keySet()) {
                    RecordMetaData rmd = logHash.get(key);
                    logUsageMessage(ri, null, rmd.getSize(), processingTime, null,
                            Status.OK, null, LogKey.getNetwork(key).trim(),
                            LogKey.getStation(key).trim(), LogKey.getLocation(key).trim(),
                            LogKey.getChannel(key).trim(), LogKey.getQuality(key).trim(),
                            rmd.getStart().convertToCalendar().getTime(),
                            rmd.getEnd().convertToCalendar().getTime(), null);
                }
            }
            
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

	protected static class LogKey {

		static String makeKey(String n, String s, String l, String c,
				Character q) {
            StringBuilder key = new StringBuilder();
            key.append(n).append("_");
            key.append(s).append("_");
            key.append(l).append("_");
            key.append(c).append("_");
            key.append(q);
			return key.toString();
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

	private void processRecord(SeedRecord sr,
			HashMap<String, RecordMetaData> logHash) throws ParseException {
		if (sr instanceof DataRecord) {

			DataRecord dr = (DataRecord) sr;
			DataHeader dh = dr.getHeader();

            String key = LogKey.makeKey(dh.getNetworkCode(),
                    dh.getStationIdentifier(), dh.getLocationIdentifier(),
                    dh.getChannelIdentifier(), dh.getQualityIndicator());

			RecordMetaData rmd = logHash.get(key);
            
			if (rmd != null) {
				rmd.setIfEarlier(dh.getStartBtime());
				rmd.setIfLater(dh.getLastSampleBtime());
				rmd.setSize(rmd.getSize() + (long) dr.getRecordSize());
			} else {
				rmd = new RecordMetaData();
				rmd.setSize((long) dr.getRecordSize());
				rmd.setIfEarlier(dh.getStartBtime());
				rmd.setIfLater(dh.getLastSampleBtime());
				logHash.put(key, rmd);
			}
			/*
			 * if (logHash.containsKey(key)) {
			 * 
			 * logHash.put(key, dr.getRecordSize() + logHash.get(key)); } else {
			 * logHash.put(key, (long) dr.getRecordSize()); }
			 */
		}
	}

	// [end region]

	// [region] Normal writer

	public void writeNormal(OutputStream output) {

		long totalBytesTransmitted = 0L;
		int bytesRead;
		byte[] buffer = new byte[1024];

		ReschedulableTimer rt = new ReschedulableTimer(
				ri.appConfig.getTimeoutSeconds() * 1000);
		rt.schedule(new killIt(output));

        // processing time, but excluding while read is blocking
        long timeNonBlockingStart = 0L;
        long timeNonBlockingTotal = 0L;
        
		try {
			while (true) {
				bytesRead = is.read(buffer, 0, buffer.length);
                timeNonBlockingStart = System.currentTimeMillis();
				if (bytesRead < 0) {
					break;
				}
				totalBytesTransmitted += bytesRead;
				output.write(buffer, 0, bytesRead);
				output.flush();
				rt.reschedule();
                timeNonBlockingTotal += System.currentTimeMillis()
                        - timeNonBlockingStart;
			}
		}

		catch (IOException ioe) {
			logger.error("Got IOE (probable client disconnect): ", ioe);
			stopProcess(process, ri.appConfig.getSigkillDelay(), output);
		} catch (Exception e) {
			logger.error("Read buffer in writeNormal exception: ", e);
			stopProcess(process, ri.appConfig.getSigkillDelay(), output);
		} finally {
            long processingTime = (new Date()).getTime() - startTime.getTime();
            logger.info("writeNormal done:  Wrote " + totalBytesTransmitted + " bytes"
                    + "  processingTime: " + processingTime
                    + "  timeNotBlocking: " + timeNonBlockingTotal);
            ri.statsKeeper.logShippedBytes(totalBytesTransmitted);

            if (ri.appConfig.getUsageLog()) {
                if (isKillingProcess.get()) {
                    logUsageMessage(ri, "_KillitInWriteNormal", 0L,
                            processingTime,
                            "killit was called, possible timeout waiting for data after intial data flow started",
                            Status.INTERNAL_SERVER_ERROR, null);
                } else {
                    logUsageMessage(ri, null, totalBytesTransmitted,
                            processingTime, null, Status.OK, null);
                }
            }

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

	// [end region]

	// [region] Zip writer and temp directory utils.

	public void writeZip(OutputStream output) {

		long totalBytesTransmitted = 0L;
		int bytesRead;
		byte[] buffer = new byte[32768];

		ReschedulableTimer rt = new ReschedulableTimer(
				ri.appConfig.getTimeoutSeconds() * 1000);
		rt.schedule(new killIt(output));

		String line = null;
		ZipOutputStream zipOutStream = new ZipOutputStream(output);

        // processing time, but excluding while read is blocking
        long timeNonBlockingStart = 0L;
        long timeNonBlockingTotal = 0L;

		// Read the InputStream is, a line at a time. Each line should be a
		// filename
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(is));

			while ((line = in.readLine()) != null) {
                timeNonBlockingStart = System.currentTimeMillis();
				rt.reschedule();
				line = line.trim();
				String fname = getBaseFilename(line);
				if (fname.trim().equals(""))
					continue;
				logger.info("Line:#" + line + "# Filename: " + fname);

				// Read the file (if it exists and put it on the Zip output
				// stream
				File inFile = new File(line);
				FileInputStream fis = new FileInputStream(inFile);
				BufferedInputStream bis = new BufferedInputStream(fis);

				zipOutStream.putNextEntry(new ZipEntry(fname));
				while ((bytesRead = bis.read(buffer)) > 0) {
					totalBytesTransmitted += bytesRead;
					zipOutStream.write(buffer, 0, bytesRead);
				}
				zipOutStream.flush();
				zipOutStream.closeEntry();
				if (bis != null)
					bis.close();
				if (fis != null)
					fis.close();
				deleteTempDirectory(inFile);
                timeNonBlockingTotal += System.currentTimeMillis()
                        - timeNonBlockingStart;
			}
		} catch (FileNotFoundException fnfe) {
			logger.error("File not found: " + line
            + "  ex: " + fnfe);
		} catch (IOException ioe) {
			logger.error("Got IOE (probable client disconnect): ", ioe);
			stopProcess(process, ri.appConfig.getSigkillDelay(), output);
		} catch (Exception e) {
			logger.error("Readline in writeZip exception: ", e);
			stopProcess(process, ri.appConfig.getSigkillDelay(), output);
		} finally {
            long processingTime = (new Date()).getTime() - startTime.getTime();
            logger.info("writeZip done:  Wrote " + totalBytesTransmitted + " bytes"
                    + "  processingTime: " + processingTime
                    + "  timeNotBlocking: " + timeNonBlockingTotal);
            ri.statsKeeper.logShippedBytes(totalBytesTransmitted);

            if (ri.appConfig.getUsageLog()) {
                if (isKillingProcess.get()) {
                    logUsageMessage(ri, "_KillitInWriteZip", 0L,
                            processingTime,
                            "killit was called, possible timeout waiting for data after intial data flow started",
                            Status.INTERNAL_SERVER_ERROR, null);
                } else {
                    logUsageMessage(ri, null, totalBytesTransmitted,
                            processingTime, null, Status.OK, null);
                }
            }

			rt.cancel();

			try {
				zipOutStream.close();
				output.close();
				is.close();
			} catch (IOException ioe) {
				// What can one do?
			}
		}

		// Clean up any working subdirectory and files / subdirectories it may
		// contain.
		if (ri.workingSubdirectory != null) {
			deleteTempDirectory(new File(ri.workingSubdirectory));
		}
	}

	public static void deleteTempDirectory(File f) {

		try {
			if (f.isDirectory()) {
				for (File c : f.listFiles())
					deleteTempDirectory(c);
			}
			if (!f.delete())
				logger.error("Couldn't delete: " + f);
		} catch (Exception e) {
			logger.info("Exception in temporary directory cleaning: ", e);
		}
	}

	public static String getBaseFilename(String filename) {
		int slashIndex = filename.lastIndexOf('/');
		String baseFilename = filename.substring(slashIndex + 1);
		return baseFilename;
	}

	// [end region]

	// [region] Process killing utilities

    private class killIt implements Runnable {
        OutputStream outputStream;
        killIt(OutputStream output) {
            outputStream = output;
        }
        @Override
        public void run() {
            logger.info("Killit ran");
            isKillingProcess.getAndSet(true);
            stopProcess(process, ri.appConfig.getSigkillDelay(),
                    outputStream);
        }
    };

	private static void stopProcess(Process process, Integer sigkillDelay,
            OutputStream output) {
        
        // writing non-miniseed data to end of stream as flag to indicate that
        // probably the desired data transfer was not completed and 
        // webserviceshell is about the stop the connection.
        
        if (output != null) {
            // My assumption, if Jersey framework never recieves data, the
            // output stream will have never been opened, so if this method
            // is called previous to output stream open, output will be null.
            try {
                logger.error("stopProcess called for timeout or exception,"
                        + " an error message is being appended"
                        + " to the output stream before killing the handler.");
                output.write(AppConfigurator.miniseedStreamInterruptionIndicator.getBytes());
                output.flush();
            } catch (IOException ex) {
                // noop, already trying to handle an error, so nothing else to do
            }
        } else {
            logger.error("stopProcess called for timeout or exception, outputStream"
            + " is null, this indicates the handler never wrote data.");
        }
        
		// Send a SIGTERM (friendly-like) via the destroy() method.
		// Wait for sigkillDelay msec, then terminate with SIGKILL.
		try {
            logger.info("Stopping process, and waiting with delay: " + sigkillDelay);
			process.destroy();
			Thread.sleep(sigkillDelay);
		} catch (InterruptedException ie) {
			logger.error("stopProcess Thread.sleep interrrupted: " + ie);
		}

		// See if the process is still running by getting its exit value.
		// If still running, it'll throw an IllegaThreadStateException
		try {
			int exitVal = process.exitValue();
			logger.info("Process terminated, exit code: " + exitVal);
		} catch (IllegalThreadStateException itse) {
			killUnixProcess(process);
			logger.info("Process recalcitrant.  Killing w/ SIGKILL");
		}
	}

	// Obv. only works w/ Unix processes.
	private static int getUnixPID(Process process) throws Exception {
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

	private static void killUnixProcess(Process process) {
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
			logger.error("Couldn't kill process w/ PID: " + pid + ": "
					+ e.getMessage());
		}
	}

	// [end region]

}
