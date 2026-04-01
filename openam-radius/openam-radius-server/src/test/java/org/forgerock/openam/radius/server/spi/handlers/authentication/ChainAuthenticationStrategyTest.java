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
 * Copyright 2026 Ping Identity Corporation.
 */
package org.forgerock.openam.radius.server.spi.handlers.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import javax.security.auth.callback.Callback;

import org.forgerock.openam.authentication.callbacks.ValidatedUsernameCallback;
import org.forgerock.openam.radius.server.InvalidCallbackException;
import org.forgerock.openam.radius.server.spi.handlers.amhandler.ContextHolder;
import org.forgerock.openam.radius.server.spi.handlers.amhandler.OpenAMAuthFactory;
import org.forgerock.openam.test.extensions.LoggerExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.PagePropertiesCallback;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.locale.L10NMessageImpl;

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Tests for {@link ChainAuthenticationStrategy}.
 */
@ExtendWith(MockitoExtension.class)
class ChainAuthenticationStrategyTest {
    private static final String LEAVING_HANDLER_DEBUG_MESSAGE =
            "Leaving ChainAuthenticationStrategy.startAuthentication";
    private static final String REALM = "/";
    private static final String AUTH_CHAIN = "radiusChain";

    @RegisterExtension
    LoggerExtension logger = new LoggerExtension(ChainAuthenticationStrategy.class);

    @Mock
    private OpenAMAuthFactory amAuthFactory;

    private ChainAuthenticationStrategy strategy;

    private AuthContext authContext;

    @BeforeEach
    void setup() {
        strategy = new ChainAuthenticationStrategy(amAuthFactory, REALM, AUTH_CHAIN);
        authContext = mock(AuthContext.class);
    }

    @Test
    void shouldStartAuthentication() throws AuthLoginException {
        // Given
        ContextHolder holder = new ContextHolder("cache-key");
        given(amAuthFactory.getAuthContext(REALM)).willReturn(authContext);
        String clientIPAddress = "127.0.0.1";

        // When
        boolean started = strategy.startAuthentication(holder, clientIPAddress);

        // Then
        assertThat(started).isTrue();
        assertThat(holder.getAuthContext()).isSameAs(authContext);
        verify(amAuthFactory).getAuthContext(REALM);
        verify(authContext).setClientHostName(clientIPAddress);
        verify(authContext).login(AuthContext.IndexType.SERVICE, AUTH_CHAIN);
    }

    @Test
    void shouldNotStartAuthenticationGivenAuthContextCannotBeRetrieved() throws AuthLoginException {
        // Given
        ContextHolder holder = new ContextHolder("cache-key");
        given(amAuthFactory.getAuthContext(REALM)).willThrow(AuthLoginException.class);

        // When - Then
        assertThat(strategy.startAuthentication(holder, "")).isFalse();
        assertThat(holder.getAuthContext()).isNull();
        assertThat(logger.getErrors(ILoggingEvent::getMessage)).containsExactly("Unable to start create "
                + AuthContext.class.getName() + ". Denying Access.");
        assertThat(logger.getDebug(ILoggingEvent::getMessage)).contains(
                LEAVING_HANDLER_DEBUG_MESSAGE);
    }

    @Test
    void shouldFailToStartAuthenticationGivenLoginError() throws AuthLoginException {
        // Given
        ContextHolder holder = new ContextHolder("cache-key");
        given(amAuthFactory.getAuthContext(REALM)).willReturn(authContext);
        willThrow(AuthLoginException.class).given(authContext).login(AuthContext.IndexType.SERVICE, AUTH_CHAIN);

        // When - Then
        assertThat(strategy.startAuthentication(holder, "")).isFalse();
        assertThat(holder.getAuthContext()).isSameAs(authContext);
        assertThat(logger.getErrors(ILoggingEvent::getMessage)).containsExactly(
                "Unable to start login process. Denying Access.");
        assertThat(logger.getDebug(ILoggingEvent::getMessage)).contains(
                LEAVING_HANDLER_DEBUG_MESSAGE);
    }

    @Test
    void shouldGetNextCallbacksGivenNoMoreRequirements() throws InvalidCallbackException {
        // Given
        ContextHolder holder = createHolderWithAuthContext();

        // When - Then
        assertThat(strategy.getNextCallbacks(holder)).isEmpty();
    }

