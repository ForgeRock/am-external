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
 * Copyright 2021 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.script;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.auth.nodes.script.AuthNodesGlobalScript.CONFIG_PROVIDER_NODE_SCRIPT;
import static org.forgerock.openam.auth.nodes.script.AuthNodesGlobalScript.DECISION_NODE_SCRIPT;
import static org.forgerock.openam.auth.nodes.script.AuthNodesScriptContext.AUTHENTICATION_TREE_DECISION_NODE;
import static org.forgerock.openam.auth.nodes.script.AuthNodesScriptContext.CONFIG_PROVIDER_NODE;

import java.util.Arrays;
import java.util.List;

import org.forgerock.openam.scripting.domain.ScriptContextDetails;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for {@link AuthNodesScriptContextProvider}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AuthNodesScriptContextProviderTest {

    @InjectMocks
    private AuthNodesScriptContextProvider provider;

    @Test
    public void shouldReturnAuthNodeScriptContext() {
        // When
        List<ScriptContextDetails> actual = provider.get();

        // Then
        assertThat(actual.size()).isEqualTo(2);

        assertThat(actual.get(0).getName()).isEqualTo(AUTHENTICATION_TREE_DECISION_NODE.name());
        assertThat(actual.get(0).getDefaultScriptId()).isEqualTo(DECISION_NODE_SCRIPT.getId());
        assertThat(actual.get(0).getWhiteList())
                .containsAll(Arrays.asList("java.lang.String", "org.forgerock.openam.auth.node.api.Action"));

        assertThat(actual.get(1).getName()).isEqualTo(CONFIG_PROVIDER_NODE.name());
        assertThat(actual.get(1).getDefaultScriptId()).isEqualTo(CONFIG_PROVIDER_NODE_SCRIPT.getId());
        assertThat(actual.get(1).getWhiteList())
                .containsAll(Arrays.asList("java.lang.String", "org.forgerock.openam.auth.node.api.Action"));
    }
}