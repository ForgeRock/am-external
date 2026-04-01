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

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.Connection;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.auth.nodes.framework.PageNode;
import org.forgerock.services.context.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PageNodeCollectionProviderUtilsTest {

    @Mock
    private Context context;
    @Mock
    private ConnectionFactory connectionFactory;
    @InjectMocks
    private PageNodeCollectionProviderUtils pageNodeCollectionProviderUtils;

    @Test
    void shouldReturnJsonValue() {
        // Given
        JsonValue fromSms = json(object(field("nodes", array("configString1", "configString2"))));
        JsonValue expectedJsonValue1 = json(object(field("_id", "configString1"), field("nodeType", "nodeType1"),
                field("displayName", "DisplayName1")));
        JsonValue expectedJsonValue2 = json(object(field("_id", "configString2"), field("nodeType", "nodeType2"),
                field("displayName", "DisplayName2")));

        try (MockedStatic<PageNode.ChildNodeConfig> mockedStatic = mockStatic(PageNode.ChildNodeConfig.class)) {
            PageNode.ChildNodeConfig mockConfig1 = mock(PageNode.ChildNodeConfig.class);
            PageNode.ChildNodeConfig mockConfig2 = mock(PageNode.ChildNodeConfig.class);
            given(mockConfig1.asJson()).willReturn(expectedJsonValue1);
            given(mockConfig2.asJson()).willReturn(expectedJsonValue2);

            mockedStatic
                    .when(() -> PageNode.ChildNodeConfig.create("configString1"))
                    .thenReturn(mockConfig1);
            mockedStatic
                    .when(() -> PageNode.ChildNodeConfig.create("configString2"))
                    .thenReturn(mockConfig2);

            // When
            JsonValue result = pageNodeCollectionProviderUtils.getJsonValue(fromSms);

            // Then
            assertThat(result.get("nodes").size()).isEqualTo(2);
            assertThat(result.get("nodes").get(0).get("_id").asString())
                    .isEqualTo(expectedJsonValue1.get("_id").asString());
            assertThat(result.get("nodes").get(0).get("nodeType").asString())
                    .isEqualTo(expectedJsonValue1.get("nodeType").asString());
            assertThat(result.get("nodes").get(0).get("displayName").asString())
                    .isEqualTo(expectedJsonValue1.get("displayName").asString());
            assertThat(result.get("nodes").get(1).get("_id").asString())
                    .isEqualTo(expectedJsonValue2.get("_id").asString());
            assertThat(result.get("nodes").get(1).get("nodeType").asString())
                    .isEqualTo(expectedJsonValue2.get("nodeType").asString());
            assertThat(result.get("nodes").get(1).get("displayName").asString())
                    .isEqualTo(expectedJsonValue2.get("displayName").asString());
        }
    }

    @Test
    void shouldDeleteNodes() throws ResourceException {

        // Given
        var response = newResourceResponse("", "", json(object(field("nodes", array(object(field("_id", "node1"),
                field("nodeType", "UsernameCollector")))))));
        final Connection connection = mock(Connection.class);
        ArgumentCaptor<DeleteRequest> captor = ArgumentCaptor.forClass(DeleteRequest.class);
        when(connection.delete(any(), captor.capture())).thenReturn(response);
        when(connectionFactory.getConnection()).thenReturn(connection);

        // When
        pageNodeCollectionProviderUtils.deleteNodes(context, response);

        // Then
        final DeleteRequest value = captor.getValue();
        assertThat(value.getResourcePath())
                .isEqualTo("realm-config/authentication/authenticationtrees/nodes/UsernameCollector/node1");
    }

    @Test
    void shouldReturnAttributesFromContent() {
        // Given
        JsonValue node = JsonValue.json(JsonValue.object(JsonValue.field("key", "value")));
        JsonValue content = JsonValue.json(JsonValue.object(JsonValue.field("nodes", JsonValue.array(node))));

        // When
        List<String> nodes = pageNodeCollectionProviderUtils.getAttributesFromContent(content);

        // Then
        assertThat(nodes).containsExactly(PageNode.ChildNodeConfig.create(node).toString());
    }
}
