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
import static org.forgerock.cuppa.Cuppa.beforeEach;
import static org.forgerock.cuppa.Cuppa.describe;
import static org.forgerock.cuppa.Cuppa.it;
import static org.forgerock.cuppa.Cuppa.when;

import java.util.Collections;

import org.forgerock.cuppa.Test;
import org.forgerock.openam.auth.nodes.DeviceGeoFencingNode;

import com.google.common.collect.ImmutableSet;

@Test
public class GeoFencingValidatorTest {
    private DeviceGeoFencingNode.GeoFencingValidator validator;

    {
        describe("GeoFencingValidator", () -> {
            describe("validate", () -> {
                beforeEach(() -> {
                    validator = new DeviceGeoFencingNode.GeoFencingValidator();
                });
                when("The input is valid", () -> {
                    it("returns true", () -> {
                        assertThat(validator.validate(ImmutableSet.of("123.1,121.1"))).isTrue();
                    });
                });
                when("there are no values", () -> {
                    it("returns false", () -> {
                        assertThat(validator.validate(Collections.emptySet())).isFalse();
                    });
                });
                when("the values aren't parsed", () -> {
                    it("returns false", () -> {
                        assertThat(validator.validate(ImmutableSet.of("invalid format"))).isFalse();
                    });
                });
                when("one of the value is not a double", () -> {
                    it("returns false", () -> {
                        assertThat(validator.validate(ImmutableSet.of("131.1,test"))).isFalse();
                    });
                });
                when("Invalid format", () -> {
                    it("returns false", () -> {
                        assertThat(validator.validate(ImmutableSet.of("123.1,232,23"))).isFalse();
                    });
                });
            });
        });
    }
}
