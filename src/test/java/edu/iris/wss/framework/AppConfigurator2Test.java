/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.iris.wss.framework;

import java.util.Map;
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
public class AppConfigurator2Test {
    AppConfigurator thisAppCfg = new AppConfigurator();
    
    public AppConfigurator2Test() {
    }
        
    @Test
    public void testMiniseedAsDefaultOutputType() throws Exception {
        java.util.Properties props = new java.util.Properties();
        // note: expecting serviceFile2.cfg to have one pair which is not binary
        java.net.URL url = ClassLoader.getSystemResource(
                "AppConfiguratorTest/serviceFile2.cfg");
        assertNotNull(url);
        
        props.load(url.openStream());

        Map<String, String> map = thisAppCfg.createOutputTypes("");
            thisAppCfg.setOutputTypes(map, (String)props.get("outputTypes"));
        
        RequestInfo ri = new RequestInfo(thisAppCfg);
        // before setting any request format, the new default should be the first
        // item in the properties list
        assert(ri.getPerRequestMediaType().equals("application/vnd.fdsn.mseed"));

        // the default type should still be available
        ri.setPerRequestOutputType("binary");
        assert(ri.getPerRequestMediaType().equals("application/octet-stream"));

        // no other types should be found
        try {
            ri.setPerRequestOutputType("text");
            fail("getting text type succeeded unexpectedly,"
                    + " should have had an Exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }
    }
}
