/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.iris.wss.framework;

import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author mike
 */
public class AppConfiguratorSetOutputTypesTest {
    AppConfigurator thisAppCfg = new AppConfigurator();

    public AppConfiguratorSetOutputTypesTest() {
    }

    @Test
    public void testForMSeExceptionInOutputTypesSetter() throws Exception {
        try {
            thisAppCfg.setOutputTypes("miniseed");
            fail("no colon try succeeded unexpectedly,"
                    + " should have had an Exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }
    }
    
    @Test
    public void testForZeroLengthExceptionInOutputTypesSetter() throws Exception {
        try {
            thisAppCfg.setOutputTypes("");
            fail("zero length try succeeded unexpectedly,"
                    + " should have had an Exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }
    }
    
    @Test
    public void testForNullExceptionInOutputTypesSetter() throws Exception {
        try {
            thisAppCfg.setOutputTypes(null);
            fail("null try succeeded unexpectedly, should have had an Exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }
    }
    
    @Test
    public void testForMissingCommasExceptionInOutputTypesSetter() throws Exception {
        // no commas should throw exception
        String outputTypes = "MINISEED: application/vnd.fdsn.mseed"
                + " BINARY: application/octet-stream"
                + " TEXT: text/plain";
        try {
            thisAppCfg.setOutputTypes(outputTypes);
            fail("comma try succeeded unexpectedly, should have had an Exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }
    }
    
    @Test
    public void testForNoColonExceptionInInOutputTypesSetter() throws Exception {
        // no colons should throw exception
        String outputTypes = "MINISEED: application/vnd.fdsn.mseed,"
                + " BINARY| application/octet-stream,"
                + " TEXT: text/plain";
        try {
            thisAppCfg.setOutputTypes(outputTypes);
            fail("colon try succeeded unexpectedly, should have had an Exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }
    }
}
