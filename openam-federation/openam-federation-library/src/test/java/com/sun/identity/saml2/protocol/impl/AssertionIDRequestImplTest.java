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

package com.sun.identity.saml2.protocol.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sun.identity.saml2.assertion.AssertionIDRef;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.assertion.impl.AssertionIDRefImpl;
import com.sun.identity.saml2.assertion.impl.IssuerImpl;

public class AssertionIDRequestImplTest {

    public static Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<samlp:AssertionIDRequest xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" " +
                        "xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                        "Consent=\"testConsent\" " +
                        "Destination=\"https://test.example.com/\" " +
                        "ID=\"testID\" " +
                        "IssueInstant=\"2286-11-20T17:46:40Z\" " +
                        "Version=\"2.0\">" +
                        "<saml:Issuer>testIssuer</saml:Issuer>" +
                        "<saml:AssertionIDRef>testRef</saml:AssertionIDRef>" +
                        "</samlp:AssertionIDRequest>" },
                { true, false, "<samlp:AssertionIDRequest " +
                        "Consent=\"testConsent\" " +
                        "Destination=\"https://test.example.com/\" " +
                        "ID=\"testID\" " +
                        "IssueInstant=\"2286-11-20T17:46:40Z\" " +
                        "Version=\"2.0\">" +
                        "<saml:Issuer>testIssuer</saml:Issuer>" +
                        "<saml:AssertionIDRef>testRef</saml:AssertionIDRef>" +
                        "</samlp:AssertionIDRequest>" },
                { false, false, "<AssertionIDRequest " +
                        "Consent=\"testConsent\" " +
                        "Destination=\"https://test.example.com/\" " +
                        "ID=\"testID\" " +
                        "IssueInstant=\"2286-11-20T17:46:40Z\" " +
                        "Version=\"2.0\">" +
                        "<Issuer>testIssuer</Issuer>" +
                        "<AssertionIDRef>testRef</AssertionIDRef>" +
                        "</AssertionIDRequest>" }
        };
    }

    @ParameterizedTest
    @MethodSource("xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        AssertionIDRequestImpl assertionIDRequest = new AssertionIDRequestImpl();
        assertionIDRequest.setID("testID");
        Issuer issuer = new IssuerImpl();
        issuer.setValue("testIssuer");
        assertionIDRequest.setIssuer(issuer);
        assertionIDRequest.setIssueInstant(new Date(10000000000000L));
        assertionIDRequest.setConsent("testConsent");
        assertionIDRequest.setDestination("https://test.example.com/");
        assertionIDRequest.setVersion("2.0");
        AssertionIDRef idRef = new AssertionIDRefImpl();
        idRef.setValue("testRef");
        assertionIDRequest.setAssertionIDRefs(List.of(idRef));

        // When
        String xml = assertionIDRequest.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}
