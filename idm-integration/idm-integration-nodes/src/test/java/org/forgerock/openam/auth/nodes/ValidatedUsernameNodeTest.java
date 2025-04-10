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
import static org.mockito.Mockito.when;

import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@ExtendWith(MockitoExtension.class)
public class ValidatedUsernameNodeTest {

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER =
                new ObjectMapper().configure(JsonParser.Feature.ALLOW_COMMENTS, true)
                        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                        .enable(SerializationFeature.INDENT_OUTPUT)
                        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
    }

    @Mock
    private ValidatedUsernameNode.Config config;

    @Mock
    private Realm realm;

    @Mock
    private IdmIntegrationService idmIntegrationService;

    @Mock
    private PreferredLocales preferredLocales;

    @InjectMocks
    private ValidatedUsernameNode node;


    @BeforeEach
    void before() throws Exception {
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
        ResourceBundle resourceBundle = new ValidatedUsernameNodeTest.MockResourceBundle("User Name");
        given(preferredLocales.getBundleInPreferredLocale(any(), any())).willReturn(resourceBundle);

        // When
        Action action = node.process(getContext(emptyList(), sharedState, transientState));

        // Then
        assertThat(action.callbacks).hasSize(1);
        assertThat(action.callbacks.get(0)).isInstanceOf(NameCallback.class);
        assertThat(((NameCallback) action.callbacks.get(0)).getPrompt()).isEqualTo("User Name");
        assertThat((Object) action.sharedState).isNull();
        assertThat((Object) action.transientState).isNull();
    }

    @Test
    void processWithCallbackShouldStoreUsernameInState() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));
        JsonValue transientState = json(object());
        ResourceBundle resourceBundle = new ValidatedUsernameNodeTest.MockResourceBundle("User Name");
        given(preferredLocales.getBundleInPreferredLocale(any(), any())).willReturn(resourceBundle);

        // When
        when(config.usernameAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(idmIntegrationService.storeAttributeInState(any(), any(), any())).thenCallRealMethod();
        NameCallback callback = new NameCallback("Username");
        callback.setName("myuser");
        Action action = node.process(getContext(singletonList(callback), sharedState, transientState));

        // Then
        assertThat(action.callbacks).hasSize(0);
        assertThat(action.sharedState.isDefined("username")).isTrue();
        assertThat(action.sharedState.get("username").asString()).isEqualTo("myuser");
        assertThat(action.sharedState.isDefined(OBJECT_ATTRIBUTES)).isTrue();
        assertThat(action.sharedState.get(OBJECT_ATTRIBUTES).get(DEFAULT_IDM_IDENTITY_ATTRIBUTE).asString())
                .isEqualTo("myuser");
    }

    @Test
    void processWithEmptyCallbackShouldNotStoreUsernameInState() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));
        JsonValue transientState = json(object());
        ResourceBundle resourceBundle = new ValidatedUsernameNodeTest.MockResourceBundle("User Name");
        given(preferredLocales.getBundleInPreferredLocale(any(), any())).willReturn(resourceBundle);

        // When
        NameCallback callback = new NameCallback("Username");
        callback.setName("");
        Action action = node.process(getContext(singletonList(callback), sharedState, transientState));

        // Then
        assertThat(action.callbacks).hasSize(0);
        assertThat(action.sharedState.isDefined("username")).isFalse();
        assertThat(action.sharedState.isDefined(OBJECT_ATTRIBUTES)).isTrue();
        assertThat(action.sharedState.get(OBJECT_ATTRIBUTES).get(DEFAULT_IDM_IDENTITY_ATTRIBUTE).asString())
                .isEqualTo("test");
    }

    @Test
    void processWithBadUsernameShouldReturnCallbackWithPolicyFailures() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));
        JsonValue transientState = json(object());

        // When
        when(config.usernameAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(idmIntegrationService.getSchema(any(), any(), any())).thenReturn(new JsonValue(OBJECT_MAPPER.readValue(
                ValidatedUsernameNodeTest.class.getResource("/ValidatedUsernameNode/idmSchema.json"), Map.class)));
        when(config.validateInput()).thenReturn(true);
        when(idmIntegrationService.getValidationRequirements(any(), any(), any()))
                .thenReturn(new JsonValue(OBJECT_MAPPER.readValue(ValidatedUsernameNodeTest.class.getResource(
                        "/ValidatedUsernameNode/idmPolicyRead.json"), Map.class)));
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
                                field("property", DEFAULT_IDM_IDENTITY_ATTRIBUTE)
                        )
                ))
        )));
        ValidatedUsernameCallback callback = new ValidatedUsernameCallback("Username", json(array()), false);
        callback.setUsername("badusername");
        Action action = node.process(getContext(singletonList(callback), sharedState, transientState));

        // Then
        assertThat(action.callbacks).hasSize(1);
        assertThat(action.callbacks.get(0)).isInstanceOf(ValidatedUsernameCallback.class);
        assertThat(((ValidatedUsernameCallback) action.callbacks.get(0)).getPrompt()).isEqualTo("Username");
        assertThat(((ValidatedUsernameCallback) action.callbacks.get(0)).getUsername()).isEqualTo("badusername");
        assertThat((Object) action.sharedState).isNull();
        assertThat((Object) action.transientState).isNull();
    }

    private TreeContext getContext(List<? extends Callback> callbacks, JsonValue sharedState,
            JsonValue transientState) {
        return new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, sharedState, transientState,
                new ExternalRequestContext.Builder().locales(preferredLocales).build(), callbacks, Optional.empty());
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
