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

import static java.util.stream.Collectors.toSet;

import java.util.Set;

import jakarta.inject.Inject;

import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.entitlement.service.ApplicationServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.delegation.DelegationManager;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.opensso.SubjectFactory;
import com.sun.identity.sm.ServiceAttributeValidator;

/**
 * Validates that the configured policy set(s) exist in the system. The internal "sunAMDelegationService" policy
 * set is never valid.
 */
public class PolicySetValidator implements ServiceAttributeValidator {

    private static final Logger logger = LoggerFactory.getLogger(PolicySetValidator.class);
    private final ApplicationServiceFactory applicationServiceFactory;
    private final SubjectFactory subjectFactory;
    private final CoreWrapper coreWrapper;

    @Inject
    PolicySetValidator(ApplicationServiceFactory applicationServiceFactory, SubjectFactory subjectFactory,
            CoreWrapper coreWrapper) {
        this.applicationServiceFactory = applicationServiceFactory;
        this.subjectFactory = subjectFactory;
        this.coreWrapper = coreWrapper;
    }

    @Override
    public boolean validate(Set<String> values) {
        return false;
    }

    @Override
    public boolean validate(Set<String> values, String orgName) {
        var subject = subjectFactory.create(coreWrapper.getAdminToken());
        try {
            var appNames = applicationServiceFactory.create(subject, orgName).getApplications().stream()
                                   .map(Application::getName)
                                   .filter(appName -> !DelegationManager.DELEGATION_SERVICE.equals(appName))
                                   .collect(toSet());
            return appNames.containsAll(values);
        } catch (EntitlementException e) {
            logger.error("Error retrieving applications for organization {}: {}", orgName, e.getMessage(), e);
            return false;
        }
    }
}
