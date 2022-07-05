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

package com.sun.identity.saml2.protocol.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.assertion.impl.NameIDImpl;
import com.sun.identity.shared.xml.XMLUtils;

public class NameIDMappingResponseImplTest {
    @Test
    public void shouldEscapeSpecialCharactersInXmlInResponseTo() throws Exception {
        // Given
        String inResponseTo = "foo\" oops=\"bar";
        NameIDMappingResponseImpl nameIDMappingResponse = new NameIDMappingResponseImpl();
        NameID nameID = new NameIDImpl();
        nameID.setValue("test");
        nameIDMappingResponse.setNameID(nameID);
        StatusImpl status = new StatusImpl();
        StatusCodeImpl statusCode = new StatusCodeImpl();
        statusCode.setValue("testCode");
        status.setStatusCode(statusCode);
        nameIDMappingResponse.setStatus(status);
        nameIDMappingResponse.setIssueInstant(new Date());
        nameIDMappingResponse.setInResponseTo(inResponseTo);

        // When
        String xml = nameIDMappingResponse.toXMLString(true, true);
        Document doc = XMLUtils.toDOMDocument(xml);

        // Then
        assertThat(doc.getDocumentElement().hasAttribute("oops")).isFalse();
        assertThat(doc.getDocumentElement().getAttribute("InResponseTo")).isEqualTo(inResponseTo);
    }

    @DataProvider
    public Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<samlp:NameIDMappingResponse xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" " +
                        "ID=\"testID\" InResponseTo=\"testInResponseTo\" IssueInstant=\"2286-11-20T17:46:40Z\" " +
                        "Version=\"2.0\"" +
                        ">" +
                        "<saml:NameID xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">test</saml:NameID>" +
                        "<samlp:Status><samlp:StatusCode Value=\"testCode\"/></samlp:Status>" +
                        "</samlp:NameIDMappingResponse>" },
                { true, false, "<samlp:NameIDMappingResponse ID=\"testID\" InResponseTo=\"testInResponseTo\" " +
                        "IssueInstant=\"2286-11-20T17:46:40Z\" Version=\"2.0\">" +
                        "<saml:NameID>test</saml:NameID>" +
                        "<samlp:Status><samlp:StatusCode Value=\"testCode\"/></samlp:Status>" +
                        "</samlp:NameIDMappingResponse>" },
                { false, false, "<NameIDMappingResponse ID=\"testID\" InResponseTo=\"testInResponseTo\" " +
                        "IssueInstant=\"2286-11-20T17:46:40Z\" Version=\"2.0\">" +
                        "<NameID>test</NameID>" +
                        "<Status><StatusCode Value=\"testCode\"/></Status>" +
                        "</NameIDMappingResponse>" }
        };
    }

    @Test(dataProvider = "xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        NameIDMappingResponseImpl response = new NameIDMappingResponseImpl();
        NameID nameID = new NameIDImpl();
        nameID.setValue("test");
        response.setNameID(nameID);
        response.setID("testID");
        response.setVersion("2.0");
        StatusImpl status = new StatusImpl();
        StatusCodeImpl statusCode = new StatusCodeImpl();
        statusCode.setValue("testCode");
        status.setStatusCode(statusCode);
        response.setStatus(status);
        response.setIssueInstant(new Date(10000000000000L));
        response.setInResponseTo("testInResponseTo");

        // When
        String xml = response.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}