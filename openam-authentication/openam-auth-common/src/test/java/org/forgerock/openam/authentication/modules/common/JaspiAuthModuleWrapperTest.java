/*
 * Copyright 2013-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.common;

import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class JaspiAuthModuleWrapperTest {

    private ServerAuthModule serverAuthModule;
    private JaspiAuthModuleWrapper<ServerAuthModule> jaspiAuthWrapper;

    @BeforeMethod
    public void setUp() {

        AMLoginModuleBinder amLoginModuleBinder = mock(AMLoginModuleBinder.class);
        serverAuthModule = mock(ServerAuthModule.class);

        jaspiAuthWrapper = new JaspiAuthModuleWrapper<ServerAuthModule>(serverAuthModule) {};

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        given(amLoginModuleBinder.getHttpServletRequest()).willReturn(request);
        given(amLoginModuleBinder.getHttpServletResponse()).willReturn(response);
    }

    @Test
    public void shouldInitialiseAuthenticationModule() throws Exception {

        //Given
        CallbackHandler callbackHandler = mock(CallbackHandler.class);
        Map config = new HashMap();

        //When
        jaspiAuthWrapper.initialize(callbackHandler, config);

        //Then
        verify(serverAuthModule).initialize(Matchers.<MessagePolicy>anyObject(), (MessagePolicy) isNull(),
                eq(callbackHandler), eq(config));
    }
}
