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

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.Ostermiller.util.CircularByteBuffer;

import edu.iris.wss.framework.AppConfigurator;
import edu.iris.wss.framework.FdsnStatus.Status;
import edu.iris.wss.framework.ParameterTranslator;
import edu.iris.wss.framework.RequestInfo;
import edu.iris.wss.framework.WssSingleton;
import edu.iris.wss.framework.Util;
import edu.iris.wss.provider.IrisProcessingResult;
import edu.iris.wss.provider.IrisProcessor;
import edu.iris.wss.utils.WebUtils;
import edu.sc.seis.seisFile.mseed.DataHeader;
import edu.sc.seis.seisFile.mseed.DataRecord;
import edu.sc.seis.seisFile.mseed.SeedRecord;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import javax.ws.rs.core.StreamingOutput;

public class CmdProcessor extends IrisProcessor {
	public static final Logger logger = Logger.getLogger(CmdProcessor.class);

	public static final String outputDirSignature = "outputdir";

	private static final int MONITOR_PROCESS_PAUSE_TIME_MSEC = 50;

	private Date startTime;

	private Process process;

    private class ExitInformation {
        String detailedMsg = null;
        Status status = Status.INTERNAL_SERVER_ERROR;
    }

	private InputStream is = null;
	private StreamEater se = null;

    private final AtomicBoolean isKillingProcess = new AtomicBoolean(false);

    private String epName = null;

    RequestInfo ri;

	public CmdProcessor() {
	}

    @Override
	public IrisProcessingResult getProcessingResults(RequestInfo ri,
          String wssMediaType) {
		startTime = new Date();

        this.ri = ri;
        epName = ri.getEndpointNameForThisRequest();

        // The cmd list is needed here, so run parseQueryParams again,
        // if the cmd list was part of the IrisStreamingOutput interface,
        // this would not be necessary, but it is not, so run it again,
        // any errors should have already been reported, so not checking
        // for existance and runability of handler program here
        String handlerName = ri.appConfig.getHandlerProgram(epName);

        ArrayList<String> cmd = new ArrayList<>(Arrays.asList(
              handlerName.split(Pattern.quote(" "))));
        try {
            // this modifies the cmd list and adds each parameter.
			ParameterTranslator.parseQueryParams(cmd, ri, epName);
		} catch (Exception ex) {
            String briefMsg = this.getClass().getName() + " parameter error: "
                  + ex.getMessage();
            Util.logAndThrowException(ri, Status.BAD_REQUEST, briefMsg, null);
		}

	    ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        processBuilder.directory(new File(ri.appConfig.getWorkingDirectory(epName)));

	    processBuilder.environment().put("REQUESTURL", WebUtils.getUrl(ri.request));
	    processBuilder.environment().put("USERAGENT", WebUtils.getUserAgent(ri.request));
	    processBuilder.environment().put("IPADDRESS", WebUtils.getClientIp(ri.request));
	    processBuilder.environment().put("APPNAME", ri.appConfig.getAppName());
	    processBuilder.environment().put("VERSION", ri.appConfig.getAppVersion());
        processBuilder.environment().put("CLIENTNAME", WebUtils.getClientName(ri.request));
        processBuilder.environment().put("HOSTNAME", WebUtils.getHostname());
		String username = WebUtils.getAuthenticatedUsername(ri.requestHeaders);
		if (AppConfigurator.isOkString(username)) {
            processBuilder.environment().put("AUTHENTICATEDUSERNAME", username);
        }

		if (processBuilder == null) {
			Util.logAndThrowException(ri, Status.INTERNAL_SERVER_ERROR,
					"No valid process found.");
		}

		logger.info("NEW CMD" + processBuilder.command());

		try {
			process = processBuilder.start();
		} catch (IOException ioe) {
            logger.error("getProcessingResults processBuilder.start ex: ", ioe);

			Util.logAndThrowException(ri, Status.INTERNAL_SERVER_ERROR,
					"IOException when starting handler: "
                          + processBuilder.command(),
                    "IOException: " + ioe.getMessage());
		}

		ReschedulableTimer rt = new ReschedulableTimer(
				ri.appConfig.getTimeoutSeconds(epName) * 1000);
		rt.schedule(new killIt(null));

		try {
			se = new StreamEater(process, process.getErrorStream());
		} catch (Exception e) {
            logger.error("getProcessingResults StreamEater exception: ", e);
			Util.logAndThrowException(ri, Status.INTERNAL_SERVER_ERROR,
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
                logger.error("getProcessingResults error writing post body ex: ",
                      ioe);
				Util.logAndThrowException(ri, Status.INTERNAL_SERVER_ERROR,
						"Failure writing POST body\n" + ioe.getMessage());
			}
		}

        boolean isHeadersChecked = false;
        Map<String, String> hdrMap = null;

        // Wait for data, error or timeout.
		while (true) {
			Boolean gotExitValue = false;
            // set exitVal to internal server error, but process should override
            int exitVal = 1;

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
                    if (! isHeadersChecked) {
                        try {
                            hdrMap = checkForHeaders(is,
                                  ri.HEADER_START_IDENTIFIER_BYTES,
                                  ri.HEADER_END_IDENTIFIER_BYTES,
                                  WssSingleton.HEADER_MAX_ACCEPTED_BYTE_COUNT,
                                  "\n", ":");
                        } catch (Exception ex) {
                            logger.error("Exception while checking for headers, ex: "
                                  + ex);
                            ex.printStackTrace();
                        }
                        isHeadersChecked = true;
                    }

                    // Assumed state at this point
                    // 1) headers were read, and there is more data on the
                    //    input stream - so create StreamingOutput object
                    //    and return
                    // 2) headers were read, stream bytes are not available
                    //    for the moment - so loop
                    // 3) exceptions were thrown, they were then caught
                    //    and logged
                    // 3a) if end of input stream - bytes are not available
                    //    and exit value will be determined at the top of
                    //    this loop
                    // 3b) bytes were read upto the maxbuffer size when
                    //    checking for headers, and some non-header data
                    //    was consumed before the max buffer size was reached

                    // so at this time, log any headers read exceptions and
                    // keep going

                    if (is.available() > 0) {
                        // Check availability again as checkForHeaders may
                        // have consumed exactly a number of bytes necessary
                        // to get headers

                        rt.cancel();

                        StreamingOutput so = new StreamingOutput() {
                            @Override
                            public void write(OutputStream output) {
                                if (ri.isWriteToMiniseed()) {
                                    writeMiniSeed(output);
                                } else {
                                    writeNormal(output);
                                }
                            }
                        };
                        IrisProcessingResult ipr =
                              IrisProcessingResult.processStream(so,
                                    wssMediaType, hdrMap);

                        return ipr;
                    } else {
                        // noop, continue to check for data
                    }
                } else {
                    // No data available yet. Just continue looping, waiting for
                    // data.
                    // The timer will kill the process after the defined
                    // interval.
                }
			} catch (IOException ioe) {
                // This means the process died or timed out and that the
                // InputStream object is no longer valid. The exit value
                // check above should take care of this during the next cycle
                // through the loop.
                // note: may loop in here more than once if
                //       MONITOR_PROCESS_PAUSE_TIME_MSEC is less than
                //       ri.appConfig.getSigkillDelay()
				logger.error("IO Exception while waiting for data: "
						+ ioe.getMessage());
			}

