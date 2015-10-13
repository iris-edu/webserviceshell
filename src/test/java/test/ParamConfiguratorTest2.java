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

/**
 *
 * @author mike
 */
public class ParamConfiguratorTest2 {
    Set<String> testEpNames = new HashSet();
    
    ParamConfigurator thisParamCfg = null;
    private final String testEP = "endpt1";
    
    public ParamConfiguratorTest2() {
        testEpNames.add("epName1");
        testEpNames.add("epName2");
        testEpNames.add(testEP);

        thisParamCfg = new ParamConfigurator(testEpNames);
    }

    @Test
    public void testCreateAliasesFromFile() throws Exception {
        java.util.Properties props = new java.util.Properties();

        props.setProperty(testEP + "." + "starttime", "dat3");

        ParamConfigurator paramCfg = new ParamConfigurator(testEpNames);
        try {
            paramCfg.loadConfigurationParameters(props);
            fail();
        } catch (Exception ex) {
            // should fail here with unknow type like dat3
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

        thisParamCfg.loadConfigurationParameters(props);

        assert(thisParamCfg.getConfigParamValue(testEP, "endtime").type
              == ParamConfigurator.ConfigParam.ParamType.DATE);
    }
}