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
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceException.BAD_REQUEST;
import static org.forgerock.json.resource.ResourceException.NOT_FOUND;
import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.json.resource.ResourceResponse.FIELD_CONTENT_ID;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.OBJECT_ATTRIBUTES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class IncrementLoginCountNodeTest {

    @Mock
    private IncrementLoginCountNode.Config config;

    @Mock
    private Realm realm;

    @Mock
    private IdmIntegrationService idmIntegrationService;

    private TreeContext context;
    private IncrementLoginCountNode node;

    @BeforeEach
    void before() throws Exception {
        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();

        context = new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, retrieveSharedState(), json(object()),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());
        node = new IncrementLoginCountNode(config, realm, idmIntegrationService);
    }

    @Test
    void shouldReturnOutcomeIfNoIdentityFound() throws Exception {
        when(idmIntegrationService.getUsernameFromContext(any())).thenCallRealMethod();
        context = new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, json(object()), json(object()),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());
        node = new IncrementLoginCountNode(config, realm, idmIntegrationService);

        assertThat(node.process(context).outcome).isEqualTo("outcome");
    }

    @Test
    void shouldReturnOutcomeIfUsernameIdentityFound() throws Exception {
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any()))
                .thenReturn(json(object(field(FIELD_CONTENT_ID, "1"))));
        when(idmIntegrationService.getUsernameFromContext(any())).thenCallRealMethod();
        context = new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, retrieveUsernameSharedState(),
                json(object()), new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());
        node = new IncrementLoginCountNode(config, realm, idmIntegrationService);

        assertThat(node.process(context).outcome).isEqualTo("outcome");
    }

    @Test
    void shouldReturnOutcomeIfNoObjectToIncrement() throws Exception {
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any()))
                .thenThrow(newResourceException(NOT_FOUND));
        verify(idmIntegrationService, times(0)).incrementLoginCount(any(), any(), any(), any());
        assertThat(node.process(context).outcome).isEqualTo("outcome");
    }

    @Test
    void shouldReturnOutcomeIfLoginCountSuccessfullyIncremented() throws Exception {
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any()))
                .thenReturn(json(object(field(FIELD_CONTENT_ID, "1"))));
        doNothing().when(idmIntegrationService).incrementLoginCount(any(), any(), any(), any());
        assertThat(node.process(context).outcome).isEqualTo("outcome");
    }

    @Test
    void shouldReturnOutcomeIfLoginCountIncrementFailed() throws Exception {
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any()))
                .thenReturn(json(object(field(FIELD_CONTENT_ID, "1"))));
        doThrow(newResourceException(BAD_REQUEST)).when(idmIntegrationService)
                .incrementLoginCount(any(), any(), any(), any());
        assertThat(node.process(context).outcome).isEqualTo("outcome");
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
