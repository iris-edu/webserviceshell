/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.iris.wss.framework;

import java.io.IOException;
import java.util.Properties;
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
public class AppConfigurator_getters_Test {

    public AppConfigurator_getters_Test() {
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

    /**
     * 
     * @param propFileName - name of file with WSS service configuration parameters
     * @return 
     */
    public static AppConfigurator createTestObjAppCfg(String propFileName) {
        AppConfigurator appCfg = new AppConfigurator();
        
        java.util.Properties props = new java.util.Properties();

        java.net.URL url = ClassLoader.getSystemResource(propFileName);
        assertNotNull(url);
        
        try {
            props.load(url.openStream());
        } catch (IOException ex) {
           fail("file name misspelled, does not exist, or is not in classpath,"
                   + "  filename: " + propFileName);
        }

        try {
            appCfg.loadConfigurationParameters(props, null);
        } catch (Exception ex) {
            // ignore handler exception for this test
            if (ex.toString().contains("Handler error for endpoint")) {
                // noop
            } else {
                fail("Unexpected failure in test setup while trying to load file: "
                      + propFileName + "  ex: " + ex);
            }
        }

        return appCfg;
    }

    public static Object[] createTestObjs() {
        AppConfigurator appCfg = new AppConfigurator();

        // setup a simple configuration for testing
        java.util.Properties props = new java.util.Properties();

        // add required cfg items
        props.setProperty(
              AppConfigurator.GL_CFGS.appName.toString(), "mock_appname");
        props.setProperty(
              AppConfigurator.GL_CFGS.appVersion.toString(), "mock_version");

        Object[] returnObjs = {appCfg, props};
        return returnObjs;
    }

    @Test
    public void test_getMediaType() throws Exception {
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
            // ignore handler exception for this test
            if (ex.toString().contains("Handler error for endpoint")) {
                // noop
            } else {
                fail("Unexpected failure in test setup, this is not a test, ex: "
                      + ex);
            }
        }

        // test for known media types
        assert(appCfg.getMediaType(endpointName, "text").equals(textMediaType));
        assert(appCfg.getMediaType(endpointName, "xml").equals(xmlMediaType));
        assert(appCfg.getMediaType(endpointName, "json").equals(jsonMediaType));

        try {
            assert(appCfg.getMediaType("unknown_endpoint", "json").equals(jsonMediaType));
            fail("Unexpected successful try, this test should have thrown an exception.");
        } catch(Exception ex) {
            // is successful
        }

        try {
            assert(appCfg.getMediaType(endpointName, "unknown_type_key").equals(jsonMediaType));
            fail("Unexpected successful try, this test should have thrown an exception.");
        } catch(Exception ex) {
            // is successful
        }
    }
    
    @Test
    public void test_getDefaultOutputTypeKey() throws Exception {
        Object[] items = createTestObjs();
        AppConfigurator appCfg = (AppConfigurator)items[0];
        Properties props = (Properties)items[1];
        
        String endpointName = "endpnt2";
        
        // set a property that is not being tested to cause a build of an endpoint
        String property = AppConfigurator.createEPPropertiesName(
              endpointName, AppConfigurator.EP_CFGS.usageLog);
        props.setProperty(property, "true");
        try {
            appCfg.loadConfigurationParameters(props, null);
        } catch (Exception ex) {
            // ignore handler exception for this test
            if (ex.toString().contains("Handler error for endpoint")) {
                // noop
            } else {
                fail("Unexpected failure in test setup, this is not a test, ex: "
                      + ex);
            }
        }

        // start tests
        // test for default of default output type, it should be binary. 
        assert(appCfg.getDefaultOutputTypeKey(endpointName).equals("BINARY"));

        // *********************
        // change outputTypes and test again
        items = createTestObjs();
        appCfg = (AppConfigurator)items[0];
        props = (Properties)items[1];

        String textMediaType = "text/plain";
        String jsonMediaType = "application/json";
        String xmlMediaType = "application/xml";

        property = AppConfigurator.createEPPropertiesName(
              endpointName, AppConfigurator.EP_CFGS.outputTypes);
        props.setProperty(property, "text: " + textMediaType + ", "
              + "json: " + jsonMediaType + ", "
              + "IAGA2002: text/plain, xml: " + xmlMediaType + "");
        
        try {
            appCfg.loadConfigurationParameters(props, null);
        } catch (Exception ex) {
            // ignore handler exception for this test
            if (ex.toString().contains("Handler error for endpoint")) {
                // noop
            } else {
                fail("Unexpected failure in test setup, this is not a test, ex: "
                      + ex);
            }
        }

        // test for default output type, should be first one in list
        assert(appCfg.getDefaultOutputTypeKey(endpointName).equals("TEXT"));

        try {
            assert(appCfg.getDefaultOutputTypeKey("unknown_endpoint").equals("TEXT"));
            fail("Unexpected successful try, this test should have thrown an exception.");
        } catch(Exception ex) {
            // is successful
        }
    }
}
