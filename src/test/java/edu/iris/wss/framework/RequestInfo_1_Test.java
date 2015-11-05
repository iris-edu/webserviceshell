/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.iris.wss.framework;

//import edu.iris.wss.framework.AppConfigurator_getters_Test;
import java.util.Properties;
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
public class RequestInfo_1_Test {

    public RequestInfo_1_Test() {
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
    public void test_getPerRequestOutputTypeKey() throws Exception {
        String endpointName = "endpnt1";
        Properties props = AppConfigurator_getters_Test
              .createInitialTestProperties(endpointName);
        AppConfigurator appCfg =  new AppConfigurator();

        String property = AppConfigurator.createEPPropertiesName(
              endpointName, AppConfigurator.EP_CFGS.outputTypes);

        String textMediaType = "text/plain";
        String jsonMediaType = "application/json";
        String xmlMediaType = "application/xml";

        props.setProperty(property, "text: " + textMediaType + ", "
              + "json: " + jsonMediaType + ", "
              + "IAGA2002: text/plain, xml: " + xmlMediaType + "");
        try {
            appCfg.loadConfigurationParameters(props);
        } catch (Exception ex) {
            // ignore handler exception for this test
            if (ex.toString().contains("Handler error for endpoint")) {
                // noop
            } else {
                fail("Unexpected failure in test setup, this is not a test, ex: "
                      + ex);
            }
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

    @Test
    public void testMiniseedAsDefaultOutputType() throws Exception {
        AppConfigurator appCfg = 
              AppConfigurator_getters_Test.createTestObjAppCfg(
                    "AppConfiguratorTest/serviceFile2.cfg");
        RequestInfo ri = new RequestInfo(appCfg);

        // before setting any request format, the new default should be the first
        // item in the properties list
        assert(ri.getPerRequestMediaType("querysf2").equals("application/vnd.fdsn.mseed"));

        // the default type should still be available
        ri.setPerRequestOutputType("querysf2", "binary");
        assert(ri.getPerRequestMediaType("querysf2").equals("application/octet-stream"));

        // no other types should be found
        try {
            ri.setPerRequestOutputType("querysf2", "text");
            fail("getting text type succeeded unexpectedly,"
                    + " should have had an Exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }
    }
}
