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

package edu.iris.wss.endpoints;

import edu.iris.wss.framework.AppConfigurator;
import edu.iris.wss.framework.FileCreaterHelper;
import edu.iris.wss.framework.GrizzlyContainerHelper;
import edu.iris.wss.framework.ParamConfigurator;
import edu.iris.wss.framework.Util;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
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
public class StreamEaterTest {
    public static final String THIS_CLASS_NAME = StreamEaterTest.class.getSimpleName();
    public static final Logger LOGGER = Logger.getLogger(THIS_CLASS_NAME);

    // Using this to automate test iterations with JUnit setup method
    private static enum TEST_ID {
		TEST1, TEST2, TEST3
	};
    private static final ArrayList<TEST_ID> TEST_IDS = new ArrayList();
    private static int nameCounter = 0;
    static {
        TEST_IDS.add(TEST_ID.TEST1);
        TEST_IDS.add(TEST_ID.TEST2);
        TEST_IDS.add(TEST_ID.TEST3);
    }

    private static enum TEST_PARAM {
		EXIT_0_WITH_STDOUT, EXIT_0_WITH_NO_STDOUT, EXIT_3_WITH_ERR_MSG,
        EXIT_3_WITH_NO_ERR_MSG, EXIT_1234_NO_OUTPUT, EXIT_4_STDOUT_THEN_STDERR
	};

    private static final String SERVICE_CONTEXT = "/tstStreamEater";
    private static final String ENDPOINT_NAME = "exiting";

    private static final String BASE_HOST = "http://localhost";
    private static final Integer BASE_PORT = 8093;

    private static final URI BASE_URI = URI.create(BASE_HOST + ":"
        + BASE_PORT + SERVICE_CONTEXT);

    public StreamEaterTest() {
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
        String handlerName = null;
        TEST_ID test = TEST_IDS.get(nameCounter);

        // objective here: iterate through the predefined number of test
        // as controlled with TEST_IDS and nameCounter, therefor doing the
        // same set of unit test for each script language

        // JUnit runs setUp and tearDown before and after each test
        // respectively, iterating through TEST_IDS enables a mechanism
        // to change test values based on the number of JUnit tests defined.
        if (test.equals(TEST_ID.TEST1)) {
            handlerName = FileCreaterHelper.createFileInWssFolder(
                  "test_script_1", ".sh", createBashExitScript(), true);
        } else if (test.equals(TEST_ID.TEST2)) {
            handlerName = FileCreaterHelper.createFileInWssFolder(
                  "test_script_2", ".py", createPythonExitScript(), true);
        } else if (test.equals(TEST_ID.TEST3)) {
            handlerName = FileCreaterHelper.createFileInWssFolder(
                  "test_script_3", ".pl", createPerlExitScript(), true);
        } else {
            // TBD
        }
        nameCounter++; // NOTE: the number of tests cannot exceed the
                       //       number of elementas in TEST_IDS

        String newFN = FileCreaterHelper.createFileInWssFolder(SERVICE_CONTEXT,
              AppConfigurator.SERVICE_CFG_NAME_SUFFIX,
              createServiceCfgFile(ENDPOINT_NAME, handlerName),
              false);

        newFN = FileCreaterHelper.createFileInWssFolder(SERVICE_CONTEXT,
              ParamConfigurator.PARAM_CFG_NAME_SUFFIX,
              createParamCfgFile(ENDPOINT_NAME),
              false);


        GrizzlyContainerHelper.setUpServer(BASE_URI, this.getClass().getName(),
              SERVICE_CONTEXT);
    }

    @After
    public void tearDown() throws Exception {
        GrizzlyContainerHelper.tearDownServer(this.getClass().getName());
    }

    @Test
    public void test_1() throws Exception {
        test_all();
    }

    @Test
    public void test_2() throws Exception {
        test_all();
    }

    @Test
    public void test_3() throws Exception {
        test_all();
    }

    private void test_all() throws Exception {
        test_0_stdout();
        test_0_no_stdout();
        test_3_with_err_msg();
        test_3_with_no_err_msg();
        test_1234_no_output();
        test_3_with_no_param();
        test_4_stdout_then_stderr();
    }

    public void test_0_stdout() throws Exception {
        Response response = do_GET(TEST_PARAM.EXIT_0_WITH_STDOUT);

        assertEquals(200, response.getStatus());
        assertEquals("text/plain", response.getMediaType().toString());
    }

    public void test_0_no_stdout() throws Exception {
        Response response = do_GET(TEST_PARAM.EXIT_0_WITH_NO_STDOUT);

        assertEquals(204, response.getStatus());

        String testMsg = response.readEntity(String.class);
        assertEquals(testMsg, "");
    }

