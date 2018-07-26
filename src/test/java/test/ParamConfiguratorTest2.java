/*******************************************************************************
 * Copyright (c) 2018 IRIS DMC supported by the National Science Foundation.
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