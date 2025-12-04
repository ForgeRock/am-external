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
 * Copyright 2019-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_PASSWORD_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.OBJECT_ATTRIBUTES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.PasswordCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.authentication.callbacks.ValidatedPasswordCallback;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.forgerock.util.i18n.PreferredLocales;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@ExtendWith(MockitoExtension.class)
public class ValidatedPasswordNodeTest {

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER =
                new ObjectMapper().configure(JsonParser.Feature.ALLOW_COMMENTS, true)
                        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                        .enable(SerializationFeature.INDENT_OUTPUT)
                        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
    }

    @Mock
    private ValidatedPasswordNode.Config config;

    @Mock
    private Realm realm;

    @Mock
    private IdmIntegrationService idmIntegrationService;

    @InjectMocks
    private ValidatedPasswordNode node;
    private PreferredLocales preferredLocales;

    @BeforeEach
    void before() throws Exception {
        preferredLocales = mock(PreferredLocales.class);
        when(config.validateInput()).thenReturn(false);
    }

    @Test
    void processWithoutCallbackShouldReturnCallback() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));
        JsonValue transientState = json(object());
        ResourceBundle resourceBundle = new ValidatedPasswordNodeTest.MockResourceBundle("Password");
        given(preferredLocales.getBundleInPreferredLocale(any(), any())).willReturn(resourceBundle);

        // When
        Action action = node.process(getContext(emptyList(), sharedState, transientState));

        // Then
        assertThat(action.callbacks).hasSize(1);
        assertThat(action.callbacks.get(0)).isInstanceOf(PasswordCallback.class);
        assertThat(((PasswordCallback) action.callbacks.get(0)).getPrompt()).isEqualTo("Password");
        assertThat(action.sharedState).isNull();
        assertThat(action.transientState).isNull();
    }

    @Test
    void processWithCallbackShouldStorePasswordInState() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));
        JsonValue transientState = json(object());

        // When
        ResourceBundle resourceBundle = new ValidatedPasswordNodeTest.MockResourceBundle("Password");
        given(preferredLocales.getBundleInPreferredLocale(any(), any())).willReturn(resourceBundle);
        when(config.passwordAttribute()).thenReturn(DEFAULT_IDM_PASSWORD_ATTRIBUTE);
        when(idmIntegrationService.storeAttributeInState(any(), any(), any())).thenCallRealMethod();
        PasswordCallback callback = new PasswordCallback("Enter password", true);
        callback.setPassword("mypw".toCharArray());
        Action action = node.process(getContext(singletonList(callback), sharedState, transientState));

        // Then
        assertThat(action.callbacks).isEmpty();
        assertThat(action.transientState.isDefined("password")).isTrue();
        assertThat(action.transientState.get("password").asString()).isEqualTo("mypw");
        assertThat(action.transientState.isDefined(OBJECT_ATTRIBUTES)).isTrue();
        assertThat(action.transientState.get(OBJECT_ATTRIBUTES).get(DEFAULT_IDM_PASSWORD_ATTRIBUTE).asString())
                .isEqualTo("mypw");
    }

    @Test
    void processWithStateContainingIdShouldAppendIdToIdentityResource() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(FIELD_CONTENT_ID, "id"),
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")

                ))
        ));
        JsonValue transientState = json(object());

        // When
        when(idmIntegrationService.getSchema(any(), any(), any())).thenReturn(new JsonValue(OBJECT_MAPPER.readValue(
                ValidatedPasswordNodeTest.class.getResource("/ValidatedPasswordNode/idmSchema.json"), Map.class)));
        when(idmIntegrationService.getValidationRequirements(any(), any(), any()))
                .thenReturn(new JsonValue(OBJECT_MAPPER.readValue(ValidatedPasswordNodeTest.class.getResource(
                        "/ValidatedPasswordNode/idmPolicyRead.json"), Map.class)));
        when(config.passwordAttribute()).thenReturn(DEFAULT_IDM_PASSWORD_ATTRIBUTE);
        when(config.validateInput()).thenReturn(true);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        when(idmIntegrationService.validateInput(any(), any(), captor.capture(), any(), any())).thenReturn(json(object(
                field("result", false),
                field("failedPolicyRequirements", array(
                        object(
                                field("policyRequirements", array(
                                        object(
                                                field("policyRequirement", "IS_NEW")
                                        )
                                )),
                                field("property", DEFAULT_IDM_PASSWORD_ATTRIBUTE)
                        )
                ))
        )));
        ValidatedPasswordCallback callback = new ValidatedPasswordCallback("Password", false, json(array()), false);
        callback.setPassword("mypw".toCharArray());
        node.process(getContext(singletonList(callback), sharedState, transientState));

        // Then
        assertThat(captor.getValue()).isEqualTo("managed/user/id");
    }

    @Test
    void processWithStateContainingNoIdForIdentityResource() throws Exception {
        // When
        when(idmIntegrationService.getSchema(any(), any(), any())).thenReturn(new JsonValue(OBJECT_MAPPER.readValue(
                ValidatedPasswordNodeTest.class.getResource("/ValidatedPasswordNode/idmSchema.json"), Map.class)));
        when(idmIntegrationService.getValidationRequirements(any(), any(), any()))
                .thenReturn(new JsonValue(OBJECT_MAPPER.readValue(ValidatedPasswordNodeTest.class.getResource(
                        "/ValidatedPasswordNode/idmPolicyRead.json"), Map.class)));
        when(config.passwordAttribute()).thenReturn(DEFAULT_IDM_PASSWORD_ATTRIBUTE);
        when(idmIntegrationService.storeAttributeInState(any(), any(), any())).thenCallRealMethod();
        when(config.validateInput()).thenReturn(true);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        when(idmIntegrationService.validateInput(any(), any(), captor.capture(), any(), any())).thenReturn(json(object(
                field("result", true))));
        ValidatedPasswordCallback callback = new ValidatedPasswordCallback("Password", false, json(array()), false);
        callback.setPassword("mypw".toCharArray());
        node.process(getContext(singletonList(callback), json(object()), json(object())));

        // Then
        assertThat(captor.getValue()).isEqualTo("managed/user");
    }

    @Test
    void processWithBadPasswordShouldReturnCallbackWithPolicyFailures() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));
        JsonValue transientState = json(object());

        // When
        when(config.passwordAttribute()).thenReturn(DEFAULT_IDM_PASSWORD_ATTRIBUTE);
        when(idmIntegrationService.getSchema(any(), any(), any())).thenReturn(new JsonValue(OBJECT_MAPPER.readValue(
                ValidatedPasswordNodeTest.class.getResource("/ValidatedPasswordNode/idmSchema.json"), Map.class)));
        when(idmIntegrationService.getValidationRequirements(any(), any(), any()))
                .thenReturn(new JsonValue(OBJECT_MAPPER.readValue(ValidatedPasswordNodeTest.class.getResource(
                        "/ValidatedPasswordNode/idmPolicyRead.json"), Map.class)));
        when(config.validateInput()).thenReturn(true);
        when(idmIntegrationService.validateInput(any(), any(), any(), any(), any())).thenReturn(json(object(
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
                                field("property", DEFAULT_IDM_PASSWORD_ATTRIBUTE)
                        )
                ))
        )));
        ValidatedPasswordCallback callback = new ValidatedPasswordCallback("Password", false, json(array()), false);
        callback.setPassword("mypw".toCharArray());
        Action action = node.process(getContext(singletonList(callback), sharedState, transientState));

        // Then
        assertThat(action.callbacks).hasSize(1);
        assertThat(action.callbacks.get(0)).isInstanceOf(ValidatedPasswordCallback.class);
        assertThat(((ValidatedPasswordCallback) action.callbacks.get(0)).getPrompt()).isEqualTo("Password");
        assertThat(((ValidatedPasswordCallback) action.callbacks.get(0)).getPassword()).isNullOrEmpty();
        assertThat(action.sharedState).isNull();
        assertThat(action.transientState).isNull();
    }

    private TreeContext getContext(List<? extends Callback> callbacks, JsonValue sharedState,
            JsonValue transientState) {

        return new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, sharedState, transientState,
                new ExternalRequestContext.Builder().locales(preferredLocales).build(),
                callbacks, Optional.empty());
    }

    static class MockResourceBundle extends ResourceBundle {
        private final String value;

        MockResourceBundle(String value) {
            this.value = value;
        }

        @Override
        protected Object handleGetObject(String key) {
            return value;
        }

        @Override
        public Enumeration<String> getKeys() {
            return null;
        }
    }
}
