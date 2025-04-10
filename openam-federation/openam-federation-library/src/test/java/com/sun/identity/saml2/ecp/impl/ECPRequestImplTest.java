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

package com.sun.identity.saml2.ecp.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sun.identity.saml2.assertion.impl.IssuerImpl;
import com.sun.identity.saml2.protocol.IDPEntry;
import com.sun.identity.saml2.protocol.IDPList;
import com.sun.identity.saml2.protocol.impl.IDPEntryImpl;
import com.sun.identity.saml2.protocol.impl.IDPListImpl;

public class ECPRequestImplTest {

    public static Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<ecp:Request xmlns:ecp=\"urn:oasis:names:tc:SAML:2.0:profiles:SSO:ecp\" " +
                        "xmlns:soap-env=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        "IsPassive=\"true\" " +
                        "ProviderName=\"testProvider\" " +
                        "soap-env:actor=\"testActor\" " +
                        "soap-env:mustUnderstand=\"true\"" +
                        ">" +
                        "<saml:Issuer xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">testIssuer</saml:Issuer>" +
                        "<samlp:IDPList xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\">" +
                        "<samlp:IDPEntry Loc=\"https://test.example.com\" Name=\"testEntry\" ProviderID=\"urn:test\"/>" +
                        "</samlp:IDPList>" +
                        "</ecp:Request>" },
                { true, false, "<ecp:Request " +
                        "xmlns:soap-env=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        "IsPassive=\"true\" " +
                        "ProviderName=\"testProvider\" " +
                        "soap-env:actor=\"testActor\" " +
                        "soap-env:mustUnderstand=\"true\"" +
                        ">" +
                        "<saml:Issuer>testIssuer</saml:Issuer>" +
                        "<samlp:IDPList>" +
                        "<samlp:IDPEntry Loc=\"https://test.example.com\" Name=\"testEntry\" ProviderID=\"urn:test\"/>" +
                        "</samlp:IDPList>" +
                        "</ecp:Request>" },
                { false, false, "<Request " +
                        "xmlns:soap-env=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        "IsPassive=\"true\" " +
                        "ProviderName=\"testProvider\" " +
                        "soap-env:actor=\"testActor\" " +
                        "soap-env:mustUnderstand=\"true\"" +
                        ">" +
                        "<Issuer>testIssuer</Issuer>" +
                        "<IDPList>" +
                        "<IDPEntry Loc=\"https://test.example.com\" Name=\"testEntry\" ProviderID=\"urn:test\"/>" +
                        "</IDPList>" +
                        "</Request>" }
        };
    }

    @ParameterizedTest
    @MethodSource("xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        ECPRequestImpl request = new ECPRequestImpl();
        request.setActor("testActor");
        request.setIsPassive(true);
        request.setMustUnderstand(true);
        request.setProviderName("testProvider");
        IssuerImpl issuer = new IssuerImpl();
        issuer.setValue("testIssuer");
        request.setIssuer(issuer);
        IDPList idpList = new IDPListImpl();
        IDPEntry entry = new IDPEntryImpl();
        entry.setProviderID("urn:test");
        entry.setLoc("https://test.example.com");
        entry.setName("testEntry");
        idpList.setIDPEntries(List.of(entry));
        request.setIDPList(idpList);

        // When
        String xml = request.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}
