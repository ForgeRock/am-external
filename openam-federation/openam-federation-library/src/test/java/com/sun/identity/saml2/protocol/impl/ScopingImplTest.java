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

public class ScopingImplTest {

    @DataProvider
    public Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<samlp:Scoping xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" ProxyCount=\"3\">" +
                        "<samlp:IDPList>" +
                        "<samlp:IDPEntry Name=\"testIdp\" ProviderID=\"urn:test\"/>" +
                        "<samlp:GetComplete>https://test.example.com</samlp:GetComplete></samlp:IDPList>" +
                        "<samlp:RequesterID>test</samlp:RequesterID>" +
                        "</samlp:Scoping>" },
                { true, false, "<samlp:Scoping ProxyCount=\"3\">" +
                        "<samlp:IDPList>" +
                        "<samlp:IDPEntry Name=\"testIdp\" ProviderID=\"urn:test\"/>" +
                        "<samlp:GetComplete>https://test.example.com</samlp:GetComplete></samlp:IDPList>" +
                        "<samlp:RequesterID>test</samlp:RequesterID>" +
                        "</samlp:Scoping>" },
                { false, false, "<Scoping ProxyCount=\"3\">" +
                        "<IDPList>" +
                        "<IDPEntry Name=\"testIdp\" ProviderID=\"urn:test\"/>" +
                        "<GetComplete>https://test.example.com</GetComplete>" +
                        "</IDPList>" +
                        "<RequesterID>test</RequesterID>" +
                        "</Scoping>" }
        };
    }

    @Test(dataProvider = "xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        ScopingImpl scoping = new ScopingImpl();
        scoping.setProxyCount(3);
        IDPListImpl idpList = new IDPListImpl();
        GetCompleteImpl getComplete = new GetCompleteImpl();
        getComplete.setValue("https://test.example.com");
        idpList.setGetComplete(getComplete);
        IDPEntryImpl idpEntry = new IDPEntryImpl();
        idpEntry.setName("testIdp");
        idpEntry.setProviderID("urn:test");
        idpList.setIDPEntries(List.of(idpEntry));
        scoping.setIDPList(idpList);
        RequesterIDImpl requesterID = new RequesterIDImpl();
        requesterID.setValue("test");
        scoping.setRequesterIDs(List.of(requesterID));

        // When
        String xml = scoping.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }

}