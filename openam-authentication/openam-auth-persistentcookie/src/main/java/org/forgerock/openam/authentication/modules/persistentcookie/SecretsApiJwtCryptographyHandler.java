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

import static org.forgerock.openam.authentication.modules.persistentcookie.PersistentCookieModuleWrapper.INSTANCE_NAME_KEY;
import static org.forgerock.openam.authentication.modules.persistentcookie.PersistentCookieModuleWrapper.SSO_TOKEN_ORGANIZATION_PROPERTY_KEY;
import static org.forgerock.secrets.Purpose.purpose;

import java.util.Map;

import org.forgerock.caf.authentication.api.AuthenticationException;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.jaspi.modules.session.jwt.JwtCryptographyHandler;
import org.forgerock.json.jose.builders.EncryptedJwtBuilder;
import org.forgerock.json.jose.builders.EncryptedThenSignedJwtBuilder;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jws.EncryptedThenSignedJwt;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.core.realms.Realms;
import org.forgerock.openam.jwt.JwtDecryptionHandler;
import org.forgerock.openam.jwt.JwtEncryptionHandler;
import org.forgerock.openam.jwt.JwtEncryptionOptions;
import org.forgerock.openam.jwt.JwtSignatureVerificationHandler;
import org.forgerock.openam.jwt.JwtSigningHandler;
import org.forgerock.openam.jwt.JwtSigningOptions;
import org.forgerock.openam.jwt.exceptions.DecryptionFailedException;
import org.forgerock.openam.secrets.DefaultingPurpose;
import org.forgerock.openam.secrets.Secrets;
import org.forgerock.openam.secrets.SecretsProviderFacade;
import org.forgerock.openam.shared.secrets.Labels;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.secrets.keys.CryptoKey;
import org.forgerock.secrets.keys.DataDecryptionKey;
import org.forgerock.secrets.keys.DataEncryptionKey;
import org.forgerock.secrets.keys.SigningKey;
import org.forgerock.secrets.keys.VerificationKey;

/**
 * An extension of the DefaultJwtCryptographyHandler that can either use old hard-coded secrets, or the Secrets API.
 */
public class SecretsApiJwtCryptographyHandler implements JwtCryptographyHandler {

    private static final DefaultingPurpose<SigningKey> PERSISTENT_COOKIE_SIGNING =
            new DefaultingPurpose<>(purpose(Labels.DEFAULT_PCOOKIE_SIGNING, SigningKey.class),
                    Labels.CUSTOM_PCOOKIE_SIGNING);
    private static final DefaultingPurpose<VerificationKey> PERSISTENT_COOKIE_VERIFICATION =
            new DefaultingPurpose<>(purpose(Labels.DEFAULT_PCOOKIE_SIGNING, VerificationKey.class),
                    Labels.CUSTOM_PCOOKIE_SIGNING);
    private static final DefaultingPurpose<? extends CryptoKey> PERSISTENT_COOKIE_ENCRYPTION =
            new DefaultingPurpose<>(purpose(Labels.DEFAULT_PCOOKIE_ENCRYPTION, DataEncryptionKey.class),
                    Labels.CUSTOM_PCOOKIE_ENCRYPTION);
    private static final DefaultingPurpose<? extends CryptoKey> PERSISTENT_COOKIE_DECRYPTION =
            new DefaultingPurpose<>(purpose(Labels.DEFAULT_PCOOKIE_ENCRYPTION, DataDecryptionKey.class),
                    Labels.CUSTOM_PCOOKIE_ENCRYPTION);
    private static final JwsAlgorithm SIGNING_ALGORITHM = JwsAlgorithm.HS256;
    private static final JweAlgorithm ENCRYPTION_ALGORITHM = JweAlgorithm.RSAES_PKCS1_V1_5;
    private static final EncryptionMethod ENCRYPTION_METHOD = EncryptionMethod.A128CBC_HS256;
    private String instanceId;
    private SecretsProviderFacade secretsProvider;

    /**
     * Create a new JWT cryptography handler for the JASPI module.
     */
    SecretsApiJwtCryptographyHandler() {
    }

    @Override
    public void initialize(Map<String, Object> map) throws AuthenticationException {
        String realmName = (String) map.get(SSO_TOKEN_ORGANIZATION_PROPERTY_KEY);
        Secrets secrets = InjectorHolder.getInstance(Secrets.class);
        Realm realm;
        try {
            realm = Realms.of(realmName);
            secretsProvider = secrets.getRealmSecrets(realm);
        } catch (RealmLookupException e) {
            throw new AuthenticationException("Realm does not exist: " + realmName);
        }
        instanceId = (String) map.get(INSTANCE_NAME_KEY);
    }

    @Override
    public void decrypt(EncryptedThenSignedJwt encryptedThenSignedJwt) {
        try {
            new JwtDecryptionHandler(new JwtEncryptionOptions(secretsProvider, ENCRYPTION_ALGORITHM, ENCRYPTION_METHOD))
                    .decryptJwe(encryptedThenSignedJwt, PERSISTENT_COOKIE_DECRYPTION, instanceId);
        } catch (DecryptionFailedException dfe) {
            throw new RuntimeException("Unable to decrypt JWT", dfe);
        }
    }

    @Override
    public EncryptedJwtBuilder jwe(JwtBuilderFactory jwtBuilderFactory) {
        try {
            return new JwtEncryptionHandler(new JwtEncryptionOptions(secretsProvider, ENCRYPTION_ALGORITHM,
                    ENCRYPTION_METHOD))
                    .buildEncryptedJwt(jwtBuilderFactory, PERSISTENT_COOKIE_ENCRYPTION, instanceId);
        } catch (NoSuchSecretException nse) {
            throw new RuntimeException("No active secret for encryption using " + PERSISTENT_COOKIE_ENCRYPTION
                    + " for " + instanceId);
        }
    }

    @Override
    public EncryptedThenSignedJwtBuilder sign(EncryptedJwtBuilder builder) {
        return new JwtSigningHandler(new JwtSigningOptions(secretsProvider, SIGNING_ALGORITHM))
                .signJwt(builder, PERSISTENT_COOKIE_SIGNING, instanceId);
    }

    @Override
    public boolean verify(EncryptedThenSignedJwt signedEncryptedJwt) {
        return new JwtSignatureVerificationHandler(new JwtSigningOptions(secretsProvider, SIGNING_ALGORITHM))
                .verifyJwsSignature(signedEncryptedJwt, PERSISTENT_COOKIE_VERIFICATION, instanceId);
    }
}
