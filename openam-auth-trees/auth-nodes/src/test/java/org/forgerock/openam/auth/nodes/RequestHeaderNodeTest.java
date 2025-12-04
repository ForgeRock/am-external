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

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.ServiceConfigException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;

@ExtendWith(MockitoExtension.class)
public class RequestHeaderNodeTest {

    @Mock
    private RequestHeaderNode.Config config;

    @InjectMocks
    private RequestHeaderNode headerNode;

    private TreeContext context;

    @Test
    void shouldProcessAllowedHeaders() throws Exception {

        // Given
        given(config.allowedHeaders()).willReturn(Map.of(
                "brand-color", "sharedStateBrandColor",
                "empty-colors", "sharedStateEmptyColors"
        ));

        ListMultimap<String, String> headers = ImmutableListMultimap.of(
                "brand-color", "green",
                "empty-colors", "",
                "brand-color", "yellow"
        );

        context = new TreeContext(json(object()), new ExternalRequestContext.Builder().headers(headers).build(),
                emptyList(), Optional.empty());

        // When
        headerNode.process(context);

        // Then
        assertThat(context.getStateFor(headerNode).get("sharedStateBrandColor").asList())
                .isEqualTo(List.of("green", "yellow"));
        assertThat(context.getStateFor(headerNode).get("sharedStateEmptyColors").asList())
                .isEqualTo(List.of(""));
    }

    @Test
    void shouldProcessAllowedAndDelimitedHeaders() throws Exception {

        // Given
        given(config.allowedHeaders()).willReturn(Map.of(
                "brand-colors-delimited", "sharedStateBrandColorsDelimited"
                ));
        given(config.headersToBeDelimited()).willReturn(Set.of(
                "brand-colors-delimited"
        ));

        ListMultimap<String, String> headers = ImmutableListMultimap.of(
                "brand-colors-delimited", "green,blue"
        );

        context = new TreeContext(json(object()), new ExternalRequestContext.Builder().headers(headers).build(),
                emptyList(), Optional.empty());

        // When
        headerNode.process(context);

        // Then
        assertThat(context.getStateFor(headerNode).get("sharedStateBrandColorsDelimited").asList())
                .isEqualTo(List.of("green", "blue"));

    }

    @Test
    void shouldProcessGivenNonAllowedHeader() throws Exception {

        // Given
        given(config.allowedHeaders()).willReturn(Map.of("brand-color", "sharedStateBrandColor"));

        ListMultimap<String, String> headers = ImmutableListMultimap.of(
                "branding-color", "green"
        );

        context = new TreeContext(json(object()), new ExternalRequestContext.Builder().headers(headers).build(),
                emptyList(), Optional.empty());

        // When
        headerNode.process(context);

        // Then
        assertThat(context.getStateFor(headerNode).get("sharedStateBrandColor").asList())
                .isEqualTo(emptyList());
    }

    @Test
    void shouldProcessGivenEmptyHeaders() throws Exception {

        // Given
        given(config.allowedHeaders()).willReturn(Map.of("brand-color", "sharedStateBrandColor"));

        ListMultimap<String, String> headers = ImmutableListMultimap.of();

        context = new TreeContext(json(object()), new ExternalRequestContext.Builder().headers(headers).build(),
                emptyList(), Optional.empty());

        // When
        headerNode.process(context);

        // Then
        assertThat(context.getStateFor(headerNode).get("sharedStateBrandColor").asList())
                .isEqualTo(emptyList());
    }

    @Test
    void shouldFailToValidateGivenEmptyConfig() {

        // Given
        Map<String, Set<String>> config = Map.of(
                "allowedHeaders", emptySet(),
                "headersToBeDelimited", emptySet()
        );

        RequestHeaderNode.RequestHeaderNodeValidator validator = new RequestHeaderNode.RequestHeaderNodeValidator();
        List<String> configPath = singletonList(UUID.randomUUID().toString());

        // When
        // Then
        assertThatExceptionOfType(ServiceConfigException.class)
                .isThrownBy(() -> validator.validate(mock(Realm.class), configPath, config))
                .withMessage("No headers configured - this node is redundant");
    }

    @Test
    void shouldFailToValidateGivenIncorrectlyConfiguredHeaderDoDelimit() {

        // Given
        Map<String, Set<String>> config = Map.of(
                "allowedHeaders", Set.of("[brand-color]=sharedStateBrandColor"),
                "headersToBeDelimited", Set.of("branding-color")
        );

        RequestHeaderNode.RequestHeaderNodeValidator validator = new RequestHeaderNode.RequestHeaderNodeValidator();
        List<String> configPath = singletonList(UUID.randomUUID().toString());

        // When
        // Then
        assertThatExceptionOfType(ServiceConfigException.class)
                .isThrownBy(() -> validator.validate(mock(Realm.class), configPath, config))
                .withMessage("Invalid header to be delimited configured");
    }

}
