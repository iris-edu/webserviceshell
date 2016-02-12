/*******************************************************************************
 * Copyright (c) 2015 IRIS DMC supported by the National Science Foundation.
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

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

public class StreamEater implements Runnable  {
	public static final Logger logger = Logger.getLogger(StreamEater.class);
	private static final int responseThreadDelayMsec = 50;

	InputStream inputStream;

	boolean     done    = false;
	static final int stringSizeLimit = 20000;

    // accumulate stderr output here
	StringBuilder output = new StringBuilder();

	IOException ioExceptionWhileReading = null;

    Process process;

	public StreamEater(Process process, InputStream is ) throws Exception {
        this.process = process;

		if (is == null) {
			throw new Exception("Null InputStream");
		}
		inputStream = is;
		
		// Automatically start
		this.start();
	}

	private void start() {
        // Create a new thread and start it
        Thread thread = new Thread(this, "StreamEater");
        thread.start();
	}

	public synchronized String getOutputString() {
		// wait until done, then return the string
        String waitMsg = "";

        if (!done) {
			try {
				wait();
			} catch (InterruptedException e) {
                String msg = "Error message output was interrupted while"
                      + " waiting for stderr read to finish";
                output.append(" -- ").append(msg);
				logger.info(waitMsg);
            }
		}
			
		// Probably timed out if an IO exception has occurred
		if( ioExceptionWhileReading != null )  {
            output.append(" -- ")
                  .append("Exception while reading stderr, excep: ")
                  .append(ioExceptionWhileReading.getMessage());
		}

		return output.toString();
	}

    @Override
	public void run() {
		byte [] buffer = new byte[1024];
		int nRead = -2 ;
		try {
            while (process.isAlive()) {
                if (inputStream.available() > 0) {
                    try {
                        while ((nRead = inputStream.read(buffer, 0, buffer.length)) != -1) {
                            if (output.length() + nRead <= stringSizeLimit) {
                                output.append(new String(buffer, 0, nRead));
                            } else {
                                output.append(" -- ERROR, error message size limit exceeded");
                                output.append(", sizeLimit: ").append(stringSizeLimit);
                            }
                        }
                    } catch(IOException e ) {
                        logger.info("Got IOException in stream eater, e: " + e.getMessage());
                        ioExceptionWhileReading = e;
                    } finally {
                        synchronized (this) {
                            // Set done and notify any waiting threads.
                            //Typically, somebody calling getOutputString()
                            done = true;
                            notify();
                        }
                        try { inputStream.close(); } catch( Exception e) {;} // noop
                    }
                } else {
                    try { Thread.sleep(responseThreadDelayMsec); } catch( Exception e) {;} // noop
                }
            }
        } catch (IOException e) {
            logger.info("Got IOException in stream eater, e: " + e.getMessage());
            ioExceptionWhileReading = e;
        } finally {
            synchronized (this) {
                // Set done and notify any waiting threads.
                //Typically, somebody calling getOutputString()
                done = true;
                notify();
            }
            try {
                inputStream.close();
            } catch (Exception e) {;
            } // noop
        }
    }
}

