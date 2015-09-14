/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.iris.wss.framework;

import java.io.IOException;
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
            thisAppCfg.setOutputTypes((String)props.get("outputTypes"));
        } catch (Exception ex) {
           fail("Error setting configuration properties, excp: "
                   + ex);
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
        assert(ri.getPerRequestMediaType().equals("application/vnd.fdsn.mseed"));
        
        // Note, these tests are determined by the values in service.cfg
        ri.setPerRequestOutputType("xml");
        assert(ri.getPerRequestMediaType().equals("application/xml"));
        ri.setPerRequestOutputType("xMl");
        assert(ri.getPerRequestMediaType().equals("application/xml"));
        ri.setPerRequestOutputType("text");
        assert(ri.getPerRequestMediaType().equals("text/plain"));
        ri.setPerRequestOutputType("texttree");
        assert(ri.getPerRequestMediaType().equals("text/plain"));
        ri.setPerRequestOutputType("json");
        assert(ri.getPerRequestMediaType().equals("application/json"));
    
        ri.setPerRequestOutputType("miniseed");
        assert(ri.getPerRequestMediaType().equals("application/vnd.fdsn.mseed"));
        ri.setPerRequestOutputType("miniseed ");
        assert(ri.getPerRequestMediaType().equals("application/vnd.fdsn.mseed"));
        ri.setPerRequestOutputType(" Miniseed");
        assert(ri.getPerRequestMediaType().equals("application/vnd.fdsn.mseed"));
        ri.setPerRequestOutputType("    minisEed ");
        assert(ri.getPerRequestMediaType().equals("application/vnd.fdsn.mseed"));
        
        ri.setPerRequestOutputType("mseed");
        assert(ri.getPerRequestMediaType().equals("application/vnd.fdsn.mseed"));
        ri.setPerRequestOutputType("binary");
        assert(ri.getPerRequestMediaType().equals("application/octet-stream"));

        try {
            ri.setPerRequestOutputType(null);
            fail("getting null type succeeded unexpectedly,"
                    + " should have had an Exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }
    }

    @Test
    public void testLoadExceptionOfOutputTypes() throws Exception {
        RequestInfo ri = new RequestInfo(thisAppCfg);

        try {
            ri.setPerRequestOutputType("unknown2");
            fail("getting unknown2 type succeeded unexpectedly,"
                    + " should have had an Exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }
    }
}
