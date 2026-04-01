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
package com.sun.identity.saml2.plugins.scripted;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.Evaluator;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.plugins.scripted.wrappers.ScriptEntitlementInfoWrapper;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.shared.Constants;

@ExtendWith(MockitoExtension.class)
class IdpAdapterNextGenScriptHelperTest {

    @Mock
    private SSOToken ssoToken;

    @Mock
    private AuthnRequest authnRequest;

    @Mock
    private Issuer issuer;

    @Mock
    private Evaluator evaluator;

    @Mock
    private Entitlement entitlement;

    @Spy
    @InjectMocks
    private IdpAdapterNextGenScriptHelper helper;

    @Test
    void shouldReturnNullEntitlementsWhenSsoTokenMissing() throws Exception {
        // Given
        IdpAdapterNextGenScriptHelper helperWithNullToken =
                new IdpAdapterNextGenScriptHelper(null, authnRequest);

        // When
        List<ScriptEntitlementInfoWrapper> result = helperWithNullToken.getEntitlements("app", "realm");

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullEntitlementsWhenAuthnRequestMissing() throws Exception {
        // Given
        IdpAdapterNextGenScriptHelper helperWithNullRequest =
                new IdpAdapterNextGenScriptHelper(ssoToken, null);

        // When
        List<ScriptEntitlementInfoWrapper> result = helperWithNullRequest.getEntitlements("app", "realm");

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnEntitlementsAndConvertEnvironment() throws Exception {
        // Given
        given(ssoToken.getProperty(Constants.UNIVERSAL_IDENTIFIER)).willReturn("user-123");
        given(authnRequest.getIssuer()).willReturn(issuer);
        given(issuer.getValue()).willReturn("issuer");

        given(entitlement.getApplicationName()).willReturn("app");
        List<Entitlement> entitlements = List.of(entitlement);

        Map<String, List<String>> environment = new HashMap<>();
        environment.put("alpha", List.of("one", "two"));
        environment.put("empty", null);

        given(evaluator.evaluate(eq("realm"), any(Subject.class), eq(Set.of("issuer")), anyMap()))
                .willReturn(entitlements);
        willReturn(evaluator).given(helper).getEvaluatorForSubject(eq("app"), any(Subject.class));

        // When
        List<ScriptEntitlementInfoWrapper> result = helper.getEntitlements("app", "realm", environment);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getApplicationName()).isEqualTo("app");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Set<String>>> envCaptor = ArgumentCaptor.forClass(Map.class);
        then(evaluator).should().evaluate(eq("realm"), any(Subject.class), eq(Set.of("issuer")), envCaptor.capture());

        Map<String, Set<String>> captured = envCaptor.getValue();
        assertThat(captured.get("alpha")).containsExactlyInAnyOrder("one", "two");
        assertThat(captured.get("empty")).isEmpty();
    }
}
