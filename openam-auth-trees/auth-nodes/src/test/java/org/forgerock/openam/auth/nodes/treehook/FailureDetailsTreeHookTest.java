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
package org.forgerock.openam.auth.nodes.treehook;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Map;

import org.forgerock.openam.auth.node.api.TreeFailureResponse;
import org.forgerock.openam.auth.nodes.SetFailureDetailsNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FailureDetailsTreeHookTest {

    @Mock
    TreeFailureResponse response;
    @Mock
    SetFailureDetailsNode.Config config;

    FailureDetailsTreeHook failureDetailsTreeHook;

    @Test
    void testAcceptDoesNothing() {
        // given
        var data = json(object());
        failureDetailsTreeHook = new FailureDetailsTreeHook(config, response, data);

        // when
        failureDetailsTreeHook.accept();

        // then
        verifyNoInteractions(config);
        verifyNoInteractions(response);
    }

    @Test
    void testAcceptFailureAddsFailureDetails() {
        // given
        var data = json(object());
        given(config.failureDetails()).willReturn(Map.of("key", "value", "key2", "value2"));

        // when
        failureDetailsTreeHook = new FailureDetailsTreeHook(config, response, data);
        failureDetailsTreeHook.acceptFailure();

        // then
        verify(response).addFailureDetail("key", "value");
        verify(response).addFailureDetail("key2", "value2");
        verify(response, never()).setCustomFailureMessage(any());
    }

    @Test
    void testAcceptFailureSetsCustomFailureMessage() {
        // given
        var data = json(object(field("message", "custom message")));

        // when
        failureDetailsTreeHook = new FailureDetailsTreeHook(config, response, data);
        failureDetailsTreeHook.acceptFailure();

        // then
        verify(response, never()).addFailureDetail(any(), any());
        verify(response).setCustomFailureMessage("custom message");
    }

    @Test
    void testAcceptFailureAddsJsonFailureDetails() {
        // given
        var data = json(object());
        given(config.failureDetails()).willReturn(Map.of("key", "{ \"key\": \"value\" }"));

        // when
        failureDetailsTreeHook = new FailureDetailsTreeHook(config, response, data);
        failureDetailsTreeHook.acceptFailure();

        // then
        verify(response).addFailureDetail("key", Map.of("key", "value"));
    }

}
