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
 * Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 * Portions Copyrighted 2017-2026 Ping Identity Corporation.
 */

package org.forgerock.openam.radius.server.spi.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.openam.radius.common.AttributeType.REPLY_MESSAGE;
import static org.forgerock.openam.radius.common.AttributeType.STATE;
import static org.forgerock.openam.radius.common.PacketType.ACCESS_ACCEPT;
import static org.forgerock.openam.radius.common.PacketType.ACCESS_CHALLENGE;
import static org.forgerock.openam.radius.common.PacketType.ACCESS_REJECT;
import static org.forgerock.openam.radius.server.spi.handlers.amhandler.ContextHolder.AuthPhase.GATHERING_INPUT;
import static org.forgerock.openam.radius.server.spi.handlers.amhandler.ContextHolder.AuthPhase.TERMINATED;
import static org.forgerock.openam.radius.server.testsupport.RadiusRequestTestFactory.accessRequestWithAnswer;
import static org.forgerock.openam.radius.server.testsupport.RadiusRequestTestFactory.basicAccessRequest;
import static org.forgerock.openam.radius.server.testsupport.RadiusRequestTestFactory.requestBuilder;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import org.forgerock.am.trees.api.Outcome;
import org.forgerock.am.trees.model.TreeResult;
import org.forgerock.am.trees.model.TreeState;
import org.forgerock.openam.authentication.callbacks.IdPCallback;
import org.forgerock.openam.radius.common.ReplyMessageAttribute;
import org.forgerock.openam.radius.common.StateAttribute;
import org.forgerock.openam.radius.server.InvalidCallbackException;
import org.forgerock.openam.radius.server.InvalidRadiusClientConfigurationException;
import org.forgerock.openam.radius.server.RadiusProcessingException;
import org.forgerock.openam.radius.server.RadiusRequest;
import org.forgerock.openam.radius.server.RadiusRequestContext;
import org.forgerock.openam.radius.server.RadiusResponse;
import org.forgerock.openam.radius.server.spi.handlers.amhandler.ContextHolder;
import org.forgerock.openam.radius.server.spi.handlers.amhandler.ContextHolderCache;
import org.forgerock.openam.radius.server.spi.handlers.authentication.AuthenticationStrategy;
import org.forgerock.openam.radius.server.spi.handlers.authentication.AuthenticationStrategyFactory;
import org.forgerock.openam.radius.server.testsupport.RadiusRequestTestFactory;
import org.forgerock.openam.test.extensions.LoggerExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.eventbus.EventBus;
import com.sun.identity.authentication.spi.PagePropertiesCallback;
import com.sun.identity.authentication.spi.RedirectCallback;

/**
 * Tests for {@link OpenAMAuthHandler}.
 */
@ExtendWith(MockitoExtension.class)
public class OpenAMAuthHandlerTest {

    private static final String REALM_PROPERTY_KEY = "realm";
    private static final String CHAIN_PROPERTY_KEY = "chain";
    private static final String TEST_REALM = "testRealm";
    private static final String TEST_CHAIN = "testChain";
    private static final String TEST_MODULE = "testModule";
    private static final String USER_NAME = "testUser";
    private static final String USER_PASSWORD = "testPassword";
    private static final String STATE_ATTRIBUTE = "cachedState";

    @RegisterExtension
    private final LoggerExtension loggerExtension = new LoggerExtension(OpenAMAuthHandler.class);

    @Mock
    private ContextHolderCache contextHolderCache;

    @Mock
    private EventBus eventBus;

    @Mock
    private AuthenticationStrategyFactory authenticationStrategyFactory;

    @InjectMocks
    private OpenAMAuthHandler openAMAuthHandler;

    private AuthenticationStrategy authenticationStrategy;

    private Properties config;

    @BeforeEach
    void setUp() throws InvalidRadiusClientConfigurationException {
        config = new Properties();
        config.setProperty(REALM_PROPERTY_KEY, TEST_REALM);
        config.setProperty(CHAIN_PROPERTY_KEY, TEST_CHAIN);
        authenticationStrategy = mock(AuthenticationStrategy.class);
        given(authenticationStrategyFactory.create(TEST_REALM, null, TEST_CHAIN))
                .willReturn(authenticationStrategy);
        openAMAuthHandler.init(config);
    }

