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
 * Copyright 2020 ForgeRock AS.
 */

package org.forgerock.openam.federation.rest.secret.manager;

import static com.sun.identity.saml2.common.SAML2Constants.SECRET_ID_IDENTIFIER;
import static java.util.stream.Collectors.toSet;
import static org.forgerock.openam.shared.secrets.Labels.SAML2_ENTITY_ROLE_ENCRYPTION;
import static org.forgerock.openam.shared.secrets.Labels.SAML2_ENTITY_ROLE_SIGNING;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;
import javax.xml.bind.JAXBElement;

import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.core.realms.Realms;
import org.forgerock.openam.secrets.config.KeyStoreSecretStore;
import org.forgerock.openam.secrets.config.PurposeMapping;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.forgerock.openam.sm.annotations.subconfigs.Multiple;
import org.forgerock.openam.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.sso.SSOException;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.sm.SMSException;

/**
 * A class with a collection of methods used for by {@link Saml2EntitySecretMappingManager}.
 */
class SecretMappingManagerHelper {

    private static final Logger logger = LoggerFactory.getLogger(SecretMappingManagerHelper.class);
    private static final String FORMAT_SPECIFIER = "%s";
    private static final String SAML2_ENTITY_SECRET_LABEL_PREFIX =
            SAML2_ENTITY_ROLE_SIGNING.substring(0, SAML2_ENTITY_ROLE_SIGNING.indexOf(FORMAT_SPECIFIER));
    private static final String SIGNING = SAML2_ENTITY_ROLE_SIGNING
            .substring(SAML2_ENTITY_ROLE_SIGNING.indexOf(FORMAT_SPECIFIER) + FORMAT_SPECIFIER.length());
    private static final String ENCRYPTION = SAML2_ENTITY_ROLE_ENCRYPTION
            .substring(SAML2_ENTITY_ROLE_ENCRYPTION.indexOf(FORMAT_SPECIFIER) + FORMAT_SPECIFIER.length());

    private final AnnotatedServiceRegistry serviceRegistry;

    /**
     * Creates an instance of SecretMappingManagerHelper.
     *
     * @param serviceRegistry The annotated service registry.
     */
    @Inject
    SecretMappingManagerHelper(AnnotatedServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * Get all the secret id identifiers associated with all the SAML2 entities in the given realm.
     *
     * @param realm The realm in which the SAML2 entities resides.
     * @return All secret id identifiers associated with all the entities in the realm.
     * @throws SAML2MetaException should an issue occur when getting all the secret id identifiers.
     */
    Set<String> getAllEntitySecretIdIdentifiers(String realm) throws SAML2MetaException {
        SAML2MetaManager saml2MetaManager = SAML2Utils.getSAML2MetaManager();
        List<EntityConfigElement> entityConfigElements = saml2MetaManager.getAllHostedEntityConfigs(realm);
        return entityConfigElements.stream()
                .map(e -> e.getValue().getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig())
                .flatMap(List::stream)
                .map(this::getSecretIdIdentifier)
                .filter(Objects::nonNull)
                .collect(toSet());
    }

    /**
     * Gets all the KeyStoreSecretStores configured in the given realm.
     *
     * @param realm The realm in which the KeyStoreSecretStores are configured.
     * @return The KeyStoreSecretStores.
     */
    Set<KeyStoreSecretStore> getRealmStores(String realm) {
        try {
            return serviceRegistry.getRealmInstances(KeyStoreSecretStore.class, Realms.of(realm));
        } catch (SSOException | SMSException | RealmLookupException e) {
            logger.error("Failed to get the realm secret stores for the realm {}", realm, e);
            return Collections.emptySet();
        }
    }

    /**
     * Checks if the given mapping id is SAML2 secret id mapping and verifies
     * if it is not used any more.
     *
     * @param secretIdIdentifiers The available saml2 secret id identifiers.
     * @param mappingId The mapping id to check.
     * @return if the mapping id is un used secret id mapping or not.
     */
    boolean isUnusedSecretMapping(Set<String> secretIdIdentifiers, String mappingId) {
        if (mappingId.startsWith(SAML2_ENTITY_SECRET_LABEL_PREFIX)
                && (mappingId.endsWith(SIGNING) || mappingId.endsWith(ENCRYPTION))) {
            if (secretIdIdentifiers.isEmpty()) {
                return true;
            }
            return secretIdIdentifiers.stream().noneMatch(mappingId::contains);
        }
        return false;
    }

    /**
     * Deletes a mapping from the purpose mappings.
     *
     * @param mappings The purpose mappings from which the mapping needs to be deleted..
     * @param mappingId The id of the mapping to be deleted.
     */
    void deleteSecretMapping(Multiple<PurposeMapping> mappings, String mappingId) {
        try {
            mappings.delete(mappingId);
        } catch (SMSException | SSOException e) {
            logger.warn("Failed to delete the saml2 entity secret mapping with id {}", mappingId, e);
        }
    }

    private String getSecretIdIdentifier(JAXBElement<BaseConfigType> role) {
        String identifier = null;
        Map<String, List<String>> attributes = SAML2MetaUtils.getAttributes(role);
        List<String> value = attributes.get(SECRET_ID_IDENTIFIER);
        if (CollectionUtils.isNotEmpty(value)) {
            identifier = value.get(0);
        }
        return identifier;
    }
}
