/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package com.sun.identity.wsfederation.servlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.utils.CollectionUtils.asList;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ReadListener;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sun.identity.saml2.common.SOAPCommunicator;
import org.forgerock.guice.core.GuiceExtension;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.federation.testutils.TestCaseConfigurationInstance;
import org.forgerock.openam.federation.testutils.TestCaseSessionProvider;
import org.forgerock.openam.utils.CollectionUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.owasp.esapi.ESAPI;

import com.google.common.collect.ImmutableMap;
import com.iplanet.sso.SSOToken;
import com.sun.identity.cot.COTConstants;
import com.sun.identity.cot.CircleOfTrustDescriptor;
import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.xmlsig.AMSignatureProvider;
import com.sun.identity.saml.xmlsig.JKSKeyProvider;
import com.sun.identity.saml.xmlsig.SignatureProvider;
import com.sun.identity.shared.Constants;
import com.sun.identity.wsfederation.common.WSFederationConstants;
import com.sun.identity.wsfederation.plugins.TestIdpDefaultAccountMapper;
import com.sun.identity.wsfederation.plugins.TestWsFedAuthenticator;

@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class ActiveRequestTest {

    @RegisterExtension
    GuiceExtension guiceExtension = new GuiceExtension.Builder()
            .addInstanceBinding(AuditEventPublisher.class,mock(AuditEventPublisher.class))
            .addInstanceBinding(SignatureProvider.class, signatureProvider)
            .addInstanceBinding(SOAPCommunicator.class, mock(SOAPCommunicator.class))
            .build();

    private static CircleOfTrustManager cotManager;

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

    private static AMSignatureProvider signatureProvider;

    @BeforeAll
    static void init() throws Exception {
        signatureProvider = new AMSignatureProvider();
        signatureProvider.initialize(new JKSKeyProvider());
        cotManager = new CircleOfTrustManager();
    }

    @BeforeEach
    void setUp() throws Exception {
        TestCaseConfigurationInstance.resetConfiguration();
        // We are not testing real authentication in the test case, do no real processing and return dummy values.
        TestWsFedAuthenticator.setSsoToken(ssoToken);
        TestIdpDefaultAccountMapper.setNameIdentifier(new NameIdentifier(TEST_USER_ID, null,
                WSFederationConstants.NAMED_CLAIM_TYPES[WSFederationConstants.NAMED_CLAIM_UPN]));
        // Providing an empty value is enough to support this test case.
        Map<String, List<String>> sessionProperties = new HashMap<>(2);
        sessionProperties.put(SessionProvider.AUTH_INSTANT, asList(""));
        TestCaseSessionProvider.setState(ssoToken, TEST_SESSION_ID, TEST_USER_ID, sessionProperties);
    }

    private static Object[][] validSoapRequests() {
        return new Object[][] {
                {"/timestamp", "/wsfed-active-soap-request.xml"},
                {"/no-timestamp", "/wsfed-active-soap-request-no-timestamp.xml"} };
    }

    @ParameterizedTest
    @MethodSource("validSoapRequests")
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
        assertThat(soapErrorOutputStream.toString()).isEmpty();
        verify(httpResponse, never()).setStatus(anyInt());
        verify(httpRequest, times(EXPECTED_NAMES.size())).setAttribute(anyString(), any());
        assertThat(httpRequest.attributes.keySet()).containsAll(EXPECTED_NAMES);
        // Sanity check a set of known values based on the original request.
        for (String key : EXPECTED_VALUES.keySet()) {
            assertThat(httpRequest.attributes).containsKey(key);
            assertThat(httpRequest.attributes.get(key)).isEqualTo(EXPECTED_VALUES.get(key));
        }
    }

    @Test
    void shouldFailDueToExpiredRequest() throws Exception {

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
        assertThat(soapErrorOutputStream.toString()).isNotEmpty();
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

        @Override
        public boolean isFinished() {
            return false;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            // do nothing
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

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            // do nothing
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
