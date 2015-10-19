/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.iris.wss.framework;

import edu.iris.wss.IrisStreamingOutput.IrisStreamingOutput;
import javax.ws.rs.core.Response;

/**
 *
 * @author mike
 */
public class Util {
    
    public static void addCORSHeadersIfConfigured(Response.ResponseBuilder rb, RequestInfo ri) {
		if (ri.appConfig.isCorsEnabled()) {
            // Insert CORS header elements.
		    rb.header("Access-Control-Allow-Origin", "*");

            // dont add this unless cookies are expected
//            rb.header("Access-Control-Allow-Credentials", "true");

            // Not setting these at this time - 2015-08-12
//            rb.header("Access-Control-Allow-Methods", "HEAD, GET, POST");
//            rb.header("Access-Control-Allow-Headers", "Content-Type, Accept");

            // not clear if needed now, 2015-08-12, but this is how to let client
            // see what headers are available, although "...Allow-Headers" may be
            // sufficient
//            rb.header("Access-Control-Expose-Headers", "X-mycustomheader1, X-mycustomheader2");
		}
    }
	
	public static void shellException(FdsnStatus.Status status, String message, RequestInfo ri) {
		ServiceShellException.logAndThrowException(ri, status, message);       
	}
	
	public static void newerShellException(FdsnStatus.Status status, RequestInfo ri, 
            IrisStreamingOutput iso) {
		ServiceShellException.logAndThrowException(ri, status,
                status.toString() + iso.getErrorString());
	}

    public static FdsnStatus.Status adjustByCfg(FdsnStatus.Status trialStatus, RequestInfo ri) {
        if (trialStatus == FdsnStatus.Status.NO_CONTENT) {
            // override 204 if configured to do so
            if (ri.perRequestUse404for204) {
                return FdsnStatus.Status.NOT_FOUND;
            }
        }
        return trialStatus;
    }    
}
