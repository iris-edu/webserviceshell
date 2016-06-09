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
	public static final Logger LOGGER = Logger.getLogger(StreamEater.class);
	private static final int MONITOR_STREAM_PAUSE_TIME_MSEC = 50;

	InputStream inputStream;

	boolean done = false;

    // accumulate stderr content here
	static final int OUTPUT_SIZE_LIMIT = 20000;
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
			} catch (InterruptedException ex) {
                String msg = "Interrupted while waiting for stderr read, ex: ";
                output.append(" -- ").append(msg).append(ex);
				LOGGER.warn(waitMsg);
            }
		}
			
		// addition information indicating stderr processing may not
        // be working as expected.
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
                    readContents(nRead, buffer);
                } else {
                    try { Thread.sleep(MONITOR_STREAM_PAUSE_TIME_MSEC); }
                    catch( Exception ex) {;} // noop
                }
            }

            // There can be zero bytes available while process is alive, but
            // after isalive goes false, bytes may become available.
            if (inputStream.available() > 0) {
                readContents(nRead, buffer);
            }
        } catch (IOException ex) {
            LOGGER.warn("IOException while checking if stderr available, ex: "
                  + ex.getMessage());
            ioExceptionWhileReading = ex;
        } finally {
            synchronized (this) {
                // Set done and notify any waiting threads.
                // Typically, somebody calling getOutputString()
                done = true;
                notify();
            }
            try {
                inputStream.close();
            } catch (Exception ex) {
                ; // noop
            }
        }
    }

    private void readContents(int byteCount, byte [] byteBuffer) {
        try {
            while ((byteCount =
                  inputStream.read(byteBuffer, 0, byteBuffer.length)) != -1) {
                // NOTE: output length is character count, read length is
                //       byte count.
                if ((output.length() + byteCount) <= OUTPUT_SIZE_LIMIT) {
                    output.append(new String(byteBuffer, 0, byteCount));
                } else {
                    // let outside world know that not all the stderr content
                    // was read
                    output.append(" -- WARNING, error message length has");
                    output.append(" exceeded this limit: ").append(OUTPUT_SIZE_LIMIT);
                }
            }
        } catch(IOException ex) {
            LOGGER.warn("IOException while reading stderr, ex: " + ex.getMessage());
            ioExceptionWhileReading = ex;
        } finally {
            synchronized (this) {
                // Set done and notify any waiting threads.
                // Typically, somebody calling getOutputString()
                done = true;
                notify();
            }
            try { inputStream.close(); }
            catch( Exception ex) {;} // noop
        }
    }
}

