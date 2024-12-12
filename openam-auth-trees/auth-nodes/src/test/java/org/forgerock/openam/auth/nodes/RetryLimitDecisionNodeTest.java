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
 * Copyright 2017-2024 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.RETRY_COUNT;
import static org.forgerock.openam.auth.nodes.TreeContextFactory.newTreeContext;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.core.CoreWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;


@RunWith(MockitoJUnitRunner.class)
public class RetryLimitDecisionNodeTest {

    @Mock
    private RetryLimitDecisionNode.Config serviceConfig;
    @Mock
    private CoreWrapper coreWrapper;
    @Mock
    private LegacyIdentityService identityService;
    @Mock
    private AMIdentity amIdentity;

    private RetryLimitDecisionNode node;
    private JsonValue sharedState;
    private UUID nodeId;
    private int configRetryLimit;


    @Before
    public void setup() {
        nodeId = UUID.randomUUID();
        configRetryLimit = 3;
        given(serviceConfig.retryLimit()).willReturn(configRetryLimit);
    }

    @Test
    public void testReturnsRejectOutcomeWhenRetryLimitIsMetInSharedState() throws NodeProcessException {
        // given
        given(serviceConfig.incrementUserAttributeOnFailure()).willReturn(false);
        sharedState = json(object(field(nodeRetryLimitKey(nodeId), configRetryLimit)));
        node = new RetryLimitDecisionNode(serviceConfig, nodeId, coreWrapper, identityService);

        // when
        Action action = node.process(newTreeContext(sharedState));

        // then
        assertThat(action.outcome).isEqualTo("Reject");
    }

    @Test
    public void testReturnsRetryOutcomeWhenRetryLimitIsNotInSharedState() throws NodeProcessException {
        // given
        sharedState = json(object());
        given(serviceConfig.incrementUserAttributeOnFailure()).willReturn(false);
        node = new RetryLimitDecisionNode(serviceConfig, nodeId, coreWrapper, identityService);

        // when
        var action = node.process(newTreeContext(sharedState));

        // then
        assertThat(action.outcome).isEqualTo("Retry");
        assertThat(sharedState.get(nodeRetryLimitKey(nodeId)).asInteger()).isEqualTo(1);
    }

    @Test
    public void testReturnsRetryOutcomeWhenRetryLimitIsNotMetInSharedState() throws NodeProcessException {
        // given
        int currentRetryLimit = 1;
        sharedState = json(object(field(nodeRetryLimitKey(nodeId), currentRetryLimit)));
        given(serviceConfig.incrementUserAttributeOnFailure()).willReturn(false);
        node = new RetryLimitDecisionNode(serviceConfig, nodeId, coreWrapper, identityService);

        // when
        var action = node.process(newTreeContext(sharedState));

        // then
        assertThat(action.outcome).isEqualTo("Retry");
        assertThat(sharedState.get(nodeRetryLimitKey(nodeId)).asInteger()).isEqualTo(currentRetryLimit + 1);
    }

    @Test
    public void testReturnsRejectOutcomeWhenRetryLimitIsMetInUserProfile() throws NodeProcessException, IdRepoException,
                                                                                              SSOException {
        // given
        given(serviceConfig.incrementUserAttributeOnFailure()).willReturn(true);
        //noinspection deprecation
        given(coreWrapper.getIdentity(any())).willReturn(amIdentity);
        Set<String> retryCounts = new HashSet<>();
        retryCounts.add(nodeId + "=" + configRetryLimit);
        given(amIdentity.getAttribute("retryLimitNodeCount")).willReturn(retryCounts);
        sharedState = json(object());
        node = new RetryLimitDecisionNode(serviceConfig, nodeId, coreWrapper, identityService);

        // when
        var action = node.process(newTreeContext(sharedState));

        // then
        assertThat(action.outcome).isEqualTo("Reject");
    }

