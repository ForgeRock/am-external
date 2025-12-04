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

package org.forgerock.openam.auth.nodes.validators;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;


import com.google.common.collect.ImmutableSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JsonValidatorTest {
    private JsonValidator validator;


    @BeforeEach
    public void beforeEach() {
        validator = new JsonValidator();
    }

    @Test
    @DisplayName(value = "The input is valid it returns true")
    public void testTheInputIsValidItReturnsTrue() throws Exception {
        assertThat(validator.validate(ImmutableSet.of(
                "{\"biometricAvailable\": { },\"deviceTampering\": {\"score\": 0.8}}"))
        ).isTrue();
    }

    @Test
    @DisplayName(value = "there are no values it returns true")
    public void testThereAreNoValuesItReturnsTrue() throws Exception {
        assertThat(validator.validate(Collections.emptySet())).isTrue();
    }

    @Test
    @DisplayName(value = "the value is empty it returns true")
    public void testTheValueIsEmptyItReturnsTrue() throws Exception {
        assertThat(validator.validate(ImmutableSet.of(""))).isTrue();
    }

    @Test
    @DisplayName(value = "the value is null it returns true")
    public void testTheValueIsNullItReturnsTrue() throws Exception {
        assertThat(validator.validate(null)).isTrue();
    }

    @Test
    @DisplayName(value = "the values aren't parsed it returns false")
    public void testTheValuesArenTParsedItReturnsFalse() throws Exception {
        assertThat(validator.validate(ImmutableSet.of("invalid format"))).isFalse();
    }
}
