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
 * Copyright 2013-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.authentication.modules.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;

public class JaspiAuthLoginModuleTest {

    private final Map<String, Object> config = new HashMap<>();
    private JaspiAuthLoginModule jaspiAuthLoginModule;
    private boolean processMethodCalled = false;
    private JaspiAuthModuleWrapper jaspiAuthWrapper;

    @BeforeEach
    void setUp() {

        jaspiAuthWrapper = mock(JaspiAuthModuleWrapper.class);

        jaspiAuthLoginModule = new JaspiAuthLoginModule("amAuthPersistentCookie", jaspiAuthWrapper) {
            @Override
            public Principal getPrincipal() {
                return null;
            }

            @Override
            protected Map<String, Object> generateConfig(Subject subject, Map sharedState, Map options) {
                return config;
            }

            @Override
            protected boolean process(MessageInfo messageInfo, Subject clientSubject, Callback[] callbacks) {
                processMethodCalled = true;
                return true;
            }
        };

        AMLoginModuleBinder amLoginModuleBinder = mock(AMLoginModuleBinder.class);

        jaspiAuthLoginModule.setAMLoginModule(amLoginModuleBinder);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        given(amLoginModuleBinder.getHttpServletRequest()).willReturn(request);
        given(amLoginModuleBinder.getHttpServletResponse()).willReturn(response);
    }

    @Test
    void shouldInitialiseAuthenticationModuleWrapper() throws Exception {

        //Given
        Subject subject = new Subject();
        Map sharedState = new HashMap();
        Map options = new HashMap();

        //When
        jaspiAuthLoginModule.init(subject, sharedState, options);

        //Then
        verify(jaspiAuthWrapper).initialize(any(), eq(config));
    }

    @Test
    void shouldProcessCallbacksAndThrowInvalidStateException() throws LoginException {

        //Given
        Callback[] callbacks = new Callback[0];
        int state = 0;

        //When
        assertThatThrownBy(() -> jaspiAuthLoginModule.process(callbacks, state))
                //Then
                .isInstanceOf(AuthLoginException.class)
                .hasMessage("Incorrect State");
    }

    @Test
    void shouldProcessCallbacksWhenValidateRequestReturnsSuccess() throws LoginException {

        //Given
        Callback[] callbacks = new Callback[0];
        int state = ISAuthConstants.LOGIN_START;

        given(jaspiAuthWrapper.validateRequest(any(), any()))
                .willReturn(AuthStatus.SUCCESS);

        //When
        int returnedState = jaspiAuthLoginModule.process(callbacks, state);

        //Then
        assertThat(processMethodCalled).isTrue();
        verify(jaspiAuthWrapper).validateRequest(any(), any());
        assertThat(returnedState).isEqualTo(ISAuthConstants.LOGIN_SUCCEED);
    }

    @Test
    void shouldProcessCallbacksWhenValidateRequestReturnsSendSuccess() throws LoginException {

        //Given
        Callback[] callbacks = new Callback[0];
        int state = ISAuthConstants.LOGIN_START;

        given(jaspiAuthWrapper.validateRequest(any(), any()))
                .willReturn(AuthStatus.SEND_SUCCESS);

        //When
        int returnedState = jaspiAuthLoginModule.process(callbacks, state);

        //Then
        assertThat(processMethodCalled).isTrue();
        verify(jaspiAuthWrapper).validateRequest(any(), any());
        assertThat(returnedState).isEqualTo(ISAuthConstants.LOGIN_SUCCEED);
    }

    @Test
    void shouldProcessCallbacksWhenValidateRequestReturnsSendFailure() throws LoginException {

        //Given
        Callback[] callbacks = new Callback[0];
        int state = ISAuthConstants.LOGIN_START;

        given(jaspiAuthWrapper.validateRequest(any(), any()))
                .willReturn(AuthStatus.SEND_FAILURE);

        //When
        assertThatThrownBy(() -> jaspiAuthLoginModule.process(callbacks, state))
                //Then
                .isInstanceOf(AuthLoginException.class)
                .hasMessage("Authentication Failed");

        assertThat(processMethodCalled).isTrue();
        verify(jaspiAuthWrapper).validateRequest(any(), any());
    }

    @Test
    void shouldProcessCallbacksWhenValidateRequestReturnsSendContinue() throws LoginException {

        //Given
        Callback[] callbacks = new Callback[0];
        int state = ISAuthConstants.LOGIN_START;

        given(jaspiAuthWrapper.validateRequest(any(), any()))
                .willReturn(AuthStatus.SEND_CONTINUE);

        //When
        int returnedState = jaspiAuthLoginModule.process(callbacks, state);

        //Then
        assertThat(processMethodCalled).isTrue();
        verify(jaspiAuthWrapper).validateRequest(any(), any());
        assertThat(returnedState).isEqualTo(ISAuthConstants.LOGIN_IGNORE);
    }

}
