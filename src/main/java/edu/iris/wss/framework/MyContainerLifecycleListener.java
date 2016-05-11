/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.iris.wss.framework;

import javax.ws.rs.core.Context;
import org.apache.log4j.Logger;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

/**
 *
 * @author mike
 */
public class MyContainerLifecycleListener implements ContainerLifecycleListener {
    public static final Logger LOGGER =
          Logger.getLogger(MyContainerLifecycleListener.class);

    @Context 	WssSingleton sw;

    @Override
    public void onStartup(Container cntnr) {
        LOGGER.info("my container started for app: " + sw.appConfig.getAppName());
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