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
 * Copyright 2021-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.script;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.auth.nodes.script.AuthNodesGlobalScript.DECISION_NODE_SCRIPT;
import static org.forgerock.openam.auth.nodes.script.AuthNodesScriptContext.AUTHENTICATION_TREE_DECISION_NODE;
import static org.forgerock.openam.auth.nodes.script.DeviceMatchNodeScriptContext.DEVICE_MATCH_NODE_DEFAULT_SCRIPT_ID;
import static org.forgerock.openam.auth.nodes.script.DeviceMatchNodeScriptContext.DEVICE_MATCH_NODE_NAME;
import static org.forgerock.openam.auth.nodes.script.ScriptedDecisionNodeContext.SCRIPTED_DECISION_NODE_DEFAULT_SCRIPT_ID;
import static org.forgerock.openam.auth.nodes.script.ScriptedDecisionNodeContext.SCRIPTED_DECISION_NODE_NAME;

import java.util.Arrays;
import java.util.List;

import org.forgerock.openam.scripting.domain.ScriptContextDetails;
import org.forgerock.openam.scripting.domain.ScriptException;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link AuthNodesScriptContextProvider}.
 */
@ExtendWith(MockitoExtension.class)
public class AuthNodesScriptContextProviderTest {

    @Mock
    private AnnotatedServiceRegistry annotatedServiceRegistry;

    private AuthNodesScriptContextProvider provider;

    @BeforeEach
    void setUp() throws ScriptException {
        provider = new AuthNodesScriptContextProvider(annotatedServiceRegistry, new ScriptedDecisionNodeContext(),
                new DeviceMatchNodeScriptContext());
    }

    @Test
    void shouldReturnAuthNodeScriptContext() {
        // When
        List<ScriptContextDetails> actual = provider.get();

        // Then
        assertThat(actual.size()).isEqualTo(3);

        assertThat(actual.get(0).name()).isEqualTo(AUTHENTICATION_TREE_DECISION_NODE.name());
        assertThat(actual.get(0).getDefaultScriptId()).isEqualTo(DECISION_NODE_SCRIPT.getId());
        assertThat(actual.get(0).getWhiteList())
                .containsAll(Arrays.asList("java.lang.String", "org.forgerock.openam.auth.node.api.Action"));


        assertThat(actual.get(1).name()).isEqualTo(SCRIPTED_DECISION_NODE_NAME);
        assertThat(actual.get(1).getDefaultScriptId()).isEqualTo(SCRIPTED_DECISION_NODE_DEFAULT_SCRIPT_ID);
        assertThat(actual.get(1).getWhiteList())
                .doesNotContain("org.forgerock.openam.auth.node.api.Action");

        assertThat(actual.get(2).name()).isEqualTo(DEVICE_MATCH_NODE_NAME);
        assertThat(actual.get(2).getDefaultScriptId()).isEqualTo(DEVICE_MATCH_NODE_DEFAULT_SCRIPT_ID);
        assertThat(actual.get(2).getWhiteList())
                .doesNotContain("org.forgerock.openam.auth.node.api.Action");
    }
}
