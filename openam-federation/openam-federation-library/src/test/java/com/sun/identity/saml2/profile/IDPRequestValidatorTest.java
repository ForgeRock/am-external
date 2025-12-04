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
 * Copyright 2015-2025 Ping Identity Corporation.
 */
package com.sun.identity.saml2.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

import jakarta.servlet.http.HttpServletRequest;

import org.forgerock.openam.saml2.IDPRequestValidator;
import org.forgerock.openam.saml2.UtilProxyIDPRequestValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.identity.saml2.meta.SAML2MetaManager;

/**
 * Note: Tests are excluding {@link IDPRequestValidator#getIDPAdapter(String, String)} because it is currently
 * untestable.
 */
public class IDPRequestValidatorTest {
    private HttpServletRequest mockRequest;
    private IDPRequestValidator validator;

    @BeforeEach
    void setUp() throws ServerFaultException, ClientFaultException {
        mockRequest = mock(HttpServletRequest.class);
        validator = new UtilProxyIDPRequestValidator("", false, mock(SAML2MetaManager.class));
    }

    @Test
    void shouldNotAllowMissingMetaAlias() throws ServerFaultException, ClientFaultException {
        assertThatThrownBy(() -> {
            given(mockRequest.getParameter(SAML2MetaManager.NAME_META_ALIAS_IN_URI)).willReturn("");
            given(mockRequest.getRequestURI()).willReturn("");
            validator.getMetaAlias(mockRequest);
        }).isInstanceOf(ClientFaultException.class);
    }

    @Test
    void shoulAllowMetaAliasInParameter() throws ServerFaultException, ClientFaultException {
        // Given
        String metaBadger = "badger";
        given(mockRequest.getParameter(SAML2MetaManager.NAME_META_ALIAS_IN_URI)).willReturn(metaBadger);
        given(mockRequest.getRequestURI()).willReturn("");
        // When
        String result = validator.getMetaAlias(mockRequest);
        // Then
        assertThat(result).isEqualTo(metaBadger);
    }

    @Test
    void shoulAllowMetaAliasInURI() throws ServerFaultException, ClientFaultException {
        // Given
        String metaBadger = "badger";
        given(mockRequest.getParameter(SAML2MetaManager.NAME_META_ALIAS_IN_URI)).willReturn("");
        given(mockRequest.getRequestURI()).willReturn("metaAlias" + metaBadger);
        // When
        String result = validator.getMetaAlias(mockRequest);
        // Then
        assertThat(result).isEqualTo(metaBadger);
    }
}
