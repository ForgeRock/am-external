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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2025 Ping Identity Corporation. All Rights Reserved.
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class WebAuthnOutcomeDeserializerTest {

    private WebAuthnOutcomeDeserializer deserializer = new WebAuthnOutcomeDeserializer();

    @ParameterizedTest
    @MethodSource("deserializeDataProvider")
    public void shouldDeserializeCorrectly(String input, WebAuthnOutcome expectedResult) {
        assertThat(deserializer.deserialize(input)).isEqualTo(expectedResult);
    }

    public static Stream<Arguments> deserializeDataProvider() {
        return Stream.of(
            Arguments.of("unsupported", WebAuthnOutcome.unsupported()),
            Arguments.of("ERROR::DataError:message",
                    WebAuthnOutcome.error(WebAuthnDomException.parse("DataError:message"))),
            Arguments.of("{\"error\":\"DataError:message\"}",
                    WebAuthnOutcome.error(WebAuthnDomException.parse("DataError:message"))),
            Arguments.of("rawOutcome",
                    new WebAuthnOutcome("rawOutcome", Optional.empty())),
            Arguments.of("{\"legacyData\":\"rawOutcome\"}",
                    new WebAuthnOutcome("rawOutcome", Optional.empty())),
            Arguments.of("{\"legacyData\":\"rawOutcome\",\"authenticatorAttachment\":\"attachment\"}",
                    new WebAuthnOutcome("rawOutcome", Optional.of("attachment")))
        );
    }

}
