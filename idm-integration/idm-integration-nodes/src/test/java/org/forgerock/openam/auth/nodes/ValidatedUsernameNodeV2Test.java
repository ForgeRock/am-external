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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.OBJECT_ATTRIBUTES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Stream;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.authentication.callbacks.ValidatedUsernameCallback;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.forgerock.util.i18n.PreferredLocales;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@ExtendWith(MockitoExtension.class)
public class ValidatedUsernameNodeV2Test {

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER =
                new ObjectMapper().configure(JsonParser.Feature.ALLOW_COMMENTS, true)
                        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                        .enable(SerializationFeature.INDENT_OUTPUT)
                        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
    }

    @Mock
    private Realm realm;

    @Mock
    private IdmIntegrationService idmIntegrationService;

    @Mock
    private PreferredLocales preferredLocales;

    private static Stream<Arguments> autocompleteValuesProvider() {
        return Stream.of(
                Arguments.of(List.of()),
                Arguments.of(List.of("a", "b"))
        );
    }

    @ParameterizedTest
    @MethodSource("autocompleteValuesProvider")
    void testProcessReturnsAutoCompleteNameCallback(List<String> autocompleteValues) throws Exception {
        // Given
        ValidatedUsernameNodeV2 node = createNode(false, false, autocompleteValues);
        JsonValue sharedState = json(object());
        TreeContext context = createTreeContext(emptyList(), sharedState);
        ResourceBundle resourceBundle = mock(ResourceBundle.class);
        given(resourceBundle.getString("callback.username")).willReturn("Enter your username");
        given(preferredLocales.getBundleInPreferredLocale(any(), any())).willReturn(resourceBundle);

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.callbacks).hasSize(1);
        Callback callback = result.callbacks.get(0);
        assertThat(callback).isInstanceOf(AutoCompleteNameCallback.class);

        AutoCompleteNameCallback nameCallback = (AutoCompleteNameCallback) callback;
        assertThat(nameCallback.getPrompt()).isEqualTo("Enter your username");
        assertThat(nameCallback.getAutocompleteValues()).containsExactlyElementsOf(autocompleteValues);
        assertThat(sharedState.get("username").isNull()).isTrue()
                .as("Username should not be in shared state");
    }

    @ParameterizedTest
    @MethodSource("autocompleteValuesProvider")
    void testProcessReturnsValidatedUsernameCallback(List<String> autocompleteValues) throws Exception {
        // Given
        ValidatedUsernameNodeV2 node = createNode(true, false, autocompleteValues);
        JsonValue sharedState = json(object());
        TreeContext context = createTreeContext(emptyList(), sharedState);
        setupValidationMocks();

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.callbacks).hasSize(1);
        Callback callback = result.callbacks.get(0);
        assertThat(callback).isInstanceOf(ValidatedUsernameCallback.class);

        ValidatedUsernameCallback validatedCallback = (ValidatedUsernameCallback) callback;
        assertThat(validatedCallback.getPrompt()).isEqualTo("Username");
        assertThat(validatedCallback.getAutocompleteValues()).containsExactlyElementsOf(autocompleteValues);
        assertThat(sharedState.get("username").isNull()).isTrue()
                .as("Username should not be in shared state");
    }

    @Test
    void shouldStoreUsernameInStateWhenValidationDisabledAndCallbackProvided() throws Exception {
        // Given
        ValidatedUsernameNodeV2 node = createNode(false, false);
        NameCallback nameCallback = new NameCallback("Username");
        nameCallback.setName("myuser");
        JsonValue sharedState = json(object());
        TreeContext context = createTreeContext(singletonList(nameCallback), sharedState);
        ResourceBundle resourceBundle = mock(ResourceBundle.class);
        given(resourceBundle.getString("callback.username")).willReturn("Enter your username");
        given(preferredLocales.getBundleInPreferredLocale(any(), any())).willReturn(resourceBundle);
        willCallRealMethod().given(idmIntegrationService).storeAttributeInSharedState(any(), any(), any());

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.callbacks).hasSize(0);
        assertThat(sharedState.isDefined("username")).isTrue();
        assertThat(sharedState.get("username").asString()).isEqualTo("myuser");
        assertThat(sharedState.isDefined(OBJECT_ATTRIBUTES)).isTrue();
        assertThat(sharedState.get(OBJECT_ATTRIBUTES).get(DEFAULT_IDM_IDENTITY_ATTRIBUTE).asString())
                .isEqualTo("myuser");
    }

    @Test
    void shouldNotStoreEmptyUsernameInState() throws Exception {
        // Given
        ValidatedUsernameNodeV2 node = createNode(false, false);
        NameCallback nameCallback = new NameCallback("Username");
        nameCallback.setName("");
        JsonValue sharedState = json(object());
        TreeContext context = createTreeContext(singletonList(nameCallback), sharedState);
        ResourceBundle resourceBundle = mock(ResourceBundle.class);
        given(resourceBundle.getString("callback.username")).willReturn("Enter your username");
        given(preferredLocales.getBundleInPreferredLocale(any(), any())).willReturn(resourceBundle);
        given(preferredLocales.getBundleInPreferredLocale(any(), any())).willReturn(resourceBundle);

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.callbacks).hasSize(0);
        var nodeState = context.getStateFor(node);
        assertThat(nodeState.isDefined("username")).isFalse();
    }

    @Test
    void shouldPrepopulateUsernameWhenValidationEnabledAndUsernameInSharedState() throws Exception {
        // Given
        ValidatedUsernameNodeV2 node = createNode(true, true);
        TreeContext context = createTreeContext(List.of(), json(object(
                field("username", "existingUser")
        )));
        setupValidationMocks();

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.callbacks).hasSize(1);
        assertThat(result.callbacks.get(0)).isInstanceOf(ValidatedUsernameCallback.class);
        ValidatedUsernameCallback callback = (ValidatedUsernameCallback) result.callbacks.get(0);
        assertThat(callback.getUsername()).isEqualTo("existingUser");
    }

    @Test
    void shouldPrepopulateUsernameWhenValidationNotEnabledAndUsernameInSharedState() throws Exception {
        // Given
        ValidatedUsernameNodeV2 node = createNode(false, true);
        TreeContext context = createTreeContext(List.of(), json(object(
                field("username", "existingUser")
        )));
        ResourceBundle resourceBundle = mock(ResourceBundle.class);
        given(resourceBundle.getString("callback.username")).willReturn("Enter your username");
        given(preferredLocales.getBundleInPreferredLocale(any(), any())).willReturn(resourceBundle);
        given(preferredLocales.getBundleInPreferredLocale(any(), any())).willReturn(resourceBundle);

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.callbacks).hasSize(1);
        assertThat(result.callbacks.get(0)).isInstanceOf(NameCallback.class);
        NameCallback callback = (NameCallback) result.callbacks.get(0);
        assertThat(callback.getName()).isEqualTo("existingUser");
        assertThat(callback.getPrompt()).isEqualTo("Enter your username");
    }

    @Test
    void shouldNotPrepopulateWhenValidationEnabledButNoUsernameInSharedState() throws Exception {
        // Given
        ValidatedUsernameNodeV2 node = createNode(true, true);
        TreeContext context = createTreeContext(List.of(), json(object()));
        setupValidationMocks();

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.callbacks).hasSize(1);
        assertThat(result.callbacks.get(0)).isInstanceOf(ValidatedUsernameCallback.class);
        ValidatedUsernameCallback callback = (ValidatedUsernameCallback) result.callbacks.get(0);
        assertThat(callback.getUsername()).isNull();
    }

    @Test
    void shouldReturnCallbackWithPolicyFailuresWhenValidationFails() throws Exception {
        // Given
        ValidatedUsernameNodeV2 node = createNode(true, false);
        ValidatedUsernameCallback validatedCallback = new ValidatedUsernameCallback("Username",
                json(array()), false, List.of());
        validatedCallback.setUsername("badusername");
        JsonValue sharedState = json(object());
        TreeContext context = createTreeContext(singletonList(validatedCallback), sharedState);
        setupValidationMocksWithFailure();

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.callbacks).hasSize(1);
        assertThat(result.callbacks.get(0)).isInstanceOf(ValidatedUsernameCallback.class);
        ValidatedUsernameCallback callback = (ValidatedUsernameCallback) result.callbacks.get(0);
        assertThat(callback.getUsername()).isEqualTo("badusername");
        assertThat(sharedState.isDefined("username")).isFalse().as("Username should not be in shared state");
    }

    @Test
    void shouldStoreUsernameWhenValidationPasses() throws Exception {
        // Given
        ValidatedUsernameNodeV2 node = createNode(true, false);
        ValidatedUsernameCallback validatedCallback = new ValidatedUsernameCallback("Username",
                json(array()), false, List.of());
        validatedCallback.setUsername("goodusername");
        JsonValue sharedState = json(object());
        TreeContext context = createTreeContext(singletonList(validatedCallback), sharedState);
        setupValidationMocksWithSuccess();

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.callbacks).hasSize(0);
        assertThat(sharedState.isDefined("username")).isTrue();
        assertThat(sharedState.get("username").asString()).isEqualTo("goodusername");
    }

    @Test
    void shouldReturnCallbackWhenValidateOnlyIsTrue() throws Exception {
        // Given
        ValidatedUsernameNodeV2 node = createNode(true, false);
        ValidatedUsernameCallback validatedCallback =
                new ValidatedUsernameCallback("Username", json(array()), true, List.of());
        validatedCallback.setUsername("goodusername");
        JsonValue sharedState = json(object());
        TreeContext context = createTreeContext(singletonList(validatedCallback), sharedState);
        setupValidationMocksWithSuccess();

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.callbacks).hasSize(1);
        assertThat(result.callbacks.get(0)).isInstanceOf(ValidatedUsernameCallback.class);
        ValidatedUsernameCallback callback = (ValidatedUsernameCallback) result.callbacks.get(0);
        assertThat(callback.validateOnly()).isTrue();
    }

    // Helper methods
    private ValidatedUsernameNodeV2 createNode(boolean validateInput, boolean prepopulate) {
        return createNode(validateInput, prepopulate, List.of());
    }

    private ValidatedUsernameNodeV2 createNode(boolean validateInput, boolean prepopulate,
            List<String> autocompleteValues) {
        var config = new TestConfig(validateInput,
                IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE, prepopulate,
                autocompleteValues);
        return new ValidatedUsernameNodeV2(config, realm, idmIntegrationService);
    }

    private TreeContext createTreeContext(List<? extends Callback> callbacks, JsonValue sharedState) {
        return TreeContext.builder()
                .sharedState(sharedState)
                .requestContext(new ExternalRequestContext.Builder().locales(preferredLocales).build())
                .callbacks(callbacks)
                .treeStateContainers(Set.of(OBJECT_ATTRIBUTES))
                .build();
    }

    private void setupValidationMocks() throws Exception {
        when(idmIntegrationService.getSchema(any(), any(), any())).thenReturn(new JsonValue(OBJECT_MAPPER.readValue(
                ValidatedUsernameNodeV2Test.class.getResource("/ValidatedUsernameNode/idmSchema.json"), Map.class)));
        when(idmIntegrationService.getValidationRequirements(any(), any(), any()))
                .thenReturn(new JsonValue(OBJECT_MAPPER.readValue(ValidatedUsernameNodeV2Test.class.getResource(
                        "/ValidatedUsernameNode/idmPolicyRead.json"), Map.class)));
    }

    private void setupValidationMocksWithFailure() throws Exception {
        setupValidationMocks();
        when(idmIntegrationService.validateInput(any(), any(), any(), any(), any(), any()))
                .thenReturn(json(object(
                        field("result", false),
                        field("failedPolicyRequirements", array(
                                object(
                                        field("policyRequirements", array(
                                                object(
                                                        field("params", object(
                                                                field("minLength", 8)
                                                        )),
                                                        field("policyRequirement", "MIN_LENGTH")
                                                )
                                        )),
                                        field("property", DEFAULT_IDM_IDENTITY_ATTRIBUTE)
                                )
                        ))
                )));
    }

    private void setupValidationMocksWithSuccess() throws Exception {
        setupValidationMocks();
        when(idmIntegrationService.validateInput(any(), any(), any(), any(), any(), any()))
                .thenReturn(json(object(field("result", true)
        )));
    }

    class TestConfig implements ValidatedUsernameNodeV2.Config {
        private final boolean validateInput;
        private final String usernameAttribute;
        private final boolean prepopulate;
        private final List<String> autocompleteValues;

        TestConfig(boolean validateInput, String usernameAttribute, boolean prepopulate,
                List<String> autocompleteValues) {
            this.validateInput = validateInput;
            this.usernameAttribute = usernameAttribute;
            this.prepopulate = prepopulate;
            this.autocompleteValues = autocompleteValues;
        }

        @Override
        public Boolean validateInput() {
            return validateInput;
        }

        @Override
        public String usernameAttribute() {
            return usernameAttribute;
        }

        @Override
        public boolean prepopulate() {
            return prepopulate;
        }

        @Override
        public List<String> autocompleteValues() {
            return autocompleteValues;
        }
    }
}
