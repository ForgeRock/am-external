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
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.authentication.modules.jwtpop;

import static java.util.Comparator.naturalOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.jwk.EcJWK;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jwk.JWKSet;
import org.forgerock.json.jose.jwk.KeyUseConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ConfirmationKeyLocatorTest {

    @Mock
    private Supplier<JWKSet> mockJwkSetProvider;

    private ConfirmationKeyLocator confirmationKeyLocator;

    /**
     * JWK and subclasses do not implement equals, so we implement it as a custom comparator.
     */
    @SuppressWarnings("unchecked")
    private static int compareJwks(JWK aJwk, JWK bJwk) {
        EcJWK a = (EcJWK) aJwk;
        EcJWK b = (EcJWK) bJwk;
        if (a == null) {
            return b == null ? 0 : -1;
        }
        if (b == null) {
            return 1;
        }

        int cmp = 0;
        List<Function<EcJWK, Comparable>> getters = Arrays.asList(
                JWK::getKeyType, JWK::getUse, JWK::getKeyId, JWK::getAlgorithm,
                EcJWK::getCurve, EcJWK::getX, EcJWK::getY, EcJWK::getD
        );
        for (Function<EcJWK, Comparable> f : getters) {
            cmp = Objects.compare(f.apply(a), f.apply(b), naturalOrder());
            if (cmp != 0) {
                break;
            }
        }
        return cmp;
    }

    @BeforeEach
    void setup() {
        confirmationKeyLocator = new ConfirmationKeyLocator(mockJwkSetProvider);
    }

    @Test
    void shouldReturnEmptyForNullInput() throws Exception {
        assertThat(confirmationKeyLocator.resolveConfirmationKey(null)).isEmpty();
        assertThat(confirmationKeyLocator.resolveConfirmationKey(json(null))).isEmpty();
    }

    @Test
    void shouldThrowIllegalArgExceptionIfNotRecognised() throws Exception {
        assertThatThrownBy(() -> confirmationKeyLocator.resolveConfirmationKey(json(object())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldReturnParsedJwkIfGiven() throws Exception {
        // Given
        JWK jwk = new EcJWK(KeyUseConstants.SIG, "ES256", "kid", "aaa", "bbb", "P-256", null, null, null);
        JsonValue cnf = json(object(field("jwk", jwk.toJsonValue())));

        // When
        Optional<JWK> result = confirmationKeyLocator.resolveConfirmationKey(cnf);

        // Then
        assertThat(result).isNotEmpty().usingValueComparator(ConfirmationKeyLocatorTest::compareJwks).contains(jwk);
    }

    @Test
    void shouldResolveKidFromJwkSetProvider() throws Exception {
        // Given
        String kid = "someKeyId";
        JWK jwk = new EcJWK(KeyUseConstants.SIG, "ES256", kid, "aaa", "bbb", "P-256", null, null, null);
        JWKSet jwkSet = new JWKSet(jwk);
        given(mockJwkSetProvider.get()).willReturn(jwkSet);
        JsonValue cnf = json(object(field("kid", kid)));

        // When
        Optional<JWK> result = confirmationKeyLocator.resolveConfirmationKey(cnf);

        // Then
        verify(mockJwkSetProvider).get();
        assertThat(result).isNotEmpty().usingValueComparator(ConfirmationKeyLocatorTest::compareJwks).contains(jwk);
    }

    @Test
    void shouldReturnEmptyIfNoJwkSetDefined() throws Exception {
        given(mockJwkSetProvider.get()).willReturn(null);
        assertThat(confirmationKeyLocator.resolveConfirmationKey(json(object(field("kid", "foo"))))).isEmpty();
    }

    @Test
    void shouldReturnEmptyIfJwkSetDoesNotContainKey() throws Exception {
        // Given
        JWK jwk = new EcJWK(KeyUseConstants.SIG, "ES256", "haystack", "aaa", "bbb", "P-256", null, null, null);
        JWKSet jwkSet = new JWKSet(jwk);
        given(mockJwkSetProvider.get()).willReturn(jwkSet);
        JsonValue cnf = json(object(field("kid", "needle")));

        // When
        Optional<JWK> result = confirmationKeyLocator.resolveConfirmationKey(cnf);

        // Then
        assertThat(result).isEmpty();
    }
}