			// Exit here on getting an exit value
			if (gotExitValue) {
                ExitInformation exitInfo = processExitVal(exitVal);
                String briefMessage = getStderrMsg(exitVal);

                IrisProcessingResult ipr =
                      IrisProcessingResult.processError(exitInfo.status,
                            briefMessage, exitInfo.detailedMsg);
                return ipr;
            }

			// Sleep for a little while.
			try {
				Thread.sleep(MONITOR_PROCESS_PAUSE_TIME_MSEC);
			} catch (InterruptedException ie) {
                // noop
			}
		}
	}

    /**
     * Note: This method may block while it is waiting for the inputstream
     *       in StreamEater to finish and be closed
     * @param handlerExitCode
     * @return
     */
	public String getStderrMsg(int handlerExitCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("  handler exited");

		if (se == null) {
            sb.append(", stderr stream is null");
        } else {
            String stderrMsg = se.getOutputString();
            if (stderrMsg.length() <= 0) {
                sb.append(", no error message from handler");
            } else {
                sb = new StringBuilder(stderrMsg);
            }
        }

        return sb.toString();
	}

	public ExitInformation processExitVal(Integer handlerExitCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("handler exited, code: ").append(handlerExitCode);
        sb.append("  reason: ");

        Status exitStatus = Status.INTERNAL_SERVER_ERROR;
		if ((handlerExitCode == 0) || (handlerExitCode == 2)) {
            // An exit value of '0' here indicates an 'OK' from handler,
            // but at this point in the processing, it is assumend there
            // was not data on the output stream, so use 204.
			exitStatus = Status.NO_CONTENT;
            sb.append(exitStatus.toString());
		} else if (handlerExitCode == 1) {
			exitStatus = Status.INTERNAL_SERVER_ERROR;
            sb.append(exitStatus.toString());
		} else if (handlerExitCode == 3) {
			exitStatus = Status.BAD_REQUEST;
            sb.append(exitStatus.toString());
		} else if (handlerExitCode == 4) {
			exitStatus = Status.REQUEST_ENTITY_TOO_LARGE;
            sb.append(exitStatus.toString());
		} else if (handlerExitCode == 9 + 128) {
			// SIGKILL
            sb.append("SIGKILL, Enforced timeout or unexpected termination of handler");
		} else if (handlerExitCode == 15 + 128) {
			// SIGTERM
            sb.append("SIGTERM, Timeout or unexpected termination of handler");
		} else {
            sb.append("unknown");
		}

        ExitInformation info = new ExitInformation();
        info.detailedMsg = sb.toString();
        info.status = exitStatus;

        return info;
	}

    /**
     * This method does not time out, it will block on read if there is
     * nothing to read, it expects the caller to be responsible for any
     * time outs control
     *
     * When making the returned map, the key part (i.e. the header name)
     * and value are trimmed and set to lowercase.
     *
     * Also note: This method works for line termination with \n and \r\n
     * because the trim removes the \r
     *
     * @param is
     * @param startId - should be from global definition
     * @param endId - should be from global definition
     * @param maxBufferSize - should be from global definition, some
     *                        references say Apache has 8 KB limit
     * @param headerLineDelimiter
     * @param headerNameValueDelimiter
     * @return
     * @throws Exception
     */
    public static Map checkForHeaders(InputStream is, byte[] startId,
          byte[] endId, int maxBufferSize, String headerLineDelimiter,
          String headerNameValueDelimiter)
          throws Exception {

    if (!is.markSupported()) {
        throw new Exception("Http Headers cannot be detected on this stream"
        + " because stream marking is not supported.");
    }

    is.mark(startId.length);

    byte[] oneByte = new byte[1];
    for (int i1 = 0; i1 < startId.length; i1++) {
        int bytesRead = is.read(oneByte, 0, 1);
        if (bytesRead < 0) {
            // did not read enough bytes to determine if there is a header,
            // so just let the caller continue
            is.reset();
            return new HashMap();
        }
        if (oneByte[0] == startId[i1]) {
            // keep reading and matching characters
            continue;
        } else {
            // data from stream does not include header information
            is.reset();
            return new HashMap();
        }
    }

    byte[] maxBytes = new byte[maxBufferSize];
    int endMatchCnt = 0;
    int keepBytesCnt = 0;
    int i1 = 0;
    for (; i1 < maxBytes.length; i1++) {
        int bytesRead = is.read(oneByte, 0, 1);
        if (bytesRead < 0) {
            // did not read enough bytes to finish
            throw new Exception("Http Headers were not completely read, the"
                  + " stream was closed before the ending identifier: "
                  + WssSingleton.HEADER_END_IDENTIFIER
                  + "  bytes index: " + i1 + "  maxBufferSize: : " + maxBufferSize
                  + "  endMatchCnt: " + endMatchCnt);
        }
        if (oneByte[0] == endId[endMatchCnt]) {
            // keep matching characters until all are matched
            endMatchCnt++;
            if (endMatchCnt == endId.length) {
                // stop reading, the next byte should be data
                keepBytesCnt = (i1 + 1) - endMatchCnt;
                break;
            } else if (endMatchCnt < endId.length) {
                // drop down and store byte
            } else if (endMatchCnt > endId.length) {
                throw new Exception("Http Headers read error, programmer error"
                    + "  bytes index: " + i1);
            }
        } else {
            endMatchCnt = 0;
        }
        maxBytes[i1] = oneByte[0];
    }

    if (endMatchCnt != endId.length) {
        throw new Exception("Http Headers check buffer size too small or"
              + " malformed ending identifier, expected identifier: "
              + WssSingleton.HEADER_END_IDENTIFIER
              + "  bytes index: " + i1 + "  maxBufferSize: : " + maxBufferSize
              + "  endMatchCnt: " + endMatchCnt);
    }

    String headers = new String(maxBytes, 0, keepBytesCnt, "UTF-8");
    String[] headersArr = headers.split(Pattern.quote(headerLineDelimiter));

    Map<String, String> headersMap = new HashMap<>();
    for (int k1 = 0; k1 < headersArr.length; k1++) {
        String header = headersArr[k1];
        int idx = header.indexOf(headerNameValueDelimiter);
        if (idx < 0) { continue; }
        // Note: for the value substring, trim also provides the removal
        // of \r if it is present
        headersMap.put(header.substring(0, idx).trim().toLowerCase(),
              header.substring(idx + 1).trim().toLowerCase());
    }

    return headersMap;
}

