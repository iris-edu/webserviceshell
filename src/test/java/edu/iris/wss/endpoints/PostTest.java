/*******************************************************************************
 * Copyright (c) 2017 IRIS DMC supported by the National Science Foundation.
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

package edu.iris.wss.endpoints;

import edu.iris.wss.framework.AppConfigurator;
import edu.iris.wss.framework.FileCreaterHelper;
import edu.iris.wss.framework.GrizzlyContainerHelper;
import edu.iris.wss.framework.ParamConfigurator;
import edu.iris.wss.framework.Util;
import java.io.File;
import java.net.URI;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author mike
 */
public class PostTest {
    public static final String THIS_CLASS_NAME = PostTest.class.getSimpleName();
    public static final Logger LOGGER = Logger.getLogger(THIS_CLASS_NAME);

    private static enum TEST_PARAM {
		EXIT_0_WITH_STDOUT, EXIT_0_WITH_NO_STDOUT, EXIT_3_WITH_ERR_MSG,
        EXIT_3_WITH_NO_ERR_MSG, EXIT_1234_NO_OUTPUT, EXIT_4_STDOUT_THEN_STDERR
	};

    private static final String SERVICE_CONTEXT = "/posttest";
    private static final String ENDPOINT_NAME = "process";

    private static final String BASE_HOST = "http://localhost";
    private static final Integer BASE_PORT = 8093;

    private static final URI BASE_URI = URI.create(BASE_HOST + ":"
        + BASE_PORT + SERVICE_CONTEXT);

    public PostTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        // define WSS config dir for this test
        System.setProperty(Util.WSS_OS_CONFIG_DIR,
            "target"
              + File.separator + "test-classes"
              + File.separator + THIS_CLASS_NAME);
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws Exception {
        String className = "edu.iris.wss.endpoints.PostEndpoint";
        String newFN = FileCreaterHelper.createFileInWssFolder(SERVICE_CONTEXT,
              AppConfigurator.SERVICE_CFG_NAME_SUFFIX,
              createServiceCfgStr(ENDPOINT_NAME, className),
              false);

        newFN = FileCreaterHelper.createFileInWssFolder(SERVICE_CONTEXT,
              ParamConfigurator.PARAM_CFG_NAME_SUFFIX,
              createParamCfgStr(ENDPOINT_NAME),
              false);


        GrizzlyContainerHelper.setUpServer(BASE_URI, this.getClass().getName(),
              SERVICE_CONTEXT);
    }

    @After
    public void tearDown() throws Exception {
        GrizzlyContainerHelper.tearDownServer(this.getClass().getName());
    }

    //@Test // TBD add a get in at some point?
    public void test_1() throws Exception {
        test_all();
    }

    private void test_all() throws Exception {
        test_0_stdout();
    }

    public void test_0_stdout() throws Exception {
        Response response = do_GET(TEST_PARAM.EXIT_0_WITH_STDOUT);

        assertEquals(200, response.getStatus());
        assertEquals("text/plain", response.getMediaType().toString());
    }

    private Response do_GET(TEST_PARAM tparm) throws Exception {
        Client c = ClientBuilder.newClient();

        WebTarget webTarget = c.target(BASE_URI)
              .path(ENDPOINT_NAME)
              .queryParam(tparm.toString(), "no_val")
              .queryParam("format", "TEXT");

        Response response = webTarget.request().get();

        // This should be true for all queries when corsEnabled=false
        assertEquals(null, response.getHeaderString("access-control-allow-origin"));

        return response;
    }

    @Test
    public void testMultipart_1() throws Exception {
        Client c = ClientBuilder.newBuilder()
              .register(MultiPartFeature.class).build();
        WebTarget webTarget = c.target(BASE_URI);

        final String TEST_STR1 = "some-multipart-data-text-one";
        final String TEST_PART_NAME_2 = "part2test";
        final String TEST_STR2 = "some-multipart-data-text-two";

        MultiPart multiPart = new MultiPart();
        multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);

        FormDataBodyPart formPart1 = new FormDataBodyPart("part1test",
              TEST_STR1);
        multiPart.bodyPart(formPart1);
        FormDataBodyPart formPart2 = new FormDataBodyPart(TEST_PART_NAME_2,
              TEST_STR2);
        multiPart.bodyPart(formPart2);

//        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file",
//            new File("/Users/mike/c2_iris_d/code/webserviceshell/pom.xml"),
//            MediaType.APPLICATION_OCTET_STREAM_TYPE);
//        multiPart.bodyPart(fileDataBodyPart);

//        Response response = webTarget.path(ENDPOINT_NAME).request(MediaType.APPLICATION_JSON_TYPE)
        Response response = webTarget.path(ENDPOINT_NAME).request()
              .post(Entity.entity(multiPart, multiPart.getMediaType()));
        String responseMsg = response.readEntity(String.class);

        assertEquals(200, response.getStatus());
        assertTrue(responseMsg.contains(TEST_STR2));
        assertTrue(responseMsg.contains(TEST_PART_NAME_2));
     }

    @Test
    public void testPost2() throws Exception {
        Client c = ClientBuilder.newClient();
        WebTarget webTarget = c.target(BASE_URI);
        final String TEST_STR = "some-post-data2-text";
        Response response = webTarget.path(ENDPOINT_NAME).request()
              .post(Entity.text(TEST_STR));
        String responseMsg = response.readEntity(String.class);

        assertEquals(200, response.getStatus());
        assertTrue(responseMsg.equals(TEST_STR));
    }

    private static String createServiceCfgStr(String endpointName,
          String endpointClass) {
        String s = String.join("\n",
              "# ---------------- globals",
              "",
              "appName=" + THIS_CLASS_NAME,
              "version=0.1",
              "",
              "corsEnabled=false",
              "",
              "# LOG4J or JMS",
              "loggingMethod=LOG4J",
              "",
              "# If present, an instance of the singleton class will be created at application start",
              "singletonClassName=edu.iris.wss.framework.TestSingleton",
              "",
              "# ----------------  endpoints",
              "",
              endpointName + ".endpointClassName=" + endpointClass,
              endpointName + ".usageLog",
              endpointName + ".postEnabled=true",
              endpointName + ".logMiniseedExtents = false",
              endpointName + ".use404For204=false",
              endpointName + ".formatTypes = \\",
              "    text: text/plain,\\",
              "    json: application/json, \\",
              "    miniseed: application/vnd.fdsn.mseed, \\",
              "    geocsv: text/plain",
              ""
        );

        return s;
    }

    private static String createParamCfgStr(String endpointName) {
        String s = String.join("\n",
              "# ----------------  endpoints",
              "",
              endpointName + ".format=TEXT"
        );

        return s;
    }
}
