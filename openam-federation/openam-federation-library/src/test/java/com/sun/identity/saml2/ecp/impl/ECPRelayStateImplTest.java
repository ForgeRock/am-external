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
 * Copyright 2021-2025 Ping Identity Corporation.
 */

package com.sun.identity.saml2.ecp.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ECPRelayStateImplTest {

    public static Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<ecp:RelayState xmlns:ecp=\"urn:oasis:names:tc:SAML:2.0:profiles:SSO:ecp\" " +
                        "xmlns:soap-env=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        "soap-env:actor=\"testActor\" soap-env:mustUnderstand=\"true\" " +
                        ">testValue</ecp:RelayState>" },
                { true, false, "<ecp:RelayState xmlns:soap-env=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        "soap-env:actor=\"testActor\" soap-env:mustUnderstand=\"true\" " +
                        ">testValue</ecp:RelayState>" },
                { false, false, "<RelayState xmlns:soap-env=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        "soap-env:actor=\"testActor\" soap-env:mustUnderstand=\"true\" " +
                        ">testValue</RelayState>" }
        };
    }

    @ParameterizedTest
    @MethodSource("xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        ECPRelayStateImpl relayState = new ECPRelayStateImpl();
        relayState.setActor("testActor");
        relayState.setValue("testValue");
        relayState.setMustUnderstand(true);

        // When
        String xml = relayState.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}
