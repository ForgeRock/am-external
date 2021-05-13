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
import static org.forgerock.json.resource.ResourceException.BAD_REQUEST;
import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.OBJECT_ATTRIBUTES;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.LoginCountDecisionNode.LoginCountIntervalType.AT;
import static org.forgerock.openam.auth.nodes.LoginCountDecisionNode.LoginCountIntervalType.EVERY;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.List;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;

import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class LoginCountDecisionNodeTest {

    @Mock
    private LoginCountDecisionNode.Config config;

    @Mock
    private Realm realm;

    @Mock
    private IdmIntegrationService idmIntegrationService;

    private LoginCountDecisionNode loginCountDecisionNode;

    @BeforeMethod
    public void setUp() throws Exception {
        initMocks(this);

        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        when(idmIntegrationService.getUsernameFromContext(any())).thenCallRealMethod();

        loginCountDecisionNode = new LoginCountDecisionNode(config, realm, idmIntegrationService);
    }

    @DataProvider
    public Object[][] loginCountData() {
        return new Object[][] {
                // config AT 2 logins, object logins at 1, expected value to be false
                { AT, 2, 1, false },
                // config AT 2 logins, object logins at 2, expected value to be true
                { AT, 2, 2, true },
                // config AT 2 logins, object logins at 3, expected value to be false
                { AT, 2, 3, false },
                // config EVERY 2 logins, object logins at 2, expected value to be true
                { EVERY, 2, 2, true },
                // config EVERY 2 logins, object logins at 4, expected value to be true
                { EVERY, 2, 4, true },
                // config EVERY 2 logins, object logins at 6, expected value to be true
                { EVERY, 2, 6, true },
                // config EVERY 2 logins, object logins at 2, expected value to be false
                { EVERY, 2, 1, false  },
                // config EVERY 2 logins, object logins at 3, expected value to be false
                { EVERY, 2, 3, false }
        };
    }

    @Test(dataProvider = "loginCountData")
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

    @Test(dataProvider = "loginCountData")
    public void shouldReturnExpectedValueOnEvaluateWithUsername(LoginCountDecisionNode.LoginCountIntervalType type,
            int amount, int objectLogins, boolean expectedResult) throws Exception {

        JsonValue sharedState = json(object(
                field(USERNAME, "test-username")
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

    @Test
    public void shouldReturnFalseIfObjectDoesNotContainLoginCountAttribute() throws Exception {
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
    public void shouldReturnFalseIfLoginCountAttributeIsNull() throws Exception {
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
    public void shouldReturnFalseIfLoginCountIsNull() throws Exception {
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

    @Test(expectedExceptions = NodeProcessException.class)
    public void shouldThrowExceptionIfFailedToRetrieveObject() throws Exception {
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));

        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any()))
                .thenThrow(newResourceException(BAD_REQUEST));

        loginCountDecisionNode.process(getContext(emptyList(), sharedState));
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void shouldThrowExceptionIfNoUsernameIdentity() throws Exception {
        JsonValue sharedState = json(object());


        loginCountDecisionNode.process(getContext(emptyList(), sharedState));
    }

    private TreeContext getContext(List<? extends Callback> callbacks, JsonValue sharedState) {
        return new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, sharedState,
                new ExternalRequestContext.Builder().build(), callbacks);
    }
}
