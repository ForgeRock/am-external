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
 * Copyright 2017-2021 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static org.assertj.core.api.Assertions.shouldHaveThrown;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.ONE_TIME_PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.ONE_TIME_PASSWORD_ENCRYPTED;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.ONE_TIME_PASSWORD_TIMESTAMP;
import static org.forgerock.openam.auth.nodes.TreeContextFactory.emptyTreeContext;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyBoolean;
import static org.mockito.BDDMockito.anyInt;
import static org.mockito.BDDMockito.anyLong;
import static org.mockito.BDDMockito.given;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.crypto.NodeSharedStateCrypto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.authentication.modules.hotp.HOTPAlgorithm;

/**
 * Tests for {@link OneTimePasswordGeneratorNode}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(SystemProperties.class)
@PowerMockIgnore("javax.crypto.*")
public class OneTimePasswordGeneratorNodeTest {

    @Mock
    private OneTimePasswordGeneratorNode.Config serviceConfig;

    @Mock
    private SecureRandom secureRandom;

    @Mock
    private HOTPAlgorithm otpGenerator;

    @Mock
    private NodeSharedStateCrypto nodeSharedStateCrypto;

    @InjectMocks
    private OneTimePasswordGeneratorNode node;

    @Test
    public void shouldProcess() throws Exception {
        // Given
        String encryptedOtp = "encryptedOtpString";
        given(nodeSharedStateCrypto.encrypt(any())).willReturn(encryptedOtp);

        // When
        Action result = node.process(emptyTreeContext());

        // Then
        assertThat(result.sharedState.get(ONE_TIME_PASSWORD_ENCRYPTED).asString(), is(encryptedOtp));
    }

    @Test
    public void shouldProcessGivenNonEncryptedOtpOfLengthSevenAndNonEmptySharedState() throws Exception {
        // Given
        setStoreOtpEncryptedSystemPropertyToFalse();
        int otpSize = 7;
        TreeContext context = emptyTreeContext();
        JsonValue sharedStateJsonValue = json(object(field("realm", "/")));
        context.sharedState.setObject(sharedStateJsonValue);
        OneTimePasswordGeneratorNode node = getOneTimePasswordGeneratorNode(otpSize);

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.sharedState.get(ONE_TIME_PASSWORD).asString().length(), is(otpSize));
        assertThat(result.sharedState.get(ONE_TIME_PASSWORD_TIMESTAMP).asLong(), is(notNullValue()));
        assertThat(result.sharedState.get("realm").asString(), is("/"));
    }

    @Test
    public void shouldProcessGivenNonEncryptedOtpOfLengthOne() throws Exception {
        // Given
        setStoreOtpEncryptedSystemPropertyToFalse();
        int otpSize = 1;
        OneTimePasswordGeneratorNode node = getOneTimePasswordGeneratorNode(otpSize);

        // When
        Action result = node.process(emptyTreeContext());

        // Then
        assertThat(result.sharedState.get(ONE_TIME_PASSWORD).asString().length(), is(otpSize));
        assertThat(result.sharedState.get(ONE_TIME_PASSWORD_TIMESTAMP).asLong(), is(notNullValue()));
    }

    private OneTimePasswordGeneratorNode getOneTimePasswordGeneratorNode(int otpSize) {
        given(serviceConfig.length()).willReturn(otpSize);
        return new OneTimePasswordGeneratorNode(serviceConfig, secureRandom,
                new HOTPAlgorithm(), nodeSharedStateCrypto);
    }

    @Test
    public void shouldFailProcessGivenInvalidKeyException() throws Exception {
        // Given
        given(otpGenerator.generateOTP(any(), anyLong(), anyInt(), anyBoolean(), anyInt()))
                .willThrow(InvalidKeyException.class);

        try {
            // When
            node.process(emptyTreeContext());

            // Then
            shouldHaveThrown(NodeProcessException.class);
        } catch (NodeProcessException e) {
            assertThat(e.getMessage(), is("java.security.InvalidKeyException"));
        }
    }

    @Test
    public void shouldFailProcessGivenNoSuchAlgorithmException() throws Exception {
        // Given
        given(otpGenerator.generateOTP(any(), anyLong(), anyInt(), anyBoolean(), anyInt()))
                .willThrow(NoSuchAlgorithmException.class);

        try {
            // When
            node.process(emptyTreeContext());

            // Then
            shouldHaveThrown(NodeProcessException.class);
        } catch (NodeProcessException e) {
            assertThat(e.getMessage(), is("java.security.NoSuchAlgorithmException"));
        }
    }

    private void setStoreOtpEncryptedSystemPropertyToFalse() {
        PowerMockito.mockStatic(SystemProperties.class);
        given(SystemProperties.getAsBoolean("org.forgerock.am.auth.node.otp.encrypted")).willReturn(false);
    }

}
