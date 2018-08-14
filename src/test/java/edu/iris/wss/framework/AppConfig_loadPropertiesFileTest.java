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

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author mike
 */
public class AppConfig_loadPropertiesFileTest {
    public static final String THIS_CLASS_NAME = AppConfig_loadPropertiesFileTest.class.getSimpleName();
    public static final Logger LOGGER = Logger.getLogger(THIS_CLASS_NAME);

    private static final String MEDIA_PARAM = "myFORMTname";

    private static final String SERVICE_CONTEXT = "/loadproptest";
    private static final String ENDPOINT_NAME = "process";

    private String okFN, notokFN;

    public AppConfig_loadPropertiesFileTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        // define WSS config dir for this test
        System.setProperty(Util.WSS_OS_CONFIG_DIR,
            "target"
              + File.separator + "test-classes"
              + File.separator + THIS_CLASS_NAME);
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws Exception {
        String className = "edu.iris.wss.endpoints.DummyNameEndpoint";

        String flaw = "";
        okFN = FileCreaterHelper.createFileInWssFolder(SERVICE_CONTEXT,
              "-ok"+AppConfigurator.SERVICE_CFG_NAME_SUFFIX,
              createServiceCfgStr(ENDPOINT_NAME, className, flaw),
              false);

        // insert a hard-to-see problem after a backslash in a list
        flaw = " ";
        notokFN = FileCreaterHelper.createFileInWssFolder(SERVICE_CONTEXT,
              "-notok"+AppConfigurator.SERVICE_CFG_NAME_SUFFIX,
              createServiceCfgStr(ENDPOINT_NAME, className, flaw),
              false);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testload_withFlaw() throws Exception {

        // file read from https://www.mkyong.com/java/java-read-a-text-file-line-by-line/
        RandomAccessFile okfile = new RandomAccessFile(okFN, "r");
        FileChannel okchannel = okfile.getChannel();
        RandomAccessFile notokfile = new RandomAccessFile(notokFN, "r");
        FileChannel notokchannel = notokfile.getChannel();

        // the files should be different, the not ok file should have
        // a flaw string after a backslass
        assertEquals(true, okchannel.size() != notokchannel.size());

        okchannel.close();
        okfile.close();
        notokchannel.close();
        notokfile.close();

        Properties props_ok =
              AppConfigurator.loadPropertiesFile(SERVICE_CONTEXT.substring(1),
              this.getClass(), "-ok"+AppConfigurator.SERVICE_CFG_NAME_SUFFIX,
              AppConfigurator.DEFAULT_SERVICE_FILE_NAME);

        Properties props_notok =
              AppConfigurator.loadPropertiesFile(SERVICE_CONTEXT.substring(1),
              this.getClass(), "-notok"+AppConfigurator.SERVICE_CFG_NAME_SUFFIX,
              AppConfigurator.DEFAULT_SERVICE_FILE_NAME);

        // the properties shoud be the same after the fix is applied when the
        // properties are loaded
        assertEquals(true, props_ok.keySet().equals(props_notok.keySet()));
     }

    @Test
    public void testload_withNoBase() throws Exception {
        String testDefaultResource = "META-INF/service_withFlaw.cfg";

        InputStream inStream = this.getClass().getClassLoader()
            .getResourceAsStream(testDefaultResource);

        Properties props_notok = new Properties();
        props_notok.load(inStream);

        // this loads the default cfg when it can't find the primary
        Properties props_ok =
              AppConfigurator.loadPropertiesFile("wrongbase_force_to_default",
              this.getClass(), "-ok"+AppConfigurator.SERVICE_CFG_NAME_SUFFIX,
              testDefaultResource);

        // the properties should be different after the fixed load
        assertEquals(false, props_ok.keySet().equals(props_notok.keySet()));
     }

    private static String createServiceCfgStr(String endpointName,
          String endpointClass, String flaw) {
        String s = String.join("\n",
              "# ---------------- globals",
              "",
              "appName=" + THIS_CLASS_NAME,
              "version=0.1",
              "",
              "corsEnabled=false",
              "",
              "# LOG4J or JMS",
              "loggingMethod=LOG4J",
              "",
              "# If present, an instance of the singleton class will be created at application start",
              "singletonClassName=edu.iris.wss.framework.UnitTestDestroySingleton",
              "",
              "# ----------------  endpoints",
              "",
              endpointName + ".endpointClassName=" + endpointClass,
              endpointName + ".usageLog",
              endpointName + ".postEnabled=true",
              endpointName + ".logMiniseedExtents = false",
              endpointName + ".use404For204=false",
              endpointName + ".formatTypes = \\",
              "    text: text/plain,\\",
              "    json: application/json, \\" + flaw,
              "    miniseed: application/vnd.fdsn.mseed, \\",
              "    geocsv: text/plain",
              endpointName + ".mediaParameter = " + MEDIA_PARAM,
              ""
        );

        return s;
    }
}
