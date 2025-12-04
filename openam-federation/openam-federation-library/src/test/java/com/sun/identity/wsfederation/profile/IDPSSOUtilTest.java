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
 * Copyright 2018-2025 Ping Identity Corporation.
 */
package com.sun.identity.wsfederation.profile;

import static com.sun.identity.cot.COTConstants.ACTIVE;
import static com.sun.identity.wsfederation.profile.IDPSSOUtil.getAuthenticationServiceURL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.federation.testutils.TestCaseConfigurationInstance.configureWsFed;
import static org.forgerock.openam.federation.testutils.TestCaseConfigurationInstance.resetConfiguration;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Date;

import jakarta.servlet.http.HttpServletRequest;

import org.forgerock.guice.core.GuiceTestCase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sun.identity.cot.CircleOfTrustDescriptor;
import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.saml2.assertion.SubjectConfirmationData;
import com.sun.identity.saml2.assertion.impl.SubjectConfirmationDataImpl;
import com.sun.identity.saml2.assertion.impl.SubjectConfirmationImpl;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.protocol.impl.ResponseImpl;
import com.sun.identity.saml2.protocol.impl.StatusCodeImpl;
import com.sun.identity.saml2.protocol.impl.StatusImpl;

public class IDPSSOUtilTest extends GuiceTestCase {

    private static final String TEST_COT = "TestCOT";
    private static CircleOfTrustManager cotManager;
    private static HttpServletRequest request;

    @BeforeAll
    static void init() throws Exception {
        cotManager = new CircleOfTrustManager();
        request = mock(HttpServletRequest.class);

        given(request.getScheme()).willReturn("https");
        given(request.getServerName()).willReturn("am.example.com");
        given(request.getServerPort()).willReturn(443);
    }

    @BeforeEach
    void setUp() throws Exception {
        resetConfiguration();
    }

    private static Object[][] mappingData() {
        return new Object[][]{
                {"testIdpWithNoAuthUrl",
                        "idp-extended.xml",
                        "/sso",
                        "https://am.example.com:443/sso/UI/Login?realm=testIdpWithNoAuthUrl"},
                {"testIdpWithEmptyAuthUrl",
                        "idp-extended-empty-authurl.xml",
                        "/sso",
                        "https://am.example.com:443/sso/UI/Login?realm=testIdpWithEmptyAuthUrl"},
                {"testIdpWithAuthUrl",
                        "idp-extended-with-authurl.xml",
                        "/sso",
                        "https://custom.example.com:443/authurl"},
                {"testIdpWithRootContext",
                        "idp-extended.xml",
                        "",
                        "https://am.example.com:443/UI/Login?realm=testIdpWithRootContext"}
        };
    }

    public static Object[][] xmlMappingForDestinationParamTestingAmpEscaping() {
        return new Object[][] {
                { true, true, "<samlp:Response xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" " +
                        "Destination=\"https://example.com?param=3420GJKDL&amp;sec=394fJDl90\" " +
                        "ID=\"testID\" InResponseTo=\"testInResponseTo\" " +
                        "IssueInstant=\"2001-09-09T01:46:40Z\" Version=\"2.0\">" +
                        "<samlp:Status>" +
                        "<samlp:StatusCode Value=\"testCode\"/></samlp:Status>" +
                        "</samlp:Response>" },
                { true, false, "<samlp:Response " +
                        "Destination=\"https://example.com?param=3420GJKDL&amp;sec=394fJDl90\" " +
                        "ID=\"testID\" InResponseTo=\"testInResponseTo\" " +
                        "IssueInstant=\"2001-09-09T01:46:40Z\" Version=\"2.0\">" +
                        "<samlp:Status>" +
                        "<samlp:StatusCode Value=\"testCode\"/></samlp:Status>" +
                        "</samlp:Response>" },
                { false, false, "<Response " +
                        "Destination=\"https://example.com?param=3420GJKDL&amp;sec=394fJDl90\"" +
                        "ID=\"testID\" InResponseTo=\"testInResponseTo\" " +
                        "IssueInstant=\"2001-09-09T01:46:40Z\" Version=\"2.0\">" +
                        "<Status>" +
                        "<StatusCode Value=\"testCode\"/></Status>" +
                        "</Response>" }
        };
    }

    public static Object[][] xmlMappingForSubjectConfirmationTestingAmpEscaping() {
        return new Object[][] {
                { true, true, "<saml:SubjectConfirmation xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                        "Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">" +
                        "<saml:SubjectConfirmationData " +
                        "Recipient=\"https://testurl.com?param=asd1231&amp;another=232ff2\" " +
                        "/>" +
                        "</saml:SubjectConfirmation>" },
                { true, false, "<saml:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">" +
                        "<saml:SubjectConfirmationData " +
                        "Recipient=\"https://testurl.com?param=asd1231&amp;another=232ff2\" " +
                        "/>" +
                        "</saml:SubjectConfirmation>" },
                { false, false, "<SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">" +
                        "<SubjectConfirmationData " +
                        "Recipient=\"https://testurl.com?param=asd1231&amp;another=232ff2\" " +
                        "/>" +
                        "</SubjectConfirmation>\n" }
        };
    }

    @ParameterizedTest
    @MethodSource("mappingData")
    public void testGetAuthenticationServiceURL(String realm, String extendedMetadataPath, String contextPath,
            String expectedLoginUrl) throws Exception {
        given(request.getContextPath()).willReturn(contextPath);
        // This just keeps the error logging noise down by having a valid COT setup, set a realm to foil the COT cache.
        cotManager.createCircleOfTrust(realm, new CircleOfTrustDescriptor(TEST_COT, realm, ACTIVE));
        configureWsFed(realm, "/wsfedmetadata/idp.xml", "/wsfedmetadata/" + extendedMetadataPath);

        assertThat(getAuthenticationServiceURL(realm, "openam-wsfed-idp", request)).isEqualTo(expectedLoginUrl);
    }

    @ParameterizedTest
    @MethodSource("xmlMappingForDestinationParamTestingAmpEscaping")
    public void testToXmlStringConversionOfAmpersandInDestinationField(
            boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        ResponseImpl response = new ResponseImpl();
        response.setID("testID");
        response.setInResponseTo("testInResponseTo");
        response.setVersion("2.0");
        response.setIssueInstant(new Date(1000000000000L));
        response.setDestination("https://example.com?param=3420GJKDL&sec=394fJDl90");
        StatusImpl status = new StatusImpl();
        StatusCodeImpl statusCode = new StatusCodeImpl();
        statusCode.setValue("testCode");
        status.setStatusCode(statusCode);
        response.setStatus(status);

        // When
        String xml = response.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }

    @ParameterizedTest
    @MethodSource("xmlMappingForSubjectConfirmationTestingAmpEscaping")
    public void testToXmlStringWithEscapedChars(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        SubjectConfirmationImpl subjectConfirmation = new SubjectConfirmationImpl();
        subjectConfirmation.setMethod(SAML2Constants.SUBJECT_CONFIRMATION_METHOD_BEARER);
        SubjectConfirmationData subjectConfirmationData = new SubjectConfirmationDataImpl();
        subjectConfirmationData.setRecipient("https://testurl.com?param=asd1231&another=232ff2");
        subjectConfirmation.setSubjectConfirmationData(subjectConfirmationData);

        // When
        String xml = subjectConfirmation.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}
