package edu.iris.wss.framework;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import edu.iris.StatsWriter.WsStatsWriter;

public class AppContextListener implements ServletContextListener {

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		WsStatsWriter.getInstance().close();
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {

	}

}
