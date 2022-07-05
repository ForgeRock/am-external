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
 * Copyright 2021-2022 ForgeRock AS.
 */

package com.sun.identity.saml2.protocol.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Date;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.shared.xml.XMLUtils;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sun.identity.saml2.assertion.impl.NameIDImpl;
import com.sun.identity.saml2.common.SAML2Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ManageNameIDRequestImplTest {

    @DataProvider
    public Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<samlp:ManageNameIDRequest " +
                        "xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" " +
                        "ID=\"testID\" " +
                        "IssueInstant=\"2001-09-09T01:46:40Z\" " +
                        "Version=\"2.0\">" +
                        "<saml:NameID xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                        "Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\" " +
                        "NameQualifier=\"testQualifier\">testValue</saml:NameID>" +
                        "<samlp:NewID>testValue2</samlp:NewID>" +
                        "</samlp:ManageNameIDRequest>" },
                { true, false, "<samlp:ManageNameIDRequest " +
                        "ID=\"testID\" " +
                        "IssueInstant=\"2001-09-09T01:46:40Z\" " +
                        "Version=\"2.0\">" +
                        "<saml:NameID " +
                        "Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\" " +
                        "NameQualifier=\"testQualifier\">testValue</saml:NameID>" +
                        "<samlp:NewID>testValue2</samlp:NewID>" +
                        "</samlp:ManageNameIDRequest>" },
                { false, false, "<ManageNameIDRequest " +
                        "ID=\"testID\" " +
                        "IssueInstant=\"2001-09-09T01:46:40Z\" " +
                        "Version=\"2.0\">" +
                        "<NameID " +
                        "Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\" " +
                        "NameQualifier=\"testQualifier\">testValue</NameID>" +
                        "<NewID>testValue2</NewID></ManageNameIDRequest>" }
        };
    }

    @Test(dataProvider = "xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        ManageNameIDRequestImpl manageNameIDRequest = new ManageNameIDRequestImpl();
        NameIDImpl nameID = new NameIDImpl();
        nameID.setFormat(SAML2Constants.NAMEID_TRANSIENT_FORMAT);
        nameID.setNameQualifier("testQualifier");
        nameID.setValue("testValue");
        manageNameIDRequest.setNameID(nameID);
        manageNameIDRequest.setTerminate(false);
        NewIDImpl newID = new NewIDImpl("testValue2");
        manageNameIDRequest.setNewID(newID);
        manageNameIDRequest.setIssueInstant(new Date(1000000000000L));
        manageNameIDRequest.setID("testID");
        manageNameIDRequest.setVersion("2.0");

        // When
        String xml = manageNameIDRequest.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }

    @Test
    public void shouldFailParsingGivenIdContainingScript() {
        // Given
        String id = "&lt;script&gt;alert&lt;/script&gt;";
        Document doc = XMLUtils.toDOMDocument(createSimpleScriptWithId(id));
        Element element = doc.getDocumentElement();
        /* note special characters e.g '<' will survive this transformation as long as they are escaped in the input */

        // Then
        assertThatThrownBy(() -> new ManageNameIDRequestImpl(element))
            .isInstanceOf(SAML2Exception.class)
            .hasMessage("ID Attribute is not present in the Authentication Request message.");
    }

    private String createSimpleScriptWithId(String id) {
        return "<ManageNameIDRequest ID=\"" + id + "\" Version=\"2.0\" IssueInstant=\"1999-05-31T13:20:00-05:00\">" +
            "</ManageNameIDRequest>";
    }
}