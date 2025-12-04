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
 * Copyright 2024-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.treehook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.forgerock.openam.auth.node.api.TreeHookException;
import org.forgerock.openam.test.extensions.LoggerExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ch.qos.logback.classic.spi.ILoggingEvent;

class DetailsTreeHookTest {

    @RegisterExtension
    LoggerExtension loggerExtension = new LoggerExtension(DetailsTreeHook.class);

    public static Stream<Arguments> parameters() {
        return Stream.of(arguments("{}", Map.of()), arguments("[]", List.of()), arguments("null", null),
                arguments("true", true), arguments("false", false), arguments("1", 1), arguments("1.0", 1.0),
                arguments("test", "test"), arguments("{ \"key\": \"value\" }", Map.of("key", "value")));
    }

    @ParameterizedTest
    @MethodSource("parameters")
    void testConvertStringToJson(String input, Object output) {
        // given
        DetailsTreeHook detailsTreeHook = new MyDetailsTreeHook();

        // when
        Object result = detailsTreeHook.convertStringToJson(input);

        // then
        assertThat(result).isEqualTo(output);
    }

    @Test
    void testConvertStringToJsonWithInvalidJson() {
        // given
        DetailsTreeHook detailsTreeHook = new MyDetailsTreeHook();

        // when
        Object result = detailsTreeHook.convertStringToJson("{");

        // then
        assertThat(result).isEqualTo("{");
        assertThat(loggerExtension.getDebug(ILoggingEvent::getFormattedMessage))
                .containsExactly("Failed to parse JSON value: {");
    }


    private static class MyDetailsTreeHook extends DetailsTreeHook {

        @Override
        public void accept() throws TreeHookException {
            // do nothing
        }
    }

}
