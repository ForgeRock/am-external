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
 * Copyright 2019-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.http.protocol.Response.newResponsePromise;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.nodes.TreeContextFactory.emptyTreeContext;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.secrets.cache.SecretCache;
import org.forgerock.openam.secrets.cache.SecretReferenceCache;
import org.forgerock.openam.secrets.service.SecretReferenceRetrievalService;
import org.forgerock.secrets.GenericSecret;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.SecretReference;
import org.forgerock.util.promise.Promise;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sun.identity.authentication.callbacks.ReCaptchaCallback;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CaptchaNodeTest {

    @Mock
    private Realm realm;

    @Mock
    private CaptchaNode.Config config;

    @Mock
    private Handler handler;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private SecretReferenceRetrievalService secretReferenceRetrievalService;

    @Mock
    private SecretReferenceCache secretReferenceCache;

    @Captor
    private ArgumentCaptor<Request> requestCaptor;

    @InjectMocks
    private CaptchaNode node;
    private static final String SITE_KEY = "siteKey";
    private static final String API_URI = "apiUri";
    private static final String DIV_CLASS = "divClass";
    private static final boolean RECAPTCHA_V3 = true;
    private static final boolean DISABLE_SUBMISSIONS = true;
    @Mock
    private SecretCache realmCache;

    @BeforeEach
    void setUp() throws Exception {

        when(config.siteKey()).thenReturn(SITE_KEY);
        when(config.captchaUri()).thenReturn("https://www.google.com/recaptcha/api/siteverify");
        when(config.apiUri()).thenReturn("https://www.google.com/recaptcha/api/siteverify");
        when(config.divClass()).thenReturn("g-recaptcha");
        when(config.reCaptchaV3()).thenReturn(RECAPTCHA_V3);
        when(config.scoreThreshold()).thenReturn("0.5");
    }

    @Test
    void shouldReturnSiteKeyDuringFirstPass() throws Exception {
        Action result = node.process(emptyTreeContext());

        assertThat(result.callbacks.size()).isEqualTo(1);
        assertThat(result.callbacks.get(0)).isInstanceOf(ReCaptchaCallback.class);
        ReCaptchaCallback callback = ((ReCaptchaCallback) result.callbacks.get(0));
        assertThat(callback.getSiteKey()).isEqualTo(SITE_KEY);
    }

    @Test
    void shouldThrowExceptionIfNoResponseProvided() throws Exception {
        ReCaptchaCallback callback = new ReCaptchaCallback(SITE_KEY, API_URI, DIV_CLASS, RECAPTCHA_V3,
                DISABLE_SUBMISSIONS);
        TreeContext context = new TreeContext(json(object()),
                new ExternalRequestContext.Builder().build(), singletonList(callback), Optional.empty());

        assertThatThrownBy(() -> node.process(context)).isInstanceOf(NodeProcessException.class);
    }

    @Test
    void shouldThrowExceptionIfResponseCannotBeVerified() throws Exception {
        when(config.secretKey()).thenReturn(Optional.of("my-secret-key"));
        ReCaptchaCallback callback = new ReCaptchaCallback(SITE_KEY, API_URI, DIV_CLASS, RECAPTCHA_V3,
                DISABLE_SUBMISSIONS);
        callback.setResponse("response");
        TreeContext context = new TreeContext(json(object()),
        new ExternalRequestContext.Builder().build(), singletonList(callback), Optional.empty());

        Promise promise = mock(Promise.class);

        when(handler.handle(any(), any())).thenReturn(promise);
        when(promise.getOrThrow()).thenThrow(InterruptedException.class);
        assertThatThrownBy(() -> node.process(context)).isInstanceOf(NodeProcessException.class);
    }

    @Test
    void shouldGoToFailOutcomeIfResponseContainsFailure() throws Exception {
        when(config.secretKey()).thenReturn(Optional.of("my-secret-key"));
        ReCaptchaCallback callback = new ReCaptchaCallback(SITE_KEY, API_URI, DIV_CLASS, RECAPTCHA_V3,
                DISABLE_SUBMISSIONS);
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
    void shouldGoToTrueOutcomeIfResponseContainsSuccessAndMeetsScoreThreshold() throws Exception {
        when(config.secretKey()).thenReturn(Optional.of("my-secret-key"));
        ReCaptchaCallback callback = new ReCaptchaCallback(SITE_KEY, API_URI, DIV_CLASS, RECAPTCHA_V3,
                DISABLE_SUBMISSIONS);
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
    void shouldGoToFalseOutcomeIfResponseDoesNotMeetThreshold() throws Exception {
        when(config.secretKey()).thenReturn(Optional.of("my-secret-key"));
        ReCaptchaCallback callback = new ReCaptchaCallback(SITE_KEY, API_URI, DIV_CLASS, RECAPTCHA_V3,
                DISABLE_SUBMISSIONS);
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

    @Test
    void shouldUseLabelWhenSecretKeyAndSecretLabelAreBothSet() throws Exception {
        var purpose = Purpose.purpose("am.authentication.nodes.captcha.banana.secret", GenericSecret.class);
        when(config.secretKeyPurpose()).thenReturn(Optional.of(purpose));
        when(secretReferenceCache.realm(realm)).thenReturn(realmCache);
        when(realmCache.active(purpose))
                .thenReturn(SecretReference.constant(GenericSecret.password("new-password".toCharArray())));
        ReCaptchaCallback callback = new ReCaptchaCallback(SITE_KEY, API_URI, DIV_CLASS, RECAPTCHA_V3,
                DISABLE_SUBMISSIONS);
        callback.setResponse("response");
        TreeContext context = new TreeContext(json(object()),
                new ExternalRequestContext.Builder().build(), singletonList(callback), Optional.empty());
        Response response = new Response(Status.OK);
        response.setEntity(json(object(
                field("success", true), field("score", 0.4)
        )));
        when(handler.handle(any(), requestCaptor.capture())).thenReturn(newResponsePromise(response));

        node.process(context);

        Request request = requestCaptor.getValue();
        assertThat(request.getEntity().getForm().get("secret")).contains("new-password");
        verify(config, times(0)).secretKey();
    }

    @Test
    void shouldUseSecretKeyWhenNoSecretLabelIsSet() throws Exception {

        // Given
        when(config.secretKey()).thenReturn(Optional.of("secret-key"));
        ReCaptchaCallback callback = new ReCaptchaCallback(SITE_KEY, API_URI, DIV_CLASS, RECAPTCHA_V3,
                DISABLE_SUBMISSIONS);
        callback.setResponse("response");
        TreeContext context = new TreeContext(json(object()),
                new ExternalRequestContext.Builder().build(), singletonList(callback), Optional.empty());
        Response response = new Response(Status.OK);
        response.setEntity(json(object(
                field("success", true), field("score", 0.4)
        )));
        when(handler.handle(any(), requestCaptor.capture()))
                .thenReturn(newResponsePromise(response));

        // When
        node.process(context);

        // Then
        Request request = requestCaptor.getValue();
        assertThat(request.getEntity().getForm().get("secret")).contains("secret-key");
    }

    @Test
    void shouldThrowExceptionIfNoSecretKeyOrSecretLabelIsSet() {
        // Given
        ReCaptchaCallback callback = new ReCaptchaCallback(SITE_KEY, API_URI, DIV_CLASS, RECAPTCHA_V3,
                DISABLE_SUBMISSIONS);
        callback.setResponse("success");
        TreeContext context = new TreeContext(json(object()),
                new ExternalRequestContext.Builder().build(), singletonList(callback), Optional.empty());

        // Then
        assertThatThrownBy(() -> node.process(context))
                .isInstanceOfSatisfying(NodeProcessException.class,
                        e -> assertThat(e.getMessage()).isEqualTo("No secret key found"));
    }
}
