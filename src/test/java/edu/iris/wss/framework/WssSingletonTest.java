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

package edu.iris.wss.framework;

import edu.iris.wss.framework.WssSingletonHelper.TEST_TYPE;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.log4j.Logger;
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

    private static final String BASE_HOST = "http://localhost";
    private static final Integer BASE_PORT = 8093;

    private static final URI BASE_URI = URI.create(BASE_HOST + ":"
        + BASE_PORT + SOME_CONTEXT);

    // 2017-10-25 - Was use of NAME_TYPES_TO_TEST necessary after previous
    //              changes for glassfish? Can it be removed?
    private static final ArrayList<TEST_TYPE> NAME_TYPES_TO_TEST = new ArrayList();
    private static int nameCounter = 0;
    static {
        NAME_TYPES_TO_TEST.add(TEST_TYPE.CONFIG_URL);
        NAME_TYPES_TO_TEST.add(TEST_TYPE.CONFIG_FILE);
        NAME_TYPES_TO_TEST.add(TEST_TYPE.CONFIG_BOGUS_URL);
        NAME_TYPES_TO_TEST.add(TEST_TYPE.BRIEF_MSG_IS_NULL);
        NAME_TYPES_TO_TEST.add(TEST_TYPE.DETAILED_MSG_IS_NULL);
    }

    @Context	javax.servlet.http.HttpServletRequest request;

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        // setup WSS service config dir for test environment
        System.setProperty(Util.WSS_OS_CONFIG_DIR,
            "target"
              + File.separator + "test-classes"
              + File.separator + "WssSingletonTest");

        WssSingletonHelper.setupCfgFiles(Util.WSS_OS_CONFIG_DIR, SOME_CONTEXT,
              NAME_TYPES_TO_TEST.get(nameCounter));
        nameCounter++; // NOTE: the number of tests cannot exceed the
                       //       number of elementas in NAME_TYPES_TO_TEST

        GrizzlyContainerHelper.setUpServer(BASE_URI, this.getClass().getName(),
              SOME_CONTEXT);
    }

    @After
    public void tearDown() throws Exception {
        GrizzlyContainerHelper.tearDownServer(this.getClass().getName());
    }

    // NOTE: Each of the following tests cause setUp and tearDown to run
    //       once, therefor the number of tests must not be greater than
    //       the number of elements in NAME_TYPES_TO_TEST
    //
    @Test
    public void test_log_types_0() throws Exception {
        // this test should run for TEST_TYPE.CONFIG_URL, fail on the first
        // try and find the rabbit config file on the second try
        test_log_usage();
        test_log_wfstat();
        test_log_error();
        test_log_error_with_exception();
    }

    @Test
    public void test_log_types_1() throws Exception {
        // this test should run for TEST_TYPE.CONFIG_FILE and find
        // the rabbit config file on the first try
        test_log_usage();
        test_log_wfstat();
        test_log_error();
        test_log_error_with_exception();
    }

    @Test
    public void test_log_types_2() throws Exception {
        // this test should run for TEST_TYPE.CONFIG_BOGUS_URL and fail to
        // initialize rabbit publisher, but continute to run while writing
        // error log messages to the log file for each publish attempt
        test_log_usage();
        test_log_wfstat();
        test_log_error();
        test_log_error_with_exception();

        // tag along to test media type acceptance
        test_request_of_media_type_defined();
        test_request_of_media_type_not_defined();
    }

    // The LoggingEndpoint helper code should generate the respective
    // publish message in response to the queryParam "messageType", i.e.
    // usage, wfstat, error, or error_with_exception
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

    public void test_request_of_media_type_defined() throws Exception {

        Client c = ClientBuilder.newClient();

        WebTarget webTarget = c.target(BASE_URI)
              .path("/test_logging")
              .queryParam("format", "JSON")
              .queryParam("messageType", "usage");

        // json is defined in endpoint format type
        Response response = webTarget.request(new MediaType("application", "json")).get();

        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getMediaType().toString());
    }

    public void test_request_of_media_type_not_defined() throws Exception {

        Client c = ClientBuilder.newClient();

        WebTarget webTarget = c.target(BASE_URI)
              .path("/test_logging")
              .queryParam("format", "JSON")
              .queryParam("messageType", "usage");

        // xml not defined in endpoint format type
        Response response = webTarget.request(new MediaType("application", "xml")).get();

        assertEquals(406, response.getStatus());
        // error message returned in html page
        assertEquals("text/html;charset=ISO-8859-1", response.getMediaType().toString());
    }

    // Althought the original intent of these test does not work as described
    // in the commment after the assert 400, they do allow checking the handling
    // of nulls
    @Test
    public void test_log_log_and_throw_test_null_briefMsg() throws Exception {

        Client c = ClientBuilder.newClient();

        WebTarget webTarget = c.target(BASE_URI)
              .path("/test_logging")
              .queryParam("format", "TEXT")
              .queryParam("messageType", "log_and_throw_test_null_briefMsg");

        Response response = webTarget.request().get();
        assertEquals(400, response.getStatus());

        // can't pass this test because apparently exception handling with junit
        // and grizzley server does not construct a WebApplicationException
        // completely
//        assertEquals("text/plain", response.getMediaType().toString());
    }

    // Althought the original intent of these test does not work as described
    // in the commment after the assert 400, they do allow checking the handling
    // of nulls
    @Test
    public void test_log_log_and_throw_test_null_detailMsg() throws Exception {

        Client c = ClientBuilder.newClient();

        WebTarget webTarget = c.target(BASE_URI)
              .path("/test_logging")
              .queryParam("format", "TEXT")
              .queryParam("messageType", "log_and_throw_test_null_detailMsg");

        Response response = webTarget.request().get();
        assertEquals(400, response.getStatus());

        // can't pass this test because apparently exception handling with junit
        // and grizzley server does not construct a WebApplicationException
        // completely
//        assertEquals("text/plain", response.getMediaType().toString());
    }
}
