/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template CONFIG_FILE, choose Tools | Templates
 * and open the template in the editor.
 */
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
		CONFIG_FILE, CONFIG_URL, CONFIG_BOGUS_URL
	};

   public static void setupCfgFiles(String wssConfigDir, String wsContext,
          TEST_TYPE nameTypes) throws Exception {

        String targetPath = System.getProperty(wssConfigDir);

        // setup Rabbit config dir for test environment
        String rabbitPropName = Util.createCfgFileName(wsContext,
              Util.RABBITMQ_CFG_NAME_SUFFIX);

        String rabbitCfgName = createIRISRabbitCfgFile(targetPath, rabbitPropName);

        if (nameTypes.equals(TEST_TYPE.CONFIG_FILE)) {
            // noop - leave name as local system name, should resolve to
            // a real file
        } else if (nameTypes.equals(TEST_TYPE.CONFIG_URL)) {
            // make a URL file name that should resolve to a real file
            rabbitCfgName = "file:" + rabbitCfgName;
        } else {
            // neither be a bogus URL name that cannot resolve to a real file
            rabbitCfgName = "file:" + "/bogus_rabbit_config_file_name";
        }

        String cfgName = Util.createCfgFileName(wsContext,
              AppConfigurator.SERVICE_CFG_NAME_SUFFIX);

        createServiceCfgFile(targetPath, cfgName, rabbitCfgName);

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
    private static void createServiceCfgFile(String filePath, String targetName,
          String rabbitFileOrURLname)
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
        sb.append("loggingMethod=RABBIT_ASYNC").append("\n");
        sb.append("loggingConfig=").append(rabbitFileOrURLname).append("\n");

        // dont' need for now, arbitrary setting
        sb.append("rootServiceDoc=").append(rabbitFileOrURLname).append("\n");

        sb.append("\n");
        sb.append("# If present, an instance of the singleton class will be created at application start").append("\n");
        sb.append("singletonClassName=edu.iris.wss.framework.TestSingleton").append("\n");
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
    // return full path of created CONFIG_FILE
    private static String createIRISRabbitCfgFile(String filePath, String targetName)
          throws FileNotFoundException, IOException {

        doFilePrep(filePath, targetName);

        File testFile = new File(targetName);
        OutputStream os = new FileOutputStream(testFile);

        StringBuilder sb = new StringBuilder();

        sb.append("# Host that's the broker. This will normally be the load balancer").append("\n");
        sb.append("broker=broker1,broker2").append("\n");
        sb.append("\n");
        sb.append("# The virtual host within the broker").append("\n");
        sb.append("virtualhost=test").append("\n");
        sb.append("\n");
        sb.append("# Internal buffer size for the async publishers").append("\n");
        sb.append("buffersize=10000").append("\n");
        sb.append("\n");
        sb.append("# Persistet or not").append("\n");
        sb.append("default_persistence=true").append("\n");
        sb.append("\n");
        sb.append("# The exchange name that recieves them messages").append("\n");
        sb.append("exchange=ws_logging").append("\n");
        sb.append("\n");
        sb.append("# Credentials").append("\n");
        sb.append("user=irisrabbit").append("\n");
        sb.append("password=eel8ed").append("\n");
        sb.append("\n");
        sb.append("# Probably never normnally reconnect").append("\n");
        sb.append("reconnect_interval=-1").append("\n");
        sb.append("\n");
        sb.append("# How often to wait between failed connection attempts in msec").append("\n");
        sb.append("retry_interval=4000").append("\n");

        os.write(sb.toString().getBytes());
        os.close();

        return testFile.getAbsolutePath();
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
