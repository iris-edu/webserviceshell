/*******************************************************************************
 * Copyright (c) 2013 IRIS DMC supported by the National Science Foundation.
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

package edu.iris.wss.framework;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;


public class StatsKeeper {

	public static final Logger logger = Logger.getLogger(StatsKeeper.class);
	
	public Date startTime = new Date();
	
	public long gets = 0;
	public long posts = 0;
	public long authGets = 0;
	public long authPosts = 0;
	
	public long errors = 0;
	public long shippedBytes = 0;
	
	public void logAuthGet() 		{ authGets++; }
	public void logGet()			{ gets++; }
	public void logPost()			{ posts++; }
	public void logAuthPost() 		{ authPosts++; }

	public void logShippedBytes(long bytes) {shippedBytes += bytes;} 
	public void logError()			{ errors++; }
	
	public String toHtmlString() {
		StringBuilder sb = new StringBuilder();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

		sb.append("<TABLE border=2 style='width: 600px'>");
		sb.append("<col style='width: 30%' />");

		sb.append("<TR><TH colspan=\"2\" >" + "Web Service Shell Usage" + "</TH></TR>");
		
		sb.append("<TR><TD>" + "Service Start" + "</TD><TD>" + sdf.format(startTime) + "</TD></TR>");
		
		sb.append("<TR><TD>" + "Number of Gets" + "</TD><TD>" + gets + "</TD></TR>");
		sb.append("<TR><TD>" + "Number of Auth Gets" + "</TD><TD>" + authGets + "</TD></TR>");
		sb.append("<TR><TD>" + "Number of Posts" + "</TD><TD>" +  posts + "</TD></TR>");
		sb.append("<TR><TD>" + "Number of Auth Posts" + "</TD><TD>" + authPosts + "</TD></TR>");
		
		sb.append("<TR><TD>" + "Shipped Bytes" + "</TD><TD>" + shippedBytes + "</TD></TR>");
		sb.append("<TR><TD>" + "Errors" + "</TD><TD>" + errors + "</TD></TR>");
		
	
		sb.append("</TABLE>");

		return sb.toString();
	}
	
}
