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
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonPointer.ptr;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceException.BAD_REQUEST;
import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.OBJECT_ATTRIBUTES;
import static org.forgerock.openam.auth.nodes.AcceptTermsAndConditionsNode.ACCEPT_DATE;
import static org.forgerock.openam.auth.nodes.AcceptTermsAndConditionsNode.TERMS_ACCEPTED;
import static org.forgerock.openam.auth.nodes.AcceptTermsAndConditionsNode.TERMS_VERSION;
import static org.forgerock.openam.auth.nodes.utils.IdmIntegrationNodeUtils.OBJECT_MAPPER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CreateObjectNodeTest {

    @Mock
    CreateObjectNode.Config config;

    @Mock
    Realm realm;

    @Mock
    private IdmIntegrationService idmIntegrationService;

    @Captor
    private ArgumentCaptor<JsonValue> termsCaptor;

    private CreateObjectNode node;
    private TreeContext context;
    private JsonValue schema;

    @BeforeMethod
    public void before() throws Exception {
        initMocks(this);

        schema = json(OBJECT_MAPPER.readValue(getClass().getResource("/CreateObjectNode/idmSchema.json"), Map.class));

        when(config.identityResource()).thenReturn("managed/user");
        when(realm.asPath()).thenReturn("/");

        when(idmIntegrationService.getSchema(any(), any(), any())).thenReturn(schema);
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();

        node = new CreateObjectNode(config, realm, idmIntegrationService);

        context = new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, retrieveSharedState(), json(object()),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());
    }

    @Test
    public void shouldReturnPropertiesAsInputStates() {
        assertThat(Arrays.stream(node.getInputs())
                .filter(input -> !input.name.equals(TERMS_ACCEPTED))
                .allMatch(input -> schema.get(convertAttributeNameToSchemaPointer(input.name)) != null))
                .isTrue();
    }

    @Test
    public void shouldReturnCreatedIfUserObjectSuccessfullyCreated() throws Exception {
        when(idmIntegrationService.createObject(any(), any(), any(), any()))
                .thenReturn(json(object(field(FIELD_CONTENT_ID, "1"))));
        assertThat(node.process(context).outcome).isEqualTo(CreateObjectNode.CreateObjectOutcome.CREATED.toString());
    }

    @Test
    public void shouldReturnFailureOnCreationIfMissingRequiredFields() throws Exception {
        // OBJECT_ATTRIBUTE in sharedState missing "mail" required field
        context.sharedState.get(OBJECT_ATTRIBUTES).remove("mail");

        assertThat(node.process(context).outcome).isEqualTo(CreateObjectNode.CreateObjectOutcome.FAILURE.toString());
    }

    @Test
    public void shouldUpdateTermsIfPresentInSharedState() throws Exception {

        JsonValue sharedState = retrieveSharedState();
        sharedState.add(TERMS_ACCEPTED, object(
                        field(TERMS_VERSION, "1"),
                        field(ACCEPT_DATE, "today")));
        context = new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, sharedState, json(object()),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());

        when(idmIntegrationService.createObject(any(), any(), any(), any()))
                .thenReturn(json(object(field(FIELD_CONTENT_ID, "1"))));
        doNothing().when(idmIntegrationService).updateTermsAccepted(any(), any(), any(), any(), termsCaptor.capture());

        node.process(context);

        assertThat(termsCaptor.getValue().isEqualTo(sharedState.get(TERMS_ACCEPTED))).isTrue();
    }

    @Test
    public void shouldReturnFailureIfCreationFailed() throws Exception {
        doThrow(newResourceException(BAD_REQUEST)).when(idmIntegrationService).createObject(any(), any(), any(), any());

        assertThat(node.process(context).outcome).isEqualTo(CreateObjectNode.CreateObjectOutcome.FAILURE.toString());
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
