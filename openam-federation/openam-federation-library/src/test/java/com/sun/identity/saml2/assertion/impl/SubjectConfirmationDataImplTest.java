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

package com.sun.identity.saml2.assertion.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;

import com.sun.identity.shared.xml.XMLUtils;

public class SubjectConfirmationDataImplTest {

    @Test
    void shouldEscapeSpecialCharactersInXmlInResponseTo() throws Exception {
        // Given
        String inResponseTo = "foo\" oops=\"bar";
        SubjectConfirmationDataImpl subjectConfirmationData = new SubjectConfirmationDataImpl();
        subjectConfirmationData.setInResponseTo(inResponseTo);

        // When
        String xml = subjectConfirmationData.toXMLString(true, true);
        Document doc = XMLUtils.toDOMDocument(xml);

        // Then
        assertThat(doc.getDocumentElement().hasAttribute("oops")).isFalse();
        assertThat(doc.getDocumentElement().getAttribute("InResponseTo")).isEqualTo(inResponseTo);
    }

    public static Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<saml:SubjectConfirmationData xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "Address=\"TestAddress\" " +
                        "InResponseTo=\"TestInResponseTo\" " +
                        "NotBefore=\"1970-01-01T00:16:40Z\" " +
                        "NotOnOrAfter=\"1970-01-03T07:33:20Z\" " +
                        "Recipient=\"TestRecipient\" " +
                        "xsi:type=\"test\"><a/><b/></saml:SubjectConfirmationData>" },
                { true, false, "<saml:SubjectConfirmationData " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "Address=\"TestAddress\" " +
                        "InResponseTo=\"TestInResponseTo\" " +
                        "NotBefore=\"1970-01-01T00:16:40Z\" " +
                        "NotOnOrAfter=\"1970-01-03T07:33:20Z\" " +
                        "Recipient=\"TestRecipient\" " +
                        "xsi:type=\"test\"><a/><b/></saml:SubjectConfirmationData>" },
                { false, false, "<SubjectConfirmationData " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "Address=\"TestAddress\" " +
                        "InResponseTo=\"TestInResponseTo\" " +
                        "NotBefore=\"1970-01-01T00:16:40Z\" " +
                        "NotOnOrAfter=\"1970-01-03T07:33:20Z\" " +
                        "Recipient=\"TestRecipient\" " +
                        "xsi:type=\"test\"><a/><b/></SubjectConfirmationData>" }
        };
    }


    @ParameterizedTest
    @MethodSource("xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        SubjectConfirmationDataImpl subjectConfirmationData = new SubjectConfirmationDataImpl();
        subjectConfirmationData.setInResponseTo("TestInResponseTo");
        subjectConfirmationData.setAddress("TestAddress");
        subjectConfirmationData.setContentType("test");
        subjectConfirmationData.setRecipient("TestRecipient");
        subjectConfirmationData.setNotBefore(new Date(1000000L));
        subjectConfirmationData.setNotOnOrAfter(new Date(200000000L));
        subjectConfirmationData.setContent(List.of("<a/>", "<b/>"));

        // When
        String xml = subjectConfirmationData.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}
