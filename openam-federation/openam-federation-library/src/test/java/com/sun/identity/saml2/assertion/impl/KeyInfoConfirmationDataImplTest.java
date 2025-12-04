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

import com.sun.identity.saml2.common.SAML2Constants;

public class KeyInfoConfirmationDataImplTest {

    public static Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<saml:KeyInfoConfirmationData xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "Address=\"TestAddress\" " +
                        "InResponseTo=\"TestInResponseTo\" " +
                        "NotBefore=\"1970-01-01T00:16:40Z\" " +
                        "NotOnOrAfter=\"1970-01-24T03:33:20Z\" " +
                        "Recipient=\"TestRecipient\" " +
                        "xsi:type=\"test\" >" +
                        "<ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">a</ds:KeyInfo>" +
                        "<ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">b</ds:KeyInfo>" +
                        "</saml:KeyInfoConfirmationData>" },
                { true, false, "<saml:KeyInfoConfirmationData " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "Address=\"TestAddress\" " +
                        "InResponseTo=\"TestInResponseTo\" " +
                        "NotBefore=\"1970-01-01T00:16:40Z\" " +
                        "NotOnOrAfter=\"1970-01-24T03:33:20Z\" " +
                        "Recipient=\"TestRecipient\" " +
                        "xsi:type=\"test\" >" +
                        "<ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">a</ds:KeyInfo>" +
                        "<ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">b</ds:KeyInfo>" +
                        "</saml:KeyInfoConfirmationData>" },
                { false, false, "<KeyInfoConfirmationData " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "Address=\"TestAddress\" " +
                        "InResponseTo=\"TestInResponseTo\" " +
                        "NotBefore=\"1970-01-01T00:16:40Z\" " +
                        "NotOnOrAfter=\"1970-01-24T03:33:20Z\" " +
                        "Recipient=\"TestRecipient\" " +
                        "xsi:type=\"test\" >" +
                        "<ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">a</ds:KeyInfo>" +
                        "<ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">b</ds:KeyInfo>" +
                        "</KeyInfoConfirmationData>" }
        };
    }

    @ParameterizedTest
    @MethodSource("xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        KeyInfoConfirmationDataImpl keyInfoConfirmationData = new KeyInfoConfirmationDataImpl();
        keyInfoConfirmationData.setAddress("TestAddress");
        keyInfoConfirmationData.setContentType("test");
        keyInfoConfirmationData.setNotBefore(new Date(1000000L));
        keyInfoConfirmationData.setNotOnOrAfter(new Date(2000000000L));
        keyInfoConfirmationData.setInResponseTo("TestInResponseTo");
        keyInfoConfirmationData.setRecipient("TestRecipient");
        // TODO: replace these raw XML string fields with something better typed
        keyInfoConfirmationData.setKeyInfo(List.of(
                "<ds:KeyInfo xmlns:ds=\"" + SAML2Constants.NS_XMLSIG + "\">a</ds:KeyInfo>",
                "<ds:KeyInfo xmlns:ds=\"" + SAML2Constants.NS_XMLSIG + "\">b</ds:KeyInfo>"));

        // When
        String xml = keyInfoConfirmationData.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}
