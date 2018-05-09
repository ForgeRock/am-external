/*
 * Copyright 2013-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.common;

import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;

public class JaspiAuthLoginModuleTest {

    private JaspiAuthLoginModule jaspiAuthLoginModule;

    private boolean processMethodCalled = false;

    private JaspiAuthModuleWrapper jaspiAuthWrapper;

    private Map<String, Object> config = new HashMap<>();

    @BeforeMethod
    public void setUp() {

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
            protected boolean process(MessageInfo messageInfo, Subject clientSubject, Callback[] callbacks) throws LoginException {
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
    public void shouldInitialiseAuthenticationModuleWrapper() throws Exception {

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
    public void shouldProcessCallbacksAndThrowInvalidStateException() throws LoginException {

        //Given
        Callback[] callbacks = new Callback[0];
        int state = 0;

        //When
        boolean exceptionCaught = false;
        AuthLoginException exception = null;
        try {
            jaspiAuthLoginModule.process(callbacks, state);
        } catch (AuthLoginException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        assertTrue(exceptionCaught);
        assertEquals(exception.getErrorCode(), "incorrectState");
    }

    @Test
    public void shouldProcessCallbacksWhenValidateRequestReturnsSuccess() throws LoginException {

        //Given
        Callback[] callbacks = new Callback[0];
        int state = ISAuthConstants.LOGIN_START;

        given(jaspiAuthWrapper.validateRequest(Matchers.<MessageInfo>anyObject(), Matchers.<Subject>anyObject()))
                .willReturn(AuthStatus.SUCCESS);

        //When
        int returnedState = jaspiAuthLoginModule.process(callbacks, state);

        //Then
        assertTrue(processMethodCalled);
        verify(jaspiAuthWrapper).validateRequest(Matchers.<MessageInfo>anyObject(), Matchers.<Subject>anyObject());
        assertEquals(returnedState, ISAuthConstants.LOGIN_SUCCEED);
    }

    @Test
    public void shouldProcessCallbacksWhenValidateRequestReturnsSendSuccess() throws LoginException {

        //Given
        Callback[] callbacks = new Callback[0];
        int state = ISAuthConstants.LOGIN_START;

        given(jaspiAuthWrapper.validateRequest(Matchers.<MessageInfo>anyObject(), Matchers.<Subject>anyObject()))
                .willReturn(AuthStatus.SEND_SUCCESS);

        //When
        int returnedState = jaspiAuthLoginModule.process(callbacks, state);

        //Then
        assertTrue(processMethodCalled);
        verify(jaspiAuthWrapper).validateRequest(Matchers.<MessageInfo>anyObject(), Matchers.<Subject>anyObject());
        assertEquals(returnedState, ISAuthConstants.LOGIN_SUCCEED);
    }

    @Test
    public void shouldProcessCallbacksWhenValidateRequestReturnsSendFailure() throws LoginException {

        //Given
        Callback[] callbacks = new Callback[0];
        int state = ISAuthConstants.LOGIN_START;

        given(jaspiAuthWrapper.validateRequest(Matchers.<MessageInfo>anyObject(), Matchers.<Subject>anyObject()))
                .willReturn(AuthStatus.SEND_FAILURE);

        //When
        boolean exceptionCaught = false;
        AuthLoginException exception = null;
        try {
            jaspiAuthLoginModule.process(callbacks, state);
        } catch (AuthLoginException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        assertTrue(processMethodCalled);
        verify(jaspiAuthWrapper).validateRequest(Matchers.<MessageInfo>anyObject(), Matchers.<Subject>anyObject());
        assertTrue(exceptionCaught);
        assertEquals(exception.getErrorCode(), "authFailed");
    }

    @Test
    public void shouldProcessCallbacksWhenValidateRequestReturnsSendContinue() throws LoginException {

        //Given
        Callback[] callbacks = new Callback[0];
        int state = ISAuthConstants.LOGIN_START;

        given(jaspiAuthWrapper.validateRequest(Matchers.<MessageInfo>anyObject(), Matchers.<Subject>anyObject()))
                .willReturn(AuthStatus.SEND_CONTINUE);

        //When
        int returnedState = jaspiAuthLoginModule.process(callbacks, state);

        //Then
        assertTrue(processMethodCalled);
        verify(jaspiAuthWrapper).validateRequest(Matchers.<MessageInfo>anyObject(), Matchers.<Subject>anyObject());
        assertEquals(returnedState, ISAuthConstants.LOGIN_IGNORE);
    }

}
