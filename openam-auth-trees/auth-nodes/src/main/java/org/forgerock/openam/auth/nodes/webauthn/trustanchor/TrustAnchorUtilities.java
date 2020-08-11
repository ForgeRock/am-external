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
package org.forgerock.openam.auth.nodes.webauthn.trustanchor;

import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Singleton;

import org.forgerock.secrets.keys.VerificationKey;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility functions for manipulation of trust anchors and secrets.
 */
@Singleton
public class TrustAnchorUtilities {

    private final Logger logger = LoggerFactory.getLogger(TrustAnchorUtilities.class);

    /**
     * Converts a promise of a stream of secrets into a set of trust anchors.
     *
     * @param secrets incoming promise.
     * @return set of trust anchors contained within the secrets, or null.
     */
    public Set<TrustAnchor> trustAnchorsFromSecrets(Promise<Stream<VerificationKey>, NeverThrowsException> secrets) {
        Set<TrustAnchor> anchors = null;

        if (secrets != null) {
            try {
                anchors = secrets.get().map(secret -> secret.getCertificate(X509Certificate.class))
                        .filter(Optional::isPresent)
                        .distinct()
                        .map(cert -> new TrustAnchor(cert.get(), null))
                        .collect(Collectors.toUnmodifiableSet());
            } catch (ExecutionException | InterruptedException e) {
                logger.error("Unable to read secrets from secret Ids, trust chain verification will not function.", e);
            }
        }

        return anchors;
    }

}
