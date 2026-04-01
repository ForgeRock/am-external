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
 * Copyright 2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.trees.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.realms.Realm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthTreeValidationGuiceModuleTest {

    private AuthTreeValidationGuiceModule validationModule;

    @BeforeEach
    void setUp() {
        validationModule = new AuthTreeValidationGuiceModule();
    }

    @Test
    void shouldOrderValidatorsGivenTheyAreAlreadyInOrder() {
        // when
        List<Validator> orderedValidators = validationModule.orderedValidators(
                Map.of(1, new FirstValidator(),
                        2, new SecondValidator()));

        // then
        assertThat(orderedValidators).hasSize(2);
        assertThat(orderedValidators.get(0)).isInstanceOf(FirstValidator.class);
        assertThat(orderedValidators.get(1)).isInstanceOf(SecondValidator.class);
    }

    @Test
    void shouldOrderValidatorsGivenTheyAreOutOfOrder() {
        // when
        var orderedValidators = validationModule.orderedValidators(
                Map.of(2, new SecondValidator(), 1, new FirstValidator()));

        // then
        assertThat(orderedValidators).hasSize(2);
        assertThat(orderedValidators.get(0)).isInstanceOf(FirstValidator.class);
        assertThat(orderedValidators.get(1)).isInstanceOf(SecondValidator.class);
    }

    private abstract static class DummyValidator implements Validator {
        @Override
        public List<ValidationFailure> validate(JsonValue requestBody, Realm realm) {
            return List.of();
        }

        @Override
        public boolean isBlocking() {
            return true;
        }
    }

    private static class FirstValidator extends DummyValidator {

    }

    private static class SecondValidator extends DummyValidator {

    }

}