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
 * Copyright 2019-2021 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.http.protocol.Response.newResponsePromise;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.nodes.TreeContextFactory.emptyTreeContext;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Optional;

import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.util.promise.Promise;

import com.sun.identity.authentication.callbacks.ReCaptchaCallback;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CaptchaNodeTest {

    @Mock
    private CaptchaNode.Config config;

    @Mock
    private Handler handler;

    private CaptchaNode node;
    private static final String SITE_KEY = "siteKey";
    private static final String API_URI = "apiUri";
    private static final String DIV_CLASS = "divClass";
    private static final boolean RECAPTCHA_V3 = true;

    @BeforeMethod
    public void setUp() throws Exception {
        node = null;
        openMocks(this);

        node = new CaptchaNode(config, handler);

        when(config.siteKey()).thenReturn(SITE_KEY);
        when(config.secretKey()).thenReturn("secretKey");
        when(config.captchaUri()).thenReturn("https://www.google.com/recaptcha/api/siteverify");
        when(config.apiUri()).thenReturn("https://www.google.com/recaptcha/api/siteverify");
        when(config.divClass()).thenReturn("g-recaptcha");
        when(config.reCaptchaV3()).thenReturn(RECAPTCHA_V3);
        when(config.scoreThreshold()).thenReturn("0.5");
    }

    @Test
    public void shouldReturnSiteKeyDuringFirstPass() throws Exception {
        Action result = node.process(emptyTreeContext());

        assertThat(result.callbacks.size()).isEqualTo(1);
        assertThat(result.callbacks.get(0)).isInstanceOf(ReCaptchaCallback.class);
        ReCaptchaCallback callback = ((ReCaptchaCallback) result.callbacks.get(0));
        assertThat(callback.getSiteKey()).isEqualTo(SITE_KEY);
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void shouldThrowExceptionIfNoResponseProvided() throws Exception {
        ReCaptchaCallback callback = new ReCaptchaCallback(SITE_KEY, API_URI, DIV_CLASS, RECAPTCHA_V3);
        TreeContext context = new TreeContext(json(object()),
                new ExternalRequestContext.Builder().build(), singletonList(callback), Optional.empty());
        node.process(context);
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void shouldThrowExceptionIfResponseCannotBeVerified() throws Exception {
        ReCaptchaCallback callback = new ReCaptchaCallback(SITE_KEY, API_URI, DIV_CLASS, RECAPTCHA_V3);
        callback.setResponse("response");
        TreeContext context = new TreeContext(json(object()),
        new ExternalRequestContext.Builder().build(), singletonList(callback), Optional.empty());

        Promise promise = mock(Promise.class);

        when(handler.handle(any(), any())).thenReturn(promise);
        when(promise.getOrThrow()).thenThrow(InterruptedException.class);
        node.process(context);
    }

    @Test
    public void shouldGoToFailOutcomeIfResponseContainsFailure() throws Exception {
        ReCaptchaCallback callback = new ReCaptchaCallback(SITE_KEY, API_URI, DIV_CLASS, RECAPTCHA_V3);
        callback.setResponse("success");
        TreeContext context = new TreeContext(json(object()),
                new ExternalRequestContext.Builder().build(), singletonList(callback), Optional.empty());

        Response response = new Response(Status.OK);
        response.setEntity(json(object(
                field("success", false)
        )));

        when(handler.handle(any(), any())).thenReturn(newResponsePromise(response));
        Action action = node.process(context);
        assertThat(action.outcome).isEqualTo("false");
    }

    @Test
    public void shouldGoToTrueOutcomeIfResponseContainsSuccessAndMeetsScoreThreshold() throws Exception {
        ReCaptchaCallback callback = new ReCaptchaCallback(SITE_KEY, API_URI, DIV_CLASS, RECAPTCHA_V3);
        callback.setResponse("response");
        TreeContext context = new TreeContext(json(object()),
                new ExternalRequestContext.Builder().build(), singletonList(callback), Optional.empty());

        Response response = new Response(Status.OK);
        response.setEntity(json(object(
                field("success", true), field("score", 0.8)
        )));

        when(handler.handle(any(), any())).thenReturn(newResponsePromise(response));
        Action action = node.process(context);

        assertThat(action.outcome).isEqualTo("true");
    }

    @Test
    public void shouldGoToFalseOutcomeIfResponseDoesNotMeetThreshold() throws Exception {
        ReCaptchaCallback callback = new ReCaptchaCallback(SITE_KEY, API_URI, DIV_CLASS, RECAPTCHA_V3);
        callback.setResponse("response");
        TreeContext context = new TreeContext(json(object()),
                new ExternalRequestContext.Builder().build(), singletonList(callback), Optional.empty());

        Response response = new Response(Status.OK);
        response.setEntity(json(object(
                field("success", true), field("score", 0.4)
        )));

        when(handler.handle(any(), any())).thenReturn(newResponsePromise(response));
        Action action = node.process(context);

        assertThat(action.outcome).isEqualTo("false");
    }
}
