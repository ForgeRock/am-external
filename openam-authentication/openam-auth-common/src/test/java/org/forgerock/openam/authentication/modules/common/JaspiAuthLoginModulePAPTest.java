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
 * Copyright 2013-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.authentication.modules.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.doThrow;
import static org.mockito.BDDMockito.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.isNull;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.module.ServerAuthModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AuthenticationException;

public class JaspiAuthLoginModulePAPTest {

    private final Map<String, Object> config = new HashMap<>();
    private JaspiAuthLoginModulePostAuthenticationPlugin jaspiPostAuthPlugin;
    private boolean onLoginSuccessMethodCalled = false;
    private JaspiAuthModuleWrapper<ServerAuthModule> jaspiAuthWrapper;

    @BeforeEach
    void setUp() {

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
    void shouldCallOnLoginSuccessAndThrowAuthenticationExceptionWhenAuthExceptionCaught() throws Exception {

        //Given
        Map requestParamsMap = new HashMap();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        doThrow(AuthException.class).when(jaspiAuthWrapper).initialize(isNull(), eq(config));

        //When
        assertThatThrownBy(() -> jaspiPostAuthPlugin.onLoginSuccess(requestParamsMap, request, response, ssoToken))
                //Then
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Authentication Failed");

        verify(jaspiAuthWrapper).initialize(any(), eq(config));
        verify(jaspiAuthWrapper, never()).secureResponse(any());
    }

    @Test
    void shouldCallOnLoginSuccessWhenSecureResponseReturnsSendSuccess() throws Exception {

        //Given
        Map requestParamsMap = new HashMap();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        given(jaspiAuthWrapper.secureResponse(any()))
                .willReturn(AuthStatus.SEND_SUCCESS);

        //When
        jaspiPostAuthPlugin.onLoginSuccess(requestParamsMap, request, response, ssoToken);

        //Then
        verify(jaspiAuthWrapper).initialize(any(), eq(config));
        assertThat(onLoginSuccessMethodCalled).isTrue();
        verify(jaspiAuthWrapper).secureResponse(any());
    }

    @Test
    void shouldCallOnLoginSuccessWhenSecureResponseReturnsSendFailure() throws
            AuthException {

        //Given
        Map requestParamsMap = new HashMap();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        given(jaspiAuthWrapper.secureResponse(any()))
                .willReturn(AuthStatus.SEND_FAILURE);

        //When
        assertThatThrownBy(() -> jaspiPostAuthPlugin.onLoginSuccess(requestParamsMap, request, response, ssoToken))
                //Then
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Authentication Failed");

        verify(jaspiAuthWrapper).initialize(any(), eq(config));
        assertThat(onLoginSuccessMethodCalled).isTrue();
        verify(jaspiAuthWrapper).secureResponse(any());
    }

    @Test
    void shouldCallOnLoginSuccessWhenSecureResponseReturnsSendContinue() throws
            AuthException {

        //Given
        Map requestParamsMap = new HashMap();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        given(jaspiAuthWrapper.secureResponse(any()))
                .willReturn(AuthStatus.SEND_CONTINUE);

        //When
        assertThatThrownBy(() -> jaspiPostAuthPlugin.onLoginSuccess(requestParamsMap, request, response, ssoToken))
                //Then
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Authentication Failed");

        verify(jaspiAuthWrapper).initialize(any(), eq(config));
        assertThat(onLoginSuccessMethodCalled).isTrue();
        verify(jaspiAuthWrapper).secureResponse(any());
    }

    @Test
    void shouldCallOnLoginSuccessWhenSecureResponseReturnsElse() throws AuthException {

        //Given
        Map requestParamsMap = new HashMap();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        given(jaspiAuthWrapper.secureResponse(any()))
                .willReturn(AuthStatus.SUCCESS);

        //When
        assertThatThrownBy(() -> jaspiPostAuthPlugin.onLoginSuccess(requestParamsMap, request, response, ssoToken))
                //Then
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Authentication Failed");

        verify(jaspiAuthWrapper).initialize(any(), eq(config));
        assertThat(onLoginSuccessMethodCalled).isTrue();
        verify(jaspiAuthWrapper).secureResponse(any());
    }

    @Test
    void shouldCallOnLoginFailureAndDoNothing() throws AuthenticationException {

        //Given
        Map requestParamsMap = new HashMap();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        //When
        jaspiPostAuthPlugin.onLoginFailure(requestParamsMap, request, response);

        //Then
        verifyNoInteractions(jaspiAuthWrapper);
    }

    @Test
    void shouldCallOnLogoutAndDoNothing() throws Exception {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        //When
        jaspiPostAuthPlugin.onLogout(request, response, ssoToken);

        //Then
        verifyNoInteractions(jaspiAuthWrapper);
    }
}
