/*******************************************************************************
 * Copyright (c) 2014 IRIS DMC supported by the National Science Foundation.
 *  
 * This file is part of the Web Service Shell (WSS).
 *  
 * The WSS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * The WSS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * A copy of the GNU Lesser General Public License is available at
 * <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package edu.iris.wss;

import com.sun.grizzly.http.SelectorThread;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.container.grizzly.GrizzlyWebContainerFactory;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author mike
 */
public class WssTest  {
    public static final Logger logger = Logger.getLogger(WssTest.class);
    private static final String BASE_PORT = "8093";
    private static final URI BASE_URI = URI.create("http://localhost:"
        + BASE_PORT + "/");
    private static SelectorThread threadSelector;
    
    public WssTest() {
    }
    
    @BeforeClass
    public static void setUpClass() throws IOException {
        Map<String, String> initParams = new HashMap<>();
        initParams.put(
            "com.sun.jersey.config.property.packages",
            Wss.class.getPackage().getName());

        logger.info("*** starting grizzly container with parameters: " + initParams);
        threadSelector = GrizzlyWebContainerFactory.create(BASE_URI, initParams);

        // uncomment this code for manual test of server, e.g. mvn clean install
//        System.out.println("***** Application started, try: " + BASE_URI);
//        System.out.println("***** control-c to stop its...");
//        System.in.read();
    }
    
    @AfterClass
    public static void tearDownClass() {
        logger.info("*** stopping grizzly container");
        threadSelector.stopEndpoint();
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testGet_wssversion() throws Exception {
        Client c = Client.create();
        WebResource webResource = c.resource(BASE_URI);
        String responseMsg = webResource.path("wssversion").get(String.class);

        // start with a basic test, that the URL exists and returns something
        assertNotNull(responseMsg);
    }


    @Test
    public void testGet_status() throws Exception {
        
        Client c = Client.create();
        WebResource webResource = c.resource(BASE_URI);
        String responseMsg = webResource.path("status").get(String.class);

        // test that the URL exists and returns something
        assertNotNull(responseMsg);
        
        // test for some basic known content
        assertTrue(
            responseMsg.indexOf("<TD>URL</TD><TD>/status</TD>") > -1);
        assertTrue(
            responseMsg.indexOf("<TD>Port</TD><TD>" + BASE_PORT + "</TD>") > -1);
    }

}