    @Test
    public void testReturnsRetryOutcomeWhenRetryLimitIsNotMetInUserProfile()
            throws NodeProcessException, IdRepoException, SSOException {
        // given
        given(serviceConfig.incrementUserAttributeOnFailure()).willReturn(true);
        //noinspection deprecation
        given(coreWrapper.getIdentity(any())).willReturn(amIdentity);
        Set<String> retryCounts = new HashSet<>();
        given(amIdentity.getAttribute("retryLimitNodeCount")).willReturn(retryCounts);
        sharedState = json(object());
        node = new RetryLimitDecisionNode(serviceConfig, nodeId, coreWrapper, identityService);

        // when
        var action = node.process(newTreeContext(sharedState));

        // then
        assertThat(action.outcome).isEqualTo("Retry");
        verify(amIdentity).setAttributes(Map.of("retryLimitNodeCount", Set.of(nodeId + "=1")));
        assertThat(sharedState.get(nodeRetryLimitKey(nodeId)).asInteger()).isEqualTo(1);
    }

    @Test
    public void testReturnsRetryOutcomeWhenRetryLimitIsNotInUserProfile()
            throws NodeProcessException, IdRepoException, SSOException {
        // given
        int currentRetryLimit = 1;
        given(serviceConfig.incrementUserAttributeOnFailure()).willReturn(true);
        //noinspection deprecation
        given(coreWrapper.getIdentity(any())).willReturn(amIdentity);
        Set<String> retryCounts = new HashSet<>();
        retryCounts.add(nodeId + "=" + currentRetryLimit);
        given(amIdentity.getAttribute("retryLimitNodeCount")).willReturn(retryCounts);
        sharedState = json(object());
        node = new RetryLimitDecisionNode(serviceConfig, nodeId, coreWrapper, identityService);

        // when
        var action = node.process(newTreeContext(sharedState));

        // then
        assertThat(action.outcome).isEqualTo("Retry");
        verify(amIdentity).setAttributes(Map.of("retryLimitNodeCount", Set.of(nodeId + "=" + (currentRetryLimit + 1))));
        assertThat(sharedState.get(nodeRetryLimitKey(nodeId)).asInteger()).isEqualTo(currentRetryLimit + 1);
    }

    @Test
    public void testReturnsRejectOutcomeWhenUserDoesNotExistButLimitIsInSharedState() throws NodeProcessException {
        // given
        given(serviceConfig.incrementUserAttributeOnFailure()).willReturn(true);
        //noinspection deprecation
        given(coreWrapper.getIdentity(any())).willReturn(null);
        sharedState = json(object(field(nodeRetryLimitKey(nodeId), configRetryLimit)));
        node = new RetryLimitDecisionNode(serviceConfig, nodeId, coreWrapper, identityService);

        // when
        var action = node.process(newTreeContext(sharedState));

        // then
        assertThat(action.outcome).isEqualTo("Reject");
    }

    @Test
    public void testReturnsRetryOutcomeWhenUserDoesNotExistAndNothingInSharedState() throws NodeProcessException {
        // given
        given(serviceConfig.incrementUserAttributeOnFailure()).willReturn(true);
        //noinspection deprecation
        given(coreWrapper.getIdentity(any())).willReturn(null);
        sharedState = json(object());
        node = new RetryLimitDecisionNode(serviceConfig, nodeId, coreWrapper, identityService);

        // when
        var action = node.process(newTreeContext(sharedState));

        // then
        assertThat(action.outcome).isEqualTo("Retry");
        assertThat(sharedState.get(nodeRetryLimitKey(nodeId)).asInteger()).isEqualTo(1);
    }

    @Test
    public void testReturnsRetryOutcomeWhenUserDoesNotExistAndNotMetInSharedState() throws NodeProcessException {
        // given
        int currentRetryLimit = 1;
        given(serviceConfig.incrementUserAttributeOnFailure()).willReturn(true);
        //noinspection deprecation
        given(coreWrapper.getIdentity(any())).willReturn(null);
        sharedState = json(object(field(nodeRetryLimitKey(nodeId), currentRetryLimit)));
        node = new RetryLimitDecisionNode(serviceConfig, nodeId, coreWrapper, identityService);

        // when
        var action = node.process(newTreeContext(sharedState));

        // then
        assertThat(action.outcome).isEqualTo("Retry");
        assertThat(sharedState.get(nodeRetryLimitKey(nodeId)).asInteger()).isEqualTo(currentRetryLimit + 1);
    }

    private String nodeRetryLimitKey(UUID nodeId) {
        return nodeId + "." + RETRY_COUNT;
    }
}

