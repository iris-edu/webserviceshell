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

package edu.iris.wss.framework;
/*
import com.sun.grizzly.http.embed.GrizzlyWebServer;
import com.sun.grizzly.http.servlet.ServletAdapter;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.spi.container.servlet.ServletContainer;*/
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.apache.log4j.Logger;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author mike
 */
public class Service_addHeaders_PrecedenceTest  {

    public static final Logger logger = Logger.getLogger(Service_addHeaders_PrecedenceTest.class);

    private static final String BASE_HOST = "http://localhost";
    private static final Integer BASE_PORT = 8093;

    // set notional webapp name
//    private static final String SOME_CONTEXT = "/testservice/dataselect/1";
    private static final String SOME_CONTEXT = "/tstbasepath3";

    private static final URI BASE_URI = URI.create(BASE_HOST + ":"
        + BASE_PORT + SOME_CONTEXT);

    public Service_addHeaders_PrecedenceTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        // setup config dir for test environment
        System.setProperty(Util.WSS_OS_CONFIG_DIR,
            "target"
              + File.separator + "test-classes"
              + File.separator + "ServiceConfigTest");

        createServiceCfgFile(System.getProperty(Util.WSS_OS_CONFIG_DIR),
              SOME_CONTEXT + "-service.cfg");
        createParamCfgFile(System.getProperty(Util.WSS_OS_CONFIG_DIR),
              SOME_CONTEXT + "-param.cfg");

        GrizzlyContainerHelper.setUpServer(BASE_URI,
              Service_addHeaders_PrecedenceTest.class.getName(), SOME_CONTEXT);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        GrizzlyContainerHelper.tearDownServer(
              Service_addHeaders_PrecedenceTest.class.getName());
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

    /**
     * Test for default settings of Content-Disposition
     * - WSS sets default
     * - override in config addHeaders
     * - no override in config formatDispostions
     * - no override by handler
     *
     * @throws Exception
     */
    @Test
    public void test_default_ContentDisp() throws Exception {
        Client c = ClientBuilder.newClient();
        WebTarget webTarget = c.target(BASE_URI)
              .path("/test_CD3")
              .queryParam("format", "TEXT");

        Response response = webTarget.request().get();

        assertEquals(200, response.getStatus());
        assertEquals("text/plain", response.getMediaType().toString());

        // with addHeaders configured with Content-Disposition, that value
        // should override the default
        //assertTrue(response.getHeaderString("Content-Disposition").contains("inline; filename="));
        assertTrue(response.getHeaderString("Content-Disposition").equals("some-addHeaders-CD-hdr"));

        webTarget = c.target(BASE_URI)
              .path("/test_CD3")
              .queryParam("format", "miniseed");

        response = webTarget.request().get();

        assertEquals(200, response.getStatus());
        assertEquals("application/vnd.fdsn.mseed", response.getMediaType().toString());
//        assertTrue(response.getHeaderString("Content-Disposition").contains("attachment; filename="));
        assertTrue(response.getHeaderString("Content-Disposition").equals("some-addHeaders-CD-hdr"));

        // other addHeader header should be present if not overridden
        assertTrue(response.getHeaderString("dummyhdr4").equals("dummyhdr4-for-dummyhdr4"));
    }

    // for the remaining tests, they should override the addHeaders version
    // of Content-Disposition

    /**
     * Test for default settings of Content-Disposition
     * - WSS sets default
     * - override in config addHeaders
     * - override in config formatDispostions
     * - no override by handler
     *
     * @throws Exception
     */
    @Test
    public void test_formatDispositions() throws Exception {
        Client c = ClientBuilder.newClient();
        WebTarget webTarget = c.target(BASE_URI)
              .path("/test_CD3")
              .queryParam("format", "json");

        Response response = webTarget.request().get();

        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getMediaType().toString());
        assertTrue(response.getHeaderString("Content-Disposition").equals("some-contentdisp-for-json"));

        webTarget = c.target(BASE_URI)
              .path("/test_CD3")
              .queryParam("format", "binary");

        response = webTarget.request().get();

        assertEquals(200, response.getStatus());
        assertEquals("application/octet-stream", response.getMediaType().toString());
        assertTrue(response.getHeaderString("Content-Disposition").equals("some-contentdisp-for-binary"));

