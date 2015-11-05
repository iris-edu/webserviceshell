/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.iris.wss.framework;

import java.io.File;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 *
 * @author mike
 */
public class AppConfig_2x_1Test {
    public AppConfig_2x_1Test() {
    }
    private final String slpr = "sleeper";
    private final String v99 = "v99/utepgm";
    private final String itmg = "intermag";

    private final String testFileName = "AppConfiguratorTest/service_mix1.cfg";

    @Test
    public void testAppConfigLoad() throws Exception {
        //System.out.println("-------------------- user.dir: " + System.getProperty("user.dir"));
        java.util.Properties props = new java.util.Properties();
        java.net.URL url = ClassLoader.getSystemResource(testFileName);
        assertNotNull(url);

        props.load(url.openStream());

        AppConfigurator appCfg = new AppConfigurator();
        try {
            appCfg.loadConfigurationParameters(props);
        } catch (Exception ex) {
            if (ex.toString().contains("Handler error for endpoint")) {
                // noop - accept this error since handler name check is an
                //        absolute check and not part of this test
            } else {
                throw new Exception(
                      "Test may be in error, rethrow of unexpected exception",
                      ex);
            }
        }
        
        //System.out.println("******* ** ** toString\n" + appCfg.toString());

        // match output to values in cfg file
        assert(appCfg.isUsageLogEnabled(v99) == true);
        assert(appCfg.isUsageLogEnabled(itmg) == true);

        assert(appCfg.isPostEnabled(v99) == true);
        assert(appCfg.isPostEnabled(itmg) == false);

        assert(appCfg.isUse404For204Enabled(v99) == false);
        assert(appCfg.isUse404For204Enabled(itmg) == true);
    }

    @Test
    public void testDefaultType() throws Exception {
        java.util.Properties props = new java.util.Properties();
        java.net.URL url = ClassLoader.getSystemResource(testFileName);
        assertNotNull(url);
        
        props.load(url.openStream());        
        
        AppConfigurator appCfg = new AppConfigurator();
        try {
            appCfg.loadConfigurationParameters(props);
        } catch (Exception ex) {
            if (ex.toString().contains("Handler error for endpoint")) {
                // noop - accept this error since handler name check is an
                //        absolute check and not part of this test
            } else {
                throw new Exception(
                      "Test may be in error, rethrow of unexpected exception",
                      ex);
            }
        }
        
        // match output to values in cfg file
        assert(appCfg.getDefaultOutputTypeKey(slpr).equals("BINARY"));
        assert(appCfg.getDefaultOutputTypeKey(v99).equals("TEXT"));
        assert(appCfg.getDefaultOutputTypeKey(itmg).equals("JSON"));
    }

    @Test
    public void testGlobals() throws Exception {
        java.util.Properties props = new java.util.Properties();
        java.net.URL url = ClassLoader.getSystemResource(testFileName);
        assertNotNull(url);

        props.load(url.openStream());

        AppConfigurator appCfg = new AppConfigurator();
        try {
            appCfg.loadConfigurationParameters(props);
        } catch (Exception ex) {
            if (ex.toString().contains("Handler error for endpoint")) {
                // noop - accept this error since handler name check is an
                //        absolute check and not part of this test
            } else {
                throw new Exception(
                      "Test may be in error, rethrow of unexpected exception",
                      ex);
            }
        }

        // match output to values in cfg file
        assert(appCfg.getAppName().equals("services-mix1"));
        assert(appCfg.getAppVersion().equals("0.5.0"));
        assert(appCfg.isCorsEnabled() == true);
        assert(appCfg.getRootServiceDoc().equals(
              "file:///earthcube/tomcat-8091-7.0.56/wss_config/intermagnet-2-swaggerindex.html"));
        assert(appCfg.getLoggingType().equals(AppConfigurator.LoggingMethod.LOG4J));
        assert(appCfg.getSigkillDelay() == 123);
        assert(appCfg.getSingletonClassName() == null);
    }

    @Test
    public void testsigkillDelayExceptionsInGlobals() throws Exception {
        java.util.Properties props = new java.util.Properties();
        props.put(AppConfigurator.GL_CFGS.sigkillDelay.toString(), "abc");

        AppConfigurator appCfg = new AppConfigurator();
        try {
            appCfg.loadConfigurationParameters(props);
            fail();
        } catch (Exception ex) {
            //  noop - should throw exception
        }
    }

    @Test
    public void testloggingMethodExceptionsInGlobals() throws Exception {
        java.util.Properties props = new java.util.Properties();
        props.put(AppConfigurator.GL_CFGS.loggingMethod.toString(), "abc");
        
        AppConfigurator appCfg = new AppConfigurator();
        try {
            appCfg.loadConfigurationParameters(props);
            fail();
        } catch (Exception ex) {
            //  noop - should throw exception
        }
    }

    @Test
    public void testHandlerFileExistException() throws Exception {
        // when an endpoint defines the IRIS class for command processing
        // the loader expects to find a valid handler file
        java.util.Properties props = new java.util.Properties();
        props.put("testEP." + AppConfigurator.EP_CFGS.irisEndpointClassName.toString(),
              "edu.iris.wss.endpoints.CmdProcessorIrisEP");
        props.put("testEP." + AppConfigurator.EP_CFGS.handlerProgram.toString(),
              "randomname123abc");

        AppConfigurator appCfg = new AppConfigurator();
        try {
            appCfg.loadConfigurationParameters(props);
            fail();
        } catch (Exception ex) {
            //  noop - should throw exception
        }
    }

    @Test
    public void testHandlerFileExistsAndExecuteException() throws Exception {
        // setup files
        String endPath = "target/test-classes/AppConfiguratorTest";
        File folder = new File(endPath);
        if(!folder.exists()){
            folder.mkdirs();
        }
        File file1 = File.createTempFile("hand", ".tmp", folder);

       java.util.Properties props = new java.util.Properties();
        props.put("testEP." + AppConfigurator.EP_CFGS.irisEndpointClassName.toString(),
              "edu.iris.wss.endpoints.CmdProcessorIrisEP");
        props.put("testEP." + AppConfigurator.EP_CFGS.handlerProgram.toString(),
              file1.getPath());

        AppConfigurator appCfg = new AppConfigurator();
        try {
            appCfg.loadConfigurationParameters(props);
            fail();
        } catch (Exception ex) {
            assert(ex.toString().contains("is not executable"));
            //  noop - should throw exception
        }

        file1.setExecutable(true);
        appCfg.loadConfigurationParameters(props);
    }

    @Test
    public void testSingletonClassName() throws Exception {
        java.util.Properties props = new java.util.Properties();
        props.put(AppConfigurator.GL_CFGS.singletonClassName.toString(),
              "edu.iris.wss.provider.TestSingleton");

        AppConfigurator appCfg = new AppConfigurator();
        appCfg.loadConfigurationParameters(props);

        props.put(AppConfigurator.GL_CFGS.singletonClassName.toString(),
              "some_singleton_notvalid_name");
        try {
            appCfg.loadConfigurationParameters(props);
            fail();
        } catch (Exception ex) {
            assert(ex.toString().contains("could not find"));
            //  noop - should throw exception
        }
    }
}
