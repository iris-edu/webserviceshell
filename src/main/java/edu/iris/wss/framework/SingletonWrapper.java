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

import edu.iris.dmc.jms.service.WebLogService;
import javax.servlet.ServletContext;

import org.apache.log4j.Logger;

import edu.iris.wss.IrisStreamingOutput.IrisSingleton;
import edu.iris.wss.utils.WebUtils;

public class SingletonWrapper {
//	public ServletContext context;

	public AppConfigurator appConfig = new AppConfigurator();
	public ParamConfigurator paramConfig = new ParamConfigurator();
	public StatsKeeper statsKeeper = new StatsKeeper();
	public IrisSingleton singleton = null;
        
    public static WebLogService webLogService = null;

	
	public static final Logger logger = Logger.getLogger(SingletonWrapper.class);	

	// Will be loaded in application scope via the AppScope class.  
	// This is essentially, a singleton.  But nothing happens until the configure method is 
	// called from the AppScope class which can only occur once the AppScope class has
	// the servlet context.
	
	public SingletonWrapper()  {
    System.out.println("***** SingletonWrapper construct this: " + this);
  }
	
	public void configure(ServletContext context) {		
		// If we've already configured the application, don't do it again.
    System.out.println("************************ sw config with context, svrinf: " + context.getServerInfo());
		if (appConfig.isValid() ) {
			return;
		}
		
		String configFileBase = null;
		if (context != null) {
			configFileBase = WebUtils.getConfigFileBase(context);
		}
		
    	try {
    		appConfig.loadConfigFile(configFileBase, context);
        System.out.println("************************ sw config with context repeated?, svrinf: " + context.getServerInfo()
        + "  configFileBase: " + configFileBase);
    	} catch (Exception e) {
    		logger.fatal("Invalid application config file, message: "
                    + e.getMessage());
    		return;
    	}
    	
    	try {
    		paramConfig.loadConfigFile(configFileBase);
    	} catch (Exception e) {
    		logger.fatal("Invalid parameter config file: " + e.getMessage());
    	}
    	
    	if (appConfig.getSingletonClassName() != null) {
    		try {
        		Class<?> singletonClass;
    			singletonClass = Class.forName(appConfig.getSingletonClassName());
    			singleton = (IrisSingleton) singletonClass.newInstance();
                logger.info("singleton: "+ singleton);
    		} catch (ClassNotFoundException e) {
    			String err = "Could not find class with name: " + appConfig.getSingletonClassName();
    			logger.fatal(err);
    			throw new RuntimeException(err);
    		} catch (InstantiationException e) {
    			logger.fatal("Could not instantiate class: " + appConfig.getSingletonClassName());
    		} catch (IllegalAccessException e) {
    			logger.fatal("Illegal access while instantiating class: " + appConfig.getSingletonClassName());
    		}
    	}

	}
	
	public SingletonWrapper(AppConfigurator ac, ParamConfigurator pc) {
    System.out.println("***** SingletonWrapper construct 2");
		this.appConfig = ac;
		this.paramConfig = pc;
	}
}
