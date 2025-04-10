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
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.ONE_TIME_PASSWORD;
import static org.forgerock.openam.auth.nodes.TreeContextFactory.emptyTreeContext;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.InvalidKeyException;
import java.security.SecureRandom;

import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.authentication.modules.hotp.HOTPAlgorithm;

@ExtendWith(MockitoExtension.class)
public class OneTimePasswordGeneratorNodeTest {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    @Mock
    OneTimePasswordGeneratorNode.Config serviceConfig;
    HOTPAlgorithm otpGenerator = new HOTPAlgorithm();
    private final ArgumentMatcher<byte[]> anyByteArrayMatcher = bytes -> true;

    @Test
    void shouldGenerateOneTimePassword() throws Exception {
        int otpSize = 7;
        given(serviceConfig.length()).willReturn(otpSize);
        OneTimePasswordGeneratorNode node = new OneTimePasswordGeneratorNode(serviceConfig, SECURE_RANDOM,
                otpGenerator);
        Action result = node.process(emptyTreeContext());
        assertThat(result.transientState).stringAt(ONE_TIME_PASSWORD).hasSize(otpSize);
    }

    @Test
    void shouldGenerateOneTimePasswordOfLengthOne() throws Exception {
        int otpSize = 1;
        given(serviceConfig.length()).willReturn(otpSize);
        OneTimePasswordGeneratorNode node = new OneTimePasswordGeneratorNode(serviceConfig, SECURE_RANDOM,
                otpGenerator);
        Action result = node.process(emptyTreeContext());
        assertThat(result.transientState).stringAt(ONE_TIME_PASSWORD).hasSize(otpSize);
    }


    @Test
    void shouldThrowWhenOTPGenerationFails() throws Exception {
        int otpSize = 8;
        given(serviceConfig.length()).willReturn(otpSize);
        HOTPAlgorithm errorGenerator = mock(HOTPAlgorithm.class);
        when(errorGenerator.generateOTP(
                ArgumentMatchers.argThat(anyByteArrayMatcher),
                anyLong(),
                anyInt(),
                anyBoolean(),
                anyInt())).thenThrow(InvalidKeyException.class);
        OneTimePasswordGeneratorNode node = new OneTimePasswordGeneratorNode(serviceConfig, SECURE_RANDOM,
                errorGenerator);
        assertThatThrownBy(() -> node.process(emptyTreeContext()))
                .isInstanceOf(NodeProcessException.class)
                .hasMessage("java.security.InvalidKeyException");
    }
}