        // other addHeader header should be present if not overridden
        assertTrue(response.getHeaderString("dummyhdr4").equals("dummyhdr4-for-dummyhdr4"));
    }

    /**
     * Test for default settings of Content-Disposition
     * - WSS sets default
     * - override in config addHeaders
     * - override in config formatDispostions
     * - override by handler
     *
     * @throws Exception
     */
    @Test
    public void test_handler_Dispositions() throws Exception {
        Client c = ClientBuilder.newClient();
        WebTarget webTarget = c.target(BASE_URI)
              .path("/test_CD3")
              .queryParam("format", "json");

        Response response = webTarget.request().get();

        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getMediaType().toString());
        assertTrue(response.getHeaderString("Content-Disposition").equals("some-contentdisp-for-json"));
        assertTrue(response.getHeaderString("dummy-header").equals("dummy-value"));

        webTarget = c.target(BASE_URI)
              .path("/test_CD3")
              .queryParam("format", "json")
              .queryParam("overrideDisp", "true");

        response = webTarget.request().get();

        assertEquals(200, response.getStatus());
//        System.out.println("*************************** entity20: "
//              + response.readEntity(String.class));
//        System.out.println("*************************** contDisp200: "
//              + response.getHeaderString("Content-Disposition"));
        assertEquals("application/json", response.getMediaType().toString());
        assertTrue(response.getHeaderString("Content-Disposition").equals("override-json-settings"));
        assertTrue(response.getHeaderString("dummy-header").equals("dummy-value"));

        // other addHeader header should be present if not overridden
        assertTrue(response.getHeaderString("dummyhdr4").equals("dummyhdr4-for-dummyhdr4"));
    }

    // create a config file to test against on a target test path
    private static void createServiceCfgFile(String filePath, String fileName)
          throws FileNotFoundException, IOException {

        File testFile = new File(filePath + File.separator + fileName);
        OutputStream os = new FileOutputStream(testFile);

        StringBuilder sb = new StringBuilder();

        sb.append("# ---------------- globals").append("\n");
        sb.append("\n");
        sb.append("appName=CmdWithHeader2Test_app").append("\n");
        sb.append("version=default-0.2").append("\n");
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

        sb.append("# ---------------- ").append("\n");
        sb.append("\n");
        sb.append("test_CD3.endpointClassName=edu.iris.wss.endpoints.CmdProcessor").append("\n");

        File file = new File(filePath + File.separator + "set_header_CD3.sh");
        file.setExecutable(true);
        sb.append("test_CD3.handlerProgram=").append(file.getAbsolutePath()).append("\n");

        sb.append("test_CD3.handlerWorkingDirectory=/tmp").append("\n");
        sb.append("\n");
        sb.append("# Timeout in seconds for command line implementation.  Pertains to initial and ongoing waits.").append("\n");
        sb.append("test_CD3.handlerTimeout=40").append("\n");
        sb.append("\n");
        sb.append("test_CD3.formatTypes = \\").append("\n");
        sb.append("    text: text/plain,\\").append("\n");
        sb.append("    json: application/json, \\").append("\n");
        sb.append("    miniseed: application/vnd.fdsn.mseed, \\").append("\n");
        sb.append("    geocsv: text/plain").append("\n");
        sb.append("\n");
        sb.append("test_CD3.formatDispositions = \\").append("\n");
        sb.append("    binary: some-contentdisp-for-binary,\\").append("\n");
        sb.append("    json: some-contentdisp-for-json, \\").append("\n");
        sb.append("    geocsv: some-contentdisp-for-geocsv").append("\n");
        sb.append("\n");
        sb.append("test_CD3.addHeaders = \\").append("\n");
        sb.append("    Content-Disposition: some-addHeaders-CD-hdr,\\").append("\n");
        sb.append("    dummyhdr4: dummyhdr4-for-dummyhdr4").append("\n");
        sb.append("\n");
        sb.append("# usageLog is true by default, set this to false to disable usage logging").append("\n");
        sb.append("##test_CD3.usageLog=false").append("\n");
        sb.append("\n");
        sb.append("# Disable or remove this to disable POST processing").append("\n");
        sb.append("test_CD3.postEnabled=true").append("\n");
        sb.append("\n");
        sb.append("# Enable this to return HTTP 404 in lieu of 204, NO CONTENT").append("\n");
        sb.append("test_CD3.use404For204=true").append("\n");

        os.write(sb.toString().getBytes());
    }

    // create a config file to test against on a target test path
    private static void createParamCfgFile(String filePath, String fileName)
          throws FileNotFoundException, IOException {

        File testFile = new File(filePath + File.separator + fileName);
        OutputStream os = new FileOutputStream(testFile);

        StringBuilder sb = new StringBuilder();

        sb.append("# ----------------  endpoints").append("\n");
        sb.append("\n");

        sb.append("# ---------------- ").append("\n");
        sb.append("\n");
        sb.append("test_CD3.format=TEXT").append("\n");
        sb.append("test_CD3.overrideDisp=BOOLEAN").append("\n");

        os.write(sb.toString().getBytes());
    }
}
