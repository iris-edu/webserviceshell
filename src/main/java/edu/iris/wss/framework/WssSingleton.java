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

package edu.iris.wss.framework;

// Reference to support JMS logging option.
import edu.iris.dmc.jms.service.WebLogService;

import org.apache.log4j.Logger;

import edu.iris.wss.provider.IrisSingleton;
import java.io.UnsupportedEncodingException;

public class WssSingleton {
	public static final Logger logger = Logger.getLogger(WssSingleton.class);	

	public AppConfigurator appConfig = null;
	public ParamConfigurator paramConfig = null;
	public StatsKeeper statsKeeper = new StatsKeeper();
	public IrisSingleton singleton = null;
        
    public static WebLogService webLogService = null;

    public static final String HEADER_START_IDENTIFIER = "HTTP_HEADERS_START";
    public byte[] HEADER_START_IDENTIFIER_BYTES;
    public static final String HEADER_END_IDENTIFIER = "HTTP_HEADERS_END";
    public byte[] HEADER_END_IDENTIFIER_BYTES;
    public static final int HEADER_MAX_ACCEPTED_BYTE_COUNT = 1024  * 16;
    
    // These are headers used internally which may be set by configuration,
    // they may be overridden by incoming settings
    public final static String CONTENT_DISPOSITION = "Content-Disposition";
    public final static String ACCESS_CONTROL_ALLOW_ORIGIN =
          "Access-Control-Allow-Origin";

	public WssSingleton() throws UnsupportedEncodingException  {
        //System.out.println("***** SingletonWrapper no-arg construct");

        // only want to create this once, but use it on every request
        // expecting an encoding with one byte per character for now,
        // so let it throw and exception if some representive byte encoding
        // (i.e. like UTF-8) is not accepted.
        HEADER_START_IDENTIFIER_BYTES = HEADER_START_IDENTIFIER.getBytes("UTF-8");
        HEADER_END_IDENTIFIER_BYTES = HEADER_END_IDENTIFIER.getBytes("UTF-8");
    }

	public void configure(String configFileBase) throws Exception {
        if (appConfig == null) {
            appConfig = new AppConfigurator();
        }

		// If we've already configured the application, don't do it again.
		if (appConfig.isValid() ) {
			return;
		}

    	try {
            appConfig.loadConfigFile(configFileBase);
        } catch (Exception ex) {
            String msg = "----------- Error loading "
                  + AppConfigurator.getConfigFileNamed() + " file, message: "
                  + ex.getMessage();
            System.out.println(msg);
            logger.error(msg);

            // even with error, try to load params so paramConfig is
            // not null
            paramConfig = getParamConfig(appConfig, configFileBase);

            throw new Exception(msg, ex);
    	}

        paramConfig = getParamConfig(appConfig, configFileBase);

        // the singleton name is read in by appConfig, but validated here.
        // just skip it if the name is null
        if (appConfig.getSingletonClassName() == null) {
            logger.warn("The value for "
                  + AppConfigurator.GL_CFGS.singletonClassName.toString()
                  + " is null. Therefore, the valuse for "
                  + this.getClass().getName() + ".singleton will be null also.");
        } else {
            singleton = appConfig.getIrisSingletonInstance(
                  appConfig.getSingletonClassName());
        }
	}

    private ParamConfigurator getParamConfig(AppConfigurator appCfg,
          String cfgFileBase) throws Exception {
        paramConfig = new ParamConfigurator(appCfg.getEndpoints());
        
    	try {
            paramConfig.loadConfigFile(cfgFileBase);
        } catch (Exception ex) {
            String msg = "----------- Error loading param.cfg file, message: "
                    + ex.getMessage();
            System.out.println(msg);
            logger.error(msg);
            throw new Exception(msg, ex);
    	}

        return paramConfig;
    }
}
