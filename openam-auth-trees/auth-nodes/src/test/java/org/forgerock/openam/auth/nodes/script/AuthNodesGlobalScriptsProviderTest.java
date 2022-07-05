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
import static org.forgerock.openam.auth.nodes.script.AuthNodesGlobalScript.DECISION_NODE_SCRIPT;
import static org.forgerock.openam.auth.nodes.script.AuthNodesGlobalScript.DEVICE_PROFILE_MATCH_DECISION_NODE_SCRIPT;
import static org.forgerock.openam.auth.nodes.script.AuthNodesGlobalScript.CONFIG_PROVIDER_NODE_SCRIPT;

import java.util.List;

import org.forgerock.openam.scripting.domain.ScriptContext;
import org.forgerock.openam.scripting.domain.ScriptException;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.forgerock.openam.scripting.domain.Script;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for {@link AuthNodesGlobalScriptsProvider}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AuthNodesGlobalScriptsProviderTest {

    @InjectMocks
    private AuthNodesGlobalScriptsProvider provider;

    @Test
    public void shouldReturnAuthNodesGlobalScripts() throws ScriptException {
        // When
        List<Script> actual = provider.get();

        // Then
        assertThat(actual.size()).isEqualTo(3);
        assertScriptConfiguration(actual.get(0), DECISION_NODE_SCRIPT.getId(), DECISION_NODE_SCRIPT.getContext());
        assertScriptConfiguration(actual.get(1), DEVICE_PROFILE_MATCH_DECISION_NODE_SCRIPT.getId(),
                DEVICE_PROFILE_MATCH_DECISION_NODE_SCRIPT.getContext());
        assertScriptConfiguration(actual.get(2), CONFIG_PROVIDER_NODE_SCRIPT.getId(),
                CONFIG_PROVIDER_NODE_SCRIPT.getContext());
    }

    private void assertScriptConfiguration(Script script, String id, ScriptContext context) {
        assertThat(script.getId()).isEqualTo(id);
        assertThat(script.getContext()).isEqualTo(context);
        assertThat(script.getLanguage()).isEqualTo(ScriptingLanguage.JAVASCRIPT);
    }

}