/*
 * Copyright 2013-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.common;

import static org.mockito.BDDMockito.doThrow;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.testng.AssertJUnit.*;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AuthenticationException;

public class JaspiAuthLoginModulePAPTest {

    private JaspiAuthLoginModulePostAuthenticationPlugin jaspiPostAuthPlugin;

    private Map<String, Object> config = new HashMap<>();
    private boolean onLoginSuccessMethodCalled = false;
    private JaspiAuthModuleWrapper<ServerAuthModule> jaspiAuthWrapper;

    @BeforeMethod
    public void setUp() {

        jaspiAuthWrapper = mock(JaspiAuthModuleWrapper.class);

        jaspiPostAuthPlugin = new JaspiAuthLoginModulePostAuthenticationPlugin("amAuthPersistentCookie", jaspiAuthWrapper) {

            @Override
            protected Map<String, Object> generateConfig(HttpServletRequest request,
                    HttpServletResponse response, SSOToken ssoToken) throws AuthenticationException {
                return config;
            }

            @Override
            protected void onLoginSuccess(MessageInfo messageInfo, Map requestParamsMap, HttpServletRequest request,
                    HttpServletResponse response, SSOToken ssoToken) throws AuthenticationException {
                onLoginSuccessMethodCalled = true;
            }

            @Override
            public void onLoginFailure(Map requestParamsMap, HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {

            }

            @Override
            public void onLogout(HttpServletRequest request, HttpServletResponse response, SSOToken ssoToken) throws AuthenticationException {

            }
        };
    }

    @Test
    public void shouldCallOnLoginSuccessAndThrowAuthenticationExceptionWhenAuthExceptionCaught() throws Exception {

        //Given
        Map requestParamsMap = new HashMap();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        doThrow(AuthException.class).when(jaspiAuthWrapper).initialize((CallbackHandler) isNull(), eq(config));

        //When
        boolean exceptionCaught = false;
        AuthenticationException exception = null;
        try {
            jaspiPostAuthPlugin.onLoginSuccess(requestParamsMap, request, response, ssoToken);
        } catch (AuthenticationException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        verify(jaspiAuthWrapper).initialize(any(CallbackHandler.class), eq(config));
        verify(jaspiAuthWrapper, never()).secureResponse(Matchers.<MessageInfo>anyObject());
        assertTrue(exceptionCaught);
        assertEquals(exception.getErrorCode(), "authFailed");
    }

    @Test
    public void shouldCallOnLoginSuccessWhenSecureResponseReturnsSendSuccess() throws Exception {

        //Given
        Map requestParamsMap = new HashMap();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        given(jaspiAuthWrapper.secureResponse(Matchers.<MessageInfo>anyObject()))
                .willReturn(AuthStatus.SEND_SUCCESS);

        //When
        jaspiPostAuthPlugin.onLoginSuccess(requestParamsMap, request, response, ssoToken);

        //Then
        verify(jaspiAuthWrapper).initialize(any(CallbackHandler.class), eq(config));
        assertTrue(onLoginSuccessMethodCalled);
        verify(jaspiAuthWrapper).secureResponse(Matchers.<MessageInfo>anyObject());
    }

    @Test
    public void shouldCallOnLoginSuccessWhenSecureResponseReturnsSendFailure() throws AuthenticationException,
            AuthException {

        //Given
        Map requestParamsMap = new HashMap();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        given(jaspiAuthWrapper.secureResponse(Matchers.<MessageInfo>anyObject()))
                .willReturn(AuthStatus.SEND_FAILURE);

        //When
        boolean exceptionCaught = false;
        AuthenticationException exception = null;
        try {
            jaspiPostAuthPlugin.onLoginSuccess(requestParamsMap, request, response, ssoToken);
        } catch (AuthenticationException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        verify(jaspiAuthWrapper).initialize(any(CallbackHandler.class), eq(config));
        assertTrue(onLoginSuccessMethodCalled);
        verify(jaspiAuthWrapper).secureResponse(Matchers.<MessageInfo>anyObject());
        assertTrue(exceptionCaught);
        assertEquals(exception.getErrorCode(), "authFailed");
    }

    @Test
    public void shouldCallOnLoginSuccessWhenSecureResponseReturnsSendContinue() throws AuthenticationException,
            AuthException {

        //Given
        Map requestParamsMap = new HashMap();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        given(jaspiAuthWrapper.secureResponse(Matchers.<MessageInfo>anyObject()))
                .willReturn(AuthStatus.SEND_CONTINUE);

        //When
        boolean exceptionCaught = false;
        AuthenticationException exception = null;
        try {
            jaspiPostAuthPlugin.onLoginSuccess(requestParamsMap, request, response, ssoToken);
        } catch (AuthenticationException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        verify(jaspiAuthWrapper).initialize(any(CallbackHandler.class), eq(config));
        assertTrue(onLoginSuccessMethodCalled);
        verify(jaspiAuthWrapper).secureResponse(Matchers.<MessageInfo>anyObject());
        assertTrue(exceptionCaught);
        assertEquals(exception.getErrorCode(), "authFailed");
    }

    @Test
    public void shouldCallOnLoginSuccessWhenSecureResponseReturnsElse() throws AuthenticationException, AuthException {

        //Given
        Map requestParamsMap = new HashMap();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        given(jaspiAuthWrapper.secureResponse(Matchers.<MessageInfo>anyObject()))
                .willReturn(AuthStatus.SUCCESS);

        //When
        boolean exceptionCaught = false;
        AuthenticationException exception = null;
        try {
            jaspiPostAuthPlugin.onLoginSuccess(requestParamsMap, request, response, ssoToken);
        } catch (AuthenticationException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        verify(jaspiAuthWrapper).initialize(any(CallbackHandler.class), eq(config));
        assertTrue(onLoginSuccessMethodCalled);
        verify(jaspiAuthWrapper).secureResponse(Matchers.<MessageInfo>anyObject());
        assertTrue(exceptionCaught);
        assertEquals(exception.getErrorCode(), "authFailed");
    }

    @Test
    public void shouldCallOnLoginFailureAndDoNothing() throws AuthenticationException {

        //Given
        Map requestParamsMap = new HashMap();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        //When
        jaspiPostAuthPlugin.onLoginFailure(requestParamsMap, request, response);

        //Then
        verifyZeroInteractions(jaspiAuthWrapper);
    }

    @Test
    public void shouldCallOnLogoutAndDoNothing() throws Exception {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        //When
        jaspiPostAuthPlugin.onLogout(request, response, ssoToken);

        //Then
        verifyZeroInteractions(jaspiAuthWrapper);
    }
}
