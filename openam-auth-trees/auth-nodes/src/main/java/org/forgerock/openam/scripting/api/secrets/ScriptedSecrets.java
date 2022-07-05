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
 * Copyright 2020-2022 ForgeRock AS.
 */
package org.forgerock.openam.scripting.api.secrets;

import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.forgerock.openam.annotations.SupportedAll;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.secrets.Secrets;
import org.forgerock.secrets.GenericSecret;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.secrets.Purpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

/**
 * A wrapper around the Secrets API that allows a simplified interface to access secrets from a scripting context.
 *
 * @since AM 7.0.0
 */
@SupportedAll
public class ScriptedSecrets {

    private static final Logger logger = LoggerFactory.getLogger(ScriptedSecrets.class);
    /**
     * Pattern for Secret IDs that may be accessed by scripts.
     */
    private static final Predicate<String> SECRET_IS_ACCESSIBLE = secretId -> secretId.startsWith("scripted.node.");

    private final Secrets secrets;
    private final Realm realm;

    /**
     * Constructor.
     * @param secrets The secrets store
     * @param realm The realm this API exists in
     */
    @Inject
    public ScriptedSecrets(Secrets secrets, @Assisted Realm realm) {
        this.secrets = secrets;
        this.realm = realm;
    }

    /**
     * Allows the caller to access a generic type of secret from the Secrets API.
     * When the caller requests the secret it will be up to the {@link Secrets} class to resolve
     * this secret to a value.
     *
     * @param secretId A non null string identifier for the secret.
     * @return A non null {@link Secret} value object representing the secret requested.
     * @throws NodeProcessException If the secret is not found or is not accessible or the operation times out then this
     * exception will be thrown.
     * @throws RuntimeException If the thread is interrupted while waiting for the promise to resolve the secret.
     */
    public Secret getGenericSecret(@Nonnull String secretId) throws NodeProcessException {
        if (!secretIsAccessible(secretId)) {
            throw new NodeProcessException(String.format("Secret id %s not accessible", secretId));
        }
        try (GenericSecret genericSecret = secrets.getRealmSecrets(realm)
                .getActiveSecret(Purpose.purpose(secretId, GenericSecret.class))
                .getOrThrowIfInterrupted()) {
            logger.debug("Retrieved secret for secret Id {}", secretId);
            return new Secret(genericSecret);
        } catch (NoSuchSecretException e) {
            throw new NodeProcessException(String.format("Secret id %s not accessible or does not exist", secretId), e);
        }
    }

    private boolean secretIsAccessible(String secretId) {
        return SECRET_IS_ACCESSIBLE.test(secretId);
    }
}
