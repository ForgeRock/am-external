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
 * Copyright 2017-2018 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.validators;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.cuppa.Cuppa.beforeEach;
import static org.forgerock.cuppa.Cuppa.describe;
import static org.forgerock.cuppa.Cuppa.it;
import static org.forgerock.cuppa.Cuppa.when;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.forgerock.cuppa.Test;
import org.forgerock.cuppa.junit.CuppaRunner;
import org.forgerock.openam.configuration.MapValueParser;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.sun.identity.sm.ServiceAttributeValidator;

@RunWith(CuppaRunner.class)
@Test
public class SessionPropertyValidatorTest {
    private final static Set<String> INPUT = ImmutableSet.of("[a]=1", "[b]=2");
    private MapValueParser parser;
    private ServiceAttributeValidator validator;

    {
        describe("SessionPropertyValidator", () -> {
            describe("validate", () -> {
                beforeEach(() -> {
                    parser = mock(MapValueParser.class);
                    given(parser.parse(anySetOf(String.class))).willReturn(ImmutableMap.of("a", "1", "b", "2"));
                    List<String> systemPropertyNames = asList("x", "y");
                    validator = new SessionPropertyValidator(parser, systemPropertyNames);
                });
                when("The input is valid", () -> {
                    it("returns true", () -> {
                        assertThat(validator.validate(INPUT)).isTrue();
                    });
                });
                when("there are no values", () -> {
                    it("returns false", () -> {
                        assertThat(validator.validate(Collections.emptySet())).isFalse();
                    });
                });
                when("the values aren't parsed", () -> {
                    it("returns false", () -> {
                        given(parser.parse(anySetOf(String.class))).willReturn(ImmutableMap.of("a", "1"));
                        assertThat(validator.validate(INPUT)).isFalse();
                    });
                });
                when("one of the keys is a system property", () -> {
                    it("returns false", () -> {
                        given(parser.parse(anySetOf(String.class))).willReturn(ImmutableMap.of("a", "1", "x", "2"));
                        assertThat(validator.validate(INPUT)).isFalse();
                    });
                });
                when("one of the values is empty", () -> {
                    it("returns false", () -> {
                        given(parser.parse(anySetOf(String.class))).willReturn(ImmutableMap.of("a", "1", "b", ""));
                        assertThat(validator.validate(INPUT)).isFalse();
                    });
                });
            });
        });
    }
}