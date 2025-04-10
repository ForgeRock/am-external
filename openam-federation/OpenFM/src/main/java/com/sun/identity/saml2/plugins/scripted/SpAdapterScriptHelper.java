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
 * Copyright 2023-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package com.sun.identity.saml2.plugins.scripted;

import static com.sun.identity.saml2.common.SAML2Constants.SP_ADAPTER_ENV;
import static com.sun.identity.saml2.common.SAML2Constants.SP_ROLE;
import static com.sun.identity.saml2.common.SAML2Utils.getAllAttributeValueFromSSOConfig;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.openam.annotations.EvolvingAll;
import org.forgerock.openam.saml2.plugins.InitializablePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides helper functions for SP Adapter Script Implementations.
 */
@EvolvingAll
@Singleton
public class SpAdapterScriptHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpAdapterScriptHelper.class);

    /**
     * Returns a map of the SP Adapter environment.
     *
     * @param realm realm of hosted entity
     * @param spEntityId name of hosted SP entity
     * @return a map of the SP Adapter Environment
     */
    public Map<String, String> getSpAdapterEnv(String realm, String spEntityId) {
        List<String> adapterEnv = getAllAttributeValueFromSSOConfig(realm, spEntityId, SP_ROLE, SP_ADAPTER_ENV);
        Map<String, String> adapterEnvMap = parseEnvList(adapterEnv);
        adapterEnvMap.put(InitializablePlugin.HOSTED_ENTITY_ID, spEntityId);
        adapterEnvMap.put(InitializablePlugin.REALM, realm);
        return adapterEnvMap;
    }

    private static Map<String, String> parseEnvList(List<String> list) {
        if (isEmpty(list)) {
            return new HashMap<>();
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SpAdapterScriptHelper.parseEnvList : processing {}", list);
        }
        return list.stream()
                .filter(StringUtils::isNotBlank)
                .filter(val -> val.contains("="))
                .map(val -> val.split("="))
                .collect(Collectors.toMap(parts -> parts[0], parts -> parts[1]));
    }
}
