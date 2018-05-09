/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package com.sun.identity.wsfederation.servlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.utils.CollectionUtils.asList;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.forgerock.openam.federation.testutils.TestCaseConfigurationInstance;
import org.forgerock.openam.federation.testutils.TestCaseSessionProvider;
import org.forgerock.openam.utils.CollectionUtils;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.owasp.esapi.ESAPI;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.iplanet.sso.SSOToken;
import com.sun.identity.cot.COTConstants;
import com.sun.identity.cot.CircleOfTrustDescriptor;
import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.plugin.configuration.ConfigurationInstance;
import com.sun.identity.plugin.configuration.ConfigurationManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.shared.Constants;
import com.sun.identity.wsfederation.common.WSFederationConstants;
import com.sun.identity.wsfederation.plugins.TestIdpDefaultAccountMapper;
import com.sun.identity.wsfederation.plugins.TestWsFedAuthenticator;

public class ActiveRequestTest {
    private CircleOfTrustManager cotManager;

    private static final String TEST_SESSION_ID = "TestSSOTokenId";
    private static final String TEST_USER_ID = "TestUser";
    private static final String TEST_COT = "TestCOT";
    private static final String TO = "http://openam.example.com:8080/am/WSFederationServlet/sts/metaAlias/idp";
    private static final String SITE_ID = "instanceID=http://openam.example.com:8080/am" +
            "|siteid=XARFfsIAXeLX8BEWNIJg9Q8r0PE=|issuerName=openam.example.com:8080";

    private static final List<String> EXPECTED_NAMES = CollectionUtils.asList(Constants.WSFED_ACTIVE_LOGIN,
            "inResponseTo", "responseCreated", "responseExpires", "notBefore", "notOnOrAfter", "targetAddress",
            "requestedSecurityToken", "assertionId");
    private static final Map<String, String> EXPECTED_VALUES = ImmutableMap.of(
            "inResponseTo","1234567890",
            "targetAddress", ESAPI.encoder().encodeForXML("urn:federation:MicrosoftOnline"));

    @Spy
    private FakeHttpServletRequest httpRequest;
    @Mock
    private HttpServletResponse httpResponse;
    @Mock
    private RequestDispatcher requestDispatcher;
    @Mock
    private SSOToken ssoToken;

