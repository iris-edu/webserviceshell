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
            thisAppCfg.setKeyValueMap(map, "miniseed", EP_CFGS.formatTypes.name(),
                  true);
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
            thisAppCfg.setKeyValueMap(map, "", EP_CFGS.formatTypes.name(), true);
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
            thisAppCfg.setKeyValueMap(map, null, EP_CFGS.formatTypes.name(),
                  true);
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
            thisAppCfg.setKeyValueMap(map, formatTypes, EP_CFGS.formatTypes.name(),
                  true);
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
            thisAppCfg.setKeyValueMap(map, formatTypes, EP_CFGS.formatTypes.name(),
                  true);
            fail("colon try succeeded unexpectedly, should have had an Exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }
    }
}
