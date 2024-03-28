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
 * Copyright 2020-2024 ForgeRock AS.
 */
package org.forgerock.openam.saml2.plugins;

import static com.sun.identity.saml2.common.SAML2Constants.SECRET_ID_IDENTIFIER;
import static com.sun.identity.saml2.common.SAML2Utils.getAttributeValueFromSSOConfig;
import static org.forgerock.openam.saml2.Saml2EntityRole.IDP;
import static org.forgerock.openam.saml2.Saml2EntityRole.SP;
import static org.forgerock.openam.shared.secrets.Labels.SAML2_DEFAULT_IDP_ENCRYPTION;
import static org.forgerock.openam.shared.secrets.Labels.SAML2_DEFAULT_IDP_SIGNING;
import static org.forgerock.openam.shared.secrets.Labels.SAML2_DEFAULT_SP_ENCRYPTION;
import static org.forgerock.openam.shared.secrets.Labels.SAML2_DEFAULT_SP_MTLS;
import static org.forgerock.openam.shared.secrets.Labels.SAML2_DEFAULT_SP_SIGNING;
import static org.forgerock.openam.shared.secrets.Labels.SAML2_ENTITY_ROLE_ENCRYPTION;
import static org.forgerock.openam.shared.secrets.Labels.SAML2_ENTITY_ROLE_MTLS;
import static org.forgerock.openam.shared.secrets.Labels.SAML2_ENTITY_ROLE_SIGNING;
import static org.forgerock.secrets.Purpose.purpose;
import static org.forgerock.util.LambdaExceptionUtils.rethrowFunction;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.core.realms.Realms;
import org.forgerock.openam.saml2.Saml2EntityRole;
import org.forgerock.openam.saml2.crypto.signing.Saml2SigningCredentials;
import org.forgerock.openam.secrets.DefaultingPurpose;
import org.forgerock.openam.secrets.Secrets;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.keys.CryptoKey;
import org.forgerock.secrets.keys.KeyDecryptionKey;
import org.forgerock.secrets.keys.KeyFormatRaw;
import org.forgerock.secrets.keys.SigningKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.sun.identity.saml2.common.SAML2Exception;

/**
 * This credential resolver implementation uses the secrets backend to resolve secrets for SAML2 signing and decryption.
 */
@Singleton
public class SecretsSaml2CredentialResolver implements Saml2CredentialResolver {

    private static final Logger logger = LoggerFactory.getLogger(SecretsSaml2CredentialResolver.class);
    private static final Map<Saml2EntityRole, Purpose<SigningKey>> SIGNING_PURPOSES = Maps.immutableEnumMap(
            ImmutableMap.of(IDP, purpose(SAML2_DEFAULT_IDP_SIGNING, SigningKey.class),
                    SP, purpose(SAML2_DEFAULT_SP_SIGNING, SigningKey.class)));
    private static final Map<Saml2EntityRole, Purpose<KeyDecryptionKey>> ENCRYPTION_PURPOSES = Maps.immutableEnumMap(
            ImmutableMap.of(IDP, purpose(SAML2_DEFAULT_IDP_ENCRYPTION, KeyDecryptionKey.class),
                    SP, purpose(SAML2_DEFAULT_SP_ENCRYPTION, KeyDecryptionKey.class)));
    private static final Map<Saml2EntityRole, Purpose<SigningKey>> MTLS_PURPOSES = Maps.immutableEnumMap(
            ImmutableMap.of(SP, purpose(SAML2_DEFAULT_SP_MTLS, SigningKey.class)));
    private static final Saml2SigningCredentials NO_SIGNING_DETAILS = new Saml2SigningCredentials(null, null);
    private final Secrets secrets;
    private final KeyStoreSaml2CredentialResolver keyStoreResolver;

    /**
     * Constructor.
     *
     * @param secrets The secrets backend.
     * @param keyStoreResolver The keystore based credential resolver to delegate to for roles that don't support
     * secrets yet.
     */
    @Inject
    public SecretsSaml2CredentialResolver(Secrets secrets, KeyStoreSaml2CredentialResolver keyStoreResolver) {
        this.secrets = secrets;
        this.keyStoreResolver = keyStoreResolver;
    }

