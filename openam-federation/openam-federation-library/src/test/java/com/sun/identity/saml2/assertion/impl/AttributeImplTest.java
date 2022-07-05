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
import java.util.Map;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class AttributeImplTest {

    @DataProvider
    public Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<saml:Attribute xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                        "FriendlyName=\"friendly-test\" Name=\"test\" NameFormat=\"testFormat\" " +
                        "a=\"b\" c=\"d\">" +
                        "<saml:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "xsi:type=\"xs:string\">a</saml:AttributeValue><saml:AttributeValue " +
                        "xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "xsi:type=\"xs:string\">b</saml:AttributeValue>" +
                        "<saml:AttributeValue " +
                        "xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "xsi:type=\"xs:string\">c</saml:AttributeValue></saml:Attribute>" },
                { true, false, "<saml:Attribute " +
                        "FriendlyName=\"friendly-test\" Name=\"test\" NameFormat=\"testFormat\" a=\"b\" c=\"d\">" +
                        "<saml:AttributeValue xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                        "xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "xsi:type=\"xs:string\">a</saml:AttributeValue>" +
                        "<saml:AttributeValue xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                        "xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "xsi:type=\"xs:string\">b</saml:AttributeValue>" +
                        "<saml:AttributeValue xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                        "xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "xsi:type=\"xs:string\">c</saml:AttributeValue></saml:Attribute>" },
                { false, false, "<Attribute FriendlyName=\"friendly-test\" Name=\"test\" NameFormat=\"testFormat\" " +
                        "a=\"b\" c=\"d\">" +
                        "<saml:AttributeValue xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                        "xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "xsi:type=\"xs:string\">a</saml:AttributeValue>" +
                        "<saml:AttributeValue xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                        "xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "xsi:type=\"xs:string\">b</saml:AttributeValue>" +
                        "<saml:AttributeValue xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                        "xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "xsi:type=\"xs:string\">c</saml:AttributeValue></Attribute>" }
        };
    }

    @Test(dataProvider = "xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        AttributeImpl attribute = new AttributeImpl();
        attribute.setName("test");
        attribute.setFriendlyName("friendly-test");
        attribute.setNameFormat("testFormat");
        attribute.setAnyAttribute(Map.of("a", "b", "c", "d"));
        attribute.setAttributeValueString(List.of("a", "b", "c"));

        // When
        String xml = attribute.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}