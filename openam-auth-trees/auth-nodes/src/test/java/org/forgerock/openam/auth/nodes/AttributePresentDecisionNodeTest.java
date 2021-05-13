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
 * Copyright 2020 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static java.lang.String.valueOf;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.OBJECT_ATTRIBUTES;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Optional;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.forgerock.util.query.QueryFilter;

import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AttributePresentDecisionNodeTest {

    @Mock
    private AttributePresentDecisionNode.Config config;

    @Mock
    private Realm realm;

    @Mock
    private IdmIntegrationService idmIntegrationService;

    private AttributePresentDecisionNode node;
    private TreeContext context;

    @BeforeMethod
    public void before() {
        initMocks(this);

        when(config.identityAttribute()).thenReturn("userName");
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        when(idmIntegrationService.getUsernameFromContext(any())).thenCallRealMethod();

        context = new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, retrieveSharedState(), json(object()),
                    new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());
        node = new AttributePresentDecisionNode(config, realm, idmIntegrationService);
    }

    @Test
    public void shouldReturnTrueIfFieldExists() throws Exception {
        when(config.presentAttribute()).thenReturn("password");
        when(idmIntegrationService.getObject(any(), any(), any(), any(QueryFilter.class), any()))
                .thenReturn(managedObject());

        assertThat(node.process(context).outcome).isEqualTo(valueOf(true));
    }

    @Test
    public void shouldReturnTrueIfUsernameIdentityExists() throws Exception {
        context = new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, retrieveUsernameSharedState(),
                json(object()), new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());
        node = new AttributePresentDecisionNode(config, realm, idmIntegrationService);

        when(config.presentAttribute()).thenReturn("password");
        when(idmIntegrationService.getObject(any(), any(), any(), any(QueryFilter.class), any()))
                .thenReturn(managedObject());

        assertThat(node.process(context).outcome).isEqualTo(valueOf(true));
    }

    @Test
    public void shouldReturnFalseIfFieldDoesNotExist() throws Exception {
        when(config.presentAttribute()).thenReturn("invalid");
        when(idmIntegrationService.getObject(any(), any(), any(), any(QueryFilter.class), any()))
                .thenThrow(new NotFoundException());

        assertThat(node.process(context).outcome).isEqualTo(valueOf(false));
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void shouldThrowNodeProcessExceptionIfGetObjectCallFails() throws Exception {
        when(config.presentAttribute()).thenReturn("password");
        when(idmIntegrationService.getObject(any(), any(), any(), any(QueryFilter.class), any()))
                .thenThrow(new BadRequestException());

        node.process(context);
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void shouldThrowNodeProcessExceptionIfNoIdentityFound() throws Exception {
        context = new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, json(object()), json(object()),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());
        node = new AttributePresentDecisionNode(config, realm, idmIntegrationService);

        when(config.presentAttribute()).thenReturn("password");

        node.process(context);
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

    private static JsonValue managedObject() {
        return json(object(
                field(FIELD_CONTENT_ID, "1"),
                field("password", "Test1234")
        ));
    }

}
