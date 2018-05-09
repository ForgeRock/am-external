/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.validators;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.cuppa.Cuppa.beforeEach;
import static org.forgerock.cuppa.Cuppa.describe;
import static org.forgerock.cuppa.Cuppa.it;
import static org.forgerock.cuppa.Cuppa.when;

import java.util.Collections;
import java.util.List;

import org.forgerock.cuppa.Test;
import org.forgerock.cuppa.junit.CuppaRunner;
import org.forgerock.guava.common.collect.ImmutableSet;
import org.junit.runner.RunWith;

import com.sun.identity.sm.ServiceAttributeValidator;

@RunWith(CuppaRunner.class)
@Test
public class SessionPropertyNameValidatorTest {
    private ServiceAttributeValidator validator;

    {
        describe("SessionPropertyNameValidator", () -> {
            describe("validate", () -> {
                beforeEach(() -> {
                    List<String> systemPropertyNames = asList("x", "y");
                    validator = new SessionPropertyNameValidator(systemPropertyNames);
                });
                when("the input is valid", () -> {
                    it("returns true", () -> {
                        assertThat(validator.validate(ImmutableSet.of("a", "b"))).isTrue();
                    });
                });
                when("there are no values", () -> {
                    it("returns false", () -> {
                        assertThat(validator.validate(Collections.emptySet())).isFalse();
                    });
                });
                when("one of the values is a system property", () -> {
                    it("returns false", () -> {
                        assertThat(validator.validate(ImmutableSet.of("a", "x"))).isFalse();
                    });
                });
            });
        });
    }
}