/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.iris.wss;

import com.sun.grizzly.http.SelectorThread;
import com.sun.jersey.api.container.grizzly.GrizzlyWebContainerFactory;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author mike
 */
public class WssTest {
    public static final Logger logger = Logger.getLogger(WssTest.class);
    private static final URI BASE_URI = URI.create("http://localhost:8093/");
    private static SelectorThread threadSelector;
    
    public WssTest() {
    }
    
    @BeforeClass
    public static void setUpClass() throws IOException {
        Map<String, String> initParams = new HashMap<>();
        initParams.put(
            "com.sun.jersey.config.property.packages",
            Wss.class.getPackage().getName());

        logger.info("*** starting grizzley container with parameters: " + initParams);
        threadSelector = GrizzlyWebContainerFactory.create(BASE_URI, initParams);

        // uncomment this code for manual test of server, e.g. mvn clean install
//        System.out.println("***** Application started, try: " + BASE_URI);
//        System.out.println("***** control-c to stop its...");
//        System.in.read();
    }
    
    @AfterClass
    public static void tearDownClass() {
        logger.info("*** stopping grizzley container");
        threadSelector.stopEndpoint();
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getWssVersion method, of class Wss.
     */
    @Test
    public void testGetWssVersion() throws Exception {
        System.out.println("getWssVersion");
        Wss instance = new Wss();
        String expResult = "";
        String result = instance.getWssVersion();
        System.out.println("*** getWssVersion result: " + result);
   //     //assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
   //     fail("The test case is a prototype.");
    }

}
