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
 * Copyright 2017-2022 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import org.forgerock.cuppa.junit.CuppaRunner;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.core.CoreWrapper;

import org.forgerock.am.identity.application.LegacyIdentityService;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

import org.forgerock.cuppa.Test;

import static org.forgerock.cuppa.Cuppa.beforeEach;
import static org.forgerock.cuppa.Cuppa.describe;
import static org.forgerock.cuppa.Cuppa.it;
import static org.forgerock.cuppa.Cuppa.when;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.RETRY_COUNT;

import static org.forgerock.openam.auth.nodes.TreeContextFactory.newTreeContext;

import static org.mockito.BDDMockito.given;


@Test
@RunWith(CuppaRunner.class)
public class RetryLimitDecisionNodeTest {

    @Mock
    RetryLimitDecisionNode.Config serviceConfig;

    @Mock
    CoreWrapper coreWrapper;

    @Mock
    LegacyIdentityService identityService;

    private RetryLimitDecisionNode node;

    private JsonValue sharedState;

    private UUID nodeId;

    private int configRetryLimit;

    {
        describe("Retry Limit Decision Node", () -> {
            beforeEach(() -> {
                MockitoAnnotations.initMocks(this);
            });

            when("using shared state to store counter", () -> {
                beforeEach(() -> {
                    given(serviceConfig.incrementUserAttributeOnFailure()).willReturn(false);
                    configRetryLimit = 3;
                });
                when("Retry Limit is met", () -> {
                    it("Returns Reject Outcome", () -> {
                        nodeId = UUID.randomUUID();
                        node = new RetryLimitDecisionNode(serviceConfig, nodeId, coreWrapper, identityService);
                        sharedState = json(object(field(nodeRetryLimitKey(nodeId), configRetryLimit)));
                        assertThat(node.process(newTreeContext(sharedState)).outcome).isEqualTo("Reject");
                    });
                });
                when("Retry Limit is not met", () -> {
                    beforeEach(() -> {
                        nodeId = UUID.randomUUID();
                        node = new RetryLimitDecisionNode(serviceConfig, nodeId, coreWrapper, identityService);
                        sharedState = json(object(field(nodeRetryLimitKey(nodeId), 0)));
                        configRetryLimit = 3;
                        given(serviceConfig.retryLimit()).willReturn(configRetryLimit);

                    });
                    it("Returns Retry Outcome", () -> {
                        assertThat(node.process(newTreeContext(sharedState)).outcome).isEqualTo("Retry");
                    });
                    it("Stores increased config retry limit in shared state", () -> {
                        Action result = node.process(newTreeContext(sharedState));
                        assertThat(result.sharedState.get(nodeRetryLimitKey(nodeId)).asInteger()).isEqualTo(1);
                    });
                    it("Stores increased retry limit in shared state", () -> {
                        configRetryLimit = 20;
                        int currentRetryLimit = 10;
                        given(serviceConfig.retryLimit()).willReturn(configRetryLimit);
                        sharedState = json(object(field(nodeRetryLimitKey(nodeId), currentRetryLimit)));
                        Action result = node.process(newTreeContext(sharedState));
                        assertThat(
                                result.sharedState.get(nodeRetryLimitKey(nodeId)).asInteger())
                                .isEqualTo(currentRetryLimit + 1);
                    });
                });
            });

        });
    }

    private String nodeRetryLimitKey(UUID nodeId) {
        return nodeId + "." + RETRY_COUNT;
    }
}

