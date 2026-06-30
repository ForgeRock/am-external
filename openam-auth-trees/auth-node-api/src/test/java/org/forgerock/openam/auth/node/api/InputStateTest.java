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
 * Copyright 2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.node.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class InputStateTest {

    @Test
    void shouldBeEqualWhenNameAndRequiredAreSame() {
        InputState input1 = new InputState("test", true);
        InputState input2 = new InputState("test", true);

        assertThat(input1).isEqualTo(input2);
        assertThat(input1.hashCode()).isEqualTo(input2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenNameIsDifferent() {
        InputState input1 = new InputState("test1", true);
        InputState input2 = new InputState("test2", true);

        assertThat(input1).isNotEqualTo(input2);
    }

    @Test
    void shouldNotBeEqualWhenRequiredIsDifferent() {
        InputState input1 = new InputState("test", true);
        InputState input2 = new InputState("test", false);

        assertThat(input1).isNotEqualTo(input2);
    }

    @Test
    void shouldNotHaveDuplicatesInSet() {
        Set<InputState> set = new HashSet<>();
        set.add(new InputState("test", true));
        set.add(new InputState("test", true));

        assertThat(set).hasSize(1);
    }
}
