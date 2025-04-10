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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.x509;

import static com.sun.identity.idm.IdConstants.REPO_SERVICE;
import static com.sun.identity.shared.Constants.ORGANIZATION_NAME;
import static com.sun.identity.shared.Constants.SSO_TOKEN;
import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.inject.Inject;

import org.forgerock.am.config.ChoiceValues;
import org.forgerock.openam.sm.ServiceConfigManagerFactory;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;

/**
 * A {@link ChoiceValues} implementation that returns the names of configured LDAP identity stores.
 */
public class IdentityStoreChoiceValues implements ChoiceValues {

    private static final Logger logger = LoggerFactory.getLogger(IdentityStoreChoiceValues.class);

    private final ServiceConfigManagerFactory serviceConfigManagerFactory;

    /**
     * Creates a new instance of the {@link IdentityStoreChoiceValues} class.
     * @param serviceConfigManagerFactory the {@link ServiceConfigManagerFactory} to create the
     * {@link ServiceConfigManager} for the identity repository service
     */
    @Inject
    public IdentityStoreChoiceValues(ServiceConfigManagerFactory serviceConfigManagerFactory) {
        this.serviceConfigManagerFactory = serviceConfigManagerFactory;
    }

    @Override
    public Map<String, String> getChoiceValues() {
        return getChoiceValues(Collections.emptyMap());
    }

    @Override
    public Map<String, String> getChoiceValues(Map<String, Object> envParams) {
        String realmName = StringUtils.ifNullOrEmpty((String) envParams.get(ORGANIZATION_NAME), "/");
        Map<String, String> result = new HashMap<>(Map.of(ISAuthConstants.BLANK, ISAuthConstants.BLANK));
        try {
            Set<String> stores = getIdentityStoreNames((SSOToken) envParams.get(SSO_TOKEN), realmName);
            result.putAll(stores.stream().collect(toMap(Function.identity(), Function.identity())));
        } catch (SMSException | SSOException e) {
            logger.warn("Failed to get identity store names", e);
        }
        return result;
    }

    private Set<String> getIdentityStoreNames(SSOToken adminToken, String realmName) throws SMSException, SSOException {
        ServiceConfigManager configManager = adminToken == null
            ? serviceConfigManagerFactory.create(REPO_SERVICE)
            : serviceConfigManagerFactory.create(REPO_SERVICE, adminToken);
        ServiceConfig config = configManager.getOrganizationConfig(realmName, null);
        return config.getSubConfigNames();
    }
}
