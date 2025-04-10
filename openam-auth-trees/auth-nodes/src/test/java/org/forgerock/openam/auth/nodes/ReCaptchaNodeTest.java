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
 * Copyright 2019-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.http.protocol.Response.newResponsePromise;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.nodes.ReCaptchaNode.SUCCESS;
import static org.forgerock.openam.auth.nodes.TreeContextFactory.emptyTreeContext;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.util.promise.Promise;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.authentication.callbacks.ReCaptchaCallback;

@ExtendWith(MockitoExtension.class)
public class ReCaptchaNodeTest {

    private static final String SITE_KEY = "siteKey";
    private static final String API_URI = "apiUri";
    private static final String DIV_CLASS = "divClass";
    @Mock
    private ReCaptchaNode.Config config;
    @Mock
    private Handler handler;
    @InjectMocks
    private ReCaptchaNode node;

    @Test
    void shouldReturnSiteKeyDuringFirstPass() throws Exception {
        when(config.siteKey()).thenReturn(SITE_KEY);
        when(config.apiUri()).thenReturn("https://www.google.com/recaptcha/api/siteverify");
        when(config.divClass()).thenReturn("g-recaptcha");
        Action result = node.process(emptyTreeContext());

        assertThat(result.callbacks.size()).isEqualTo(1);
        assertThat(result.callbacks.get(0)).isInstanceOf(ReCaptchaCallback.class);
        ReCaptchaCallback callback = ((ReCaptchaCallback) result.callbacks.get(0));
        assertThat(callback.getSiteKey()).isEqualTo(SITE_KEY);
    }

    @Test
    void shouldThrowExceptionIfNoResponseProvided() throws Exception {
        ReCaptchaCallback callback = new ReCaptchaCallback(SITE_KEY, API_URI, DIV_CLASS, false);
        TreeContext context = new TreeContext(json(object()),
                new ExternalRequestContext.Builder().build(), singletonList(callback), Optional.empty());

        // when
        assertThatThrownBy(() -> node.process(context))
                // then
                .isInstanceOf(NodeProcessException.class);
    }

    @Test
    void shouldThrowExceptionIfResponseCannotBeVerified() throws Exception {
        when(config.reCaptchaUri()).thenReturn("https://www.google.com/recaptcha/api/siteverify");
        ReCaptchaCallback callback = new ReCaptchaCallback(SITE_KEY, API_URI, DIV_CLASS, false);
        callback.setResponse("response");
        TreeContext context = new TreeContext(json(object()),
                new ExternalRequestContext.Builder().build(), singletonList(callback), Optional.empty());

        Promise promise = mock(Promise.class);

        when(handler.handle(any(), any())).thenReturn(promise);
        when(promise.getOrThrow()).thenThrow(InterruptedException.class);

        //when
        assertThatThrownBy(() -> node.process(context))
                //then
                .isInstanceOf(NodeProcessException.class);
    }

    @Test
    void shouldThrowExceptionIfResponseContainsFailure() throws Exception {
        when(config.reCaptchaUri()).thenReturn("https://www.google.com/recaptcha/api/siteverify");
        ReCaptchaCallback callback = new ReCaptchaCallback(SITE_KEY, API_URI, DIV_CLASS, false);
        callback.setResponse(SUCCESS);
        TreeContext context = new TreeContext(json(object()),
                new ExternalRequestContext.Builder().build(), singletonList(callback), Optional.empty());

        Response response = new Response(Status.OK);
        response.setEntity(json(object(
                field(SUCCESS, false)
        )));

        when(handler.handle(any(), any())).thenReturn(newResponsePromise(response));
        //when
        assertThatThrownBy(() -> node.process(context))
                //then
                .isInstanceOf(NodeProcessException.class);
    }

    @Test
    void shouldContinueToNextNodeIfResponseContainsSuccess() throws Exception {
        when(config.reCaptchaUri()).thenReturn("https://www.google.com/recaptcha/api/siteverify");
        ReCaptchaCallback callback = new ReCaptchaCallback(SITE_KEY, API_URI, DIV_CLASS, false);
        callback.setResponse("response");
        TreeContext context = new TreeContext(json(object()),
                new ExternalRequestContext.Builder().build(), singletonList(callback), Optional.empty());

        Response response = new Response(Status.OK);
        response.setEntity(json(object(
                field(SUCCESS, true)
        )));

        when(handler.handle(any(), any())).thenReturn(newResponsePromise(response));
        Action action = node.process(context);

        assertThat(action.outcome).isEqualTo("outcome");
    }
}
