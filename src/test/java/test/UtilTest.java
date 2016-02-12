/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import edu.iris.wss.framework.FdsnStatus;
import edu.iris.wss.framework.RequestInfo;
import edu.iris.wss.framework.ServiceShellException;
import edu.iris.wss.framework.Util;
import javax.ws.rs.core.Response;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author mike
 */
public class UtilTest {
    public UtilTest() {
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
    public void test_FileNameBase() {
        System.out.println("* ------------------------- base: "
        + Util.getWssFileNameBase("/geows-uf/aaa/bbb/cccc/ddddd"));
    }

    @Test
    public void test_ServicShell_createMsg() {
        Exception ex = new Exception("an exception message");
        String briefMsg = "No type defined or unknown query parameter: formatx";
        String detailedMsg = ex.getMessage();
        String requestURL = "http://cube1:8092/fdsnwsbeta/station/1/staquery";
        String queryString = "latitude=-56.1&longitude=-26.7&maxradius=180&nodata=404&formatx=text";
        String appName = "fdsnwsbeta-station";
        String appVersion = "1.1.20_wss2x";

        FdsnStatus.Status status = FdsnStatus.Status.BAD_REQUEST;

        System.out.println("* ------------------------- err msg: \n"
        + ServiceShellException.createFdsnErrorMsg(status, briefMsg,
              detailedMsg, requestURL, queryString, appName, appVersion));


        System.out.println("* ------------------------- BR: " + FdsnStatus.Status.BAD_REQUEST);
        System.out.println("* ------------------------- BR.gr: " + FdsnStatus.Status.BAD_REQUEST.getReasonPhrase());
        System.out.println("* ------------------------- BR.ts: " + FdsnStatus.Status.BAD_REQUEST.toString());
        System.out.println("* ------------------------- BR.nm: " + FdsnStatus.Status.BAD_REQUEST.name());
        System.out.println("* ------------------------- BR.fa: " + FdsnStatus.Status.BAD_REQUEST.getFamily());
        System.out.println("* ------------------------- BR.sc: " + FdsnStatus.Status.BAD_REQUEST.getStatusCode());
    }
}
