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
 * Copyright 2021-2025 Ping Identity Corporation.
 */

package com.sun.identity.saml2.assertion.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sun.identity.saml2.assertion.AuthnContext;
import com.sun.identity.saml2.assertion.SubjectLocality;

public class AuthnStatementImplTest {

    public static Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<saml:AuthnStatement xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                        "AuthnInstant=\"1970-01-12T13:46:40Z\" " +
                        "SessionIndex=\"testSession\" " +
                        "SessionNotOnOrAfter=\"1976-05-03T19:33:20Z\">" +
                        "<saml:SubjectLocality " +
                        "Address=\"testAddress\" " +
                        "DNSName=\"foo.example.com\"/>" +
                        "<saml:AuthnContext>" +
                        "<saml:AuthnContextClassRef>testAcr</saml:AuthnContextClassRef>" +
                        "<saml:AuthenticatingAuthority>testAuthority</saml:AuthenticatingAuthority>" +
                        "</saml:AuthnContext></saml:AuthnStatement>" },
                { true, false, "<saml:AuthnStatement " +
                        "AuthnInstant=\"1970-01-12T13:46:40Z\" " +
                        "SessionIndex=\"testSession\" " +
                        "SessionNotOnOrAfter=\"1976-05-03T19:33:20Z\">" +
                        "<saml:SubjectLocality Address=\"testAddress\" DNSName=\"foo.example.com\"/>" +
                        "<saml:AuthnContext>" +
                        "<saml:AuthnContextClassRef>testAcr</saml:AuthnContextClassRef>" +
                        "<saml:AuthenticatingAuthority>testAuthority</saml:AuthenticatingAuthority>" +
                        "</saml:AuthnContext></saml:AuthnStatement>" },
                { false, false, "<AuthnStatement " +
                        "AuthnInstant=\"1970-01-12T13:46:40Z\" " +
                        "SessionIndex=\"testSession\" " +
                        "SessionNotOnOrAfter=\"1976-05-03T19:33:20Z\">" +
                        "<SubjectLocality Address=\"testAddress\" DNSName=\"foo.example.com\"/>" +
                        "<AuthnContext><AuthnContextClassRef>testAcr</AuthnContextClassRef>" +
                        "<AuthenticatingAuthority>testAuthority</AuthenticatingAuthority>" +
                        "</AuthnContext></AuthnStatement>" }
        };
    }

    @ParameterizedTest
    @MethodSource("xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        AuthnStatementImpl authnStatement = new AuthnStatementImpl();
        AuthnContext authnContext = new AuthnContextImpl();
        authnContext.setAuthnContextClassRef("testAcr");
        authnContext.setAuthenticatingAuthority(List.of("testAuthority"));
        authnStatement.setAuthnContext(authnContext);
        authnStatement.setAuthnInstant(new Date(1000000000L));
        authnStatement.setSessionIndex("testSession");
        authnStatement.setSessionNotOnOrAfter(new Date(200000000000L));
        SubjectLocality subjectLocality = new SubjectLocalityImpl();
        subjectLocality.setAddress("testAddress");
        subjectLocality.setDNSName("foo.example.com");
        authnStatement.setSubjectLocality(subjectLocality);

        // When
        String xml = authnStatement.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }

}
