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

import java.util.List;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class IDPListImplTest {

    @DataProvider
    public Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<samlp:IDPList xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\">" +
                        "<samlp:IDPEntry " +
                        "Name=\"test1\" ProviderID=\"urn:test\"/>" +
                        "<samlp:IDPEntry " +
                        "Name=\"test2\" ProviderID=\"urn:test\"/>" +
                        "<samlp:GetComplete>https://idps.example.com/</samlp:GetComplete>" +
                        "</samlp:IDPList>" },
                { true, false, "<samlp:IDPList>" +
                        "<samlp:IDPEntry " +
                        "Name=\"test1\" ProviderID=\"urn:test\"/>" +
                        "<samlp:IDPEntry " +
                        "Name=\"test2\" ProviderID=\"urn:test\"/>" +
                        "<samlp:GetComplete>https://idps.example.com/</samlp:GetComplete>" +
                        "</samlp:IDPList>" },
                { false, false, "<IDPList>" +
                        "<IDPEntry Name=\"test1\" ProviderID=\"urn:test\"/>" +
                        "<IDPEntry Name=\"test2\" ProviderID=\"urn:test\"/>" +
                        "<GetComplete>https://idps.example.com/</GetComplete>" +
                        "</IDPList>" }
        };
    }

    @Test(dataProvider = "xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        IDPListImpl idpList = new IDPListImpl();
        IDPEntryImpl entry1 = new IDPEntryImpl();
        entry1.setProviderID("urn:test");
        entry1.setName("test1");
        IDPEntryImpl entry2 = new IDPEntryImpl();
        entry2.setProviderID("urn:test");
        entry2.setName("test2");
        idpList.setIDPEntries(List.of(entry1, entry2));
        GetCompleteImpl getComplete = new GetCompleteImpl();
        getComplete.setValue("https://idps.example.com/");
        idpList.setGetComplete(getComplete);

        // When
        String xml = idpList.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}