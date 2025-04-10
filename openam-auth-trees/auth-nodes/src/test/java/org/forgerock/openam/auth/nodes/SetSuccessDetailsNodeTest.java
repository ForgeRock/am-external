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
 * Copyright 2024-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.forgerock.openam.auth.node.api.TreeHook;
import org.forgerock.openam.auth.nodes.SetSuccessDetailsNode.ResponseBodyPredefinedKeysValidator;
import org.forgerock.openam.auth.nodes.treehook.SuccessDetailsTreeHook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SetSuccessDetailsNodeTest {

    @Test
    void shouldReturnActionWithSuccessDetailsHookWhenProcessCalled() {
        // given
        UUID uuid = UUID.randomUUID();
        SetSuccessDetailsNode node = new SetSuccessDetailsNode(uuid);

        // when
        var action = node.process(null);

        // then
        assertThat(action.outcome).isEqualTo("outcome");
        assertThat(action.sessionHooks).hasSize(1);
        var sessionHook = action.sessionHooks.get(0);
        assertThat(sessionHook.get(TreeHook.SESSION_HOOK_CLASS_KEY).asString())
                .isEqualTo(SuccessDetailsTreeHook.class.getName());
        assertThat(sessionHook.get(TreeHook.NODE_ID_KEY).asString()).isEqualTo(uuid.toString());
        assertThat(sessionHook.get(TreeHook.NODE_TYPE_KEY).asString())
                .isEqualTo(SetSuccessDetailsNode.class.getSimpleName());
        assertThat(sessionHook.keys()).doesNotContain(TreeHook.HOOK_DATA);
    }



    static Stream<Arguments> validatorInputs() {
        return Stream.of(
                arguments(Set.of("[key1]=value1", "[key2]=value2"), true),
                arguments(Set.of("[key1]=value1", "[realm]=/"), false),
                arguments(Set.of("[key1]=value1", "[tokenId]=1234"), false),
                arguments(Set.of("[key1]=value1", "[successUrl]=/profile"), false)
        );
    }

    @ParameterizedTest
    @MethodSource("validatorInputs")
    void shouldOnlyRejectPredefinedKeysWhenValidateCalled(Set<String> arguments, boolean expectedResult) {
        // given
        ResponseBodyPredefinedKeysValidator validator = new ResponseBodyPredefinedKeysValidator();

        // when
        boolean result = validator.validate(arguments);

        // then
        assertThat(result).isEqualTo(expectedResult);

    }

}
