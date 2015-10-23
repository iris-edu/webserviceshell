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

import edu.iris.wss.framework.AppConfigurator;
import org.glassfish.jersey.grizzly2.servlet.GrizzlyWebContainerFactory;
import edu.iris.wss.framework.MyApplication;
import edu.iris.wss.utils.WebUtils;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.servlet.ServletProperties;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
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
    
    // set notional webapp name
    // grizzley only seems to recognize the first path element
//    private static final String SOME_CONTEXT = "/wsstest/wsstservice/1";
    private static final String SOME_CONTEXT = "/";

    private static final URI BASE_URI = URI.create("http://localhost:"
        + BASE_PORT + SOME_CONTEXT);

    private static HttpServer server;
    
    public WssTest() {
    }
    
    @BeforeClass
    public static void setUpClass() throws IOException {;
        Map<String, String> initParams = new HashMap<>();
        // Note: by default, SingletonWrapper, when created and called
        //       in MyApplication, will get configuration information
        //       from META-INF cfg files.
        initParams.put(
            ServletProperties.JAXRS_APPLICATION_CLASS,
            MyApplication.class.getName());

        logger.info("*** starting grizzly container with parameters: " + initParams);
        System.out.println("********** start GrizzlyWebContainerFactory in class: "
            + WssTest.class.getName());
        
        server = GrizzlyWebContainerFactory.create(BASE_URI, initParams);

        // setup config dir for log4j
        System.setProperty(WebUtils.wssConfigDirSignature,
            "target/test-classes");
        server.start();
        System.out.println("********** started GrizzlyWebContainerFactory in class: "
              + WssTest.class.getName());

        // uncomment this code for manual test of server, e.g. mvn clean install
//        System.out.println("***** Application started, try: " + BASE_URI);
//        System.out.println("***** control-c to stop its...");
//        System.in.read();
    }
    
    @AfterClass
    public static void tearDownClass() {
        logger.info("*** stopping grizzly container");
        System.out.println("***************** stopping grizzly container");
        //threadSelector.stopEndpoint();
        server.shutdownNow();
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testGet_wssversion() throws Exception {
        Client c = ClientBuilder.newClient();
        WebTarget webTarget = c.target(BASE_URI);
        Response response = webTarget.path("wssversion").request().get();

        assertNotNull(response);        
        String testMsg = response.readEntity(String.class);
        assertEquals(200, response.getStatus());
        assertTrue(testMsg.equals(AppConfigurator.wssVersion));
    }

    @Test
    public void testGet_status() throws Exception {
        
        Client c = ClientBuilder.newClient();
        WebTarget webTarget = c.target(BASE_URI);
        Response response = webTarget.path("wssstatus").request().get();
        String responseMsg = response.readEntity(String.class);
        
        assertEquals(200, response.getStatus());
        // test some content, this can break if the format of the status html changes
        assertTrue(
            responseMsg.toString().indexOf("<TD>URL</TD><TD>/wssstatus</TD>") > -1);
        assertTrue(
            responseMsg.toString().indexOf("<TD>Port</TD><TD>" + BASE_PORT
             + "</TD>") > -1);
    }

    @Test
    public void testdummy() throws Exception {
        
    }
    // run test again to see if Singleton Configure does not run again
    @Test
    public void testGet_status2() throws Exception {
        
        Client c = ClientBuilder.newClient();
        WebTarget webTarget = c.target(BASE_URI);
        Response response = webTarget.path("wssstatus").request().get();
        String responseMsg = response.readEntity(String.class);

        assertEquals(200, response.getStatus());
        // test some content, this can break if the format of the status html changes
        assertTrue(
            responseMsg.toString().indexOf("<TD>URL</TD><TD>/wssstatus</TD>") > -1);
        assertTrue(
            responseMsg.toString().indexOf("<TD>Port</TD><TD>" + BASE_PORT
             + "</TD>") > -1);
    }
    
    @Test
    public void testGet_status3() throws Exception {
        
        Client c = ClientBuilder.newClient();
        WebTarget webTarget = c.target(BASE_URI);
        Response response = webTarget.path("").request().get();

        // lightweigh test, just check status, it should return default doc page
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testPost1() throws Exception {
        Client c = ClientBuilder.newClient();
        WebTarget webTarget = c.target(BASE_URI);
        final String TEST_STR = "some-post-data-text";
        Response response = webTarget.path("queryEPpostecho").request()
              .post(Entity.text(TEST_STR));
        String responseMsg = response.readEntity(String.class);

        assertEquals(200, response.getStatus());
        assertTrue(responseMsg.equals(TEST_STR));
    }

    @Test
    public void testPost2() throws Exception {
        Client c = ClientBuilder.newClient();
        WebTarget webTarget = c.target(BASE_URI);
        final String TEST_STR = "some-post-data2-text";
        Response response = webTarget.path("swag").request()
              .post(Entity.text(TEST_STR));
        String responseMsg = response.readEntity(String.class);

        System.out.println("*** ------------- testPost2 responseMsg: " + responseMsg);
//        assertEquals(200, response.getStatus());
//        assertTrue(responseMsg.equals(TEST_STR));
    }

    @Test
    public void testGet_swag() throws Exception {
        Client c = ClientBuilder.newClient();
        WebTarget webTarget = c.target(BASE_URI);
        Response response = webTarget.path("swag").request().get();
        String responseMsg = response.readEntity(String.class);

        // in progress
    //    System.out.println("*** ------------- swag: " + responseMsg);
//        assertEquals(200, response.getStatus());
//        assertTrue(responseMsg.equals(TEST_STR));
    }
}