    @Test
    void shouldFailToInitGivenMissingRealmProperty() {
        // Given
        clearInvocations(authenticationStrategyFactory);
        config.remove(REALM_PROPERTY_KEY);

        // When - Then
        assertThatThrownBy(() -> openAMAuthHandler.init(config))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Configuration property 'realm' not found");
        verifyNoInteractions(authenticationStrategyFactory);
    }

    @Test
    void shouldFailToInitGivenEmptyRealmProperty() {
        // Given
        clearInvocations(authenticationStrategyFactory);
        config.setProperty(REALM_PROPERTY_KEY, "");

        // When - Then
        assertThatThrownBy(() -> openAMAuthHandler.init(config))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Configuration property 'realm' not found");
        verifyNoInteractions(authenticationStrategyFactory);
    }

    @Test
    void handleShouldRespondWithAccessAccept() throws RadiusProcessingException, InvalidCallbackException {
        // Given
        RadiusRequest request = basicAccessRequest(USER_PASSWORD, USER_NAME);
        RadiusResponse response = new RadiusResponse();

        ContextHolder holder = new ContextHolder("randomKey");
        given(contextHolderCache.createCachedContextHolder()).willReturn(holder);
        given(authenticationStrategy.startAuthentication(holder, null)).willReturn(true);
        NameCallback nameCallback = new NameCallback("Username:");
        PasswordCallback passwordCallback = new PasswordCallback("Password:", false);
        Callback[] callbacks = new Callback[]{nameCallback, passwordCallback};
        given(authenticationStrategy.getNextCallbacks(holder)).willReturn(callbacks, new Callback[0]);
        given(authenticationStrategy.isAuthenticationSuccessful(holder)).willReturn(true);
        String universalId = "universalId";
        given(authenticationStrategy.getUniversalId(holder)).willReturn(universalId);

        // When
        openAMAuthHandler.handle(request, response, mockContext(request));

        // Then
        assertThat(response.getResponsePacket().getType()).isEqualTo(ACCESS_ACCEPT);
        assertThat(response.getUniversalId()).isEqualTo(universalId);
        assertThat(nameCallback.getName()).isEqualTo(USER_NAME);
        assertThat(passwordCallback.getPassword()).isEqualTo(USER_PASSWORD.toCharArray());
        assertThat(holder.getAuthPhase()).isEqualTo(TERMINATED);
    }

    @Test
    void handleShouldRespondWithAccessAcceptGivenState() throws RadiusProcessingException, InvalidCallbackException {
        // Given
        RadiusRequest request = accessRequestWithAnswer("unused", STATE_ATTRIBUTE);
        RadiusResponse response = new RadiusResponse();

        ContextHolder holder = new ContextHolder(STATE_ATTRIBUTE);
        holder.setAuthPhase(GATHERING_INPUT);
        given(contextHolderCache.get(STATE_ATTRIBUTE)).willReturn(holder);
        given(authenticationStrategy.getNextCallbacks(holder)).willReturn(new Callback[0]);
        given(authenticationStrategy.isAuthenticationSuccessful(holder)).willReturn(true);
        String universalId = "universalId";
        given(authenticationStrategy.getUniversalId(holder)).willReturn(universalId);

        // When
        openAMAuthHandler.handle(request, response, mockContext(request));

        // Then
        assertThat(response.getResponsePacket().getType()).isEqualTo(ACCESS_ACCEPT);
        assertThat(response.getUniversalId()).isEqualTo(universalId);
    }

