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
package org.forgerock.openam.saml2.meta;

import static org.forgerock.openam.shared.secrets.Labels.SAML2_METADATA_SIGNING_RSA;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.inject.Inject;

import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.core.realms.Realms;
import org.forgerock.openam.secrets.Secrets;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.keys.KeyFormatRaw;
import org.forgerock.secrets.keys.SigningKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.saml2.meta.SAML2MetaException;

/**
 * Secrets api utilities for Saml2 metadata signing.
 */
public class Saml2MetaSecrets {

    private static final Logger logger = LoggerFactory.getLogger(Saml2MetaSecrets.class);
    private static final Purpose<SigningKey> METADATA_SIGNING_PURPOSE =
            Purpose.purpose(SAML2_METADATA_SIGNING_RSA, SigningKey.class);
    private final Secrets secrets;

    /**
     * Constructor.
     */
    @Inject
    public Saml2MetaSecrets(Secrets secrets) {
        this.secrets = secrets;
    }

    /**
     * Gets the signing key for the metadata signing purpose.
     *
     * @param realmName the realm the entity belongs to
     * @return the signing key for the metadata signing purpose
     * @throws SAML2MetaException if the realm does not exist or the signing key could not be retrieved
     */
    public SigningKey getSigningKey(String realmName) throws SAML2MetaException {
        try {
            return secrets.getRealmSecrets(Realms.of(realmName))
                    .getActiveSecret(METADATA_SIGNING_PURPOSE)
                    .getOrThrow();
        } catch (RealmLookupException e) {
            logger.error("Realm does not exist: {}", realmName);
            throw new SAML2MetaException(e);
        } catch (NoSuchSecretException e) {
            logger.error("Secret not found for purpose: {}", METADATA_SIGNING_PURPOSE.getLabel());
            throw new SAML2MetaException(e);
        } catch (InterruptedException e) {
            logger.error("Failed to retrieve signing key for purpose: {}", METADATA_SIGNING_PURPOSE.getLabel());
            Thread.currentThread().interrupt();
            throw new SAML2MetaException(e);
        }
    }

    /**
     * Gets the private key from the signing key.
     *
     * @param signingKey the signing key
     * @return the private key
     * @throws SAML2MetaException if the private key cannot be retrieved
     */
    public PrivateKey getPrivateKey(SigningKey signingKey) throws SAML2MetaException {
        try {
            return (PrivateKey) signingKey.export(KeyFormatRaw.INSTANCE);
        } catch (NoSuchSecretException e) {
            logger.error("Failed to retrieve private key: {}", METADATA_SIGNING_PURPOSE.getLabel());
            throw new SAML2MetaException(e);
        }
    }

    /**
     * Gets the X509 certificate from the signing key.
     *
     * @param signingKey the signing key
     * @return the X509 certificate
     * @throws SAML2MetaException if the X509 certificate cannot be retrieved
     */
    public X509Certificate getX509Certificate(SigningKey signingKey) throws SAML2MetaException {
        return signingKey.getCertificate()
                .map(X509Certificate.class::cast)
                .orElseThrow(() ->
                        new SAML2MetaException("Certificate not found: " + METADATA_SIGNING_PURPOSE.getLabel()));
    }
}
