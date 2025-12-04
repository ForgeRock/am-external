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
 * Copyright 2018-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.TextOutputCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.LocalizedMessageProvider;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test the message node.
 */
@ExtendWith(MockitoExtension.class)
public class MessageNodeTest {

    private static final int DEFAULT_CHOICE_INDEX = 1;

    private static final Map<Locale, String> CUSTOM_YES = new HashMap<>() {{
        put(Locale.US, "CustomDefaultUSYes");
        put(Locale.GERMANY, "CustomGermanyYes");
        put(Locale.CANADA, "CustomCanadaYes");
        put(Locale.CHINESE, "CustomChineseYes");
    }};
    private static final Map<Locale, String> CUSTOM_NO = new HashMap<>() {{
        put(Locale.US, "CustomDefaultUSNo");
        put(Locale.CANADA, "CustomCanadaNo");
        put(Locale.CHINESE, "CustomChineseNo");
        put(Locale.GERMANY, "CustomGermanyNo");
    }};
    private static final Map<Locale, String> CUSTOM_MESSAGE = new HashMap<>() {{
        put(Locale.US, "CustomDefaultUSMessage");
        put(Locale.CHINESE, "CustomChineseMessage");
        put(Locale.GERMANY, "CustomGermanyMessage");
        put(Locale.CANADA, "CustomCanadaMessage");
    }};

    @Mock
    private MessageNode.Config config;
    @Mock
    private Realm realm;
    @Mock
    private LocalizedMessageProvider localizedMessageProvider;

    public static Stream<Arguments> confirmations() {
        return Stream.of(Arguments.of(0, "true"), Arguments.of(1, "false"));
    }

    @Test
    void processSetResultInSharedStateWhenNoError() throws Exception {
        //GIVEN
        when(localizedMessageProvider.getLocalizedMessage(any(), any(), any(), any())).thenReturn("message");
        when(config.stateField()).thenReturn(Optional.of("stateField"));

        MessageNode messageNode = new MessageNode(config, realm, r -> localizedMessageProvider);

        //WHEN
        Action action = messageNode.process(getContext());

        //THEN
        assertThat(config.stateField()).isPresent();
        assertThat(action.sharedState.isDefined(config.stateField().get())).isTrue();
        assertThat(action.sharedState.get(config.stateField().get())).isNotEqualTo(Optional.empty());

    }

    @Test
    void processSetResultWithoutSharedStateWhenNoError() throws Exception {
        //GIVEN
        when(localizedMessageProvider.getLocalizedMessage(any(), any(), any(), any())).thenReturn("message");
        MessageNode messageNode = new MessageNode(config, realm, r -> localizedMessageProvider);

        //WHEN
        Action action = messageNode.process(getContext());

        //THEN
        assertThat(action.outcome).isEqualTo(null);
    }

    @ParameterizedTest
    @MethodSource("confirmations")
    public void shouldGetCorrectOutcomeForChoiceIndex(int index, String response) throws Exception {
        when(localizedMessageProvider.getLocalizedMessage(any(), any(), any(), any())).thenReturn("message");
        given(config.stateField()).willReturn(Optional.of("confirmationVariable"));

        TextOutputCallback textOutputCallback = new TextOutputCallback(TextOutputCallback.INFORMATION,
                CUSTOM_MESSAGE.getOrDefault(Locale.US, ""));
        ConfirmationCallback confirmationCallback = new ConfirmationCallback(ConfirmationCallback.INFORMATION,
                new String[]{CUSTOM_YES.getOrDefault(Locale.US, ""),
                        CUSTOM_NO.getOrDefault(Locale.US, "")}, DEFAULT_CHOICE_INDEX);
        confirmationCallback.setSelectedIndex(index);

        List<Callback> arrayList = new ArrayList<>();
        arrayList.add(textOutputCallback);
        arrayList.add(confirmationCallback);

        MessageNode messageNode = new MessageNode(config, realm, r -> localizedMessageProvider);

        Action action = messageNode.process(getContext(arrayList));

        assertThat(action.outcome).isEqualTo(response);
        assertThat(config.stateField()).isPresent();
        assertThat(action.sharedState.get(config.stateField().get()).asInteger()).isEqualTo(index);
    }

