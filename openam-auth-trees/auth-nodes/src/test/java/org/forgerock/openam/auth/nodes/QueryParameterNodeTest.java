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
 * Copyright 2023-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;

@ExtendWith(MockitoExtension.class)
public class QueryParameterNodeTest {

    @Mock
    private QueryParameterNode.Config config;

    @InjectMocks
    private QueryParameterNode queryParameterNode;

    private TreeContext context;

    @Test
    void shouldProcessAllowedQueryParameters() throws Exception {

        // Given
        given(config.allowedQueryParameters()).willReturn(Map.of(
                "brandColor", "sharedStateBrandColor",
                "emptyColors", "sharedStateEmptyColors"
        ));

        Map<String, String[]> params = Map.of(
                "brandColor", new String[]{"green"},
                "emptyColors", new String[]{}
        );

        context = new TreeContext(json(object()), new ExternalRequestContext.Builder().parameters(params).build(),
                emptyList(), Optional.empty());

        // When
        queryParameterNode.process(context);

        // Then
        assertThat(context.getStateFor(queryParameterNode).get("sharedStateBrandColor").asList())
                .isEqualTo(List.of("green"));
        assertThat(context.getStateFor(queryParameterNode).get("sharedStateEmptyColors").asList())
                .isEqualTo(emptyList());
    }

    @Test
    void shouldProcessAllowedAndDelimitedQueryParameters() throws Exception {

        // Given
        given(config.allowedQueryParameters()).willReturn(Map.of(
                "brandColorsDelimited", "sharedStateBrandColorsDelimited"
                ));
        given(config.queryParametersToBeDelimited()).willReturn(Set.of(
                "brandColorsDelimited"
        ));

        Map<String, String[]> params = Map.of(
                "brandColorsDelimited", new String[]{"green,blue"}
        );

        context = new TreeContext(json(object()), new ExternalRequestContext.Builder().parameters(params).build(),
                emptyList(), Optional.empty());

        // When
        queryParameterNode.process(context);

        // Then
        assertThat(context.getStateFor(queryParameterNode).get("sharedStateBrandColorsDelimited").asList())
                .isEqualTo(List.of("green", "blue"));

    }

    @Test
    void shouldProcessGivenNonAllowedQueryParameter() throws Exception {

        // Given
        given(config.allowedQueryParameters()).willReturn(Map.of("brandColor", "sharedStateBrandColor"));

        Map<String, String[]> params = Map.of("brandingColor", new String[]{"green"});

        context = new TreeContext(json(object()), new ExternalRequestContext.Builder().parameters(params).build(),
                emptyList(), Optional.empty());

        // When
        queryParameterNode.process(context);

        // Then
        assertThat(context.getStateFor(queryParameterNode).get("sharedStateBrandColor").asList())
                .isEqualTo(emptyList());
    }

    @Test
    void shouldProcessGivenEmptyQueryParameters() throws Exception {

        // Given
        given(config.allowedQueryParameters()).willReturn(Map.of("brandColor", "sharedStateBrandColor"));

        Map<String, String[]> params = emptyMap();

        context = new TreeContext(json(object()), new ExternalRequestContext.Builder().parameters(params).build(),
                emptyList(), Optional.empty());

        // When
        queryParameterNode.process(context);

        // Then
        assertThat(context.getStateFor(queryParameterNode).get("sharedStateBrandColor").asList())
                .isEqualTo(emptyList());
    }

    @Test
    void shouldProcessGivenURLEncodedQueryParameters() throws Exception {

        // Given
        given(config.allowedQueryParameters()).willReturn(Map.of(
                "brandColor", "sharedStateBrandColor",
                "brandColorsDelimited", "sharedStateBrandColorsDelimited"));

        given(config.queryParametersToBeDelimited()).willReturn(Set.of(
                "brandColorsDelimited"
        ));

        Map<String, String[]> params = Map.of(
                "brandColor", new String[]{"green%2Cred%2Cyellow,brown%2Bviolet+grey"},
                "brandColorsDelimited", new String[]{"purple%2Corange%2Cpink%26cyan%2Fblue%5Emint"});

        context = new TreeContext(json(object()), new ExternalRequestContext.Builder().parameters(params).build(),
                emptyList(), Optional.empty());

        // When
        queryParameterNode.process(context);

        // Then
        assertThat(context.getStateFor(queryParameterNode).get("sharedStateBrandColor").asList())
                .isEqualTo(List.of("green,red,yellow,brown+violet grey"));

        assertThat(context.getStateFor(queryParameterNode).get("sharedStateBrandColorsDelimited").asList())
                .isEqualTo(List.of("purple", "orange", "pink&cyan/blue^mint"));
    }

    @Test
    void shouldFailToValidateGivenEmptyConfig() {

        // Given
        Map<String, Set<String>> config = Map.of(
                "allowedQueryParameters", emptySet(),
                "queryParametersToBeDelimited", emptySet()
        );

        QueryParameterNode.QueryParameterNodeValidator validator = new QueryParameterNode.QueryParameterNodeValidator();
        List<String> configPath = singletonList(UUID.randomUUID().toString());

        // When
        // Then
        assertThatExceptionOfType(ServiceConfigException.class)
                .isThrownBy(() -> validator.validate(mock(Realm.class), configPath, config))
                .withMessage("No parameters configured - this node is redundant");
    }

    @Test
    void shouldFailToValidateGivenIncorrectlyConfiguredParamDoDelimit() {

        // Given
        Map<String, Set<String>> config = Map.of(
                "allowedQueryParameters", Set.of("[brandColor]=green"),
                "queryParametersToBeDelimited", Set.of("brandingColor")
        );

        QueryParameterNode.QueryParameterNodeValidator validator = new QueryParameterNode.QueryParameterNodeValidator();
        List<String> configPath = singletonList(UUID.randomUUID().toString());

        // When
        // Then
        assertThatExceptionOfType(ServiceConfigException.class)
                .isThrownBy(() -> validator.validate(mock(Realm.class), configPath, config))
                .withMessage("Invalid parameter to be delimited configured");
    }

}
