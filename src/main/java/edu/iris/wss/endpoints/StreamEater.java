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

	InputStream inputStream;

	boolean     done    = false;
	static final int stringSizeLimit = 20000;

	String      output = null;
	IOException ioException = null;

	public StreamEater(Process process, InputStream is ) throws Exception {

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
	
	public synchronized String getOutputString() throws IOException {
		// wait until done, then return the string
		if (!done) {
			try {
				wait();
			} catch (InterruptedException e) {
				logger.info("interrupted while doing getOutputString");
			}
		}
			
		// Probably timed out if an IO exception has occurred
		if( ioException != null )  {
			// throw ioException;
			return "ioException occurred, probably because request timed out.";
		}
		return output;
	}

    @Override
	public void run() {
		// accumulate lines in this buffer
		StringBuilder sb = new StringBuilder();		
		
		byte [] buffer = new byte[1024];
		
		try {
			int nRead;
			while ((nRead = inputStream.read(buffer, 0, buffer.length)) != -1) {
				if (sb.length() + nRead <= stringSizeLimit)
					sb.append(new String(buffer, 0, nRead));
			}
		} catch(IOException e ) {
			logger.info("Got IO exception in stream eater");
			ioException = e;
		} finally {
			output = sb.toString();
			synchronized (this) {
				// Set done and notify any waiting threads.
                //Typically, somebody calling getOutputString()
				done = true;
				notify();
			}
			try{ inputStream.close(); } catch( Exception e) {;}
		}		
	}
}

