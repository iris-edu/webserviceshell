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

package edu.iris.wss.framework;

import edu.iris.wss.framework.AppConfigurator.EP_CFGS;
import java.util.Map;
import java.util.Properties;
import org.junit.Test;
import static org.junit.Assert.fail;

/**
 *
 * @author mike
 */
public class AppConfigurator_FormatDispositionsTest {
    AppConfigurator thisAppCfg;

    public static final String formatTypeNamed_text = "text";
    public static final String textDisp =
          "inline; filename=\"${appName}_${UTC}.txt\"";

    public static final String formatTypeNamed_miniseed = "miniseed";
    public static final String miniseedDisp =
          "attachment; filename=\"a_miniseed_file.mseed\"";

    public static final String appNameValue = "mock_appname";

    public AppConfigurator_FormatDispositionsTest() throws Exception {
        this.thisAppCfg = new AppConfigurator();
    }

    @Test
    public void testForNoInput() throws Exception {
        try {
            // should fail because there are not defaults, so there needs to
            // be valid input.
            Map<String, String> map = thisAppCfg.createFormatDispositions("");
        } catch (Exception ex) {
            fail("failed, Exception: " + ex);
        }
    }

    @Test
    public void testForMissingCommasException() throws Exception {
        // no commas should throw exception
        String input = "miniseed: attachment; filename=\"a_miniseed_file.mseed\""
              + "text: inline; filename=\"${appName}_${UTC}.txt\"";

        try {
            Map<String, String> map = thisAppCfg.createFormatDispositions(input);
            fail("comma try succeeded unexpectedly, should have had an Exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }
    }

    @Test
    public void test_getDisposition() throws Exception {

        String epName = "endpntA";
        Properties props = createTestInputProperties(epName);

        AppConfigurator appCfg = new AppConfigurator();
        appCfg.loadConfigurationParameters(props);

        // test for appName replacement value
        assert(appCfg.getDisposition(epName, formatTypeNamed_text)
              .contains(appNameValue));

        // test for UTC replacement value
        assert(appCfg.getDisposition(epName, formatTypeNamed_text)
              .matches(".*[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z.*"));

        // test for known static value
        assert(appCfg.getDisposition(epName, formatTypeNamed_miniseed)
              .equals(miniseedDisp));

        // test for a format type that should not be in the test set
        assert(appCfg.getDisposition(epName, "json") == null);
    }

    /**
     * Create a properties object for input to tests
     *
     * @param epName - endpoint name used to construct a set of parameters
     * @return
     */
    public static Properties createTestInputProperties(String epName) {
        // setup a simple configuration for testing
        java.util.Properties props = new java.util.Properties();

        // add required cfg items
        props.setProperty(
              AppConfigurator.GL_CFGS.appName.toString(), appNameValue);
        props.setProperty(
              AppConfigurator.GL_CFGS.version.toString(), "mock_version");

        String ep_prop_name = AppConfigurator.createEPdotPropertyName(epName,
              EP_CFGS.formatTypes);

        String textMediaType = "text/plain";
        String jsonMediaType = "application/json";
        String xmlMediaType = "application/xml";

        props.setProperty(ep_prop_name, "text: " + textMediaType + ", "
              + "json: " + jsonMediaType + ", "
              + "IAGA2002: text/plain, xml: " + xmlMediaType + "");

        ep_prop_name = AppConfigurator.createEPdotPropertyName(epName,
              EP_CFGS.formatDispositions);

        props.setProperty(ep_prop_name, formatTypeNamed_text + ": " + textDisp
              + ", " + formatTypeNamed_miniseed + ": " + miniseedDisp);

        return props;
    }
}
