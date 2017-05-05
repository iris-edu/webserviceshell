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

package edu.iris.wss.framework;

import edu.iris.wss.framework.AppConfigurator.InternalTypes;
import java.util.ArrayList;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author mike
 */
public class ParameterTranslatorTest {
    public final static String outputControlSignature2 = "format";
    public final static String outputControlSignature1 = "output";

    public ParameterTranslatorTest() {
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
     * Test of parseQueryParams method, of class ParameterTranslator.
     */
    @Test
    public void testParseQueryParams() throws Exception {
        String postData =
        "query=format%3Dbinary%0D%0Alevel%3Dchannel%0D%0AIU+ANMO+--+*+1995-01-11T01%3A11%3A00+1998-02-22T02%3A22%3A00";
        String value = ParameterTranslator.extractValueByKey(postData,
            outputControlSignature2);

        assertEquals(InternalTypes.valueOf(value.toUpperCase()), InternalTypes.BINARY);

        postData =
        "query=output%3Dbinary%0D%0Alevel%3Dchannel%0D%0AIU+ANMO+--+*+1995-01-11T01%3A11%3A00+1998-02-22T02%3A22%3A00";
        value = ParameterTranslator.extractValueByKey(postData,
            outputControlSignature1);
        assertEquals(InternalTypes.valueOf(value.toUpperCase()), InternalTypes.BINARY);

        postData =
        "query=level%3Dchannel%0D%0Aformat%3Dbinary%0D%0AIU+ANMO+--+*+1995-01-11T01%3A11%3A00+1998-02-22T02%3A22%3A00";
        value = ParameterTranslator.extractValueByKey(postData,
            outputControlSignature2);
        assertEquals(InternalTypes.valueOf(value.toUpperCase()), InternalTypes.BINARY);

        postData =
        "AIU+ANMO+--+*+1995-01-11T01%3A11%3A00+1998-02-22T02%3A22%3A00%0D%0Aquery=level%3Dchannel%0D%0Aformat%3Dbinary";
        value = ParameterTranslator.extractValueByKey(postData,
            outputControlSignature2);
        assertEquals(InternalTypes.valueOf(value.toUpperCase()), InternalTypes.BINARY);

        postData =
        "%0q";
        try {
            value = ParameterTranslator.extractValueByKey(postData,
                outputControlSignature2);
            fail("Unexpected successess, should have had an java.lang.IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            // noop
        }
    }

    /**
     * Test of parseQueryParams method, of class ParameterTranslator.
     * Using Station parameter setup to test format for miniseed and mseed
     */
    @Test
    public void testParseQueryParamsMiniseed() throws Exception {

        String postData =
        "query=level%3Dchannel%0D%0Aformat%3Dminiseed%0D%0Aminlat%3D34%0D%0Amaxlat%3D35%0D%0AWY+YOF+--+EHZ+1990-01-01T12%3A00%3A00+1992-07-01T12%3A00%3A00%0D%0AWY+YDC+01+EHZ+2013-06-01T00%3A00%3A00+2013-07-01T00%3A00%3A00%0D%0APT+HON+--+HH%3F+2001-01-01T00%3A00%3A00+2002-01-01T00%3A00%3A00%0D%0ASC+CBET+--+EH%3F+1999-01-01T00%3A00%3A00+2002-01-01T00%3A00%3A00%0D%0ASC+CPRX+--+EH%3F+1999-01-01T00%3A00%3A00+2002-01-01T00%3A00%3A00%0D%0AIU+ANMO+10+BHZ+2005-01-01T00%3A00%3A00+2006-01-01T00%3A00%3A00%0D%0AIU+ANMO+10+H*+2005-01-01T00%3A00%3A00+2007-01-01T00%3A00%3A00%0D%0A";
        String value = ParameterTranslator.extractValueByKey(postData,
            outputControlSignature2);

        assertEquals(InternalTypes.MINISEED, InternalTypes.valueOf(value.toUpperCase()));

        // mseed should work also
        postData =
        "query=level%3Dchannel%0D%0Aformat%3Dmseed%0D%0Aminlat%3D34%0D%0Amaxlat%3D35%0D%0AWY+YOF+--+EHZ+1990-01-01T12%3A00%3A00+1992-07-01T12%3A00%3A00%0D%0AWY+YDC+01+EHZ+2013-06-01T00%3A00%3A00+2013-07-01T00%3A00%3A00%0D%0APT+HON+--+HH%3F+2001-01-01T00%3A00%3A00+2002-01-01T00%3A00%3A00%0D%0ASC+CBET+--+EH%3F+1999-01-01T00%3A00%3A00+2002-01-01T00%3A00%3A00%0D%0ASC+CPRX+--+EH%3F+1999-01-01T00%3A00%3A00+2002-01-01T00%3A00%3A00%0D%0AIU+ANMO+10+BHZ+2005-01-01T00%3A00%3A00+2006-01-01T00%3A00%3A00%0D%0AIU+ANMO+10+H*+2005-01-01T00%3A00%3A00+2007-01-01T00%3A00%3A00%0D%0A";
        value = ParameterTranslator.extractValueByKey(postData,
            outputControlSignature2);

        assertEquals(InternalTypes.MSEED, InternalTypes.valueOf(value.toUpperCase()));

        postData =
        "level=channel\n" +
        "format=miniseed\n" +
        "minlat=34\n" +
        "maxlat=35\n" +
        "WY YOF -- EHZ 1990-01-01T12:00:00 1992-07-01T12:00:00\n" +
        "WY YDC 01 EHZ 2013-06-01T00:00:00 2013-07-01T00:00:00\n" +
        "PT HON -- HH? 2001-01-01T00:00:00 2002-01-01T00:00:00\n" +
        "SC CBET -- EH? 1999-01-01T00:00:00 2002-01-01T00:00:00\n" +
        "SC CPRX -- EH? 1999-01-01T00:00:00 2002-01-01T00:00:00\n" +
        "IU ANMO 10 BHZ 2005-01-01T00:00:00 2006-01-01T00:00:00\n" +
        "IU ANMO 10 H* 2005-01-01T00:00:00 2007-01-01T00:00:00";

        value = ParameterTranslator.extractValueByKey(postData,
            outputControlSignature2);

        assertEquals(InternalTypes.MINISEED, InternalTypes.valueOf(value.toUpperCase()));
    }


    /**
     * Test of parseQueryParams method, of class ParameterTranslator.
     * Using Station parameter setup to test for missing format parameter
     */
    @Test
    public void testParseQueryParamsNoFormatPresent() throws Exception {

        String postData =
        "query=latitude%3D30%0D%0Alongitude%3D-100%0D%0Aminradius%3D0.1%0D%0Amaxradius%3D5%0D%0A*+*+*+*+*+*%0D%0A";
        String value = ParameterTranslator.extractValueByKey(postData,
            outputControlSignature2);

        assertEquals(null, value);


        postData =
        "latitude=30\n" +
        "longitude=-100\n" +
        "minradius=0.1\n" +
        "maxradius=5\n" +
        "* * * * * *";

        value = ParameterTranslator.extractValueByKey(postData,
            outputControlSignature2);

        assertEquals(null, value);
    }

    /**
     * Test of parseQueryParams method, of class ParameterTranslator.
     * Using Station parameter setup to test for missing format parameter
     */
    @Test
    public void testParseQueryParamsNoValue() throws Exception {

        String postData =
        "query=latitude%3D30%0D%0Aformat%3D%0D%0Alongitude%3D-100%0D%0Aminradius%3D0.1%0D%0Amaxradius%3D5%0D%0A*+*+*+*+*+*%0D%0A";
        String value = ParameterTranslator.extractValueByKey(postData,
            outputControlSignature2);

        assertEquals("", value);

        postData =
        "latitude=30\n" +
        "format=\n" +
        "longitude=-100\n" +
        "minradius=0.1\n" +
        "maxradius=5\n" +
        "* * * * * *";

        value = ParameterTranslator.extractValueByKey(postData,
            outputControlSignature2);

        assertEquals("", value);
    }

    /**
     * Test of parseQueryParams method, of class ParameterTranslator.
     * looking for format=text
     */
    @Test
    public void testParseQueryFormatText() throws Exception {

        String postData =
        "query=level%3Dchannel%0D%0Aformat%3Dbinary%0D%0ATA+*+*+*+*+2014-05-01T00%3A00%3A00";
        String value = ParameterTranslator.extractValueByKey(postData,
            outputControlSignature2);

        assertEquals(InternalTypes.BINARY, InternalTypes.valueOf(value.toUpperCase()));

        postData =
        "level=channel\n" +
        "format=binary\n" +
        "TA * * * * 2014-05-01T00:00:00";

        value = ParameterTranslator.extractValueByKey(postData,
            outputControlSignature2);

        assertEquals(InternalTypes.BINARY, InternalTypes.valueOf(value.toUpperCase()));
    }
}
