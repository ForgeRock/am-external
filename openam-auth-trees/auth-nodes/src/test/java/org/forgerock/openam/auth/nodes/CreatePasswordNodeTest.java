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
 * Copyright 2018 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.PasswordCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.mockito.Mock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test for the create password Node
 */
public class CreatePasswordNodeTest {

    public static final int PASSWORD_LENGTH_MIN = 8;
    @Mock
    private CreatePasswordNode.Config config;

    @BeforeMethod
    public void before() {
        initMocks(this);
        when(config.minPasswordLength()).thenReturn(PASSWORD_LENGTH_MIN);
    }

    @Test
    public void processReturnsCallbacksOutcomeNoCallbacksArePresent() throws Exception {
        //GIVEN
        TreeContext context = new TreeContext(JsonValue.json(new Object()),
                new ExternalRequestContext.Builder().build(), Collections.emptyList());
        CreatePasswordNode node = new CreatePasswordNode(config);

        //WHEN
        Action process = node.process(context);

        //THEN
        assertExpectedCallbacks(process);
    }

    @Test
    public void processReturnsTrueOutcomeIfPasswordIsValid() throws Exception {
        //GIVEN
        TreeContext context = new TreeContext(JsonValue.json(new Object()),
                new ExternalRequestContext.Builder().build(), createPasswordCallback("password", "password"));
        CreatePasswordNode node = new CreatePasswordNode(config);

        //WHEN
        Action process = node.process(context);

        //THEN
        Assert.assertEquals(process.outcome, "outcome");
    }

    @Test
    public void processSetAPasswordInTransientStateOutcomeIfPasswordIsValid() throws Exception {
        //GIVEN
        TreeContext context = new TreeContext(JsonValue.json(new Object()),
                new ExternalRequestContext.Builder().build(), createPasswordCallback("password", "password"));
        CreatePasswordNode node = new CreatePasswordNode(config);

        //WHEN
        Action process = node.process(context);

        //THEN
        Assert.assertTrue(process.transientState.isDefined("password"));
        Assert.assertEquals(process.transientState.get("password").asString(), "password");
    }

    @Test
    public void processReturnsCallbacksIfPasswordsMismatch() throws Exception {
        //GIVEN
        TreeContext context = new TreeContext(JsonValue.json(new Object()),
                new ExternalRequestContext.Builder().build(), createPasswordCallback("password1", "password2"));
        CreatePasswordNode node = new CreatePasswordNode(config);

        //WHEN
        Action process = node.process(context);

        //THEN
        assertExpectedCallbacks(process);
    }

    @Test
    public void processReturnsCallbacksIfPasswordToShort() throws Exception {
        //GIVEN
        String shortPassword = IntStream.range(1, PASSWORD_LENGTH_MIN)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining());

        TreeContext context = new TreeContext(JsonValue.json(new Object()),
                new ExternalRequestContext.Builder().build(), createPasswordCallback(shortPassword, shortPassword));
        CreatePasswordNode node = new CreatePasswordNode(config);

        //WHEN
        Action process = node.process(context);

        //THEN
        assertExpectedCallbacks(process);
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
        Assert.assertEquals(process.callbacks.size(), 2);
        Assert.assertTrue(process.callbacks.get(0) instanceof PasswordCallback);
        Assert.assertTrue(process.callbacks.get(1) instanceof PasswordCallback);
    }
}