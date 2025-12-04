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
package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.stream.Stream;

import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;

@ExtendWith(MockitoExtension.class)
public class ZeroPageLoginNodeTest {

    private ZeroPageLoginNode node;
    @Mock
    private ZeroPageLoginNode.Config config;
    @Mock
    private LegacyIdentityService identityService;

    public static Stream<Arguments> headers() {
        return Stream.of(
                arguments("user"),
                arguments("pass")
        );
    }

    @BeforeEach
    void setUp() throws Exception {
        when(config.usernameHeader()).thenReturn("user");
        when(config.passwordHeader()).thenReturn("pass");
        node = new ZeroPageLoginNode(config, identityService);
    }

    @Test
    void shouldReturnFalseOutcomeWhenNoHeaders() throws Exception {
        // Given
        ListMultimap<String, String> headers = ImmutableListMultimap.of();
        JsonValue sharedState = json(object());

        // When
        Action result = node.process(getContext(headers, sharedState));

        // Then
        assertThat(result.outcome).isEqualTo("false");
        assertThat((Object) result.sharedState).isNull();
    }

    @Test
    void shouldCaptureUsernameWhenPresent() throws Exception {
        // Given
        when(config.allowWithoutReferer()).thenReturn(true);
        ListMultimap<String, String> headers = ImmutableListMultimap.of("user", "fred");
        JsonValue sharedState = json(object());

        // When
        Action result = node.process(getContext(headers, sharedState));

        // Then
        assertThat(result.outcome).isEqualTo("true");
        assertThat(result.sharedState).isNotSameAs(sharedState);
        assertThat(result.sharedState).stringAt(USERNAME).isEqualTo("fred");
        assertThat(result.sharedState).doesNotContain(PASSWORD);
    }

    @Test
    void shouldCapturePasswordWhenPresent() throws Exception {
        // Given
        when(config.allowWithoutReferer()).thenReturn(true);
        ListMultimap<String, String> headers = ImmutableListMultimap.of("pass", "secure");
        JsonValue sharedState = json(object());

        // When
        Action result = node.process(getContext(headers, sharedState));

        // Then
        assertThat(result.outcome).isEqualTo("true");
        assertThat(result.sharedState).isNotSameAs(sharedState);
        assertThat(result.transientState.get(PASSWORD).getObject()).isEqualTo(("secure"));
        assertThat(result.sharedState).doesNotContain(USERNAME);
    }

    @Test
    void shouldCaptureUsernameAndPasswordWhenPresent() throws Exception {
        // Given
        when(config.allowWithoutReferer()).thenReturn(true);
        ListMultimap<String, String> headers = ImmutableListMultimap.of("pass", "secure", "user", "fred");
        JsonValue sharedState = json(object());

        // When
        Action result = node.process(getContext(headers, sharedState));

        // Then
        assertThat(result.outcome).isEqualTo("true");
        assertThat(result.sharedState).isNotSameAs(sharedState);
        assertThat(result.sharedState).stringAt(USERNAME).isEqualTo("fred");
        assertThat(result.transientState.get(PASSWORD).getObject()).isEqualTo("secure");
    }

    @Test
    void shouldCaptureEncodedUsernameAndPasswordWhenPresent() throws Exception {
        // Given
        when(config.allowWithoutReferer()).thenReturn(true);
        ListMultimap<String, String> headers = ImmutableListMultimap.of("pass", "=?UTF-8?B?c2VjdXJl?=",
                "user", "=?UTF-8?B?ZnJlZA==?=");
        JsonValue sharedState = json(object());

        // When
        Action result = node.process(getContext(headers, sharedState));

        // Then
        assertThat(result.outcome).isEqualTo("true");
        assertThat(result.sharedState).isNotSameAs(sharedState);
        assertThat(result.sharedState).stringAt(USERNAME).isEqualTo("fred");
        assertThat(result.transientState.get(PASSWORD).getObject()).isEqualTo("secure");
    }

    @ParameterizedTest
    @MethodSource("headers")
    public void shouldThrowExceptionWhenHeadersAreMultivalued(String header) {
        // Given
        when(config.allowWithoutReferer()).thenReturn(true);
        ListMultimap<String, String> headers = ImmutableListMultimap.of(header, "value1", header, "value2");
        JsonValue sharedState = json(object());

        // When
        assertThatThrownBy(() -> node.process(getContext(headers, sharedState)))
                // Then
                .isInstanceOf(NodeProcessException.class)
                .hasMessage("Expecting only one header value for username and/or password but size is2");
    }

    private TreeContext getContext(ListMultimap<String, String> headers, JsonValue sharedState) {
        return new TreeContext(sharedState, new Builder().headers(headers).build(), emptyList(), Optional.empty());
    }
}
