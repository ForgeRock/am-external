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
 * Copyright 2019-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceException.BAD_REQUEST;
import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.utils.IdmIntegrationNodeUtils.OBJECT_MAPPER;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.OBJECT_ATTRIBUTES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.authentication.callbacks.KbaCreateCallback;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.forgerock.openam.integration.idm.KbaConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class KbaCreateNodeTest {

    @Mock
    IdmIntegrationService idmIntegrationService;
    @Mock
    LocaleSelector localeSelector;
    @Mock
    private KbaCreateNode.Config config;
    @Mock
    private Realm realm;
    @InjectMocks
    private KbaCreateNode node;
    private KbaConfig kbaConfig;
    private final Map<Locale, String> messages = Map.of(Locale.ENGLISH, "A message");

    @BeforeEach
    void setUp() throws Exception {
        kbaConfig = OBJECT_MAPPER.readValue(getClass()
                .getResource("/KbaCreateNode/idmKbaConfig.json"), KbaConfig.class);
    }

    @Test
    void shouldThrowExceptionIfFailedToRetrieveKbaConfig() throws Exception {
        when(idmIntegrationService.getKbaConfig(any(), any())).thenReturn(kbaConfig);
        JsonValue sharedState = json(object(
                field(USERNAME, "test")
        ));

        when(idmIntegrationService.getKbaConfig(any(), any())).thenThrow(newResourceException(BAD_REQUEST));

        assertThatThrownBy(() -> node.process(getContext(emptyList(), sharedState)))
                .isInstanceOf(NodeProcessException.class)
                .hasMessageContaining("org.forgerock.json.resource.BadRequestException: Bad Request");
    }

    @Test
    void callbacksAbsentShouldReturnCallbacks() throws Exception {
        // Given
        when(config.message()).thenReturn(messages);
        when(idmIntegrationService.getKbaConfig(any(), any())).thenReturn(kbaConfig);
        when(localeSelector.getBestLocale(any(), any())).thenReturn(Locale.ENGLISH);
        JsonValue sharedState = json(object(field("initial", "initial")));

        // When
        Action action = node.process(getContext(emptyList(), sharedState));

        // Then
        assertThat(action.outcome).isEqualTo(null);
        assertThat(action.callbacks).hasSize(2);
        assertThat(action.callbacks.get(0)).isInstanceOf(KbaCreateCallback.class);
        assertThat(((KbaCreateCallback) action.callbacks.get(0)).getPrompt()).isEqualTo("A message");
        assertThat(action.callbacks.get(1)).isInstanceOf(KbaCreateCallback.class);
        assertThat(((KbaCreateCallback) action.callbacks.get(1)).getPrompt()).isEqualTo("A message");
        assertThat((Object) action.sharedState).isNull();
    }

    @Test
    void shouldDefaultToPropertiesIfNoTranslationFound() throws Exception {
        // Given
        when(config.message()).thenReturn(messages);
        when(idmIntegrationService.getKbaConfig(any(), any())).thenReturn(kbaConfig);
        when(localeSelector.getBestLocale(any(), any())).thenReturn(Locale.ENGLISH);
        JsonValue sharedState = json(object(field("initial", "initial")));

        // When
        when(config.message()).thenReturn(emptyMap());
        Action action = node.process(getContext(emptyList(), sharedState));

        // Then
        assertThat(action.outcome).isEqualTo(null);
        assertThat(action.callbacks.get(0)).isInstanceOf(KbaCreateCallback.class);
        assertThat(((KbaCreateCallback) action.callbacks.get(0)).getPrompt()).isEqualTo("Select a security question.");
    }

    @Test
    void callbacksPresentProcessesSuccessfully() throws Exception {
        // Given
        when(config.message()).thenReturn(messages);
        when(idmIntegrationService.getKbaConfig(any(), any())).thenReturn(kbaConfig);
        when(idmIntegrationService.storeAttributeInState(any(), any(), any())).thenCallRealMethod();
        when(localeSelector.getBestLocale(any(), any())).thenReturn(Locale.ENGLISH);
        JsonValue sharedState = json(object(field("initial", "initial")));
        List<Callback> callbacks = new ArrayList<>();
        callbacks.add(new KbaCreateCallback("first", singletonList("Question One"), true)
                .setSelectedQuestion("Question One").setSelectedAnswer("uno"));
        callbacks.add(new KbaCreateCallback("second", singletonList("Question Two"), true)
                .setSelectedQuestion("Custom Question").setSelectedAnswer("dos"));

        // When
        Action action = node.process(getContext(callbacks, sharedState));

        // Then
        assertThat(action.transientState.isDefined(OBJECT_ATTRIBUTES)).isTrue();
        JsonValue attributes = action.transientState.get(OBJECT_ATTRIBUTES);
        assertThat(attributes.asMap()).hasSize(1);
        assertThat(attributes.isDefined("kbaInfo")).isTrue();
        assertThat(attributes.get("kbaInfo")).hasSize(2);
    }

    @Test
    void callbacksPresentProcessesSuccessfullyWithNonLatinQuestions() throws Exception {
        // Given
        when(config.message()).thenReturn(messages);
        when(idmIntegrationService.getKbaConfig(any(), any())).thenReturn(kbaConfig);
        when(idmIntegrationService.storeAttributeInState(any(), any(), any())).thenCallRealMethod();
        when(localeSelector.getBestLocale(any(), any())).thenReturn(Locale.ENGLISH);
        JsonValue sharedState = json(object(field("initial", "initial")));
        List<Callback> callbacks = new ArrayList<>();
        callbacks.add(new KbaCreateCallback("first", singletonList("Question One"), true)
                .setSelectedQuestion("質問?").setSelectedAnswer("uno"));
        callbacks.add(new KbaCreateCallback("second", singletonList("Question Two"), true)
                .setSelectedQuestion("あなたの好きな色は何ですか?").setSelectedAnswer("dos"));

        // When
        Action action = node.process(getContext(callbacks, sharedState));

        // Then
        assertThat(action.transientState).isNotNull();
        assertThat(action.callbacks).isEmpty();
        assertThat(action.transientState.isDefined(OBJECT_ATTRIBUTES)).isTrue();
        JsonValue attributes = action.transientState.get(OBJECT_ATTRIBUTES);
        assertThat(attributes.asMap()).hasSize(1);
        assertThat(attributes.isDefined("kbaInfo")).isTrue();
        assertThat(attributes.get("kbaInfo")).hasSize(2);
    }

    @Test
    void duplicateQuestionsReturnOriginalCallbacks() throws Exception {
        // Given
        when(config.message()).thenReturn(messages);
        when(idmIntegrationService.getKbaConfig(any(), any())).thenReturn(kbaConfig);
        when(localeSelector.getBestLocale(any(), any())).thenReturn(Locale.ENGLISH);
        JsonValue sharedState = json(object(field("initial", "initial")));
        List<Callback> callbacks = new ArrayList<>();
        callbacks.add(new KbaCreateCallback("first", singletonList("Question One"), true)
                .setSelectedQuestion("あ!なたの好  きな色は何&ですか??").setSelectedAnswer("uno"));
        callbacks.add(new KbaCreateCallback("second", singletonList("Question Two"), true)
                .setSelectedQuestion("あなたの好きな色は何ですか?").setSelectedAnswer("dos"));

        // When
        Action action = node.process(getContext(callbacks, sharedState));

        // Then
        assertThat(action.transientState).isNull();
        assertThat(action.callbacks).isNotEmpty();
    }

    @Test
    void invalidCallbacksReturnOriginalCallbacks() throws Exception {
        // Given
        when(config.message()).thenReturn(messages);
        when(idmIntegrationService.getKbaConfig(any(), any())).thenReturn(kbaConfig);
        when(localeSelector.getBestLocale(any(), any())).thenReturn(Locale.ENGLISH);
        JsonValue sharedState = json(object(field("initial", "initial")));
        List<Callback> callbacks = new ArrayList<>();
        callbacks.add(new KbaCreateCallback("first", singletonList("Question One"), true)
                .setSelectedQuestion(null).setSelectedAnswer(null));

        // When
        Action action = node.process(getContext(callbacks, sharedState));

        // Then
        assertThat(action.transientState).isNull();
        assertThat(action.callbacks).isNotEmpty();
    }

    @Test
    void shouldFetchKbaQuestionsGivenUndefinedBestLocale() {
        // Given
        JsonValue sharedState = json(object(field("initial", "initial")));
        when(localeSelector.getBestLocale(any(), any())).thenReturn(null);
        when(localeSelector.getBestLocale(any(), any())).thenReturn(Locale.ENGLISH);

        // When
        Map<String, String> questions = node.fetchKbaQuestions(getContext(emptyList(), sharedState), kbaConfig);

        // Then
        // defaults to the first map entry question ie. "en"
        assertThat(questions).containsEntry("1", "Question One?");
    }

    private TreeContext getContext(List<? extends Callback> callbacks, JsonValue sharedState) {
        return new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, sharedState,
                new ExternalRequestContext.Builder().build(), callbacks);
    }
}
