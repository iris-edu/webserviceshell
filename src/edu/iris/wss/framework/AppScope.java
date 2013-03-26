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

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.apache.log4j.Logger;

import com.sun.jersey.spi.inject.SingletonTypeInjectableProvider;

// This class configures the Application to know about the app-scoped class,
// SingletonWrapper classes through @Context annotations.

@Provider
public class AppScope extends SingletonTypeInjectableProvider<Context, SingletonWrapper> {
	public static final Logger logger = Logger.getLogger(AppScope.class);	
	
	public  AppScope() {
		super(SingletonWrapper.class, new SingletonWrapper());
	}
	
	// I'd love to have the servlet context injected for use in this class's constructor
	// but that's obv. not viable.  The @Context annotation allows the context to be injected
	// when it's available.  
	//
	// Therefore we call the configure method of the SingletonWrapper object in the method 
	// below when this setter is called.
	@Context
	public void setServletContext(ServletContext context) {
		this.getValue().configure(context);
	}
}
