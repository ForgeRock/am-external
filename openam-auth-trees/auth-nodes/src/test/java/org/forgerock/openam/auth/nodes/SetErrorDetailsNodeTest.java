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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Map;
import java.util.UUID;

import org.forgerock.openam.auth.node.api.LocalizedMessageProvider;
import org.forgerock.openam.auth.node.api.TreeHook;
import org.forgerock.openam.auth.nodes.treehook.ErrorDetailsTreeHook;
import org.forgerock.openam.core.realms.Realm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SetErrorDetailsNodeTest {

    @Mock
    private LocalizedMessageProvider localizedMessageProvider;
    @Mock
    private SetErrorDetailsNode.Config config;
    @Mock
    private Realm realm;
    private UUID uuid = UUID.randomUUID();

    private SetErrorDetailsNode node;

    @BeforeEach
    void setup() {
        node = new SetErrorDetailsNode(uuid, config, realm, (realm) -> localizedMessageProvider);
    }

    @Test
    void testGivenNoLocalizationsReturnsSessionHookWithNoMessage() {
        // given
        given(localizedMessageProvider.getLocalizedMessage(null, SetErrorDetailsNode.class, Map.of(), ""))
                .willReturn(null);

        // when
        var action = node.process(null);

        // then
        assertThat(action.outcome).isEqualTo("outcome");
        assertThat(action.sessionHooks).hasSize(1);
        var sessionHook = action.sessionHooks.get(0);
        assertThat(sessionHook.get(TreeHook.SESSION_HOOK_CLASS_KEY).asString())
                .isEqualTo(ErrorDetailsTreeHook.class.getName());
        assertThat(sessionHook.get(TreeHook.NODE_ID_KEY).asString()).isEqualTo(uuid.toString());
        assertThat(sessionHook.get(TreeHook.NODE_TYPE_KEY).asString())
                .isEqualTo(SetErrorDetailsNode.class.getSimpleName());
        assertThat(sessionHook.keys()).doesNotContain(TreeHook.HOOK_DATA);
    }


    @Test
    void testGivenLocalizationsReturnsSessionHookWithMessage() {
        // given
        given(localizedMessageProvider.getLocalizedMessage(null, SetErrorDetailsNode.class, Map.of(), ""))
                .willReturn("localized message");

        // when
        var action = node.process(null);

        // then
        assertThat(action.outcome).isEqualTo("outcome");
        assertThat(action.sessionHooks).hasSize(1);
        var sessionHook = action.sessionHooks.get(0);
        assertThat(sessionHook.get(TreeHook.SESSION_HOOK_CLASS_KEY).asString())
                .isEqualTo(ErrorDetailsTreeHook.class.getName());
        assertThat(sessionHook.get(TreeHook.NODE_ID_KEY).asString()).isEqualTo(uuid.toString());
        assertThat(sessionHook.get(TreeHook.NODE_TYPE_KEY).asString())
                .isEqualTo(SetErrorDetailsNode.class.getSimpleName());
        assertThat(sessionHook.get(TreeHook.HOOK_DATA).get("message").asString())
                .isEqualTo("localized message");
    }

}