    @Override
    public Saml2SigningCredentials resolveActiveSigningCredential(String realmName, String entityId,
            Saml2EntityRole role) throws SAML2Exception {
        if (!SIGNING_PURPOSES.containsKey(role)) {
            return keyStoreResolver.resolveActiveSigningCredential(realmName, entityId, role);
        }

        String secretIdIdentifier = null;
        try {
            Realm realm = Realms.of(realmName);
            secretIdIdentifier = getSecretIdIdentifier(realmName, entityId, role);
            logger.debug("secret label identifier is {} for the entity {} with role {}",
                    secretIdIdentifier, entityId, role.name());
            DefaultingPurpose<SigningKey> purpose = new DefaultingPurpose<>(SIGNING_PURPOSES.get(role),
                    SAML2_ENTITY_ROLE_SIGNING);
            SigningKey signingKey = secrets.getRealmSecrets(realm)
                    .getActiveSecret(purpose, secretIdIdentifier)
                    .getOrThrowIfInterrupted();
            return new Saml2SigningCredentials(getPrivateKey(signingKey), getX509Certificate(signingKey));
        } catch (RealmLookupException ex) {
            throw new SAML2Exception(ex);
        } catch (NoSuchSecretException ex) {
            logger.warn("Secret not found for the entity {} with role {} and secret label identifier {}",
                    entityId, role, secretIdIdentifier, ex);
            return NO_SIGNING_DETAILS;
        }
    }

    @Override
    public Set<X509Certificate> resolveValidSigningCredentials(String realm, String entityId, Saml2EntityRole role)
            throws SAML2Exception {
        if (!SIGNING_PURPOSES.containsKey(role)) {
            return keyStoreResolver.resolveValidSigningCredentials(realm, entityId, role);
        }

        if (MTLS_PURPOSES.containsKey(role)) {
            return Stream.concat(resolveValidSecrets(realm, entityId,
                    new DefaultingPurpose<>(SIGNING_PURPOSES.get(role), SAML2_ENTITY_ROLE_SIGNING), role),
                    resolveValidSecrets(realm, entityId,
                    new DefaultingPurpose<>(MTLS_PURPOSES.get(role), SAML2_ENTITY_ROLE_MTLS), role))
                    .map(this::getX509Certificate)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        return resolveValidSecrets(realm, entityId,
                new DefaultingPurpose<>(SIGNING_PURPOSES.get(role), SAML2_ENTITY_ROLE_SIGNING), role)
                .map(this::getX509Certificate)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public Set<PrivateKey> resolveValidDecryptionCredentials(String realm, String entityId, Saml2EntityRole role)
            throws SAML2Exception {
        if (!ENCRYPTION_PURPOSES.containsKey(role)) {
            return keyStoreResolver.resolveValidDecryptionCredentials(realm, entityId, role);
        }

        return resolveValidSecrets(realm, entityId,
                new DefaultingPurpose<>(ENCRYPTION_PURPOSES.get(role), SAML2_ENTITY_ROLE_ENCRYPTION), role)
                .map(rethrowFunction(this::getPrivateKey))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public Set<X509Certificate> resolveValidEncryptionCredentials(String realm, String entityId, Saml2EntityRole role)
            throws SAML2Exception {
        if (!ENCRYPTION_PURPOSES.containsKey(role)) {
            return keyStoreResolver.resolveValidEncryptionCredentials(realm, entityId, role);
        }

        return resolveValidSecrets(realm, entityId,
                new DefaultingPurpose<>(ENCRYPTION_PURPOSES.get(role), SAML2_ENTITY_ROLE_ENCRYPTION), role)
                .map(this::getX509Certificate)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private <T extends CryptoKey> Stream<T> resolveValidSecrets(String realm, String entityId,
            DefaultingPurpose<T> purpose, Saml2EntityRole role) throws SAML2Exception {
        try {
            String secretIdIdentifier = getSecretIdIdentifier(realm, entityId, role);
            return secrets.getRealmSecrets(Realms.of(realm))
                    .getValidSecrets(purpose, secretIdIdentifier)
                    .getOrThrowIfInterrupted();
        } catch (RealmLookupException ex) {
            throw new SAML2Exception(ex);
        }
    }

    private String getSecretIdIdentifier(String realm, String entityId, Saml2EntityRole role) {
        return getAttributeValueFromSSOConfig(realm, entityId, role.getName(), SECRET_ID_IDENTIFIER);
    }

    private PrivateKey getPrivateKey(CryptoKey cryptoKey) throws SAML2Exception {
        try {
            return (PrivateKey) cryptoKey.export(KeyFormatRaw.INSTANCE);
        } catch (NoSuchSecretException e) {
            throw new SAML2Exception(e);
        }
    }

    private X509Certificate getX509Certificate(CryptoKey cryptoKey) {
        return cryptoKey.getCertificate(X509Certificate.class).orElse(null);
    }
}
