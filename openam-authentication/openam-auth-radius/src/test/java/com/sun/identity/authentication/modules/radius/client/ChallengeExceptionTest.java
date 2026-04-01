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
package com.sun.identity.authentication.modules.radius.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.forgerock.openam.radius.common.AccessChallenge;
import org.forgerock.openam.radius.common.AttributeSet;
import org.forgerock.openam.radius.common.ReplyMessageAttribute;
import org.forgerock.openam.radius.common.StateAttribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ChallengeException}.
 * Verifies correct behavior for attribute retrieval and edge cases.
 */
class ChallengeExceptionTest {

    private AccessChallenge accessChallenge;
    private StateAttribute stateAttribute;

    @BeforeEach
    void setUp() {
        stateAttribute = new StateAttribute("testState");
        accessChallenge = new AccessChallenge();

        accessChallenge.addAttribute(stateAttribute);
        accessChallenge.addAttribute(new ReplyMessageAttribute("replyMessage"));
    }

    @Test
    void shouldGetAttributeSetGivenAccessChallengeAndAttributeSet() {
        // Given
        ChallengeException exception = new ChallengeException(accessChallenge);

        // When
        AttributeSet result = exception.getAttributeSet();

        // Then
        assertThat(result).isEqualTo(accessChallenge.getAttributeSet());
    }

    @Test
    void shouldGetStateAttributeGivenAccessChallengeAndStateAttribute() {
        // Given
        ChallengeException exception = new ChallengeException(accessChallenge);

        // When
        StateAttribute result = exception.getStateAttribute();

        // Then
        assertThat(result).isEqualTo(stateAttribute);
    }

    @Test
    void shouldGetStateGivenAccessChallengeAndStateAttribute() {
        // Given
        ChallengeException exception = new ChallengeException(accessChallenge);

        // When
        String state = exception.getState();

        // Then
        assertThat(state).isEqualTo("testState");
    }

    @Test
    void shouldGetReplyMessageGivenAccessChallengeAndReplyMessageAttribute() {
        // Given
        ChallengeException exception = new ChallengeException(accessChallenge);

        // When
        String replyMessage = exception.getReplyMessage();

        // Then
        assertThat(replyMessage).isEqualTo("replyMessage");
    }

    @Test
    void shouldGetEmptyStringGivenAccessChallengeAndNoReplyMessageAttribute() {
        // Given
        ChallengeException exception = new ChallengeException(new AccessChallenge());

        // When
        String replyMessage = exception.getReplyMessage();

        // Then
        assertThat(replyMessage).isEmpty();
    }
}