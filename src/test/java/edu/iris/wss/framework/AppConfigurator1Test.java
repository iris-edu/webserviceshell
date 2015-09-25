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
public class AppConfigurator1Test {
    AppConfigurator thisAppCfg = new AppConfigurator();
    
    public AppConfigurator1Test() {
    }
    
    @Test
    public void testNoOutputTypes() throws Exception {
        java.util.Properties props = new java.util.Properties();
        // note: expecting serviceFile1.cfg to have no outputTypes specified
        java.net.URL url = ClassLoader.getSystemResource(
                "AppConfiguratorTest/serviceFile1.cfg");
        assertNotNull(url);
        
        props.load(url.openStream());
        
        try {
            Map<String, String> map = thisAppCfg.createOutputTypes("");
            thisAppCfg.setOutputTypes(map, (String)props.get("outputTypes"));
            fail("should be seeing null properties setter succeeded unexpectedly,"
                    + " should have had an Exception");
        } catch(Exception ex) {
            // noop - this is expected result
        }
        
        RequestInfo ri = new RequestInfo(thisAppCfg);

        // should be the default type
        assert(ri.getPerRequestMediaType().equals("application/octet-stream"));

        try {
            ri.setPerRequestOutputType("miniseed");
            fail("getting miniseed type succeeded unexpectedly,"
                    + " should have had an Exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }
    }
}
