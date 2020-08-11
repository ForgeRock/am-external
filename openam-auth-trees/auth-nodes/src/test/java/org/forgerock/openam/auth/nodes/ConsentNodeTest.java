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
 * Copyright 2019-2020 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.OBJECT_ATTRIBUTES;
import static org.forgerock.openam.auth.nodes.utils.IdmIntegrationNodeUtils.OBJECT_MAPPER;
import static org.forgerock.openam.integration.idm.IdmIntegrationConfig.CONSENT;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.authentication.callbacks.ConsentMappingCallback;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;

import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ConsentNodeTest {

    @Mock
    ConsentNode.Config config;

    @Mock
    Realm realm;

    @Mock
    IdmIntegrationService idmIntegrationService;

    @Mock
    LocaleSelector localeSelector;

    ConsentNode node;
    JsonValue consentMappings;

    @BeforeMethod
    private void init() throws Exception {
        initMocks(this);

        // Given
        when(config.message()).thenReturn(singletonMap(Locale.ENGLISH, "Privacy & Consent wording"));

        consentMappings = getJsonPayload(CONSENT);
        when(idmIntegrationService.getConsentMappings(any(), any(), any())).thenReturn(consentMappings);
        when(idmIntegrationService.storeAttributeInState(any(), any(), any())).thenCallRealMethod();

        when(realm.asPath()).thenReturn("/");
        when(localeSelector.getBestLocale(any(), any())).thenReturn(Locale.ENGLISH);

        node = new ConsentNode(config, realm, idmIntegrationService, localeSelector);
    }

    @Test
    public void allCallbacksAbsentShouldReturnCallbacks() throws Exception {
        // Given
        JsonValue sharedState = json(object(field("initial", "initial")));

        // When
        Action action = node.process(getContext(emptyList(), sharedState));

        // Then
        assertCallbacksAreReturned(action);
    }

    @Test
    public void allCallbacksAbsentShouldAdvanceIfNoConsentMappingsExist() throws Exception {
        // Given
        JsonValue sharedState = json(object(field("initial", "initial")));
        when(idmIntegrationService.getConsentMappings(any(), any(), any())).thenReturn(json(array()));

        // When
        Action action = node.process(getContext(emptyList(), sharedState));

        // Then
        assertThat(action.outcome).isEqualTo("outcome");
        assertThat(action.callbacks).isEmpty();
    }

    @Test
    public void oneCallbackAbsentShouldReturnCallbacks() throws Exception {
        // Given
        JsonValue sharedState = json(object(field("initial", "initial")));
        List<Callback> callbacks = consentMappings.stream()
                .map(mapping -> buildCallbackFromMapping(mapping).setValue(true))
                .collect(toList());
        callbacks.remove(callbacks.size() - 1);

        // When
        Action action = node.process(getContext(callbacks, sharedState));

        // Then
        assertCallbacksAreReturned(action);
    }

    @Test
    public void consentNotGivenWhenConsentIsRequiredShouldReturnCallbacks() throws Exception {
        // Given
        JsonValue sharedState = json(object(field("initial", "initial")));
        when(config.allRequired()).thenReturn(true);
        List<Callback> callbacks = consentMappings.stream()
                .map(mapping -> buildCallbackFromMapping(mapping).setValue(false))
                .collect(toList());

        // When
        Action action = node.process(getContext(callbacks, sharedState));

        // Then
        assertCallbacksAreReturned(action);
    }

    private void assertCallbacksAreReturned(Action action) {
        assertThat(action.outcome).isEqualTo(null);
        assertThat(action.callbacks).hasSize(consentMappings.size());
        action.callbacks.forEach(callback -> {
           assertThat(callback).isInstanceOf(ConsentMappingCallback.class);
           assertThat(consentMappings.stream()
                   .anyMatch(mapping -> ((ConsentMappingCallback) callback).getName()
                           .equals(mapping.get("name").asString())))
                   .isTrue();
           assertThat(((ConsentMappingCallback) callback).getMessage()).isEqualTo("Privacy & Consent wording");
        });
        assertThat((Object) action.sharedState).isNull();
    }

    @Test
    public void shouldDefaultToPropertiesIfNoTranslationFound() throws Exception {
        // Given
        JsonValue sharedState = json(object(field("initial", "initial")));

        // When
        when(config.message()).thenReturn(emptyMap());
        Action action = node.process(getContext(emptyList(), sharedState));

        action.callbacks.forEach(callback -> {
            assertThat(callback).isInstanceOf(ConsentMappingCallback.class);
            assertThat(((ConsentMappingCallback) callback).getMessage()).isEqualTo("Privacy & Consent.");
        });
    }

    @Test
    public void allCallbacksPresentAddsToSharedState() throws Exception {
        // Given
        JsonValue sharedState = json(object(field("initial", "initial")));
        List<Callback> callbacks = consentMappings.stream()
                .map(mapping -> buildCallbackFromMapping(mapping).setValue(true))
                .collect(toList());

        // When
        Action action = node.process(getContext(callbacks, sharedState));

        // Then
        assertThat(action.sharedState.isDefined(OBJECT_ATTRIBUTES)).isTrue();
        assertThat(action.sharedState.get(OBJECT_ATTRIBUTES).asMap()).hasSize(1);
        JsonValue mapping = action.sharedState.get(OBJECT_ATTRIBUTES).get("consentedMappings").get(0);
        assertThat(mapping.get("consentDate").asString()).isNotEmpty();
        assertThat(mapping.get("mapping").asString()).isEqualTo("first");
    }

    @Test
    public void consentNotGivenDoesNotAddConsentMappingToSharedState() throws Exception {
        // Given
        JsonValue sharedState = json(object(field("initial", "initial")));
        List<Callback> callbacks = consentMappings.stream()
                .map(mapping -> buildCallbackFromMapping(mapping).setValue(false))
                .collect(toList());

        // When
        Action action = node.process(getContext(callbacks, sharedState));

        // Then
        assertThat(action.outcome).isEqualTo("outcome");
        assertThat(action.sharedState.isDefined(OBJECT_ATTRIBUTES)).isFalse();
    }

    private ConsentMappingCallback buildCallbackFromMapping(JsonValue mapping) {
        return new ConsentMappingCallback(mapping.get("name").asString(), mapping.get("displayName").asString(),
                mapping.get("icon").asString(), mapping.get("accessLevel").asString(),
                mapping.get("titles").stream().map(JsonValue::asString).collect(toList()),
                "Privacy & Consent wording", config.allRequired());
    }

    private TreeContext getContext(List<? extends Callback> callbacks, JsonValue sharedState) {
        return new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, sharedState,
                new ExternalRequestContext.Builder().build(), callbacks);
    }

    private JsonValue getJsonPayload(String path) throws IOException {
        switch (path) {
            case CONSENT:
                return new JsonValue(OBJECT_MAPPER.readValue(
                        ConsentNodeTest.class.getResource("/ConsentNode/idmMappings.json"), List.class));
            default:
                return json(object());
        }
    }
}