    @Test
    void handleShouldRespondWithAccessAcceptGivenRequestWithAnswer()
            throws RadiusProcessingException, InvalidCallbackException {
        // Given
        String nameCallbackAnswer = "NameAnswer";
        RadiusRequest request = accessRequestWithAnswer(nameCallbackAnswer, STATE_ATTRIBUTE);
        RadiusResponse response = new RadiusResponse();

        NameCallback nameCallback = new NameCallback("Username:");
        ContextHolder holder = inProgressHolder(new NameCallback[]{nameCallback});
        given(contextHolderCache.get(STATE_ATTRIBUTE)).willReturn(holder);
        given(authenticationStrategy.getNextCallbacks(holder)).willReturn(new Callback[0]);
        given(authenticationStrategy.isAuthenticationSuccessful(holder)).willReturn(true);

        // When
        openAMAuthHandler.handle(request, response, mockContext(request));

        // Then
        assertThat(response.getResponsePacket().getType()).isEqualTo(ACCESS_ACCEPT);
        assertThat(nameCallback.getName()).isEqualTo(nameCallbackAnswer);
    }

    @Test
    void handleShouldRespondWithAccessChallengeGivenRequestWithNoAnswer()
            throws RadiusProcessingException, InvalidCallbackException {
        // Given
        RadiusRequest request = basicAccessRequest(USER_PASSWORD, USER_NAME);
        RadiusResponse response = new RadiusResponse();

        ContextHolder holder = new ContextHolder(STATE_ATTRIBUTE);
        given(contextHolderCache.createCachedContextHolder()).willReturn(holder);
        given(authenticationStrategy.startAuthentication(holder, null)).willReturn(true);
        Callback[] basicCallbacks = createBasicCallbacks();
        ChoiceCallback choiceCallback = new ChoiceCallback("Test option prompt", new String[]{"one", "two"}, 0, false);
        Callback[] unansweredCallbacks = new Callback[]{
                new PagePropertiesCallback(TEST_MODULE, "Test Header", null, 30, null, false, null), choiceCallback};
        given(authenticationStrategy.getNextCallbacks(holder)).willReturn(basicCallbacks, unansweredCallbacks);

        // When
        openAMAuthHandler.handle(request, response, mockContext(request));

        // Then
        assertThat(response.getResponsePacket().getType()).isEqualTo(ACCESS_CHALLENGE);
        assertThat(((ReplyMessageAttribute) response.getResponsePacket().getAttributeSet()
                .getAttributeByType(REPLY_MESSAGE)).getMessage()).isEqualTo("""
                Test Header Test option prompt
                0 = one,
                1 = two""");
        assertThat(((StateAttribute) response.getResponsePacket().getAttributeSet()
                .getAttributeByType(STATE)).getState()).isEqualTo(STATE_ATTRIBUTE);
        assertThat(holder.getAuthPhase()).isEqualTo(GATHERING_INPUT);
    }

    @Test
    void handleShouldRespondWithAccessChallengeGivenRequestWithNoAnswerAndTreeHeader()
            throws RadiusProcessingException, InvalidCallbackException {
        // Given
        RadiusRequest request = basicAccessRequest(USER_PASSWORD, USER_NAME);
        RadiusResponse response = new RadiusResponse();

        ContextHolder holder = new ContextHolder(STATE_ATTRIBUTE);
        holder.setTreeResult(new TreeResult(mock(TreeState.class), Outcome.NEED_INPUT,
                Map.of("header", "Tree Header"), List.of()));
        given(contextHolderCache.createCachedContextHolder()).willReturn(holder);
        given(authenticationStrategy.startAuthentication(holder, null)).willReturn(true);
        Callback[] basicCallbacks = createBasicCallbacks();
        ChoiceCallback choiceCallback = new ChoiceCallback("Test option prompt", new String[]{"one", "two"}, 0, false);
        Callback[] unansweredCallbacks = new Callback[]{choiceCallback};
        given(authenticationStrategy.getNextCallbacks(holder)).willReturn(basicCallbacks, unansweredCallbacks);

        // When
        openAMAuthHandler.handle(request, response, mockContext(request));

        // Then
        assertThat(response.getResponsePacket().getType()).isEqualTo(ACCESS_CHALLENGE);
        assertThat(((ReplyMessageAttribute) response.getResponsePacket().getAttributeSet()
                .getAttributeByType(REPLY_MESSAGE)).getMessage()).isEqualTo("""
                Tree Header Test option prompt
                0 = one,
                1 = two""");
        assertThat(((StateAttribute) response.getResponsePacket().getAttributeSet()
                .getAttributeByType(STATE)).getState()).isEqualTo(STATE_ATTRIBUTE);
        assertThat(holder.getAuthPhase()).isEqualTo(GATHERING_INPUT);
    }

