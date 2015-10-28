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

package edu.iris.wss.provider;

import edu.iris.wss.framework.FdsnStatus.Status;
import edu.iris.wss.framework.RequestInfo;
import java.io.OutputStream;
import javax.ws.rs.core.StreamingOutput;
import org.apache.log4j.Logger;

public abstract class IrisStreamingOutput implements StreamingOutput,
      IrisProcessMarker {
	public static final Logger logger = Logger.getLogger(IrisStreamingOutput.class);

	protected RequestInfo ri;

	public IrisStreamingOutput() { }

    /**
     * Required by web framework to stream data out.
     * 
     * @param os 
     */
    @Override
	public abstract void write(OutputStream os);

    /**
     * Do main processing here before the framework does write.
     * @return 
     */
	public abstract Status getResponse();

    /**
     * Called by exception handlers, the returned string should be
     * respective messages for the end user but include enough details
     * to isolate the source of the error.
     * 
     * @return 
     */
	public abstract String getErrorString();
	
    /**
     * Somewhat equivalent to an initialization, will be called first by
     * topExec so as to make contextual information available to implementer
     * 
     * @param ri 
     */
	public abstract void setRequestInfo(RequestInfo ri);
}
