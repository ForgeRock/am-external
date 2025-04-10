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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;

import com.sun.identity.shared.xml.XMLUtils;

public class ArtifactResponseImplTest {

    @Test
    void shouldEscapeSpecialCharactersInXmlInResponseTo() throws Exception {
        // Given
        String inResponseTo = "foo\" oops=\"bar";
        ArtifactResponseImpl artifactResponse = new ArtifactResponseImpl();
        artifactResponse.setID("test");
        artifactResponse.setVersion("2.0");
        artifactResponse.setIssueInstant(new Date());
        artifactResponse.setStatus(new StatusImpl());
        artifactResponse.setInResponseTo(inResponseTo);

        // When
        String xml = artifactResponse.toXMLString(true, true);
        Document doc = XMLUtils.toDOMDocument(xml);

        // Then
        assertThat(doc.getDocumentElement().hasAttribute("oops")).isFalse();
        assertThat(doc.getDocumentElement().getAttribute("InResponseTo")).isEqualTo(inResponseTo);
    }

    public static Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<samlp:ArtifactResponse xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" " +
                        "ID=\"test\" InResponseTo=\"testInResponseTo\" " +
                        "IssueInstant=\"2286-11-20T17:46:40Z\" Version=\"2.0\"" +
                        "><samlp:Status>" +
                        "<samlp:StatusCode Value=\"testCode\"/></samlp:Status>" +
                        "<saml:test xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"/><foo>test</foo>" +
                        "</samlp:ArtifactResponse>" },
                { true, false, "<samlp:ArtifactResponse " +
                        "ID=\"test\" InResponseTo=\"testInResponseTo\" " +
                        "IssueInstant=\"2286-11-20T17:46:40Z\" Version=\"2.0\"" +
                        "><samlp:Status>" +
                        "<samlp:StatusCode Value=\"testCode\"/></samlp:Status>" +
                        "<saml:test xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"/><foo>test</foo>" +
                        "</samlp:ArtifactResponse>" },
                { false, false, "<ArtifactResponse " +
                        "ID=\"test\" InResponseTo=\"testInResponseTo\" " +
                        "IssueInstant=\"2286-11-20T17:46:40Z\" Version=\"2.0\"" +
                        "><Status><StatusCode Value=\"testCode\"/></Status>" +
                        "<saml:test xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"/><foo>test</foo>" +
                        "</ArtifactResponse>" }
        };
    }

    @ParameterizedTest
    @MethodSource("xmlTestCases")
    public void testToXml(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        ArtifactResponseImpl artifactResponse = new ArtifactResponseImpl();
        artifactResponse.setID("test");
        artifactResponse.setVersion("2.0");
        artifactResponse.setIssueInstant(new Date(10000000000000L));
        StatusImpl status = new StatusImpl();
        StatusCodeImpl statusCode = new StatusCodeImpl();
        statusCode.setValue("testCode");
        status.setStatusCode(statusCode);
        artifactResponse.setStatus(status);
        artifactResponse.setInResponseTo("testInResponseTo");
        artifactResponse.setAny("<saml:test/><foo>test</foo>");

        // When
        String xml = artifactResponse.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}
