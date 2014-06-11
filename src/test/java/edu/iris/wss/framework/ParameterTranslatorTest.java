/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.iris.wss.framework;

import edu.iris.wss.framework.AppConfigurator.OutputType;
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
        "query=format%3Dtext%0D%0Alevel%3Dchannel%0D%0AIU+ANMO+--+*+1995-01-11T01%3A11%3A00+1998-02-22T02%3A22%3A00";
        String value = ParameterTranslator.extractValueByKey(postData,
            ParameterTranslator.outputControlSignature2);
        
        System.out.println("******* test value: " + value);
        assertEquals(OutputType.valueOf(value.toUpperCase()), OutputType.TEXT);
        
        postData =
        "query=output%3Dtext%0D%0Alevel%3Dchannel%0D%0AIU+ANMO+--+*+1995-01-11T01%3A11%3A00+1998-02-22T02%3A22%3A00";
        value = ParameterTranslator.extractValueByKey(postData,
            ParameterTranslator.outputControlSignature1);
        assertEquals(OutputType.valueOf(value.toUpperCase()), OutputType.TEXT);

        postData =
        "query=level%3Dchannel%0D%0Aformat%3Dtext%0D%0AIU+ANMO+--+*+1995-01-11T01%3A11%3A00+1998-02-22T02%3A22%3A00";
        value = ParameterTranslator.extractValueByKey(postData,
            ParameterTranslator.outputControlSignature2);
        assertEquals(OutputType.valueOf(value.toUpperCase()), OutputType.TEXT);

        postData =
        "AIU+ANMO+--+*+1995-01-11T01%3A11%3A00+1998-02-22T02%3A22%3A00%0D%0Aquery=level%3Dchannel%0D%0Aformat%3Dtext";
        value = ParameterTranslator.extractValueByKey(postData,
            ParameterTranslator.outputControlSignature2);
        assertEquals(OutputType.valueOf(value.toUpperCase()), OutputType.TEXT);

        postData =
        "%0q";
        try {
            value = ParameterTranslator.extractValueByKey(postData,
                ParameterTranslator.outputControlSignature2);
            fail("Unexpected successess, should have had an java.lang.IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            // noop
        }
    }
}
