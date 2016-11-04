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

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;

/**
 *
 * Activated by being defined as a listener in web.xml
 */
public class AppContextListener implements ServletContextListener {
    public static final Logger logger = Logger.getLogger(AppContextListener.class);

    public static ServletContext servletContext;
    public static WssSingleton sw;

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        System.out.println("**************************** AppContextListener "
              + " contextDestroyedc, arg0: " + arg0);
        logger.info("contextDestroyed called, web app is being shutdown");
        if (WssSingleton.webLogService != null) {
            try {
                // for JMS
                WssSingleton.webLogService.cleanUp();
                logger.info("contextDestroyed called, JMS cleanUp finished");
            } catch (Exception ex) {
                System.out.println("*** AppContextListener, webLogService"
                        + " cleanup exception: " + ex
                        + "  msg: " + ex.getMessage());
                logger.info("*** AppContextListener, webLogService cleanup"
                        + " exception: ", ex);
            }
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        System.out.println("**************************** AppContextListener"
              + " contextInitialized, arg0: " + arg0);
        logger.info("AppContextListener contextInitialized, web app is starting");

        System.out.println("**************************** AppContextListener"
              + " servletContextStatic: " + servletContext);

        servletContext = arg0.getServletContext();
        sw  = new WssSingleton();
    }

}
