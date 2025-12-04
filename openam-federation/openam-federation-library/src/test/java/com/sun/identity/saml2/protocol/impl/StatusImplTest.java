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

package com.sun.identity.saml2.protocol.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class StatusImplTest {

    public static Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<samlp:Status xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\">" +
                        "<samlp:StatusCode Value=\"test\"/>" +
                        "<samlp:StatusMessage>" +
                        "test message" +
                        "</samlp:StatusMessage>" +
                        "<samlp:StatusDetail>" +
                        "<saml:foo xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"/>" +
                        "<bar x=\"1\">test</bar></samlp:StatusDetail>" +
                        "</samlp:Status>" },
                { true, false, "<samlp:Status>" +
                        "<samlp:StatusCode Value=\"test\"/>" +
                        "<samlp:StatusMessage>" +
                        "test message" +
                        "</samlp:StatusMessage>" +
                        "<samlp:StatusDetail>" +
                        "<saml:foo xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"/>" +
                        "<bar x=\"1\">test</bar></samlp:StatusDetail>" +
                        "</samlp:Status>" },
                { false, false, "<Status>" +
                        "<StatusCode Value=\"test\"/>" +
                        "<StatusMessage>" +
                        "test message" +
                        "</StatusMessage>" +
                        "<StatusDetail>" +
                        "<saml:foo xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"/>" +
                        "<bar x=\"1\">test</bar></StatusDetail>" +
                        "</Status>" }
        };
    }

    @ParameterizedTest
    @MethodSource("xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        StatusImpl status = new StatusImpl();
        StatusDetailImpl detail = new StatusDetailImpl();
        detail.setAny(List.of("<saml:foo/>", "<bar x=\"1\">test</bar>"));
        status.setStatusDetail(detail);
        status.setStatusMessage("test message");
        StatusCodeImpl statusCode = new StatusCodeImpl();
        statusCode.setValue("test");
        status.setStatusCode(statusCode);

        // When
        String xml = status.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}
