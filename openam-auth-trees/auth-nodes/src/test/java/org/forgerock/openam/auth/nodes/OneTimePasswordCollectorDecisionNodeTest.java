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

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.ONE_TIME_PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.ONE_TIME_PASSWORD_ENCRYPTED;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.ONE_TIME_PASSWORD_TIMESTAMP;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.PasswordCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.crypto.NodeSharedStateCrypto;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for {@link OneTimePasswordCollectorDecisionNode}.
 */
public class OneTimePasswordCollectorDecisionNodeTest {

    @Mock
    private OneTimePasswordCollectorDecisionNode.Config serviceConfig;

    @Mock
    private NodeSharedStateCrypto nodeSharedStateCrypto;

    private JsonValue encryptedOtpJson;

    @BeforeMethod
    public void before() {
        encryptedOtpJson = json(object(field(ONE_TIME_PASSWORD_ENCRYPTED, "encryptedOtpString")));
        initMocks(this);
    }

    private static final int FOUR_MINS_IN_SECONDS = 240;
    private static final int FIVE_MINS_IN_SECONDS = 300;

    @Test
    public void processReturnsTrueOutcomeIfOTPIsEqualToInputOTPAndNotExpired() throws Exception {
        // Given
        PasswordCallback callback = new PasswordCallback("prompt", false);
        callback.setPassword("123456".toCharArray());
        JsonValue otpJson = json(object(
                field(ONE_TIME_PASSWORD_TIMESTAMP, Instant.now().getEpochSecond()),
                field(ONE_TIME_PASSWORD, "123456")));
        given(nodeSharedStateCrypto.decrypt(any())).willReturn(otpJson);
        given(serviceConfig.passwordExpiryTime()).willReturn(5L);
        Node node = new OneTimePasswordCollectorDecisionNode(serviceConfig, nodeSharedStateCrypto);
        TreeContext treeContext = getContext(encryptedOtpJson, singletonList(callback));

        // When
        Action result = node.process(treeContext);

        // Then
        assertThat(result.outcome).isEqualTo("true");
    }

    @Test
    public void processReturnsFalseOutcomeIfOTPIsNotEqualToInputOTPAndNotExpired() throws Exception {
        // Given
        PasswordCallback callback = new PasswordCallback("prompt", false);
        callback.setPassword("123456".toCharArray());
        JsonValue otpJson = json(object(
                field(ONE_TIME_PASSWORD_TIMESTAMP, Instant.now().getEpochSecond()),
                field(ONE_TIME_PASSWORD, "654321")));
        given(nodeSharedStateCrypto.decrypt(any())).willReturn(otpJson);
        given(serviceConfig.passwordExpiryTime()).willReturn(5L);
        Node node = new OneTimePasswordCollectorDecisionNode(serviceConfig, nodeSharedStateCrypto);
        TreeContext treeContext = getContext(encryptedOtpJson, singletonList(callback));

        // When
        Action result = node.process(treeContext);

        // Then
        assertThat(result.outcome).isEqualTo("false");
    }

    @Test
    public void processReturnsNoOutcomeIfCallbacksIsEmpty() throws Exception {
        TreeContext treeContext = getContext(json(object(
                field(ONE_TIME_PASSWORD_TIMESTAMP, Instant.now().getEpochSecond()),
                field(ONE_TIME_PASSWORD, "654321"))),
                Collections.emptyList());
        given(serviceConfig.passwordExpiryTime()).willReturn(5L);
        Node node = new OneTimePasswordCollectorDecisionNode(serviceConfig, nodeSharedStateCrypto);
        Action result = node.process(treeContext);
        assertThat(result.outcome).isEqualTo(null);
        assertThat(result.callbacks).hasSize(1);
        assertThat(result.callbacks.get(0)).isInstanceOf(PasswordCallback.class);
    }

    @Test
    public void processReturnsFalseOutcomeIfOTPIsEmpty() throws Exception {
        PasswordCallback callback = new PasswordCallback("prompt", false);
        callback.setPassword("".toCharArray());
        TreeContext treeContext = getContext(json(object(
                field(ONE_TIME_PASSWORD_TIMESTAMP, Instant.now().getEpochSecond()),
                field(ONE_TIME_PASSWORD, "654321"))),
                singletonList(callback));
        given(serviceConfig.passwordExpiryTime()).willReturn(5L);
        Node node = new OneTimePasswordCollectorDecisionNode(serviceConfig, nodeSharedStateCrypto);
        Action result = node.process(treeContext);
        assertThat(result.outcome).isEqualTo(null);
        assertThat(result.callbacks).hasSize(1);
        assertThat(result.callbacks.get(0)).isInstanceOf(PasswordCallback.class);
    }

