/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package com.sun.identity.saml2.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;

import javax.servlet.http.HttpServletRequest;

import org.forgerock.openam.saml2.IDPRequestValidator;
import org.forgerock.openam.saml2.UtilProxyIDPRequestValidator;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.shared.debug.Debug;

/**
 * Note: Tests are excluding {@link IDPRequestValidator#getIDPAdapter(String, String)} because it is currently
 * untestable.
 */
public class IDPRequestValidatorTest {
    private HttpServletRequest mockRequest;
    private IDPRequestValidator validator;

    @BeforeMethod
    public void setUp() throws ServerFaultException, ClientFaultException {
        mockRequest = mock(HttpServletRequest.class);
        validator = new UtilProxyIDPRequestValidator("", false, mock(Debug.class), mock(SAML2MetaManager.class));
    }

    @Test (expectedExceptions = ClientFaultException.class)
    public void shouldNotAllowMissingMetaAlias() throws ServerFaultException, ClientFaultException {
        given(mockRequest.getParameter(SAML2MetaManager.NAME_META_ALIAS_IN_URI)).willReturn("");
        given(mockRequest.getRequestURI()).willReturn("");
        validator.getMetaAlias(mockRequest);
    }

    @Test
    public void shoulAllowMetaAliasInParameter() throws ServerFaultException, ClientFaultException {
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
    public void shoulAllowMetaAliasInURI() throws ServerFaultException, ClientFaultException {
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
