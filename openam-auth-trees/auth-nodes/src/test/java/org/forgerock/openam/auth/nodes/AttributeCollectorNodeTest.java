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
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.OBJECT_ATTRIBUTES;
import static org.forgerock.openam.auth.nodes.utils.IdmIntegrationNodeUtils.OBJECT_MAPPER;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.authentication.callbacks.BooleanAttributeInputCallback;
import org.forgerock.openam.authentication.callbacks.NumberAttributeInputCallback;
import org.forgerock.openam.authentication.callbacks.StringAttributeInputCallback;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AttributeCollectorNodeTest {

    static final String SCHEMA_PATH = "openidm/schema/managed";
    static final String OBJECT_PATH = "openidm/managed";
    static final String POLICY_PATH = "openidm/policy/managed/user";

    @Mock
    AttributeCollectorNode.Config config;

    @Mock
    Realm realm;

    @Mock
    IdmIntegrationService idmIntegrationService;

    @Mock
    RequestHandler requestHandler;

    AttributeCollectorNode node;
    boolean returnObject = false;

    @BeforeMethod
    private void init() throws Exception {
        openMocks(this);

        // Given
        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(config.attributesToCollect()).thenReturn(Arrays.asList(
                "givenName",
                "sn",
                "mail",
                "preferences/updates",
                "age"));

        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        when(idmIntegrationService.storeAttributeInState(any(), any(), any())).thenCallRealMethod();

        when(realm.asPath()).thenReturn("/");

        node = new AttributeCollectorNode(config, realm, idmIntegrationService);
    }

    private void initIdmPayloads() throws Exception {
        when(idmIntegrationService.getSchema(any(), any(), any()))
                .thenReturn(getHttpReturnFor(SCHEMA_PATH));
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any()))
                .thenReturn(getHttpReturnFor(OBJECT_PATH));
        when(idmIntegrationService.validateInput(any(), any(), any(), any(), any()))
                .thenReturn(json(object(field("result", true))));
        when(idmIntegrationService.getValidationRequirements(any(), any(), any()))
                .thenReturn(getHttpReturnFor(POLICY_PATH));
    }

    @Test
    public void callbacksAbsentShouldReturnCallbacks() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));
        returnObject = false;

        // When
        initIdmPayloads();
        Action action = node.process(getContext(emptyList(), sharedState));

        // Then
        assertThat(action.outcome).isEqualTo(null);
        assertThat(action.callbacks).hasSize(5);
        assertThat(action.callbacks.get(0)).isInstanceOf(StringAttributeInputCallback.class);
        assertThat(((StringAttributeInputCallback) action.callbacks.get(0)).getName()).isEqualTo("givenName");
        assertThat(action.callbacks.get(1)).isInstanceOf(StringAttributeInputCallback.class);
        assertThat(((StringAttributeInputCallback) action.callbacks.get(1)).getName()).isEqualTo("sn");
        assertThat(action.callbacks.get(2)).isInstanceOf(StringAttributeInputCallback.class);
        assertThat(((StringAttributeInputCallback) action.callbacks.get(2)).getName()).isEqualTo("mail");
        assertThat(action.callbacks.get(3)).isInstanceOf(BooleanAttributeInputCallback.class);
        assertThat(((BooleanAttributeInputCallback) action.callbacks.get(3)).getName())
                .isEqualTo("preferences/updates");
        assertThat(((BooleanAttributeInputCallback) action.callbacks.get(3)).getPrompt())
                .isEqualTo("Send me news and updates");
        assertThat(action.callbacks.get(4)).isInstanceOf(NumberAttributeInputCallback.class);
        assertThat(((NumberAttributeInputCallback) action.callbacks.get(4)).getName())
                .isEqualTo("age");
        assertThat(((NumberAttributeInputCallback) action.callbacks.get(4)).getPrompt())
                .isEqualTo("Age");
        assertThat((Object) action.sharedState).isNull();
    }

    @Test
    public void callbacksAbsentShouldStoreSchemaInState() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));
        returnObject = false;

        // When
        initIdmPayloads();
        Action action = node.process(getContext(emptyList(), sharedState));

        // Then
        assertThat(action.outcome).isEqualTo(null);
    }

    @Test
    public void shouldReturnExistingObjectValuesInCallbacks() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));
        returnObject = true;

        // When
        initIdmPayloads();
        Action action = node.process(getContext(emptyList(), sharedState));

        // Then
        assertThat(action.callbacks).isNotEmpty();
        assertThat(action.callbacks).hasSize(5);
        assertThat(action.callbacks.get(0)).isInstanceOf(StringAttributeInputCallback.class);
        assertThat(((StringAttributeInputCallback) action.callbacks.get(0)).getValue()).isEqualTo("First");
        assertThat(action.callbacks.get(1)).isInstanceOf(StringAttributeInputCallback.class);
        assertThat(((StringAttributeInputCallback) action.callbacks.get(1)).getValue()).isEqualTo("Last");
        assertThat(action.callbacks.get(2)).isInstanceOf(StringAttributeInputCallback.class);
        assertThat(((StringAttributeInputCallback) action.callbacks.get(2)).getValue()).isEqualTo("nobody@example.com");
        assertThat(action.callbacks.get(3)).isInstanceOf(BooleanAttributeInputCallback.class);
        assertThat(((BooleanAttributeInputCallback) action.callbacks.get(3)).getValue())
                .isEqualTo(true);
        assertThat(((NumberAttributeInputCallback) action.callbacks.get(4)).getValue())
                .isEqualTo(18);
    }

    @Test
    public void shouldDefaultValuesInCallbackIfNoExistingObject() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));
        returnObject = false;

        // When
        initIdmPayloads();
        Action action = node.process(getContext(emptyList(), sharedState));

        // Then
        assertThat(action.callbacks).isNotEmpty();
        assertThat(action.callbacks).hasSize(5);
        assertThat(action.callbacks.get(0)).isInstanceOf(StringAttributeInputCallback.class);
        assertThat(((StringAttributeInputCallback) action.callbacks.get(0)).getValue()).isNullOrEmpty();
        assertThat(action.callbacks.get(1)).isInstanceOf(StringAttributeInputCallback.class);
        assertThat(((StringAttributeInputCallback) action.callbacks.get(1)).getValue()).isNullOrEmpty();
        assertThat(action.callbacks.get(2)).isInstanceOf(StringAttributeInputCallback.class);
        assertThat(((StringAttributeInputCallback) action.callbacks.get(2)).getValue()).isNullOrEmpty();
        assertThat(action.callbacks.get(3)).isInstanceOf(BooleanAttributeInputCallback.class);
        assertThat(((BooleanAttributeInputCallback) action.callbacks.get(3)).getValue()).isEqualTo(false);
        assertThat(((NumberAttributeInputCallback) action.callbacks.get(4)).getValue()).isEqualTo(null);
    }

    @Test
    public void shouldDefaultValuesInCallbackToStateIfNoExistingObject() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test"),
                        field("givenName", "Bob"),
                        field("sn", "User"),
                        field("mail", "bob@example.com"),
                        field("preferences", object(
                                field("updates", true)
                        ))
                ))
        ));
        returnObject = false;

        // When
        initIdmPayloads();
        Action action = node.process(getContext(emptyList(), sharedState));

        // Then
        assertThat(action.callbacks).isNotEmpty();
        assertThat(action.callbacks).hasSize(5);
        assertThat(action.callbacks.get(0)).isInstanceOf(StringAttributeInputCallback.class);
        assertThat(((StringAttributeInputCallback) action.callbacks.get(0)).getValue()).isEqualTo("Bob");
        assertThat(action.callbacks.get(1)).isInstanceOf(StringAttributeInputCallback.class);
        assertThat(((StringAttributeInputCallback) action.callbacks.get(1)).getValue()).isEqualTo("User");
        assertThat(action.callbacks.get(2)).isInstanceOf(StringAttributeInputCallback.class);
        assertThat(((StringAttributeInputCallback) action.callbacks.get(2)).getValue()).isEqualTo("bob@example.com");
        assertThat(action.callbacks.get(3)).isInstanceOf(BooleanAttributeInputCallback.class);
        assertThat(((BooleanAttributeInputCallback) action.callbacks.get(3)).getValue()).isEqualTo(true);
        assertThat(((NumberAttributeInputCallback) action.callbacks.get(4)).getValue()).isEqualTo(null);
    }

    @Test
    public void callbacksPresentAddsToSharedStateAndClearsTransientState() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));
        returnObject = false;
        List<Callback> callbacks = new ArrayList<>();
        callbacks.add(new StringAttributeInputCallback("givenName", "First Name", "First", true));
        callbacks.add(new StringAttributeInputCallback("sn", "Last Name", "Last", true));
        callbacks.add(new StringAttributeInputCallback("mail", "Email Address", "nobody@example.com", true));
        callbacks.add(new BooleanAttributeInputCallback("preferences/updates", "Send me updates", false, true));
        callbacks.add(new NumberAttributeInputCallback("age", "Age", 21.0, true));

        // When
        initIdmPayloads();
        Action action = node.process(getContext(callbacks, sharedState));

        // Then
        assertThat(action.sharedState.isDefined(OBJECT_ATTRIBUTES)).isTrue();
        assertThat(action.sharedState.get(OBJECT_ATTRIBUTES).asMap()).hasSize(6);
        assertThat(action.sharedState.get(OBJECT_ATTRIBUTES).get("givenName").asString()).isEqualTo("First");
        assertThat(action.sharedState.get(OBJECT_ATTRIBUTES).get("sn").asString()).isEqualTo("Last");
        assertThat(action.sharedState.get(OBJECT_ATTRIBUTES).get("mail").asString()).isEqualTo("nobody@example.com");
        assertThat(action.sharedState.get(OBJECT_ATTRIBUTES).get("preferences").get("updates")
                .asBoolean()).isEqualTo(false);
        assertThat(action.sharedState.get(OBJECT_ATTRIBUTES).get("age").asDouble()).isEqualTo(21.0);
    }

    @Test
    public void callbacksPresentMissingRequiredValueReturnsCallbacksAgain() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));
        returnObject = false;
        List<Callback> callbacks = new ArrayList<>();
        callbacks.add(new StringAttributeInputCallback("givenName", "First Name", "First", true));
        callbacks.add(new StringAttributeInputCallback("sn", "Last Name", "Last", true));
        callbacks.add(new StringAttributeInputCallback("mail", "Email Address", null, true));
        callbacks.add(new BooleanAttributeInputCallback("preferences/updates", "Send me updates", false, true));
        callbacks.add(new NumberAttributeInputCallback("age", "Age", null, true));

        // When
        initIdmPayloads();
        when(config.required()).thenReturn(true);
        Action action = node.process(getContext(callbacks, sharedState));

        // Then
        assertThat(action.outcome).isEqualTo(null);
        assertThat(action.callbacks).hasSize(5);
        assertThat(action.callbacks.get(0)).isInstanceOf(StringAttributeInputCallback.class);
        assertThat(((StringAttributeInputCallback) action.callbacks.get(0)).getName()).isEqualTo("givenName");
        assertThat(action.callbacks.get(1)).isInstanceOf(StringAttributeInputCallback.class);
        assertThat(((StringAttributeInputCallback) action.callbacks.get(1)).getName()).isEqualTo("sn");
        assertThat(action.callbacks.get(2)).isInstanceOf(StringAttributeInputCallback.class);
        assertThat(((StringAttributeInputCallback) action.callbacks.get(2)).getName()).isEqualTo("mail");
        assertThat(action.callbacks.get(3)).isInstanceOf(BooleanAttributeInputCallback.class);
        assertThat(((BooleanAttributeInputCallback) action.callbacks.get(3)).getName())
                .isEqualTo("preferences/updates");
        assertThat(action.callbacks.get(4)).isInstanceOf(NumberAttributeInputCallback.class);
        assertThat(((NumberAttributeInputCallback) action.callbacks.get(4)).getName()).isEqualTo("age");
    }

    @Test
    public void shouldRejectRequiredCallbacksWithEmptyValues() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));
        returnObject = false;
        List<Callback> callbacks = new ArrayList<>();
        callbacks.add(new StringAttributeInputCallback("givenName", "First Name", "", true));

        // When
        initIdmPayloads();
        when(config.required()).thenReturn(true);
        Action action = node.process(getContext(callbacks, sharedState));

        // Then
        assertThat(action.outcome).isNotEqualTo("outcome");
        assertThat(action.callbacks).hasAtLeastOneElementOfType(StringAttributeInputCallback.class);
        assertThat(action.callbacks).extracting("name").contains("givenName");
    }

    @Test
    public void callbacksPresentMissingAndNotRequiredAddsToSharedStateAndClearsTransientState() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));
        returnObject = false;
        List<Callback> callbacks = new ArrayList<>();
        callbacks.add(new StringAttributeInputCallback("givenName", "First Name", "First", false));
        callbacks.add(new StringAttributeInputCallback("sn", "Last Name", "Last", false));
        callbacks.add(new StringAttributeInputCallback("mail", "Email Address", null, false));
        callbacks.add(new BooleanAttributeInputCallback("preferences/updates", "Send me updates", false, false));
        callbacks.add(new NumberAttributeInputCallback("age", "Age", 21.0, false));

        // When
        initIdmPayloads();
        when(config.required()).thenReturn(false);
        Action action = node.process(getContext(callbacks, sharedState));

        // Then
        assertThat(action.sharedState.isDefined(OBJECT_ATTRIBUTES)).isTrue();
        assertThat(action.sharedState.get(OBJECT_ATTRIBUTES).asMap()).hasSize(6);
        assertThat(action.sharedState.get(OBJECT_ATTRIBUTES).get("givenName").asString()).isEqualTo("First");
        assertThat(action.sharedState.get(OBJECT_ATTRIBUTES).get("sn").asString()).isEqualTo("Last");
        assertThat(action.sharedState.get(OBJECT_ATTRIBUTES).get("mail").asString()).isEqualTo(null);
        assertThat(action.sharedState.get(OBJECT_ATTRIBUTES).get("preferences").get("updates")
                .asBoolean()).isEqualTo(false);
        assertThat(action.sharedState.get(OBJECT_ATTRIBUTES).get("age").asDouble()).isEqualTo(21.0);
    }

    @Test
    public void nullableFieldGeneratesNotRequiredCallback() throws Exception {
        // Given
        when(config.attributesToCollect()).thenReturn(Collections.singletonList("description"));
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));
        returnObject = false;

        // When
        initIdmPayloads();
        Action action = node.process(getContext(emptyList(), sharedState));

        // Then
        assertThat(action.outcome).isEqualTo(null);
        assertThat(action.callbacks).hasSize(1);
        assertThat(action.callbacks.get(0)).isInstanceOf(StringAttributeInputCallback.class);
        assertThat(((StringAttributeInputCallback) action.callbacks.get(0)).getName()).isEqualTo("description");
        assertThat(((StringAttributeInputCallback) action.callbacks.get(0)).isRequired()).isFalse();
    }

    @Test
    public void unrequestedCallbacksAreNotAddedToSharedState() throws Exception {
        // Given
        JsonValue sharedState = json(object(
            field(OBJECT_ATTRIBUTES, object(
                field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
            ))
        ));
        returnObject = false;
        List<Callback> callbacks = new ArrayList<>();
        callbacks.add(new StringAttributeInputCallback("givenName", "First Name", "First", true));

        // Add unrequested callback
        callbacks.add(new StringAttributeInputCallback("description", "HAX", "HAXXED", true));

        // When
        initIdmPayloads();
        Action action = node.process(getContext(callbacks, sharedState));

        // Then
        assertThat(action.sharedState.isDefined(OBJECT_ATTRIBUTES)).isTrue();
        assertThat(action.sharedState.get(OBJECT_ATTRIBUTES).get("givenName").asString()).isEqualTo("First");

        // Unrequested callback should NOT be added to shared state
        assertThat(action.sharedState.get(OBJECT_ATTRIBUTES).get("description").asString()).isNullOrEmpty();
    }

    @Test
    public void missingMandatoryRequestedCallbacksResendsCallbacks() throws Exception {
        // Given
        JsonValue sharedState = json(object(
            field(OBJECT_ATTRIBUTES, object(
                field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
            ))
        ));
        returnObject = false;
        List<Callback> callbacks = new ArrayList<>();
        callbacks.add(new StringAttributeInputCallback("givenName", "First Name", "First", true));
        callbacks.add(new StringAttributeInputCallback("sn", "Last Name", "Last", true));
        callbacks.add(new StringAttributeInputCallback("mail", "Email Address", "nobody@example.com", true));
        callbacks.add(new BooleanAttributeInputCallback("preferences/updates", "Send me updates", false, true));
        // missing requested 'Age' callback completely

        // When
        initIdmPayloads();
        when(config.required()).thenReturn(true);
        Action action = node.process(getContext(callbacks, sharedState));

        // Then
        assertThat(action.sendingCallbacks()).isTrue();
    }

    @Test
    public void nullMandatoryRequestedCallbacksResendsCallbacks() throws Exception {
        // Given
        JsonValue sharedState = json(object(
            field(OBJECT_ATTRIBUTES, object(
                field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
            ))
        ));
        returnObject = false;
        List<Callback> callbacks = new ArrayList<>();
        callbacks.add(new StringAttributeInputCallback("givenName", "First Name", "First", true));
        callbacks.add(new StringAttributeInputCallback("sn", "Last Name", "Last", true));
        callbacks.add(new StringAttributeInputCallback("mail", "Email Address", "nobody@example.com", true));
        callbacks.add(new BooleanAttributeInputCallback("preferences/updates", "Send me updates", false, true));
        // add Age callback but with null value
        callbacks.add(new NumberAttributeInputCallback("age", "Age", null, true));

        // When
        initIdmPayloads();
        when(config.required()).thenReturn(true);
        Action action = node.process(getContext(callbacks, sharedState));

        // Then
        assertThat(action.sendingCallbacks()).isTrue();
    }

    @Test
    public void nullMandatoryNonRequestedCallbacksDoesNotResendCallbacks() throws Exception {
        // Given
        JsonValue sharedState = json(object(
            field(OBJECT_ATTRIBUTES, object(
                field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
            ))
        ));
        returnObject = false;
        List<Callback> callbacks = new ArrayList<>();
        callbacks.add(new StringAttributeInputCallback("givenName", "First Name", "First", true));
        callbacks.add(new StringAttributeInputCallback("sn", "Last Name", "Last", true));
        callbacks.add(new StringAttributeInputCallback("mail", "Email Address", "nobody@example.com", true));
        callbacks.add(new BooleanAttributeInputCallback("preferences/updates", "Send me updates", false, true));
        callbacks.add(new NumberAttributeInputCallback("age", "Age", 21.0, true));

        // Add unrequested callback with null value
        callbacks.add(new StringAttributeInputCallback("description", "HAX", null, true));

        // When
        initIdmPayloads();
        when(config.required()).thenReturn(true);
        Action action = node.process(getContext(callbacks, sharedState));

        // Then
        assertThat(action.sharedState.isDefined(OBJECT_ATTRIBUTES)).isTrue();
        assertThat(action.sharedState.get(OBJECT_ATTRIBUTES).get("givenName").asString()).isEqualTo("First");
        assertThat(action.sharedState.get(OBJECT_ATTRIBUTES).get("sn").asString()).isEqualTo("Last");
        assertThat(action.sharedState.get(OBJECT_ATTRIBUTES).get("mail").asString()).isEqualTo("nobody@example.com");
        assertThat(action.sharedState.get(OBJECT_ATTRIBUTES).get("preferences").get("updates")
            .asBoolean()).isEqualTo(false);
        assertThat(action.sharedState.get(OBJECT_ATTRIBUTES).get("age").asDouble()).isEqualTo(21.0);

        // Unrequested callback should NOT be added to shared state
        assertThat(action.sharedState.get(OBJECT_ATTRIBUTES).get("description").asString()).isNullOrEmpty();
    }

    private TreeContext getContext(List<? extends Callback> callbacks, JsonValue sharedState) {
        return new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, sharedState,
                new ExternalRequestContext.Builder().build(), callbacks);
    }

    private JsonValue getHttpReturnFor(String path) throws IOException {
        if (path.equals(SCHEMA_PATH)) {
            return new JsonValue(OBJECT_MAPPER.readValue(
                    AttributeCollectorNodeTest.class.getResource("/AttributeCollectorNode/idmSchema.json"), Map.class));
        } else if (path.equals(OBJECT_PATH) && returnObject) {
            return new JsonValue(OBJECT_MAPPER.readValue(
                    AttributeCollectorNodeTest.class
                            .getResource("/AttributeCollectorNode/idmExistingObject.json"), Map.class));
        } else if (path.equals(POLICY_PATH)) {
            return new JsonValue(OBJECT_MAPPER.readValue(
                    AttributeCollectorNodeTest.class
                            .getResource("/AttributeCollectorNode/idmPolicyRead.json"), Map.class));
        } else {
            return json(object());
        }
    }
}
