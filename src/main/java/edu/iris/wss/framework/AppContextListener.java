package edu.iris.wss.framework;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;

public class AppContextListener implements ServletContextListener {
    public static final Logger logger = Logger.getLogger(AppContextListener.class);

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        System.out.println("**************************** AppContextListener contextDestroyedcalled, arg0: " + arg0);
        if (SingletonWrapper.webLogService != null) {
            try {
                // for JMS
                SingletonWrapper.webLogService.cleanUp();
            } catch (Exception ex) {
                System.out.println("*** AppContextListener, WsStatsWriter"
                        + " cleanup exception: " + ex
                        + "  msg: " + ex.getMessage());
                logger.info("*** AppContextListener, webLogService cleanup"
                        + " exception: ", ex);
            }
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        System.out.println("**************************** AppContextListener contextInitialized called, arg0: " + arg0);
        logger.info("Web application AppContextListener called");
    }
}
