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
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonPointer.ptr;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceException.BAD_REQUEST;
import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.openam.auth.nodes.AcceptTermsAndConditionsNode.ACCEPT_DATE;
import static org.forgerock.openam.auth.nodes.AcceptTermsAndConditionsNode.TERMS_ACCEPTED;
import static org.forgerock.openam.auth.nodes.AcceptTermsAndConditionsNode.TERMS_VERSION;
import static org.forgerock.openam.auth.nodes.utils.IdmIntegrationNodeUtils.OBJECT_MAPPER;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.OBJECT_ATTRIBUTES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import org.forgerock.json.JsonPatch;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CreateObjectNodeTest {

    @Mock
    CreateObjectNode.Config config;

    @Mock
    Realm realm;

    @Mock
    private IdmIntegrationService idmIntegrationService;

    @Captor
    private ArgumentCaptor<JsonValue> termsCaptor;

    @Captor
    private ArgumentCaptor<JsonValue> newObjectCaptor;

    private CreateObjectNode node;
    private TreeContext context;
    private JsonValue schema;

    @BeforeEach
    void before() throws Exception {
        schema = json(OBJECT_MAPPER.readValue(getClass().getResource("/CreateObjectNode/idmSchema.json"), Map.class));

        when(config.identityResource()).thenReturn("managed/user");
        when(idmIntegrationService.getSchema(any(), any(), any())).thenReturn(schema);

        node = new CreateObjectNode(config, realm, idmIntegrationService);

        context = new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, retrieveSharedState(), json(object()),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());
    }

    @Test
    void shouldReturnPropertiesAsInputStates() {
        assertThat(Arrays.stream(node.getInputs())
                .filter(input -> !input.name.equals(TERMS_ACCEPTED))
                .allMatch(input -> schema.get(convertAttributeNameToSchemaPointer(input.name)) != null))
                .isTrue();
    }

    @Test
    void shouldReturnCreatedIfUserObjectSuccessfullyCreated() throws Exception {
        // given
        JsonValue expectedValue = json(object(
                field("userName", "test"),
                field("sn", "foo"),
                field("givenName", "bar"),
                field("mail", "test@gmail.com"),
                field("preferences", object(field("updates", true)))));
        when(idmIntegrationService.createObject(any(), any(), any(), newObjectCaptor.capture()))
                .thenReturn(json(object(field(FIELD_CONTENT_ID, "1"))));
        assertThat(node.process(context).outcome).isEqualTo(CreateObjectNode.CreateObjectOutcome.CREATED.toString());
        assertThat(JsonPatch.diff(newObjectCaptor.getValue(), expectedValue).asList()).isEmpty();
    }

    @Test
    void shouldReturnFailureOnCreationIfMissingRequiredFields() throws Exception {
        // given
        // OBJECT_ATTRIBUTE in sharedState missing "mail" required field
        context.sharedState.get(OBJECT_ATTRIBUTES).remove("mail");

        // when & then
        assertThat(node.process(context).outcome).isEqualTo(CreateObjectNode.CreateObjectOutcome.FAILURE.toString());
    }

    @Test
    void shouldUpdateTermsIfPresentInSharedState() throws Exception {
        // given
        JsonValue sharedState = retrieveSharedState();
        sharedState.add(TERMS_ACCEPTED, object(
                field(TERMS_VERSION, "1"),
                field(ACCEPT_DATE, "today")));
        context = new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, sharedState, json(object()),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());

        when(idmIntegrationService.createObject(any(), any(), any(), any()))
                .thenReturn(json(object(field(FIELD_CONTENT_ID, "1"))));
        doNothing().when(idmIntegrationService).updateTermsAccepted(any(), any(), any(), any(), termsCaptor.capture());

        // when
        node.process(context);

        // then
        assertThat(termsCaptor.getValue().isEqualTo(sharedState.get(TERMS_ACCEPTED))).isTrue();
    }

    @Test
    void shouldReturnFailureIfCreationFailed() throws Exception {
        // given
        doThrow(newResourceException(BAD_REQUEST, "Failed Policy"))
                .when(idmIntegrationService).createObject(any(), any(), any(), any());

        // when & then
        assertThat(node.process(context).outcome).isEqualTo(CreateObjectNode.CreateObjectOutcome.FAILURE.toString());
        assertThat(node.process(context).errorMessage).isEqualTo("Failed Policy");
    }

    private JsonPointer convertAttributeNameToSchemaPointer(String attributeName) {
        return Arrays.stream(attributeName.split("/"))
                .map(attrToken -> ptr("properties").child(attrToken))
                .reduce(JsonPointer::concat)
                .orElse(ptr("properties"));
    }

    private JsonValue retrieveSharedState() {
        return json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field("userName", "test"),
                        field("sn", "foo"),
                        field("givenName", "bar"),
                        field("mail", "test@gmail.com"),
                        field("preferences", object(
                                field("updates", true)
                        ))))));

    }
}
