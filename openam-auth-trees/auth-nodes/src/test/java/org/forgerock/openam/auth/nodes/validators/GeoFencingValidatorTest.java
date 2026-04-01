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
 * Copyright 2020-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.validators;

import com.google.common.collect.ImmutableSet;
import org.forgerock.openam.auth.nodes.DeviceGeoFencingNode.GeoFencingValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GeoFencingValidator}.
 */
class GeoFencingValidatorTest {

    private GeoFencingValidator validator;

    @BeforeEach
    void setUp() {
        validator = new GeoFencingValidator();
    }

    @Test
    void shouldReturnTrueGivenValidInput() {
        // When
        boolean result = validator.validate(ImmutableSet.of("123.1,121.1"));

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseGivenNoValues() {
        // When
        boolean result = validator.validate(Collections.emptySet());

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalseGivenUnparsableFormat() {
        // When
        boolean result = validator.validate(ImmutableSet.of("invalid format"));

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalseGivenNonNumericValue() {
        // When
        boolean result = validator.validate(ImmutableSet.of("131.1,test"));

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalseGivenTooManyComponents() {
        // When
        boolean result = validator.validate(ImmutableSet.of("123.1,232,23"));

        // Then
        assertThat(result).isFalse();
    }
}