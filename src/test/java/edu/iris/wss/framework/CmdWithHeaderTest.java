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

package edu.iris.wss.framework;
/*
import com.sun.grizzly.http.embed.GrizzlyWebServer;
import com.sun.grizzly.http.servlet.ServletAdapter;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.spi.container.servlet.ServletContainer;*/
import edu.iris.wss.endpoints.CmdWithHeaderIrisEP;
import edu.iris.wss.utils.WebUtils;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.servlet.GrizzlyWebContainerFactory;
import org.glassfish.jersey.servlet.ServletProperties;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 * @author mike
 */
public class CmdWithHeaderTest  {

    public static final Logger logger = Logger.getLogger(CmdWithHeaderTest.class);

    private static final String BASE_HOST = "http://localhost";
    private static final Integer BASE_PORT = 8093;
    
    // set notional webapp name
//    private static final String SOME_CONTEXT = "/testservice/dataselect/1";
    private static final String SOME_CONTEXT = "/tstcmd";
    
    private static final URI BASE_URI = URI.create(BASE_HOST + ":"
        + BASE_PORT + SOME_CONTEXT);

    private static HttpServer server;
    
    public CmdWithHeaderTest() {
    }

    @BeforeClass
    public static void setUpClass() throws IOException {
        // setup config dir for test environment
        System.setProperty(WebUtils.wssConfigDirSignature,
            "src"
              + File.separator + "test"
              + File.separator + "resources"
              + File.separator + "ServiceConfigTest");

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
            + CmdWithHeaderTest.class.getName());
        System.out.println("********** started GrizzlyWebServer, config: "
            + server.getServerConfiguration());

