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
package org.forgerock.openam.auth.nodes.adapters;

import static com.sun.identity.shared.Constants.ORGANIZATION_NAME;
import static com.sun.identity.shared.Constants.SSO_TOKEN;
import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.Map;

import javax.security.auth.Subject;

import jakarta.inject.Inject;

import org.forgerock.am.config.ChoiceValues;
import org.forgerock.openam.entitlement.service.ApplicationService;
import org.forgerock.openam.entitlement.service.ApplicationServiceFactory;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.sso.SSOToken;
import com.sun.identity.delegation.DelegationManager;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.opensso.SubjectFactory;

/**
 * A {@link ChoiceValues} implementation that returns the names of configured Policy Sets (Applications).
 */
public class PolicySetChoiceValues implements ChoiceValues {

    private static final Logger logger = LoggerFactory.getLogger(PolicySetChoiceValues.class);

    private final ApplicationServiceFactory applicationServiceFactory;
    private final SubjectFactory subjectFactory;

    /**
     * Creates a new instance of {@link PolicySetChoiceValues}.
     *
     * @param applicationServiceFactory the factory to create ApplicationService.
     * @param subjectFactory            the factory to create Subject from SSOToken.
     */
    @Inject
    PolicySetChoiceValues(ApplicationServiceFactory applicationServiceFactory, SubjectFactory subjectFactory) {
        this.applicationServiceFactory = applicationServiceFactory;
        this.subjectFactory = subjectFactory;
    }

    @Override
    public Map<String, String> getChoiceValues() {
        return getChoiceValues(Collections.emptyMap());
    }

    @Override
    public Map<String, String> getChoiceValues(Map<String, Object> envParams) {
        String realm = StringUtils.ifNullOrEmpty((String) envParams.get(ORGANIZATION_NAME), "/");
        SSOToken ssoToken = (SSOToken) envParams.get(SSO_TOKEN);

        if (ssoToken == null) {
            logger.debug("No SSOToken provided, returning empty choices.");
            return Collections.emptyMap();
        }

        try {
            Subject subject = subjectFactory.create(ssoToken);
            ApplicationService applicationService = applicationServiceFactory.create(subject, realm);
            return applicationService.getApplications().stream()
                           // Exclude the sunAMDelegationService application as it is an internal policy set
                           .filter(app -> !DelegationManager.DELEGATION_SERVICE.equals(app.getName()))
                    .collect(toMap(Application::getName, Application::getDisplayName));
        } catch (Exception e) {
            logger.warn("Failed to retrieve policy sets", e);
            return Collections.emptyMap();
        }
    }
}
