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

package edu.iris.wss;


import edu.iris.wss.framework.AppConfigurator;
import edu.iris.wss.framework.FdsnStatus.Status;
import edu.iris.wss.framework.RequestInfo;
import edu.iris.wss.framework.WssSingleton;
import edu.iris.wss.framework.Util;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import org.apache.log4j.Logger;

/**
 *
 * @author mike
 *
 * Keeping this class around to illustrate a simple way to add functionality
 * using the addEndpoint method in MyApplication.java.
 *
 * @Context variables are populated with every request. The remainder of the
 * of the class can be pojo methods that are configured by addEndpoint.
 */
public class Info1 {
	@Context 	ServletContext context;
	@Context	javax.servlet.http.HttpServletRequest request;
    @Context 	UriInfo uriInfo;
    @Context 	HttpHeaders requestHeaders;

    @Context 	WssSingleton sw;

	public static final Logger logger = Logger.getLogger(Info1.class);

	public Info1()  {
        System.out.println("***************&&& Info1 constr");
	}
	public Response getDyWssVersion() throws IOException {
        RequestInfo ri = RequestInfo.createInstance(sw, uriInfo, request, requestHeaders);

        ResponseBuilder builder = Response.status(Status.OK)
              .type(MediaType.TEXT_PLAIN)
              .entity("Dynamiclly added, wssVersion: " + AppConfigurator.wssVersion);

        Map<String, String> headersMap = new HashMap<>();
        Util.updateWithCORSHeadersIfConfigured(ri, headersMap);
        Util.setResponseHeaders(builder, headersMap);

		return builder.build();
	}
}