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
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceException.BAD_REQUEST;
import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.OBJECT_ATTRIBUTES;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.utils.IdmIntegrationNodeUtils.OBJECT_MAPPER;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.List;
import java.util.Map;

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

/**
 * Tests for QueryFilterDecisionNode.
 */
public class QueryFilterDecisionNodeTest {

    @Mock
    private QueryFilterDecisionNode.Config config;

    @Mock
    private Realm realm;

    @Mock
    private IdmIntegrationService idmIntegrationService;

    private QueryFilterDecisionNode node;
    private JsonValue userObject;

    @BeforeMethod
    public void setUp() throws Exception {
        initMocks(this);
        userObject = json(OBJECT_MAPPER.readValue(getClass()
                .getResource("/QueryFilterDecisionNode/idmUserObject.json"), Map.class));

        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any(), any(), any()))
                .thenReturn(userObject);
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        when(idmIntegrationService.getUsernameFromContext(any())).thenCallRealMethod();

        node = new QueryFilterDecisionNode(config, realm, idmIntegrationService);
    }

    @DataProvider
    public Object[][] queryFilterData() {
        return new Object[][] {
                { "userName pr", true },
                { "userName eq \"foo\"", false },
                { "mail co \"example.com\"", true },
                { "age gt 10 AND givenName eq \"First\"", true },
                { "age lt 15 AND givenName eq \"First\"", false }
        };
    }

    @Test(dataProvider = "queryFilterData")
    public void shouldReturnExpectedValueOnEvaluate(String queryFilter, boolean expectedResult) throws Exception {
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "nobody")
                ))
        ));

        when(config.queryFilter()).thenReturn(queryFilter);

        String outcome = node.process(getContext(emptyList(), sharedState)).outcome;

        assertThat(outcome).isEqualTo(String.valueOf(expectedResult));
    }

    @Test(dataProvider = "queryFilterData")
    public void shouldReturnExpectedValueFromUsernameIdentity(String queryFilter, boolean expectedResult)
            throws Exception {
        JsonValue sharedState = json(object(
                field(USERNAME, "test-username")
        ));

        when(config.queryFilter()).thenReturn(queryFilter);

        String outcome = node.process(getContext(emptyList(), sharedState)).outcome;

        assertThat(outcome).isEqualTo(String.valueOf(expectedResult));
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void shouldThrowExceptionIfFailedToRetrieveIdentity() throws Exception {
        JsonValue sharedState = json(object());
        node.process(getContext(emptyList(), sharedState));
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void shouldThrowExceptionIfFailedToRetrieveObject() throws Exception {
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));

        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any(), any(), any()))
                .thenThrow(newResourceException(BAD_REQUEST));

        node.process(getContext(emptyList(), sharedState));
    }

    private TreeContext getContext(List<? extends Callback> callbacks, JsonValue sharedState) {
        return new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, sharedState,
                new ExternalRequestContext.Builder().build(), callbacks);
    }
}
