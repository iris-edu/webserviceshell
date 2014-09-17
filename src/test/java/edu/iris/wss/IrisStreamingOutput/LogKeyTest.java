/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.iris.wss.IrisStreamingOutput;

import edu.iris.wss.IrisStreamingOutput.ProcessStreamingOutput.LogKey;
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
