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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sun.identity.saml2.assertion.BaseID;
import com.sun.identity.saml2.assertion.EncryptedID;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.common.SAML2Constants;

public class SubjectImplTest {

    public static Object[][] xmlTestCasesWithBaseID() {
        return new Object[][] {
                { true, true, "<saml:Subject xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">" +
                        "<saml:BaseID NameQualifier=\"testNameQualifier\" SPNameQualifier=\"testSPNameQualifier\" />" +
                        "</saml:Subject>" },
                { true, false, "<saml:Subject>" +
                        "<saml:BaseID NameQualifier=\"testNameQualifier\" SPNameQualifier=\"testSPNameQualifier\" />" +
                        "</saml:Subject>" },
                { false, false, "<Subject>" +
                        "<BaseID NameQualifier=\"testNameQualifier\" SPNameQualifier=\"testSPNameQualifier\" />" +
                        "</Subject>" }
        };
    }

    @ParameterizedTest
    @MethodSource("xmlTestCasesWithBaseID")
    public void testToXmlStringWithBaseID(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        SubjectImpl subject = new SubjectImpl();
        BaseID baseID = new BaseIDImpl();
        baseID.setNameQualifier("testNameQualifier");
        baseID.setSPNameQualifier("testSPNameQualifier");
        subject.setBaseID(baseID);

        // When
        String xml = subject.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }

    public static Object[][] xmlTestCasesWithNameID() {
        return new Object[][] {
                { true, true, "<saml:Subject xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">\n" +
                        "<saml:NameID " +
                        "Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\" " +
                        "NameQualifier=\"testNameQualifier\" " +
                        "SPNameQualifier=\"testSPNameQualifier\" " +
                        "SPProvidedID=\"testSPProvidedID\">testValue</saml:NameID></saml:Subject>" },
                { true, false, "<saml:Subject>" +
                        "<saml:NameID " +
                        "Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\" " +
                        "NameQualifier=\"testNameQualifier\" " +
                        "SPNameQualifier=\"testSPNameQualifier\" " +
                        "SPProvidedID=\"testSPProvidedID\">testValue</saml:NameID></saml:Subject>" },
                { false, false, "<Subject>\n" +
                        "<NameID " +
                        "Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\" " +
                        "NameQualifier=\"testNameQualifier\" " +
                        "SPNameQualifier=\"testSPNameQualifier\" " +
                        "SPProvidedID=\"testSPProvidedID\">testValue</NameID></Subject>" }
        };
    }

    @ParameterizedTest
    @MethodSource("xmlTestCasesWithNameID")
    public void testToXmlStringWithNameID(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        SubjectImpl subject = new SubjectImpl();
        NameID nameID = new NameIDImpl();
        nameID.setNameQualifier("testNameQualifier");
        nameID.setSPNameQualifier("testSPNameQualifier");
        nameID.setFormat(SAML2Constants.NAMEID_TRANSIENT_FORMAT);
        nameID.setSPProvidedID("testSPProvidedID");
        nameID.setValue("testValue");
        subject.setNameID(nameID);

        // When
        String xml = subject.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }

    public static Object[][] xmlTestCasesWithEncryptedID() {
        return new Object[][] {
                { true, true, "<saml:Subject xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">" +
                        "<EncryptedID>test</EncryptedID></saml:Subject>" },
                { true, false, "<saml:Subject><EncryptedID>test</EncryptedID></saml:Subject>" },
                { false, false, "<Subject><EncryptedID>test</EncryptedID></Subject>" }
        };
    }

    @ParameterizedTest
    @MethodSource("xmlTestCasesWithEncryptedID")
    public void testToXmlStringWithEncryptedID(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        SubjectImpl subject = new SubjectImpl();
        EncryptedID encryptedID = new EncryptedIDImpl("<EncryptedID>test</EncryptedID>");
        subject.setEncryptedID(encryptedID);

        // When
        String xml = subject.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}
