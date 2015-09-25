/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.iris.wss.framework;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 *
 * @author mike
 */
public class AppConfig_2x_1Test {
    AppConfigurator thisAppCfg = new AppConfigurator();
    public AppConfig_2x_1Test() {
    }
        
    @Test
    public void testAppConfigLoad() throws Exception {
        java.util.Properties props = new java.util.Properties();
        // note: expecting serviceFile2.cfg to have one pair which is not binary
        java.net.URL url = ClassLoader.getSystemResource(
                "AppConfiguratorTest/service-file-2x-1.cfg");
        assertNotNull(url);
        
        props.load(url.openStream());

        System.out.println("************* ** names: " + props.stringPropertyNames());
        
        
        thisAppCfg.loadConfigurationParameters(props, null);
        System.out.println("******* ** ** toString\n" + thisAppCfg.toString());
        System.out.println("\n\n******* toHtmlString\n" + thisAppCfg.toHtmlString());
        
//////        HashMap<String, HashMap> endpoints = new HashMap();
////        HashMap<String, Set> endpoints = new HashMap();
////        Enumeration names = props.propertyNames();
////        while (names.hasMoreElements()) {
////            String name = (String)names.nextElement();      
////            System.out.println("**** name: " + name
////                + "  value: " + props.getProperty(name));
////            
////            String[] withEPs = name.split(java.util.regex.Pattern.quote("."));
////            if (withEPs.length == 1) {
////                System.out.println("*** glb: " + withEPs[0]);
////            } else if (withEPs.length == 2) {
////                String epName = withEPs[0];
////        //        String epAttr = withEPs[1];
//////                HashMap<String, String> endpoint = null;
//////                if (endpoints.containsKey(epName)) {
//////                    endpoint = endpoints.get(epName);
//////                } else {
//////                    endpoint = new HashMap();
//////                }
//////                endpoint.put(epAttr, props.getProperty(name));
//////                endpoints.put(epName, endpoint);
////                Set<String> endpoint = null;
////        //        if (endpoints.containsKey(epName)) {
////        //            endpoint = endpoints.get(epName);
////        //        } else {
////        //            endpoint = new HashSet();
////        //        }
////        //        endpoint.add(epAttr);
////        //        endpoints.put(epName, endpoint);
////                if (endpoints.containsKey(epName)) {
////                    endpoint = endpoints.get(epName);
////                } else {
////                    endpoint = new HashSet();
////                }
////                endpoint.add(name);
////                endpoints.put(epName, endpoint);
////            } else if (withEPs.length > 2) {
////                System.out.println("*** ERR *** multiple dots not allowed, key: "
////                + name);
////            }
////        }
////        
////        System.out.println("-------------------------- epName");
////        for (String epName: endpoints.keySet()) {
////            System.out.println("******* epName: " + epName);
////        }
////        
////        System.out.println("-------------------------- all");
////        for (String epName: endpoints.keySet()) {
//////            HashMap<String, String> endpoint = endpoints.get(epName);
////            Set<String> endpoint = endpoints.get(epName);
////            Iterator<String> epAttrs = endpoint.iterator();
//////            for (String epAttr: endpoint.iterator().) {
////            while(epAttrs.hasNext()) {
////        //        String wholeName = epName + "." + epAttrs.next();
////                String wholeName = epAttrs.next();
////                System.out.println("******* wholeName: " + wholeName
////                    + "  val: " + props.getProperty(wholeName));                
////            }
////        }
    }
}