    @Test
    void handleShouldRespondWithAccessRejectGivenLoginFailed()
            throws RadiusProcessingException, InvalidCallbackException {
        // Given
        RadiusRequest request = basicAccessRequest(USER_PASSWORD, USER_NAME);
        RadiusResponse response = new RadiusResponse();

        ContextHolder holder = new ContextHolder("randomKey");
        given(contextHolderCache.createCachedContextHolder()).willReturn(holder);
        given(authenticationStrategy.startAuthentication(holder, null)).willReturn(true);
        given(authenticationStrategy.getNextCallbacks(holder)).willReturn(createBasicCallbacks(), new Callback[0]);
        given(authenticationStrategy.isAuthenticationSuccessful(holder)).willReturn(false);

        // When
        openAMAuthHandler.handle(request, response, mockContext(request));

        // Then
        assertThat(response.getResponsePacket().getType()).isEqualTo(ACCESS_REJECT);
        assertThat(holder.getAuthPhase()).isEqualTo(ContextHolder.AuthPhase.TERMINATED);
    }

    @Test
    void handleShouldRespondWithAccessRejectGivenRequestWithNoUserNameAttribute() throws RadiusProcessingException {
        // Given
        RadiusRequest request = requestBuilder()
                .withUserPasswordAttribute(USER_PASSWORD)
                .build();
        RadiusResponse response = new RadiusResponse();
        given(contextHolderCache.createCachedContextHolder()).willReturn(mock(ContextHolder.class));

        // When
        openAMAuthHandler.handle(request, response, mockContext(request));

        // Then
        assertThat(response.getResponsePacket().getType()).isEqualTo(ACCESS_REJECT);
        assertThat(loggerExtension.getFormattedErrors())
                .containsExactly("Request is missing USER_NAME attribute. Denying Access.");
    }

    @Test
    void handleShouldRespondWithAccessRejectGivenNoCallbacks()
            throws RadiusProcessingException, InvalidCallbackException {
        // Given
        RadiusRequest request = basicAccessRequest(USER_PASSWORD, USER_NAME);
        RadiusResponse response = new RadiusResponse();

        ContextHolder holder = new ContextHolder("randomKey");
        given(contextHolderCache.createCachedContextHolder()).willReturn(holder);
        given(authenticationStrategy.startAuthentication(holder, null)).willReturn(true);
        given(authenticationStrategy.getNextCallbacks(holder)).willReturn(new Callback[0]);

        // When
        openAMAuthHandler.handle(request, response, mockContext(request));

        // Then
        assertThat(response.getResponsePacket().getType()).isEqualTo(ACCESS_REJECT);
        assertThat(loggerExtension.getFormattedErrors())
                .containsExactly("Unable to start login process. No callbacks available. Denying Access.");
    }

    @Test
    void handleShouldRespondWithAccessRejectGivenInvalidCallback()
            throws RadiusProcessingException, InvalidCallbackException {
        // Given
        RadiusRequest request = basicAccessRequest(USER_PASSWORD, USER_NAME);
        RadiusResponse response = new RadiusResponse();

        ContextHolder holder = new ContextHolder("randomKey");
        given(contextHolderCache.createCachedContextHolder()).willReturn(holder);
        given(authenticationStrategy.startAuthentication(holder, null)).willReturn(true);
        Callback invalidCallback = new RedirectCallback();
        PagePropertiesCallback pagePropertiesCallback =
                new PagePropertiesCallback(TEST_MODULE, "Test Header", null, 30, null, false, null);
        Callback[] callbacks = new Callback[]{pagePropertiesCallback, invalidCallback};
        given(authenticationStrategy.getNextCallbacks(holder)).willReturn(callbacks);

        // When
        openAMAuthHandler.handle(request, response, mockContext(request));

        // Then
        assertThat(response.getResponsePacket().getType()).isEqualTo(ACCESS_REJECT);
        assertThat(loggerExtension.getFormattedErrors())
                .containsExactly("Unable to start login process. Invalid callback encountered. Denying Access.");
        assertThat(holder.getCallbackSetProps()).isEqualTo(pagePropertiesCallback);
        assertThat(holder.getModuleName()).isEqualTo(TEST_MODULE);
    }

