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

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonPointer.ptr;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceException.BAD_REQUEST;
import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.AcceptTermsAndConditionsNode.ACCEPT_DATE;
import static org.forgerock.openam.auth.nodes.AcceptTermsAndConditionsNode.TERMS_ACCEPTED;
import static org.forgerock.openam.auth.nodes.AcceptTermsAndConditionsNode.TERMS_VERSION;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDENTITY_RESOURCE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.OBJECT_ATTRIBUTES;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.security.auth.callback.Callback;

import org.assertj.core.api.Assertions;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@ExtendWith(MockitoExtension.class)
public class PatchObjectNodeTest {

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER =
                new ObjectMapper().configure(JsonParser.Feature.ALLOW_COMMENTS, true)
                        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                        .enable(SerializationFeature.INDENT_OUTPUT)
                        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
    }

    @Mock
    private PatchObjectNode.Config config;

    @Mock
    private IdmIntegrationService idmIntegrationService;

    @Mock
    private Realm realm;

    @Captor
    private ArgumentCaptor<JsonValue> termsCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> patchCaptor;

    @InjectMocks
    private PatchObjectNode node;
    private JsonValue schema;

    public static Stream<Arguments> patchData() {
        return Stream.of(
                // fieldsToIgnore - expect none to be left over
                Arguments.of(asSet("userName", "preferences/marketing", "age", "fake-field", "aliasList")),
                // patch all fields - expect all to be leftover
                Arguments.of(emptySet())
        );
    }

    @BeforeEach
    void setUp() throws Exception {
        schema = json(OBJECT_MAPPER.readValue(getClass().getResource("/PatchObjectNode/idmSchema.json"), Map.class));

        when(config.identityResource()).thenReturn(DEFAULT_IDENTITY_RESOURCE);
        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
    }

    @Test
    void shouldReturnPropertiesAsInputStates() throws ResourceException {
        when(idmIntegrationService.getSchema(any(), any(), any())).thenReturn(schema);
        Assertions.assertThat(Arrays.stream(node.getInputs())
                        .filter(input -> !input.name.equals(TERMS_ACCEPTED))
                        .allMatch(input -> schema.get(convertAttributeNameToSchemaPointer(input.name)) != null))
                .isTrue();
    }

    @Test
    void shouldThrowExceptionIfErrorRetrievingExistingObject() throws Exception {
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));

        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any()))
                .thenThrow(newResourceException(BAD_REQUEST));

        assertThatThrownBy(() -> node.process(getContext(Collections.emptyList(), sharedState)))
                .isInstanceOf(NodeProcessException.class)
                .hasMessageContaining("org.forgerock.json.resource.BadRequestException: Bad Request");
    }

    @Test
    void shouldReturnFailureIfFailureToPatchObject() throws Exception {
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test"),
                        field("mail", "new@gmail.com")
                ))
        ));

        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any())).thenReturn(userObject());
        doThrow(newResourceException(BAD_REQUEST, "Failed Policy"))
                .when(idmIntegrationService).patchObject(any(), any(), any(), any(), any(), any());

        Action outcome = node.process(getContext(Collections.emptyList(), sharedState));

        assertThat(outcome.outcome).isEqualTo(PatchObjectNode.PatchObjectOutcome.FAILURE.name());
        assertThat(outcome.errorMessage).isEqualTo("Failed Policy");
    }

    @Test
    void shouldReturnPatchedIfPatchSuccessful() throws Exception {
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test"),
                        field("mail", "new@gmail.com"),
                        field("preferences", object(
                                field("marketing", true
                                ))),
                        field("age", 21)
                ))
        ));

        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any())).thenReturn(userObject());
        doNothing().when(idmIntegrationService).patchObject(any(), any(), any(), any(), patchCaptor.capture(), any());

        String outcome = node.process(getContext(Collections.emptyList(), sharedState)).outcome;
        assertThat(outcome).isEqualTo(PatchObjectNode.PatchObjectOutcome.PATCHED.name());
        assertThat(patchCaptor.getValue())
                .containsExactlyInAnyOrderEntriesOf(sharedState.get(OBJECT_ATTRIBUTES).asMap());
    }

    @Test
    void shouldReturnPatchedIfPatchSuccessfulAndIdentityFromUsername() throws Exception {
        JsonValue sharedState = json(object(
                field(USERNAME, "test-username"),
                field(OBJECT_ATTRIBUTES, object(
                        field("mail", "new@gmail.com"),
                        field("preferences", object(
                                field("marketing", true
                                ))),
                        field("age", 21)
                ))
        ));

        when(idmIntegrationService.getUsernameFromContext(any())).thenCallRealMethod();
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any())).thenReturn(userObject());
        doNothing().when(idmIntegrationService).patchObject(any(), any(), any(), any(), any(), any());

        String outcome = node.process(getContext(Collections.emptyList(), sharedState)).outcome;
        assertThat(outcome).isEqualTo(PatchObjectNode.PatchObjectOutcome.PATCHED.name());
    }

    @Test
    void shouldFailIfNoUsernameInSharedState() throws Exception {
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        String outcome = node.process(getContext(Collections.emptyList(), json(object()))).outcome;

        assertThat(outcome).isEqualTo(PatchObjectNode.PatchObjectOutcome.FAILURE.name());
    }

    @ParameterizedTest
    @MethodSource("patchData")
    public void shouldOnlyPatchItemsAllowed(Set<String> ignoredFields) throws Exception {
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test"),
                        field("mail", "new@gmail.com"),
                        field("preferences", object(
                                field("marketing", true
                                ),
                                field("updates", false))),
                        field("age", 21),
                        field("aliasList", array("foo123", "bar1234"))
                ))
        ));

        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        when(config.ignoredFields()).thenReturn(ignoredFields);
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any())).thenReturn(userObject());
        doNothing().when(idmIntegrationService).patchObject(any(), any(), any(), any(), patchCaptor.capture(), any());

        node.process(getContext(Collections.emptyList(), sharedState));

        // ensure that there are no fields accidentally left in patch
        for (String field : ignoredFields) {
            assertThat(json(patchCaptor.getValue()).get(ptr(field)) == null).isTrue();
        }

        if (ignoredFields.size() == 0) {
            assertThat(patchCaptor.getValue().size()).isEqualTo(sharedState.get(OBJECT_ATTRIBUTES).size());
        }
    }

    @Test
    void shouldSkipPatchIfNoFieldsToPatch() throws Exception {
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test"),
                        field("mail", "new@gmail.com")
                ))
        ));

        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        Set<String> ignoredFields = asSet("userName", "mail");
        when(config.ignoredFields()).thenReturn(ignoredFields);
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any())).thenReturn(userObject());

        node.process(getContext(Collections.emptyList(), sharedState));
        verify(idmIntegrationService, times(0)).patchObject(any(), any(), any(), any(), any(), any());
    }


    @Test
    void shouldUpdateTermsIfPresentInSharedState() throws Exception {

        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test"))),
                field(TERMS_ACCEPTED, object(
                        field(TERMS_VERSION, "1"),
                        field(ACCEPT_DATE, "today")))));

        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any())).thenReturn(userObject());
        doNothing().when(idmIntegrationService).patchObject(any(), any(), any(), any(), any(), any());
        doNothing().when(idmIntegrationService).updateTermsAccepted(any(), any(), any(), any(), termsCaptor.capture());

        node.process(getContext(Collections.emptyList(), sharedState));

        Assertions.assertThat(termsCaptor.getValue().isEqualTo(sharedState.get(TERMS_ACCEPTED))).isTrue();
    }

    private TreeContext getContext(List<? extends Callback> callbacks, JsonValue sharedState) {
        return new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, sharedState,
                new ExternalRequestContext.Builder().build(), callbacks);
    }

    private JsonPointer convertAttributeNameToSchemaPointer(String attributeName) {
        return Arrays.stream(attributeName.split("/"))
                .map(attrToken -> ptr("properties").child(attrToken))
                .reduce(JsonPointer::concat)
                .orElse(ptr("properties"));
    }

    private JsonValue userObject() {
        return json(object(
                field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test"),
                field("mail", "test@gmail.com"),
                field("givenName", "foo"),
                field("sn", "bar")
        ));
    }
}
