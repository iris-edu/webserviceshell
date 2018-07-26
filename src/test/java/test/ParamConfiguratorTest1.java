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
import java.util.Map;
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
public class ParamConfiguratorTest1 {
    Set<String> testEpNames = new HashSet();

    ParamConfigurator thisParamCfg = null;

    public ParamConfiguratorTest1() {
        testEpNames.add("epName1");
        testEpNames.add("epName2");

        thisParamCfg = new ParamConfigurator(testEpNames);
    }

    @Test
    public void testCreateAliasesFromFile() throws Exception {
        java.util.Properties props = new java.util.Properties();
        // note: expecting serviceFile2.cfg to have one pair which is not binary
        java.net.URL url = ClassLoader.getSystemResource(
                "ParameterConfiguratorTest/paramConfig2.cfg");
        assertNotNull(url);

        props.load(url.openStream());

        Map<String, String> aliases =
                thisParamCfg.createAliasesMap((String)props.get("endpt1.aliases"));
    }

    @Test
    public void testManyOpenPerenthesis() throws Exception {
        String str =  "station: (sta , (sta2, st3), network: net, location: loc";

        try {
            Map<String, String> aliases = thisParamCfg.createAliasesMap(str);
            fail("succeeded unexpectedly,"
                    +" should have too many open perenthesis exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }
    }

    @Test
    public void testManyClosedPerenthesis() throws Exception {
        String str =  "station: (sta , sta2, st3)), network: net, location: loc";

        try {
            Map<String, String> aliases = thisParamCfg.createAliasesMap(str);
            fail("succeeded unexpectedly,"
                    +" should have too many closed perenthesis exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }
    }

    @Test
    public void testUnbalancedPerenthesis() throws Exception {
        String str =  "station: (sta , sta2, st3), network: (net, location: loc";

        try {
            Map<String, String> aliases = thisParamCfg.createAliasesMap(str);
            fail("succeeded unexpectedly,"
                    +" should have unbalanced perenthesis exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }
    }

    @Test
    public void testMissingColon() throws Exception {
        String str =  "station: (sta , sta2, st3), network net, location: loc";

        try {
            Map<String, String> aliases = thisParamCfg.createAliasesMap(str);
            fail("succeeded unexpectedly,"
                    +" should have unexpected items exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }
    }
}