        // for manual test of server, uncomment this code then mvn clean install
//        System.out.println("***** Application started, try: " + BASE_URI);
//        System.out.println("***** control-c to stop its...");
//        System.in.read();
    }
    
    @AfterClass
    public static void tearDownClass() {
        System.out.println("********** stopping grizzlyWebServer, class: "
            + CmdWithHeaderTest.class.getName());
        logger.info("*********** stopping grizzlyWebServer");
        server.shutdownNow();
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    private static String readInputStream(InputStream is, int maxBufSize)
          throws IOException {
        byte[] oneByte = new byte[1];
        byte[] bytes = new byte[maxBufSize];
        int keepBytesCnt = 0;
        int i1 = 0;
        for (; i1 < bytes.length; i1++) {
            int bytesRead = is.read(oneByte, 0, 1);
            if (bytesRead < 0) {
                // finished
                break;
            }

            bytes[keepBytesCnt] = oneByte[0];
            keepBytesCnt++;
        }

        System.out.println("* --------------- keepBytesCnt2: " + keepBytesCnt);

        return new String(bytes, 0, keepBytesCnt, "UTF-8");
    }

    @Test
    public void test_getJsondata() throws Exception {
        Client c = ClientBuilder.newClient();
        WebTarget webTarget = c.target(BASE_URI);
        Response response = webTarget.path("/jsonproxy").request().get();
        assertEquals(200, response.getStatus());
        assertEquals(response.getMediaType().toString(), "application/json");

        String testMsg = response.readEntity(String.class);
        assertTrue(testMsg.contains("data from specified test file"));
    }

    @Test
    public void test_getCmd1() throws Exception {
        Client c = ClientBuilder.newClient();
        WebTarget webTarget = c.target(BASE_URI);
        Response response = webTarget.path("/queryEP").request().get();
        System.out.println("* ---------------------------- response: " + response);
        System.out.println("* ---------------------------- user.dir: "
              + System.getProperty("user.dir"));
        assertEquals(200, response.getStatus());

        String testMsg = response.readEntity(String.class);
        System.out.println("* ---------------------------- testMsg: " + testMsg);

        // response should be from sleep_handle2.sh
        // handler name set in the service cfg file, which is depends on the value
        // of SOME_CONTEXT
        assertTrue(testMsg.indexOf("sleep handle2 stdout args") == 0);

        // mediatype should be default value from outputs on queryEP 
        assertEquals(response.getMediaType().toString(), "text/plain");
    }

    @Test
    public void test_getCmd2() throws Exception {
        // simple test with no header identifiers, input should pass through
        // unaffected

        String testInput = "HTTello goodbye";
        ByteArrayInputStream sbis = new ByteArrayInputStream(
              testInput.getBytes("UTF-8"));
        SingletonWrapper sw = new SingletonWrapper();

        Map map = CmdWithHeaderIrisEP.checkForHeaders(sbis,
              sw.HEADER_START_IDENTIFIER_BYTES, sw.HEADER_END_IDENTIFIER_BYTES,
              100, "\n", ":");
        System.out.println("* ---------------------------- test_getCmd2 map: " + map);

        String remaining = readInputStream(sbis, 100);

        assertTrue(map.isEmpty());
        assertTrue(remaining.equals(testInput));
    }

    @Test
    public void test_getCmd3() throws Exception {
        // canonical form with new lines as delimiter

        String hdr1Name = "Headr1";
        String hdr2Name = "Headr2";
        String headers = hdr1Name + " : value1\n" + hdr2Name + " : value2\n";
        String followingData = "some other data";
        String data = SingletonWrapper.HEADER_START_IDENTIFIER
              + headers + SingletonWrapper.HEADER_END_IDENTIFIER
              + followingData;

        ByteArrayInputStream sbis = new ByteArrayInputStream(
              data.getBytes("UTF-8"));

        SingletonWrapper sw = new SingletonWrapper();
        Map map = CmdWithHeaderIrisEP.checkForHeaders(sbis,
              sw.HEADER_START_IDENTIFIER_BYTES, sw.HEADER_END_IDENTIFIER_BYTES,
              100, "\n", ":");

        String remaining = readInputStream(sbis, 100);
        System.out.println("* ---------------------------- test_getCmd3 map: " + map
              + "  remaining: " + remaining
              + "  map.get h1: " + map.get("headr1"));

        assertTrue(map.get(hdr1Name.toLowerCase()).equals("value1"));
        assertTrue(map.get(hdr2Name.toLowerCase()).equals("value2"));
        assertTrue(map.size() == 2);
        assertTrue(remaining.equals(followingData));
    }

    @Test
    public void test_getCmd4() throws Exception {
        // canonical form with too small of buffer, throwing an exception
        // is the expected result

        String hdr1Name = "Headr1";
        String hdr2Name = "Headr2";
        String headers = hdr1Name + " : value1\n" + hdr2Name + " : value2\n";
        String followingData = "some other data";
        String data = SingletonWrapper.HEADER_START_IDENTIFIER
              + headers + SingletonWrapper.HEADER_END_IDENTIFIER
              + followingData;

        ByteArrayInputStream sbis = new ByteArrayInputStream(
              data.getBytes("UTF-8"));

        int bufferSizeToSmall = headers.length()
              + SingletonWrapper.HEADER_END_IDENTIFIER.length() - 1;

        SingletonWrapper sw = new SingletonWrapper();
        try {
            Map map = CmdWithHeaderIrisEP.checkForHeaders(sbis,
                sw.HEADER_START_IDENTIFIER_BYTES, sw.HEADER_END_IDENTIFIER_BYTES,
                bufferSizeToSmall, "\n", ":");
            fail("buffer size to small unexpectedly succeeded,"
                  + " should have thrown exception");
        } catch(Exception ex) {
            System.out.println("* --------------------------- ex cmd4: " + ex);
            assertTrue(ex.toString().contains("Headers check buffer size too"
                  + " small or malformed ending identifier"));
        }
    }

    @Test
    public void test_getCmd5() throws Exception {
        // data ends with incomplete ending identifier with no following
        // data

        String hdr1Name = "Headr1";
        String hdr2Name = "Headr2";
        String headers = hdr1Name + " : value1\n" + hdr2Name + " : value2\n";
        String followingData = "some other data";
        String data = SingletonWrapper.HEADER_START_IDENTIFIER
              + headers
              + SingletonWrapper.HEADER_END_IDENTIFIER.substring(0,
                    SingletonWrapper.HEADER_END_IDENTIFIER.length() - 1 );
        System.out.println("* --------------------------- data cmd5: " + data
        + "  dL: " + data.length());
        ByteArrayInputStream sbis = new ByteArrayInputStream(
              data.getBytes("UTF-8"));

        int bufferSize = 100;

        SingletonWrapper sw = new SingletonWrapper();
        try {
            Map map = CmdWithHeaderIrisEP.checkForHeaders(sbis,
                sw.HEADER_START_IDENTIFIER_BYTES, sw.HEADER_END_IDENTIFIER_BYTES,
                bufferSize, "\n", ":");
            fail("incomplete ending identifier, no following data unexpectedly"
                  + " succeeded, should have thrown exception");
        } catch(Exception ex) {
            System.out.println("* --------------------------- ex cmd5: " + ex);
            assertTrue(ex.toString().contains(
                  "Headers were not completely read"));
        }
    }

    @Test
    public void test_getCmd6() throws Exception {
        // data ends with incomplete ending identifier after reading following
        // data

        String hdr1Name = "Headr1";
        String hdr2Name = "Headr2";
        String headers = hdr1Name + " : value1\n" + hdr2Name + " : value2\n";
        String followingData = "some other data";
        String data = SingletonWrapper.HEADER_START_IDENTIFIER
              + headers
              + SingletonWrapper.HEADER_END_IDENTIFIER.substring(0,
                    SingletonWrapper.HEADER_END_IDENTIFIER.length() - 1 )
              + followingData;
        System.out.println("* --------------------------- data cmd6: " + data
        + "  dL: " + data.length());
        ByteArrayInputStream sbis = new ByteArrayInputStream(
              data.getBytes("UTF-8"));

        int bufferSize = 100;

        SingletonWrapper sw = new SingletonWrapper();
        try {
            Map map = CmdWithHeaderIrisEP.checkForHeaders(sbis,
                sw.HEADER_START_IDENTIFIER_BYTES, sw.HEADER_END_IDENTIFIER_BYTES,
                bufferSize, "\n", ":");
            fail("incomplete ending identifier, and following data unexpectedly succeeded,"
                  + " should have thrown exception");
        } catch(Exception ex) {
            System.out.println("* --------------------------- ex cmd6: " + ex);
            assertTrue(ex.toString().contains(
                  "Headers were not completely read"));
        }
    }

    @Test
    public void test_set_header_CD1() throws Exception {
        Client c = ClientBuilder.newClient();
        WebTarget webTarget = c.target(BASE_URI);
        Response response = webTarget.path("/test_CD1").request().get();
        System.out.println("* ---------------------------- response: " + response);
        System.out.println("* ---------------------------- response headers CD1: " + response.getHeaders());
        System.out.println("* ---------------------------- response header CD: " + response.getHeaderString("Content-Disposition"));

        System.out.println("* ---------------------------- user.dir: "
              + System.getProperty("user.dir"));
        assertEquals(200, response.getStatus());

        String testMsg = response.readEntity(String.class);
        System.out.println("* ---------------------------- testMsg: " + testMsg);

        assertEquals("inline", response.getHeaderString("Content-Disposition"));
        assertEquals("http://host.example", response.getHeaderString("Access-Control-Allow-Origin"));

        // mediatype should be default value from outputs on test_CD1 
        assertEquals("application/octet-stream", response.getMediaType().toString());
        
        // should also have the extra header
        assertEquals("value-for-test-hdr", response.getHeaderString("Test-Header"));
    }

    @Test
    public void test_set_header_CD2() throws Exception {
        Client c = ClientBuilder.newClient();
        WebTarget webTarget = c.target(BASE_URI);
        Response response = webTarget.path("/test_CD2").request().get();
        System.out.println("* ---------------------------- response cd2: " + response);
        System.out.println("* ---------------------------- response headers cd2: " + response.getHeaders());
        System.out.println("* ---------------------------- response header CD2: " + response.getHeaderString("Content-Disposition"));

        assertEquals(200, response.getStatus());

        String testMsg = response.readEntity(String.class);
        System.out.println("* ---------------------------- testMsg cd2: " + testMsg);

        // mediatype should be default value from outputs on test_CD1 
        assertEquals("application/octet-stream", response.getMediaType().toString());

        // must be binary media type in order to have WSS add "Content-Disposition"
        assertTrue(response.getHeaderString("Content-Disposition").contains("attachment"));
        assertEquals("*", response.getHeaderString("Access-Control-Allow-Origin"));
    }
}
