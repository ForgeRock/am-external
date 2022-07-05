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

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sun.identity.saml2.assertion.BaseID;
import com.sun.identity.saml2.assertion.SubjectConfirmationData;
import com.sun.identity.saml2.common.SAML2Constants;

public class SubjectConfirmationImplTest {

    @DataProvider
    public Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<saml:SubjectConfirmation xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                        "Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">" +
                        "<saml:BaseID NameQualifier=\"testNameQualifier\" " +
                        "SPNameQualifier=\"testSPNameQualifier\"/>" +
                        "<saml:SubjectConfirmationData " +
                        "Address=\"testAddress\" " +
                        "InResponseTo=\"testInResponseTo\" " +
                        "Recipient=\"testRecipient\" " +
                        "/>" +
                        "</saml:SubjectConfirmation>" },
                { true, false, "<saml:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">" +
                        "<saml:BaseID NameQualifier=\"testNameQualifier\" " +
                        "SPNameQualifier=\"testSPNameQualifier\"/>" +
                        "<saml:SubjectConfirmationData " +
                        "Address=\"testAddress\" " +
                        "InResponseTo=\"testInResponseTo\" " +
                        "Recipient=\"testRecipient\" " +
                        "/>" +
                        "</saml:SubjectConfirmation>" },
                { false, false, "<SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">" +
                        "<BaseID NameQualifier=\"testNameQualifier\" SPNameQualifier=\"testSPNameQualifier\"/>" +
                        "<SubjectConfirmationData " +
                        "Address=\"testAddress\" " +
                        "InResponseTo=\"testInResponseTo\" " +
                        "Recipient=\"testRecipient\" " +
                        "/>" +
                        "</SubjectConfirmation>\n" }
        };
    }

    @Test(dataProvider = "xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        SubjectConfirmationImpl subjectConfirmation = new SubjectConfirmationImpl();
        BaseID baseID = new BaseIDImpl();
        baseID.setNameQualifier("testNameQualifier");
        baseID.setSPNameQualifier("testSPNameQualifier");
        subjectConfirmation.setBaseID(baseID);
        subjectConfirmation.setMethod(SAML2Constants.SUBJECT_CONFIRMATION_METHOD_BEARER);
        SubjectConfirmationData subjectConfirmationData = new SubjectConfirmationDataImpl();
        subjectConfirmationData.setInResponseTo("testInResponseTo");
        subjectConfirmationData.setRecipient("testRecipient");
        subjectConfirmationData.setAddress("testAddress");
        subjectConfirmation.setSubjectConfirmationData(subjectConfirmationData);

        // When
        String xml = subjectConfirmation.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}