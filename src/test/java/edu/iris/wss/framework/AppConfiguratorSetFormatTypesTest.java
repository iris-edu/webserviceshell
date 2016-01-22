/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.iris.wss.framework;

import edu.iris.wss.framework.AppConfigurator.EP_CFGS;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.fail;

/**
 *
 * @author mike
 */
public class AppConfiguratorSetFormatTypesTest {
    AppConfigurator thisAppCfg;

    public AppConfiguratorSetFormatTypesTest() throws Exception {
        this.thisAppCfg = new AppConfigurator();
    }

    @Test
    public void testForMSeExceptionInFormatTypesSetter() throws Exception {
        try {
            Map<String, String> map = thisAppCfg.createFormatTypes("");
            thisAppCfg.setKeyValueMap(map, "miniseed", EP_CFGS.formatTypes.name());
            fail("no colon try succeeded unexpectedly,"
                    + " should have had an Exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }
    }
    
    @Test
    public void testForZeroLengthExceptionInFormatTypesSetter() throws Exception {
        try {
            Map<String, String> map = thisAppCfg.createFormatTypes("");
            thisAppCfg.setKeyValueMap(map, "", EP_CFGS.formatTypes.name());
            fail("zero length try succeeded unexpectedly,"
                    + " should have had an Exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }
    }
    
    @Test
    public void testForNullExceptionInFormatTypesSetter() throws Exception {
        try {
            Map<String, String> map = thisAppCfg.createFormatTypes("");
            thisAppCfg.setKeyValueMap(map, null, EP_CFGS.formatTypes.name()
            );
            fail("null try succeeded unexpectedly, should have had an Exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }
    }
    
    @Test
    public void testForMissingCommasExceptionInFormatTypesSetter() throws Exception {
        // no commas should throw exception
        String formatTypes = "MINISEED: application/vnd.fdsn.mseed"
                + " BINARY: application/octet-stream"
                + " TEXT: text/plain";
        try {
            Map<String, String> map = thisAppCfg.createFormatTypes("");
            thisAppCfg.setKeyValueMap(map, formatTypes, EP_CFGS.formatTypes.name());
            fail("comma try succeeded unexpectedly, should have had an Exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }
    }
    
    @Test
    public void testForNoColonExceptionInInFormatTypesSetter() throws Exception {
        // no colons should throw exception
        String formatTypes = "MINISEED: application/vnd.fdsn.mseed,"
                + " BINARY| application/octet-stream,"
                + " TEXT: text/plain";
        try {
            Map<String, String> map = thisAppCfg.createFormatTypes("");
            thisAppCfg.setKeyValueMap(map, formatTypes, EP_CFGS.formatTypes.name());
            fail("colon try succeeded unexpectedly, should have had an Exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }
    }
}
