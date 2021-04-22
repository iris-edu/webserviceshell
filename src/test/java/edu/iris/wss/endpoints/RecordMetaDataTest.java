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


import edu.iris.wss.framework.Util;
import edu.sc.seis.seisFile.mseed.Btime;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author mike
 */
public class RecordMetaDataTest {
    public static final Logger logger = Logger.getLogger(RecordMetaDataTest.class);

    public RecordMetaDataTest() {
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

    public Calendar btimeToCalendar(Btime btime) {
        Calendar cal = Calendar.getInstance();

        cal.set(Calendar.YEAR, btime.getYear());
        cal.set(Calendar.DAY_OF_YEAR, btime.getDayOfYear());
        cal.set(Calendar.HOUR_OF_DAY, btime.getHour());
        cal.set(Calendar.MINUTE, btime.getMin());
        cal.set(Calendar.SECOND, btime.getSec());
        cal.set(Calendar.MILLISECOND, btime.getTenthMilli() / 10);

        return cal;
    }

    /**
     * Test of getSize method, checks for null on new class.
     */
    @Test
    public void testGetSize() {
        RecordMetaData instance = new RecordMetaData();
        Long expResult = null;
        Long result = instance.getSize();
        assertEquals(expResult, result);
    }

    /**
     * Test of setSize method with null input.
     */
    @Test
    public void testSetSize() {
        Long size = null;
        RecordMetaData instance = new RecordMetaData();
        instance.setSize(size);
    }

    /**
     * Test of setSize and getSize with non null Long.
     */
    @Test
    public void testSetAndGetSize() {
        Long size = new Long(23);
        RecordMetaData instance = new RecordMetaData();
        instance.setSize(size);
        Long result = instance.getSize();
        assertTrue(result.equals(size));
    }

    /**
     * Test start and end are created as null, may not be that important.
     */
    @Test
    public void testGetStartAndEndAreNull() {
        RecordMetaData instance = new RecordMetaData();
        Btime expResult = null;
        Btime result = instance.getStart();
        assertEquals(expResult, result);

        Btime endResult = instance.getEnd();
        assertEquals(expResult, endResult);
    }

    /**
     * Test start not affected by end change.
     */
    @Test
    public void testGetStartAndEnd() {
        RecordMetaData instance = new RecordMetaData();
        Btime expStart = new Btime(Instant.now());
        instance.setStart(expStart);
        Btime result = instance.getStart();
        assertEquals(expStart, result);

        // check end is default null
        assertEquals(instance.getEnd(), null);

        Btime expEnd = new Btime(Instant.now());
        instance.setEnd(expEnd);
        Btime endResult = instance.getEnd();
        assertEquals(expEnd, endResult);

        // check start not changed
        assertEquals(expStart, result);
    }

    /**
     * Test of setIfEarlier method, of class RecordMetaData.
     */
    @Test
    public void testSetIfEarlier() throws Exception {
        RecordMetaData instance = new RecordMetaData();
        assertEquals(instance.getStart(), null);

        //start = "2011,036,17:24:50.9999";
        Calendar start = Calendar.getInstance(Util.UTZ_TZ);
        start.set(Calendar.YEAR, 2011);
        start.set(Calendar.DAY_OF_YEAR, 36);
        start.set(Calendar.HOUR_OF_DAY, 17);
        start.set(Calendar.MINUTE, 24);
        start.set(Calendar.SECOND, 50);
        start.set(Calendar.MILLISECOND, 999);

        instance.setIfEarlier(new Btime(Instant.ofEpochMilli(start.getTimeInMillis())));

        // expected string base on constraints of substring in method
        String startExpected = "2011,036,17:24:50.999";

        // creating format everytime because SimpleDataFormat is not
        // thread safe
        SimpleDateFormat fmt = new SimpleDateFormat(RecordMetaData.SeisFileDataFormat);
        fmt.setTimeZone(Util.UTZ_TZ);
        String startResult = fmt.format(btimeToCalendar(instance.getStart()).getTime());
        // using the same formatter, I should get the same string from date
        // less the microsecond part

        assertTrue(startResult.equals(startExpected));

        // an earlier time should result in a change to start
        startExpected = "2011,036,17:24:49.123";
        start.set(Calendar.SECOND, 49);
        start.set(Calendar.MILLISECOND, 123);

        instance.setIfEarlier(new Btime(Instant.ofEpochMilli(start.getTimeInMillis())));
        startResult = fmt.format(btimeToCalendar(instance.getStart()).getTime());
        assertTrue(startResult.equals(startExpected));

        // a later time should result in no change
        String startPrevious = startResult;
        //startExpected = "2011,036,17:24:51.123";
        start.set(Calendar.SECOND, 51);
        start.set(Calendar.MILLISECOND, 123);
        instance.setIfEarlier(new Btime(Instant.ofEpochMilli(start.getTimeInMillis())));
        startResult = fmt.format(btimeToCalendar(instance.getStart()).getTime());

        logger.info("*** startPrevious: " + startPrevious);
        logger.info("***   later start: " + start);
        logger.info("***   startResult: " + startResult);

        assertTrue(startResult.equals(startPrevious));
    }

    /**
     * Test of setIfLater method, of class RecordMetaData.
     */
    @Test
    public void testSetIfLater() throws Exception {
        RecordMetaData instance = new RecordMetaData();
        assertEquals(instance.getEnd(), null);

        //end = "2011,036,17:24:50.9999";
        Calendar end = Calendar.getInstance(Util.UTZ_TZ);
        end.set(Calendar.YEAR, 2011);
        end.set(Calendar.DAY_OF_YEAR, 36);
        end.set(Calendar.HOUR_OF_DAY, 17);
        end.set(Calendar.MINUTE, 24);
        end.set(Calendar.SECOND, 50);
        end.set(Calendar.MILLISECOND, 999);

        instance.setIfLater(new Btime(Instant.ofEpochMilli(end.getTimeInMillis())));

        String endExpected = "2011,036,17:24:50.999";
        // creating format everytime because SimpleDataFormat is not
        // thread safe
        SimpleDateFormat fmt = new SimpleDateFormat(RecordMetaData.SeisFileDataFormat);
        fmt.setTimeZone(Util.UTZ_TZ);
        String endResult = fmt.format(btimeToCalendar(instance.getEnd()).getTime());
        assertTrue(endResult.equals(endExpected));

        // a later time should result in a change to end
        endExpected = "2011,036,17:24:51.123";
        end.set(Calendar.SECOND, 51);
        end.set(Calendar.MILLISECOND, 123);
        instance.setIfLater(new Btime(Instant.ofEpochMilli(end.getTimeInMillis())));
        endResult = fmt.format(btimeToCalendar(instance.getEnd()).getTime());
        assertTrue(endResult.equals(endExpected));

        // an earlier time should result in no change
        String endPrevious = endResult;
        //endExpected = "2011,036,17:24:49.123";
        end.set(Calendar.SECOND, 49);
        end.set(Calendar.MILLISECOND, 123);
        instance.setIfLater(new Btime(Instant.ofEpochMilli(end.getTimeInMillis())));
        endResult = fmt.format(btimeToCalendar(instance.getEnd()).getTime());

        logger.info("*** endPrevious: " + endPrevious);
        logger.info("*** earlier end: " + end);
        logger.info("***   endResult: " + endResult);

        assertTrue(endResult.equals(endPrevious));
    }
}
