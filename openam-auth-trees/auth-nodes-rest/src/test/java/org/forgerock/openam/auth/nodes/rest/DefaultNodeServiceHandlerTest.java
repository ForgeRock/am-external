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
 * Copyright 2025-2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.rest;

import static com.sun.identity.sm.SchemaType.ORGANIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Set;

import org.forgerock.am.trees.api.NodeRegistry;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.rest.sms.SmsCollectionProviderFactory;
import org.forgerock.openam.core.rest.sms.SubordinateActionsInterceptorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.sm.SchemaType;

@ExtendWith(MockitoExtension.class)
class DefaultNodeServiceHandlerTest {

    @Mock
    private NodeRegistry nodeRegistry;
    @Mock
    private CoreWrapper coreWrapper;
    @Mock
    private SmsCollectionProviderFactory nodeProviderFactory;
    @Mock
    private NodeServiceHandler customNodeHandler;
    @Mock
    private NodeResourceVersionRouterFactory nodeResourceVersionRouterFactory;
    @Mock
    private NodeVersionProviderFactory nodeVersionProviderFactory;
    @Mock
    private SubordinateActionsInterceptorFactory subordinateActionsInterceptorFactory;

    private DefaultNodeServiceHandler nodeServiceHandler;

    @BeforeEach
    void setUp() {
        nodeServiceHandler = new DefaultNodeServiceHandler(nodeRegistry, nodeResourceVersionRouterFactory,
                nodeProviderFactory, nodeVersionProviderFactory, coreWrapper,
                Set.of(customNodeHandler), subordinateActionsInterceptorFactory);
    }

    @Test
    void shouldReturnOrganisationWhenGetApplicableSchemaTypes() {
        // When
        Set<SchemaType> applicableSchemaTypes = nodeServiceHandler.getApplicableSchemaTypes();

        // Then
        assertThat(applicableSchemaTypes).containsExactly(ORGANIZATION);
    }


    @Test
    void shouldReturnTrueWhenHandlesWithNodeType() {
        // Given
        String serviceName = "testService";
        given(nodeRegistry.isNodeService(serviceName)).willReturn(true);

        // When
        boolean handles = nodeServiceHandler.handles(serviceName);

        // Then
        assertThat(handles).isTrue();
    }

    @Test
    void shouldReturnFalseWhenHandlesWithNonNodeType() {
        // Given
        String serviceName = "testService";
        given(nodeRegistry.isNodeService(serviceName)).willReturn(false);

        // When
        boolean handles = nodeServiceHandler.handles(serviceName);

        // Then
        assertThat(handles).isFalse();
    }

    @Test
    void shouldReturnFalseWhenHandlesWithNodeTypeButCustomHandlerHandlesIt() {
        // Given
        String serviceName = "testService";
        given(nodeRegistry.isNodeService(serviceName)).willReturn(true);
        given(customNodeHandler.handles(serviceName)).willReturn(true);

        // When
        boolean handles = nodeServiceHandler.handles(serviceName);

        // Then
        assertThat(handles).isFalse();
    }
}
