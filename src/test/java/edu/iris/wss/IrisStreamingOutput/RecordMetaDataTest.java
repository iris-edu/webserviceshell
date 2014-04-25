/*******************************************************************************
 * Copyright (c) 2014 IRIS DMC supported by the National Science Foundation.
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

package edu.iris.wss.IrisStreamingOutput;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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

    /**
     * Test of getSize method, checks for null on new class.
     */
    @Test
    public void testGetSize() {
        System.out.println("getSize");
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
        Date expResult = null;
        Date result = instance.getStart();
        assertEquals(expResult, result);

        Date endResult = instance.getEnd();
        assertEquals(expResult, endResult);
    }

    /**
     * Test start not affected by end change.
     */
    @Test
    public void testGetStartAndEnd() {
        RecordMetaData instance = new RecordMetaData();
        Date expStart = new Date();
        instance.setStart(expStart);
        Date result = instance.getStart();
        assertEquals(expStart, result);
        
        // check end is default null
        assertEquals(instance.getEnd(), null);

        Date expEnd = new Date();
        instance.setEnd(expEnd);
        Date endResult = instance.getEnd();
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
        
        String start = "";
        // note: this test is a by product of our choice to
        //       so use substring to restrict the total length
        //       of an input and trucate microsecond values
        try {
            instance.setIfEarlier(start);
            fail("string should be 21 chars or longer");
        } catch (java.lang.StringIndexOutOfBoundsException ex) {
            assertTrue("should pass for start g>= to 21 cahrs", true);
        }
        
        start = "2011,036,17:24:50.9999";
        instance.setIfEarlier(start);
        
        // expected string base on constraints of substring in method
        String startExpected = "2011,036,17:24:50.999";
        // creating format everytime because SimpleDataFormat is not
        // thread safe
        SimpleDateFormat fmt = new SimpleDateFormat(RecordMetaData.SeisFileDataFormat);
        String startResult = fmt.format(instance.getStart());
        // using the same formatter, I should get the same string from date
        // less the microsecond part
        assertTrue(startResult.equals(startExpected));
        
        // an earlier time should result in a change to start
        start = "2011,036,17:24:49.123";
        instance.setIfEarlier(start);
        startResult = fmt.format(instance.getStart());
        assertTrue(startResult.equals(start));
        
        // a later time should result in no change
        String startPrevious = fmt.format(instance.getStart());
        start = "2011,036,17:24:51.123";
        instance.setIfEarlier(start);
        startResult = fmt.format(instance.getStart());
        
        logger.info("*** startPrevious: " + startPrevious);
        logger.info("***   later start: " + start);
        logger.info("***   startResult: " + startResult);
        
        assertTrue(startResult.equals(startPrevious));
    }

    /**
     * Test of setIfEarlier method, of class RecordMetaData.
     */
    @Test
    public void testSetIfLater() throws Exception {
        RecordMetaData instance = new RecordMetaData();
        assertEquals(instance.getEnd(), null);
        
        String end = "";
        // see testSetIfEarlier for comments
        try {
            instance.setIfLater(end);
            fail("string should be 21 chars or longer");
        } catch (java.lang.StringIndexOutOfBoundsException ex) {
            assertTrue("should pass for start g>= to 21 cahrs", true);
        }
        
        end = "2011,036,17:24:50.9999";
        instance.setIfLater(end);
        
        String endExpected = "2011,036,17:24:50.999";
        // creating format everytime because SimpleDataFormat is not
        // thread safe
        SimpleDateFormat fmt = new SimpleDateFormat(RecordMetaData.SeisFileDataFormat);
        String endResult = fmt.format(instance.getEnd());
        assertTrue(endResult.equals(endExpected));
        
        // a later time should result in a change to end
        end = "2011,036,17:24:51.123";
        instance.setIfLater(end);
        endResult = fmt.format(instance.getEnd());
        assertTrue(endResult.equals(end));
        
        // an earlier time should result in no change
        String endPrevious = fmt.format(instance.getEnd());
        end = "2011,036,17:24:49.123";
        instance.setIfLater(end);
        endResult = fmt.format(instance.getEnd());
        
        logger.info("*** endPrevious: " + endPrevious);
        logger.info("*** earlier end: " + end);
        logger.info("***   endResult: " + endResult);
        
        assertTrue(endResult.equals(endPrevious));
    }
}
