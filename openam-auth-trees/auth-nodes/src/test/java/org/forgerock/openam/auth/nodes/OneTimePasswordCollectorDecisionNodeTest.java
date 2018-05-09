/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.ONE_TIME_PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.ONE_TIME_PASSWORD_TIMESTAMP;
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
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class OneTimePasswordCollectorDecisionNodeTest {

    @Mock
    private OneTimePasswordCollectorDecisionNode.Config serviceConfig;

    @BeforeMethod
    public void before() {
        initMocks(this);
    }

    private static final int FOUR_MINS_IN_SECONDS = 240;
    private static final int FIVE_MINS_IN_SECONDS = 300;

    @Test
    public void processReturnsTrueOutcomeIfOTPIsEqualToInputOTPAndNotExpired() throws Exception {
        PasswordCallback callback = new PasswordCallback("prompt", false);
        callback.setPassword("123456".toCharArray());
        TreeContext treeContext = getContext(json(object(
                field(ONE_TIME_PASSWORD_TIMESTAMP, Instant.now().getEpochSecond()),
                field(ONE_TIME_PASSWORD, "123456"))),
                singletonList(callback));
        given(serviceConfig.passwordExpiryTime()).willReturn(5L);
        Node node = new OneTimePasswordCollectorDecisionNode(serviceConfig);
        Action result = node.process(treeContext);
        assertThat(result.outcome).isEqualTo("true");
    }

    @Test
    public void processReturnsFalseOutcomeIfOTPIsNotEqualToInputOTPAndNotExpired() throws Exception {
        PasswordCallback callback = new PasswordCallback("prompt", false);
        callback.setPassword("123456".toCharArray());
        TreeContext treeContext = getContext(json(object(
                field(ONE_TIME_PASSWORD_TIMESTAMP, Instant.now().getEpochSecond()),
                field(ONE_TIME_PASSWORD, "654321"))),
                singletonList(callback));
        given(serviceConfig.passwordExpiryTime()).willReturn(5L);
        Node node = new OneTimePasswordCollectorDecisionNode(serviceConfig);
        Action result = node.process(treeContext);
        assertThat(result.outcome).isEqualTo("false");
    }

    @Test
    public void processReturnsNoOutcomeIfCallbacksIsEmpty() throws Exception {
        TreeContext treeContext = getContext(json(object(
                field(ONE_TIME_PASSWORD_TIMESTAMP, Instant.now().getEpochSecond()),
                field(ONE_TIME_PASSWORD, "654321"))),
                Collections.emptyList());
        given(serviceConfig.passwordExpiryTime()).willReturn(5L);
        Node node = new OneTimePasswordCollectorDecisionNode(serviceConfig);
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
        Node node = new OneTimePasswordCollectorDecisionNode(serviceConfig);
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
        Node node = new OneTimePasswordCollectorDecisionNode(serviceConfig);
        Action result = node.process(treeContext);
        assertThat(result.outcome).isEqualTo(null);
        assertThat(result.callbacks).hasSize(1);
        assertThat(result.callbacks.get(0)).isInstanceOf(PasswordCallback.class);
    }

    @Test
    public void processReturnsFalseOutcomeIfOTPIsEqualToInputOTPAndExpired() throws Exception {
        PasswordCallback callback = new PasswordCallback("prompt", false);
        callback.setPassword("123456".toCharArray());
        TreeContext treeContext = getContext(json(object(
                field(ONE_TIME_PASSWORD_TIMESTAMP, Instant.now().getEpochSecond() - (FIVE_MINS_IN_SECONDS)),
                field(ONE_TIME_PASSWORD, "123456"))),
                singletonList(callback));
        given(serviceConfig.passwordExpiryTime()).willReturn(5L);
        Node node = new OneTimePasswordCollectorDecisionNode(serviceConfig);
        Action result = node.process(treeContext);
        assertThat(result.outcome).isEqualTo("false");
    }

    @Test
    public void processReturnsTrueOutcomeIfOTPIsEqualToInputOTPAndNotYetExpired() throws Exception {
        PasswordCallback callback = new PasswordCallback("prompt", false);
        callback.setPassword("123456".toCharArray());
        TreeContext treeContext = getContext(json(object(
                field(ONE_TIME_PASSWORD_TIMESTAMP, Instant.now().getEpochSecond() - (FOUR_MINS_IN_SECONDS)),
                field(ONE_TIME_PASSWORD, "123456"))),
                singletonList(callback));
        given(serviceConfig.passwordExpiryTime()).willReturn(5L);
        Node node = new OneTimePasswordCollectorDecisionNode(serviceConfig);
        Action result = node.process(treeContext);
        assertThat(result.outcome).isEqualTo("true");
    }

    private TreeContext getContext(JsonValue sharedState, List<Callback> callbacks) {
        return new TreeContext(sharedState, new Builder().build(), callbacks);
    }
}