    @Test
    void handleShouldRespondWithAccessRejectGivenLoginUnsuccessful() throws RadiusProcessingException {
        // Given
        RadiusRequest request = basicAccessRequest(USER_PASSWORD, USER_NAME);
        RadiusResponse response = new RadiusResponse();

        ContextHolder holder = new ContextHolder("randomKey");
        given(contextHolderCache.createCachedContextHolder()).willReturn(holder);
        given(authenticationStrategy.startAuthentication(holder, null)).willReturn(false);

        // When
        openAMAuthHandler.handle(request, response, mockContext(request));

        // Then
        assertThat(response.getResponsePacket().getType()).isEqualTo(ACCESS_REJECT);
        assertThat(loggerExtension.getFormattedErrors())
                .containsExactly("Failed to start login process. Denying Access.");
    }

    @Test
    void handleShouldRespondWithAccessRejectGivenCallbackSubmissionFailed() throws RadiusProcessingException {
        // Given
        RadiusRequest request = accessRequestWithAnswer("failingAnswer", STATE_ATTRIBUTE);
        RadiusResponse response = new RadiusResponse();

        Callback[] callbacks = {new NameCallback("Username:")};
        ContextHolder holder = inProgressHolder(callbacks);
        given(contextHolderCache.get(STATE_ATTRIBUTE)).willReturn(holder);
        willThrow(RuntimeException.class).given(authenticationStrategy).submitCallbacks(holder, callbacks);

        // When
        openAMAuthHandler.handle(request, response, mockContext(request));

        // Then
        assertThat(response.getResponsePacket().getType()).isEqualTo(ACCESS_REJECT);
        assertThat(loggerExtension.getFormattedErrors())
                .containsExactly("Exception thrown while submitting callbacks. Rejecting access.");
    }

    @Test
    void handleShouldRespondWithAccessRejectGivenUnrecognisedCallback() throws RadiusProcessingException {
        // Given
        RadiusRequest request = accessRequestWithAnswer("unused", STATE_ATTRIBUTE);
        RadiusResponse response = new RadiusResponse();

        Callback[] unrecognisedCallbacks = {new IdPCallback()};
        ContextHolder holder = inProgressHolder(unrecognisedCallbacks);
        given(contextHolderCache.get(STATE_ATTRIBUTE)).willReturn(holder);

        // When
        openAMAuthHandler.handle(request, response, mockContext(request));

        // Then
        assertThat(response.getResponsePacket().getType()).isEqualTo(ACCESS_REJECT);
        assertThat(loggerExtension.getFormattedErrors()).containsExactly("Unrecognized callback type "
                + "'IdPCallback' while processing challenge response. Unable to submit answer. Denying Access.");
    }

    @Test
    void handleShouldRespondWithAccessRejectGivenInvalidAnswer() throws RadiusProcessingException {
        // Given
        RadiusRequest request = accessRequestWithAnswer("not-a-number", STATE_ATTRIBUTE);
        RadiusResponse response = new RadiusResponse();

        Callback[] callbacks = {new ConfirmationCallback(0, new String[]{"yes", "no"}, 0)};
        ContextHolder holder = inProgressHolder(callbacks);
        given(contextHolderCache.get(STATE_ATTRIBUTE)).willReturn(holder);

        // When
        openAMAuthHandler.handle(request, response, mockContext(request));

        // Then
        assertThat(response.getResponsePacket().getType()).isEqualTo(ACCESS_REJECT);
        assertThat(loggerExtension.getFormattedErrors()).containsExactly("Invalid number 'not-a-number' specified "
                + "in answer 'not-a-number' for callback 1 of type ConfirmationCallback for callback set 0 "
                + "in realm testRealm. Denying Access.");
    }

