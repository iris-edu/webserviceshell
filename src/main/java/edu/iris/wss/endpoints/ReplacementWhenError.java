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

import edu.iris.wss.framework.FdsnStatus;
import edu.iris.wss.framework.RequestInfo;
import edu.iris.wss.framework.Util;
import edu.iris.wss.provider.IrisProcessingResult;
import edu.iris.wss.provider.IrisProcessor;

/**
 * This endpoint class will be used as a substitute for a configured endpoint
 * when respective configurations parameters have exceptions that can't be
 * handled with a simple default or replacement
 *
 * @author mike
 */
public class ReplacementWhenError extends IrisProcessor {

    @Override
    public IrisProcessingResult getProcessingResults(RequestInfo ri,
          String wssMediaType) {

        String briefMsg = "There is a configuration error or unavailable"
              + " endpointClassName object.";
        String detailedMsg = "Check the configuration for this endpoint and"
              + " also check that the respective class file is deployed";
        Util.logAndThrowException(ri, FdsnStatus.Status.NOT_IMPLEMENTED, briefMsg,
              detailedMsg);

        // should never get here!
        IrisProcessingResult ipr = IrisProcessingResult.processError(
              FdsnStatus.Status.INTERNAL_SERVER_ERROR,
              "Coding error, this code should never be reached",
              "An exception should have been logged and a log message sent");

        return ipr;
    }
}