    @Test
    public void processReturnsFalseOutcomeIfOTPIsNull() throws Exception {
        PasswordCallback callback = new PasswordCallback("prompt", false);
        callback.setPassword(null);
        TreeContext treeContext = getContext(json(object(
                field(ONE_TIME_PASSWORD_TIMESTAMP, Instant.now().getEpochSecond()),
                field(ONE_TIME_PASSWORD, "654321"))),
                singletonList(callback));
        given(serviceConfig.passwordExpiryTime()).willReturn(5L);
        Node node = new OneTimePasswordCollectorDecisionNode(serviceConfig, nodeSharedStateCrypto);
        Action result = node.process(treeContext);
        assertThat(result.outcome).isEqualTo(null);
        assertThat(result.callbacks).hasSize(1);
        assertThat(result.callbacks.get(0)).isInstanceOf(PasswordCallback.class);
    }

    @Test
    public void processReturnsFalseOutcomeIfOTPIsEqualToInputOTPAndExpired() throws Exception {
        // Given
        PasswordCallback callback = new PasswordCallback("prompt", false);
        callback.setPassword("123456".toCharArray());
        JsonValue otpJson = json(object(
                field(ONE_TIME_PASSWORD_TIMESTAMP, Instant.now().getEpochSecond() - (FIVE_MINS_IN_SECONDS)),
                field(ONE_TIME_PASSWORD, "123456")));
        given(nodeSharedStateCrypto.decrypt(any())).willReturn(otpJson);
        given(serviceConfig.passwordExpiryTime()).willReturn(5L);
        Node node = new OneTimePasswordCollectorDecisionNode(serviceConfig, nodeSharedStateCrypto);
        TreeContext treeContext = getContext(encryptedOtpJson, singletonList(callback));

        // When
        Action result = node.process(treeContext);

        // Then
        assertThat(result.outcome).isEqualTo("false");
    }

    @Test
    public void processReturnsTrueOutcomeIfOTPIsEqualToInputOTPAndNotYetExpired() throws Exception {
        // Given
        PasswordCallback callback = new PasswordCallback("prompt", false);
        callback.setPassword("123456".toCharArray());
        JsonValue otpJson = json(object(
                field(ONE_TIME_PASSWORD_TIMESTAMP, Instant.now().getEpochSecond() - (FOUR_MINS_IN_SECONDS)),
                field(ONE_TIME_PASSWORD, "123456")));
        given(nodeSharedStateCrypto.decrypt(any())).willReturn(otpJson);
        given(serviceConfig.passwordExpiryTime()).willReturn(5L);
        Node node = new OneTimePasswordCollectorDecisionNode(serviceConfig, nodeSharedStateCrypto);
        TreeContext treeContext = getContext(encryptedOtpJson, singletonList(callback));

        // When
        Action result = node.process(treeContext);

        // Then
        assertThat(result.outcome).isEqualTo("true");
        assertThat(treeContext.sharedState.get(ONE_TIME_PASSWORD).isNull()).isEqualTo(true);
        assertThat(treeContext.sharedState.get(ONE_TIME_PASSWORD_TIMESTAMP).isNull()).isEqualTo(true);
    }

    @Test
    public void shouldProcessGivenNonEncryptedOtpPresent() throws Exception {
        // Given
        PasswordCallback callback = new PasswordCallback("prompt", false);
        callback.setPassword("123456".toCharArray());
        TreeContext treeContext = getContext(json(object(
                field(ONE_TIME_PASSWORD_TIMESTAMP, Instant.now().getEpochSecond() - (FOUR_MINS_IN_SECONDS)),
                field(ONE_TIME_PASSWORD, "123456"))),
                singletonList(callback));
        given(serviceConfig.passwordExpiryTime()).willReturn(5L);
        Node node = new OneTimePasswordCollectorDecisionNode(serviceConfig, nodeSharedStateCrypto);

        // When
        Action result = node.process(treeContext);

        // Then
        assertThat(result.outcome).isEqualTo("true");
        assertThat(treeContext.sharedState.get(ONE_TIME_PASSWORD_ENCRYPTED).isNull()).isEqualTo(true);
    }

    private TreeContext getContext(JsonValue sharedState, List<Callback> callbacks) {
        return new TreeContext(sharedState, new Builder().build(), callbacks);
    }
}