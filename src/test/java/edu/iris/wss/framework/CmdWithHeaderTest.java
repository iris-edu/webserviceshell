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
import edu.iris.wss.endpoints.CmdProcessor;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
        System.setProperty(Util.WSS_OS_CONFIG_DIR,
            "target"
              + File.separator + "test-classes"
              + File.separator + "ServiceConfigTest");

        createTestCfgFile(System.getProperty(Util.WSS_OS_CONFIG_DIR),
              SOME_CONTEXT + "-service.cfg");

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

        assertEquals(200, response.getStatus());

        String testMsg = response.readEntity(String.class);

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
        WssSingleton sw = new WssSingleton();

        Map map = CmdProcessor.checkForHeaders(sbis,
              sw.HEADER_START_IDENTIFIER_BYTES, sw.HEADER_END_IDENTIFIER_BYTES,
              100, "\n", ":");

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
        String data = WssSingleton.HEADER_START_IDENTIFIER
              + headers + WssSingleton.HEADER_END_IDENTIFIER
              + followingData;

        ByteArrayInputStream sbis = new ByteArrayInputStream(
              data.getBytes("UTF-8"));

        WssSingleton sw = new WssSingleton();
        Map map = CmdProcessor.checkForHeaders(sbis,
              sw.HEADER_START_IDENTIFIER_BYTES, sw.HEADER_END_IDENTIFIER_BYTES,
              100, "\n", ":");

        String remaining = readInputStream(sbis, 100);

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
        String data = WssSingleton.HEADER_START_IDENTIFIER
              + headers + WssSingleton.HEADER_END_IDENTIFIER
              + followingData;

        ByteArrayInputStream sbis = new ByteArrayInputStream(
              data.getBytes("UTF-8"));

        int bufferSizeToSmall = headers.length()
              + WssSingleton.HEADER_END_IDENTIFIER.length() - 1;

        WssSingleton sw = new WssSingleton();
        try {
            Map map = CmdProcessor.checkForHeaders(sbis,
                sw.HEADER_START_IDENTIFIER_BYTES, sw.HEADER_END_IDENTIFIER_BYTES,
                bufferSizeToSmall, "\n", ":");
            fail("buffer size to small unexpectedly succeeded,"
                  + " should have thrown exception");
        } catch(Exception ex) {
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
        String data = WssSingleton.HEADER_START_IDENTIFIER
              + headers
              + WssSingleton.HEADER_END_IDENTIFIER.substring(0,
                    WssSingleton.HEADER_END_IDENTIFIER.length() - 1 );

        ByteArrayInputStream sbis = new ByteArrayInputStream(
              data.getBytes("UTF-8"));

        int bufferSize = 100;

        WssSingleton sw = new WssSingleton();
        try {
            Map map = CmdProcessor.checkForHeaders(sbis,
                sw.HEADER_START_IDENTIFIER_BYTES, sw.HEADER_END_IDENTIFIER_BYTES,
                bufferSize, "\n", ":");
            fail("incomplete ending identifier, no following data unexpectedly"
                  + " succeeded, should have thrown exception");
        } catch(Exception ex) {
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
        String data = WssSingleton.HEADER_START_IDENTIFIER
              + headers
              + WssSingleton.HEADER_END_IDENTIFIER.substring(0,
                    WssSingleton.HEADER_END_IDENTIFIER.length() - 1 )
              + followingData;

        ByteArrayInputStream sbis = new ByteArrayInputStream(
              data.getBytes("UTF-8"));

        int bufferSize = 100;

        WssSingleton sw = new WssSingleton();
        try {
            Map map = CmdProcessor.checkForHeaders(sbis,
                sw.HEADER_START_IDENTIFIER_BYTES, sw.HEADER_END_IDENTIFIER_BYTES,
                bufferSize, "\n", ":");
            fail("incomplete ending identifier, and following data unexpectedly succeeded,"
                  + " should have thrown exception");
        } catch(Exception ex) {
            assertTrue(ex.toString().contains(
                  "Headers were not completely read"));
        }
    }

    @Test
    public void test_set_header_CD1() throws Exception {
        Client c = ClientBuilder.newClient();
        WebTarget webTarget = c.target(BASE_URI);
        Response response = webTarget.path("/test_CD1").request().get();

        assertEquals(200, response.getStatus());

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

        assertEquals(200, response.getStatus());

        // mediatype should be default value from outputs on test_CD1 
        assertEquals("application/octet-stream", response.getMediaType().toString());

        // must be binary media type in order to have WSS add "Content-Disposition"
        assertTrue(response.getHeaderString("Content-Disposition").contains("attachment"));
        assertEquals("*", response.getHeaderString("Access-Control-Allow-Origin"));
    }

    // create a config file to test against on a target test path
    private static void createTestCfgFile(String filePath, String fileName)
          throws FileNotFoundException, IOException {

        File testFile = new File(filePath + File.separator + fileName);
        OutputStream os = new FileOutputStream(testFile);

        StringBuilder sb = new StringBuilder();

        sb.append("# ---------------- globals").append("\n");
        sb.append("\n");
        sb.append("appName=cmd-with-headers-test").append("\n");
        sb.append("appVersion=default-0.1").append("\n");
        sb.append("\n");
        sb.append("# CORS is enabled by default, set to false to disable CORS processing").append("\n");
        sb.append("#corsEnabled=false").append("\n");
        sb.append("\n");
        sb.append("##rootServiceDoc=http://service/fdsnwsbeta/dataselect/docs/1/root/").append("\n");
        sb.append("rootServiceDoc=file:///Users/tomcat/tomcat-8092-7.0.54/dataselect_config1/dataselect-root.html").append("\n");
        sb.append("\n");
        sb.append("# Override the default 100msec SIGKILL delay (from SIGTERM signal)").append("\n");
        sb.append("sigkillDelay=200").append("\n");
        sb.append("\n");
        sb.append("# If present, an instance of the singleton class will be created at application start").append("\n");
        sb.append("singletonClassName=edu.iris.wss.provider.TestSingleton").append("\n");
        sb.append("\n");
        sb.append("# LOG4J or JMS").append("\n");
        sb.append("loggingMethod=LOG4J").append("\n");
        sb.append("\n");
        sb.append("# ----------------  endpoints").append("\n");
        sb.append("\n");
        sb.append("queryEP.endpointClassName=edu.iris.wss.endpoints.CmdProcessor").append("\n");

        // determine full file path within this test environment
        File file = new File(filePath + File.separator + "sleep_handle2.sh");
        file.setExecutable(true);
        sb.append("queryEP.handlerProgram=").append(file.getAbsolutePath()).append("\n");

        sb.append("queryEP.handlerWorkingDirectory=/tmp").append("\n");
        sb.append("\n");
        sb.append("# Timeout in seconds for command line implementation.  Pertains to initial and ongoing waits.").append("\n");
        sb.append("queryEP.handlerTimeout=40").append("\n");
        sb.append("\n");
        sb.append("queryEP.outputTypes = \\").append("\n");
        sb.append("    text: text/plain,\\").append("\n");
        sb.append("    json: application/json, \\").append("\n");
        sb.append("    texttree: text/plain,\\").append("\n");
        sb.append("    xml: application/xml").append("\n");
        sb.append("\n");
        sb.append("# usageLog is true by default, set this to false to disable usage logging").append("\n");
        sb.append("##queryEP.usageLog=false").append("\n");
        sb.append("\n");
        sb.append("# Disable or remove this to disable POST processing").append("\n");
        sb.append("queryEP.postEnabled=true").append("\n");
        sb.append("\n");
        sb.append("# Enable this to return HTTP 404 in lieu of 204, NO CONTENT").append("\n");
        sb.append("queryEP.use404For204=true").append("\n");
        sb.append("\n");
        sb.append("# ---------------- ").append("\n");
        sb.append("\n");
        sb.append("jsonproxy.endpointClassName=edu.iris.wss.endpoints.ProxyResource").append("\n");
        sb.append("\n");

        file = new File(filePath + File.separator + "testdata1.json");
        System.out.println("* ------------------------------------- pfilenamea : " + file.getAbsolutePath());

        sb.append("jsonproxy.proxyURL=file://").append(file.getAbsolutePath()).append("\n");
        sb.append("\n");
        sb.append("jsonproxy.outputTypes = \\").append("\n");
        sb.append("    json: application/json, \\").append("\n");
        sb.append("    text: text/plain,\\").append("\n");
        sb.append("    texttree: text/plain,\\").append("\n");
        sb.append("    xml: application/xml").append("\n");
        sb.append("\n");
        sb.append("# ---------------- ").append("\n");
        sb.append("\n");
        sb.append("test_CD1.endpointClassName=edu.iris.wss.endpoints.CmdProcessor").append("\n");

        file = new File(filePath + File.separator + "set_header_CD1.sh");
        file.setExecutable(true);
        sb.append("test_CD1.handlerProgram=").append(file.getAbsolutePath()).append("\n");

        sb.append("test_CD1.handlerWorkingDirectory=/tmp").append("\n");
        sb.append("\n");
        sb.append("# Timeout in seconds for command line implementation.  Pertains to initial and ongoing waits.").append("\n");
        sb.append("test_CD1.handlerTimeout=40").append("\n");
        sb.append("\n");
        sb.append("test_CD1.outputTypes = \\").append("\n");
        sb.append("    binary: application/octet-stream,\\").append("\n");
        sb.append("    text: text/plain,\\").append("\n");
        sb.append("    json: application/json, \\").append("\n");
        sb.append("    xml: application/xml").append("\n");
        sb.append("\n");
        sb.append("# usageLog is true by default, set this to false to disable usage logging").append("\n");
        sb.append("##test_CD1.usageLog=false").append("\n");
        sb.append("\n");
        sb.append("# Disable or remove this to disable POST processing").append("\n");
        sb.append("test_CD1.postEnabled=true").append("\n");
        sb.append("\n");
        sb.append("# Enable this to return HTTP 404 in lieu of 204, NO CONTENT").append("\n");
        sb.append("test_CD1.use404For204=true").append("\n");
        sb.append("\n");
        sb.append("# ---------------- ").append("\n");
        sb.append("\n");
        sb.append("test_CD2.endpointClassName=edu.iris.wss.endpoints.CmdProcessor").append("\n");

        file = new File(filePath + File.separator + "set_header_CD2.sh");
        file.setExecutable(true);
        sb.append("test_CD2.handlerProgram=").append(file.getAbsolutePath()).append("\n");

        sb.append("test_CD2.handlerWorkingDirectory=/tmp").append("\n");
        sb.append("\n");
        sb.append("# Timeout in seconds for command line implementation.  Pertains to initial and ongoing waits.").append("\n");
        sb.append("test_CD2.handlerTimeout=40").append("\n");
        sb.append("\n");
        sb.append("test_CD2.outputTypes = \\").append("\n");
        sb.append("    binary: application/octet-stream,\\").append("\n");
        sb.append("    text: text/plain,\\").append("\n");
        sb.append("    json: application/json, \\").append("\n");
        sb.append("    xml: application/xml").append("\n");
        sb.append("\n");
        sb.append("# usageLog is true by default, set this to false to disable usage logging").append("\n");
        sb.append("##test_CD2.usageLog=false").append("\n");
        sb.append("\n");
        sb.append("# Disable or remove this to disable POST processing").append("\n");
        sb.append("test_CD2.postEnabled=true").append("\n");
        sb.append("\n");
        sb.append("# Enable this to return HTTP 404 in lieu of 204, NO CONTENT").append("\n");
        sb.append("test_CD2.use404For204=true").append("\n");

        os.write(sb.toString().getBytes());
    }
}
