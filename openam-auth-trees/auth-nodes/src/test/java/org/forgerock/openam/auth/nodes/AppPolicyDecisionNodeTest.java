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

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.auth.nodes.PolicyDecisionNode.Outcome.ACCEPT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.forgerock.openam.auth.node.api.AuthScriptUtilities;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.entitlement.service.PolicySubjectService;
import org.forgerock.openam.scripting.domain.OAuthScriptedBindingObject;
import org.forgerock.openam.scripting.domain.SAMLScriptedBindingObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
public class AppPolicyDecisionNodeTest {

    @Mock
    private AppPolicyDecisionNode.Config config;
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
    private AuthScriptUtilities authScriptUtils;
    @Mock
    private NodeUserIdentityProvider nodeUserIdentityProvider;

    @Mock
    private SSOToken adminToken;
    @Mock
    private PolicyEvaluator policyEvaluator;
    @Mock
    private Subject adminSubject;
    @Mock
    private Subject policySubject;
    @Mock
    private Principal principal;

    private AppPolicyDecisionNode node;

    private static final String POLICY_SET = "customerApplicationPolicySet";
    private static final String APP_ID = "myAppId";
    private static final String REALM_PATH = "/realm";

    @BeforeEach
    public void setup() {
        given(config.getConfig()).willCallRealMethod();
        node = new AppPolicyDecisionNode(config, realm, adminTokenProvider, evaluatorFactory, subjectFactory,
                policySubjectService, nodeUserIdentityProvider, authScriptUtils);
    }

    @Test
    public void shouldReturnAcceptWhenSAMLPolicyAllowsAction() throws Exception {
        // Given
        setupMocks();
        TreeContext context = TreeContextFactory.emptyTreeContext();

        SAMLScriptedBindingObject samlData = mock(SAMLScriptedBindingObject.class);
        given(samlData.getApplicationId()).willReturn(APP_ID);
        given(authScriptUtils.getSAMLData(context)).willReturn(Optional.of(samlData));

        setupEvaluationMocks();

        Entitlement entitlement = new EntitlementImpl();
        Map<String, Boolean> actionValues = new HashMap<>();
        actionValues.put("Access", true);
        entitlement.setActionValues(actionValues);
        given(policyEvaluator.evaluate(eq(REALM_PATH), eq(policySubject), eq(singleton(APP_ID)), any()))
                .willReturn(singletonList(entitlement));

        // When
        Action action = node.process(context);

        // Then
        assertThat(action.outcome).isEqualTo(ACCEPT.name());
    }

    @Test
    public void shouldReturnAcceptWhenOAuthPolicyAllowsAction() throws Exception {
        // Given
        setupMocks();
        TreeContext context = TreeContextFactory.emptyTreeContext();

        given(authScriptUtils.getSAMLData(context)).willReturn(Optional.empty());
        OAuthScriptedBindingObject oauthData = mock(OAuthScriptedBindingObject.class);
        given(oauthData.getApplicationId()).willReturn(APP_ID);
        given(authScriptUtils.getOauthDecisionNodeApplication(context)).willReturn(Optional.of(oauthData));

        setupEvaluationMocks();

        Entitlement entitlement = new EntitlementImpl();
        Map<String, Boolean> actionValues = new HashMap<>();
        actionValues.put("Access", true);
        entitlement.setActionValues(actionValues);
        given(policyEvaluator.evaluate(eq(REALM_PATH), eq(policySubject), eq(singleton(APP_ID)), any()))
                .willReturn(singletonList(entitlement));

        // When
        Action action = node.process(context);

        // Then
        assertThat(action.outcome).isEqualTo(ACCEPT.name());
    }

    @Test
    public void shouldReturnUnknownResourceWhenNoApplicationFound() throws Exception {
        // Given
        TreeContext context = TreeContextFactory.emptyTreeContext();

        given(authScriptUtils.getSAMLData(context)).willReturn(Optional.empty());
        given(authScriptUtils.getOauthDecisionNodeApplication(context)).willReturn(Optional.empty());

        // When
        Action action = node.process(context);

        // Then
        assertThat(action.outcome).isEqualTo("UNKNOWN_RESOURCE");
    }

    private void setupMocks() {
        given(realm.asPath()).willReturn(REALM_PATH);
        given(adminTokenProvider.get()).willReturn(adminToken);
        given(subjectFactory.create(adminToken)).willReturn(adminSubject);
        given(evaluatorFactory.create(adminSubject, POLICY_SET)).willReturn(policyEvaluator);
    }

    private void setupEvaluationMocks() throws Exception {
        Identity identity = mock(Identity.class);
        UniversalId universalId = mock(UniversalId.class);
        given(identity.asUniversalId()).willReturn(universalId);
        given(nodeUserIdentityProvider.getIdentity(eq(realm), any(), any()))
                .willReturn(Optional.of(identity));
        given(policySubjectService.getPolicySubject(any(), any(), any(), any()))
                .willReturn(policySubject);
        given(policySubject.getPrincipals()).willReturn(Set.of(principal));
    }
}
