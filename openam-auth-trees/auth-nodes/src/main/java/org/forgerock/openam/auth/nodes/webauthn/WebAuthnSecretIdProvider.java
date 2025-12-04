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
 * Copyright 2020-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.secrets.SecretIdProvider;
import org.forgerock.openam.shared.secrets.Labels;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.SMSException;

/**
 * Provides secret ids for instances of the webAuthn registration node's trust stores.
 */
public class WebAuthnSecretIdProvider implements SecretIdProvider {

    private static final Logger logger = LoggerFactory.getLogger(WebAuthnSecretIdProvider.class);

    private final AnnotatedServiceRegistry serviceRegistry;

    /**
     * Generates the secret provider. We require the service registry to be able to
     * easily look up the configurations for all webAuthn registration nodes within a given realm.
     *
     * @param serviceRegistry the annotated service registry
     */
    @Inject
    public WebAuthnSecretIdProvider(AnnotatedServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    public Multimap<String, String> getGlobalSingletonSecretIds() {
        return ImmutableMultimap.<String, String>builder()
                .putAll("FIDO Metadata Service Root Certificate", Labels.FIDO_METADATA_SERVICE_ROOT_CERTIFICATE)
                .build();
    }

    @Override
    public Multimap<String, String> getRealmSingletonSecretIds() {
        return getGlobalSingletonSecretIds();
    }

    @Override
    public Multimap<String, String> getRealmMultiInstanceSecretIds(SSOToken authorizationToken, Realm realm) {
        try {

            Set<WebAuthnRegistrationNode.Config> configs =
                    serviceRegistry.getRealmInstances(WebAuthnRegistrationNode.Config.class, realm);

            Set<String> secretIds =
                    configs.stream()
                            .filter(config -> config.trustStoreAlias().isPresent())
                            .map(config -> config.trustStoreAlias().get())
                            .map(alias -> String.format(Labels.WEBAUTHN_TRUST_STORE, alias))
                            .collect(Collectors.toUnmodifiableSet());

            return ImmutableMultimap.<String, String>builder()
                    .putAll("WebAuthn CA Trust Stores", secretIds)
                    .build();
        } catch (SMSException | SSOException amce) {
            logger.warn("Unable to read realm information for secret source, ", amce);
            return ImmutableMultimap.of();
        }

    }

}
