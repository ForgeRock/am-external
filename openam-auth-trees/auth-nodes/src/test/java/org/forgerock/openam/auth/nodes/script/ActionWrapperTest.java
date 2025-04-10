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
 * Copyright 2023-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.script;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.IdentifiedIdentity;
import org.junit.jupiter.api.Test;

import com.sun.identity.idm.IdType;

public class ActionWrapperTest {

    private ActionWrapper actionWrapper;

    @Test
    void testIdentifiedUserAddedCorrectly() {
        // given
        actionWrapper = new ActionWrapper().withIdentifiedUser("demo-user")
                                .goTo("outcome");

        // when
        Action action = actionWrapper.buildAction();

        // then
        assertThat(action.identifiedIdentity).contains(new IdentifiedIdentity("demo-user", IdType.USER));
    }

    @Test
    void testIdentifiedAgentAddedCorrectly() {
        // given
        actionWrapper = new ActionWrapper().withIdentifiedAgent("demo-agent")
                                .goTo("outcome");

        // when
        Action action = actionWrapper.buildAction();

        // then
        assertThat(action.identifiedIdentity).contains(new IdentifiedIdentity("demo-agent", IdType.AGENT));
    }

    @Test
    void testSuspendDurationAddedCorrectly() {
        // given
        actionWrapper = new ActionWrapper().suspend("text", (a) -> { }, 3)
                                .goTo("outcome");

        // when
        Action action = actionWrapper.buildAction();

        // then
        assertThat(action.suspendDuration).contains(Duration.ofMinutes(3));
    }

    @Test
    void testSuspendDurationNullIfSuspendMethodWithoutDurationUsed() {
        // given
        actionWrapper = new ActionWrapper().suspend("text")
                                .goTo("outcome");

        // when
        Action action = actionWrapper.buildAction();

        // then
        assertThat(action.suspendDuration).isEmpty();
    }

}
