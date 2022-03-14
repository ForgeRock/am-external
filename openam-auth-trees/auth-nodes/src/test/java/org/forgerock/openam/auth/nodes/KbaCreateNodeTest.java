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
 * Copyright 2019-2022 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceException.BAD_REQUEST;
import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.OBJECT_ATTRIBUTES;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.utils.IdmIntegrationNodeUtils.OBJECT_MAPPER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.HashMap;
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
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class KbaCreateNodeTest {

    @Mock
    private KbaCreateNode.Config config;

    @Mock
    private Realm realm;

    @Mock
    IdmIntegrationService idmIntegrationService;

    @Mock
    LocaleSelector localeSelector;

    private KbaCreateNode node;
    private KbaConfig kbaConfig;

    @BeforeMethod
    public void setUp() throws Exception {
        initMocks(this);
        kbaConfig = OBJECT_MAPPER.readValue(getClass()
                .getResource("/KbaCreateNode/idmKbaConfig.json"), KbaConfig.class);

        Map<Locale, String> messages = new HashMap<>();
        messages.put(Locale.ENGLISH, "A message");
        when(config.message()).thenReturn(messages);
        when(idmIntegrationService.getKbaConfig(any(), any())).thenReturn(kbaConfig);
        when(idmIntegrationService.storeAttributeInState(any(), any(), any())).thenCallRealMethod();
        when(localeSelector.getBestLocale(any(), any())).thenReturn(Locale.ENGLISH);

        node = new KbaCreateNode(config, realm, idmIntegrationService, localeSelector);
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void shouldThrowExceptionIfFailedToRetrieveKbaConfig() throws Exception {
        JsonValue sharedState = json(object(
                field(USERNAME, "test")
        ));

        when(idmIntegrationService.getKbaConfig(any(), any())).thenThrow(newResourceException(BAD_REQUEST));

        node.process(getContext(emptyList(), sharedState));
    }

    @Test
    public void callbacksAbsentShouldReturnCallbacks() throws Exception {
        // Given
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
    public void shouldDefaultToPropertiesIfNoTranslationFound() throws Exception {
        // Given
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
    public void callbacksPresentProcessesSuccessfully() throws Exception {
        // Given
        JsonValue sharedState = json(object(field("initial", "initial")));
        List<Callback> callbacks = new ArrayList<>();
        callbacks.add(new KbaCreateCallback("first", singletonList("Question One"))
                .setSelectedQuestion("Question One").setSelectedAnswer("uno"));
        callbacks.add(new KbaCreateCallback("second", singletonList("Question Two"))
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
    public void callbacksPresentProcessesSuccessfullyWithNonLatinQuestions() throws Exception {
        // Given
        JsonValue sharedState = json(object(field("initial", "initial")));
        List<Callback> callbacks = new ArrayList<>();
        callbacks.add(new KbaCreateCallback("first", singletonList("Question One"))
                .setSelectedQuestion("質問?").setSelectedAnswer("uno"));
        callbacks.add(new KbaCreateCallback("second", singletonList("Question Two"))
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
    public void duplicateQuestionsReturnOriginalCallbacks() throws Exception {
        // Given
        JsonValue sharedState = json(object(field("initial", "initial")));
        List<Callback> callbacks = new ArrayList<>();
        callbacks.add(new KbaCreateCallback("first", singletonList("Question One"))
                .setSelectedQuestion("あ!なたの好  きな色は何&ですか??").setSelectedAnswer("uno"));
        callbacks.add(new KbaCreateCallback("second", singletonList("Question Two"))
                .setSelectedQuestion("あなたの好きな色は何ですか?").setSelectedAnswer("dos"));

        // When
        Action action = node.process(getContext(callbacks, sharedState));

        // Then
        assertThat(action.transientState).isNull();
        assertThat(action.callbacks).isNotEmpty();
    }

    @Test
    public void invalidCallbacksReturnOriginalCallbacks() throws Exception {
        // Given
        JsonValue sharedState = json(object(field("initial", "initial")));
        List<Callback> callbacks = new ArrayList<>();
        callbacks.add(new KbaCreateCallback("first", singletonList("Question One"))
                .setSelectedQuestion(null).setSelectedAnswer(null));

        // When
        Action action = node.process(getContext(callbacks, sharedState));

        // Then
        assertThat(action.transientState).isNull();
        assertThat(action.callbacks).isNotEmpty();
    }

    @Test
    public void shouldFetchKbaQuestionsGivenUndefinedBestLocale() {
        // Given
        JsonValue sharedState = json(object(field("initial", "initial")));
        when(localeSelector.getBestLocale(any(), any())).thenReturn(null);

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
