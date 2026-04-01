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
package org.forgerock.openam.auth.trees.validation.validators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.trees.validation.ValidationFailure.Severity.ERROR;

import java.util.stream.Stream;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.trees.validation.ValidationFailure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class RequestBodyExistsValidatorTest {

    RequestBodyExistsValidator validator = new RequestBodyExistsValidator();

    @Test
    void shouldReturnNoFailureMessagesWhenRequestBodyExists() {
        assertThat(validator.validate(json(object(field("key", "value"))), null))
                .withFailMessage("Validator should return no failures when request body exists")
                .isEmpty();
    }

    @Test
    void shouldReturnFailureMessageWhenRequestBodyIsNull() {
        assertThat(validator.validate(null, null))
                .withFailMessage("Validator should return a failure when request body is null")
                .hasSize(1)
                .extracting("message")
                .containsExactly("Request body is empty");
    }

    @Test
    void shouldBeBlocking() {
        assertThat(validator.isBlocking())
                .withFailMessage("RequestBodyExistsValidator should be blocking")
                .isTrue();
    }

    @ParameterizedTest
    @MethodSource("emptyValuesProvider")
    void validateShouldReturnEmptyListWhenNoValidationErrors(JsonValue emptyValue) {
        // Given
        // When
        var result = validator.validate(emptyValue, null);
        // Then
        assertThat(result).containsExactly(new ValidationFailure("Request body is empty", ERROR));
    }

    private static Stream<Arguments> emptyValuesProvider() {
        return Stream.of(
                Arguments.of((JsonValue) null),
                Arguments.of(json(null)),
                Arguments.of(json(object()))
        );
    }
}
