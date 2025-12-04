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
 * Copyright 2022-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.push;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.PUSH_NUMBER_CHALLENGE_KEY;
import static org.forgerock.openam.auth.nodes.push.PushWaitNode.DEFAULT_CHALLENGE_MESSAGE_KEY;
import static org.forgerock.openam.auth.nodes.push.PushWaitNode.DEFAULT_EXIT_MESSAGE_KEY;
import static org.forgerock.openam.auth.nodes.push.PushWaitNode.DEFAULT_WAITING_MESSAGE_KEY;
import static org.forgerock.openam.auth.nodes.push.PushWaitNode.PUSH_CHALLENGE_CALLBACK_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.LocalizedMessageProvider;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.authentication.callbacks.PollingWaitCallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import com.sun.identity.authentication.callbacks.HiddenValueCallback;

/**
 * Test the Push Wait node.
 */
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class PushWaitNodeTest {

    private static final String DEFAULT_CHALLENGE_MESSAGE = "CHALLENGE MESSAGE";
    private static final String DEFAULT_EXIT_MESSAGE = "EXIT MESSAGE";
    private static final String DEFAULT_WAITING_MESSAGE = "WAITING MESSAGE";
    private static final String NUMBER_CHALLENGE = "20";
    private static final int SECONDS_TO_WAIT = 5;
    private static final Map<Locale, String> MAP_WAITING_MESSAGE = new HashMap<>() {{
        put(Locale.CANADA, DEFAULT_WAITING_MESSAGE);
    }};
    private static final Map<Locale, String> MAP_CHALLENGE_MESSAGE = new HashMap<>() {{
        put(Locale.CANADA, DEFAULT_CHALLENGE_MESSAGE);
    }};
    private static final Map<Locale, String> MAP_EXIT_MESSAGE = new HashMap<>() {{
        put(Locale.CANADA, DEFAULT_EXIT_MESSAGE);
    }};
    private static final Locale DEFAULT_LOCALE = Locale.CANADA;
    @Mock
    PushWaitNode.Config config;
    PushWaitNode node;
    @Mock
    private LocalizedMessageProvider localizationHelper;
    private UUID uuid;
    private TreeContext context;

    @BeforeEach
    void setup() throws NodeProcessException {
        uuid = UUID.randomUUID();
    }

    @Test
    void processNoException() throws Exception {
        // Given
        whenNodeConfigHasDefaultValues();
        context = getContext();

        PushWaitNode node = new PushWaitNode(config, uuid, null, realm -> localizationHelper);

        // When
        node.process(context);

        // Then
        // no exception
    }

    @Test
    void processShouldReturnCorrectWaitTime() throws Exception {
        // Given
        JsonValue sharedState = json(object());
        JsonValue transientState = json(object());
        context = getContext(sharedState, transientState, emptyList());

        whenNodeConfigHasDefaultValues(
                MAP_WAITING_MESSAGE,
                MAP_CHALLENGE_MESSAGE,
                MAP_EXIT_MESSAGE,
                10);

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.callbacks.size()).isEqualTo(2);
        assertThat(result.callbacks.get(0)).isInstanceOf(PollingWaitCallback.class);
        assertThat(((PollingWaitCallback) result.callbacks.get(0)).getWaitTime())
                .isLessThanOrEqualTo("10000");
        assertThat(result.callbacks.get(1)).isInstanceOf(ConfirmationCallback.class);
    }

    @Test
    void processShouldReturnCorrectCallbacksForDefaultPushType() throws Exception {
        // Given
        JsonValue sharedState = json(object());
        JsonValue transientState = json(object());
        context = getContext(sharedState, transientState, emptyList());

        whenNodeConfigHasDefaultValues(
                MAP_WAITING_MESSAGE,
                MAP_CHALLENGE_MESSAGE,
                MAP_EXIT_MESSAGE,
                SECONDS_TO_WAIT);

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.callbacks.size()).isEqualTo(2);
        assertThat(result.callbacks.get(0)).isInstanceOf(PollingWaitCallback.class);
        assertThat(result.callbacks.get(1)).isInstanceOf(ConfirmationCallback.class);
    }

    @Test
    void processShouldReturnCorrectCallbacksForChallengePushType() throws Exception {
        // Given
        JsonValue sharedState = json(object(field(PUSH_NUMBER_CHALLENGE_KEY, NUMBER_CHALLENGE)));
        JsonValue transientState = json(object());
        context = getContext(sharedState, transientState, emptyList());

        whenNodeConfigHasDefaultValues();

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.callbacks.size()).isEqualTo(3);
        assertThat(result.callbacks.get(0)).isInstanceOf(PollingWaitCallback.class);
        assertThat(result.callbacks.get(1)).isInstanceOf(HiddenValueCallback.class);
        assertThat(result.callbacks.get(2)).isInstanceOf(ConfirmationCallback.class);
        assertThat(((HiddenValueCallback) result.callbacks.get(1)).getId()).isEqualTo(PUSH_CHALLENGE_CALLBACK_ID);
        assertThat(((HiddenValueCallback) result.callbacks.get(1)).getValue()).isEqualTo(NUMBER_CHALLENGE);
    }

    @Test
    void shouldDisplayCorrectDefaultWaitAndExitMessages() throws Exception {
        // Given
        JsonValue sharedState = json(object());
        JsonValue transientState = json(object());
        context = getContext(sharedState, transientState, emptyList());

        whenNodeConfigHasDefaultValues();

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.callbacks.size()).isEqualTo(2);
        assertThat(result.callbacks.get(0)).isInstanceOf(PollingWaitCallback.class);
        assertThat(((PollingWaitCallback) result.callbacks.get(0)).getMessage())
                .isEqualTo(DEFAULT_WAITING_MESSAGE);
        assertThat(result.callbacks.get(1)).isInstanceOf(ConfirmationCallback.class);
        assertThat(((ConfirmationCallback) result.callbacks.get(1)).getOptions()[0])
                .isEqualTo(DEFAULT_EXIT_MESSAGE);
    }

    @Test
    void shouldDisplayCorrectCustomWaitMessage() throws Exception {
        // Given
        JsonValue sharedState = json(object());
        JsonValue transientState = json(object());
        context = getContext(sharedState, transientState, emptyList());
        String message = "CUSTOM WAIT MESSAGE";
        Map<Locale, String> customWaitMessage = new HashMap<>() {{
            put(Locale.CANADA, message);
        }};

        whenNodeConfigHasDefaultValues(
                customWaitMessage,
                MAP_CHALLENGE_MESSAGE,
                MAP_EXIT_MESSAGE,
                SECONDS_TO_WAIT);

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.callbacks.size()).isEqualTo(2);
        assertThat(result.callbacks.get(0)).isInstanceOf(PollingWaitCallback.class);
        assertThat(((PollingWaitCallback) result.callbacks.get(0)).getMessage()).isEqualTo(message);
        assertThat(result.callbacks.get(1)).isInstanceOf(ConfirmationCallback.class);
    }

    @Test
    void shouldDisplayCorrectDefaultChallengeMessage() throws Exception {
        // Given
        JsonValue sharedState = json(object(field(PUSH_NUMBER_CHALLENGE_KEY, NUMBER_CHALLENGE)));
        JsonValue transientState = json(object());
        context = getContext(sharedState, transientState, emptyList());

        whenNodeConfigHasDefaultValues();

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.callbacks.size()).isEqualTo(3);
        assertThat(result.callbacks.get(0)).isInstanceOf(PollingWaitCallback.class);
        assertThat(((PollingWaitCallback) result.callbacks.get(0)).getMessage())
                .isEqualTo(DEFAULT_CHALLENGE_MESSAGE);
        assertThat(result.callbacks.get(1)).isInstanceOf(HiddenValueCallback.class);
        assertThat(result.callbacks.get(2)).isInstanceOf(ConfirmationCallback.class);
    }

    @Test
    void shouldDisplayCorrectCustomChallengeMessage() throws Exception {
        // Given
        JsonValue sharedState = json(object(field(PUSH_NUMBER_CHALLENGE_KEY, NUMBER_CHALLENGE)));
        JsonValue transientState = json(object());
        context = getContext(sharedState, transientState, emptyList());
        String message = "CUSTOM CHALLENGE MESSAGE";
        Map<Locale, String> customChallengeMessage = new HashMap<>() {{
            put(Locale.ITALY, "ITALIAN-MESSAGE");
            put(Locale.GERMANY, "GERMANY-MESSAGE");
            put(Locale.CANADA, message);
        }};

        whenNodeConfigHasDefaultValues(
                MAP_WAITING_MESSAGE,
                customChallengeMessage,
                MAP_EXIT_MESSAGE,
                SECONDS_TO_WAIT);

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.callbacks.size()).isEqualTo(3);
        assertThat(result.callbacks.get(0)).isInstanceOf(PollingWaitCallback.class);
        assertThat(((PollingWaitCallback) result.callbacks.get(0)).getMessage()).isEqualTo(message);
        assertThat(result.callbacks.get(1)).isInstanceOf(HiddenValueCallback.class);
        assertThat(result.callbacks.get(2)).isInstanceOf(ConfirmationCallback.class);
    }

    @Test
    void shouldDisplayCorrectExitMessage() throws Exception {
        // Given
        JsonValue sharedState = json(object(field(PUSH_NUMBER_CHALLENGE_KEY, NUMBER_CHALLENGE)));
        JsonValue transientState = json(object());
        context = getContext(sharedState, transientState, emptyList());
        String message = "CUSTOM EXIT MESSAGE";
        Map<Locale, String> customExitMessage = new HashMap<>() {{
            put(Locale.ITALY, "ITALIAN-MESSAGE");
            put(Locale.GERMANY, "GERMAN-MESSAGE");
            put(Locale.CANADA, message);
        }};

        whenNodeConfigHasDefaultValues(
                MAP_WAITING_MESSAGE,
                MAP_CHALLENGE_MESSAGE,
                customExitMessage,
                SECONDS_TO_WAIT);

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.callbacks.size()).isEqualTo(3);
        assertThat(result.callbacks.get(0)).isInstanceOf(PollingWaitCallback.class);
        assertThat(result.callbacks.get(1)).isInstanceOf(HiddenValueCallback.class);
        assertThat(result.callbacks.get(2)).isInstanceOf(ConfirmationCallback.class);
        assertThat(((ConfirmationCallback) result.callbacks.get(2)).getOptions()[0]).isEqualTo(message);
    }

    private TreeContext getContext() {
        return new TreeContext(JsonValue.json(object()),
                new ExternalRequestContext.Builder().build(),
                emptyList(),
                Optional.empty()
        );
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState,
            List<? extends Callback> callbacks) {
        return new TreeContext(
                sharedState,
                transientState,
                new ExternalRequestContext.Builder().build(),
                callbacks,
                Optional.empty()
        );
    }

    private void whenNodeConfigHasDefaultValues() {
        whenNodeConfigHasDefaultValues(
                MAP_WAITING_MESSAGE,
                MAP_CHALLENGE_MESSAGE,
                MAP_EXIT_MESSAGE,
                SECONDS_TO_WAIT
        );
    }

    private void whenNodeConfigHasDefaultValues(
            Map<Locale, String> waitingMessage,
            Map<Locale, String> challengeMessage,
            Map<Locale, String> exitMessage,
            int secondsToWait) {

        config = mock(PushWaitNode.Config.class);
        given(config.secondsToWait()).willReturn(secondsToWait);
        given(config.waitingMessage()).willReturn(waitingMessage);
        given(config.challengeMessage()).willReturn(challengeMessage);
        given(config.exitMessage()).willReturn(exitMessage);

        localizationHelper = mock(LocalizedMessageProvider.class);
        given(localizationHelper.getLocalizedMessage(any(), any(), any(), eq(DEFAULT_WAITING_MESSAGE_KEY)))
                .willReturn(waitingMessage.get(DEFAULT_LOCALE));
        given(localizationHelper.getLocalizedMessage(any(), any(), any(), eq(DEFAULT_CHALLENGE_MESSAGE_KEY)))
                .willReturn(challengeMessage.get(DEFAULT_LOCALE));
        given(localizationHelper.getLocalizedMessage(any(), any(), any(), eq(DEFAULT_EXIT_MESSAGE_KEY)))
                .willReturn(exitMessage.get(DEFAULT_LOCALE));

        node = spy(
                new PushWaitNode(
                        config,
                        uuid, null,
                        realm -> localizationHelper
                )
        );
    }

}
