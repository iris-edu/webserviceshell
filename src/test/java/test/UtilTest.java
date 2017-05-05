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
