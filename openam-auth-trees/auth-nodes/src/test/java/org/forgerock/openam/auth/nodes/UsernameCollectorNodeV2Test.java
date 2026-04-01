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
 * Copyright 2025-2026 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.util.i18n.PreferredLocales;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;


public class UsernameCollectorNodeV2Test {

    private static Stream<Arguments> autocompleteValuesProvider() {
        return Stream.of(
                Arguments.of(List.of()),
                Arguments.of(List.of("user1", "user2", "user3"))
        );
    }

    @ParameterizedTest
    @MethodSource("autocompleteValuesProvider")
    void testProcessWithNoCallbacksReturnsCallback(List<String> autocompleteValues) {
        // Given
        var config = mock(UsernameCollectorNodeV2.Config.class);
        given(config.prepopulate()).willReturn(false);
        given(config.autocompleteValues()).willReturn(autocompleteValues);
        UsernameCollectorNodeV2 node = new UsernameCollectorNodeV2(config);
        JsonValue sharedState = json(object(field("initial", "initial")));
        PreferredLocales preferredLocales = mock(PreferredLocales.class);
        ResourceBundle resourceBundle = mock(ResourceBundle.class);
        given(resourceBundle.getString("callback.username")).willReturn("Username");
        given(preferredLocales.getBundleInPreferredLocale(any(), any())).willReturn(resourceBundle);

        // When
        Action result = node.process(getContext(sharedState, preferredLocales, List.of()));

        // Then
        assertThat(result.outcome).isEqualTo(null);
        assertThat(result.callbacks).hasSize(1);
        assertThat(result.callbacks.get(0)).isInstanceOf(AutoCompleteNameCallback.class);
        AutoCompleteNameCallback callback = (AutoCompleteNameCallback) result.callbacks.get(0);
        assertThat(callback.getPrompt()).isEqualTo("Username");
        assertThat(callback.getAutocompleteValues()).containsExactlyElementsOf(autocompleteValues);
        assertThat(sharedState.asMap()).containsExactly(entry("initial", "initial"));
    }

    @Test
    void shouldAddUsernameToSharedStateWhenCallbackProvided() {
        // Given
        var config = mock(UsernameCollectorNodeV2.Config.class);
        given(config.prepopulate()).willReturn(false);
        UsernameCollectorNodeV2 node = new UsernameCollectorNodeV2(config);
        JsonValue sharedState = json(object(field("initial", "initial")));
        NameCallback callback = new NameCallback("prompt");
        callback.setName("bob");

        // When
        Action result = node.process(getContext(sharedState, new PreferredLocales(), singletonList(callback)));

        // Then
        assertThat(result.outcome).isEqualTo("outcome");
        assertThat(result.callbacks).isEmpty();
        assertThat(sharedState).isObject().contains(USERNAME, "bob").contains("initial", "initial");
    }

    @Test
    void shouldNotAddUsernameToCallbacksWhenPrepopulateIsFalseAndUsernameIsInSharedState() {
        // Given
        var config = mock(UsernameCollectorNodeV2.Config.class);
        given(config.prepopulate()).willReturn(false);
        UsernameCollectorNodeV2 node = new UsernameCollectorNodeV2(config);
        JsonValue sharedState = json(object(field("username", "bob")));
        NameCallback callback = new NameCallback("prompt");

        // When
        Action result = node.process(getContext(sharedState, new PreferredLocales(), singletonList(callback)));

        // Then
        assertThat(result.outcome).isNull();
        assertThat(result.callbacks).hasSize(1);
        assertThat(result.callbacks.get(0)).isInstanceOf(NameCallback.class);
        assertThat(((NameCallback) result.callbacks.get(0)).getName()).isEqualTo(null);
    }

    @Test
    void shouldAddUsernameToCallbacksWhenPrepopulateIsTrueAndUsernameIsInSharedState() {
        // Given
        var config = mock(UsernameCollectorNodeV2.Config.class);
        given(config.prepopulate()).willReturn(true);
        UsernameCollectorNodeV2 node = new UsernameCollectorNodeV2(config);
        JsonValue sharedState = json(object(field("username", "bob")));
        NameCallback callback = new NameCallback("prompt");

        // When
        Action result = node.process(getContext(sharedState, new PreferredLocales(), singletonList(callback)));

        // Then
        assertThat(result.outcome).isNull();
        assertThat(result.callbacks).hasSize(1);
        assertThat(result.callbacks.get(0)).isInstanceOf(NameCallback.class);
        assertThat(((NameCallback) result.callbacks.get(0)).getName()).isEqualTo("bob");
    }

    @Test
    void shouldNotAddUsernameToCallbacksWhenPrepopulateIsTrueAndUsernameIsNotInSharedState() {
        // Given
        var config = mock(UsernameCollectorNodeV2.Config.class);
        given(config.prepopulate()).willReturn(true);
        UsernameCollectorNodeV2 node = new UsernameCollectorNodeV2(config);
        JsonValue sharedState = json(object(field("initial", "initial")));
        NameCallback callback = new NameCallback("prompt");

        // When
        Action result = node.process(getContext(sharedState, new PreferredLocales(), singletonList(callback)));

        // Then
        assertThat(result.outcome).isNull();
        assertThat(result.callbacks).hasSize(1);
        assertThat(result.callbacks.get(0)).isInstanceOf(NameCallback.class);
        assertThat(((NameCallback) result.callbacks.get(0)).getName()).isEqualTo(null);
    }

    private TreeContext getContext(JsonValue sharedState, PreferredLocales preferredLocales,
                                   List<? extends Callback> callbacks) {
        return TreeContext.builder()
                .sharedState(sharedState)
                .requestContext(new Builder().locales(preferredLocales).build())
                .callbacks(callbacks)
                .build();
    }
}
