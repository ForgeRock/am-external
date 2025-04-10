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

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests the {@link FlowControlNode}.
 */
class FlowControlNodeTest {

    public static Stream<Arguments> flowControlSource() {
        return Stream.of(arguments(10, 800, 1200), arguments(0, 0, 0), arguments(100, 10_000, 10_000));
    }

    @ParameterizedTest
    @MethodSource("flowControlSource")
    public void testFlowControl(Integer percentageA, Integer expectedFlowsLower, Integer expectedFlowsUpper) {
        // given
        FlowControlNode.Config config = new FlowControlNode.Config() {
            @Override
            public int percentageA() {
                return percentageA;
            }
        };
        FlowControlNode flowControlNode = new FlowControlNode(config);

        // when
        int aCount = 0;
        for (int i = 0; i < 10_000; i++) {
            var outcome = flowControlNode.process(null).outcome;
            if ("A".equals(outcome)) {
                aCount++;
            }
        }

        // then
        assertThat(aCount).isBetween(expectedFlowsLower, expectedFlowsUpper);
    }

}
