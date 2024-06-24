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
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.AbstractDecisionNode.FALSE_OUTCOME_ID;
import static org.forgerock.openam.auth.node.api.AbstractDecisionNode.TRUE_OUTCOME_ID;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.OBJECT_ATTRIBUTES;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
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

public class ProfileCompletenessDecisionNodeTest {

    @Mock
    private ProfileCompletenessDecisionNode.Config config;

    @Mock
    private Realm realm;

    @Mock
    private IdmIntegrationService idmIntegrationService;

    private ProfileCompletenessDecisionNode node;
    private TreeContext context;

    @DataProvider
    public Object[][] profileCompletenessData() {
        return new Object[][] {
                {50, 51f, TRUE_OUTCOME_ID},
                {50, 50f, FALSE_OUTCOME_ID},
                {50, 49f, FALSE_OUTCOME_ID}
        };
    }

    @BeforeMethod
    public void before() throws Exception {
        initMocks(this);

        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        when(idmIntegrationService.getUsernameFromContext(any())).thenCallRealMethod();

        node = new ProfileCompletenessDecisionNode(config, realm, idmIntegrationService);
        context = new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, retrieveSharedState(), json(object()),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());
    }

    @Test(dataProvider = "profileCompletenessData")
    public void shouldReturnExpectedOutcome(int threshold, float score, String expected) throws Exception {
        when(config.threshold()).thenReturn(threshold);
        when(idmIntegrationService.getProfileCompleteness(any(), any(), any(), any(), any())).thenReturn(score);

        assertThat(node.process(context).outcome).isEqualTo(expected);
    }

    @Test(dataProvider = "profileCompletenessData")
    public void shouldReturnExpectedOutceomFromUsernameIdentity(int threshold, float score, String expected)
            throws Exception {

        context = new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, retrieveUsernameSharedState(),
                json(object()), new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());

        when(config.threshold()).thenReturn(threshold);
        when(idmIntegrationService.getProfileCompleteness(any(), any(), any(), any(), any())).thenReturn(score);

        assertThat(node.process(context).outcome).isEqualTo(expected);
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void shouldThrowExceptionIfNoIdentityFound() throws Exception {
        context = new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, json(object()),
                json(object()), new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());

        node.process(context);
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void shouldThrowNodeProcessExceptionIfGetObjectCallFails() throws Exception {
        when(config.threshold()).thenReturn(100);
        when(idmIntegrationService.getProfileCompleteness(any(), any(), any(), any(), any()))
                .thenThrow(new BadRequestException());

        node.process(context);
    }

    private JsonValue retrieveSharedState() {
        return json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));
    }

    private JsonValue retrieveUsernameSharedState() {
        return json(object(
                field(USERNAME, "test-username")
        ));
    }
}
