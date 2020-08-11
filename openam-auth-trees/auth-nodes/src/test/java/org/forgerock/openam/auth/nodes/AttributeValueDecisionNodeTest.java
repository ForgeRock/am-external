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
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.AbstractDecisionNode.FALSE_OUTCOME_ID;
import static org.forgerock.openam.auth.node.api.AbstractDecisionNode.TRUE_OUTCOME_ID;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.OBJECT_ATTRIBUTES;
import static org.forgerock.openam.auth.nodes.AttributeValueDecisionNode.ComparisonOperation.EQUALS;
import static org.forgerock.openam.auth.nodes.AttributeValueDecisionNode.ComparisonOperation.PRESENT;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Optional;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;

import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class AttributeValueDecisionNodeTest {

    @Mock
    private AttributeValueDecisionNode.Config config;

    @Mock
    private Realm realm;

    @Mock
    private IdmIntegrationService idmIntegrationService;

    private AttributeValueDecisionNode node;
    private TreeContext context;

    @DataProvider
    public Object[][] attributeValueData() {
        return new Object[][] {
                {PRESENT, "userName", null, json(object(field("userName", "test"))), TRUE_OUTCOME_ID},
                {PRESENT, "userName", null, json(object()), FALSE_OUTCOME_ID},
                {EQUALS, "userName", "test", json(object(field("userName", "test"))), TRUE_OUTCOME_ID},
                {EQUALS, "userName", "test", json(object(field("userName", "bad"))), FALSE_OUTCOME_ID},
                {EQUALS, "userName", "test", json(object()), FALSE_OUTCOME_ID},
                {EQUALS, "preferences/updates", "true",
                        json(object(field("preferences", object(field("updates", true))))), TRUE_OUTCOME_ID},
                {EQUALS, "age", "1", json(object(field("age", 1))), TRUE_OUTCOME_ID}
        };
    }

    @BeforeMethod
    public void before() throws Exception {
        initMocks(this);

        when(config.identityAttribute()).thenReturn("userName");
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any()))
                .thenReturn(json(object(field("userName", "test"))));
        when(idmIntegrationService.getObject(any(), any(), any(), any(), any(), any()))
                .thenReturn(json(object(field("userName", "test"))));

        context = new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, retrieveSharedState(), json(object()),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());
        node = new AttributeValueDecisionNode(config, realm, idmIntegrationService);
    }

    @Test(dataProvider = "attributeValueData")
    public void shouldReturnExpectedOutcome(AttributeValueDecisionNode.ComparisonOperation operation, String attribute,
            String value, JsonValue user, String expectedOutcome) throws Exception {
        when(config.comparisonOperation()).thenReturn(operation);
        when(config.comparisonAttribute()).thenReturn(attribute);
        when(config.comparisonValue()).thenReturn(Optional.ofNullable(value));
        when(idmIntegrationService.getObject(any(), any(), any(), any(), any(), any())).thenReturn(user);

        assertThat(node.process(context).outcome).isEqualTo(expectedOutcome);
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void shouldThrowNodeProcessExceptionIfGetObjectCallFails() throws Exception {
        when(config.comparisonOperation()).thenReturn(EQUALS);
        when(config.comparisonAttribute()).thenReturn("test");
        when(config.comparisonValue()).thenReturn(Optional.of("foo"));
        when(idmIntegrationService.getObject(any(), any(), any(), any(), any(), any()))
                .thenThrow(new BadRequestException());

        node.process(context);
    }

    private JsonValue retrieveSharedState() {
        return json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")))
                ));
    }
}
