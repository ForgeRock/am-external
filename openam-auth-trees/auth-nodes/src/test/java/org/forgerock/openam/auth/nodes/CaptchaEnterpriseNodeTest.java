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
 * Copyright 2024-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.http.protocol.Response.newResponsePromise;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.openam.auth.nodes.TreeContextFactory.emptyTreeContext;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.identity.authentication.callbacks.CaptchaEnterpriseCallback;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.secrets.cache.SecretCache;
import org.forgerock.openam.secrets.cache.SecretReferenceCache;
import org.forgerock.secrets.GenericSecret;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.SecretReference;
import org.forgerock.util.promise.Promise;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CaptchaEnterpriseNodeTest {

    @Mock
    private Realm realm;

    @Mock
    private CaptchaEnterpriseNode.Config config;

    @Mock
    private Handler handler;

    @Mock
    private SecretReferenceCache secretReferenceCache;

    @Captor
    private ArgumentCaptor<Request> requestCaptor;

    @InjectMocks
    private CaptchaEnterpriseNode node;
    private static final String SITE_KEY = "siteKey";
    private static final String PROJECT_ID = "PROJECT_ID";
    private static final String API_URI = "https://www.google.com/recaptcha/enterprise.js";
    private static final String CAPTCHA_URI = "https://recaptchaenterprise.googleapis.com/v1";
    private static final String DIV_CLASS = "g-recaptcha";
    private static final String THRESHOLD = "0.5";
    @Mock
    private SecretCache realmCache;

    @BeforeEach
    void setUp() throws Exception {

        when(config.siteKey()).thenReturn(SITE_KEY);
        when(config.projectId()).thenReturn(PROJECT_ID);
        when(config.captchaUri()).thenReturn(CAPTCHA_URI);
        when(config.apiUri()).thenReturn(API_URI);
        when(config.divClass()).thenReturn(DIV_CLASS);
        when(config.scoreThreshold()).thenReturn(THRESHOLD);
        when(config.storeErrors()).thenReturn(true);

        var purpose = Purpose
                .purpose("am.authentication.nodes.captchaEnterprise.enterprise.secret", GenericSecret.class);
        when(config.secretKeyPurpose()).thenReturn(Optional.of(purpose));
        when(secretReferenceCache.realm(realm)).thenReturn(realmCache);
        when(realmCache.active(purpose))
                .thenReturn(SecretReference.constant(GenericSecret.password("new-password".toCharArray())));
    }

    @Test
    void shouldReturnSiteKey() throws Exception {
        Action result = node.process(emptyTreeContext());

        assertThat(result.callbacks.size()).isEqualTo(1);
        assertThat(result.callbacks.get(0)).isInstanceOf(CaptchaEnterpriseCallback.class);
        CaptchaEnterpriseCallback callback = ((CaptchaEnterpriseCallback) result.callbacks.get(0));
        assertThat(callback.getSiteKey()).isEqualTo(SITE_KEY);
        assertThat(callback.getApiUri()).isEqualTo(API_URI);
        assertThat(callback.getDivClass()).isEqualTo(DIV_CLASS);
    }

    @Test
    void shouldFailGivenClientError() throws NodeProcessException {
        CaptchaEnterpriseCallback callback = new CaptchaEnterpriseCallback(SITE_KEY, API_URI, DIV_CLASS);
        String clientError = "Token expired";
        callback.setClientError(clientError);
        JsonValue sharedState = json(object());
        TreeContext context = new TreeContext(json(object()), sharedState,
                new ExternalRequestContext.Builder().build(), singletonList(callback), Optional.empty());
        Action action = node.process(context);
        assertThat(action.outcome).isEqualTo("false");
        assertThat(sharedState.get("CaptchaEnterpriseNode.FAILURE").asString())
                .isEqualTo("CLIENT_ERROR:" + clientError);
    }

    @Test
    void shouldFailGivenNoTokenProvided() throws NodeProcessException {
        CaptchaEnterpriseCallback callback = new CaptchaEnterpriseCallback(SITE_KEY, API_URI, DIV_CLASS);
        JsonValue sharedState = json(object());
        TreeContext context = new TreeContext(json(object()), sharedState, json(object()),
                new ExternalRequestContext.Builder().build(), singletonList(callback), Optional.empty());
        Action action = node.process(context);
        assertThat(action.outcome).isEqualTo("false");
        assertThat(sharedState.get("CaptchaEnterpriseNode.FAILURE").asString())
                .isEqualTo("INVALID_TOKEN:Token is empty");
    }

    @Test
    void shouldFailGivenProjectIdIsEmpty() throws NodeProcessException {
        when(config.projectId()).thenReturn("");
        CaptchaEnterpriseCallback callback = new CaptchaEnterpriseCallback(SITE_KEY, API_URI, DIV_CLASS);
        callback.setToken("success");

        JsonValue sharedState = json(object());
        TreeContext context = new TreeContext(json(object()), sharedState,
                new ExternalRequestContext.Builder().build(), singletonList(callback), Optional.empty());
        Action action = node.process(context);
        assertThat(action.outcome).isEqualTo("false");
        assertThat(sharedState.get("CaptchaEnterpriseNode.FAILURE").asString())
                .isEqualTo("INVALID_PROJECT_ID:Project ID is empty");
    }

    @Test
    void shouldFailEvenWhenFailureIsNotStored() throws NodeProcessException {
        when(config.secretKeyPurpose()).thenReturn(Optional.empty());
        when(config.storeErrors()).thenReturn(false);

        CaptchaEnterpriseCallback callback = new CaptchaEnterpriseCallback(SITE_KEY, API_URI, DIV_CLASS);
        callback.setToken("success");

        JsonValue sharedState = json(object());
        TreeContext context = new TreeContext(json(object()), sharedState,
                new ExternalRequestContext.Builder().build(), singletonList(callback), Optional.empty());
        Action action = node.process(context);
        assertThat(action.outcome).isEqualTo("false");
        assertThat(sharedState.get("CaptchaEnterpriseNode.FAILURE").asString())
                .isEqualTo(null);
    }

    @Test
    void shouldFailGivenSecretLabelNotSet() throws NodeProcessException {
        when(config.secretKeyPurpose()).thenReturn(Optional.empty());

        CaptchaEnterpriseCallback callback = new CaptchaEnterpriseCallback(SITE_KEY, API_URI, DIV_CLASS);
        callback.setToken("success");

        JsonValue sharedState = json(object());
        TreeContext context = new TreeContext(json(object()), sharedState,
                new ExternalRequestContext.Builder().build(), singletonList(callback), Optional.empty());
        Action action = node.process(context);
        assertThat(action.outcome).isEqualTo("false");
        assertThat(sharedState.get("CaptchaEnterpriseNode.FAILURE").asString())
                .isEqualTo("INVALID_SECRET_KEY:Secret key could not be retrieved");
    }

    @Test
    void shouldFailGivenSecretIdentifierMissing() throws Exception {
        var purpose = Purpose
                .purpose("am.authentication.nodes.captchaEnterprise.enterprise.secret", GenericSecret.class);
        when(config.secretKeyPurpose()).thenReturn(Optional.of(purpose));
        when(secretReferenceCache.realm(realm)).thenReturn(realmCache);
        when(realmCache.active(purpose))
                .thenReturn(SecretReference.constant(GenericSecret.password("".toCharArray())));
        CaptchaEnterpriseCallback callback = new CaptchaEnterpriseCallback(SITE_KEY, API_URI, DIV_CLASS);
        callback.setToken("response");
        callback.setAction("action");

        JsonValue sharedState = json(object());
        TreeContext context = new TreeContext(json(object()), sharedState,
                new ExternalRequestContext.Builder().build(), singletonList(callback), Optional.empty());
        Action action = node.process(context);
        assertThat(action.outcome).isEqualTo("false");
        assertThat(sharedState.get("CaptchaEnterpriseNode.FAILURE").asString())
                .isEqualTo("INVALID_SECRET_KEY:Secret key is empty");
    }

    @Test
    void shouldFailToGivenAPIFailure() throws Exception {
        CaptchaEnterpriseCallback callback = new CaptchaEnterpriseCallback(SITE_KEY, API_URI, DIV_CLASS);
        callback.setToken("response");

        JsonValue sharedState = json(object());
        TreeContext context = new TreeContext(json(object()), sharedState,
                new ExternalRequestContext.Builder().build(), singletonList(callback), Optional.empty());

        Promise promise = mock(Promise.class);

        when(handler.handle(any(), any())).thenReturn(promise);
        when(promise.getOrThrow()).thenThrow(new InterruptedException("API issue"));

        Action action = node.process(context);

        assertThat(action.outcome).isEqualTo("false");
        assertThat(sharedState.get("CaptchaEnterpriseNode.FAILURE").asString())
                .isEqualTo("API_ERROR:API issue");
    }

    @Test
    void shouldFailGivenIOFailure() throws Exception {
        CaptchaEnterpriseCallback callback = new CaptchaEnterpriseCallback(SITE_KEY, API_URI, DIV_CLASS);
        callback.setToken("response");

        JsonValue sharedState = json(object());
        TreeContext context = new TreeContext(json(object()), sharedState,
                new ExternalRequestContext.Builder().build(), singletonList(callback), Optional.empty());

        Promise promise = mock(Promise.class);

        when(handler.handle(any(), any())).thenReturn(promise);
        when(promise.getOrThrow()).thenThrow(new IOException("Network issue"));

        Action action = node.process(context);

        assertThat(action.outcome).isEqualTo("false");
        assertThat(sharedState.get("CaptchaEnterpriseNode.FAILURE").asString())
                .isEqualTo("IO_ERROR:Network issue");
    }

    @Test
    void shouldFailGivenResponseFailure() throws Exception {
        when(config.scoreThreshold()).thenReturn("0.1");
        CaptchaEnterpriseCallback callback = new CaptchaEnterpriseCallback(SITE_KEY, API_URI, DIV_CLASS);
        callback.setToken("success");
        JsonValue sharedState = json(object());
        TreeContext context = new TreeContext(json(object()), sharedState,
                new ExternalRequestContext.Builder().build(), singletonList(callback), Optional.empty());

        Response response = new Response(Status.OK);

        Map<String, Object> entity = object(field("riskAnalysis",
                object(field("score", 0.8))), field("tokenProperties", object(field("valid", false))));

        response.setEntity(entity);

        when(handler.handle(any(), any())).thenReturn(newResponsePromise(response));
        Action action = node.process(context);
        assertThat(action.outcome).isEqualTo("false");
        assertThat(sharedState.get("CaptchaEnterpriseNode.FAILURE").asString())
                .isEqualTo("VALIDATION_ERROR:CAPTCHA validation failed");
    }

    @Test
    void shouldPassDoSomethingGivenSuccessfulResponseAndScoreThreshold() throws Exception {
        CaptchaEnterpriseCallback callback = new CaptchaEnterpriseCallback(SITE_KEY, API_URI, DIV_CLASS);
        callback.setToken("response");

        TreeContext context = new TreeContext(json(object()),
                new ExternalRequestContext.Builder().build(), singletonList(callback), Optional.empty());

        Response response = new Response(Status.OK);
        Map<String, Object> entity = object(field("riskAnalysis",
                object(field("score", 0.8))), field("tokenProperties", object(field("valid", true))));
        response.setEntity(entity);

        when(handler.handle(any(), any())).thenReturn(newResponsePromise(response));
        Action action = node.process(context);

        assertThat(action.outcome).isEqualTo("true");
    }

    @Test
    void shouldPassDoSomethingGivenSuccessfulResponseAndExactScoreThreshold() throws Exception {
        CaptchaEnterpriseCallback callback = new CaptchaEnterpriseCallback(SITE_KEY, API_URI, DIV_CLASS);
        callback.setToken("response");

        TreeContext context = new TreeContext(json(object()),
                new ExternalRequestContext.Builder().build(), singletonList(callback), Optional.empty());

        Response response = new Response(Status.OK);
        Map<String, Object> entity = object(field("riskAnalysis",
                object(field("score", 0.5))), field("tokenProperties", object(field("valid", true))));
        response.setEntity(entity);

        when(handler.handle(any(), any())).thenReturn(newResponsePromise(response));
        Action action = node.process(context);

        assertThat(action.outcome).isEqualTo("true");
    }

    @Test
    void shouldFailGivenResponseDoesNotMeetScoreThreshold() throws Exception {

        CaptchaEnterpriseCallback callback = new CaptchaEnterpriseCallback(SITE_KEY, API_URI, DIV_CLASS);
        callback.setToken("response");
        JsonValue sharedState = json(object());
        TreeContext context = new TreeContext(json(object()), sharedState,
                new ExternalRequestContext.Builder().build(), singletonList(callback), Optional.empty());

        Response response = new Response(Status.OK);
        Map<String, Object> entity = object(field("riskAnalysis",
                object(field("score", 0.4))), field("tokenProperties", object(field("valid", true))));
        response.setEntity(entity);

        when(handler.handle(any(), any())).thenReturn(newResponsePromise(response));
        Action action = node.process(context);

        assertThat(action.outcome).isEqualTo("false");
        assertThat(sharedState.get("CaptchaEnterpriseNode.FAILURE").asString())
                .isEqualTo("VALIDATION_ERROR:CAPTCHA validation failed");
    }

    @Test
    void shouldPersistInTransientStateGivenSuccessfulResponseAndScoreThreshold() throws Exception {
        when(config.storeAssessmentResult()).thenReturn(true);
        CaptchaEnterpriseCallback callback = new CaptchaEnterpriseCallback(SITE_KEY, API_URI, DIV_CLASS);
        callback.setToken("response");
        JsonValue transientState = json(object());
        TreeContext context = new TreeContext(json(object()), transientState,
                new ExternalRequestContext.Builder().build(), singletonList(callback), Optional.empty());

        Response response = new Response(Status.OK);
        Map<String, Object> entity = object(field("riskAnalysis",
                object(field("score", 0.8))), field("tokenProperties", object(field("valid", true))));
        response.setEntity(entity);

        when(handler.handle(any(), any())).thenReturn(newResponsePromise(response));
        Action action = node.process(context);

        assertThat(transientState.get("CaptchaEnterpriseNode.ASSESSMENT_RESULT")
                .get("riskAnalysis").get("score").asDouble()).isEqualTo(0.8);
        assertThat(transientState.get("CaptchaEnterpriseNode.ASSESSMENT_RESULT")
                .get("tokenProperties").get("valid").asBoolean()).isTrue();
        assertThat(action.outcome).isEqualTo("true");
    }

    @Test
    void shouldVerifySecretKeyExistsInRequest() throws Exception {
        var purpose = Purpose
                .purpose("am.authentication.nodes.captchaEnterprise.enterprise.secret", GenericSecret.class);
        when(config.secretKeyPurpose()).thenReturn(Optional.of(purpose));
        when(secretReferenceCache.realm(realm)).thenReturn(realmCache);
        when(realmCache.active(purpose))
                .thenReturn(SecretReference.constant(GenericSecret.password("new-password".toCharArray())));
        CaptchaEnterpriseCallback callback = new CaptchaEnterpriseCallback(SITE_KEY, API_URI, DIV_CLASS);
        callback.setToken("response");
        TreeContext context = new TreeContext(json(object()),
                new ExternalRequestContext.Builder().build(), singletonList(callback), Optional.empty());
        Response response = new Response(Status.OK);
        Map<String, Object> entity = object(field("riskAnalysis",
                object(field("score", 0.4))), field("tokenProperties", object(field("valid", true))));
        response.setEntity(entity);
        when(handler.handle(any(), requestCaptor.capture())).thenReturn(newResponsePromise(response));
        node.process(context);
        Request request = requestCaptor.getValue();
        assertThat(request.getUri().toString()).contains("new-password");
        assertThat(request.getMethod()).isEqualTo("POST");
    }

    @Test
    void shouldValidateCaptchaRequestBody() throws Exception {
        var purpose = Purpose
                .purpose("am.authentication.nodes.captchaEnterprise.enterprise.secret", GenericSecret.class);
        when(config.secretKeyPurpose()).thenReturn(Optional.of(purpose));
        when(secretReferenceCache.realm(realm)).thenReturn(realmCache);
        when(realmCache.active(purpose))
                .thenReturn(SecretReference.constant(GenericSecret.password("new-password".toCharArray())));
        CaptchaEnterpriseCallback callback = new CaptchaEnterpriseCallback(SITE_KEY, API_URI, DIV_CLASS);
        callback.setToken("response");
        callback.setAction("action");
        TreeContext context = new TreeContext(json(object()),
                new ExternalRequestContext.Builder().build(), singletonList(callback), Optional.empty());
        Response response = new Response(Status.OK);
        Map<String, Object> entity = object(field("riskAnalysis",
                object(field("score", 0.4))), field("tokenProperties", object(field("valid", true))));
        response.setEntity(entity);
        when(handler.handle(any(), requestCaptor.capture())).thenReturn(newResponsePromise(response));
        node.process(context);
        Request request = requestCaptor.getValue();
        assertThat(request.getUri().toString()).contains("new-password");
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getEntity().getString()).contains("response");
        assertThat(request.getEntity().getString()).contains("action");
        assertThat(request.getEntity().getString()).contains("siteKey");
        assertThat(request.getEntity().getString()).contains("expectedAction");
        assertThat(request.getEntity().getString()).contains("userIpAddress");
    }

    @Test
    void shouldValidateAdditionalAttributesInPayload() throws NodeProcessException, IOException {
        CaptchaEnterpriseCallback callback = new CaptchaEnterpriseCallback(SITE_KEY, API_URI, DIV_CLASS);
        callback.setToken("response");
        callback.setAction("action");
        JsonValue transientState = json(object());
        JsonValue sharedState = json(object());
        String jsonString = "{"
                + "\"userInfo\": \"userInfo\","
                + "\"transactionData\": \"transactionData\","
                + "\"firewallPolicyEvaluation\": \"firewallPolicyEvaluation\","
                + "\"wafTokenAssessment\": \"wafTokenAssessment\","
                + "\"requestedUri\": \"requestedUri\","
                + "\"express\": \"express\","
                + "\"userAgent\": [\"userAgent1\", \"userAgent2\", \"userAgent3\"],"
                + "\"userIpAddress\": \"userIpAddress\","
                + "\"siteKey\": \"siteKey\","
                + "\"expectedAction\": \"expectedAction\","
                + "\"token\": \"token\","
                + "\"ja3\": \"ja3\","
                + "\"headers\": \"headers\","
                + "\"fraudPrevention\": \"fraudPrevention\""
                + "}";
        callback.setPayload(jsonString);
        TreeContext context = new TreeContext(sharedState, transientState,
                new ExternalRequestContext.Builder().build(), singletonList(callback), Optional.empty());
        Response response = new Response(Status.OK);
        Map<String, Object> entity = object(field("riskAnalysis",
                object(field("score", 0.4))), field("tokenProperties", object(field("valid", true))));
        response.setEntity(entity);
        when(handler.handle(any(), requestCaptor.capture())).thenReturn(newResponsePromise(response));
        node.process(context);
        Request request = requestCaptor.getValue();
        assertThat(request.getUri().toString()).contains("new-password");
        assertThat(request.getMethod()).isEqualTo("POST");
        // Validate JSON payload includes all expected fields
        String requestBody = request.getEntity().getJson().toString();
        List<String> expectedFields = List.of(
                "userInfo", "transactionData", "fraudPrevention", "ja3", "requestedUri",
                "firewallPolicyEvaluation", "wafTokenAssessment", "express", "headers",
                "userAgent", "userIpAddress", "expectedAction", "token", "siteKey", "userAgent1, userAgent2, userAgent3"
        );

        for (String field : expectedFields) {
            assertThat(requestBody).contains(field);
        }
    }

    @Test
    void shouldValidateAdditionalAttributesInSharedState() throws NodeProcessException, IOException {
        CaptchaEnterpriseCallback callback = new CaptchaEnterpriseCallback(SITE_KEY, API_URI, DIV_CLASS);
        callback.setToken("response");
        callback.setAction("action");
        JsonValue transientState = json(object());
        JsonValue sharedState = json(object());
        sharedState.put("CaptchaEnterpriseNode.PAYLOAD", object(field("userInfo", "userInfo"),
                field("transactionData", "transactionData"),
                field("firewallPolicyEvaluation", "firewallPolicyEvaluation"),
                field("wafTokenAssessment", "wafTokenAssessment"),
                field("requestedUri", "requestedUri"),
                field("express", "express"),
                field("userAgent", "userAgent"),
                field("userIpAddress", "userIpAddress"),
                field("siteKey", "siteKey"),
                field("expectedAction", "expectedAction"),
                field("token", "token"),
                field("ja3", "ja3"),
                field("headers", "headers"),
                field("fraudPrevention", "fraudPrevention")));

        TreeContext context = new TreeContext(sharedState, transientState,
                new ExternalRequestContext.Builder().build(), singletonList(callback), Optional.empty());
        Response response = new Response(Status.OK);
        Map<String, Object> entity = object(field("riskAnalysis",
                object(field("score", 0.4))), field("tokenProperties", object(field("valid", true))));
        response.setEntity(entity);

        when(handler.handle(any(), requestCaptor.capture())).thenReturn(newResponsePromise(response));

        node.process(context);

        Request request = requestCaptor.getValue();

        assertThat(request.getUri().toString()).contains("new-password");
        assertThat(request.getMethod()).isEqualTo("POST");
        // Validate JSON payload includes all expected fields
        String requestBody = request.getEntity().getJson().toString();
        List<String> expectedFields = List.of(
                "userInfo", "transactionData", "fraudPrevention", "ja3", "requestedUri",
                "firewallPolicyEvaluation", "wafTokenAssessment", "express", "headers",
                "userAgent", "userIpAddress", "expectedAction", "token", "siteKey"
        );

        for (String field : expectedFields) {
            assertThat(requestBody).contains(field);
        }
    }
}
