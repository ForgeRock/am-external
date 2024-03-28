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
 * Copyright 2021-2023 ForgeRock AS.
 */
package com.sun.identity.saml2.plugins.scripted;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstructionWithAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.security.auth.Subject;

import org.forgerock.openam.entitlement.monitoring.EntitlementConfigurationWrapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Evaluator;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.protocol.AuthnRequest;

/**
 * Tests for {@link IdpAdapterScriptHelper}.
 */
@RunWith(MockitoJUnitRunner.class)
public class IdpAdapterScriptHelperTest {

    private static final List<Entitlement> ENTITLEMENTS = Collections.singletonList(mock(Entitlement.class));

    @Mock
    private AuthnRequest authnRequest;
    @Mock
    private Issuer issuer;
    @Mock
    private SSOToken ssoToken;
    @Mock
    private Evaluator evaluator;

    @InjectMocks
    private IdpAdapterScriptHelper idpAdapterScriptHelper;

    private final Object ssoTokenObj = mock(SSOToken.class);
    private final Subject subject = new Subject();
    private final String issuerValue = "testIssuer";
    private final String applicationName = "saml";
    private final String realm = "testRealm";

    private MockedStatic<SubjectUtils> mockSubjectUtils;
    private MockedConstruction<EntitlementConfigurationWrapper> ignoredEntitlementConfigurationWrapper;
    private MockedConstruction<Evaluator> ignoredEvaluator;

    @Before
    public void setup() throws Exception {
        when(authnRequest.getIssuer()).thenReturn(issuer);
        when(issuer.getValue()).thenReturn(issuerValue);
        mockSubjectUtils = mockStatic(SubjectUtils.class);
        mockSubjectUtils.when(() -> SubjectUtils.createSubject(ssoToken)).thenReturn(subject);
        mockSubjectUtils.when(() -> SubjectUtils.createSubject((SSOToken) ssoTokenObj)).thenReturn(subject);

        ignoredEntitlementConfigurationWrapper = mockConstructionWithAnswer(EntitlementConfigurationWrapper.class,
                invocation -> mock(EntitlementConfigurationWrapper.class));

        ignoredEvaluator = mockConstructionWithAnswer(Evaluator.class, invocation -> {
            if (invocation.getMethod().equals(Evaluator.class.getMethod("evaluate", String.class, Subject.class,
                    Set.class, Map.class))) {
                return ENTITLEMENTS;
            }
            return evaluator;
        });
    }

    @After
    public void tearDown() {
        mockSubjectUtils.close();
        ignoredEvaluator.close();
        ignoredEntitlementConfigurationWrapper.close();
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
        assertThat(evaluatorForSubject).usingRecursiveComparison().isEqualTo(evaluator);
    }

    @Test
    public void testGetEntitlementsForToken() throws EntitlementException {
        // Given
        List<Entitlement> mockedEntitlements = ENTITLEMENTS;

        // When
        List<ScriptEntitlementInfo> entitlementsForToken = idpAdapterScriptHelper.getEntitlements(applicationName,
                realm, ssoTokenObj, authnRequest);

        // Then
        List<ScriptEntitlementInfo> expected = convertToScriptEntitlementInfoList(mockedEntitlements);
        assertThat(entitlementsForToken).isEqualTo(expected);
    }

    @Test
    public void testGetEntitlementsWithEnvironmentParameters() throws EntitlementException {
        // Given
        Map<String, Set<String>> environmentParams = Map.of("key1", Set.of("value1"));
        List<Entitlement> mockedEntitlements = ENTITLEMENTS;

        // When
        List<ScriptEntitlementInfo> entitlementsFromEnvParams = idpAdapterScriptHelper.getEntitlements(applicationName,
                realm, subject, Collections.singleton(issuerValue), environmentParams);

        // Then
        assertThat(entitlementsFromEnvParams).isEqualTo(convertToScriptEntitlementInfoList(mockedEntitlements));
    }

    private List<ScriptEntitlementInfo> convertToScriptEntitlementInfoList(List<Entitlement> entitlements) {
        return entitlements.stream().map(ScriptEntitlementInfo::new).collect(Collectors.toList());
    }
}