    @Test
    void shouldGetNextCallbacksGivenPagePropertiesCallback() throws InvalidCallbackException {
        // Given
        ContextHolder holder = createHolderWithAuthContext();
        PagePropertiesCallback pagePropertiesCallback = mock(PagePropertiesCallback.class);
        Callback callback = mock(Callback.class);
        Callback[] callbacks = new Callback[]{pagePropertiesCallback, callback};
        given(authContext.hasMoreRequirements()).willReturn(true);
        given(authContext.getRequirements(true)).willReturn(callbacks);

        // When - Then
        assertThat(strategy.getNextCallbacks(holder)).containsExactly(callbacks);
    }

    @Test
    void shouldFailToGetNextCallbacksGivenNoPagePropertiesCallback() {
        // Given
        ContextHolder holder = createHolderWithAuthContext();
        given(authContext.hasMoreRequirements()).willReturn(true);
        given(authContext.getRequirements(true)).willReturn(new Callback[]{mock(ValidatedUsernameCallback.class)});

        // When - Then
        assertThatThrownBy(() -> strategy.getNextCallbacks(holder))
                .isInstanceOf(InvalidCallbackException.class);
    }

    @Test
    void shouldSubmitCallbacks() {
        // Given
        ContextHolder holder = createHolderWithAuthContext();
        Callback[] callbacks = new Callback[]{mock(Callback.class)};

        // When
        strategy.submitCallbacks(holder, callbacks);

        // Then
        verify(authContext, times(1)).submitRequirements(callbacks);
    }

    @Test
    void shouldReturnAuthenticationSuccessful() {
        // Given
        ContextHolder holder = createHolderWithAuthContext();
        given(authContext.getStatus()).willReturn(AuthContext.Status.SUCCESS);

        // When - Then
        assertThat(strategy.isAuthenticationSuccessful(holder)).isTrue();
    }

    @Test
    void shouldNotReturnAuthenticationSuccessfulGivenAuthContextIsNull() {
        // Given
        ContextHolder holder = new ContextHolder("cache-key");

        // When - Then
        assertThat(strategy.isAuthenticationSuccessful(holder)).isFalse();
    }

    @Test
    void shouldNotReturnAuthenticationSuccessfulGivenStatusIsNotSuccess() {
        // Given
        ContextHolder holder = createHolderWithAuthContext();
        given(authContext.getStatus()).willReturn(AuthContext.Status.FAILED);

        // When - Then
        assertThat(strategy.isAuthenticationSuccessful(holder)).isFalse();
    }

    @Test
    void shouldTerminateAuthentication() {
        // Given
        ContextHolder holder = createHolderWithAuthContext();

        // When - Then
        assertThat(strategy.terminateAuthentication(holder)).isTrue();
    }

    @Test
    void shouldFailToTerminateAuthenticationGivenLogoutError() throws AuthLoginException {
        // Given
        ContextHolder holder = createHolderWithAuthContext();
        willThrow(AuthLoginException.class).given(authContext).logout();

        // When - Then
        assertThat(strategy.terminateAuthentication(holder)).isFalse();
    }

    @Test
    void shouldGetUniversalIdGivenSSOToken() throws L10NMessageImpl {
        // Given
        ContextHolder holder = createHolderWithAuthContext();
        SSOToken ssoToken = mock(SSOToken.class);
        String expectedUniversalId = "ssoTokenId";
        given(authContext.getSSOToken()).willReturn(ssoToken);
        given(ssoToken.getProperty(Constants.UNIVERSAL_IDENTIFIER)).willReturn(expectedUniversalId);

        // When - Then
        assertThat(strategy.getUniversalId(holder)).isEqualTo(expectedUniversalId);
    }

    @Test
    void shouldGetUniversalIdGivenNoSSOToken() {
        // Given
        ContextHolder holder = createHolderWithAuthContext();

        // When - Then
        assertThat(strategy.getUniversalId(holder)).isNull();
        assertThat(logger.getDebug(ILoggingEvent::getMessage)).contains(
                "No SSO token available from the auth context.");
    }

    @Test
    void shouldGetUniversalIdGivenErrorWhenRetrievingSSOToken() throws L10NMessageImpl {
        // Given
        ContextHolder holder = createHolderWithAuthContext();
        given(authContext.getSSOToken()).willThrow(L10NMessageImpl.class);

        // When - Then
        assertThat(strategy.getUniversalId(holder)).isNull();
        assertThat(logger.getWarnings(ILoggingEvent::getMessage)).contains(
                "Could not get universal ID from the SSOToken.");
    }

    private ContextHolder createHolderWithAuthContext() {
        ContextHolder holder = new ContextHolder("cache-key");
        holder.setAuthContext(authContext);
        return holder;
    }

}
