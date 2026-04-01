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
 * Copyright 2025-2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.ad;

import static org.forgerock.openam.shared.secrets.Labels.AD_DECISION_NODE_MTLS_CERT;

import java.util.function.Function;

import org.forgerock.openam.auth.nodes.ADDecisionNode;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.secrets.SecretIdProvider;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.forgerock.util.Reject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.SMSException;

import jakarta.inject.Inject;

/**
 * This provider exposes the secret IDs used by AD Nodes.
 */
public class ADDecisionNodeSecretIdProvider implements SecretIdProvider {

    private static final String AD_NODE_KEY = "ADNode";
    private static final Logger logger = LoggerFactory.getLogger(ADDecisionNodeSecretIdProvider.class);

    private final AnnotatedServiceRegistry serviceRegistry;

    /**
     * Constructor for the ADDecisionNodeSecretIdProvider.
     *
     * @param serviceRegistry The AnnotatedServiceRegistry.
     */
    @Inject
    public ADDecisionNodeSecretIdProvider(AnnotatedServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    public Multimap<String, String> getRealmMultiInstanceSecretIds(SSOToken authorizationToken, Realm realm) {
        Reject.ifNull(realm);
        try {
            return serviceRegistry.getRealmInstances(ADDecisionNode.Config.class, realm).stream()
                    .filter(ADDecisionNode.Config::mtlsEnabled)
                    .filter(config -> config.mtlsSecretLabel().isPresent())
                    .map(config -> String.format(AD_DECISION_NODE_MTLS_CERT, config.mtlsSecretLabel().get()))
                    .collect(ImmutableSetMultimap.toImmutableSetMultimap(k -> AD_NODE_KEY, Function.identity()));
        } catch (SSOException | SMSException e) {
            logger.error("Failed to get AD node secret labels", e);
        }

        return ImmutableMultimap.of();
    }
}
