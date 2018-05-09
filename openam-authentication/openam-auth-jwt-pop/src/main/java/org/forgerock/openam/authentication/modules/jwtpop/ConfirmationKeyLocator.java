/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.jwtpop;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jwk.JWKSet;

/**
 * Resolves confirmation key claims to the actual keys that can be used to confirm that a subject possesses the
 * corresponding private key.
 */
final class ConfirmationKeyLocator {

    private final Supplier<JWKSet> subjectJwkSetProvider;
    private final Set<ConfirmationKeyType> supportedKeyTypes;

    /**
     * Creates the confirmation key locator using the given provider to retrieve a JWK Set for the subject if required.
     *
     * @param subjectJwkSetProvider the provider to use to retrieve a JWK Set for the subject.
     * @param supportedKeyTypes the supported key types.
     */
    ConfirmationKeyLocator(Supplier<JWKSet> subjectJwkSetProvider, ConfirmationKeyType... supportedKeyTypes) {
        this.subjectJwkSetProvider = subjectJwkSetProvider;
        this.supportedKeyTypes = EnumSet.copyOf(Arrays.asList(supportedKeyTypes));
    }

    /**
     * Creates the confirmation key locator using the given provider to retrieve a JWK Set for the subject if required.
     *
     * @param subjectJwkSetProvider the provider to use to retrieve a JWK Set for the subject.
     */
    ConfirmationKeyLocator(Supplier<JWKSet> subjectJwkSetProvider) {
        this(subjectJwkSetProvider, ConfirmationKeyType.values());
    }


    /**
     * Resolves a confirmation key claim value to the actual JSON Web Key (JWK).
     *
     * @param cnf the confirmation key claim as a JSON value.
     * @return the resolved confirmation key, or {@link Optional#empty()} if there is no such key.
     * @throws org.forgerock.json.JsonException if the key can be found but is in an invalid format.
     * @throws IllegalArgumentException if the cnf claim is not in a recognised format.
     */
    Optional<JWK> resolveConfirmationKey(JsonValue cnf) {
        if (cnf == null || cnf.isNull()) {
            return Optional.empty();
        }

        if (supportedKeyTypes.contains(ConfirmationKeyType.JWK) && cnf.isDefined("jwk")) {
            return Optional.of(JWK.parse(cnf.get("jwk")));
        }
        if (supportedKeyTypes.contains(ConfirmationKeyType.KID) && cnf.isDefined("kid")) {
            final JWKSet jwkSet = subjectJwkSetProvider.get();
            if (jwkSet == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(jwkSet.findJwk(cnf.get("kid").asString()));
        }

        throw new IllegalArgumentException("unsupported cnf claim: " + cnf);
    }
}
