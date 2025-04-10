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
import static org.forgerock.json.JsonPointer.ptr;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.AbstractDecisionNode.FALSE_OUTCOME_ID;
import static org.forgerock.openam.auth.node.api.AbstractDecisionNode.TRUE_OUTCOME_ID;
import static org.forgerock.openam.auth.nodes.utils.IdmIntegrationNodeUtils.OBJECT_MAPPER;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.OBJECT_ATTRIBUTES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RequiredAttributesDecisionNodeTest {

    @Mock
    RequiredAttributesDecisionNode.Config config;

    @Mock
    private Realm realm;

    @Mock
    private IdmIntegrationService idmIntegrationService;

    @InjectMocks
    private RequiredAttributesDecisionNode node;
    private JsonValue schema;

    @BeforeEach
    void before() throws Exception {
        schema = json(OBJECT_MAPPER.readValue(getClass().getResource("/RequiredAttributesDecisionNode/idmSchema.json"),
                Map.class));

        when(config.identityResource()).thenReturn("managed/user");
        when(idmIntegrationService.getSchema(any(), any(), any())).thenReturn(schema);
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
    }

    @Test
    void shouldReturnTrueOutcomeIfAllRequiredAttributesArePresent() throws Exception {
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
    void shouldReturnFalseOutcomeIfAnyRequiredAttributesAreMissing() throws Exception {
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
    void shouldReturnFalseOutcomeIfAnyRequiredAttributesAreNull() throws Exception {
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
    void shouldReturnFalseOutcomeIfObjectAttributesIsMissing() throws Exception {
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
