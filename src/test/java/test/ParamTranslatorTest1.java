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

package test;

import edu.iris.wss.framework.ParamConfigurator;
import edu.iris.wss.framework.ParameterTranslator;
import edu.iris.wss.framework.RequestInfo;
import edu.iris.wss.framework.WssSingleton;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 *
 * @author mike
 */
public class ParamTranslatorTest1 {

    public ParamTranslatorTest1() {
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

// TBD
//        // mock up objects
//        SingletonWrapper sw = new SingletonWrapper();
//        UriInfo uriInfo = null;
//        javax.servlet.http.HttpServletRequest request= null;
//        HttpHeaders requestHeaders = null;
//
//        RequestInfo ri = RequestInfo.createInstance(sw, uriInfo, request,
//              requestHeaders);
//
//        ParameterTranslator pt = new ParameterTranslator();
    }
}
