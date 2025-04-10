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
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.LoginCountDecisionNode.LoginCountIntervalType.AT;
import static org.forgerock.openam.auth.nodes.LoginCountDecisionNode.LoginCountIntervalType.EVERY;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.OBJECT_ATTRIBUTES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
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

@ExtendWith(MockitoExtension.class)
public class LoginCountDecisionNodeTest {

    @Mock
    private LoginCountDecisionNode.Config config;

    @Mock
    private Realm realm;

    @Mock
    private IdmIntegrationService idmIntegrationService;

    @InjectMocks
    private LoginCountDecisionNode loginCountDecisionNode;

    public static Stream<Arguments> loginCountData() {
        return Stream.of(
                // config AT 2 logins, object logins at 1, expected value to be false
                Arguments.of(AT, 2, 1, false),
                // config AT 2 logins, object logins at 2, expected value to be true
                Arguments.of(AT, 2, 2, true),
                // config AT 2 logins, object logins at 3, expected value to be false
                Arguments.of(AT, 2, 3, false),
                // config EVERY 2 logins, object logins at 2, expected value to be true
                Arguments.of(EVERY, 2, 2, true),
                // config EVERY 2 logins, object logins at 4, expected value to be true
                Arguments.of(EVERY, 2, 4, true),
                // config EVERY 2 logins, object logins at 6, expected value to be true
                Arguments.of(EVERY, 2, 6, true),
                // config EVERY 2 logins, object logins at 2, expected value to be false
                Arguments.of(EVERY, 2, 1, false),
                // config EVERY 2 logins, object logins at 3, expected value to be false
                Arguments.of(EVERY, 2, 3, false)
        );
    }

    @BeforeEach
    void setUp() throws Exception {
        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
    }

    @ParameterizedTest
    @MethodSource("loginCountData")
    public void shouldReturnExpectedValueOnEvaluate(LoginCountDecisionNode.LoginCountIntervalType type,
            int amount, int objectLogins, boolean expectedResult) throws Exception {

        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));

        when(config.interval()).thenReturn(type);
        when(config.amount()).thenReturn(amount);
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any()))
                .thenReturn(json(object(field(FIELD_CONTENT_ID, "1"))));
        when(idmIntegrationService.retrieveLoginCount(any(), any(), any(), any()))
                .thenReturn(json(object(field("loginCount", objectLogins))));


        String outcome = loginCountDecisionNode.process(getContext(emptyList(), sharedState)).outcome;

        assertThat(outcome).isEqualTo(String.valueOf(expectedResult));
    }

    @ParameterizedTest
    @MethodSource("loginCountData")
    public void shouldReturnExpectedValueOnEvaluateWithUsername(LoginCountDecisionNode.LoginCountIntervalType type,
            int amount, int objectLogins, boolean expectedResult) throws Exception {

        JsonValue sharedState = json(object(
                field(USERNAME, "test-username")
        ));

        when(idmIntegrationService.getUsernameFromContext(any())).thenCallRealMethod();
        when(config.interval()).thenReturn(type);
        when(config.amount()).thenReturn(amount);
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any()))
                .thenReturn(json(object(field(FIELD_CONTENT_ID, "1"))));
        when(idmIntegrationService.retrieveLoginCount(any(), any(), any(), any()))
                .thenReturn(json(object(field("loginCount", objectLogins))));


        String outcome = loginCountDecisionNode.process(getContext(emptyList(), sharedState)).outcome;

        assertThat(outcome).isEqualTo(String.valueOf(expectedResult));
    }

    @Test
    void shouldReturnFalseIfObjectDoesNotContainLoginCountAttribute() throws Exception {
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));

        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any()))
                .thenReturn(json(object(field(FIELD_CONTENT_ID, "1"))));
        when(idmIntegrationService.retrieveLoginCount(any(), any(), any(), any()))
                .thenReturn(json(object()));

        String outcome = loginCountDecisionNode.process(getContext(emptyList(), sharedState)).outcome;

        assertThat(outcome).isEqualTo(String.valueOf(false));
    }

    @Test
    void shouldReturnFalseIfLoginCountAttributeIsNull() throws Exception {
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));

        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any()))
                .thenReturn(json(object(field(FIELD_CONTENT_ID, "1"))));
        when(idmIntegrationService.retrieveLoginCount(any(), any(), any(), any()))
                .thenReturn(json(object(field("loginCount", null))));

        String outcome = loginCountDecisionNode.process(getContext(emptyList(), sharedState)).outcome;

        assertThat(outcome).isEqualTo(String.valueOf(false));
    }

    @Test
    void shouldReturnFalseIfLoginCountIsNull() throws Exception {
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));

        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any()))
                .thenReturn(json(object(field(FIELD_CONTENT_ID, "1"))));
        when(idmIntegrationService.retrieveLoginCount(any(), any(), any(), any()))
                .thenReturn(json(object()));

        String outcome = loginCountDecisionNode.process(getContext(emptyList(), sharedState)).outcome;

        assertThat(outcome).isEqualTo(String.valueOf(false));
    }

    @Test
    void shouldThrowExceptionIfFailedToRetrieveObject() throws Exception {
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));

        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any()))
                .thenThrow(newResourceException(BAD_REQUEST));

        assertThatThrownBy(() -> loginCountDecisionNode.process(getContext(emptyList(), sharedState)))
                .isInstanceOf(NodeProcessException.class)
                .hasMessageContaining("org.forgerock.json.resource.BadRequestException: Bad Request");
    }

    @Test
    void shouldThrowExceptionIfNoUsernameIdentity() throws Exception {
        when(idmIntegrationService.getUsernameFromContext(any())).thenCallRealMethod();
        JsonValue sharedState = json(object());

        assertThatThrownBy(() -> loginCountDecisionNode.process(getContext(emptyList(), sharedState)))
                .isInstanceOf(NodeProcessException.class)
                .hasMessageContaining("userName not present in state");
    }

    private TreeContext getContext(List<? extends Callback> callbacks, JsonValue sharedState) {
        return new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, sharedState,
                new ExternalRequestContext.Builder().build(), callbacks);
    }
}
