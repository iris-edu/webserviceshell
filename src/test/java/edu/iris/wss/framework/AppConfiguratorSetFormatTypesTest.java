/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.iris.wss.framework;

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
            thisAppCfg.setFormatTypes(map, "miniseed");
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
            thisAppCfg.setFormatTypes(map, "");
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
            thisAppCfg.setFormatTypes(map, null);
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
            thisAppCfg.setFormatTypes(map, formatTypes);
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
            thisAppCfg.setFormatTypes(map, formatTypes);
            fail("colon try succeeded unexpectedly, should have had an Exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }
    }
}
