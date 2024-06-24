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
 * Copyright 2019-2023 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonPointer.ptr;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.AbstractDecisionNode.FALSE_OUTCOME_ID;
import static org.forgerock.openam.auth.node.api.AbstractDecisionNode.TRUE_OUTCOME_ID;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.OBJECT_ATTRIBUTES;
import static org.forgerock.openam.auth.nodes.utils.IdmIntegrationNodeUtils.OBJECT_MAPPER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Map;
import java.util.Optional;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;

import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RequiredAttributesDecisionNodeTest {

    @Mock
    RequiredAttributesDecisionNode.Config config;

    @Mock
    private Realm realm;

    @Mock
    private IdmIntegrationService idmIntegrationService;

    private RequiredAttributesDecisionNode node;
    private JsonValue schema;

    @BeforeMethod
    public void before() throws Exception {
        initMocks(this);

        schema = json(OBJECT_MAPPER.readValue(getClass().getResource("/RequiredAttributesDecisionNode/idmSchema.json"),
                Map.class));

        when(config.identityResource()).thenReturn("managed/user");
        when(idmIntegrationService.getSchema(any(), any(), any())).thenReturn(schema);
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();

        node = new RequiredAttributesDecisionNode(config, realm, idmIntegrationService);
    }

    @Test
    public void shouldReturnTrueOutcomeIfAllRequiredAttributesArePresent() throws Exception {
        // given
        JsonValue sharedState = json(object());
        schema.get("required").stream()
                .forEach(attr -> sharedState.putPermissive(ptr(OBJECT_ATTRIBUTES).child(attr.asString()), "someValue"));
        TreeContext context = new TreeContext(sharedState, json(object()),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());

        // when
        Action action = node.process(context);

        // then
        assertThat(action.outcome).isEqualTo(TRUE_OUTCOME_ID);
    }

    @Test
    public void shouldReturnFalseOutcomeIfAnyRequiredAttributesAreMissing() throws Exception {
        // given
        JsonValue sharedState = json(object());
        sharedState.putPermissive(ptr(OBJECT_ATTRIBUTES).child(schema.get("required").get(0).asString()), "someValue");
        TreeContext context = new TreeContext(sharedState, json(object()),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());

        // when
        Action action = node.process(context);

        // then
        assertThat(action.outcome).isEqualTo(FALSE_OUTCOME_ID);
    }

    @Test
    public void shouldReturnFalseOutcomeIfAnyRequiredAttributesAreNull() throws Exception {
        // given
        JsonValue sharedState = json(object());
        schema.get("required").stream()
                .forEach(attr -> sharedState.putPermissive(ptr(OBJECT_ATTRIBUTES).child(attr.asString()), null));
        TreeContext context = new TreeContext(sharedState, json(object()),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());

        // when
        Action action = node.process(context);

        // then
        assertThat(action.outcome).isEqualTo(FALSE_OUTCOME_ID);
    }

    @Test
    public void shouldReturnFalseOutcomeIfObjectAttributesIsMissing() throws Exception {
        // given
        JsonValue sharedState = json(object());
        TreeContext context = new TreeContext(sharedState, json(object()),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());

        // when
        Action action = node.process(context);

        // then
        assertThat(action.outcome).isEqualTo(FALSE_OUTCOME_ID);
    }
}
