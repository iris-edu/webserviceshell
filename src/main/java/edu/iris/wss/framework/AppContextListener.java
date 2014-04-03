package edu.iris.wss.framework;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import edu.iris.StatsWriter.WsStatsWriter;
import org.apache.log4j.Logger;

public class AppContextListener implements ServletContextListener {
    public static final Logger logger = Logger.getLogger(AppContextListener.class);

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        try {
            WsStatsWriter.getInstance().close();
        } catch (java.lang.NoClassDefFoundError ex) {
            System.out.println("*** AppContextListener, WsStatsWriter not loaded");
            logger.info("WsStatsWriter is not loaded when contextDestroyed called,"
                + " exception: java.lang.NoClassDefFoundError");
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        logger.info("Web application initialization process is starting");
    }
}
