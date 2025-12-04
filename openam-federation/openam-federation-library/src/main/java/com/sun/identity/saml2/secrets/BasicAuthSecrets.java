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
 * Copyright 2023-2025 Ping Identity Corporation.
 */

package com.sun.identity.saml2.secrets;

import static org.forgerock.openam.shared.secrets.Labels.SAML2_ENTITY_ROLE_BASICAUTH;

import java.util.Optional;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.core.realms.Realms;
import org.forgerock.openam.secrets.Secrets;
import org.forgerock.secrets.GenericSecret;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.secrets.Purpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for SAML2 Basic Auth Secrets.
 */
public class BasicAuthSecrets {

    private static final Logger logger = LoggerFactory.getLogger(BasicAuthSecrets.class);

    private final Secrets secrets;

    /**
     * Constructs an instance of the BasicAuthSecrets.
     *
     */
    public BasicAuthSecrets() {
        this.secrets = InjectorHolder.getInstance(Secrets.class);
    }

    /**
     * Gets the realm's SAML2 Basic Auth secret based on the Secret ID Identifier.
     *
     * @param realm the realm in which the secret mapping is to be looked up
     * @param secretIDIdentifier the Secret ID Identifier configured on SAML2 entities
     * @return the secret value if it can be retrieved or empty
     */
    public Optional<char[]> getBasicAuthPassword(String realm, String secretIDIdentifier) {

        String secretLabel = String.format(SAML2_ENTITY_ROLE_BASICAUTH, secretIDIdentifier);
        try {
            char[] basicAuthPassword = secrets.getRealmSecrets(Realms.of(realm)).getActiveSecret(
                    Purpose.purpose(secretLabel, GenericSecret.class)
            ).getOrThrowIfInterrupted().revealAsUtf8(char[]::clone);
            return Optional.of(basicAuthPassword);
        } catch (NoSuchSecretException ex) {
            // Do nothing. This is already logged at debug level in the secrets API.
        } catch (RealmLookupException ex) {
            logger.warn(String.format("Problem finding realm %s when trying to retrieve secret mapping for %s", realm,
                    secretLabel), ex);
        }
        return Optional.empty();
    }
}
