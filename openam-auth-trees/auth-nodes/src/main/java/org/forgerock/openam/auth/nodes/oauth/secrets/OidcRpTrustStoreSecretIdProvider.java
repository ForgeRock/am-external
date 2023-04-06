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
 * Copyright 2022 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.oauth.secrets;

import static org.forgerock.openam.secrets.SecretsUtils.sanitisePurposeLabel;
import static org.forgerock.openam.shared.secrets.Labels.OIDC_RELIANT_PARTY_CONFIG_TRUST_STORE;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.secrets.SecretIdProvider;
import org.forgerock.openam.social.idp.OAuth2ClientConfig;
import org.forgerock.openam.social.idp.SocialIdentityProviders;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.iplanet.sso.SSOToken;

/**
 * This provider exposes secret Ids for the instances of OAuth2/OIDC reliant party trust stores.
 */
public class OidcRpTrustStoreSecretIdProvider implements SecretIdProvider {

    private static final String SECRET_ID_KEY = "OIDC Reliant Party CA Trust Stores";

    private final SocialIdentityProviders providerConfigStore;

    /**
     * Constructor.
     * @param providerConfigStore service containing social provider configurations
     */
    @Inject
    public OidcRpTrustStoreSecretIdProvider(SocialIdentityProviders providerConfigStore) {
        this.providerConfigStore = providerConfigStore;
    }

    @Override
    public Multimap<String, String> getRealmMultiInstanceSecretIds(SSOToken token, Realm realm) {
        return ImmutableMultimap.<String, String>builder()
                .putAll(SECRET_ID_KEY, getSecretIds(realm))
                .build();
    }

    private Set<String> getSecretIds(Realm realm) {
        return providerConfigStore.getProviders(realm).values().stream()
                .filter(OAuth2ClientConfig.class::isInstance)
                .map(OAuth2ClientConfig.class::cast)
                .filter(OAuth2ClientConfig::useCustomTrustStore)
                .map(OAuth2ClientConfig::provider)
                .map(config -> sanitisePurposeLabel(String.format(OIDC_RELIANT_PARTY_CONFIG_TRUST_STORE, config)))
                .collect(Collectors.toSet());
    }
}
