/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.iris.wss.framework;

import java.io.IOException;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author mike
 */
public class AppConfigurator0Test {
    private static final AppConfigurator thisAppCfg = new AppConfigurator();

    public AppConfigurator0Test() {
    }
    
    @BeforeClass
    public static void setUpClass() {
        java.util.Properties props = new java.util.Properties();
        // test file being used as default for loader
        String filename = "META-INF/service.cfg";
        java.net.URL url = ClassLoader.getSystemResource(filename);
        assertNotNull(url);
        
        try {
            props.load(url.openStream());
        } catch (IOException ex) {
           fail("file name misspelled, does not exist, or is not in classpath,"
                   + "  filename: " + filename);
        }

        try {
            thisAppCfg.loadConfigurationParameters(props, null);
        } catch (Exception ex) {
            fail("Unexpected failure in test setup, this is not a test, ex: " + ex);
        }
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testLoadOfOutputTypes() throws Exception {
        RequestInfo ri = new RequestInfo(thisAppCfg);
        
        // test for default
        // endpoint name is taken from the service.cfg file
        String endpointName = "queryEP";
        assert(ri.getPerRequestMediaType(endpointName).equals("application/vnd.fdsn.mseed"));
        
        // Note, these tests are determined by the values in service.cfg
        ri.setPerRequestOutputType(endpointName, "xml");
        assert(ri.getPerRequestMediaType(endpointName).equals("application/xml"));
        ri.setPerRequestOutputType(endpointName, "xMl");
        assert(ri.getPerRequestMediaType(endpointName).equals("application/xml"));
        ri.setPerRequestOutputType(endpointName, "text");
        assert(ri.getPerRequestMediaType(endpointName).equals("text/plain"));
        ri.setPerRequestOutputType(endpointName, "texttree");
        assert(ri.getPerRequestMediaType(endpointName).equals("text/plain"));
        ri.setPerRequestOutputType(endpointName, "json");
        assert(ri.getPerRequestMediaType(endpointName).equals("application/json"));
    
        ri.setPerRequestOutputType(endpointName, "miniseed");
        assert(ri.getPerRequestMediaType(endpointName).equals("application/vnd.fdsn.mseed"));
        ri.setPerRequestOutputType(endpointName, "miniseed ");
        assert(ri.getPerRequestMediaType(endpointName).equals("application/vnd.fdsn.mseed"));
        ri.setPerRequestOutputType(endpointName, " Miniseed");
        assert(ri.getPerRequestMediaType(endpointName).equals("application/vnd.fdsn.mseed"));
        ri.setPerRequestOutputType(endpointName, "    minisEed ");
        assert(ri.getPerRequestMediaType(endpointName).equals("application/vnd.fdsn.mseed"));
        
        ri.setPerRequestOutputType(endpointName, "mseed");
        assert(ri.getPerRequestMediaType(endpointName).equals("application/vnd.fdsn.mseed"));
        ri.setPerRequestOutputType(endpointName, "binary");
        assert(ri.getPerRequestMediaType(endpointName).equals("application/octet-stream"));
    }

    @Test
    public void testLoadExceptionOfOutputTypes() throws Exception {
        RequestInfo ri = new RequestInfo(thisAppCfg);

        // endpoint name is taken from the service.cfg file
        String endpointName = "queryEP";

        try {
            ri.setPerRequestOutputType(endpointName, null);
            fail("getting null type succeeded unexpectedly,"
                    + " should have had an Exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }

        try {
            ri.setPerRequestOutputType(endpointName, "unknown2");
            fail("getting unknown2 type succeeded unexpectedly,"
                    + " should have had an Exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }
    }
}
