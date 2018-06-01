/*******************************************************************************
 * Copyright (c) 2017 IRIS DMC supported by the National Science Foundation.
 *
 * This file is part of the Web Service Shell (WSS).
 *
 * The WSS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * The WSS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * A copy of the GNU Lesser General Public License is available at
 * <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package edu.iris.wss.framework;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.BeforeClass;
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

    @BeforeClass
    public static void setUpClass() throws IOException {
        // setup config folder for this test
        System.setProperty(Util.WSS_OS_CONFIG_DIR,
            "target"
              + File.separator + "test-classes"
              + File.separator + "AppConfig_2x_1Test");
    }

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
        assert(appCfg.getDefaultFormatTypeKey(slpr).equals("BINARY"));
        assert(appCfg.getDefaultFormatTypeKey(v99).equals("TEXT"));
        assert(appCfg.getDefaultFormatTypeKey(itmg).equals("JSON"));
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

    // changed 2016-02-11 when V1CmdProcessor was removed, may reinstate
    // if config load is changed to know when and endpoint does not
    // require a command line handler
    @Test
    public void testHandlerFileExistException() throws Exception {
        // when an endpoint defines the IRIS class for command processing
        // the loader expects to find a valid handler file
        java.util.Properties props = new java.util.Properties();
        props.put("testEP." + AppConfigurator.EP_CFGS.endpointClassName.toString(),
              "edu.iris.wss.endpoints.CmdProcessor");
        props.put("testEP." + AppConfigurator.EP_CFGS.handlerProgram.toString(),
              "randomname123abc");

        AppConfigurator appCfg = new AppConfigurator();
        try {
            appCfg.loadConfigurationParameters(props);
            // changed 2016-02-11, too restrictive - fail();
        } catch (Exception ex) {
            ex.printStackTrace();
            fail();
            //  noop - should throw exception
        }
    }

    // changed 2016-02-11 when V1CmdProcessor was removed, may reinstate
    // if config load is changed to know when and endpoint does not
    // require a command line handler
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
        props.put("testEP." + AppConfigurator.EP_CFGS.endpointClassName.toString(),
              "edu.iris.wss.endpoints.CmdProcessor");
        props.put("testEP." + AppConfigurator.EP_CFGS.handlerProgram.toString(),
              file1.getPath());

        AppConfigurator appCfg = new AppConfigurator();
        try {
            appCfg.loadConfigurationParameters(props);
            // changed 2016-02-11, too restrictive - fail();
        } catch (Exception ex) {
            assert(ex.toString().contains("is not executable"));
            ex.printStackTrace();
            fail();
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

        // test singleton should load without exception
        appCfg.getIrisSingletonInstance(appCfg.getSingletonClassName());

        props.put(AppConfigurator.GL_CFGS.singletonClassName.toString(),
              "some_singleton_notvalid_name");
        try {
            appCfg.loadConfigurationParameters(props);
            appCfg.getIrisSingletonInstance(
                  appCfg.getSingletonClassName());
            fail("Unexpectedly succeeded");
        } catch (Exception ex) {
            assert(ex.toString().contains("could not find"));
            //  noop - should throw exception
        }
    }

    @Test
    public void testNoSingletonClassName() throws Exception {

        // setup a simple service cfg file where singletonClassName
        // is not set
        String service_cfg_name = "NoSingletonClassName";

        StringBuilder sb = new StringBuilder();

        sb.append("# ---------------- globals").append("\n");
        sb.append("\n");
        sb.append("appName=").append(service_cfg_name).append("\n");
        sb.append("version=test-0.123").append("\n");
        sb.append("\n");

        // Note: system property Util.WSS_OS_CONFIG_DIR must be set befor now
        FileCreaterHelper.createFileInWssFolder(service_cfg_name,
             AppConfigurator.SERVICE_CFG_NAME_SUFFIX, sb.toString(), false);

        WssSingleton ws = new WssSingleton();
        // running configure will create the singleton if it is defined
        ws.configure(service_cfg_name);

        assert(null == ws.singleton);
    }

    @Test
    public void testSingletonDestroy() throws Exception {

        // setup a simple service cfg
        String service_cfg_name = "SingletonDestroy";

        StringBuilder sb = new StringBuilder();

        sb.append("# ---------------- globals").append("\n");
        sb.append("\n");
        sb.append("appName=").append(service_cfg_name).append("\n");
        sb.append("version=test-0.123").append("\n");
        sb.append("singletonClassName=edu.iris.wss.framework.UnitTestDestroySingleton").append("\n");
        sb.append("\n");

        // Note: system property Util.WSS_OS_CONFIG_DIR must be set befor now
        FileCreaterHelper.createFileInWssFolder(service_cfg_name,
             AppConfigurator.SERVICE_CFG_NAME_SUFFIX, sb.toString(), false);

        WssSingleton ws = new WssSingleton();
        // running configure will create the singleton if it is defined
        ws.configure(service_cfg_name);

        assertEquals(false, ((UnitTestDestroySingleton)ws.singleton).getIsDestroyedCalled());

        // setup a life cycle object and do shutdown
        TestMyContainerLifecycleListener tmcll = new TestMyContainerLifecycleListener();
        tmcll.forTestingSetSw(ws);
        tmcll.onShutdown(null);
        assertEquals(true, ((UnitTestDestroySingleton)ws.singleton).getIsDestroyedCalled());
    }

    @Test
    public void testAppinitFileRead() throws Exception {

        // setup a simple service cfg file
        String service_cfg_name = "SingletonAppinitFileRead";

        StringBuilder sb = new StringBuilder();

        sb.append("# ---------------- globals").append("\n");
        sb.append("\n");
        sb.append("appName=").append(service_cfg_name).append("\n");
        sb.append("version=test-0.124").append("\n");
        sb.append("singletonClassName=edu.iris.wss.framework.UnitTestDestroySingleton").append("\n");
        sb.append("\n");

        // Note: system property Util.WSS_OS_CONFIG_DIR must be set befor now
        FileCreaterHelper.createFileInWssFolder(service_cfg_name,
             AppConfigurator.SERVICE_CFG_NAME_SUFFIX, sb.toString(), false);

        sb.setLength(0);

        sb.append("# ---------------- test propertes file").append("\n");
        sb.append("\n");
        sb.append("testline1=").append("valueofline1").append("\n");
        sb.append("testline2=").append("line2value").append("\n");
        sb.append("\n");

        // Note: system property Util.WSS_OS_CONFIG_DIR must be set befor now
        FileCreaterHelper.createFileInWssFolder(service_cfg_name,
             WssSingleton.APPINIT_CFG_NAME_SUFFIX, sb.toString(), false);

        WssSingleton ws = new WssSingleton();
        // running configure will create the singleton if it is defined
        ws.configure(service_cfg_name);

        Properties prop = ((UnitTestDestroySingleton)ws.singleton).getAppinitProperties();
        assertEquals("line2value", prop.getProperty("testline2"));
    }
}
