/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.iris.wss.framework;

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
        java.util.Properties props = new java.util.Properties();
        java.net.URL url = ClassLoader.getSystemResource(testFileName);
        assertNotNull(url);

        props.load(url.openStream());

        AppConfigurator appCfg = new AppConfigurator();
        appCfg.loadConfigurationParameters(props, null);
        
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
        appCfg.loadConfigurationParameters(props, null);
        
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
        appCfg.loadConfigurationParameters(props, null);

        // match output to values in cfg file
        assert(appCfg.getAppName().equals("services-mix1"));
        assert(appCfg.getAppVersion().equals("0.5.0"));
        assert(appCfg.isCorsEnabled() == true);
        assert(appCfg.getSwaggerV2URL().equals(
              "http://geows.ds.iris.edu/geows-uf/v2/intermagnet-2-swagger.json"));
        assert(appCfg.getWadlPath() == null);
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
            appCfg.loadConfigurationParameters(props, null);
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
            appCfg.loadConfigurationParameters(props, null);
            fail();
        } catch (Exception ex) {
            //  noop - should throw exception
        }
    }
}
