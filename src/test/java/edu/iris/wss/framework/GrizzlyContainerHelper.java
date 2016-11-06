/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.iris.wss.framework;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.servlet.GrizzlyWebContainerFactory;
import org.glassfish.jersey.servlet.ServletProperties;

/**
 *
 * @author mike
 */
public class GrizzlyContainerHelper {
    public static final Logger LOGGER = Logger.getLogger(GrizzlyContainerHelper.class);

    private static HttpServer server;

    /**
     * Note: When setup is called, almost certainly system property
     *       Util.WSS_OS_CONFIG_DIR needs to have been set
     *
     * @param base_uri -
     * @param callerClassName - used to help understand logs
     * @throws Exception
     */
    public static void setUpServer(URI base_uri, String callerClassName,
          String contextPath) throws Exception {
        Map<String, String> initParams = new HashMap<>();
        initParams.put(
            ServletProperties.JAXRS_APPLICATION_CLASS,
            MyApplication.class.getName());

        String msg = "********** starting GrizzlyWebContainerFactory";
        System.out.println(msg + ", BASE_URI: " + base_uri);
        LOGGER.info(msg + ", BASE_URI: " + base_uri);
        LOGGER.info(msg + ", parameters: " + initParams);

        // do to restructure of WSS startup to enable glassfish container
        // environment, there now needs to be an instance of AppContextListener
        // before MyApplication can complete
        ServletContext sc = new ServletContextHelper(contextPath);
        AppContextListener apc = new AppContextListener();
        apc.contextInitialized(new ServletContextEvent(sc));

        server = GrizzlyWebContainerFactory.create(base_uri, initParams);
        server.start();

        msg = "********** started GrizzlyWebServer";
        System.out.println(msg + ", from class: " + callerClassName);
//        System.out.println(msg + ", config: " + server.getServerConfiguration());

        // for manual test of server, uncomment this code then mvn clean install
////        System.out.println("***** Application started, try: " + base_uri);
////        System.out.println("***** control-c to stop its...");
////        Thread.sleep(100000000);
    }

    public static void tearDownServer(String callerClassName) throws Exception {
        String msg = "********** stopping grizzlyWebServer, class: "
            + callerClassName;
        System.out.println(msg);
        LOGGER.info(msg);
        server.shutdownNow();
        server = null;
     }
}
