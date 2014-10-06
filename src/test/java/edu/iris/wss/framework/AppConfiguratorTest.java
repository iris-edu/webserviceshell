/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.iris.wss.framework;

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
public class AppConfiguratorTest {
    AppConfigurator appConInstance1 = new AppConfigurator();

    public AppConfiguratorTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
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
        java.util.Properties props = new java.util.Properties();
        java.net.URL url = ClassLoader.getSystemResource("META-INF/service.cfg");
        assertNotNull(url);
        
        props.load(url.openStream());
        
        AppConfigurator serviceCfg = new AppConfigurator();
        serviceCfg.setOutputTypes((String)props.get("outputTypes"));
        
        // Note, these tests are determined on the values in service.cfg
        assert(serviceCfg.getOutputContentType("xml").equals("application/xml"));
        assert(serviceCfg.getOutputContentType("XML").equals("application/xml"));
        assert(serviceCfg.getOutputContentType("text").equals("text/plain"));
        assert(serviceCfg.getOutputContentType("TEXT").equals("text/plain"));
        assert(serviceCfg.getOutputContentType("miniseed").equals("application/vnd.fdsn.mseed"));
        assert(serviceCfg.getOutputContentType("MINISEED").equals("application/vnd.fdsn.mseed"));
        assert(serviceCfg.getOutputContentType("mseed").equals("application/vnd.fdsn.mseed"));
        assert(serviceCfg.getOutputContentType("MSEED").equals("application/vnd.fdsn.mseed"));
        assert(serviceCfg.getOutputContentType("binary").equals("application/octet-stream"));
        assert(serviceCfg.getOutputContentType("BINARY").equals("application/octet-stream"));            

        try {
            serviceCfg.getOutputContentType("unknown");
            fail("getting unknown type succeeded unexpectedly, should have had an Exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }
    }
    
    @Test
    public void testForMSeExceptionInOutputTypesSetter() throws Exception {
        try {
            appConInstance1.setOutputTypes("miniseed");
            fail("zero length try succeeded unexpectedly, should have had an Exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }
    }
    
    @Test
    public void testForZeroLengthExceptionInOutputTypesSetter() throws Exception {
        try {
            appConInstance1.setOutputTypes("");
            fail("zero length try succeeded unexpectedly, should have had an Exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }
    }
    
    @Test
    public void testForNullExceptionInOutputTypesSetter() throws Exception {
        try {
            appConInstance1.setOutputTypes(null);
            fail("null try succeeded unexpectedly, should have had an Exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }
    }
    
    @Test
    public void testForMissingCommasExceptionInOutputTypesSetter() throws Exception {
        // no commas should throw exception
        String outputTypes = "MINISEED: application/vnd.fdsn.mseed"
                + " BINARY: application/octet-stream"
                + " TEXT: text/plain";
        try {
            appConInstance1.setOutputTypes(outputTypes);
            fail("comma try succeeded unexpectedly, should have had an Exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }
    }
    
    @Test
    public void testForNoColonExceptionInInOutputTypesSetter() throws Exception {
        // no colorns should throw exception
        String outputTypes = "MINISEED: application/vnd.fdsn.mseed,"
                + " BINARY| application/octet-stream,"
                + " TEXT: text/plain";
        try {
            appConInstance1.setOutputTypes(outputTypes);
            fail("colon try succeeded unexpectedly, should have had an Exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }
    }
}
