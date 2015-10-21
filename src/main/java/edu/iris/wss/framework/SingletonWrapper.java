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
	public static final Logger logger = Logger.getLogger(SingletonWrapper.class);	

	public AppConfigurator appConfig = new AppConfigurator();
	public ParamConfigurator paramConfig = null;
	public StatsKeeper statsKeeper = new StatsKeeper();
	public IrisSingleton singleton = null;
        
    public static WebLogService webLogService = null;
	
	public SingletonWrapper()  {
        //System.out.println("***** SingletonWrapper no-arg construct");
    }
	
	public void configure(ServletContext context) {		
		// If we've already configured the application, don't do it again.

		if (appConfig.isValid() ) {
			return;
		}

		String configFileBase = null;
		if (context != null) {
			configFileBase = WebUtils.getConfigFileBase(context);
		}

    	try {
    		appConfig.loadConfigFile(configFileBase, context);
        } catch (Exception ex) {
            String msg = "Error in -service.cfg file, message: "
                    + ex.getMessage();
            System.out.println(msg);
            logger.error(msg);

            // even with error, try to load params so paramConfig is
            // not null
            paramConfig = getParamConfig(appConfig, configFileBase);

    		return;
    	}

        paramConfig = getParamConfig(appConfig, configFileBase);

        // the singleton is validated and created by appConfig
        singleton = appConfig.getIrisSingleton();
	}

    private ParamConfigurator getParamConfig(AppConfigurator appCfg,
          String cfgFileBase) {
        paramConfig = new ParamConfigurator(appCfg.getEndpoints());
        
    	try {
            paramConfig.loadConfigFile(cfgFileBase);
        } catch (Exception ex) {
            logger.fatal("Invalid parameter config file: " + ex.getMessage());
    	}

        return paramConfig;
    }
}
