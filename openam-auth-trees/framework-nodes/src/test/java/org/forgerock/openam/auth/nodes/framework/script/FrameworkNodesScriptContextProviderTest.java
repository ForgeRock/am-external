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
package org.forgerock.openam.auth.nodes.framework.script;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.auth.nodes.framework.script.FrameworkNodesGlobalScript.CONFIG_PROVIDER_NODE_SCRIPT;
import static org.forgerock.openam.auth.nodes.framework.script.FrameworkNodesScriptContext.CONFIG_PROVIDER_NODE;

import java.util.Arrays;
import java.util.List;

import org.forgerock.openam.scripting.domain.ScriptContextDetails;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FrameworkNodesScriptContextProviderTest {

    private FrameworkNodesScriptContextProvider provider;

    @Mock
    private AnnotatedServiceRegistry annotatedServiceRegistry;

    @BeforeEach
    void setUp() {
        provider = new FrameworkNodesScriptContextProvider(annotatedServiceRegistry, new ConfigProviderNodeContext());
    }

    @Test
    void shouldReturnAuthNodeScriptContext() {
        // When
        List<ScriptContextDetails> actual = provider.get();

        // Then
        assertThat(actual.size()).isEqualTo(2);

        assertThat(actual.get(0).name()).isEqualTo(CONFIG_PROVIDER_NODE.name());
        assertThat(actual.get(0).getDefaultScriptId()).isEqualTo(CONFIG_PROVIDER_NODE_SCRIPT.getId());
        assertThat(actual.get(0).getWhiteList())
                .containsAll(Arrays.asList("java.lang.String", "org.forgerock.openam.auth.node.api.Action"));

        assertThat(actual.get(1).name()).isEqualTo(ConfigProviderNodeContext.CONFIG_PROVIDER_NEXT_GEN_NAME);
    }

}
