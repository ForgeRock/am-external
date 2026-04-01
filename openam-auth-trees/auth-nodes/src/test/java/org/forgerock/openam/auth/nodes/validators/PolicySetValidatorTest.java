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
package org.forgerock.openam.auth.nodes.validators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;

import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.entitlement.service.ApplicationService;
import org.forgerock.openam.entitlement.service.ApplicationServiceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iplanet.sso.SSOToken;
import com.sun.identity.delegation.DelegationManager;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.opensso.SubjectFactory;

@ExtendWith(MockitoExtension.class)
public class PolicySetValidatorTest {

    @Mock
    private ApplicationServiceFactory applicationServiceFactory;
    @Mock
    private SubjectFactory subjectFactory;
    @Mock
    private CoreWrapper coreWrapper;
    @Mock
    private ApplicationService applicationService;
    @Mock
    private SSOToken adminToken;

    private PolicySetValidator validator;

    @BeforeEach
    public void setup() {
        validator = new PolicySetValidator(applicationServiceFactory, subjectFactory, coreWrapper);
    }

    @Test
    public void validateWithoutOrgNameReturnsFalse() {
        assertThat(validator.validate(Collections.emptySet())).isFalse();
    }

    @Test
    public void validateWithValidPolicySets() throws EntitlementException {
        String orgName = "testOrg";
        Set<String> policySets = new HashSet<>(Arrays.asList("policy1", "policy2"));

        given(coreWrapper.getAdminToken()).willReturn(adminToken);
        Subject subject = mock(Subject.class);
        given(subjectFactory.create(adminToken)).willReturn(subject);
        given(applicationServiceFactory.create(subject, orgName)).willReturn(applicationService);

        Application app1 = mock(Application.class);
        given(app1.getName()).willReturn("policy1");
        Application app2 = mock(Application.class);
        given(app2.getName()).willReturn("policy2");
        Application app3 = mock(Application.class);
        given(app3.getName()).willReturn("policy3");

        given(applicationService.getApplications()).willReturn(new HashSet<>(Arrays.asList(app1, app2, app3)));

        assertThat(validator.validate(policySets, orgName)).isTrue();
    }

    @Test
    public void validateWithInvalidPolicySets() throws EntitlementException {
        String orgName = "testOrg";
        Set<String> policySets = new HashSet<>(Arrays.asList("policy1", "invalidPolicy"));

        given(coreWrapper.getAdminToken()).willReturn(adminToken);
        Subject subject = mock(Subject.class);
        given(subjectFactory.create(adminToken)).willReturn(subject);
        given(applicationServiceFactory.create(subject, orgName)).willReturn(applicationService);

        Application app1 = mock(Application.class);
        given(app1.getName()).willReturn("policy1");

        given(applicationService.getApplications()).willReturn(new HashSet<>(Collections.singletonList(app1)));

        assertThat(validator.validate(policySets, orgName)).isFalse();
    }

    @Test
    public void validateFiltersDelegationService() throws EntitlementException {
        String orgName = "testOrg";
        // Attempting to validate the delegation service should fail as it is filtered out
        Set<String> policySets = new HashSet<>(Collections.singletonList(DelegationManager.DELEGATION_SERVICE));

        given(coreWrapper.getAdminToken()).willReturn(adminToken);
        Subject subject = mock(Subject.class);
        given(subjectFactory.create(adminToken)).willReturn(subject);
        given(applicationServiceFactory.create(subject, orgName)).willReturn(applicationService);

        Application app1 = mock(Application.class);
        given(app1.getName()).willReturn(DelegationManager.DELEGATION_SERVICE);
        Application app2 = mock(Application.class);
        given(app2.getName()).willReturn("policy1");

        given(applicationService.getApplications()).willReturn(new HashSet<>(Arrays.asList(app1, app2)));

        assertThat(validator.validate(policySets, orgName)).isFalse();
    }

    @Test
    public void validateFiltersSunAMDelegationService() throws EntitlementException {
        String orgName = "testOrg";
        // Attempting to validate the sunAMDelegationService should fail as it is filtered out
        Set<String> policySets = new HashSet<>(Collections.singletonList("sunAMDelegationService"));

        given(coreWrapper.getAdminToken()).willReturn(adminToken);
        Subject subject = mock(Subject.class);
        given(subjectFactory.create(adminToken)).willReturn(subject);
        given(applicationServiceFactory.create(subject, orgName)).willReturn(applicationService);

        Application app1 = mock(Application.class);
        given(app1.getName()).willReturn("sunAMDelegationService");
        Application app2 = mock(Application.class);
        given(app2.getName()).willReturn("policy1");

        given(applicationService.getApplications()).willReturn(new HashSet<>(Arrays.asList(app1, app2)));

        assertThat(validator.validate(policySets, orgName)).isFalse();
    }

    @Test
    public void validateReturnsFalseOnEntitlementException() throws EntitlementException {
        String orgName = "testOrg";
        Set<String> policySets = new HashSet<>(Collections.singletonList("policy1"));

        given(coreWrapper.getAdminToken()).willReturn(adminToken);
        Subject subject = mock(Subject.class);
        given(subjectFactory.create(adminToken)).willReturn(subject);
        given(applicationServiceFactory.create(subject, orgName)).willReturn(applicationService);
        given(applicationService.getApplications()).willThrow(mock(EntitlementException.class));

        assertThat(validator.validate(policySets, orgName)).isFalse();
    }
}
