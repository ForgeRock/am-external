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
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.assertion.impl.AttributeImpl;
import com.sun.identity.saml2.assertion.impl.IssuerImpl;
import com.sun.identity.saml2.assertion.impl.NameIDImpl;
import com.sun.identity.saml2.assertion.impl.SubjectImpl;
import com.sun.identity.saml2.common.SAML2Constants;

public class AttributeQueryImplTest {

    public static Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<samlp:AttributeQuery xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" " +
                        "xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                        "Consent=\"testConsent\" " +
                        "Destination=\"https://test.example.com/\" " +
                        "ID=\"testID\" " +
                        "IssueInstant=\"2009-02-13T23:31:30Z\" " +
                        "Version=\"2.0\">" +
                        "<saml:Issuer NameQualifier=\"testNameQualifier\">testIssuer</saml:Issuer>" +
                        "<samlp:Extensions><samlp:Extension>foo</samlp:Extension></samlp:Extensions>" +
                        "<saml:Subject>" +
                        "<saml:NameID " +
                        "Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\">testUser</saml:NameID>" +
                        "</saml:Subject>" +
                        "<saml:Attribute FriendlyName=\"testFriendlyName\" Name=\"testAttr\">" +
                        "<saml:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "xsi:type=\"xs:string\">a</saml:AttributeValue>" +
                        "<saml:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "xsi:type=\"xs:string\">b</saml:AttributeValue></saml:Attribute>" +
                        "</samlp:AttributeQuery>" },
                { true, false, "<samlp:AttributeQuery " +
                        "Consent=\"testConsent\" " +
                        "Destination=\"https://test.example.com/\" " +
                        "ID=\"testID\" " +
                        "IssueInstant=\"2009-02-13T23:31:30Z\" " +
                        "Version=\"2.0\">" +
                        "<saml:Issuer NameQualifier=\"testNameQualifier\">testIssuer</saml:Issuer>" +
                        "<samlp:Extensions>" +
                        "<samlp:Extension xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\">foo</samlp:Extension>" +
                        "</samlp:Extensions>" +
                        "<saml:Subject>" +
                        "<saml:NameID " +
                        "Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\">testUser</saml:NameID>" +
                        "</saml:Subject>" +
                        "<saml:Attribute FriendlyName=\"testFriendlyName\" Name=\"testAttr\">" +
                        "<saml:AttributeValue xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                        "xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "xsi:type=\"xs:string\">a</saml:AttributeValue>" +
                        "<saml:AttributeValue xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                        "xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "xsi:type=\"xs:string\">b</saml:AttributeValue></saml:Attribute>" +
                        "</samlp:AttributeQuery>" },
                { false, false, "<AttributeQuery " +
                        "Consent=\"testConsent\" " +
                        "Destination=\"https://test.example.com/\" " +
                        "ID=\"testID\" " +
                        "IssueInstant=\"2009-02-13T23:31:30Z\" " +
                        "Version=\"2.0\">" +
                        "<Issuer NameQualifier=\"testNameQualifier\">testIssuer</Issuer>" +
                        "<Extensions>" +
                        "<samlp:Extension xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\">foo</samlp:Extension>" +
                        "</Extensions>" +
                        "<Subject>" +
                        "<NameID Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\">testUser</NameID>" +
                        "</Subject>" +
                        "<Attribute FriendlyName=\"testFriendlyName\" Name=\"testAttr\">" +
                        "<saml:AttributeValue xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                        "xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "xsi:type=\"xs:string\">a</saml:AttributeValue>" +
                        "<saml:AttributeValue xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                        "xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "xsi:type=\"xs:string\">b</saml:AttributeValue></Attribute></AttributeQuery>" }
        };
    }

    @ParameterizedTest
    @MethodSource("xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        AttributeQueryImpl attributeQuery = new AttributeQueryImpl();
        attributeQuery.setID("testID");
        attributeQuery.setConsent("testConsent");
        attributeQuery.setDestination("https://test.example.com/");
        Issuer issuer = new IssuerImpl();
        issuer.setValue("testIssuer");
        issuer.setNameQualifier("testNameQualifier");
        attributeQuery.setIssuer(issuer);
        attributeQuery.setIssueInstant(new Date(1234567890123L));
        attributeQuery.setVersion("2.0");
        Subject subject = new SubjectImpl();
        NameID nameID = new NameIDImpl();
        nameID.setFormat(SAML2Constants.NAMEID_TRANSIENT_FORMAT);
        nameID.setValue("testUser");
        subject.setNameID(nameID);
        attributeQuery.setSubject(subject);
        ExtensionsImpl extensions = new ExtensionsImpl();
        extensions.setAny(List.of("<samlp:Extension>foo</samlp:Extension>"));
        attributeQuery.setExtensions(extensions);
        AttributeImpl attribute = new AttributeImpl();
        attribute.setAttributeValueString(List.of("a", "b"));
        attribute.setName("testAttr");
        attribute.setFriendlyName("testFriendlyName");
        attributeQuery.setAttributes(List.of(attribute));

        // When
        String xml = attributeQuery.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}
