/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.iris.wss.endpoints;

import edu.iris.wss.framework.FdsnStatus;
import edu.iris.wss.framework.ParameterTranslator;
import edu.iris.wss.framework.RequestInfo;
import edu.iris.wss.framework.Util;
import edu.iris.wss.provider.IrisProcessingResult;
import edu.iris.wss.provider.IrisProcessor;
import java.util.ArrayList;
import java.util.Date;
import java.util.ListIterator;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

/**
 *
 * @author mike
 */
public class LoggingEndpoint extends IrisProcessor {

    private MultivaluedMap<String, String> getParameters(RequestInfo ri) {
        String requestedEpName = ri.getEndpointNameForThisRequest();
        ArrayList<String> cmd = new ArrayList<>();
		try {
			ParameterTranslator.parseQueryParams(cmd, ri, requestedEpName);
		} catch (Exception e) {
			Util.logAndThrowException(ri, FdsnStatus.Status.BAD_REQUEST,
                  "LoggingEndpoint - " + e.getMessage());
		}

        MultivaluedMap<String, String> mvm = new MultivaluedHashMap();
        ListIterator<String> cmdIter = cmd.listIterator();
        while (cmdIter.hasNext()) {
            String key = cmdIter.next();
            String val = cmdIter.next();
            mvm.add(key.replace("--", ""), val);
        }

        return mvm;
    }

    @Override
    public IrisProcessingResult getProcessingResults(RequestInfo ri, String wssMediaType) {
        IrisProcessingResult ipr = IrisProcessingResult.processString(
              "endpoint for testing logging");

        MultivaluedMap<String, String> mvm = getParameters(ri);

        if (mvm.containsKey("messageType")) {
            String value = mvm.get("messageType").get(0);

            if (value.equals("usage")) {
                Util.logUsageMessage(ri, null, 44L, 55L, null,
                      FdsnStatus.Status.OK, null);

            } else if (value.equals("wfstat")) {
                Util.logWfstatMessage(ri, null, 66L, 77L, null,
                      FdsnStatus.Status.OK, "wfstat  extra-two",
                      "ab", "cd", "ef", "gh", "ij", new Date(), new Date(),
                      "123duration");

            } else if (value.equals("error")) {
                Util.logUsageMessage(ri, "_killittype", 88L, 99L,
                      "example usage errortype set for kill after timeout",
                      FdsnStatus.Status.BAD_REQUEST, ri.getEndpointNameForThisRequest());

            } else if (value.equals("error_with_exception")) {
                Util.logAndThrowException(ri, FdsnStatus.Status.BAD_REQUEST,
                      "show bad_request messageType option: " + value,
                      "detailed message for bad_request option: " + value);

            } else {
                ipr = IrisProcessingResult.processError(
                      FdsnStatus.Status.BAD_REQUEST,
                      "unrecognized messageType option: " + value,
                      "detailed message unrecognized option: " + value);
            }
        } else {
            ipr = IrisProcessingResult.processError(
                  FdsnStatus.Status.BAD_REQUEST,
                  "messageType parameter not provided",
                  "detailed about no messageType parameter");
        }
        return ipr;
    }
}
