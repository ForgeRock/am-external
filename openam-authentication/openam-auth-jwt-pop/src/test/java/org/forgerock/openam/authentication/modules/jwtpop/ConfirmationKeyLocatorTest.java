/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.jwtpop;

import static java.util.Comparator.naturalOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

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
import org.forgerock.json.jose.jwk.KeyUse;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ConfirmationKeyLocatorTest {

    @Mock
    private Supplier<JWKSet> mockJwkSetProvider;

    private ConfirmationKeyLocator confirmationKeyLocator;

    @BeforeMethod
    public void setup() {
        initMocks(this);
        confirmationKeyLocator = new ConfirmationKeyLocator(mockJwkSetProvider);
    }

    @Test
    public void shouldReturnEmptyForNullInput() throws Exception {
        assertThat(confirmationKeyLocator.resolveConfirmationKey(null)).isEmpty();
        assertThat(confirmationKeyLocator.resolveConfirmationKey(json(null))).isEmpty();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldThrowIllegalArgExceptionIfNotRecognised() throws Exception {
        confirmationKeyLocator.resolveConfirmationKey(json(object()));
    }

    @Test
    public void shouldReturnParsedJwkIfGiven() throws Exception {
        // Given
        JWK jwk = new EcJWK(KeyUse.SIG, "ES256", "kid", "aaa", "bbb", "P-256", null, null, null);
        JsonValue cnf = json(object(field("jwk", jwk.toJsonValue())));

        // When
        Optional<JWK> result = confirmationKeyLocator.resolveConfirmationKey(cnf);

        // Then
        assertThat(result).isNotEmpty().usingValueComparator(ConfirmationKeyLocatorTest::compareJwks).contains(jwk);
    }

    @Test
    public void shouldResolveKidFromJwkSetProvider() throws Exception {
        // Given
        String kid = "someKeyId";
        JWK jwk = new EcJWK(KeyUse.SIG, "ES256", kid, "aaa", "bbb", "P-256", null, null, null);
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
    public void shouldReturnEmptyIfNoJwkSetDefined() throws Exception {
        given(mockJwkSetProvider.get()).willReturn(null);
        assertThat(confirmationKeyLocator.resolveConfirmationKey(json(object(field("kid", "foo"))))).isEmpty();
    }

    @Test
    public void shouldReturnEmptyIfJwkSetDoesNotContainKey() throws Exception {
        // Given
        JWK jwk = new EcJWK(KeyUse.SIG, "ES256", "haystack", "aaa", "bbb", "P-256", null, null, null);
        JWKSet jwkSet = new JWKSet(jwk);
        given(mockJwkSetProvider.get()).willReturn(jwkSet);
        JsonValue cnf = json(object(field("kid", "needle")));

        // When
        Optional<JWK> result = confirmationKeyLocator.resolveConfirmationKey(cnf);

        // Then
        assertThat(result).isEmpty();
    }

    /** JWK and subclasses do not implement equals, so we implement it as a custom comparator. */
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
            if (cmp != 0) { break; }
        }
        return cmp;
    }
}