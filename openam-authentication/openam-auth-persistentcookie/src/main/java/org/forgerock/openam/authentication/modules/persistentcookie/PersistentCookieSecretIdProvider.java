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
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.authentication.modules.persistentcookie;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.secrets.SecretIdProvider;
import org.forgerock.openam.shared.secrets.Labels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.authentication.config.AMAuthenticationManagerFactory;
import com.sun.identity.authentication.config.AMConfigurationException;

/**
 * This provider exposes the secret IDs used by the persistent cookie authentication module instances to the
 * {@link org.forgerock.openam.secrets.config.SecretIdRegistry}.
 */
public class PersistentCookieSecretIdProvider implements SecretIdProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersistentCookieSecretIdProvider.class);
    private static final String COMPONENT_NAME = "Persistent Cookie Authentication Module";
    private static final String PERSISTENT_COOKIE = "PersistentCookie";
    private static final Multimap<String, String> SINGLETON_SECRET_IDS = ImmutableMultimap.<String, String>builder()
                .putAll(COMPONENT_NAME,
                        Labels.DEFAULT_PCOOKIE_SIGNING,
                        Labels.DEFAULT_PCOOKIE_ENCRYPTION)
                .build();
    private final AMAuthenticationManagerFactory amAuthenticationManagerFactory;

    @Inject
    public PersistentCookieSecretIdProvider(AMAuthenticationManagerFactory amAuthenticationManagerFactory) {
        this.amAuthenticationManagerFactory = amAuthenticationManagerFactory;
    }

    @Override
    public Multimap<String, String> getGlobalSingletonSecretIds() {
        return SINGLETON_SECRET_IDS;
    }

    @Override
    public Multimap<String, String> getRealmSingletonSecretIds() {
        return SINGLETON_SECRET_IDS;
    }

    @Override
    public Multimap<String, String> getRealmMultiInstanceSecretIds(SSOToken authorizationToken, Realm realm) {
        Set<String> secretIds = new HashSet<>();
            try {
                AMAuthenticationManager authManager = amAuthenticationManagerFactory.create(authorizationToken, realm);
                Set<String> instanceNames = authManager.getModuleInstanceNames(PERSISTENT_COOKIE);
                for (String instanceName : instanceNames) {
                    secretIds.add(String.format(Labels.CUSTOM_PCOOKIE_SIGNING, instanceName));
                    secretIds.add(String.format(Labels.CUSTOM_PCOOKIE_ENCRYPTION, instanceName));
                }
            } catch (AMConfigurationException amce) {
                LOGGER.error("Unable to retrieve secret IDs for persistent cookie modules in realm {}", realm, amce);
                return ImmutableMultimap.of();
            }

        return ImmutableMultimap.<String, String>builder()
                .putAll("Persistent Cookie Authentication Module", secretIds)
                .build();
    }
}
