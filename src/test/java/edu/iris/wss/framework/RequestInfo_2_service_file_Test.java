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
    public void testLoadOfFormatTypes() throws Exception {
        AppConfigurator appCfg = 
              AppConfigurator_getters_Test.createTestObjAppCfg("META-INF/service.cfg");
        RequestInfo ri = new RequestInfo(appCfg);
        
        // test for default
        // endpoint name is taken from the service.cfg file
        String endpointName = "dummyEP";
        assert(ri.getPerRequestMediaType(endpointName).equals("application/vnd.fdsn.mseed"));
        
        // Note, these tests are determined by the values in service.cfg
        ri.setPerRequestFormatType(endpointName, "xml");
        assert(ri.getPerRequestMediaType(endpointName).equals("application/xml"));
        ri.setPerRequestFormatType(endpointName, "xMl");
        assert(ri.getPerRequestMediaType(endpointName).equals("application/xml"));
        ri.setPerRequestFormatType(endpointName, "text");
        assert(ri.getPerRequestMediaType(endpointName).equals("text/plain"));
        ri.setPerRequestFormatType(endpointName, "texttree");
        assert(ri.getPerRequestMediaType(endpointName).equals("text/plain"));
        ri.setPerRequestFormatType(endpointName, "json");
        assert(ri.getPerRequestMediaType(endpointName).equals("application/json"));
    
        ri.setPerRequestFormatType(endpointName, "miniseed");
        assert(ri.getPerRequestMediaType(endpointName).equals("application/vnd.fdsn.mseed"));
        ri.setPerRequestFormatType(endpointName, "miniseed ");
        assert(ri.getPerRequestMediaType(endpointName).equals("application/vnd.fdsn.mseed"));
        ri.setPerRequestFormatType(endpointName, " Miniseed");
        assert(ri.getPerRequestMediaType(endpointName).equals("application/vnd.fdsn.mseed"));
        ri.setPerRequestFormatType(endpointName, "    minisEed ");
        assert(ri.getPerRequestMediaType(endpointName).equals("application/vnd.fdsn.mseed"));
        
        ri.setPerRequestFormatType(endpointName, "mseed");
        assert(ri.getPerRequestMediaType(endpointName).equals("application/vnd.fdsn.mseed"));
        ri.setPerRequestFormatType(endpointName, "binary");
        assert(ri.getPerRequestMediaType(endpointName).equals("application/octet-stream"));
    }

    @Test
    public void testLoadExceptionOfFormatTypes() throws Exception {
        AppConfigurator appCfg = 
              AppConfigurator_getters_Test.createTestObjAppCfg("META-INF/service.cfg");
        RequestInfo ri = new RequestInfo(appCfg);

        // endpoint name is taken from the service.cfg file
        String endpointName = "dummyEP";

        try {
            ri.setPerRequestFormatType(endpointName, null);
            fail("getting null type succeeded unexpectedly,"
                    + " should have had an Exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }

        try {
            ri.setPerRequestFormatType(endpointName, "unknown2");
            fail("getting unknown2 type succeeded unexpectedly,"
                    + " should have had an Exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }
    }
}
