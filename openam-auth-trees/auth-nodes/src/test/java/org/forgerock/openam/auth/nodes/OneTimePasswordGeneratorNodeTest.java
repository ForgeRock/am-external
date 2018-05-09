/*
 * Copyright 2017-2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.ONE_TIME_PASSWORD;
import static org.forgerock.openam.auth.nodes.TreeContextFactory.emptyTreeContext;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.authentication.modules.hotp.HOTPAlgorithm;

public class OneTimePasswordGeneratorNodeTest {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private ArgumentMatcher<byte[]> anyByteArrayMatcher = bytes -> true;

    @Mock
    OneTimePasswordGeneratorNode.Config serviceConfig;

    HOTPAlgorithm otpGenerator = new HOTPAlgorithm();

    @BeforeMethod
    public void before() throws InvalidKeyException, NoSuchAlgorithmException {
        initMocks(this);
    }

    @Test
    public void shouldGenerateOneTimePassword() throws Exception {
        int otpSize = 7;
        given(serviceConfig.length()).willReturn(otpSize);
        OneTimePasswordGeneratorNode node = new OneTimePasswordGeneratorNode(serviceConfig, SECURE_RANDOM,
                otpGenerator);
        Action result = node.process(emptyTreeContext());
        assertThat(result.sharedState).stringAt(ONE_TIME_PASSWORD).hasSize(otpSize);
    }

    @Test
    public void shouldGenerateOneTimePasswordOfLengthOne() throws Exception {
        int otpSize = 1;
        given(serviceConfig.length()).willReturn(otpSize);
        OneTimePasswordGeneratorNode node = new OneTimePasswordGeneratorNode(serviceConfig, SECURE_RANDOM,
                otpGenerator);
        Action result = node.process(emptyTreeContext());
        assertThat(result.sharedState).stringAt(ONE_TIME_PASSWORD).hasSize(otpSize);
    }


    @Test(expectedExceptions = NodeProcessException.class)
    public void shouldThrowWhenOTPGenerationFails() throws Exception {
        int otpSize = 8;
        given(serviceConfig.length()).willReturn(otpSize);
        HOTPAlgorithm errorGenerator = mock(HOTPAlgorithm.class);
        when(errorGenerator.generateOTP(
                Matchers.argThat(anyByteArrayMatcher),
                anyLong(),
                anyInt(),
                anyBoolean(),
                anyInt())).thenThrow(InvalidKeyException.class);
        OneTimePasswordGeneratorNode node = new OneTimePasswordGeneratorNode(serviceConfig, SECURE_RANDOM,
                errorGenerator);
        Action result = node.process(emptyTreeContext());
        assertThat(result.sharedState).stringAt(ONE_TIME_PASSWORD).hasSize(otpSize);
    }
}
