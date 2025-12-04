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
 * Copyright 2016-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.authentication.modules.fr.oath.validators;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class AlphaNumericValidatorTest {

    private final AlphaNumericValidator validator = new AlphaNumericValidator();

    private static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of("asdf", true),
                Arguments.of("123", true),
                Arguments.of("asdf1234", true),
                Arguments.of(",123", false),
                Arguments.of("/w 15 you jailor?", false),
                Arguments.of("12/!@£%awefoun@weg", false),
                Arguments.of("", false)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void checkCorrectness(String name, boolean expected) {
        //given

        //when
        boolean result = validator.validate(Collections.singleton(name));

        //then
        assertThat(result).isEqualTo(expected);
    }

}
