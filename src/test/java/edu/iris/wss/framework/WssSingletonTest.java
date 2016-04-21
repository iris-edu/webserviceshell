/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.iris.wss.framework;

import edu.iris.dmc.jms.WebUsageItem;
import edu.iris.wss.utils.LoggerUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import org.apache.log4j.Level;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author mike
 */
public class WssSingletonTest {
    private static final String SOME_CONTEXT = "/tstWssSinglton";
    private static String rabbitPropName = "setit";

    @BeforeClass
    public static void setUpClass() throws Exception {

        // setup WSS service config dir for test environment
        System.setProperty(Util.WSS_OS_CONFIG_DIR,
            "target"
              + File.separator + "test-classes"
              + File.separator + "WssSingletonTest");

        createTestCfgFile(System.getProperty(Util.WSS_OS_CONFIG_DIR),
              SOME_CONTEXT + "-service.cfg");

        // setup IRIS Rabbit  config dir for test environment

        String filename = SOME_CONTEXT + "-rabbitconfig-publisher.properties";
        rabbitPropName = getFileNameAndDoPrep(System.getProperty(Util.WSS_OS_CONFIG_DIR),
              filename);
        createIRISRabbitCfgFile(System.getProperty(Util.WSS_OS_CONFIG_DIR),
              filename);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test of configure method, of class WssSingleton.
     */
////    @Test
////    public void testHolder() throws Exception {
////        // place holder until test is activated or removed, depends on
////        // having a rabbitmq instance available for unit test and Jenkins
////    }
    @Test
    public void testConfigure() throws Exception {
        String configFileBase = SOME_CONTEXT;
        System.setProperty(Util.CONFIG_FILE_SYSTEM_PROPERTY_NAME, rabbitPropName);
        WssSingleton instance = new WssSingleton();
        instance.configure(configFileBase);

        RequestInfo ri = new RequestInfo(instance.appConfig);

        WebUsageItem webUI = new WebUsageItem();

        webUI.setApplication("app-WssSingletonTest-webUI");
        webUI.setHost(       "mywebUIHost");
        webUI.setAccessDate( new Date());
        webUI.setClientIP(   "noipforwebUI");

        webUI.setClientName( "webUI test1");
        webUI.setStartTime(new Date());
        webUI.setEndTime(new Date());

        // not checking for errors for now
        // should pass even if it does not work
        // can look for message to real broker with
        //usagelog.rabbitmq_noconfig  broker=broker1 virtualhost=test exchange=ws_logging routing=# user=zzz password=zzz
        System.out.println("-------------- sleeping for 10 for RabbitMQ test start");
        Thread.sleep(10000);
        LoggerUtils.logWssUsageMessage(Level.INFO, webUI, ri);
        System.out.println("-------------- sleeping for 10 for RabbitMQ test - doing destroy");
        Thread.sleep(10000);

        AppContextListener ctxLisenter = new AppContextListener();
        ctxLisenter.contextDestroyed(null);
    }

    private static String getFileNameAndDoPrep(String filePath, String fileName)
          throws FileNotFoundException, IOException {

        String targetName = filePath + File.separator + fileName;

        File testFile = new File(targetName);
        if (testFile.exists()) {
            testFile.delete();
        }

        File dirs = new File(filePath);
        if(!dirs.exists()){
            dirs.mkdirs();
        }

        return targetName;
    }

    // create a config file to test against on a target test path
    private static void createTestCfgFile(String filePath, String fileName)
          throws FileNotFoundException, IOException {

        String targetName = getFileNameAndDoPrep(filePath, fileName);

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
        sb.append("# ----------------  endpoints").append("\n");
        sb.append("\n");
        sb.append("wsssingletondummyEP.endpointClassName=edu.iris.wss.endpoints.CmdProcessor").append("\n");

        os.write(sb.toString().getBytes());
    }

    // create a config file to test against on a target test path
    private static void createIRISRabbitCfgFile(String filePath, String fileName)
          throws FileNotFoundException, IOException {

        String targetName = getFileNameAndDoPrep(filePath, fileName);

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
}
