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
 * Copyright 2015-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package com.sun.identity.saml2.profile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.openam.saml2.IDPRequestValidator;
import org.forgerock.openam.saml2.IDPSSOFederateRequest;
import org.forgerock.openam.saml2.SAML2ActorFactory;
import org.forgerock.openam.saml2.SAMLAuthenticator;
import org.forgerock.openam.saml2.SAMLAuthenticatorLookup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class IDPSSOFederateTest {

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    @Mock
    private PrintWriter mockPrintWriter;

    @Mock
    private SAML2ActorFactory actorFactory;

    @Mock
    private IDPRequestValidator validator;

    @Mock
    private SAMLAuthenticator authenticator;

    @Mock
    private SAMLAuthenticatorLookup authenticationLookup;

    private IDPSSOFederate idpSsoFederateRequest;

    @BeforeEach
    void initMocks() throws ServerFaultException {
        MockitoAnnotations.initMocks(this);
        when(actorFactory.getIDPRequestValidator(any(), anyBoolean())).thenReturn(validator);
        when(actorFactory.getSAMLAuthenticator(
                any(IDPSSOFederateRequest.class),
                any(HttpServletRequest.class),
                any(HttpServletResponse.class),
                any(PrintWriter.class),
                anyBoolean())).thenReturn(authenticator);
        when(actorFactory.getSAMLAuthenticatorLookup(
                any(IDPSSOFederateRequest.class),
                any(HttpServletRequest.class),
                any(HttpServletResponse.class),
                any(PrintWriter.class))).thenReturn(authenticationLookup);
        idpSsoFederateRequest = new IDPSSOFederate(false, actorFactory);
    }

    @Test
    void shouldBeTestable() throws Exception {
        idpSsoFederateRequest.process(mockRequest, mockResponse, mockPrintWriter, null);
    }

    @Test
    void shouldCallAuthenticateIfThereIsNoRequestId() throws Exception {

        // Arrange
        when(mockRequest.getParameter("ReqID")).thenReturn("");

        // Act
        idpSsoFederateRequest.process(mockRequest, mockResponse, mockPrintWriter, null);

        // Assert
        Mockito.verify(authenticator).authenticate();
        Mockito.verifyZeroInteractions(authenticationLookup);
    }

    @Test
    void shouldCallAuthenticateLookupIfThereIsARequestId() throws Exception {

        // Arrange
        when(mockRequest.getParameter("ReqID")).thenReturn("12345");

        // Act
        idpSsoFederateRequest.process(mockRequest, mockResponse, mockPrintWriter, null);

        // Assert
        Mockito.verify(authenticationLookup).retrieveAuthenticationFromCache();
        Mockito.verifyZeroInteractions(authenticator);
    }
}
