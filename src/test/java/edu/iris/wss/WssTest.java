/*******************************************************************************
 * Copyright (c) 2018 IRIS DMC supported by the National Science Foundation.
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
import edu.iris.wss.framework.GrizzlyContainerHelper;
import edu.iris.wss.framework.Util;
import java.io.File;
import java.net.URI;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.http.server.HttpServer;
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
    private static final String SOME_CONTEXT = "/";

    private static final URI BASE_URI = URI.create("http://localhost:"
        + BASE_PORT + SOME_CONTEXT);

    private static HttpServer server;

    public WssTest() {
    }

    @BeforeClass
    public static void setUpClass() {;
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws Exception {
        // setup config dir for log4j,
        // NOTE: without service and parama cfg, default, dummy information
        //       will be read from META-INF
        System.setProperty(Util.WSS_OS_CONFIG_DIR,
            "target"
              + File.separator + "test-classes");

        GrizzlyContainerHelper.setUpServer(BASE_URI, this.getClass().getName(),
              SOME_CONTEXT);
    }

    @After
    public void tearDown() throws Exception {
        GrizzlyContainerHelper.tearDownServer(this.getClass().getName());
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
    public void testGet_nopath() throws Exception {

        Client c = ClientBuilder.newClient();
        WebTarget webTarget = c.target(BASE_URI);
        Response response = webTarget.path("").request().get();

        // lightweight test, just check status, the return should be a doc page
        assertEquals(200, response.getStatus());
    }
}
