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
 * Copyright 2019-2020 ForgeRock AS.
 */
package org.forgerock.openam.saml2.secrets;

import static com.sun.identity.saml2.common.SAML2Constants.SECRET_ID_IDENTIFIER;
import static com.sun.identity.saml2.common.SAML2Utils.getAttributeValueFromSSOConfig;
import static org.forgerock.openam.shared.secrets.Labels.SAML2_CLIENT_STORAGE_JWT_ENCRYPTION;
import static org.forgerock.openam.shared.secrets.Labels.SAML2_DEFAULT_IDP_ENCRYPTION;
import static org.forgerock.openam.shared.secrets.Labels.SAML2_DEFAULT_IDP_SIGNING;
import static org.forgerock.openam.shared.secrets.Labels.SAML2_DEFAULT_SP_ENCRYPTION;
import static org.forgerock.openam.shared.secrets.Labels.SAML2_DEFAULT_SP_SIGNING;
import static org.forgerock.openam.shared.secrets.Labels.SAML2_ENTITY_ROLE_ENCRYPTION;
import static org.forgerock.openam.shared.secrets.Labels.SAML2_ENTITY_ROLE_SIGNING;
import static org.forgerock.openam.shared.secrets.Labels.SAML2_METADATA_SIGNING_RSA;
import static org.forgerock.openam.utils.StringUtils.isNotBlank;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.saml2.Saml2EntityRole;
import org.forgerock.openam.secrets.SecretIdProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.iplanet.sso.SSOToken;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;

/**
 * This provider exposes the secret IDs used by the SAML2 component to the
 * {@link org.forgerock.openam.secrets.config.SecretIdRegistry}.
 */
public class Saml2SecretIdProvider implements SecretIdProvider {

    private static final Logger logger = LoggerFactory.getLogger(Saml2SecretIdProvider.class);
    private static final String SAML2_KEY = "Saml2";

    @Override
    public Multimap<String, String> getGlobalSingletonSecretIds() {
        return ImmutableMultimap.<String, String>builder()
                .putAll(SAML2_KEY,
                        SAML2_CLIENT_STORAGE_JWT_ENCRYPTION,
                        SAML2_METADATA_SIGNING_RSA,
                        SAML2_DEFAULT_IDP_SIGNING,
                        SAML2_DEFAULT_IDP_ENCRYPTION,
                        SAML2_DEFAULT_SP_SIGNING,
                        SAML2_DEFAULT_SP_ENCRYPTION)
                .build();
    }

    @Override
    public Multimap<String, String> getRealmSingletonSecretIds() {
        return ImmutableMultimap.<String, String>builder()
                .putAll(SAML2_KEY,
                        SAML2_METADATA_SIGNING_RSA,
                        SAML2_DEFAULT_IDP_SIGNING,
                        SAML2_DEFAULT_IDP_ENCRYPTION,
                        SAML2_DEFAULT_SP_SIGNING,
                        SAML2_DEFAULT_SP_ENCRYPTION)
                .build();
    }

    @Override
    public Multimap<String, String> getRealmMultiInstanceSecretIds(SSOToken token, Realm realm) {
        return ImmutableMultimap.<String, String>builder()
                .putAll(SAML2_KEY,
                        getSecretIdsForHostedRealmEntities(token, realm.asPath()))
                .build();
    }

    private Set<String> getSecretIdsForHostedRealmEntities(SSOToken token, String realm) {
        Set<String> secretIds = new HashSet<>();
        try {
            SAML2MetaManager metaManager = new SAML2MetaManager(token);
            List<String> entityIds = metaManager.getAllHostedEntities(realm);
            for (String entityId : entityIds) {
                secretIds.addAll(createEntitySecretsIds(realm, metaManager, entityId));
            }
        } catch (SAML2MetaException e) {
            logger.error("Unable to retrieve secret IDs for saml2 entities", e);
            return Collections.emptySet();
        }
        return secretIds;
    }

    private Set<String> createEntitySecretsIds(String realm, SAML2MetaManager metaManager, String entity)
            throws SAML2MetaException {
        Set<String> secretIds = new HashSet<>();
        EntityDescriptorElement entityDescriptor = metaManager.getEntityDescriptor(realm, entity);
        for (Saml2EntityRole role : EnumSet.of(Saml2EntityRole.IDP, Saml2EntityRole.SP)) {
            if (role.isPresent(entityDescriptor)) {
                String secretIdIdentifier = getAttributeValueFromSSOConfig(realm, entity,
                        role.getName(), SECRET_ID_IDENTIFIER);
                if (isNotBlank(secretIdIdentifier)) {
                    secretIds.add(String.format(SAML2_ENTITY_ROLE_SIGNING, secretIdIdentifier));
                    secretIds.add(String.format(SAML2_ENTITY_ROLE_ENCRYPTION, secretIdIdentifier));
                }
            }
        }
        return secretIds;
    }
}
