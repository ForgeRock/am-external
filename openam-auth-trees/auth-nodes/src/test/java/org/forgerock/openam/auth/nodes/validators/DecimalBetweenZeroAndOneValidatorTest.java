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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;


import com.google.common.collect.ImmutableSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DecimalBetweenZeroAndOneValidatorTest {
    private DecimalBetweenZeroAndOneValidator validator;

    @BeforeEach
    public void beforeEach() {
        validator = new DecimalBetweenZeroAndOneValidator();
    }

    @Test
    @DisplayName(value = "The input is in range it returns true")
    public void testTheInputIsInRangeItReturnsTrue() throws Exception {
        assertThat(validator.validate(ImmutableSet.of("0.5"))).isTrue();
    }

    @Test
    @DisplayName(value = "The input is 0 it returns true")
    public void testTheInputIs0ItReturnsTrue() throws Exception {
        assertThat(validator.validate(ImmutableSet.of("0"))).isTrue();
    }

    @Test
    @DisplayName(value = "The input is 1 it returns true")
    public void testTheInputIs1ItReturnsTrue() throws Exception {
        assertThat(validator.validate(ImmutableSet.of("1"))).isTrue();
    }

    @Test
    @DisplayName(value = "The input is out range it returns false")
    public void testTheInputIsOutRangeItReturnsFalse() throws Exception {
        assertThat(validator.validate(ImmutableSet.of("1.5"))).isFalse();
    }

    @Test
    @DisplayName(value = "there are no values it returns true")
    public void testThereAreNoValuesItReturnsTrue() throws Exception {
        assertThat(validator.validate(Collections.emptySet())).isTrue();
    }

    @Test
    @DisplayName(value = "the values aren't parsed it returns false")
    public void testTheValuesArenTParsedItReturnsFalse() throws Exception {
        assertThat(validator.validate(ImmutableSet.of("invalid format"))).isFalse();
    }
}
