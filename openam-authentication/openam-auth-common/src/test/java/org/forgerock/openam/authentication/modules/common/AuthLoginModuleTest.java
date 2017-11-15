/*
 * Copyright 2013-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.common;

import com.sun.identity.authentication.spi.AuthLoginException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;
import java.security.Principal;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class AuthLoginModuleTest {

    private AuthLoginModule authLoginModule;

    private AMLoginModuleBinder amLoginModuleBinder;

    @BeforeClass
    public void setUp() {

        amLoginModuleBinder = mock(AMLoginModuleBinder.class);

        authLoginModule = new AuthLoginModule() {
            @Override
            public void init(Subject subject, Map sharedState, Map options) {
            }

            @Override
            public int process(Callback[] callbacks, int state) throws LoginException {
                return 0;
            }

            @Override
            public Principal getPrincipal() {
                return null;
            }
        };
        authLoginModule.setAMLoginModule(amLoginModuleBinder);
    }

    @Test
    public void shouldGetCallbackHandler() {

        //Given

        //When
        authLoginModule.getCallbackHandler();

        //Then
        verify(amLoginModuleBinder).getCallbackHandler();
    }

    @Test
    public void shouldGetHttpServletRequest() {

        //Given

        //When
        authLoginModule.getHttpServletRequest();

        //Then
        verify(amLoginModuleBinder).getHttpServletRequest();
    }

    @Test
    public void shouldGetHttpServletResponse() {

        //Given

        //When
        authLoginModule.getHttpServletResponse();

        //Then
        verify(amLoginModuleBinder).getHttpServletResponse();
    }

    @Test
    public void shouldGetRequestOrg() {

        //Given

        //When
        authLoginModule.getRequestOrg();

        //Then
        verify(amLoginModuleBinder).getRequestOrg();
    }

    @Test
    public void shouldSetUserSessionProperty() throws AuthLoginException {

        //Given
        String name = "NAME";
        String value = "VALUE";

        //When
        authLoginModule.setUserSessionProperty(name, value);

        //Then
        verify(amLoginModuleBinder).setUserSessionProperty(name, value);
    }
}
