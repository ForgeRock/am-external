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
 * Copyright 2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.auth.nodes.PolicyDecisionNode.Outcome.ACCEPT;
import static org.forgerock.openam.auth.nodes.PolicyDecisionNode.Outcome.ERROR;
import static org.forgerock.openam.auth.nodes.PolicyDecisionNode.Outcome.REJECT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.security.auth.Subject;

import org.forgerock.am.identity.domain.Identity;
import org.forgerock.am.identity.domain.UniversalId;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.entitlement.service.PolicySubjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.inject.Provider;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementImpl;
import com.sun.identity.entitlement.PolicyEvaluator;
import com.sun.identity.entitlement.PolicyEvaluatorFactory;
import com.sun.identity.entitlement.opensso.SubjectFactory;

@ExtendWith(MockitoExtension.class)
public class PolicyDecisionNodeTest {

    @Mock
    private PolicyDecisionNode.Config config;
    @Mock
    private Realm realm;
    @Mock
    private Provider<SSOToken> adminTokenProvider;
    @Mock
    private PolicyEvaluatorFactory evaluatorFactory;
    @Mock
    private SubjectFactory subjectFactory;
    @Mock
    private PolicySubjectService policySubjectService;
    @Mock
    private SSOToken adminToken;
    @Mock
    private PolicyEvaluator policyEvaluator;
    @Mock
    private Subject adminSubject;
    @Mock
    private Subject policySubject;
    @Mock
    private NodeUserIdentityProvider nodeUserIdentityProvider;
    @Mock
    private Principal principal;

    @InjectMocks
    private PolicyDecisionNode node;

    private static final String POLICY_SET = "policySet";
    private static final String RESOURCE = "resource";
    private static final String ACTION = "GET";
    private static final String REALM_PATH = "/realm";

    @Test
    public void shouldReturnAcceptWhenPolicyAllowsAction() throws Exception {
        // Given
        setupMocks();

        TreeContext context = TreeContextFactory.emptyTreeContext();
        Identity identity = mock(Identity.class);
        UniversalId universalId = mock(UniversalId.class);
        given(identity.asUniversalId()).willReturn(universalId);
        given(nodeUserIdentityProvider.getIdentity(eq(realm), any(), any()))
                .willReturn(Optional.of(identity));
        given(policySubjectService.getPolicySubject(any(), any(), any(), any()))
                .willReturn(policySubject);
        given(policySubject.getPrincipals()).willReturn(Set.of(principal));

        Entitlement entitlement = new EntitlementImpl();
        Map<String, Boolean> actionValues = new HashMap<>();
        actionValues.put(ACTION, true);
        entitlement.setActionValues(actionValues);
        given(policyEvaluator.evaluate(eq(REALM_PATH), eq(policySubject), eq(singleton(RESOURCE)), any()))
                .willReturn(singletonList(entitlement));

        // When
        Action action = node.process(context);

        // Then
        assertThat(action.outcome).isEqualTo(ACCEPT.name());
    }

    private void setupMocks() {
        given(config.policySet()).willReturn(POLICY_SET);
        given(config.resource()).willReturn(RESOURCE);
        given(config.action()).willReturn(ACTION);
        given(config.environment()).willReturn(emptyMap());
        given(realm.asPath()).willReturn(REALM_PATH);
        given(adminTokenProvider.get()).willReturn(adminToken);
        given(subjectFactory.create(adminToken)).willReturn(adminSubject);
        given(evaluatorFactory.create(adminSubject, POLICY_SET)).willReturn(policyEvaluator);
    }

    @Test
    public void shouldReturnRejectWhenPolicyDeniesAction() throws Exception {
        // Given
        setupMocks();

        TreeContext context = TreeContextFactory.emptyTreeContext();
        Identity identity = mock(Identity.class);
        UniversalId universalId = mock(UniversalId.class);
        given(identity.asUniversalId()).willReturn(universalId);
        given(nodeUserIdentityProvider.getIdentity(eq(realm), any(), any()))
                .willReturn(Optional.of(identity));
        given(policySubjectService.getPolicySubject(any(), any(), any(), any()))
                .willReturn(policySubject);
        given(policySubject.getPrincipals()).willReturn(Set.of(principal));

        Entitlement entitlement = new EntitlementImpl();
        Map<String, Boolean> actionValues = new HashMap<>();
        actionValues.put(ACTION, false);
        entitlement.setActionValues(actionValues);
        given(policyEvaluator.evaluate(eq(REALM_PATH), eq(policySubject), eq(singleton(RESOURCE)), any()))
                .willReturn(singletonList(entitlement));

        // When
        Action action = node.process(context);

        // Then
        assertThat(action.outcome).isEqualTo(REJECT.name());
    }

    @Test
    public void shouldEvaluatePolicyEvenWhenSubjectIsNull() throws Exception {
        // Given
        setupMocks();

        TreeContext context = TreeContextFactory.emptyTreeContext();
        // User not found
        given(nodeUserIdentityProvider.getIdentity(eq(realm), any(), any()))
                .willReturn(Optional.empty());
        // PolicySubjectService returns null for empty subject
        given(policySubjectService.getPolicySubject(any(), any(), any(), any()))
                .willReturn(null);

        Entitlement entitlement = new EntitlementImpl();
        Map<String, Boolean> actionValues = new HashMap<>();
        actionValues.put(ACTION, true);
        entitlement.setActionValues(actionValues);

        // Evaluator is called with null subject
        given(policyEvaluator.evaluate(eq(REALM_PATH), isNull(), eq(singleton(RESOURCE)), any()))
                .willReturn(singletonList(entitlement));

        // When
        Action action = node.process(context);

        // Then
        assertThat(action.outcome).isEqualTo(ACCEPT.name());
    }

    @Test
    public void shouldReturnErrorWhenExceptionOccurs() throws Exception {
        // Given
        given(config.policySet()).willReturn(POLICY_SET);
        given(config.resource()).willReturn(RESOURCE);

        TreeContext context = TreeContextFactory.emptyTreeContext();

        given(nodeUserIdentityProvider.getIdentity(eq(realm), any(), any()))
                .willThrow(new RuntimeException("Error"));

        // When
        Action action = node.process(context);

        // Then
        assertThat(action.outcome).isEqualTo(ERROR.name());
    }
}