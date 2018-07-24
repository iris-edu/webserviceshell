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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author mike
 */
public class WssSingletonHelper {
    public static enum TEST_TYPE {
		CONFIG_FILE, CONFIG_URL, CONFIG_BOGUS_URL, BRIEF_MSG_IS_NULL,
        DETAILED_MSG_IS_NULL
	};

   public static void setupCfgFiles(String wssConfigDir, String wsContext,
          TEST_TYPE nameTypes) throws Exception {

        String targetPath = System.getProperty(wssConfigDir);

        if (nameTypes.equals(TEST_TYPE.CONFIG_FILE)) {
            // noop - leave name as local system name, should resolve to
            // a real file
        } else if (nameTypes.equals(TEST_TYPE.BRIEF_MSG_IS_NULL)) {
            // noop - leave name as local system name, should resolve to
            // a real file
        } else if (nameTypes.equals(TEST_TYPE.DETAILED_MSG_IS_NULL)) {
            // noop - leave name as local system name, should resolve to
            // a real file
        }

        String cfgName = Util.createCfgFileName(wsContext,
              AppConfigurator.SERVICE_CFG_NAME_SUFFIX);

        createServiceCfgFile(targetPath, cfgName);

        cfgName = Util.createCfgFileName(wsContext,
              ParamConfigurator.PARAM_CFG_NAME_SUFFIX);
        createParamCfgFile(targetPath, cfgName);
    }

    private static void doFilePrep(String filePath, String targetName)
          throws FileNotFoundException, IOException {

        File testFile = new File(targetName);
        if (testFile.exists()) {
            testFile.delete();
        }

        File dirs = new File(filePath);
        if(!dirs.exists()){
            dirs.mkdirs();
        }
    }

    // create a config CONFIG_FILE to test against on a target test path
    private static void createServiceCfgFile(String filePath, String targetName)
          throws FileNotFoundException, IOException {

        doFilePrep(filePath, targetName);

        File testFile = new File(targetName);
        OutputStream os = new FileOutputStream(testFile);

        StringBuilder sb = new StringBuilder();

        sb.append("# ---------------- globals").append("\n");
        sb.append("\n");
        sb.append("appName=WssSingleton-logging-test").append("\n");
        sb.append("version=default-0.1").append("\n");
        sb.append("\n");
        sb.append("# LOG4J or JMS").append("\n");
        sb.append("loggingMethod=LOG4J").append("\n");

        // dont' need for now, arbitrary setting
        sb.append("rootServiceDoc=").append("dummyDocString").append("\n");

        sb.append("\n");
        sb.append("# If present, an instance of the singleton class will be created at application start").append("\n");
        sb.append("singletonClassName=edu.iris.wss.framework.UnitTestSingleton").append("\n");
        sb.append("# ----------------  endpoints").append("\n");
        sb.append("\n");
        sb.append("test_logging.endpointClassName=edu.iris.wss.endpoints.LoggingEndpoint").append("\n");

        sb.append("test_logging.formatTypes = \\").append("\n");
        sb.append("    text: text/plain,\\").append("\n");
        sb.append("    json: application/json, \\").append("\n");
        sb.append("    miniseed: application/vnd.fdsn.mseed, \\").append("\n");
        sb.append("    geocsv: text/plain").append("\n");
        sb.append("\n");

        os.write(sb.toString().getBytes());
    }

    // create a config CONFIG_FILE to test against on a target test path
    private static void createParamCfgFile(String filePath, String targetName)
          throws FileNotFoundException, IOException {

        doFilePrep(filePath, targetName);

        File testFile = new File(targetName);
        OutputStream os = new FileOutputStream(testFile);

        StringBuilder sb = new StringBuilder();

        sb.append("# ----------------  endpoints").append("\n");
        sb.append("\n");

        sb.append("# ---------------- ").append("\n");
        sb.append("\n");
        sb.append("test_logging.messageType=TEXT").append("\n");

        sb.append("test_logging.format=TEXT").append("\n");
        sb.append("test_logging.overrideDisp=BOOLEAN").append("\n");

        os.write(sb.toString().getBytes());
    }
}
