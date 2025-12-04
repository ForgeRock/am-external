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
 * Copyright 2018-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

import java.util.Collections;
import java.util.Optional;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 *
 */
@ExtendWith(MockitoExtension.class)
public class SocialOAuthIgnoreProfileNodeTest {

    @Mock
    private SocialOAuthIgnoreProfileNode.Config config;

    @Test
    void processReturnsTrueWhenSharedSessionIsValid() throws Exception {
        //GIVEN
        JsonValue sharedState = JsonValue.json(object(
                field("userInfo", object(
                        field("userNames", object(
                                field("test", Collections.singletonList("user"))
                        ))
                ))
        ));

        TreeContext context = new TreeContext(sharedState,
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());
        SocialOAuthIgnoreProfileNode node = new SocialOAuthIgnoreProfileNode(config);

        //WHEN
        Action process = node.process(context);

        //THEN
        assertThat(process.outcome).isEqualTo("outcome");
    }

    @Test
    void processSetUserInSessionWhenSharedSessionIsValid() throws Exception {
        //GIVEN
        JsonValue sharedState = JsonValue.json(object(
                field("userInfo", object(
                        field("userNames", object(
                                field("test", Collections.singletonList("user"))
                        ))
                ))
        ));

        TreeContext context = new TreeContext(sharedState,
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());
        SocialOAuthIgnoreProfileNode node = new SocialOAuthIgnoreProfileNode(config);

        //WHEN
        Action process = node.process(context);

        //THEN

        assertThat(process.sharedState.isDefined(USERNAME)).isTrue();
        assertThat(process.sharedState.get(USERNAME).asString()).isEqualTo("user");
    }
}
