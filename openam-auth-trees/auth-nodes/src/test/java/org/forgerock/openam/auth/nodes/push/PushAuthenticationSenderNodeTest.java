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
 * Copyright 2023-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.push;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.push.PushAuthenticationSenderNode.FailureReason.MISSING_USERNAME;
import static org.forgerock.openam.auth.nodes.push.PushAuthenticationSenderNode.FailureReason.SENDER_ALREADY_USED;
import static org.forgerock.openam.auth.nodes.push.PushAuthenticationSenderNode.PUSH_AUTH_FAILURE_REASON;
import static org.forgerock.openam.auth.nodes.push.PushAuthenticationSenderNode.PushAuthenticationOutcomeProvider.PushAuthNOutcome.FAILURE;
import static org.forgerock.openam.auth.nodes.push.PushAuthenticationSenderNode.PushAuthenticationOutcomeProvider.PushAuthNOutcome.NOT_REGISTERED;
import static org.forgerock.openam.auth.nodes.push.PushAuthenticationSenderNode.PushAuthenticationOutcomeProvider.PushAuthNOutcome.SENT;
import static org.forgerock.openam.auth.nodes.push.PushAuthenticationSenderNode.PushAuthenticationOutcomeProvider.PushAuthNOutcome.SKIPPED;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.MESSAGE_ID_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.BoundedOutcomeProvider;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.LocaleSelector;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.auth.nodes.mfa.MultiFactorNodeDelegate;
import org.forgerock.openam.core.rest.devices.push.UserPushDeviceProfileManager;
import org.forgerock.openam.core.rest.devices.services.AuthenticatorDeviceServiceFactory;
import org.forgerock.openam.core.rest.devices.services.SkipSetting;
import org.forgerock.openam.services.push.MessageIdFactory;
import org.forgerock.openam.services.push.PushNotificationService;
import org.forgerock.openam.session.SessionCookies;
import org.forgerock.util.i18n.PreferredLocales;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sun.identity.idm.AMIdentity;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PushAuthenticationSenderNodeTest {

    private static final int DEFAULT_MESSAGE_TIMEOUT_CONFIG = 120000;
    private static final Map<Locale, String> DEFAULT_USER_MESSAGE_CONFIG = Collections.emptyMap();
    private static final boolean DEFAULT_MANDATORY_CONFIG = false;
    private static final boolean DEFAULT_CONTEXT_INFO_CONFIG = false;
    private static final Set<String> DEFAULT_CUSTOM_PAYLOAD_CONFIG = Collections.emptySet();
    private static final PushType DEFAULT_PUSH_TYPE_CONFIG = PushType.DEFAULT;
    private static final boolean DEFAULT_CAPTURE_FAILURE_CONFIG = false;

    @Mock
    private PushAuthenticationSenderNode.Config config;
    @Mock
    private UserPushDeviceProfileManager userPushDeviceProfileManager;
    @Mock
    private PushNotificationService pushNotificationService;
    @Mock
    private SessionCookies sessionCookies;
    @Mock
    private MessageIdFactory messageIdFactory;
    @Mock
    private LocaleSelector localeSelector;

    private PushAuthenticationSenderNode node;
    @Mock
    private NodeUserIdentityProvider identityProvider;

    @Test
    void processThrowExceptionIfUserNameNotPresentInSharedStateAndCaptureFailureIsDisabled() throws Exception {
        // Given
        JsonValue sharedState = json(object());
        JsonValue transientState = json(object());

        // When
        whenNodeConfigHasDefaultValues();

        // Then
        // throw exception
        assertThatThrownBy(() -> node.process(getContext(sharedState, transientState, emptyList())))
                .isInstanceOf(NodeProcessException.class);
    }

    @Test
    void returnFailureOutcomeIfUserNameNotPresentInSharedStateAndCaptureFailureIsEnabled() throws Exception {
        // Given
        JsonValue sharedState = json(object());
        JsonValue transientState = json(object());

        whenNodeConfigHasValues(
                DEFAULT_MESSAGE_TIMEOUT_CONFIG,
                DEFAULT_USER_MESSAGE_CONFIG,
                DEFAULT_MANDATORY_CONFIG,
                DEFAULT_CONTEXT_INFO_CONFIG,
                DEFAULT_CUSTOM_PAYLOAD_CONFIG,
                DEFAULT_PUSH_TYPE_CONFIG,
                true
        );

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        // Then
        assertThat(result.outcome).isEqualTo(FAILURE.toString());
        assertThat(result.sharedState.isDefined(PUSH_AUTH_FAILURE_REASON)).isTrue();
        assertThat(result.sharedState.get(PUSH_AUTH_FAILURE_REASON).asString()).isEqualTo(MISSING_USERNAME.name());
    }

    @Test
    void processThrowExceptionIfMessageIdPresentInSharedStateAndCaptureFailureIsDisabled() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(USERNAME, "rod"),
                field(MESSAGE_ID_KEY, "someMessageId"))
        );
        JsonValue transientState = json(object());

        // When
        whenNodeConfigHasDefaultValues();

        // Then
        // throw exception
        assertThatThrownBy(() -> node.process(getContext(sharedState, transientState, emptyList())))
                .isInstanceOf(NodeProcessException.class);
    }

    @Test
    void returnFailureOutcomeIfMessageIdPresentInSharedStateAndCaptureFailureIsEnabled() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(USERNAME, "rod"),
                field(MESSAGE_ID_KEY, "someMessageId"))
        );
        JsonValue transientState = json(object());

        whenNodeConfigHasValues(
                DEFAULT_MESSAGE_TIMEOUT_CONFIG,
                DEFAULT_USER_MESSAGE_CONFIG,
                DEFAULT_MANDATORY_CONFIG,
                DEFAULT_CONTEXT_INFO_CONFIG,
                DEFAULT_CUSTOM_PAYLOAD_CONFIG,
                DEFAULT_PUSH_TYPE_CONFIG,
                true
        );

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        // Then
        assertThat(result.outcome).isEqualTo(FAILURE.name());
        assertThat(result.sharedState.isDefined(PUSH_AUTH_FAILURE_REASON)).isTrue();
        assertThat(result.sharedState.get(PUSH_AUTH_FAILURE_REASON).asString()).isEqualTo(SENDER_ALREADY_USED.name());
    }

    @Test
    void returnSkipOutcomeIfUserHasSkipEnabled() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(USERNAME, "rod"),
                field(REALM, "root"))
        );
        JsonValue transientState = json(object());

        whenNodeConfigHasDefaultValues();

        doReturn(mock(AMIdentity.class))
                .when(node).getIdentityFromIdentifier(any());
        doReturn(SkipSetting.SKIPPABLE)
                .when(node).shouldSkip(any(), anyString());

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        // Then
        assertThat(result.outcome).isEqualTo(SKIPPED.name());
    }

    @Test
    void returnNotRegisteredOutcomeIfUserHasSkipNotSet() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(USERNAME, "rod"),
                field(REALM, "root"))
        );
        JsonValue transientState = json(object());

        whenNodeConfigHasDefaultValues();

        doReturn(mock(AMIdentity.class))
                .when(node).getIdentityFromIdentifier(any());
        doReturn(SkipSetting.NOT_SET)
                .when(node).shouldSkip(any(), anyString());

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        // Then
        assertThat(result.outcome).isEqualTo(NOT_REGISTERED.name());
    }

    @Test
    void returnNotRegisteredOutcomeIfUserHasNotSkippableSetAndNoDevice() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(USERNAME, "rod"),
                field(REALM, "root"))
        );
        JsonValue transientState = json(object());

        whenNodeConfigHasDefaultValues();

        doReturn(mock(AMIdentity.class))
                .when(node).getIdentityFromIdentifier(any());
        doReturn(SkipSetting.NOT_SKIPPABLE)
                .when(node).shouldSkip(any(), anyString());

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        // Then
        assertThat(result.outcome).isEqualTo(NOT_REGISTERED.name());
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState,
                                   List<? extends Callback> callbacks) {
        return new TreeContext(
                sharedState, transientState, new ExternalRequestContext.Builder().build(),
                callbacks, Optional.empty()
        );
    }

    private void whenNodeConfigHasDefaultValues() {
        whenNodeConfigHasValues(
                DEFAULT_MESSAGE_TIMEOUT_CONFIG,
                DEFAULT_USER_MESSAGE_CONFIG,
                DEFAULT_MANDATORY_CONFIG,
                DEFAULT_CONTEXT_INFO_CONFIG,
                DEFAULT_CUSTOM_PAYLOAD_CONFIG,
                DEFAULT_PUSH_TYPE_CONFIG,
                DEFAULT_CAPTURE_FAILURE_CONFIG
        );
    }

    private void whenNodeConfigHasValues(int messageTimeout, Map<Locale, String> userMessage,
                                                boolean mandatory, boolean contextInfo, Set<String> customPayload,
                                                PushType pushType, boolean captureFailure) {
        config = mock(PushAuthenticationSenderNode.Config.class);

        given(config.mandatory()).willReturn(mandatory);
        given(config.captureFailure()).willReturn(captureFailure);

        MultiFactorNodeDelegate multiFactorNodeDelegate = new MultiFactorNodeDelegate(
            mock(AuthenticatorDeviceServiceFactory.class)
        );

        node = spy(
            new PushAuthenticationSenderNode(
                config,
                userPushDeviceProfileManager,
                pushNotificationService,
                sessionCookies,
                multiFactorNodeDelegate,
                localeSelector,
                messageIdFactory,
                identityProvider)
        );
    }

    @Test
    void shouldReturnAllOutcomesWhenGetAllOutcomes() throws NodeProcessException {
        // given
        BoundedOutcomeProvider provider = new PushAuthenticationSenderNode.PushAuthenticationOutcomeProvider();

        // when
        var outcomes = provider.getAllOutcomes(new PreferredLocales());

        // then
        assertThat(outcomes.stream().map(outcome -> outcome.id).toList())
                .containsExactly(SENT.name(), NOT_REGISTERED.name(), SKIPPED.name(), FAILURE.name());
    }

    @Test
    void shouldReturnAllOutcomesWhenMandatoryIsFalseAndCaptureFailureIsTrue() throws NodeProcessException {
        // given
        OutcomeProvider provider = new PushAuthenticationSenderNode.PushAuthenticationOutcomeProvider();
        var attributes = json(object(
                field("mandatory", false),
                field("captureFailure", true)
        ));

        // when
        var outcomes = provider.getOutcomes(new PreferredLocales(), attributes);

        // then
        assertThat(outcomes.stream().map(outcome -> outcome.id).toList())
                .containsExactly(SENT.name(), NOT_REGISTERED.name(), SKIPPED.name(), FAILURE.name());
    }

    @Test
    void shouldNotReturnSkippedOutcomeWhenMandatoryIsTrue() throws NodeProcessException {
        // given
        OutcomeProvider provider = new PushAuthenticationSenderNode.PushAuthenticationOutcomeProvider();
        var attributes = json(object(
                field("mandatory", true),
                field("captureFailure", true)
        ));

        // when
        var outcomes = provider.getOutcomes(new PreferredLocales(), attributes);

        // then
        assertThat(outcomes.stream().map(outcome -> outcome.id).toList())
                .containsExactly(SENT.name(), NOT_REGISTERED.name(), FAILURE.name());
    }

    @Test
    void shouldNotReturnFailureOutcomeWhenCaptureFailureIsFalse() throws NodeProcessException {
        // given
        OutcomeProvider provider = new PushAuthenticationSenderNode.PushAuthenticationOutcomeProvider();
        var attributes = json(object(
                field("mandatory", true),
                field("captureFailure", false)
        ));

        // when
        var outcomes = provider.getOutcomes(new PreferredLocales(), attributes);

        // then
        assertThat(outcomes.stream().map(outcome -> outcome.id).toList())
                .containsExactly(SENT.name(), NOT_REGISTERED.name());
    }

    @Test
    void shouldNotReturnFailureOutcomeWhenAttributesIsNull() throws NodeProcessException {
        // given
        OutcomeProvider provider = new PushAuthenticationSenderNode.PushAuthenticationOutcomeProvider();
        var attributes = json(null);

        // when
        var outcomes = provider.getOutcomes(new PreferredLocales(), attributes);

        // then
        assertThat(outcomes.stream().map(outcome -> outcome.id).toList())
                .containsExactly(SENT.name(), NOT_REGISTERED.name(), SKIPPED.name());
    }
}
