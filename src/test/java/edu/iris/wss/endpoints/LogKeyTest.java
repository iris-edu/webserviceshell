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

package edu.iris.wss.endpoints;

import edu.iris.wss.endpoints.CmdProcessor.LogKey;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author mike
 */
public class LogKeyTest {
    public static final Logger logger = Logger.getLogger(LogKeyTest.class);

    public LogKeyTest() {
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
    public void testMakeKey() {

        String net = "IU";
        String sta = "ANMO";
        String loc = "50";
        String chan = "LWD";
        char qual = 'M';

        String key = LogKey.makeKey(net, sta, loc, chan, qual);

        assertEquals(net, LogKey.getNetwork(key));
        assertEquals(sta, LogKey.getStation(key));
        assertEquals(loc, LogKey.getLocation(key));
        assertEquals(chan, LogKey.getChannel(key));
        assertEquals(Character.toString(qual), LogKey.getQuality(key));
    }

    @Test
    public void testMakeKeyWithSpaces() {

        String net = "I ";
        String sta = "AN   ";
        String loc = " 3";
        String chan = "LWD ";
        char qual = 'M';

        String key = LogKey.makeKey(net, sta, loc, chan, qual);

        assertEquals(net, LogKey.getNetwork(key));
        assertEquals(sta, LogKey.getStation(key));
        assertEquals(loc, LogKey.getLocation(key));
        assertEquals(chan, LogKey.getChannel(key));
        assertEquals(Character.toString(qual), LogKey.getQuality(key));
    }

    @Test
    public void testMakeKeyWithSpacesAndEmpty() {

        String net = "";
        String sta = "AN   ";
        String loc = "";
        String chan = "LWD ";
        char qual = 'M';

        String key = LogKey.makeKey(net, sta, loc, chan, qual);

        assertEquals(net, LogKey.getNetwork(key));
        assertEquals(sta, LogKey.getStation(key));
        assertEquals(loc, LogKey.getLocation(key));
        assertEquals(chan, LogKey.getChannel(key));
        assertEquals(Character.toString(qual), LogKey.getQuality(key));

        assertEquals(net.trim(), LogKey.getNetwork(key).trim());
        assertEquals(sta.trim(), LogKey.getStation(key).trim());
        assertEquals(loc.trim(), LogKey.getLocation(key).trim());
        assertEquals(chan.trim(), LogKey.getChannel(key).trim());
    }
}
