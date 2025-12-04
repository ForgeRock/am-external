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

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SetSessionPropertiesNodeTest {

    private TreeContext context;

    @BeforeEach
    void setUp() {
        context = new TreeContext(DEFAULT_IDM_IDENTITY_RESOURCE, json(object()), json(object()),
                json(object()), new ExternalRequestContext.Builder().build(), Collections.emptyList(), false,
                Optional.empty(), Collections.emptySet());
    }

    @Test
    void shouldSetSessionPropertiesIfProvided() {
        // given
        Map<String, String> properties = Map.of("property1", "value1", "property2", "value2");
        SetSessionPropertiesNode node = new SetSessionPropertiesNode(
                new TestConfig(properties, Optional.empty(), Optional.empty()));

        // when
        Action result = node.process(context);

        // then
        assertThat(result.sessionProperties).containsExactlyInAnyOrderEntriesOf(properties);
    }

    @Test
    void shouldSetMaxSessionTimeAndMaxIdleTimeIfProvided() {
        // given
        int timeoutMinutes = 100;
        Duration timeoutDuration = Duration.ofMinutes(timeoutMinutes);
        SetSessionPropertiesNode node = new SetSessionPropertiesNode(
                new TestConfig(emptyMap(), Optional.of(timeoutDuration), Optional.of(timeoutDuration)));

        // when
        Action result = node.process(context);

        // then
        assertThat(result.maxSessionTime).isEqualTo(Optional.of(timeoutDuration));
        assertThat(result.maxIdleTime).isEqualTo(Optional.of(timeoutDuration));
    }

    private record TestConfig(Map<String, String> properties,
                              Optional<Duration> maxSessionTime,
                              Optional<Duration> maxIdleTime)
            implements SetSessionPropertiesNode.Config {
    }
}
