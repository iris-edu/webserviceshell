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

package edu.iris.wss.framework;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.fail;
import static org.junit.Assert.fail;

/**
 *
 * @author mike
 */
public class RequestInfo_2_service_file_Test {
    public RequestInfo_2_service_file_Test() {
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
    public void testLoadOfFormatTypes() throws Exception {
        AppConfigurator appCfg =
              AppConfigurator_getters_Test.createTestObjAppCfg("META-INF/service.cfg");
        RequestInfo ri = new RequestInfo(appCfg);

        // test for default
        // endpoint name is taken from the service.cfg file
        String endpointName = "dummyEP";
        assert(ri.getPerRequestMediaType(endpointName).equals("application/vnd.fdsn.mseed"));

        // Note, these tests are determined by the values in service.cfg
        ri.setPerRequestFormatType(endpointName, "xml");
        assert(ri.getPerRequestMediaType(endpointName).equals("application/xml"));
        ri.setPerRequestFormatType(endpointName, "xMl");
        assert(ri.getPerRequestMediaType(endpointName).equals("application/xml"));
        ri.setPerRequestFormatType(endpointName, "text");
        assert(ri.getPerRequestMediaType(endpointName).equals("text/plain"));
        ri.setPerRequestFormatType(endpointName, "texttree");
        assert(ri.getPerRequestMediaType(endpointName).equals("text/plain"));
        ri.setPerRequestFormatType(endpointName, "json");
        assert(ri.getPerRequestMediaType(endpointName).equals("application/json"));

        ri.setPerRequestFormatType(endpointName, "miniseed");
        assert(ri.getPerRequestMediaType(endpointName).equals("application/vnd.fdsn.mseed"));
        ri.setPerRequestFormatType(endpointName, "miniseed ");
        assert(ri.getPerRequestMediaType(endpointName).equals("application/vnd.fdsn.mseed"));
        ri.setPerRequestFormatType(endpointName, " Miniseed");
        assert(ri.getPerRequestMediaType(endpointName).equals("application/vnd.fdsn.mseed"));
        ri.setPerRequestFormatType(endpointName, "    minisEed ");
        assert(ri.getPerRequestMediaType(endpointName).equals("application/vnd.fdsn.mseed"));

        ri.setPerRequestFormatType(endpointName, "mseed");
        assert(ri.getPerRequestMediaType(endpointName).equals("application/vnd.fdsn.mseed"));
        ri.setPerRequestFormatType(endpointName, "binary");
        assert(ri.getPerRequestMediaType(endpointName).equals("application/octet-stream"));
    }

    @Test
    public void testLoadExceptionOfFormatTypes() throws Exception {
        AppConfigurator appCfg =
              AppConfigurator_getters_Test.createTestObjAppCfg("META-INF/service.cfg");
        RequestInfo ri = new RequestInfo(appCfg);

        // endpoint name is taken from the service.cfg file
        String endpointName = "dummyEP";

        try {
            ri.setPerRequestFormatType(endpointName, null);
            fail("getting null type succeeded unexpectedly,"
                    + " should have had an Exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }

        try {
            ri.setPerRequestFormatType(endpointName, "unknown2");
            fail("getting unknown2 type succeeded unexpectedly,"
                    + " should have had an Exception");
        } catch (Exception ex) {
            // noop - this is expected result
        }
    }

    @Test
    public void testThatSlashOnAfterEndpointNameFails() throws Exception {
        AppConfigurator appCfg =
              AppConfigurator_getters_Test.createTestObjAppCfg("META-INF/service.cfg");
        RequestInfo ri = new RequestInfo(appCfg);

        // endpoint name is taken from the service.cfg file
        String endpointName = "dummyEP";
        String contextPath = "/geows-uf/intermagnet/1";
        String requestURI = contextPath + "/" + endpointName;

        HttpServletRequest req = new MockHttpReq (contextPath, requestURI);
        assert(ri.isThisEndpointConfigured(req, appCfg) == true);

        req = new MockHttpReq (contextPath, requestURI + "/");
        assert(ri.isThisEndpointConfigured(req, appCfg) == false);
    }

    private class MockHttpReq implements HttpServletRequest {
        private String contextPath;
        private String requestURI;

        public MockHttpReq(String contextPath, String requestURI) {
            this.contextPath = contextPath;
            this.requestURI = requestURI;
        }

        @Override
        public String getAuthType() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Cookie[] getCookies() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public long getDateHeader(String string) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getHeader(String string) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Enumeration<String> getHeaders(String string) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getIntHeader(String string) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getMethod() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getPathInfo() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getPathTranslated() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getContextPath() {
            return contextPath;
        }

        @Override
        public String getQueryString() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getRemoteUser() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isUserInRole(String string) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Principal getUserPrincipal() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getRequestedSessionId() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getRequestURI() {
            return requestURI;
        }

        @Override
        public StringBuffer getRequestURL() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getServletPath() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public HttpSession getSession(boolean bln) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public HttpSession getSession() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isRequestedSessionIdValid() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isRequestedSessionIdFromCookie() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isRequestedSessionIdFromURL() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isRequestedSessionIdFromUrl() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean authenticate(HttpServletResponse hsr) throws IOException, ServletException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void login(String string, String string1) throws ServletException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void logout() throws ServletException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Collection<Part> getParts() throws IOException, ServletException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Part getPart(String string) throws IOException, ServletException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Object getAttribute(String string) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Enumeration<String> getAttributeNames() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getCharacterEncoding() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setCharacterEncoding(String string) throws UnsupportedEncodingException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getContentLength() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getContentType() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getParameter(String string) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Enumeration<String> getParameterNames() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String[] getParameterValues(String string) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getProtocol() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getScheme() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getServerName() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getServerPort() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public BufferedReader getReader() throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getRemoteAddr() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getRemoteHost() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setAttribute(String string, Object o) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void removeAttribute(String string) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Locale getLocale() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Enumeration<Locale> getLocales() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isSecure() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public RequestDispatcher getRequestDispatcher(String string) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getRealPath(String string) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getRemotePort() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getLocalName() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getLocalAddr() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int getLocalPort() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ServletContext getServletContext() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public AsyncContext startAsync() throws IllegalStateException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public AsyncContext startAsync(ServletRequest sr, ServletResponse sr1) throws IllegalStateException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isAsyncStarted() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isAsyncSupported() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public AsyncContext getAsyncContext() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public DispatcherType getDispatcherType() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
