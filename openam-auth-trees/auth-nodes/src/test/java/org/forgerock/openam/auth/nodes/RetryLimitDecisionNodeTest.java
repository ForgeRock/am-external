/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
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
