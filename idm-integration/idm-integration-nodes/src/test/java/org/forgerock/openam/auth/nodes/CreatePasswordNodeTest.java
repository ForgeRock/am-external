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
 * Copyright 2018-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static javax.security.auth.callback.TextOutputCallback.ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.openam.auth.nodes.CreatePasswordNode.BUNDLE;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;

import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test for the create password Node
 */
@ExtendWith(MockitoExtension.class)
public class CreatePasswordNodeTest {

    private static final int PASSWORD_LENGTH_MIN = 8;
    @Mock
    private CreatePasswordNode.Config config;

    @Test
    void processReturnsCallbacksOutcomeNoCallbacksArePresent() throws Exception {
        //GIVEN
        TreeContext context = new TreeContext(json(new Object()),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());
        CreatePasswordNode node = new CreatePasswordNode(config);

        //WHEN
        Action process = node.process(context);

        //THEN
        assertExpectedCallbacks(process);
    }

    @Test
    void processReturnsTrueOutcomeIfPasswordIsValid() throws Exception {
        //GIVEN
        when(config.minPasswordLength()).thenReturn(PASSWORD_LENGTH_MIN);
        TreeContext context = new TreeContext(json(new Object()), new ExternalRequestContext.Builder().build(),
                createPasswordCallback("password", "password"), Optional.empty());
        CreatePasswordNode node = new CreatePasswordNode(config);

        //WHEN
        Action process = node.process(context);

        //THEN
        assertThat(process.outcome).isEqualTo("outcome");
    }

    @Test
    void processSetAPasswordInTransientStateOutcomeIfPasswordIsValid() throws Exception {
        //GIVEN
        when(config.minPasswordLength()).thenReturn(PASSWORD_LENGTH_MIN);
        TreeContext context = new TreeContext(json(new Object()), new ExternalRequestContext.Builder().build(),
                createPasswordCallback("password", "password"), Optional.empty());
        CreatePasswordNode node = new CreatePasswordNode(config);

        //WHEN
        Action process = node.process(context);

        //THEN
        assertThat(process.transientState.isDefined("password")).isTrue();
        assertThat(process.transientState.get("password").asString()).isEqualTo("password");
    }

    @Test
    void processReturnsCallbacksIfPasswordsMismatch() throws Exception {
        //GIVEN
        when(config.minPasswordLength()).thenReturn(PASSWORD_LENGTH_MIN);
        TreeContext context = new TreeContext(json(new Object()), new ExternalRequestContext.Builder().build(),
                createPasswordCallback("password1", "password2"), Optional.empty());
        CreatePasswordNode node = new CreatePasswordNode(config);

        //WHEN
        Action process = node.process(context);

        //THEN
        assertExpectedCallbacksWithErrorMessage(process, expectedErrorMessage(context, "error.password.mismatch"));
    }

    private String expectedErrorMessage(TreeContext context, String s) {
        return getResourceBundle(context).getString(s);
    }

    private ResourceBundle getResourceBundle(TreeContext context) {
        return context.request.locales.getBundleInPreferredLocale(BUNDLE,
                getClass().getClassLoader());
    }

    @Test
    void processReturnsCallbacksIfPasswordTooShort() throws Exception {
        //GIVEN
        when(config.minPasswordLength()).thenReturn(PASSWORD_LENGTH_MIN);
        String shortPassword = IntStream.range(1, PASSWORD_LENGTH_MIN)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining());

        TreeContext context = new TreeContext(json(new Object()), new ExternalRequestContext.Builder().build(),
                createPasswordCallback(shortPassword, shortPassword), Optional.empty());
        CreatePasswordNode node = new CreatePasswordNode(config);

        //WHEN
        Action process = node.process(context);

        //THEN
        String errorMessage = String.format(expectedErrorMessage(context, "error.password.length"),
                PASSWORD_LENGTH_MIN);
        assertExpectedCallbacksWithErrorMessage(process, errorMessage);
    }

    private List<Callback> createPasswordCallback(String password, String confirmPassword) {
        List<Callback> callbacks = new ArrayList<>(2);
        callbacks.add(getPassword("password", password));
        callbacks.add(getPassword("confirm_password", confirmPassword));
        return callbacks;
    }

    private PasswordCallback getPassword(String text, String password) {
        PasswordCallback passwordCallback = new PasswordCallback(text, false);
        passwordCallback.setPassword(password.toCharArray());
        return passwordCallback;
    }

    private void assertExpectedCallbacks(Action process) {
        assertThat(process.callbacks).hasSize(2);
        assertThat(process.callbacks.get(0)).isInstanceOf(PasswordCallback.class);
        assertThat(process.callbacks.get(1)).isInstanceOf(PasswordCallback.class);


    }

    private void assertExpectedCallbacksWithErrorMessage(Action process, String message) {
        assertThat(process.callbacks).hasSize(3);
        assertThat(process.callbacks.get(0)).isInstanceOf(PasswordCallback.class);
        assertThat(process.callbacks.get(1)).isInstanceOf(PasswordCallback.class);
        final TextOutputCallback errorMessage = (TextOutputCallback) process.callbacks.get(2);
        assertThat(errorMessage.getMessageType()).isEqualTo(ERROR);
        assertThat(errorMessage.getMessage()).isEqualTo(message);
    }
}