////    @Override
////    public void write(OutputStream output) {
////        if (isWriteToMiniseed) {
////            writeMiniSeed(output);
////        } else {
////            writeNormal(output);
////        }
////    }

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
				ri.appConfig.getTimeoutSeconds(epName) * 1000);
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

                if (ri.appConfig.isUsageLogEnabled(epName)) {
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

            if (ri.appConfig.isUsageLogEnabled(epName)) {
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

            // set some arbitrary exit values to help determine if the test
            // for process exit code is failing versus the process itself
            int handlerExitVal = -99999;
            try {
                handlerExitVal = process.waitFor();
                if (handlerExitVal != 0) {
                    ExitInformation exitInfo = processExitVal(handlerExitVal);
                    String briefMessage = getStderrMsg(handlerExitVal);

                    logger.error("writeMiniSeed resource finishing with error,"
                          + "  exitStatus: " + exitInfo.status
                          + "  handlerExitVal: " + handlerExitVal
                          + "  briefMessage: " + briefMessage
                          + "  detailedMsg: " + exitInfo.detailedMsg);
                    try {
                        output.write(AppConfigurator
                                .STREAM_INTERRUPT_INDICATOR.getBytes());
                        output.flush();
                    } catch (IOException ex) {
                        // noop, already trying to handle an error, so nothing else to do
                    }
                }
            } catch (IllegalThreadStateException ex) {
                // ignore exception
                handlerExitVal = -88888;
            } catch (InterruptedException ex) {
                // ignore exception
                handlerExitVal = -77777;
            }

            logger.info("writeMiniSeed done:  Wrote " + totalBytesTransmitted + " bytes"
                    + "  processingTime: " + processingTime
                    + "  timeNotBlocking: " + timeNonBlockingTotal
                    + "  handlerExitVal: " + handlerExitVal);

            ri.statsKeeper.logShippedBytes(totalBytesTransmitted);

            if (ri.appConfig.isUsageLogEnabled(epName)) {
                try {
                    if (isKillingProcess.get()) {
                        Util.logUsageMessage(ri, "_KillitInWriteMiniSeed",
                                totalBytesTransmitted, processingTime,
                                "killit was called, possible timeout waiting"
                                + " for data after intial data flow started",
                                Status.INTERNAL_SERVER_ERROR, epName);
                    } else {
                        Util.logUsageMessage(ri, "_summary", totalBytesTransmitted,
                                processingTime, null, Status.OK, epName);
                    }
                } catch (Exception ex) {
                    logger.error("Error logging MiniSEED response summary , ex: "
                          + ex, ex);
                }

                try {
                    for (String key : logHash.keySet()) {
                        RecordMetaData rmd = logHash.get(key);
                        Util.logWfstatMessage(ri, null, rmd.getSize(), processingTime, null,
                                Status.OK, epName, LogKey.getNetwork(key).trim(),
                                LogKey.getStation(key).trim(), LogKey.getLocation(key).trim(),
                                LogKey.getChannel(key).trim(), LogKey.getQuality(key).trim(),
                                rmd.getStart().convertToCalendar().getTime(),
                                rmd.getEnd().convertToCalendar().getTime(), epName);
                    }
                } catch (Exception ex) {
                    logger.error("Error logging MiniSEED response for record count: "
                          + logHash.size() + "  ex: " + ex, ex);
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

	public void writeNormal(OutputStream output) {

		long totalBytesTransmitted = 0L;
		int bytesRead;
		byte[] buffer = new byte[1024];

		ReschedulableTimer rt = new ReschedulableTimer(
				ri.appConfig.getTimeoutSeconds(epName) * 1000);
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

            // set some arbitrary exit values to help determine if the test
            // for process exit code is failing versus the process itself
            int handlerExitVal = -99999;
            try {
                handlerExitVal = process.waitFor();
                if (handlerExitVal != 0) {

                    ExitInformation exitInfo = processExitVal(handlerExitVal);
                    String briefMessage = getStderrMsg(handlerExitVal);

                    logger.error("writeNormal resource finishing with error,"
                          + "  exitStatus: " + exitInfo.status
                          + "  handlerExitVal: " + handlerExitVal
                          + "  briefMessage: " + briefMessage
                          + "  detailedMsg: " + exitInfo.detailedMsg);
                    try {
                        output.write(AppConfigurator
                                .STREAM_INTERRUPT_INDICATOR.getBytes());
                        //output.write(msg.getBytes());
                        output.flush();
                    } catch (IOException ex) {
                        // noop, already trying to handle an error, so nothing else to do
                        handlerExitVal = -66666;
                    }
                }
            } catch (IllegalThreadStateException ex) {
                // ignore exception
                handlerExitVal = -88888;
            } catch (InterruptedException ex) {
                // ignore exception
                handlerExitVal = -77777;
            }

            logger.info("writeNormal done:  Wrote " + totalBytesTransmitted + " bytes"
                    + "  processingTime: " + processingTime
                    + "  timeNotBlocking: " + timeNonBlockingTotal
                    + "  handlerExitVal: " + handlerExitVal);

            ri.statsKeeper.logShippedBytes(totalBytesTransmitted);

            if (ri.appConfig.isUsageLogEnabled(epName)) {
                try {
                    if (isKillingProcess.get()) {
                        Util.logUsageMessage(ri, "_KillitInWriteNormal",
                              totalBytesTransmitted, processingTime,
                              "killit was called, possible timeout waiting for"
                              + " data after intial data flow started",
                              Status.INTERNAL_SERVER_ERROR, epName);
                    } else {
                        Util.logUsageMessage(ri, null, totalBytesTransmitted,
                                processingTime, null, Status.OK, epName);
                    }
                } catch (Exception ex) {
                    logger.error("Error logging writeNormal response, ex: "
                          + ex, ex);
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

	public static String getBaseFilename(String filename) {
		int slashIndex = filename.lastIndexOf('/');
		String baseFilename = filename.substring(slashIndex + 1);
		return baseFilename;
	}

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

    private static String writeInterruptedMsg(OutputStream output) {
        if (output != null) {
            // My assumption, if Jersey framework never recieves data, the
            // output stream will have never been opened, so if this method
            // is called previous to output stream open, output will be null.

            try {
                logger.error("stopProcess called for timeout or exception,"
                        + " an error message is being appended"
                        + " to the output stream before killing the handler.");
                output.write(AppConfigurator.STREAM_INTERRUPT_INDICATOR.getBytes());
                output.flush();
            } catch (IOException ex) {
                // noop, already trying to handle an error, so nothing else to do
            }

        } else {
            logger.error("stopProcess called for timeout or exception, outputStream"
            + " is null, this indicates the handler never wrote data.");
        }

        return "no writeInterruptedMsg message";
    }

	private static void stopProcess(Process process, Integer sigkillDelay,
            OutputStream output) {

        // writing non-miniseed data to end of stream as flag to indicate that
        // probably the desired data transfer was not completed and
        // the process is about to be killed.

        // from https://stackoverflow.com/questions/19456313/simple-timeout-in-java
        ExecutorService executor = Executors.newSingleThreadExecutor();

        final Future<String> handler = executor.submit(new Callable() {
            @Override
            public String call() throws Exception {
                return writeInterruptedMsg(output);
            }
        });

        try {
            handler.get(AppConfigurator.INTERRUPT_WRITE_TIMEOUT.toMillis(),
                  TimeUnit.MILLISECONDS);
        }
        catch (Exception ex) {
            // this is to avoid deadlock when writting to output stream, which
            // can cause a stalled request.  It should be relatively rare,
            // re-evaluate if this log message shows up frequently
            handler.cancel(true);
            logger.info("The executor timer tripped while trying to write"
                  + " interrupt message, ignore and continue");
        }

		// Send a SIGTERM (friendly-like) via the destroy() method.
		// Wait for sigkillDelay msec, then terminate with SIGKILL.

        try {
            logger.info("Stopping process, and waitFor with delay: " + sigkillDelay);
            process.destroy();

            //Thread.sleep(sigkillDelay * 1000);
            // waitFor may return faster if the process handles the terminate
            // exits before sigKillDelay time is exceeded.
            process.waitFor(sigkillDelay, TimeUnit.SECONDS);
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
}
