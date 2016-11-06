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

        // this is to provide a reference for testign the the global passing
        // of configBase, these two servlet context should be the same?
        ServletContext scForSwCreation =
              AppContextListener.configBaseToServletContext.get(sw.getConfigFileBase());
        System.out.println("***************** MyContainerLifecycleListener"
              + " onStartup, servlet context first: " + scForSwCreation);
        System.out.println("***************** MyContainerLifecycleListener"
              + " onStartup, servlet context   now: " + context);

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