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

import com.sun.identity.saml2.common.SAML2Constants;

public class NameIDImplTest {

    @DataProvider
    public Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true,
                        "<saml:NameID xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                                "Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\" " +
                                "NameQualifier=\"testNameQualifier\" SPNameQualifier=\"testSPNameQualifier\" " +
                                "SPProvidedID=\"spProvided\">testValue</saml:NameID>" },
                { true, false, "<saml:NameID " +
                        "Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\" " +
                        "NameQualifier=\"testNameQualifier\" " +
                        "SPNameQualifier=\"testSPNameQualifier\" " +
                        "SPProvidedID=\"spProvided\">testValue</saml:NameID>" },
                { false, false, "<NameID " +
                        "Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\" " +
                        "NameQualifier=\"testNameQualifier\" " +
                        "SPNameQualifier=\"testSPNameQualifier\" " +
                        "SPProvidedID=\"spProvided\">testValue</NameID>" }
        };
    }

    @Test(dataProvider = "xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        NameIDImpl nameID = new NameIDImpl();
        nameID.setNameQualifier("testNameQualifier");
        nameID.setSPNameQualifier("testSPNameQualifier");
        nameID.setFormat(SAML2Constants.NAMEID_TRANSIENT_FORMAT);
        nameID.setSPProvidedID("spProvided");
        nameID.setValue("testValue");

        // When
        String xml = nameID.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}