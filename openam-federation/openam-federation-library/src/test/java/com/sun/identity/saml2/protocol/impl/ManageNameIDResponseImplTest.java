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
 * Copyright 2021-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package com.sun.identity.saml2.protocol.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.assertion.impl.IssuerImpl;

public class ManageNameIDResponseImplTest {

    public static Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<samlp:ManageNameIDResponse xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" " +
                        "Consent=\"testConsent\" Destination=\"https://test.example.com/\" " +
                        "ID=\"testId\" InResponseTo=\"testInResponseTo\" IssueInstant=\"2286-11-20T17:46:40Z\" " +
                        "Version=\"2.0\">" +
                        "<saml:Issuer xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">testIssuer</saml:Issuer>" +
                        "<samlp:Status><samlp:StatusCode Value=\"testCode\"/></samlp:Status>" +
                        "</samlp:ManageNameIDResponse>" },
                { true, false, "<samlp:ManageNameIDResponse " +
                        "Consent=\"testConsent\" Destination=\"https://test.example.com/\" " +
                        "ID=\"testId\" InResponseTo=\"testInResponseTo\" IssueInstant=\"2286-11-20T17:46:40Z\" " +
                        "Version=\"2.0\">" +
                        "<saml:Issuer>testIssuer</saml:Issuer>" +
                        "<samlp:Status><samlp:StatusCode Value=\"testCode\"/></samlp:Status>" +
                        "</samlp:ManageNameIDResponse>" },
                { false, false, "<ManageNameIDResponse " +
                        "Consent=\"testConsent\" Destination=\"https://test.example.com/\" " +
                        "ID=\"testId\" InResponseTo=\"testInResponseTo\" IssueInstant=\"2286-11-20T17:46:40Z\" " +
                        "Version=\"2.0\">" +
                        "<Issuer>testIssuer</Issuer>" +
                        "<Status><StatusCode Value=\"testCode\"/></Status>" +
                        "</ManageNameIDResponse>" }
        };
    }

    @ParameterizedTest
    @MethodSource("xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        ManageNameIDResponseImpl manageNameIDResponse = new ManageNameIDResponseImpl();
        manageNameIDResponse.setID("testId");
        manageNameIDResponse.setInResponseTo("testInResponseTo");
        manageNameIDResponse.setConsent("testConsent");
        manageNameIDResponse.setDestination("https://test.example.com/");
        manageNameIDResponse.setIssueInstant(new Date(10000000000000L));
        manageNameIDResponse.setVersion("2.0");
        Issuer issuer = new IssuerImpl();
        issuer.setValue("testIssuer");
        manageNameIDResponse.setIssuer(issuer);
        StatusImpl status = new StatusImpl();
        StatusCodeImpl statusCode = new StatusCodeImpl();
        statusCode.setValue("testCode");
        status.setStatusCode(statusCode);
        manageNameIDResponse.setStatus(status);

        // When
        String xml = manageNameIDResponse.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}
