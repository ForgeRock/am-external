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
import static org.forgerock.json.resource.ResourceException.BAD_REQUEST;
import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.utils.IdmIntegrationNodeUtils.OBJECT_MAPPER;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.OBJECT_ATTRIBUTES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for QueryFilterDecisionNode.
 */
@ExtendWith(MockitoExtension.class)
public class QueryFilterDecisionNodeTest {

    @Mock
    private QueryFilterDecisionNode.Config config;

    @Mock
    private Realm realm;

    @Mock
    private IdmIntegrationService idmIntegrationService;

    @InjectMocks
    private QueryFilterDecisionNode node;
    private JsonValue userObject;

    public static Stream<Arguments> queryFilterData() {
        return Stream.of(
                Arguments.of("userName pr", true),
                Arguments.of("userName eq \"foo\"", false),
                Arguments.of("mail co \"example.com\"", true),
                Arguments.of("age gt 10 AND givenName eq \"First\"", true),
                Arguments.of("age lt 15 AND givenName eq \"First\"", false)
        );
    }

    @BeforeEach
    void setUp() throws Exception {
        userObject = json(OBJECT_MAPPER.readValue(getClass()
                .getResource("/QueryFilterDecisionNode/idmUserObject.json"), Map.class));

        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
    }

    @ParameterizedTest
    @MethodSource("queryFilterData")
    public void shouldReturnExpectedValueOnEvaluate(String queryFilter, boolean expectedResult) throws Exception {
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "nobody")
                ))
        ));

        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any(), any(), any()))
                .thenReturn(userObject);
        when(config.queryFilter()).thenReturn(queryFilter);

        String outcome = node.process(getContext(emptyList(), sharedState)).outcome;

        assertThat(outcome).isEqualTo(String.valueOf(expectedResult));
    }

    @ParameterizedTest
    @MethodSource("queryFilterData")
    public void shouldReturnExpectedValueFromUsernameIdentity(String queryFilter, boolean expectedResult)
            throws Exception {
        JsonValue sharedState = json(object(
                field(USERNAME, "test-username")
        ));

        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any(), any(), any()))
                .thenReturn(userObject);
        when(idmIntegrationService.getUsernameFromContext(any())).thenCallRealMethod();
        when(config.queryFilter()).thenReturn(queryFilter);

        String outcome = node.process(getContext(emptyList(), sharedState)).outcome;

        assertThat(outcome).isEqualTo(String.valueOf(expectedResult));
    }

    @Test
    void shouldThrowExceptionIfFailedToRetrieveIdentity() throws Exception {
        when(idmIntegrationService.getUsernameFromContext(any())).thenCallRealMethod();
        JsonValue sharedState = json(object());
        assertThatThrownBy(() -> node.process(getContext(emptyList(), sharedState)))
                .isInstanceOf(NodeProcessException.class)
                .hasMessageContaining("userName not present in state");
    }

    @Test
    void shouldThrowExceptionIfFailedToRetrieveObject() throws Exception {
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));

        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any(), any(), any()))
                .thenThrow(newResourceException(BAD_REQUEST));

        assertThatThrownBy(() -> node.process(getContext(emptyList(), sharedState)))
                .isInstanceOf(NodeProcessException.class)
                .hasMessageContaining("org.forgerock.json.resource.BadRequestException: Bad Request");
    }

    private TreeContext getContext(List<? extends Callback> callbacks, JsonValue sharedState) {
        return new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, sharedState,
                new ExternalRequestContext.Builder().build(), callbacks);
    }
}
