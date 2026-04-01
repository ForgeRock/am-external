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
 * Copyright 2026 Ping Identity Corporation.
 */
package org.forgerock.openam.radius.server.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RadiusHandlerConfigValidator}.
 */
class RadiusHandlerConfigValidatorTest {

    private final RadiusHandlerConfigValidator validator = new RadiusHandlerConfigValidator();

    @Test
    void shouldReturnFalseGivenDuplicateTreeKey() {
        assertThat(validator.validate(Set.of("tree=alpha", "tree=beta"))).isFalse();
        assertThat(validator.validationMessages())
                .containsExactly("radius-handlerconfig-duplicates");
    }

    @Test
    void shouldReturnTrueGivenDuplicateChainKey() {
        assertThat(validator.validate(Set.of("chain=primary", "chain=secondary"))).isTrue();
        assertThat(validator.validationMessages()).isEmpty();
    }

    @Test
    void shouldReturnTrueGivenSingleTreeOrChainKey() {
        assertThat(validator.validate(Set.of("realm=/", "tree=alpha"))).isTrue();
        assertThat(validator.validate(Set.of("realm=/", "chain=primary"))).isTrue();
        assertThat(validator.validationMessages()).isEmpty();
    }
}
