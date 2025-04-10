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
 * Copyright 2017-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.authentication.modules.persistentcookie.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sun.identity.shared.encode.Base64;

public class SigningKeyValidatorTest {
    private SigningKeyValidator testValidator;

    private static Stream<Arguments> signingKeyData() {
        return Stream.of(
                // non Base64 values are invalid
                Arguments.of(Collections.singleton("*&(*£&(&$£$£(**!%£"), false),
                // Value smaller than 256-bit is invalid
                Arguments.of(Collections.singleton(Base64.encode(new byte[31])), false),
                // Correct 256-bit value
                Arguments.of(Collections.singleton(Base64.encode(new byte[32])), true),
                // larger value should still be accepted
                Arguments.of(Collections.singleton(Base64.encode(new byte[33])), true)
        );
    }

    @BeforeEach
    void createValidator() {
        testValidator = new SigningKeyValidator();
    }

    @ParameterizedTest
    @MethodSource("signingKeyData")
    public void validate(Set<String> value, boolean expectedResult) {
        assertThat(testValidator.validate(value)).isEqualTo(expectedResult);
    }
}
