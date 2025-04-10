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

package org.forgerock.openam.auth.node.api;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.NODE_TYPE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.forgerock.json.JsonValue;
import org.junit.jupiter.api.Test;

public class ActionTest {

    @Test
    void testAddNodeType() {
        //Given
        String nodeType = "SOME_NODE_TYPE";
        JsonValue sharedState = json(object());
        JsonValue transientState = json(object());
        TreeContext context = new TreeContext(sharedState, transientState, new ExternalRequestContext.Builder().build(),
                emptyList(), Optional.empty());

        //When
        Action action = Action.goTo("successOutcome")
                .addNodeType(context, nodeType)
                .build();

        //Then
        assertTrue(action.sharedState.isDefined(NODE_TYPE));
        assertFalse(action.sharedState.contains(NODE_TYPE));
        assertThat(action.sharedState.get(NODE_TYPE).asString()).contains(nodeType);
    }

    @Test
    void shouldSetSuspendTimeoutWhenSuspendWithTimeoutProvided() {
        // given
        Duration expectedDuration = Duration.of(10, ChronoUnit.MINUTES);

        // when
        var action = Action.suspend(arg -> null, expectedDuration).build();

        // then
        assertThat(action.suspendDuration).isEqualTo(Optional.of(expectedDuration));
    }

    @Test
    void shouldNotSetSuspendTimeoutWhenSuspendWithoutTimeoutProvided() {
        // when
        var action = Action.suspend(arg -> null).build();

        // then
        assertThat(action.suspendDuration).isEmpty();
    }
}
