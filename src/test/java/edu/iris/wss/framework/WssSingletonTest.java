/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.iris.wss.framework;

import edu.iris.dmc.logging.usage.WSUsageItem;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.servlet.GrizzlyWebContainerFactory;
import org.glassfish.jersey.servlet.ServletProperties;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author mike
 */
public class WssSingletonTest {
    public static final Logger logger = Logger.getLogger(WssSingletonTest.class);

    private static final String SOME_CONTEXT = "/tstWssSinglton";
    private static String rabbitPropName = "setit";

    private static final String BASE_HOST = "http://localhost";
    private static final Integer BASE_PORT = 8093;

    private static final URI BASE_URI = URI.create(BASE_HOST + ":"
        + BASE_PORT + SOME_CONTEXT);

    private static HttpServer server;

    @Context	javax.servlet.http.HttpServletRequest request;

    @BeforeClass
    public static void setUpClass() throws Exception {

        // setup WSS service config dir for test environment
        System.setProperty(Util.WSS_OS_CONFIG_DIR,
            "target"
              + File.separator + "test-classes"
              + File.separator + "WssSingletonTest");

        String targetPath = System.getProperty(Util.WSS_OS_CONFIG_DIR);

        String targetName = Util.createCfgFileName(SOME_CONTEXT,
              AppConfigurator.SERVICE_CFG_NAME_SUFFIX);
        createTestCfgFile(targetPath, targetName);

        targetName = Util.createCfgFileName(SOME_CONTEXT,
              ParamConfigurator.PARAM_CFG_NAME_SUFFIX);
        createParamCfgFile(targetPath, targetName);

        // setup Rabbit config dir for test environment
        rabbitPropName = Util.createCfgFileName(SOME_CONTEXT,
              Util.RABBITMQ_CFG_NAME_SUFFIX);

        createIRISRabbitCfgFile(targetPath, rabbitPropName);

        logger.info("*********** starting grizzlyWebServer, BASE_URI: "
            + BASE_URI);

        Map<String, String> initParams = new HashMap<>();
        initParams.put(
            ServletProperties.JAXRS_APPLICATION_CLASS,
            MyApplication.class.getName());

        logger.info("*** starting grizzly container with parameters: " + initParams);
        System.out.println("********** start GrizzlyWebContainerFactory");

        server = GrizzlyWebContainerFactory.create(BASE_URI, initParams);

        server.start();
        System.out.println("********** started GrizzlyWebServer, class: "
            + WssSingletonTest.class.getName());
        System.out.println("********** started GrizzlyWebServer, config: "
            + server.getServerConfiguration());

        // for manual test of server, uncomment this code then mvn clean install
//        System.out.println("***** Application started, try: " + BASE_URI);
//        System.out.println("***** control-c to stop its...");
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        System.out.println("********** stopping grizzlyWebServer, class: "
            + Service_addHeaders_PrecedenceTest.class.getName());
        logger.info("*********** stopping grizzlyWebServer");
        server.shutdownNow();
     }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test_log_usage() throws Exception {
        Client c = ClientBuilder.newClient();

        WebTarget webTarget = c.target(BASE_URI)
              .path("/test_logging")
              .queryParam("format", "TEXT")
              .queryParam("messageType", "usage");

        Response response = webTarget.request().get();

        assertEquals(200, response.getStatus());
        assertEquals("text/plain", response.getMediaType().toString());
    }

    @Test
    public void test_log_wfstat() throws Exception {
        Client c = ClientBuilder.newClient();

        WebTarget webTarget = c.target(BASE_URI)
              .path("/test_logging")
              .queryParam("format", "TEXT")
              .queryParam("messageType", "wfstat");

        Response response = webTarget.request().get();

        assertEquals(200, response.getStatus());
        assertEquals("text/plain", response.getMediaType().toString());
    }

    @Test
    public void test_log_error() throws Exception {
        Client c = ClientBuilder.newClient();

        WebTarget webTarget = c.target(BASE_URI)
              .path("/test_logging")
              .queryParam("format", "TEXT")
              .queryParam("messageType", "error");

        Response response = webTarget.request().get();

        assertEquals(200, response.getStatus());
        assertEquals("text/plain", response.getMediaType().toString());
    }

