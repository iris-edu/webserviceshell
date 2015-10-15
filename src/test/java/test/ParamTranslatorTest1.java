/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import edu.iris.wss.framework.ParamConfigurator;
import edu.iris.wss.framework.ParameterTranslator;
import edu.iris.wss.framework.RequestInfo;
import edu.iris.wss.framework.SingletonWrapper;
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
