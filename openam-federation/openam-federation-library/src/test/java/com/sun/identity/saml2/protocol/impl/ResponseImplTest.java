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
import java.util.List;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.assertion.impl.AssertionImpl;
import com.sun.identity.saml2.assertion.impl.IssuerImpl;
import com.sun.identity.saml2.assertion.impl.NameIDImpl;
import com.sun.identity.saml2.assertion.impl.SubjectImpl;
import com.sun.identity.shared.xml.XMLUtils;

public class ResponseImplTest {

    @Test
    public void shouldEscapeSpecialCharactersInXmlInResponseTo() throws Exception {
        // Given
        String inResponseTo = "foo\" oops=\"bar";
        ResponseImpl response = new ResponseImpl();
        response.setID("test");
        response.setVersion("2.0");
        response.setIssueInstant(new Date());
        response.setStatus(new StatusImpl());
        response.setInResponseTo(inResponseTo);

        // When
        String xml = response.toXMLString(true, true);
        Document doc = XMLUtils.toDOMDocument(xml);

        // Then
        assertThat(doc.getDocumentElement().hasAttribute("oops")).isFalse();
        assertThat(doc.getDocumentElement().getAttribute("InResponseTo")).isEqualTo(inResponseTo);
    }

    @DataProvider
    public Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<samlp:Response xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" " +
                        "ID=\"testID\" InResponseTo=\"testInResponseTo\" " +
                        "IssueInstant=\"2001-09-09T01:46:40Z\" Version=\"2.0\">" +
                        "<saml:Issuer xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">testIssuer</saml:Issuer>" +
                        "<samlp:Status>" +
                        "<samlp:StatusCode Value=\"testCode\"/></samlp:Status>" +
                        "<saml:Assertion xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"testAssertionID\" " +
                        "IssueInstant=\"2001-09-09T01:46:40Z\" Version=\"2.0\">" +
                        "<saml:Issuer>testIssuer</saml:Issuer>" +
                        "<saml:Subject><saml:NameID>testName</saml:NameID></saml:Subject></saml:Assertion>" +
                        "</samlp:Response>" },
                { true, false, "<samlp:Response " +
                        "ID=\"testID\" InResponseTo=\"testInResponseTo\" " +
                        "IssueInstant=\"2001-09-09T01:46:40Z\" Version=\"2.0\">" +
                        "<saml:Issuer>testIssuer</saml:Issuer>" +
                        "<samlp:Status>" +
                        "<samlp:StatusCode Value=\"testCode\"/></samlp:Status>" +
                        "<saml:Assertion ID=\"testAssertionID\" " +
                        "IssueInstant=\"2001-09-09T01:46:40Z\" Version=\"2.0\">" +
                        "<saml:Issuer>testIssuer</saml:Issuer>" +
                        "<saml:Subject><saml:NameID>testName</saml:NameID></saml:Subject></saml:Assertion>" +
                        "</samlp:Response>" },
                { false, false, "<Response " +
                        "ID=\"testID\" InResponseTo=\"testInResponseTo\" " +
                        "IssueInstant=\"2001-09-09T01:46:40Z\" Version=\"2.0\">" +
                        "<Issuer>testIssuer</Issuer>" +
                        "<Status>" +
                        "<StatusCode Value=\"testCode\"/></Status>" +
                        "<Assertion ID=\"testAssertionID\" " +
                        "IssueInstant=\"2001-09-09T01:46:40Z\" Version=\"2.0\">" +
                        "<Issuer>testIssuer</Issuer>" +
                        "<Subject><NameID>testName</NameID></Subject></Assertion>" +
                        "</Response>" }
        };
    }

    @Test(dataProvider = "xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        ResponseImpl response = new ResponseImpl();
        response.setID("testID");
        response.setInResponseTo("testInResponseTo");
        response.setVersion("2.0");
        response.setIssueInstant(new Date(1000000000000L));
        Issuer issuer = new IssuerImpl();
        issuer.setValue("testIssuer");
        response.setIssuer(issuer);
        Assertion assertion = new AssertionImpl();
        assertion.setID("testAssertionID");
        assertion.setVersion("2.0");
        assertion.setIssueInstant(response.getIssueInstant());
        assertion.setIssuer(response.getIssuer());
        SubjectImpl subject = new SubjectImpl();
        NameID nameID = new NameIDImpl();
        nameID.setValue("testName");
        subject.setNameID(nameID);
        assertion.setSubject(subject);
        response.setAssertion(List.of(assertion));
        StatusImpl status = new StatusImpl();
        StatusCodeImpl statusCode = new StatusCodeImpl();
        statusCode.setValue("testCode");
        status.setStatusCode(statusCode);
        response.setStatus(status);

        // When
        String xml = response.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}