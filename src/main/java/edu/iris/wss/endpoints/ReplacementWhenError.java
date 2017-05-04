/*******************************************************************************
 * Copyright (c) 2017 IRIS DMC supported by the National Science Foundation.
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
import java.util.Map;
import java.util.TreeMap;

/**
 * This endpoint class will be used as a substitute for a configured endpoint
 * when respective configurations parameters have exceptions that can't be
 * handled with a simple default or replacement
 *
 * @author mike
 */
public class ReplacementWhenError extends IrisProcessor {

    public static final Map<String, String> errorMsgMap = new TreeMap();

    @Override
    public IrisProcessingResult getProcessingResults(RequestInfo ri,
          String wssMediaType) {

        String briefMsg = "There is a configuration error or unavailable"
              + " endpointClassName object.";

        StringBuilder detailedMsg = new StringBuilder();
        detailedMsg.append("Check the configuration files and");
        detailedMsg.append(" also check that respective class file(s) is/are deployed.");

        for (Map.Entry<String, String> entry : errorMsgMap.entrySet()) {
            detailedMsg.append("\n").append(" - paramerter: ").append(entry.getKey())
                  .append("  msg: ").append(entry.getValue());
        }

        Util.logAndThrowException(ri, FdsnStatus.Status.NOT_IMPLEMENTED, briefMsg,
              detailedMsg.toString());

        // should never get here!
        IrisProcessingResult ipr = IrisProcessingResult.processError(
              FdsnStatus.Status.INTERNAL_SERVER_ERROR,
              "Coding error, this code should never be reached",
              "An exception should have been logged and a log message sent");

        return ipr;
    }
}
