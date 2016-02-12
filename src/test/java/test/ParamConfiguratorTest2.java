/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import edu.iris.wss.framework.ParamConfigurator;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 *
 * @author mike
 */
public class ParamConfiguratorTest2 {
    Set<String> setOfEndpointNames = new HashSet();
    
    private final String testEP = "endpt1";
    
    public ParamConfiguratorTest2() {
        setOfEndpointNames.add("epName1");
        setOfEndpointNames.add("epName2");
        setOfEndpointNames.add(testEP);
    }

    @Test
    public void testCreateAliasesFromFile() throws Exception {
        java.util.Properties props = new java.util.Properties();

        props.setProperty(testEP + "." + "starttime", "dat3");

        ParamConfigurator paramCfg = new ParamConfigurator(setOfEndpointNames);
        try {
            paramCfg.loadConfigurationParameters(props);
            fail();
        } catch (Exception ex) {
            // should fail here with unknow type  dat3
            assert(true);
        }
    }

    @Test
    public void testGettingDateType() throws Exception {
        java.util.Properties props = new java.util.Properties();
        java.net.URL url = ClassLoader.getSystemResource(
                "ParameterConfiguratorTest/paramConfig2.cfg");
        assertNotNull(url);

        props.load(url.openStream());

        ParamConfigurator paramCfg = new ParamConfigurator(setOfEndpointNames);
        paramCfg.loadConfigurationParameters(props);

        assert(paramCfg.getConfigParamValue(testEP, "endtime").type
              == ParamConfigurator.ConfigParam.ParamType.DATE);
    }

    @Test
    public void testGettingParams() throws Exception {
        java.util.Properties props = new java.util.Properties();
        java.net.URL url = ClassLoader.getSystemResource(
                "ParameterConfiguratorTest/param_mix1.cfg");
        assertNotNull(url);

        props.load(url.openStream());

        Set<String> setOfEndpointNames = new HashSet<>();
        setOfEndpointNames.add("intermag");
        setOfEndpointNames.add("v99/utepgm");
        setOfEndpointNames.add("sleeper");

        ParamConfigurator paramCfg = new ParamConfigurator(setOfEndpointNames);
        paramCfg.loadConfigurationParameters(props);

        //System.out.println("***************** param_mix1.cfg toString\n" + paramCfg.toString());

        assert(paramCfg.getConfigParamValue("intermag", "endtime").type
              == ParamConfigurator.ConfigParam.ParamType.DATE);
        assert(paramCfg.getConfigParamValue("v99/utepgm", "maxlatitude").type
              == ParamConfigurator.ConfigParam.ParamType.NUMBER);
        assert(paramCfg.getConfigParamValue("sleeper", "targ1").type
              == ParamConfigurator.ConfigParam.ParamType.TEXT);

        assert(paramCfg.getParamFromAlias("v99/utepgm", "mnlg").equals("minlongitude"));

        assert(paramCfg.containsParamAlias("v99/utepgm", "mnlg"));
        assert(paramCfg.containsParamAlias("v99/utepgm", "mxlg"));
        assert(!paramCfg.containsParamAlias("v99/utepgm", "other"));
    }
}