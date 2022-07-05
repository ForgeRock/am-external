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

public class IssuerImplTest {

    @DataProvider
    public Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<saml:Issuer xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                        "Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\" " +
                        "NameQualifier=\"testNameQualifier\" " +
                        "SPNameQualifier=\"testSPNameQualifier\" " +
                        "SPProvidedID=\"spProvidedId\">testIssuer</saml:Issuer>" },
                { true, false, "<saml:Issuer " +
                        "Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\" " +
                        "NameQualifier=\"testNameQualifier\" " +
                        "SPNameQualifier=\"testSPNameQualifier\" " +
                        "SPProvidedID=\"spProvidedId\">testIssuer</saml:Issuer>" },
                { false, false, "<Issuer " +
                        "Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\" " +
                        "NameQualifier=\"testNameQualifier\" " +
                        "SPNameQualifier=\"testSPNameQualifier\" " +
                        "SPProvidedID=\"spProvidedId\">testIssuer</Issuer>" }
        };
    }

    @Test(dataProvider = "xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        IssuerImpl issuer = new IssuerImpl();
        issuer.setFormat(SAML2Constants.NAMEID_TRANSIENT_FORMAT);
        issuer.setNameQualifier("testNameQualifier");
        issuer.setSPNameQualifier("testSPNameQualifier");
        issuer.setSPProvidedID("spProvidedId");
        issuer.setValue("testIssuer");

        // When
        String xml = issuer.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}