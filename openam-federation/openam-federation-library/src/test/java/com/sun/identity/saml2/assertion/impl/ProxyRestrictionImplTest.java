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

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;


public class ProxyRestrictionImplTest {

    public static Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<saml:ProxyRestriction xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                        "Count=\"3\">" +
                        "<saml:Audience>a</saml:Audience>" +
                        "<saml:Audience>b</saml:Audience>" +
                        "<saml:Audience>c</saml:Audience>" +
                        "</saml:ProxyRestriction>" },
                { true, false, "<saml:ProxyRestriction Count=\"3\">" +
                        "<saml:Audience>a</saml:Audience>" +
                        "<saml:Audience>b</saml:Audience>" +
                        "<saml:Audience>c</saml:Audience>" +
                        "</saml:ProxyRestriction>" },
                { false, false, "<ProxyRestriction Count=\"3\">" +
                        "<Audience>a</Audience>" +
                        "<Audience>b</Audience>" +
                        "<Audience>c</Audience>" +
                        "</ProxyRestriction>" }
        };
    }

    @ParameterizedTest
    @MethodSource("xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        ProxyRestrictionImpl proxyRestriction = new ProxyRestrictionImpl();
        proxyRestriction.setAudience(List.of("a", "b", "c"));
        proxyRestriction.setCount(3);

        // When
        String xml = proxyRestriction.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}
