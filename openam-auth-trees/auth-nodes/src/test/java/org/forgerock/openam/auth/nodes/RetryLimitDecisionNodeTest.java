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
 * Copyright 2017-2019 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.RETRIES_REMAINING;
import static org.forgerock.openam.auth.nodes.TreeContextFactory.emptyTreeContext;
import static org.forgerock.openam.auth.nodes.TreeContextFactory.newTreeContext;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.UUID;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RetryLimitDecisionNodeTest {

    @Mock
    RetryLimitDecisionNode.Config serviceConfig;

    @BeforeMethod
    public void before() {
        initMocks(this);
    }

    @Test
    public void processReturnsFalseOutcomeIfRetryLimitIsZero() throws Exception {
        UUID nodeId = UUID.randomUUID();
        Node node = new RetryLimitDecisionNode(serviceConfig, nodeId);
        Action result = node.process(newTreeContext(sharedState(nodeId, 0)));
        assertThat(result.outcome).isEqualTo("Reject");
    }

    @Test
    public void processReturnsTrueOutcomeIfRetryLimitIsGreaterThanZero() throws Exception {
        UUID nodeId = UUID.randomUUID();
        Node node = new RetryLimitDecisionNode(serviceConfig, nodeId);
        Action result = node.process(newTreeContext(sharedState(nodeId, 1)));
        assertThat(result.sharedState.get(nodeRetryLimitKey(nodeId)).asInteger()).isEqualTo(0);
        assertThat(result.outcome).isEqualTo("Retry");
    }

    @Test
    public void processStoresDecreasedConfigRetryLimitInSharedState() throws Exception {
        int configRetryLimit = 10;
        given(serviceConfig.retryLimit()).willReturn(configRetryLimit);
        UUID nodeId = UUID.randomUUID();
        Node node = new RetryLimitDecisionNode(serviceConfig, nodeId);
        Action result = node.process(emptyTreeContext());
        assertThat(result.sharedState.get(nodeRetryLimitKey(nodeId)).asInteger()).isEqualTo(configRetryLimit - 1);
    }

    @Test
    public void processStoresDecreasedRetryLimitInSharedState() throws Exception {
        int configRetryLimit = 20;
        int currentRetryLimit = 10;
        given(serviceConfig.retryLimit()).willReturn(configRetryLimit);
        UUID nodeId = UUID.randomUUID();
        Node node = new RetryLimitDecisionNode(serviceConfig, nodeId);
        Action result = node.process(newTreeContext(sharedState(nodeId, currentRetryLimit)));
        assertThat(result.sharedState.get(nodeRetryLimitKey(nodeId)).asInteger()).isEqualTo(currentRetryLimit - 1);
    }

    private String nodeRetryLimitKey(UUID nodeId) {
        return nodeId + "." + RETRIES_REMAINING;
    }

    private JsonValue sharedState(UUID nodeId, int currentRetryLimit) {
        return json(object(field(nodeRetryLimitKey(nodeId), currentRetryLimit)));
    }
}
