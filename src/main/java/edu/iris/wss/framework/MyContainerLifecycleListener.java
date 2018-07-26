/*******************************************************************************
 * Copyright (c) 2018 IRIS DMC supported by the National Science Foundation.
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
import org.apache.log4j.Logger;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

/**
 *
 * @author mike
 */
public class MyContainerLifecycleListener implements ContainerLifecycleListener {
    public static final Logger LOGGER =
          Logger.getLogger(MyContainerLifecycleListener.class);

    @Context ServletContext context;

    protected WssSingleton sw;

    @Override
    public void onStartup(Container cntnr) {
        sw = (WssSingleton)cntnr.getConfiguration().getProperty(
              MyApplication.WSS_SINGLETON_KEYWORD);

        // check for container startup timing errors that should never
        // occur in regular use, but might appear in unit testing, or when
        // container/injection frameworks are changed
        //
        // compare the configBase used for the configuration of this sw
        // versus the configBase associated with this ServletContext and
        // serviceLocator which will bind sw to all the resources

        String configuredSw_configBase = sw.getConfigFileBase();
        String this_configBase = Util.getWssFileNameBase(
              context.getContextPath());
        if (configuredSw_configBase != null && configuredSw_configBase.equals(this_configBase)) {
            // noop - no problem
        } else {
            String msg = "POSSIBLE ERROR, MyContainerLifecycleListener: possible mis-match of configured WssSingleton object with respective resources,"
                  + "  configured configBase: " + configuredSw_configBase
                  + "  resource configBase: " + this_configBase;
            System.out.println(msg);
            LOGGER.error(msg);
        }

//        // expecting the servletContext objects to be the same from
//        // different times in the startup process, except sometimes in
//        // the unit test framework
//        ServletContext scForSwCreation =
//              AppContextListener.configBaseToServletContext.get(configuredSw_configBase);
//        System.out.println("***************** MyContainerLifecycleListener"
//              + " onStartup, servlet context first: " + scForSwCreation);
//        System.out.println("***************** MyContainerLifecycleListener"
//              + " onStartup, servlet context   now: " + context);

        // bind objects as needed to make them available to the framework
        // via a CONTEXT annotation
        ServiceLocator serviceLocator = cntnr.getApplicationHandler().getServiceLocator();
        DynamicConfiguration dc = Injections.getConfiguration(serviceLocator);
        Injections.addBinding(
            Injections.newBinder(sw).to(WssSingleton.class), dc);
        dc.commit();

        context.setAttribute("wssSinlgton", sw);

        LOGGER.info("onStartup, context path: " + context.getContextPath()
              + "  configBase: " + sw.getConfigFileBase());
    }

    @Override
    public void onReload(Container cntnr) {
        LOGGER.info("my container reloaded for app: " + sw.appConfig.getAppName());
    }

    @Override
    public void onShutdown(Container cntnr) {
        LOGGER.info("my container shutdown for app: " + sw.appConfig.getAppName());
        if (WssSingleton.rabbitAsyncPublisher != null) {
            try {
                // RabbitMQ shutdown just before container goes away
                LOGGER.info("RABBIT_ASYNC shutdown(10000) started");
                Thread.sleep(250); // help prevent loss of message
                WssSingleton.rabbitAsyncPublisher.shutdown(10000);
                LOGGER.info("RABBIT_ASYNC shutdown(10000) returned");
            } catch (Exception ex) {
                String msg = "*** MyContainerLifecycleListener, rabbitAsyncPublisher"
                        + " shutdown exception: " + ex
                        + "  msg: " + ex.getMessage();

                System.out.println(msg);
                LOGGER.info(msg);
            }
        }

        sw.destroyIrisSingleton();
    }
}