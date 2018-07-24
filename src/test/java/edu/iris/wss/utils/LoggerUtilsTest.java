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

package edu.iris.wss.utils;


import edu.iris.dmc.logging.usage.WSUsageItem;
import edu.iris.wss.utils.LoggerUtils;
import java.util.Date;
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
public class LoggerUtilsTest {



    public LoggerUtilsTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }


    @Test
    public void test_headerOutput() throws Exception {

        String expectedHeading =
              "# Application|Host Name|Access Date|Client Name|Client IP"
              + "|Data Length|Processing Time (ms)"
              + "|Error Type|User Agent|HTTP Status|User"
              + "|Network|Station|Location|Channel|Quality"
              + "|Start Time|End Time|Extra|Message Type";

        String headerTrial = LoggerUtils.getUsageLogHeader();

        assertEquals(expectedHeading, headerTrial);
    }

    @Test
    public void test_usageLogOutput() throws Exception {

        long datasize = 1234;
        long processTime = 10203040;

        long aTime = 1490887550333L; // 2017-03-30T15:25:50Z
        Date accessDate = new Date(aTime);
        Date startDate = new Date(aTime + (5 * 1000));
        Date endDate = new Date(aTime + (2 * 60 * 1000));

        int status = 200;

        WSUsageItem wsuItems = new WSUsageItem();

        wsuItems.setApplication(      "Application");
        wsuItems.setHost(             "Host Name");
        wsuItems.setAccessDate(       accessDate);
        wsuItems.setClientName(       "Client Name");
        wsuItems.setClientIp(         "Client IP");
        wsuItems.setDataSize(         datasize);
        wsuItems.setProcessTimeMsec(  processTime);

        wsuItems.setErrorType(        "Error Type");
        wsuItems.setUserAgent(        "User Agent");
        wsuItems.setHttpCode(         status);
        wsuItems.setUserName(         "User");

        wsuItems.setNetwork(          "Network");
        wsuItems.setStation(          "Station");
        wsuItems.setLocation(         "Location");
        wsuItems.setChannel(          "Channel");
        wsuItems.setQuality(          "Quality");

        wsuItems.setStartTime(        startDate);
        wsuItems.setEndTime(          endDate);

        wsuItems.setExtra(            "Extra");
        wsuItems.setMessagetype(      "wfstat");

        String usageTrial = LoggerUtils.makeUsageLogString(wsuItems);

        String expectedUsage = "Application|Host Name|2017-03-30T15:25:50Z"
              + "|Client Name|Client IP|1234|10203040|Error Type|User Agent"
              + "|200|User|Network|Station|Location|Channel|Quality"
              + "|2017-03-30T15:25:55Z|2017-03-30T15:27:50Z|Extra|wfstat";

        assertEquals(expectedUsage, usageTrial);
    }
}
