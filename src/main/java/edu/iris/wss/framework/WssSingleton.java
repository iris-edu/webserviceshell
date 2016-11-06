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
import edu.iris.dmc.logging.rabbitmq.IrisRabbitAsyncPublisher;
import edu.iris.dmc.logging.rabbitmq.IrisRabbitPublisherFactory;

import org.apache.log4j.Logger;

import edu.iris.wss.provider.IrisSingleton;
import java.io.UnsupportedEncodingException;

/**
 *  A top level class used by Web Service Shell to create and hold objects
 *  needed for the entire application. Objects in this class are available
 *  to other objects in the Jersey framework via the @Context annotation.
 *  WssSingleton objects are made available to the framework with the
 *  Injections.addBinding code in the MyApplication object.
 *
 *  WssSingleton is used to:
 *  - contain StatsKeeper
 *  - contain JMS logging object if needed
 *  - load service.cfg and param.cfg information
 *  - instantiate an application IrisSingleton if specified in service.cfg
 *
 */

public class WssSingleton {
	public static final Logger logger = Logger.getLogger(WssSingleton.class);

	public AppConfigurator appConfig = null;
	public ParamConfigurator paramConfig = null;
	public StatsKeeper statsKeeper = new StatsKeeper();
	public IrisSingleton singleton = null;

    public static WebLogService webLogService = null;
    public static IrisRabbitAsyncPublisher rabbitAsyncPublisher = null;

    public static final String HEADER_START_IDENTIFIER = "HTTP_HEADERS_START";
    public static byte[] HEADER_START_IDENTIFIER_BYTES;
    public static final String HEADER_END_IDENTIFIER = "HTTP_HEADERS_END";
    public static byte[] HEADER_END_IDENTIFIER_BYTES;
    public static final int HEADER_MAX_ACCEPTED_BYTE_COUNT = 1024  * 16;

    // These are headers used internally which may be set by configuration,
    // they may be overridden by incoming settings
    public final static String CONTENT_DISPOSITION = "Content-Disposition";
    public final static String ACCESS_CONTROL_ALLOW_ORIGIN =
          "Access-Control-Allow-Origin";

    private String configFileBase = "notDefinedYet";

	public WssSingleton(){
        System.out.println("***** ***** ***** ***** ***** "
              + this.getClass().getSimpleName()+ " no-arg construct");
        // Create these for CmdProcessor, they are used =on every request
        // Note: These only work for an encoding with one byte per character,
        // so let it throw and exception if some representive byte encoding
        // i.e. like UTF-8 is not handled.
        try {
            HEADER_START_IDENTIFIER_BYTES = HEADER_START_IDENTIFIER.getBytes("UTF-8");
            HEADER_END_IDENTIFIER_BYTES = HEADER_END_IDENTIFIER.getBytes("UTF-8");
        } catch(UnsupportedEncodingException ex) {
            //noop
        }
    }

    public String getConfigFileBase() {
        return configFileBase;
    }

	public void configure(String configFileBase) throws Exception {
        if (appConfig == null) {
            this.configFileBase = configFileBase;
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

        // the singleton name is read in by appConfig, but instantiated here.
        // Null is ok, it indicates the application does not need a singleton
        if (appConfig.getSingletonClassName() == null) {
            logger.warn("The value for "
                  + AppConfigurator.GL_CFGS.singletonClassName.toString()
                  + " is null. Therefore, the valuse for "
                  + this.getClass().getName() + ".singleton will be null also.");
        } else {
            singleton = appConfig.getIrisSingletonInstance(
                  appConfig.getSingletonClassName());
        }

        if (appConfig.getLoggingType().equals(
              AppConfigurator.LoggingMethod.RABBIT_ASYNC)) {
            String fileName = appConfig.getLoggingConfig().toString();
            setupRabbitLogging(fileName);
        } else if (appConfig.getLoggingType().equals(
              AppConfigurator.LoggingMethod.JMS)) {
            setupJMSLogging();
        }
	}

    private ParamConfigurator getParamConfig(AppConfigurator appCfg,
          String cfgFileBase) throws Exception {
        paramConfig = new ParamConfigurator(appCfg.getEndpoints());

    	try {
            paramConfig.loadConfigFile(cfgFileBase);
        } catch (Exception ex) {
            String msg = "----------- Error loading param.cfg file, ex: " ;
            System.out.println(msg + ex);
            throw new Exception(msg, ex);
    	}

        return paramConfig;
    }

    private void setupRabbitLogging(String rabbitCfgFile) {
        boolean isCreated = false;
        try {
            rabbitAsyncPublisher =
                  IrisRabbitPublisherFactory.createAsyncPublisher(rabbitCfgFile,
                        appConfig.getAppName());

            isCreated = true;
        } catch (Exception ex) {
            String msg = "Error creating rabbitAsyncPublisher with file API," +
                  " trying URL form, ex: ";
            System.out.println(msg + ex);
            logger.warn(msg, ex);
            try {
                int secondsToTry = 5;
                rabbitAsyncPublisher =
                      IrisRabbitPublisherFactory.createAsyncPublisherFromUrl(
                            rabbitCfgFile, appConfig.getAppName(), secondsToTry);
                isCreated = true;
            } catch (Exception exUURL) {
                msg = "Error creating rabbitAsyncPublisher with URL API  ex: ";
                System.out.println(msg + exUURL);
                logger.error(msg, exUURL);
            }
        }

        if (isCreated) {
            try {
                rabbitAsyncPublisher.activate();
                logger.info("Rabbit Async activate finished");

            } catch (Exception ex) {
                String msg = "Error activating rabbitAsyncPublisher ex: ";
                System.out.println(msg + ex);
                logger.error(msg, ex);
            }
        }
    }

    private void setupJMSLogging() {
        webLogService = new WebLogService();
        try {
            webLogService.init();
            logger.info("JMS webLogService init finished");
        } catch (Exception ex) {
            String msg = "JMS webLogService init ex: ";
            System.out.println(msg + ex);
            logger.error(msg, ex);
        }
    }
}
