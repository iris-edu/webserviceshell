/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.iris.wss.framework;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.fail;

/**
 *
 * @author mike
 */
public class RequestInfo_2_service_file_Test {
    public RequestInfo_2_service_file_Test() {
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
        AppConfigurator appCfg = 
              AppConfigurator_getters_Test.createTestObjAppCfg("META-INF/service.cfg");
        RequestInfo ri = new RequestInfo(appCfg);
        
        // test for default
        // endpoint name is taken from the service.cfg file
        String endpointName = "dummyEP";
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
        AppConfigurator appCfg = 
              AppConfigurator_getters_Test.createTestObjAppCfg("META-INF/service.cfg");
        RequestInfo ri = new RequestInfo(appCfg);

        // endpoint name is taken from the service.cfg file
        String endpointName = "dummyEP";

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
