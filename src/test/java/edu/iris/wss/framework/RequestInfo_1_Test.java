/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.iris.wss.framework;

import static edu.iris.wss.framework.AppConfigurator_getters_Test.createTestObjs;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 *
 * @author mike
 */
public class RequestInfo_1_Test {
//    private static final AppConfigurator thisAppCfg = new AppConfigurator();

    public RequestInfo_1_Test() {
    }
        
    
    @BeforeClass
    public static void setUpClass() {
//        System.out.println("***************** setupclass1");
//        java.util.Properties props = new java.util.Properties();
//
//        // add required cfg items
//        props.setProperty(
//              AppConfigurator.GL_CFGS.appName.toString(), "mock_appname");
//        props.setProperty(
//              AppConfigurator.GL_CFGS.appVersion.toString(), "mock_version");
//
//        
//        props.setProperty("endpnt1.outputTypes", "text: text/plain,"
//              + " IAGA2002: text/plain, xml: application/xml");
//        try {
//            thisAppCfg.loadConfigurationParameters(props, null);
//        } catch (Exception ex) {
//            Logger.getLogger(RequestInfo_1_Test.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        System.out.println("******* ** ** toString\n" + thisAppCfg.toString());
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        System.out.println("***************** setup1");
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void test_getPerRequestOutputTypeKey() throws Exception {
        Object[] items = createTestObjs();
        AppConfigurator appCfg = (AppConfigurator)items[0];
        Properties props = (Properties)items[1];

        String endpointName = "endpnt1";
        String property = AppConfigurator.createEPPropertiesName(
              endpointName, AppConfigurator.EP_CFGS.outputTypes);

        String textMediaType = "text/plain";
        String jsonMediaType = "application/json";
        String xmlMediaType = "application/xml";

        props.setProperty(property, "text: " + textMediaType + ", "
              + "json: " + jsonMediaType + ", "
              + "IAGA2002: text/plain, xml: " + xmlMediaType + "");
        try {
            appCfg.loadConfigurationParameters(props, null);
        } catch (Exception ex) {
            fail("Unexpected failure in test setup, this is not a test, ex: " + ex);
        }

        RequestInfo ri = new RequestInfo(appCfg);
        
        // test for default
        assert(ri.getPerRequestMediaType(endpointName).equals(textMediaType));

        try {
            assert(ri.getPerRequestMediaType("unknown_endpoint").equals(textMediaType));
            fail("Unexpected successful try, this test should have thrown an exception.");
        } catch(Exception ex) {
            // is successful
        }

        // set new default and test
        ri.setPerRequestOutputType(endpointName, "json");
        assert(ri.getPerRequestMediaType(endpointName).equals(jsonMediaType));

        try {
            ri.setPerRequestOutputType(endpointName, "unconfigured_type");
            fail("Unexpected successful try, this test should have thrown an exception.");
        } catch(Exception ex) {
            // is successful
        }

        try {
            ri.setPerRequestOutputType(endpointName, null);
            fail("Unexpected successful try, this test should have thrown an exception.");
        } catch(Exception ex) {
            // is successful
        }
    }

//    @Test
//    public void testLoadOfOutputTypes() throws Exception {
//        System.out.println("***************** testLoadOfOutputTypes");
//        RequestInfo ri = new RequestInfo(thisAppCfg);
//        
//        // test for default
//        assert(ri.getPerRequestMediaType().equals("application/vnd.fdsn.mseed"));
//        
//        // Note, these tests are determined by the values in service.cfg
//        ri.setPerRequestOutputType("xml");
//        assert(ri.getPerRequestMediaType().equals("application/xml"));
//        ri.setPerRequestOutputType("xMl");
//        assert(ri.getPerRequestMediaType().equals("application/xml"));
//        ri.setPerRequestOutputType("text");
//        assert(ri.getPerRequestMediaType().equals("text/plain"));
//        ri.setPerRequestOutputType("texttree");
//        assert(ri.getPerRequestMediaType().equals("text/plain"));
//        ri.setPerRequestOutputType("json");
//        assert(ri.getPerRequestMediaType().equals("application/json"));
//    
//        ri.setPerRequestOutputType("miniseed");
//        assert(ri.getPerRequestMediaType().equals("application/vnd.fdsn.mseed"));
//        ri.setPerRequestOutputType("miniseed ");
//        assert(ri.getPerRequestMediaType().equals("application/vnd.fdsn.mseed"));
//        ri.setPerRequestOutputType(" Miniseed");
//        assert(ri.getPerRequestMediaType().equals("application/vnd.fdsn.mseed"));
//        ri.setPerRequestOutputType("    minisEed ");
//        assert(ri.getPerRequestMediaType().equals("application/vnd.fdsn.mseed"));
//        
//        ri.setPerRequestOutputType("mseed");
//        assert(ri.getPerRequestMediaType().equals("application/vnd.fdsn.mseed"));
//        ri.setPerRequestOutputType("binary");
//        assert(ri.getPerRequestMediaType().equals("application/octet-stream"));
//
//        try {
//            ri.setPerRequestOutputType(null);
//            fail("getting null type succeeded unexpectedly,"
//                    + " should have had an Exception");
//        } catch (Exception ex) {
//            // noop - this is expected result
//        }
//    }
}
