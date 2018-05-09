/*
 * Copyright 2015-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package com.sun.identity.saml2.profile;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.when;

import org.forgerock.openam.saml2.IDPRequestValidator;
import org.forgerock.openam.saml2.IDPSSOFederateRequest;
import org.forgerock.openam.saml2.SAML2ActorFactory;
import org.forgerock.openam.saml2.SAMLAuthenticator;
import org.forgerock.openam.saml2.SAMLAuthenticatorLookup;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

public class IDPSSOFederateTest {

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    @Mock
    private PrintWriter mockPrintWriter;

    @Mock
    private FederateCookieRedirector cookieRedirector;

    @Mock
    private SAML2ActorFactory actorFactory;

    @Mock
    private IDPRequestValidator validator;

    @Mock
    private SAMLAuthenticator authenticator;

    @Mock
    private SAMLAuthenticatorLookup authenticationLookup;

    private IDPSSOFederate idpSsoFederateRequest;

    @BeforeMethod
    public void initMocks() throws ServerFaultException, ClientFaultException {
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
        idpSsoFederateRequest = new IDPSSOFederate(false, cookieRedirector, actorFactory);
    }

    @Test
    public void shouldBeTestable() throws Exception {
        idpSsoFederateRequest.process(mockRequest, mockResponse, mockPrintWriter, null);
    }

    @Test
    public void shouldNotCallAnyFurtherFunctionsAfterNeedSetLBCookieAndRedirectReturnsTrue() throws Exception {

        // Arrange
        when(cookieRedirector.needSetLBCookieAndRedirect(
                any(HttpServletRequest.class),
                any(HttpServletResponse.class),
                anyBoolean())).thenReturn(true);
        // Act
        idpSsoFederateRequest.process(mockRequest, mockResponse, mockPrintWriter, null);
        // Assert
        Mockito.verifyZeroInteractions(authenticator, authenticationLookup);
    }

    @Test
    public void shouldCallAuthenticateIfThereIsNoRequestId() throws Exception {

        // Arrange
        when(mockRequest.getParameter("ReqID")).thenReturn("");

        when(cookieRedirector.needSetLBCookieAndRedirect(
                any(HttpServletRequest.class),
                any(HttpServletResponse.class),
                anyBoolean())).thenReturn(false);

        // Act
        idpSsoFederateRequest.process(mockRequest, mockResponse, mockPrintWriter, null);

        // Assert
        Mockito.verify(authenticator).authenticate();
        Mockito.verifyZeroInteractions(authenticationLookup);
    }

    @Test
    public void shouldCallAuthenticateLookupIfThereIsARequestId() throws Exception {

        // Arrange
        when(mockRequest.getParameter("ReqID")).thenReturn("12345");

        when(cookieRedirector.needSetLBCookieAndRedirect(
                any(HttpServletRequest.class),
                any(HttpServletResponse.class),
                anyBoolean())).thenReturn(false);

        // Act
        idpSsoFederateRequest.process(mockRequest, mockResponse, mockPrintWriter, null);

        // Assert
        Mockito.verify(authenticationLookup).retrieveAuthenticationFromCache();
        Mockito.verifyZeroInteractions(authenticator);
    }
}
