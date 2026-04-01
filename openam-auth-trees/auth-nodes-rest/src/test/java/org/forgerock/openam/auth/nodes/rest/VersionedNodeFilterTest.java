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
 * Copyright 2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Requests.newActionRequest;
import static org.forgerock.json.resource.Responses.newActionResponse;
import static org.mockito.BDDMockito.given;

import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.util.promise.Promises;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VersionedNodeFilterTest {

    @Mock
    RequestHandler handler;

    @Test
    void filterActionShouldReplaceIdWhenGetType() {
        // Given
        VersionedNodeFilter versionedNodeFilter = new VersionedNodeFilter("1.0");
        ActionRequest actionRequest = newActionRequest("1.0", "getType");
        ActionResponse actionResponse = newActionResponse(json(object(field("_id", "UsernameCollectorNode"))));
        given(handler.handleAction(null, actionRequest)).willReturn(Promises.newResultPromise(actionResponse));

        // When
        versionedNodeFilter.filterAction(null, actionRequest, handler);

        // Then
        assertThat(actionResponse.getJsonContent().get("_id").asString()).isEqualTo("1.0");
    }

    @Test
    void filterActionShouldNotReplaceIdWhenNotGetType() {
        // Given
        VersionedNodeFilter versionedNodeFilter = new VersionedNodeFilter("1.0");
        ActionRequest actionRequest = newActionRequest("1.0", "otherAction");
        ActionResponse actionResponse = newActionResponse(json(object(field("_id", "UsernameCollectorNode"))));
        given(handler.handleAction(null, actionRequest)).willReturn(Promises.newResultPromise(actionResponse));

        // When
        versionedNodeFilter.filterAction(null, actionRequest, handler);

        // Then
        assertThat(actionResponse.getJsonContent().get("_id").asString()).isEqualTo("UsernameCollectorNode");
    }
}
