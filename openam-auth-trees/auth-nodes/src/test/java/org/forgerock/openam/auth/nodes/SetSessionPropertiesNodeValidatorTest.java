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
package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.forgerock.openam.configuration.MapValueParser;
import org.forgerock.openam.sm.ServiceConfigException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SetSessionPropertiesNodeValidatorTest {

    private static final String SYSTEM_PROPERTY = "systemProperty";

    private SetSessionPropertiesNodeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SetSessionPropertiesNodeValidator(new MapValueParser(), List.of(SYSTEM_PROPERTY));
    }

    @Test
    void shouldSucceedIfInputIsValid() {
        Map<String, Set<String>> attributes = Map.of(
                "properties", Set.of("[property1]=thing1", "[property2]=thing2"),
                "maxSessionTime", Set.of("123"),
                "maxIdleTime", Set.of("123")
        );

        assertThatNoException().isThrownBy(() -> validator.validate(null, emptyList(), attributes));
    }

    @Test
    void shouldSucceedIfOnlyPropertiesArePresent() {
        Map<String, Set<String>> attributes = Map.of(
                "properties", Set.of("[property1]=thing1", "[property2]=thing2")
        );

        assertThatNoException().isThrownBy(() -> validator.validate(null, emptyList(), attributes));
    }

    @Test
    void shouldSucceedIfOnlyMaxSessionTimeIsPresent() {
        Map<String, Set<String>> attributes = Map.of(
                "maxSessionTime", Set.of("123")
        );

        assertThatNoException().isThrownBy(() -> validator.validate(null, emptyList(), attributes));
    }

    @Test
    void shouldSucceedIfOnlyMaxIdleTimeIsPresent() {
        Map<String, Set<String>> attributes = Map.of(
                "maxIdleTime", Set.of("123")
        );

        assertThatNoException().isThrownBy(() -> validator.validate(null, emptyList(), attributes));
    }

    @Test
    void shouldFailIfAllPropertiesAreEmpty() {
        Map<String, Set<String>> attributes = Map.of(
                "properties", emptySet(),
                "maxSessionTime", emptySet(),
                "maxIdleTime", emptySet()
        );

        assertThatThrownBy(() -> validator.validate(null, emptyList(), attributes))
                .isInstanceOf(ServiceConfigException.class)
                .hasMessage("At least one of the following properties must be set: properties, maxSessionTime, "
                        + "maxIdleTime");
    }

    @Test
    void shouldFailIfOneOfThePropertyKeysIsASystemProperty() {
        Map<String, Set<String>> attributes = Map.of(
                "properties", Set.of(String.format("[%s]=thing1", SYSTEM_PROPERTY), "[property2]=thing2"),
                "maxSessionTime", emptySet(),
                "maxIdleTime", emptySet()
        );

        assertThatThrownBy(() -> validator.validate(null, emptyList(), attributes))
                .isInstanceOf(ServiceConfigException.class)
                .hasMessage(String.format("Provided properties cannot include system session properties: [%s]",
                        SYSTEM_PROPERTY));
    }

    @Test
    void shouldFailIfOneOfThePropertyValuesIsEmpty() {
        Map<String, Set<String>> attributes = Map.of(
                "properties", Set.of("[property1]=thing1", "[property2]="),
                "maxSessionTime", emptySet(),
                "maxIdleTime", emptySet()
        );

        assertThatThrownBy(() -> validator.validate(null, emptyList(), attributes))
                .isInstanceOf(ServiceConfigException.class)
                .hasMessage("Session property values cannot be empty");
    }
}
