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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.AbstractDecisionNode.FALSE_OUTCOME_ID;
import static org.forgerock.openam.auth.node.api.AbstractDecisionNode.TRUE_OUTCOME_ID;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.AttributeValueDecisionNode.ComparisonOperation.EQUALS;
import static org.forgerock.openam.auth.nodes.AttributeValueDecisionNode.ComparisonOperation.PRESENT;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.OBJECT_ATTRIBUTES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.stream.Stream;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class AttributeValueDecisionNodeTest {

    @Mock
    private AttributeValueDecisionNode.Config config;

    @Mock
    private Realm realm;

    @Mock
    private IdmIntegrationService idmIntegrationService;

    private AttributeValueDecisionNode node;
    private TreeContext context;

    public static Stream<Arguments> attributeValueData() {
        return Stream.of(
                Arguments.of(PRESENT, "userName", null, json(object(field("userName", "test"))), TRUE_OUTCOME_ID),
                Arguments.of(PRESENT, "userName", null, json(object()), FALSE_OUTCOME_ID),
                Arguments.of(EQUALS, "userName", "test", json(object(field("userName", "test"))), TRUE_OUTCOME_ID),
                Arguments.of(EQUALS, "userName", "test", json(object(field("userName", "bad"))), FALSE_OUTCOME_ID),
                Arguments.of(EQUALS, "userName", "test", json(object()), FALSE_OUTCOME_ID),
                Arguments.of(EQUALS, "preferences/updates", "true", json(object(field("preferences", object(field("updates", true))))), TRUE_OUTCOME_ID),
                Arguments.of(EQUALS, "age", "1", json(object(field("age", 1))), TRUE_OUTCOME_ID)
        );
    }

    @BeforeEach
    void before() throws Exception {
        when(config.identityAttribute()).thenReturn("userName");
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        when(idmIntegrationService.getUsernameFromContext(any())).thenCallRealMethod();
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any()))
                .thenReturn(json(object(field("userName", "test"))));
        when(idmIntegrationService.getObject(any(), any(), any(), any(), any(), any()))
                .thenReturn(json(object(field("userName", "test"))));

        context = new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, retrieveSharedState(), json(object()),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());
        node = new AttributeValueDecisionNode(config, realm, idmIntegrationService);
    }

    @ParameterizedTest
    @MethodSource("attributeValueData")
    public void shouldReturnExpectedOutcome(AttributeValueDecisionNode.ComparisonOperation operation, String attribute,
            String value, JsonValue user, String expectedOutcome) throws Exception {
        when(config.comparisonOperation()).thenReturn(operation);
        when(config.comparisonAttribute()).thenReturn(attribute);
        when(config.comparisonValue()).thenReturn(Optional.ofNullable(value));
        when(idmIntegrationService.getObject(any(), any(), any(), any(), any(), any())).thenReturn(user);

        assertThat(node.process(context).outcome).isEqualTo(expectedOutcome);
    }

    @ParameterizedTest
    @MethodSource("attributeValueData")
    public void shouldReturnExpectedOutcomeFromUsername(AttributeValueDecisionNode.ComparisonOperation operation,
            String attribute, String value, JsonValue user, String expectedOutcome) throws Exception {
        context = new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, retrieveUsernameSharedState(),
                json(object()), new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());
        node = new AttributeValueDecisionNode(config, realm, idmIntegrationService);

        when(config.comparisonOperation()).thenReturn(operation);
        when(config.comparisonAttribute()).thenReturn(attribute);
        when(config.comparisonValue()).thenReturn(Optional.ofNullable(value));
        when(idmIntegrationService.getObject(any(), any(), any(), any(), any(), any())).thenReturn(user);

        assertThat(node.process(context).outcome).isEqualTo(expectedOutcome);
    }

    @Test
    void shouldThrowNodeProcessExceptionIfGetObjectCallFails() throws Exception {
        when(config.comparisonOperation()).thenReturn(EQUALS);
        when(config.comparisonAttribute()).thenReturn("test");
        when(config.comparisonValue()).thenReturn(Optional.of("foo"));
        when(idmIntegrationService.getObject(any(), any(), any(), any(), any(), any()))
                .thenThrow(new BadRequestException());

        assertThatThrownBy(() -> node.process(context)).isInstanceOf(NodeProcessException.class);
    }

    @Test
    void shouldThrowNodeProcessExceptionIfNoIdentityFound() throws Exception {
        context = new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, json(object()), json(object()),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());
        node = new AttributeValueDecisionNode(config, realm, idmIntegrationService);
        when(config.comparisonOperation()).thenReturn(EQUALS);
        when(config.comparisonAttribute()).thenReturn("test");
        when(config.comparisonValue()).thenReturn(Optional.of("foo"));

        assertThatThrownBy(() -> node.process(context)).isInstanceOf(NodeProcessException.class);
    }

    private JsonValue retrieveSharedState() {
        return json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")))
        ));
    }

    private JsonValue retrieveUsernameSharedState() {
        return json(object(
                field(USERNAME, "test-username")
        ));
    }
}
