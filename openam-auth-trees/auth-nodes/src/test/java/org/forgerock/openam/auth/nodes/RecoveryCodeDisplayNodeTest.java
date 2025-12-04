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
 * Copyright 2023-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.nodes.RecoveryCodeDisplayNode.RECOVERY_CODE_DEVICE_NAME;
import static org.forgerock.openam.auth.nodes.RecoveryCodeDisplayNode.RECOVERY_CODE_KEY;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.TextOutputCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.webauthn.ClientScriptUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RecoveryCodeDisplayNodeTest {

    RecoveryCodeDisplayNode recoveryCodeDisplayNode;

    @Mock
    ClientScriptUtilities clientScriptUtilities;

    @BeforeEach
    void setUp() {
        recoveryCodeDisplayNode = new RecoveryCodeDisplayNode(clientScriptUtilities);
    }

    @Test
    void assertThatRecoveryCodesAreReturnedIfTheyAreInTheTransientState() throws NodeProcessException {
        JsonValue sharedState = json(object());
        JsonValue secureState = json(object());
        JsonValue transientState = json(object(
                field(RECOVERY_CODE_KEY, JsonValue.array("banana")),
                field(RECOVERY_CODE_DEVICE_NAME, "pajama")));

        given(clientScriptUtilities.getScriptAsString(anyString())).willReturn("%s,%s,%s,%s,%s");

        Action action = recoveryCodeDisplayNode
                .process(getContext(sharedState, transientState, secureState, Collections.emptyList()));

        assertThat(action.callbacks).hasSize(1);
        var script = ((TextOutputCallback) action.callbacks.get(0)).getMessage().split(",");
        assertThat(script[0]).isEqualTo("Your Recovery Codes");
        assertThat(script[1]).isEqualTo("Use one of these codes to authenticate if you lose your device");
        assertThat(script[2]).isEqualTo(" which has been named:");
        assertThat(script[3]).isEqualTo("pajama");
        assertThat(script[4])
                .isEqualTo("You must make a copy of these recovery codes. They cannot be displayed again.");
        assertThat(script[5]).isEqualTo("banana");
    }

    @Test
    void assertThatThereAreNoCallBacksIfThereAreNoRecoveryCodes() throws NodeProcessException {
        JsonValue transientState = json(object());
        JsonValue secureState = json(object());
        JsonValue sharedState = json(object());

        Action action = recoveryCodeDisplayNode
                .process(getContext(sharedState, transientState, secureState, Collections.emptyList()));

        assertThat(action.callbacks).isEmpty();
    }

    @Test
    void assertThatThereAreNoCallbacksAfterRecoveryCodesHaveBeenDisplayedOnce() throws NodeProcessException {
        JsonValue transientState = json(object());
        JsonValue secureState = json(object());
        JsonValue sharedState = json(object(
                field(RECOVERY_CODE_KEY, JsonValue.array("banana")),
                field(RECOVERY_CODE_DEVICE_NAME, "pajama")));

        given(clientScriptUtilities.getScriptAsString(anyString())).willReturn("%s,%s,%s,%s,%s");

        Action action = recoveryCodeDisplayNode
                .process(getContext(sharedState, transientState, secureState, Collections.emptyList()));

        assertThat(action.callbacks).hasSize(1);

        Action action2 = recoveryCodeDisplayNode
                .process(getContext(sharedState, transientState, secureState, Collections.emptyList()));

        assertThat(action2.callbacks).isEmpty();
    }

    @Test
    void assertThatRecoveryCodesAreDisplayedIfTheyAreInTheSecureState() throws NodeProcessException {
        JsonValue sharedState = json(object());
        JsonValue transientState = json(object());
        JsonValue secureState = json(object(
                field(RECOVERY_CODE_KEY, JsonValue.array("banana")),
                field(RECOVERY_CODE_DEVICE_NAME, "pajama")));

        given(clientScriptUtilities.getScriptAsString(anyString())).willReturn("%s,%s,%s,%s,%s");

        Action action = recoveryCodeDisplayNode
                .process(getContext(sharedState, transientState, secureState, Collections.emptyList()));

        assertThat(action.callbacks).hasSize(1);
        var script = ((TextOutputCallback) action.callbacks.get(0)).getMessage().split(",");
        assertThat(script[0]).isEqualTo("Your Recovery Codes");
        assertThat(script[1]).isEqualTo("Use one of these codes to authenticate if you lose your device");
        assertThat(script[2]).isEqualTo(" which has been named:");
        assertThat(script[3]).isEqualTo("pajama");
        assertThat(script[4])
                .isEqualTo("You must make a copy of these recovery codes. They cannot be displayed again.");
        assertThat(script[5]).isEqualTo("banana");
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState, JsonValue secureState,
            List<? extends Callback> callbacks) {
        return new TreeContext(sharedState, transientState, secureState,
                new ExternalRequestContext.Builder().build(), callbacks,
                Optional.empty());
    }

}
