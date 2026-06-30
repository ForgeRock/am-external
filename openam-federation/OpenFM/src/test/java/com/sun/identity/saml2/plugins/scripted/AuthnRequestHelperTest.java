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
 * Copyright 2026 Ping Identity Corporation.
 */

package com.sun.identity.saml2.plugins.scripted;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.AuthnRequest;

/**
 * Tests for {@link AuthnRequestHelper}.
 */
public class AuthnRequestHelperTest {

    private static final String DESTINATION = "https://idp.example.com/saml2";

    private AuthnRequest authnRequest;
    private AuthnRequestHelper authnRequestHelper;

    @BeforeEach
    void setup() {
        authnRequest = mock(AuthnRequest.class);
        authnRequestHelper = new AuthnRequestHelper(authnRequest);
    }

    @Test
    void shouldGetDestinationFromUnderlyingAuthnRequest() {
        // Given
        when(authnRequest.getDestination()).thenReturn(DESTINATION);

        // When
        String result = authnRequestHelper.getDestination();

        // Then
        assertThat(result).isEqualTo(DESTINATION);
        verify(authnRequest).getDestination();
    }

    @Test
    void shouldSetDestinationOnUnderlyingAuthnRequest() throws Exception {
        // When
        authnRequestHelper.setDestination(DESTINATION);

        // Then
        verify(authnRequest).setDestination(DESTINATION);
    }

    @Test
    void shouldReturnNullWhenAuthnRequestIsNull() {
        // Given
        AuthnRequestHelper helperWithNullRequest = new AuthnRequestHelper(null);

        // When
        String result = helperWithNullRequest.getDestination();

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldIgnoreSettingDestinationWhenAuthnRequestIsNull() throws Exception {
        // Given
        AuthnRequestHelper helperWithNullRequest = new AuthnRequestHelper(null);

        // When
        helperWithNullRequest.setDestination(DESTINATION);

        // Then
        assertThat(helperWithNullRequest.getDestination()).isNull();
    }

    @Test
    void shouldPropagateSaml2ExceptionFromSetter() throws Exception {
        // Given
        SAML2Exception exception = new SAML2Exception("immutable request");
        doThrow(exception).when(authnRequest).setDestination(DESTINATION);

        // When / Then
        assertThatThrownBy(() -> authnRequestHelper.setDestination(DESTINATION))
                .isSameAs(exception);
    }
}
