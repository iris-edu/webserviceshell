/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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

    private WssSingleton sw;

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
System.out.println("************************* mclc sw: " + sw);
System.out.println("************************* mclc sw.getConfigFileBase: " + sw.getConfigFileBase());
System.out.println("************************* mclc util: " + Util.getWssFileNameBase(
              context.getContextPath()));

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
    }
}