    @ParameterizedTest
    @MethodSource("confirmations")
    public void shouldGetCorrectOutcomeForChoiceIndexWithoutSharedState(int index, String response) throws Exception {
        when(localizedMessageProvider.getLocalizedMessage(any(), any(), any(), any())).thenReturn("message");

        TextOutputCallback textOutputCallback = new TextOutputCallback(TextOutputCallback.INFORMATION,
                CUSTOM_MESSAGE.getOrDefault(Locale.US, ""));
        ConfirmationCallback confirmationCallback = new ConfirmationCallback(ConfirmationCallback.INFORMATION,
                new String[]{CUSTOM_YES.getOrDefault(Locale.US, ""),
                        CUSTOM_NO.getOrDefault(Locale.US, "")}, DEFAULT_CHOICE_INDEX);
        confirmationCallback.setSelectedIndex(index);

        List<Callback> arrayList = new ArrayList<>();
        arrayList.add(textOutputCallback);
        arrayList.add(confirmationCallback);

        MessageNode messageNode = new MessageNode(config, realm, r -> localizedMessageProvider);

        Action action = messageNode.process(getContext(arrayList));

        assertThat(action.outcome).isEqualTo(response);
    }

    @Test
    void processNoException() throws Exception {
        //GIVEN
        when(localizedMessageProvider.getLocalizedMessage(any(), any(), any(), any())).thenReturn("message");
        MessageNode messageNode = new MessageNode(config, realm, r -> localizedMessageProvider);

        //WHEN
        messageNode.process(getContext());

        //THEN
        //no exception
    }

    @Test
    void processThrowExceptionWhenEmptyMessage() {
        when(localizedMessageProvider.getLocalizedMessage(any(), any(), any(), any())).thenReturn("message");
        given(localizedMessageProvider.getLocalizedMessage(any(), any(), any(), any()))
                .willThrow(IllegalArgumentException.class);

        //GIVEN
        MessageNode messageNode = new MessageNode(config, realm, r -> localizedMessageProvider);

        //WHEN / THEN
        assertThatThrownBy(() -> messageNode.process(getContext())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void processThrowExceptionWhenEmptyYesMessage() {
        given(localizedMessageProvider.getLocalizedMessage(any(), any(), any(), any()))
                .willThrow(IllegalArgumentException.class);

        //GIVEN
        MessageNode messageNode = new MessageNode(config, realm, r -> localizedMessageProvider);

        //WHEN
        assertThatThrownBy(() -> messageNode.process(getContext())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void processThrowExceptionWhenEmptyNoMessage() {
        //GIVEN
        MessageNode messageNode = new MessageNode(config, realm, r -> localizedMessageProvider);
        given(localizedMessageProvider.getLocalizedMessage(any(), any(), any(), any()))
                .willThrow(IllegalArgumentException.class);

        //WHEN
        assertThatThrownBy(() -> messageNode.process(getContext())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void processNoExceptionWhenEmptyStateField() throws Exception {
        //GIVEN
        when(localizedMessageProvider.getLocalizedMessage(any(), any(), any(), any())).thenReturn("message");
        MessageNode messageNode = new MessageNode(config, realm, r -> localizedMessageProvider);

        //WHEN
        messageNode.process(getContext());

        //THEN
        //no exception
    }

    @Test
    void shouldGetCorrectOutcomeIfDefaultLocaleDoesNotExist() throws NodeProcessException {
        //GIVEN
        when(localizedMessageProvider.getLocalizedMessage(any(), any(), any(), any())).thenReturn("message");
        MessageNode messageNode = new MessageNode(config, realm, r -> localizedMessageProvider);

        //WHEN
        messageNode.process(getContext());

        //THEN
        // No exception thrown.
        // Cannot consistently predict outcome of default node as it picks the first element in an unordered map
    }

    private TreeContext getContext() {
        return new TreeContext(JsonValue.json(object()),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());
    }

    private TreeContext getContext(List<? extends Callback> callbacks) {
        return getContext(callbacks, json(object()));
    }

    private TreeContext getContext(List<? extends Callback> callbacks, JsonValue sharedState) {
        return new TreeContext(sharedState, new Builder().build(), callbacks, Optional.empty());
    }
}