    @Test
    void handleShouldRespondWithAccessRejectGivenAnswerOutOfRange() throws RadiusProcessingException {
        // Given
        RadiusRequest request = accessRequestWithAnswer("2", STATE_ATTRIBUTE);
        RadiusResponse response = new RadiusResponse();

        Callback[] callbacks = {new ConfirmationCallback(0, new String[]{"yes", "no"}, 0)};
        ContextHolder holder = inProgressHolder(callbacks);
        given(contextHolderCache.get(STATE_ATTRIBUTE)).willReturn(holder);

        // When
        openAMAuthHandler.handle(request, response, mockContext(request));

        // Then
        assertThat(response.getResponsePacket().getType()).isEqualTo(ACCESS_REJECT);
        assertThat(loggerExtension.getFormattedErrors()).containsExactly("Out of range index specified in answer '2' "
                + "for callback 1 of type ConfirmationCallback for callback set 0 in realm testRealm. "
                + "Must be from 0 to 1. Denying Access.");
    }

    @Test
    void handleShouldRespondWithAccessRejectGivenRequestWithNoUserPasswordAttribute() throws RadiusProcessingException {
        // Given
        RadiusRequest request = requestBuilder().build();
        RadiusResponse response = new RadiusResponse();

        // When
        openAMAuthHandler.handle(request, response, mock(RadiusRequestContext.class));

        // Then
        assertThat(response.getResponsePacket().getType()).isEqualTo(ACCESS_REJECT);
    }

    @Test
    void handleShouldRespondWithAccessRejectGivenNoNameCallback()
            throws RadiusProcessingException, InvalidCallbackException {
        // Given
        RadiusRequest request = basicAccessRequest(USER_PASSWORD, USER_NAME);
        RadiusResponse response = new RadiusResponse();

        ContextHolder holder = new ContextHolder("randomKey");
        given(contextHolderCache.createCachedContextHolder()).willReturn(holder);
        given(authenticationStrategy.startAuthentication(holder, null)).willReturn(true);
        Callback[] callbacks = new Callback[]{new PasswordCallback("Password:", false)};
        given(authenticationStrategy.getNextCallbacks(holder)).willReturn(callbacks);

        // When
        openAMAuthHandler.handle(request, response, mockContext(request));

        // Then
        assertThat(response.getResponsePacket().getType()).isEqualTo(ACCESS_REJECT);
    }

    @Test
    void handleShouldRespondWithAccessRejectGivenNoPasswordCallback()
            throws RadiusProcessingException, InvalidCallbackException {
        // Given
        RadiusRequest request = basicAccessRequest(USER_PASSWORD, USER_NAME);
        RadiusResponse response = new RadiusResponse();

        ContextHolder holder = new ContextHolder("randomKey");
        given(contextHolderCache.createCachedContextHolder()).willReturn(holder);
        given(authenticationStrategy.startAuthentication(holder, null)).willReturn(true);
        Callback[] callbacks = new Callback[]{new NameCallback("Username:")};
        given(authenticationStrategy.getNextCallbacks(holder)).willReturn(callbacks);

        // When
        openAMAuthHandler.handle(request, response, mockContext(request));

        // Then
        assertThat(response.getResponsePacket().getType()).isEqualTo(ACCESS_REJECT);
    }

    private RadiusRequestContext mockContext(RadiusRequest request) {
        RadiusRequestContext context = mock(RadiusRequestContext.class);
        given(context.getRequestAuthenticator()).willReturn(request.getRequestPacket().getAuthenticator());
        given(context.getClientSecret()).willReturn(RadiusRequestTestFactory.HMAC_MD5_SECRET);
        return context;
    }

    private Callback[] createBasicCallbacks() {
        NameCallback nameCallback = new NameCallback("Username:");
        PasswordCallback passwordCallback = new PasswordCallback("Password:", false);
        return new Callback[]{nameCallback, passwordCallback};
    }

    private ContextHolder inProgressHolder(Callback[] callbacks) {
        ContextHolder holder = new ContextHolder(STATE_ATTRIBUTE);
        holder.setAuthPhase(GATHERING_INPUT);
        holder.setCallbacks(callbacks);
        return holder;
    }
}
