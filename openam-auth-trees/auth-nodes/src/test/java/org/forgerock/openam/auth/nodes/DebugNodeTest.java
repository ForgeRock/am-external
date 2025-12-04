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
 * Copyright 2022-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.TextOutputCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.authentication.callbacks.ScriptTextOutputCallback;

@ExtendWith(MockitoExtension.class)
public class DebugNodeTest {

    @InjectMocks
    DebugNode node;
    @Mock
    private DebugNode.Config config;

    private TreeContext getContext(List<? extends Callback> callbacks, JsonValue sharedState) {
        return new TreeContext(sharedState, new Builder().build(), callbacks, Optional.empty());
    }

    private TreeContext getContext(List<? extends Callback> callbacks, JsonValue sharedState,
            JsonValue transientState, Optional<String> universalId) {
        return new TreeContext(sharedState, transientState, new Builder().build(), callbacks, universalId);
    }

    private Boolean contains(String searchString, String value) {
        int index = searchString.indexOf(value);
        return index > 0;
    }

    @Test
    void testProcessWithNoCallbacksReturnsAScriptAndTextCallback() throws Exception {
        // Given
        when(config.popupEnabled()).thenReturn(true);
        JsonValue sharedState = json(object());

        // When
        Action result = node.process(getContext(emptyList(), sharedState));

        // Then
        assertThat(result.outcome).isNull();
        assertThat(result.callbacks).hasSize(3);
        assertThat(result.callbacks.get(0)).isInstanceOf(ScriptTextOutputCallback.class);
        assertThat(result.callbacks.get(1)).isInstanceOf(ScriptTextOutputCallback.class);
        assertThat(result.callbacks.get(2)).isInstanceOf(TextOutputCallback.class);
        assertThat((Object) result.sharedState).isNull();
    }

    @Test
    void testProcessWithCallbacksGoesToOutcome() throws Exception {
        // Given
        JsonValue sharedState = json(object());
        TextOutputCallback textCallback = new TextOutputCallback(TextOutputCallback.INFORMATION, "Debug Step");
        List<Callback> callbacks = new ArrayList<>();
        callbacks.add(textCallback);

        // When
        Action result = node.process(getContext(callbacks, sharedState));

        // Then
        assertThat(result.outcome).isEqualTo("outcome");
        assertThat(result.callbacks).isEmpty();
        assertThat((Object) result.sharedState).isNull();
    }

    @Test
    void testScriptOutputCallbackContainsLogInformation() throws Exception {
        // Given
        when(config.popupEnabled()).thenReturn(true);
        JsonValue sharedState = json(object(
                field("username", "demo")
        ));
        JsonValue transientState = json(object(
                field("password", "Ch4ng31t")
        ));
        Optional<String> universalId = Optional.of("id=demo,ou=user,dc=openam,dc=forgerock,dc=org");


        // When
        List<Callback> callbacks = node.process(getContext(emptyList(), sharedState,
                transientState, universalId)).callbacks;

        // Then
        ScriptTextOutputCallback callback = (ScriptTextOutputCallback) callbacks.get(1);
        String output = callback.getMessage();

        assertThat(contains(output, "Testing")).isFalse();
        assertThat(contains(output, "\"username\": \"demo\"")).isTrue();
        assertThat(contains(output, "\"password\": \"Ch4ng31t\"")).isTrue();
        assertThat(contains(output, "\"transactionId\"")).isTrue();
        assertThat(contains(output, "\"universalId\": \"" + universalId.get() + "\"")).isTrue();
    }

    @Test
    void testScriptOutputCallbackContainsCreatePopupWhenEnabled() throws Exception {
        // Given
        when(config.popupEnabled()).thenReturn(true);
        JsonValue sharedState = json(object());

        // When
        List<Callback> callbacks = node.process(getContext(emptyList(), sharedState)).callbacks;

        // Then
        ScriptTextOutputCallback callback = (ScriptTextOutputCallback) callbacks.get(1);
        String output = callback.getMessage();

        assertThat(contains(output, "createPopup()")).isTrue();
    }

    @Test
    void testScriptOutputCallbackDoesntContainCreatePopupWhenDisabled() throws Exception {
        // Given
        when(config.popupEnabled()).thenReturn(true);
        JsonValue sharedState = json(object());

        // When
        when(config.popupEnabled()).thenReturn(false);
        DebugNode node = new DebugNode(config);
        List<Callback> callbacks = node.process(getContext(emptyList(), sharedState)).callbacks;

        // Then
        ScriptTextOutputCallback callback = (ScriptTextOutputCallback) callbacks.get(1);
        String output = callback.getMessage();

        assertThat(contains(output, "createPopup()")).isFalse();
    }
}
