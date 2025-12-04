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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.Mockito.mock;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sun.identity.saml2.assertion.impl.IssuerImpl;
import com.sun.identity.saml2.common.SAML2Exception;

public class RequestAbstractImplTest {

    @Test
    void shouldRejectInvalidIDValues() throws Exception {
        assertThatThrownBy(() -> {
            // Given
            RequestAbstractImpl request = mock(RequestAbstractImpl.class);
            willCallRealMethod().given(request).validateID(anyString());

            // When
            request.validateID("x\" oops=\"bad\"");
        }).isInstanceOf(SAML2Exception.class);
    }

    public static Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<samlp:test " +
                        "xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" " +
                        "Consent=\"testConsent\" " +
                        "Destination=\"https://test.example.com/\" " +
                        "ID=\"testID\" " +
                        "IssueInstant=\"2001-09-09T01:46:40Z\" " +
                        "Version=\"2.0\">" +
                        "<saml:Issuer xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">testIssuer</saml:Issuer>" +
                        "<samlp:Extensions>" +
                        "<samlp:Extension>foo</samlp:Extension></samlp:Extensions>" +
                        "</samlp:test>" },
                { true, false, "<samlp:test " +
                        "Consent=\"testConsent\" " +
                        "Destination=\"https://test.example.com/\" " +
                        "ID=\"testID\" " +
                        "IssueInstant=\"2001-09-09T01:46:40Z\" " +
                        "Version=\"2.0\">" +
                        "<saml:Issuer>testIssuer</saml:Issuer>" +
                        "<samlp:Extensions>" +
                        "<samlp:Extension xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\">foo</samlp:Extension>" +
                        "</samlp:Extensions>" +
                        "</samlp:test>" },
                { false, false, "<test " +
                        "Consent=\"testConsent\" " +
                        "Destination=\"https://test.example.com/\" " +
                        "ID=\"testID\" " +
                        "IssueInstant=\"2001-09-09T01:46:40Z\" " +
                        "Version=\"2.0\">" +
                        "<Issuer>testIssuer</Issuer>" +
                        "<Extensions>" +
                        "<samlp:Extension xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\">foo</samlp:Extension>" +
                        "</Extensions>" +
                        "</test>" }
        };
    }

    @ParameterizedTest
    @MethodSource("xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        RequestAbstractImpl requestAbstract = new StubRequest();
        requestAbstract.setConsent("testConsent");
        requestAbstract.setDestination("https://test.example.com/");
        requestAbstract.setID("testID");
        IssuerImpl issuer = new IssuerImpl();
        issuer.setValue("testIssuer");
        requestAbstract.setIssuer(issuer);
        requestAbstract.setVersion("2.0");
        requestAbstract.setIssueInstant(new Date(1000000000000L));
        ExtensionsImpl extensions = new ExtensionsImpl();
        extensions.setAny(List.of("<samlp:Extension>foo</samlp:Extension>"));
        requestAbstract.setExtensions(extensions);

        // When
        String xml = requestAbstract.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }

    private static class StubRequest extends RequestAbstractImpl {
        public StubRequest() {
            super("test");
            isMutable = true;
        }
    }
}
