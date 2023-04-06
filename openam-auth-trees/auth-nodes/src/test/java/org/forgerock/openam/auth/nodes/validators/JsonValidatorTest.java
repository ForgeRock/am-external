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
 * Copyright 2023 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.validators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.cuppa.Cuppa.beforeEach;
import static org.forgerock.cuppa.Cuppa.describe;
import static org.forgerock.cuppa.Cuppa.it;
import static org.forgerock.cuppa.Cuppa.when;

import java.util.Collections;

import org.forgerock.cuppa.Test;
import org.forgerock.cuppa.junit.CuppaRunner;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableSet;

@RunWith(CuppaRunner.class)
@Test
public class JsonValidatorTest {
    private JsonValidator validator;

    {
        describe("DecimalValidator", () -> {
            describe("validate", () -> {
                beforeEach(() -> {
                    validator = new JsonValidator();
                });
                when("The input is valid", () -> {
                    it("returns true", () -> {
                        assertThat(validator.validate(ImmutableSet.of(
                                "{\"biometricAvailable\": { },\"deviceTampering\": {\"score\": 0.8}}"))
                        ).isTrue();
                    });
                });
                when("there are no values", () -> {
                    it("returns true", () -> {
                        assertThat(validator.validate(Collections.emptySet())).isTrue();
                    });
                });
                when("the value is empty", () -> {
                    it("returns true", () -> {
                        assertThat(validator.validate(ImmutableSet.of(""))).isTrue();
                    });
                });
                when("the value is null", () -> {
                    it("returns true", () -> {
                        assertThat(validator.validate(null)).isTrue();
                    });
                });
                when("the values aren't parsed", () -> {
                    it("returns false", () -> {
                        assertThat(validator.validate(ImmutableSet.of("invalid format"))).isFalse();
                    });
                });
            });
        });
    }
}
