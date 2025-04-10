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
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.assertion.impl.IssuerImpl;
import com.sun.identity.saml2.assertion.impl.NameIDImpl;
import com.sun.identity.saml2.common.SAML2Constants;

public class LogoutRequestImplTest {

    public static Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<samlp:LogoutRequest " +
                        "xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" " +
                        "Consent=\"testConsent\" " +
                        "Destination=\"https://test.example.com/\" " +
                        "ID=\"testID\" " +
                        "IssueInstant=\"2021-06-02T09:29:37Z\" " +
                        "NotOnOrAfter=\"2021-06-02T09:29:37Z\" " +
                        "Reason=\"idle timeout\"" +
                        "Version=\"2.0\" " +
                        "><saml:Issuer xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">testIssuer</saml:Issuer>" +
                        "<saml:NameID xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                        "Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\" " +
                        "NameQualifier=\"testQualifier\">testName</saml:NameID>" +
                        "<samlp:SessionIndex>testSessionIdx" +
                        "</samlp:SessionIndex></samlp:LogoutRequest>" },
                { true, false, "<samlp:LogoutRequest " +
                        "Consent=\"testConsent\" " +
                        "Destination=\"https://test.example.com/\" " +
                        "ID=\"testID\" " +
                        "IssueInstant=\"2021-06-02T09:29:37Z\" " +
                        "NotOnOrAfter=\"2021-06-02T09:29:37Z\" " +
                        "Reason=\"idle timeout\"" +
                        "Version=\"2.0\" " +
                        "><saml:Issuer>testIssuer</saml:Issuer>" +
                        "<saml:NameID " +
                        "Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\" " +
                        "NameQualifier=\"testQualifier\">testName</saml:NameID>" +
                        "<samlp:SessionIndex>testSessionIdx</samlp:SessionIndex></samlp:LogoutRequest>" },
                { false, false, "<LogoutRequest " +
                        "Consent=\"testConsent\" " +
                        "Destination=\"https://test.example.com/\" " +
                        "ID=\"testID\" " +
                        "IssueInstant=\"2021-06-02T09:29:37Z\" " +
                        "NotOnOrAfter=\"2021-06-02T09:29:37Z\" " +
                        "Reason=\"idle timeout\"" +
                        "Version=\"2.0\" " +
                        "><Issuer>testIssuer</Issuer>" +
                        "<NameID " +
                        "Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\" " +
                        "NameQualifier=\"testQualifier\">testName</NameID>" +
                        "<SessionIndex>testSessionIdx</SessionIndex></LogoutRequest>" }
        };
    }

    @ParameterizedTest
    @MethodSource("xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        LogoutRequestImpl logoutRequest = new LogoutRequestImpl();
        logoutRequest.setID("testID");
        logoutRequest.setConsent("testConsent");
        logoutRequest.setDestination("https://test.example.com/");
        logoutRequest.setIssueInstant(new Date(1622626177000L));
        logoutRequest.setVersion("2.0");
        Issuer issuer = new IssuerImpl();
        issuer.setValue("testIssuer");
        logoutRequest.setIssuer(issuer);
        logoutRequest.setNotOnOrAfter(new Date(1622626177000L));
        logoutRequest.setReason("idle timeout");
        logoutRequest.setSessionIndex(List.of("testSessionIdx"));
        NameID nameID = new NameIDImpl();
        nameID.setValue("testName");
        nameID.setFormat(SAML2Constants.NAMEID_TRANSIENT_FORMAT);
        nameID.setNameQualifier("testQualifier");
        logoutRequest.setNameID(nameID);

        // When
        String xml = logoutRequest.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }

}
