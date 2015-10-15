/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
        
        System.out.println("********\n aliases" + aliases);
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