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
 * Copyright 2025-2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.script;

import static java.util.Collections.emptyList;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.LocalizedMessageProvider;
import org.forgerock.openam.auth.node.api.ScriptedLocalizedMessage;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScriptedLocalizedMessageImplTest {

    private LocalizedMessageProvider provider;
    private ScriptedLocalizedMessage scriptedLocalizedMessage;

    @BeforeEach
    void setUp() {
        Realm realm = mock(Realm.class);
        JsonValue sharedState = json(object());
        JsonValue transientState = json(object());
        TreeContext context = new TreeContext(sharedState, transientState, new ExternalRequestContext.Builder().build(),
                emptyList(), Optional.empty());
        LocalizedMessageProvider.LocalizedMessageProviderFactory providerFactory =
                mock(LocalizedMessageProvider.LocalizedMessageProviderFactory.class);
        provider = mock(LocalizedMessageProvider.class);

        when(providerFactory.create(realm)).thenReturn(provider);

        // Directly instantiate the implementation for testing
        scriptedLocalizedMessage = new ScriptedLocalizedMessageImpl(realm, context, providerFactory);
    }

    @Test
    void shouldHandleDashInLocales() {
        Map<String, String> input = new HashMap<>();
        input.put("ja-JP", "Konnichiwa");

        scriptedLocalizedMessage.getLocalizedMessage(input);

        Map<Locale, String> expected = new HashMap<>();
        expected.put(new Locale("ja", "JP"), "Konnichiwa");

        verify(provider).getLocalizedMessage(any(), any(), eq(expected), any());
    }

    @Test
    void shouldHandleUnderscoreInLocales() {
        Map<String, String> input = new HashMap<>();
        input.put("ja_JP", "Konnichiwa");

        scriptedLocalizedMessage.getLocalizedMessage(input);

        Map<Locale, String> expected = new HashMap<>();
        expected.put(new Locale("ja", "JP"), "Konnichiwa");

        verify(provider).getLocalizedMessage(any(), any(), eq(expected), any());
    }
}