    @BeforeClass
    public void init() throws Exception {
        cotManager = new CircleOfTrustManager();
    }

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        TestCaseConfigurationInstance.resetConfiguration();
        // Set up the expected default org level attributes.
        ConfigurationInstance ci = ConfigurationManager.getConfigurationInstance(SAMLConstants.SAML_SERVICE_NAME);
        Map<String, Set<String>> attributes = new HashMap<>();
        attributes.put(SAMLConstants.SITE_ID_ISSUER_NAME_LIST, CollectionUtils.asSet(SITE_ID));
        attributes.put(SAMLConstants.PARTNER_URLS, CollectionUtils.asSet(""));
        // Using null will set them as org level.
        ci.setConfiguration(null, null, attributes);
        // We are not testing real authentication in the test case, do no real processing and return dummy values.
        TestWsFedAuthenticator.setSsoToken(ssoToken);
        TestIdpDefaultAccountMapper.setNameIdentifier(new NameIdentifier(TEST_USER_ID, null,
                WSFederationConstants.NAMED_CLAIM_TYPES[WSFederationConstants.NAMED_CLAIM_UPN]));
        // Providing an empty value is enough to support this test case.
        Map<String, List<String>> sessionProperties = new HashMap<>(2);
        sessionProperties.put(SessionProvider.AUTH_INSTANT, asList(""));
        TestCaseSessionProvider.setState(ssoToken, TEST_SESSION_ID, TEST_USER_ID, sessionProperties);
    }

    @DataProvider
    private Object[][] validSoapRequests() {
        return new Object[][] {
                {"/timestamp", "/wsfed-active-soap-request.xml"},
                {"/no-timestamp", "/wsfed-active-soap-request-no-timestamp.xml"} };
    }

    @Test(dataProvider = "validSoapRequests")
    public void requestValidationTest(String realm, String soapRequest) throws Exception {

        // Given
        configureRealm(realm, "idp-extended-active.xml");
        ByteArrayOutputStream soapErrorOutputStream = new ByteArrayOutputStream();
        InputStream activeRequestStream = getClass().getResourceAsStream(soapRequest);
        when(httpRequest.getRequestURI()).thenReturn(TO);
        when(httpRequest.getContentLength()).thenReturn(1024);
        when(httpRequest.getInputStream()).thenReturn(new TestServletInputStream(activeRequestStream));
        when(httpRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(httpRequest.getRequestDispatcher("/wsfederation/jsp/activeresponse.jsp")).thenReturn(requestDispatcher);
        when(httpResponse.getOutputStream()).thenReturn(new TestServletOutputStream(soapErrorOutputStream));

        // When
        ActiveRequest activeRequest = new ActiveRequest(httpRequest, httpResponse);
        activeRequest.process();

        // Then
        // If this is not empty then there was a SOAP fault, fail early.
        assertTrue(soapErrorOutputStream.toString().isEmpty());
        verify(httpResponse, never()).setStatus(anyInt());
        verify(httpRequest, times(EXPECTED_NAMES.size())).setAttribute(anyString(), any());
        assertThat(httpRequest.attributes.keySet()).containsAll(EXPECTED_NAMES);
        // Sanity check a set of known values based on the original request.
        for (String key : EXPECTED_VALUES.keySet()) {
            assertEquals(httpRequest.attributes.get(key), EXPECTED_VALUES.get(key));
        }
    }

    @Test
    public void shouldFailDueToExpiredRequest() throws Exception {

        // Given
        configureRealm("Expired", "idp-extended-active.xml");
        ByteArrayOutputStream soapErrorOutputStream = new ByteArrayOutputStream();
        InputStream activeRequestStream = getClass().getResourceAsStream("/wsfed-active-soap-request-expired.xml");
        when(httpRequest.getRequestURI()).thenReturn(TO);
        when(httpRequest.getContentLength()).thenReturn(1024);
        when(httpRequest.getInputStream()).thenReturn(new TestServletInputStream(activeRequestStream));
        when(httpRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(httpResponse.getOutputStream()).thenReturn(new TestServletOutputStream(soapErrorOutputStream));

        // When
        ActiveRequest activeRequest = new ActiveRequest(httpRequest, httpResponse);
        activeRequest.process();

        // Then
        // An expired request should generate a SOAP fault and set the status.
        assertFalse(soapErrorOutputStream.toString().isEmpty());
        verify(httpResponse).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    private void configureRealm(String realm, String idpExtended) throws Exception {
        // This just keeps the error logging noise down by having a valid COT setup, set a realm to foil the COT cache.
        cotManager.createCircleOfTrust(realm, new CircleOfTrustDescriptor(TEST_COT, realm, COTConstants.ACTIVE));
        TestCaseConfigurationInstance.configureWsFed(realm, "/wsfedmetadata/idp.xml", "/wsfedmetadata/" + idpExtended);
    }

    static class TestServletInputStream extends ServletInputStream {

        private InputStream wrappedInputStream;

        private TestServletInputStream(InputStream wrappedInputStream) {
            this.wrappedInputStream = wrappedInputStream;
        }

        @Override
        public int read() throws IOException {
            return wrappedInputStream.read();
        }
    }

    static class TestServletOutputStream extends ServletOutputStream {

        private OutputStream wrappedOutputStream;

        private TestServletOutputStream(OutputStream wrappedOutputStream) {
            this.wrappedOutputStream = wrappedOutputStream;
        }

        @Override
        public void write(int b) throws IOException {
            wrappedOutputStream.write(b);
        }
    }

    // Used to capture the attributes being set on the request, these are used within the
    // /wsfederation/jsp/activeresponse.jsp template when building the final SOAP response.
    static abstract class FakeHttpServletRequest implements HttpServletRequest {
        private final Map<String, Object> attributes = new HashMap<>();

        @Override
        public void setAttribute(String name, Object value) {
            attributes.put(name, value);
        }
    }
}