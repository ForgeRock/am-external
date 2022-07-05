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
 * Copyright 2021 ForgeRock AS.
 */

package com.sun.identity.saml2.assertion.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class AdviceImplTest {

    @DataProvider
    public Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<saml:Advice xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">" +
                        "<saml:AssertionIDRef>testIDRef</saml:AssertionIDRef>" +
                        "<saml:AssertionURIRef>urn:a</saml:AssertionURIRef>" +
                        "<saml:AssertionURIRef>urn:b</saml:AssertionURIRef>" +
                        "<EncryptedAssertion>test</EncryptedAssertion><test>foo</test>" +
                        "</saml:Advice>" },
                { true, false, "<saml:Advice>" +
                        "<saml:AssertionIDRef>testIDRef</saml:AssertionIDRef>" +
                        "<saml:AssertionURIRef>urn:a</saml:AssertionURIRef>" +
                        "<saml:AssertionURIRef>urn:b</saml:AssertionURIRef>" +
                        "<EncryptedAssertion>test</EncryptedAssertion><test>foo</test>" +
                        "</saml:Advice>" },
                { false, false, "<Advice>" +
                        "<AssertionIDRef>testIDRef</AssertionIDRef>" +
                        "<AssertionURIRef>urn:a</AssertionURIRef>" +
                        "<AssertionURIRef>urn:b</AssertionURIRef>" +
                        "<EncryptedAssertion>test</EncryptedAssertion><test>foo</test>" +
                        "</Advice>" }
        };
    }

    @Test(dataProvider = "xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        AdviceImpl advice = new AdviceImpl();
        AssertionIDRefImpl assertionIDRef = new AssertionIDRefImpl();
        assertionIDRef.setValue("testIDRef");
        advice.setAssertionIDRefs(List.of(assertionIDRef));
        advice.setAssertionURIRefs(List.of("urn:a", "urn:b"));
        EncryptedAssertionImpl encryptedAssertion = new EncryptedAssertionImpl(
                "<EncryptedAssertion>test</EncryptedAssertion>");
        advice.setEncryptedAssertions(List.of(encryptedAssertion));
        advice.setAdditionalInfo(List.of("<test>foo</test>"));

        // When
        String xml = advice.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}