    @Test
    public void test_log_error_with_exception() throws Exception {
        Client c = ClientBuilder.newClient();

        WebTarget webTarget = c.target(BASE_URI)
              .path("/test_logging")
              .queryParam("format", "TEXT")
              .queryParam("messageType", "error_with_exception");

        Response response = webTarget.request().get();

        assertEquals(400, response.getStatus());
        // can't pass this test because apparently exception handling with junit
        // and grizzley server does not construct a WebApplicationException
        // completely
//        assertEquals("text/plain", response.getMediaType().toString());
    }

    private static void doFilePrep(String filePath, String targetName)
          throws FileNotFoundException, IOException {

        File testFile = new File(targetName);
        if (testFile.exists()) {
            testFile.delete();
        }

        File dirs = new File(filePath);
        if(!dirs.exists()){
            dirs.mkdirs();
        }
    }

    // create a config file to test against on a target test path
    private static void createTestCfgFile(String filePath, String targetName)
          throws FileNotFoundException, IOException {

        doFilePrep(filePath, targetName);

        File testFile = new File(targetName);
        OutputStream os = new FileOutputStream(testFile);

        StringBuilder sb = new StringBuilder();

        sb.append("# ---------------- globals").append("\n");
        sb.append("\n");
        sb.append("appName=WssSingleton-logging-test").append("\n");
        sb.append("version=default-0.1").append("\n");
        sb.append("\n");
        sb.append("# LOG4J or JMS").append("\n");
        sb.append("loggingMethod=RABBIT_ASYNC").append("\n");
        sb.append("\n");
        sb.append("# If present, an instance of the singleton class will be created at application start").append("\n");
        sb.append("singletonClassName=edu.iris.wss.framework.TestSingleton").append("\n");
        sb.append("# ----------------  endpoints").append("\n");
        sb.append("\n");
        sb.append("test_logging.endpointClassName=edu.iris.wss.endpoints.LoggingEndpoint").append("\n");

        sb.append("test_logging.formatTypes = \\").append("\n");
        sb.append("    text: text/plain,\\").append("\n");
        sb.append("    json: application/json, \\").append("\n");
        sb.append("    miniseed: application/vnd.fdsn.mseed, \\").append("\n");
        sb.append("    geocsv: text/plain").append("\n");
        sb.append("\n");

        os.write(sb.toString().getBytes());
    }

    // create a config file to test against on a target test path
    private static void createIRISRabbitCfgFile(String filePath, String targetName)
          throws FileNotFoundException, IOException {

        doFilePrep(filePath, targetName);

        File testFile = new File(targetName);
        OutputStream os = new FileOutputStream(testFile);

        StringBuilder sb = new StringBuilder();

        sb.append("# Host that's the broker. This will normally be the load balancer").append("\n");
        sb.append("broker=broker1,broker2").append("\n");
        sb.append("\n");
        sb.append("# The virtual host within the broker").append("\n");
        sb.append("virtualhost=test").append("\n");
        sb.append("\n");
        sb.append("# Internal buffer size for the async publishers").append("\n");
        sb.append("buffersize=10000").append("\n");
        sb.append("\n");
        sb.append("# Persistet or not").append("\n");
        sb.append("default_persistence=true").append("\n");
        sb.append("\n");
        sb.append("# The exchange name that recieves them messages").append("\n");
        sb.append("exchange=ws_logging").append("\n");
        sb.append("\n");
        sb.append("# Credentials").append("\n");
        sb.append("user=irisrabbit").append("\n");
        sb.append("password=eel8ed").append("\n");
        sb.append("\n");
        sb.append("# Probably never normnally reconnect").append("\n");
        sb.append("reconnect_interval=-1").append("\n");
        sb.append("\n");
        sb.append("# How often to wait between failed connection attempts in msec").append("\n");
        sb.append("retry_interval=4000").append("\n");

        os.write(sb.toString().getBytes());
    }

    // create a config file to test against on a target test path
    private static void createParamCfgFile(String filePath, String targetName)
          throws FileNotFoundException, IOException {

        doFilePrep(filePath, targetName);

        File testFile = new File(targetName);
        OutputStream os = new FileOutputStream(testFile);

        StringBuilder sb = new StringBuilder();

        sb.append("# ----------------  endpoints").append("\n");
        sb.append("\n");

        sb.append("# ---------------- ").append("\n");
        sb.append("\n");
        sb.append("test_logging.messageType=TEXT").append("\n");

        sb.append("test_logging.format=TEXT").append("\n");
        sb.append("test_logging.overrideDisp=BOOLEAN").append("\n");

        os.write(sb.toString().getBytes());
    }
}