    public void test_3_with_err_msg() throws Exception {
        Response response = do_GET(TEST_PARAM.EXIT_3_WITH_ERR_MSG);

        assertEquals(400, response.getStatus());

//      possible Grizzley shortcoming, I expect standard FDSN error message
//      as type text/plain, does not happen in the unittest
////        assertEquals("text/plain", response.getMediaType().toString());
//      instead, get this html message
//        String testMsg = response.readEntity(String.class);
//        System.out.println("* -----------------------------------------test_3_with_err_msg - text: " + testMsg);
    }

    public void test_3_with_no_err_msg() throws Exception {
        Response response = do_GET(TEST_PARAM.EXIT_3_WITH_NO_ERR_MSG);

        assertEquals(400, response.getStatus());
//        assertEquals("text/plain", response.getMediaType().toString());
    }

    public void test_1234_no_output() throws Exception {
        Response response = do_GET(TEST_PARAM.EXIT_1234_NO_OUTPUT);

        assertEquals(500, response.getStatus());
//        assertEquals("text/plain", response.getMediaType().toString());
    }

    public void test_3_with_no_param() throws Exception {
        // test that the script returns a 3, bad parameter for
        //undefined test parameters
        Client c = ClientBuilder.newClient();

        WebTarget webTarget = c.target(BASE_URI)
              .path(ENDPOINT_NAME)
              .queryParam("format", "TEXT");

        Response response = webTarget.request().get();

//        System.out.println("^^^^^^^^^^^^^^^^ test: " + "not specified param" +
//              "  text: " + response.readEntity(String.class));

        assertEquals(400, response.getStatus());
//        assertEquals("text/plain", response.getMediaType().toString());
    }

    public void test_4_stdout_then_stderr() throws Exception {
        Response response = do_GET(TEST_PARAM.EXIT_4_STDOUT_THEN_STDERR);

        String answer = response.readEntity(String.class);
//        System.out.println("^^^^^^^^^^^^^^^^ test: " +
//              TEST_PARAM.EXIT_4_STDOUT_THEN_STDERR.toString() +
//              "  text: " + answer);

        // normal - CmdProcessor returns 200 before starting to stream
        // even though a latter error occurs, causes STREAMERROR to data
        // stream. Eventully fails with exit code 4, see junit_wss_log
        assertEquals(200, response.getStatus());
        assertEquals("text/plain", response.getMediaType().toString());
        assert(answer.contains("STREAMERROR##STREAMERROR"));
    }

    private Response do_GET(TEST_PARAM tparm) throws Exception {
        Client c = ClientBuilder.newClient();

        WebTarget webTarget = c.target(BASE_URI)
              .path(ENDPOINT_NAME)
              .queryParam(tparm.toString(), "no_val")
              .queryParam("format", "TEXT");

        Response response = webTarget.request().get();

        // This should be true for all queries when corsEnabled=false
        assertEquals(null, response.getHeaderString("access-control-allow-origin"));

        return response;
    }

    private static String createServiceCfgFile(String endpointName,
          String handlerName) {
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
              "singletonClassName=edu.iris.wss.framework.TestSingleton",
              "",
              "# ----------------  endpoints",
              "",
              endpointName + ".endpointClassName=edu.iris.wss.endpoints.CmdProcessor",
              endpointName + ".handlerProgram=" + handlerName,
              endpointName + ".handlerTimeout=200",
              endpointName + ".handlerWorkingDirectory=/tmp",
              endpointName + ".usageLog",
              endpointName + ".postEnabled=true",
              endpointName + ".logMiniseedExtents = false",
              endpointName + ".use404For204=false",
              endpointName + ".formatTypes = \\",
              "    text: text/plain,\\",
              "    json: application/json, \\",
              "    miniseed: application/vnd.fdsn.mseed, \\",
              "    geocsv: text/plain",
              ""
        );

