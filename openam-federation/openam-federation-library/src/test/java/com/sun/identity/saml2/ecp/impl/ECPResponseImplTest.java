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

package com.sun.identity.saml2.ecp.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ECPResponseImplTest {

    @DataProvider
    public Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<ecp:Response xmlns:ecp=\"urn:oasis:names:tc:SAML:2.0:profiles:SSO:ecp\" " +
                        "xmlns:soap-env=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        "AssertionConsumerServiceURL=\"https://acs.example.com/\" " +
                        "soap-env:actor=\"testActor\" soap-env:mustUnderstand=\"true\"/>" },
                { true, false, "<ecp:Response " +
                        "xmlns:soap-env=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        "AssertionConsumerServiceURL=\"https://acs.example.com/\" " +
                        "soap-env:actor=\"testActor\" soap-env:mustUnderstand=\"true\"/>" },
                { false, false, "<Response " +
                        "xmlns:soap-env=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        "AssertionConsumerServiceURL=\"https://acs.example.com/\" " +
                        "soap-env:actor=\"testActor\" soap-env:mustUnderstand=\"true\"/>" },
        };
    }

    @Test(dataProvider = "xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        ECPResponseImpl response = new ECPResponseImpl();
        response.setActor("testActor");
        response.setMustUnderstand(true);
        response.setAssertionConsumerServiceURL("https://acs.example.com/");

        // When
        String xml = response.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}