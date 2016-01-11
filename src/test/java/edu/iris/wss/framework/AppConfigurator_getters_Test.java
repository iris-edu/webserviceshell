/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.iris.wss.framework;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 *
 * @author mike
 */
public class AppConfigurator_getters_Test {
    public static final String EXECUTABLE_FILE_FOR_TEST = 
          "src/test/resources/ServiceConfigTest/sleep_handle2.sh";

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
    public static AppConfigurator createTestObjAppCfg(String propFileName) throws Exception {
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
            appCfg.loadConfigurationParameters(props);
        } catch (Exception ex) {
            // ignore handler exception for this test
            if (ex.toString().contains("Handler error for endpoint")) {
                // noop
            } else {
                fail("Unexpected failure in test setup while trying to load file: "
                      + propFileName + "  from app, ex: " + ex);
            }
        }

        return appCfg;
    }

    public static Properties createInitialTestProperties(String epName) {
        // setup a simple configuration for testing
        java.util.Properties props = new java.util.Properties();

        // add required cfg items
        props.setProperty(
              AppConfigurator.GL_CFGS.appName.toString(), "mock_appname");
        props.setProperty(
              AppConfigurator.GL_CFGS.version.toString(), "mock_version");

        // handlerProgram is required, but not part of this test
        String property = AppConfigurator.createEPPropertiesName(
              epName, AppConfigurator.EP_CFGS.handlerProgram);
        props.setProperty(property, EXECUTABLE_FILE_FOR_TEST);

        return props;
    }

    @Test
    public void test_getMediaType() throws Exception {
        String endpointName = "endpnt1";
        Properties props = createInitialTestProperties(endpointName);
        AppConfigurator appCfg = new AppConfigurator();

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
        String endpointName = "endpnt2";
        Properties props = createInitialTestProperties(endpointName);
        AppConfigurator appCfg = new AppConfigurator();
    
        // set a property that is not being tested to cause a build of an endpoint
        String property = AppConfigurator.createEPPropertiesName(
              endpointName, AppConfigurator.EP_CFGS.usageLog);
        props.setProperty(property, "true");
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

        // start tests
        // test for default of default output type, it should be binary. 
        assert(appCfg.getDefaultOutputTypeKey(endpointName).equals("BINARY"));

        // *********************
        // change outputTypes and test again
        props = createInitialTestProperties(endpointName);

        String textMediaType = "text/plain";
        String jsonMediaType = "application/json";
        String xmlMediaType = "application/xml";

        property = AppConfigurator.createEPPropertiesName(
              endpointName, AppConfigurator.EP_CFGS.outputTypes);
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

        // test for default output type, should be first one in list
        assert(appCfg.getDefaultOutputTypeKey(endpointName).equals("TEXT"));

        try {
            assert(appCfg.getDefaultOutputTypeKey("unknown_endpoint").equals("TEXT"));
            fail("Unexpected successful try, this test should have thrown an exception.");
        } catch(Exception ex) {
            // is successful
        }
    }

    @Test
    public void test_ProxyResourceConfiguration() throws Exception {
        String endpointName = "endpntProxy";
        Properties props = createInitialTestProperties(endpointName);
        AppConfigurator appCfg = new AppConfigurator();

        // setup up proxy resourse for JSON and text
        
        // first the respective class name
        String property = AppConfigurator.createEPPropertiesName(endpointName, AppConfigurator.EP_CFGS.endpointClassName);
        props.setProperty(property, edu.iris.wss.endpoints.ProxyResource.class.getName());

        try {
            appCfg.loadConfigurationParameters(props);
            fail("Unexpected success, proxyURL should be a required property here.");
        } catch(Exception ex) {
            // successful test,
            // should throw exception if propxyURL property is not defined
        }
        
        // add the proxyURL property
        property = AppConfigurator.createEPPropertiesName(
              endpointName, AppConfigurator.EP_CFGS.proxyURL);
        props.setProperty(property, "file:///someURL");
        
        // load this version of properties
        appCfg = new AppConfigurator();
        try {
            appCfg.loadConfigurationParameters(props);
            fail("Unexpected success, this load should fail with file not found.");
        } catch(Exception ex) {
            assert(ex.toString().contains("java.io.FileNotFoundException"));
        }

        // use any file to test with
        String endPath = "src"
              + File.separator + "test"
              + File.separator + "resources"
              + File.separator + "AppConfiguratorTest"
              + File.separator + "service_mix1.cfg";
        String currentDir = System.getProperty("user.dir");
        String testURL = "file://" + currentDir + File.separator + endPath;
        
        // create URL with this name and reset property
        props.setProperty(property, testURL);
        
        // this version should load ok
        appCfg = new AppConfigurator();
        appCfg.loadConfigurationParameters(props);

        // the default media type should be explicitly set
        // update output types property
        String jsonMediaType = "application/json";
        property = AppConfigurator.createEPPropertiesName(
              endpointName, AppConfigurator.EP_CFGS.outputTypes);
        props.setProperty(property, "json: " + jsonMediaType
              + ", "+ "text: text/plain");
        
        // this version should load ok
        appCfg = new AppConfigurator();
        appCfg.loadConfigurationParameters(props);

        assert(appCfg.getMediaType(endpointName, "json").equals(jsonMediaType));
    }
}