        return s;
    }

    private static String createParamCfgFile(String endpointName) {
        String s = String.join("\n",
              "# ----------------  endpoints",
              "",
              endpointName + ".format=TEXT",
              endpointName + "." + TEST_PARAM.EXIT_0_WITH_STDOUT + "=TEXT",
              endpointName + "." + TEST_PARAM.EXIT_0_WITH_NO_STDOUT + "=TEXT",
              endpointName + "." + TEST_PARAM.EXIT_3_WITH_ERR_MSG + "=TEXT",
              endpointName + "." + TEST_PARAM.EXIT_3_WITH_NO_ERR_MSG + "=TEXT",
              endpointName + "." + TEST_PARAM.EXIT_1234_NO_OUTPUT + "=TEXT",
              endpointName + "." + TEST_PARAM.EXIT_4_STDOUT_THEN_STDERR + "=TEXT"
        );

        return s;
    }

    private static String createScriptForSTDOUT() {
        String s = String.join("\n",
              "#!/bin/bash",
              "",
              "args=\"$@\"",
              "",
              "# bash for writing to strderr",
              "##echo \"TEST message FROM sleep script after 105, TO stderr\" >&2",
              "##exit 3",
              "",
              "echo \"line one of script\"",
              "echo \"date -u: \" `date -u`",
              "echo \"input args: ${args}\"",
              "echo \"line 4 of script, last line\""
        );

        return s;
    }

    private static String createBashExitScript() {
        String s = String.join("\n",
              "#!/bin/bash",
              "",
              "# do logging without disrupting stdout, stderr",
              "logfile=/tmp/exit_testing_with_bash_log.txt",
              "",
              "args=\"$@\"",
              "echo \"input args: ${args}  date: \" `date` >> ${logfile}",
              "",
              "# http://stackoverflow.com/questions/192249/how-do-i-parse-command-line-arguments-in-bash",
              "",
              "# need to loop through all arguments because order can vary",
              "# depending on the argument name, exit on the first one recognized ",
              "",
              "# $# is count of positional arguments, arguments",
              "# \"shift\" means decrement positional counter by 1",
              "",
              "while [[ $# > 1 ]]",
              "do",
              "    case $1 in",
              "        --" + TEST_PARAM.EXIT_3_WITH_ERR_MSG + ")",
              "            # bash syntax for writing to stderr",
              "            echo \"TEST bash error message, written to stderr\" >&2",
              "            exit 3",
              "        ;;",
              "        --" + TEST_PARAM.EXIT_3_WITH_NO_ERR_MSG + ")",
              "            exit 3",
              "        ;;",
              "        --" + TEST_PARAM.EXIT_0_WITH_STDOUT + ")",
              "            echo \"TEST bash message, written to stdout\"",
              "            exit 0",
              "        ;;",
              "        --" + TEST_PARAM.EXIT_0_WITH_NO_STDOUT + ")",
              "            exit 0",
              "        ;;",
              "        --" + TEST_PARAM.EXIT_1234_NO_OUTPUT + ")",
              "            exit 1234",
              "        ;;",
              "        --" + TEST_PARAM.EXIT_4_STDOUT_THEN_STDERR + ")",
              "            # bash syntax for writing to stderr",
              "            echo \"TEST bash message, written to stdout, then err, then exit 4\"",
              "            echo \"TEST bash error message, written to stderr, then exit 4\" >&2",
              "            exit 4",
              "        ;;",
              "        *)",
              "            # unknown parameter, fall through",
              "        ;;",
              "    esac",
              "shift",
              "done",
              "",
              "echo \"Unit test unexpected bash parameter: \" ${1}" +
                    " \"  date: \" `date` >> ${logfile}",
              "echo \"Unit test unexpected bash parameter: \" ${1}" +
                    " \"  date: \" `date` >&2",
              "exit 3",
              ""
        );

        return s;
    }

    private static String createPythonExitScript() {
        String s = String.join("\n",
              "#!/usr/bin/python",
              "",
              "from __future__ import print_function",
              "import sys",
              "import argparse",
              "",
              "parser = argparse.ArgumentParser()",
              "parser.add_argument(\"--format\", help=\"enable format selection (ignored in this version)\")",
              "parser.add_argument(\"--" + TEST_PARAM.EXIT_3_WITH_ERR_MSG + "\", help=\"write to stderr and exit with code 3\")",
              "parser.add_argument(\"--" + TEST_PARAM.EXIT_3_WITH_NO_ERR_MSG + "\", help=\"exit with code 3\")",
              "parser.add_argument(\"--" + TEST_PARAM.EXIT_0_WITH_STDOUT + "\", help=\"write to stdout and exit with code 0\")",
              "parser.add_argument(\"--" + TEST_PARAM.EXIT_0_WITH_NO_STDOUT + "\", help=\"exit with code 0\")",
              "parser.add_argument(\"--" + TEST_PARAM.EXIT_1234_NO_OUTPUT + "\", help=\"exit with code 1234\")",
              "parser.add_argument(\"--" + TEST_PARAM.EXIT_4_STDOUT_THEN_STDERR + "\", help=\"write to stdout, write to stderr, and exit with code 4\")",
              "args = parser.parse_args()",
              "",
              "def printErr(*args, **kwargs):",
              "    print(*args, file=sys.stderr, **kwargs)",
              "",
              "if args." + TEST_PARAM.EXIT_3_WITH_ERR_MSG + ":",
              "    printErr(\"TEST python error message, written to stderr\")",
              "    exit(3)",
              "elif args." + TEST_PARAM.EXIT_3_WITH_NO_ERR_MSG + ":",
              "    exit(3)",
              "elif args." + TEST_PARAM.EXIT_0_WITH_STDOUT + ":",
              "    print(\"TEST python message, written to stdout\")",
              "    exit(0)",
              "elif args." + TEST_PARAM.EXIT_0_WITH_NO_STDOUT + ":",
              "    exit(0)",
              "elif args." + TEST_PARAM.EXIT_1234_NO_OUTPUT + ":",
              "    exit(1234)",
              "elif args." + TEST_PARAM.EXIT_4_STDOUT_THEN_STDERR + ":",
              "    print(\"TEST python message, written to stdout, then err, then exit 4\")",
              "    printErr(\"TEST python error message, written to stderr, then exit 4\")",
              "    exit (4)",
              "",
              "printErr(\"Unit test no argument in python\")",
              "exit(3)",
              ""
        );

        return s;
    }

    private static String createPerlExitScript() {
        String s = String.join("\n",
              "#!/usr/bin/perl",
              "",
              "use strict;",
              "use warnings;",
              "use Getopt::Long;",
              "",
              "my $" + TEST_PARAM.EXIT_3_WITH_ERR_MSG + " = undef;",
              "my $" + TEST_PARAM.EXIT_3_WITH_NO_ERR_MSG + " = undef;",
              "my $" + TEST_PARAM.EXIT_0_WITH_STDOUT + " = undef;",
              "my $" + TEST_PARAM.EXIT_0_WITH_NO_STDOUT + " = undef;",
              "my $" + TEST_PARAM.EXIT_1234_NO_OUTPUT + " = undef;",
              "my $" + TEST_PARAM.EXIT_4_STDOUT_THEN_STDERR + " = undef;",
              "my $format = undef;",
              "",
              "# Parse command line arguments",
              "Getopt::Long::Configure (qw{ bundling_override no_auto_abbrev no_ignore_case_always });",
              "my $getoptsret = GetOptions (",
              "    '" + TEST_PARAM.EXIT_3_WITH_ERR_MSG + "=s'        => \\$" + TEST_PARAM.EXIT_3_WITH_ERR_MSG + ",",
              "    '" + TEST_PARAM.EXIT_3_WITH_NO_ERR_MSG + "=s'     => \\$" + TEST_PARAM.EXIT_3_WITH_NO_ERR_MSG + ",",
              "    '" + TEST_PARAM.EXIT_0_WITH_STDOUT + "=s'         => \\$" + TEST_PARAM.EXIT_0_WITH_STDOUT + ",",
              "    '" + TEST_PARAM.EXIT_0_WITH_NO_STDOUT + "=s'      => \\$" + TEST_PARAM.EXIT_0_WITH_NO_STDOUT + ",",
              "    '" + TEST_PARAM.EXIT_1234_NO_OUTPUT + "=s'        => \\$" + TEST_PARAM.EXIT_1234_NO_OUTPUT + ",",
              "    '" + TEST_PARAM.EXIT_4_STDOUT_THEN_STDERR + "=s'  => \\$" + TEST_PARAM.EXIT_4_STDOUT_THEN_STDERR + ",",
              "    'format=s'                     => \\$format",
              ");",
              "",
              "if ($" + TEST_PARAM.EXIT_3_WITH_ERR_MSG + ") {",
              "    print STDERR (\"TEST perl error message, written to stderr\");",
              "    exit(3);",
              "}",
              "elsif ($" + TEST_PARAM.EXIT_3_WITH_NO_ERR_MSG + ") {",
              "    exit(3);",
              "}",
              "elsif ($" + TEST_PARAM.EXIT_0_WITH_STDOUT + ") {",
              "    print(\"TEST perl message, written to stdout\");",
              "    exit(0);",
              "}",
              "elsif ($" + TEST_PARAM.EXIT_0_WITH_NO_STDOUT + ") {",
              "    exit(0);",
              "}",
              "elsif ($" + TEST_PARAM.EXIT_1234_NO_OUTPUT + ") {",
              "    exit(1234);",
              "}",
              "elsif ($" + TEST_PARAM.EXIT_4_STDOUT_THEN_STDERR + ") {",
              "    print(\"TEST perl message, written to stdout, then err, then exit 4\");",
              "    print STDERR(\"TEST perl error message, written to stderr, then exit 4\");",
              "    exit (4);",
              "}",
              "",
              "print STDERR (\"Unit test no argument in perl\");",
              "exit(3);",
              ""
        );

        return s;
    }
}
