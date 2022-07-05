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
 * Copyright 2021-2022 ForgeRock AS.
 */
package com.sun.identity.saml2.plugins.scripted;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.security.auth.Subject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Evaluator;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.protocol.AuthnRequest;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SubjectUtils.class, IdpAdapterScriptHelper.class})
public class IdpAdapterScriptHelperTest {
    private final IdpAdapterScriptHelper idpAdapterScriptHelper = new IdpAdapterScriptHelper();

    @Mock
    private AuthnRequest authnRequest;
    @Mock
    private Issuer issuer;
    @Mock
    private SSOToken ssoToken;
    @Mock
    private Evaluator evaluator;

    private final Object ssoTokenObj = mock(SSOToken.class);
    private final Subject subject = new Subject();
    private final String issuerValue = "testIssuer";
    private final String applicationName = "saml";
    private final String realm = "testRealm";

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.openMocks(this).close();
        when(authnRequest.getIssuer()).thenReturn(issuer);
        when(issuer.getValue()).thenReturn(issuerValue);
        mockStatic(SubjectUtils.class);
        when(SubjectUtils.createSubject(ssoToken)).thenReturn(subject);
        when(SubjectUtils.createSubject((SSOToken) ssoTokenObj)).thenReturn(subject);
        whenNew(Evaluator.class).withArguments(subject, applicationName).thenReturn(evaluator);
    }

    @Test
    public void testGetIssuer() {
        // When
        String issuerName = idpAdapterScriptHelper.getIssuerName(authnRequest);

        // Then
        assertThat(issuerName).isEqualTo(issuerValue);
    }

    @Test
    public void testGetResourcesForToken() {
        // When
        Set<String> resources = idpAdapterScriptHelper.getResourcesForToken(authnRequest);

        // Then
        assertThat(resources).isEqualTo(Collections.singleton(issuerValue));
    }

    @Test
    public void testGetSubjectForToken() {
        // When
        Subject subjectForToken = idpAdapterScriptHelper.getSubjectForToken(ssoToken);

        // Then
        assertThat(subjectForToken).isEqualTo(subject);
    }

    @Test
    public void testGetSubjectForTokenObject() {
        // When
        Subject subjectForToken = idpAdapterScriptHelper.getSubjectForToken(ssoTokenObj);

        // Then
        assertThat(subjectForToken).isEqualTo(subject);
    }

    @Test
    public void testGetEvaluatorForSubject() {
        // When
        Evaluator evaluatorForSubject = idpAdapterScriptHelper.getEvaluatorForSubject(applicationName, subject);

        // Then
        assertThat(evaluatorForSubject).isEqualTo(evaluator);
    }

    @Test
    public void testGetEntitlementsForToken() throws EntitlementException {
        // Given
        List<Entitlement> entitlements = List.of(mock(Entitlement.class), mock(Entitlement.class));
        when(evaluator.evaluate(realm, subject, Collections.singleton(issuerValue), Collections.emptyMap()))
                .thenReturn(entitlements);

        // When
        List<ScriptEntitlementInfo> entitlementsForToken = idpAdapterScriptHelper.getEntitlements(applicationName,
                realm, ssoTokenObj, authnRequest);

        // Then
        List<ScriptEntitlementInfo> expected = convertToScriptEntitlementInfoList(entitlements);
        assertThat(entitlementsForToken).isEqualTo(expected);
    }

    @Test
    public void testGetEntitlementsWithEnvironmentParameters() throws EntitlementException {
        // Given
        Map<String, Set<String>> environmentParams = Map.of("key1", Set.of("value1"));
        List<Entitlement> entitlements = Collections.singletonList(mock(Entitlement.class));
        when(evaluator.evaluate(realm, subject, Collections.singleton(issuerValue), environmentParams))
                .thenReturn(entitlements);

        // When
        List<ScriptEntitlementInfo> entitlementsFromEnvParams = idpAdapterScriptHelper.getEntitlements(applicationName,
                realm, subject, Collections.singleton(issuerValue), environmentParams);

        // Then
        assertThat(entitlementsFromEnvParams).isEqualTo(convertToScriptEntitlementInfoList(entitlements));
    }

    private List<ScriptEntitlementInfo> convertToScriptEntitlementInfoList(List<Entitlement> entitlements) {
        return entitlements.stream().map(ScriptEntitlementInfo::new).collect(Collectors.toList());
    }
}
