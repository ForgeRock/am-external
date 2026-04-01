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
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.ArrayList;
import java.util.List;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.realms.Realm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthTreeValidationEngineTest {

    private List<Validator> validators;
    private AuthTreeValidationEngine validationEngine;

    @BeforeEach
    void setUp() {
        validators = new ArrayList<>();
        validationEngine = new AuthTreeValidationEngine(validators);
    }

    @Test
    void assertValidateWithNoValidatorsReturnsEmptyList() {
        // When
        List<ValidationFailure> results = validationEngine.validate(json(object()), null);

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    void assertValidateWithNonBlockingValidatorReturnsAllFailures() {
        // Given
        validators.add(new ConstantValidator(List.of(
                new ValidationFailure("Warning 1", ValidationFailure.Severity.WARNING),
                new ValidationFailure("Error 1", ValidationFailure.Severity.ERROR)
        ), false));
        validators.add(new ConstantValidator(List.of(
                new ValidationFailure("Warning 2", ValidationFailure.Severity.WARNING)
        ), false));

        // When
        List<ValidationFailure> results = validationEngine.validate(json(object()), null);

        // Then
        assertThat(results).hasSize(3);

    }

    @Test
    void assertValidateWithBlockingValidatorStopsOnError() {
        // Given
        validators.add(new ConstantValidator(List.of(
                new ValidationFailure("Warning 1", ValidationFailure.Severity.WARNING)
        ), false));
        validators.add(new ConstantValidator(List.of(
                new ValidationFailure("Error 1", ValidationFailure.Severity.ERROR)
        ), true));
        validators.add(new ConstantValidator(List.of(
                new ValidationFailure("Warning 2", ValidationFailure.Severity.WARNING)
        ), false));

        // When
        List<ValidationFailure> results = validationEngine.validate(json(object()), null);

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void assertValidateWithBlockingValidatorContinuesOnWarning() {
        // Given
        validators.add(new ConstantValidator(List.of(
                new ValidationFailure("Warning 1", ValidationFailure.Severity.WARNING)
        ), true));
        validators.add(new ConstantValidator(List.of(
                new ValidationFailure("Warning 2", ValidationFailure.Severity.WARNING)
        ), true));
        validators.add(new ConstantValidator(List.of(
                new ValidationFailure("Error 1", ValidationFailure.Severity.ERROR)
        ), true));

        // When
        List<ValidationFailure> results = validationEngine.validate(json(object()), null);

        // Then
        assertThat(results).hasSize(3);
    }

    @Test
    void assertValidateWithNoFailuresContinuesThroughAllValidators() {
        // Given
        validators.add(new ConstantValidator(List.of(
        ), true));
        validators.add(new ConstantValidator(List.of(
        ), true));
        validators.add(new ConstantValidator(List.of(
                new ValidationFailure("Warning", ValidationFailure.Severity.WARNING)
        ), true));

        // When
        List<ValidationFailure> results = validationEngine.validate(json(object()), null);

        // Then
        assertThat(results).hasSize(1);
    }

    @Test
    void assertThatRequestBodyIsPassedToEachValidator() {
        // Given
        JsonValue requestBody = json(object());
        List<JsonValue> receivedRequestBodies = new ArrayList<>();

        validators.add(new RequestBodyStorageValidator(receivedRequestBodies));

        validators.add(new RequestBodyStorageValidator(receivedRequestBodies));

        // When
        validationEngine.validate(requestBody, null);

        // Then
        assertThat(receivedRequestBodies).hasSize(2);
        assertThat(receivedRequestBodies).allMatch(rb -> rb.equals(requestBody));
    }

    @Test
    void assertThatExceptionInValidatorIsHandledGracefully() {
        // Given
        validators.add(new Validator() {
            @Override
            public List<ValidationFailure> validate(JsonValue requestBody, Realm realm) {
                throw new RuntimeException("Unexpected error");
            }

            @Override
            public boolean isBlocking() {
                return true;
            }
        });

        validators.add(new ConstantValidator(List.of(
                new ValidationFailure("Warning", ValidationFailure.Severity.WARNING)
        ), true));

        // When
        List<ValidationFailure> results = validationEngine.validate(json(object()), null);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).message()).isEqualTo("Validation failed due to an unexpected error");
        assertThat(results.get(0).severity()).isEqualTo(ValidationFailure.Severity.ERROR);
    }

    private record ConstantValidator(List<ValidationFailure> validationFailures, boolean blocking)
            implements Validator {

        @Override
        public List<ValidationFailure> validate(JsonValue requestBody, Realm realm) {
            return validationFailures;
        }

        @Override
        public boolean isBlocking() {
            return blocking;
        }
    }

    private record RequestBodyStorageValidator(List<JsonValue> receivedRequestBodies) implements Validator {

        @Override
        public List<ValidationFailure> validate(JsonValue requestBody, Realm realm) {
            receivedRequestBodies.add(requestBody);
            return List.of();
        }

        @Override
        public boolean isBlocking() {
            return false;
        }
    }
}