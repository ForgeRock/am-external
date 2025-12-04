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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.assertion.impl.IssuerImpl;

public class LogoutResponseImplTest {

    public static Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<samlp:LogoutResponse xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" " +
                        "ID=\"testID\" InResponseTo=\"testInResponseTo\" IssueInstant=\"5138-11-16T09:46:40Z\" " +
                        "Version=\"2.0\">" +
                        "<saml:Issuer xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">testIssuer</saml:Issuer>" +
                        "<samlp:Status><samlp:StatusCode Value=\"testValue\"/></samlp:Status></samlp:LogoutResponse>" },
                { true, false, "<samlp:LogoutResponse ID=\"testID\" InResponseTo=\"testInResponseTo\" " +
                        "IssueInstant=\"5138-11-16T09:46:40Z\" Version=\"2.0\">" +
                        "<saml:Issuer>testIssuer</saml:Issuer>" +
                        "<samlp:Status><samlp:StatusCode Value=\"testValue\"/></samlp:Status></samlp:LogoutResponse>" },
                { false, false, "<LogoutResponse ID=\"testID\" InResponseTo=\"testInResponseTo\" " +
                        "IssueInstant=\"5138-11-16T09:46:40Z\" Version=\"2.0\">" +
                        "<Issuer>testIssuer</Issuer>" +
                        "<Status><StatusCode Value=\"testValue\"/></Status></LogoutResponse>" }
        };
    }

    @ParameterizedTest
    @MethodSource("xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        LogoutResponseImpl logoutResponse = new LogoutResponseImpl();
        logoutResponse.setID("testID");
        logoutResponse.setInResponseTo("testInResponseTo");
        logoutResponse.setVersion("2.0");
        Issuer issuer = new IssuerImpl();
        issuer.setValue("testIssuer");
        logoutResponse.setIssuer(issuer);
        logoutResponse.setIssueInstant(new Date(100000000000000L));
        StatusImpl status = new StatusImpl();
        StatusCodeImpl statusCode = new StatusCodeImpl();
        statusCode.setValue("testValue");
        status.setStatusCode(statusCode);
        logoutResponse.setStatus(status);

        // When
        String xml = logoutResponse.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}
