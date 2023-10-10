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
 * Copyright 2023 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.node.api.TreeHook;
import org.forgerock.openam.auth.nodes.treehook.CreatePersistentCookieTreeHook;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SetPersistentCookieNodeTest {

    private UUID nodeId;
    private SetPersistentCookieNode setPersistentCookieNode;


    @Before
    public void setup() throws NodeProcessException {
        nodeId = UUID.randomUUID();
        SetPersistentCookieNode.Config config = () -> new char[0];
        setPersistentCookieNode = new SetPersistentCookieNode(config, nodeId);
    }

    @Test
    public void testSessionHookWrittenWithNodeId() throws NodeProcessException {

        // when
        Action action = setPersistentCookieNode.process(new TreeContext(json(object()), json(object()),
                new ExternalRequestContext.Builder().build(), List.of(), Optional.empty()));

        // then
        assertThat(action.outcome).isEqualTo("outcome");
        assertThat(action.sessionProperties).containsExactlyInAnyOrderEntriesOf(
                Map.of("persistentCookieName_session-jwt", "session-jwt"));
        assertThat(action.sessionHooks).hasSize(1);
        assertThat(action.sessionHooks.get(0).asMap()).containsExactlyInAnyOrderEntriesOf(
                Map.of(TreeHook.SESSION_HOOK_CLASS_KEY, CreatePersistentCookieTreeHook.class.getName(),
                        TreeHook.NODE_ID_KEY, nodeId.toString(),
                        TreeHook.NODE_TYPE_KEY, SetPersistentCookieNode.class.getSimpleName()));
    }
}