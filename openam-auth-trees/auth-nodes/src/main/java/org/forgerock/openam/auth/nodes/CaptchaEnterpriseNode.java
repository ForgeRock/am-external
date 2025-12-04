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
 * Copyright 2024-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.oauth.clients.oauth2.OAuth2Client.HTTP_POST;
import static org.forgerock.openam.auth.node.api.Action.send;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.callbacks.CaptchaEnterpriseCallback;
import com.sun.identity.sm.RequiredValueValidator;
import com.sun.identity.sm.SMSException;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Namespace;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.nodes.validators.DecimalBetweenZeroAndOneValidator;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.secrets.cache.SecretReferenceCache;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.forgerock.openam.sm.ServiceConfigException;
import org.forgerock.openam.sm.ServiceConfigValidator;
import org.forgerock.openam.sm.ServiceErrorException;
import org.forgerock.openam.sm.annotations.adapters.SecretPurpose;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.secrets.GenericSecret;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.SecretReference;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A node that displays a CAPTCHA to the user and verifies the user's response.
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = CaptchaEnterpriseNode.Config.class,
        tags = {"risk"},
        namespace = Namespace.PRODUCT, configValidator = CaptchaEnterpriseNode.ConfigValidator.class)
public class CaptchaEnterpriseNode extends AbstractDecisionNode {

    private static final Logger logger = LoggerFactory.getLogger(CaptchaEnterpriseNode.class);
    private static final String ASSESSMENT_RESULT = "CaptchaEnterpriseNode.ASSESSMENT_RESULT";
    private static final String FAILURE_REASON = "CaptchaEnterpriseNode.FAILURE";
    private static final String PAYLOAD = "CaptchaEnterpriseNode.PAYLOAD";

    private final Config config;
    private final Handler handler;
    private final Realm realm;
    private final SecretReferenceCache secretReferenceCache;

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * The reCAPTCHA project id.
         *
         * @return the key.
         */
        @Attribute(order = 100)
        String projectId();

        /**
         * The reCAPTCHA site key.
         *
         * @return the key.
         */
        @Attribute(order = 200)
        String siteKey();

        /**
         * The secret key purpose.
         *
         * @return The secret key purpose.
         */
        @Attribute(order = 300, resourceName = "secretLabelIdentifier", validators = {RequiredValueValidator.class})
        @SecretPurpose("am.authentication.nodes.captchaEnterprise.%s.secret")
        Optional<Purpose<GenericSecret>> secretKeyPurpose();

        /**
         * The threshold that the score must be greater than or equal to for a successful response.
         * A number between 0.0 and 1.0.
         *
         * @return the score threshold.
         */
        @Attribute(order = 400, validators = {DecimalBetweenZeroAndOneValidator.class})
        default String scoreThreshold() {
            return "0.0";
        }

        /**
         * Store evaluate result.
         *
         * @return True to store the result in Transient State.
         */
        @Attribute(order = 500)
        default boolean storeAssessmentResult() {
            return false;
        }

       /**
         * Store errors.
         *
         * @return True to store the error in Transient State
         */
        @Attribute(order = 600)
        default boolean storeErrors() {
            return false;
        }

        /**
         * The class of the captcha div that appears on the login screen.
         *
         * @return the class of the captcha div that appears on the login screen.
         */
        @Attribute(order = 700)
        default String divClass() {
            return "g-recaptcha";
        }

        /**
         * The uri to verify the reCAPTCHA.
         *
         * @return the verification URI.
         */
        @Attribute(order = 800)
        default String captchaUri() {
            return "https://recaptchaenterprise.googleapis.com/v1";
        }

        /**
         * The uri of the captcha API.
         *
         * @return the API uri.
         */
        @Attribute(order = 900)
        default String apiUri() {
            return "https://www.google.com/recaptcha/enterprise.js";
        }

    }

    /**
     * Constructs a new {@link CaptchaEnterpriseNode} instance.
     *
     * @param config The node configuration.
     * @param handler The http handler.
     * @param realm The current realm.
     * @param secretReferenceCache The secret reference cache.
     */
    @Inject
    public CaptchaEnterpriseNode(@Assisted Config config, @Named("CloseableHttpClientHandler") Handler handler,
                                 @Assisted Realm realm, SecretReferenceCache secretReferenceCache) {
        this.config = config;
        this.handler = handler;
        this.realm = realm;
        this.secretReferenceCache = secretReferenceCache;
    }

    /**
     * Processes the CAPTCHA response.
     *
     * @param context The current tree context.
     * @return The next action.
     * @throws NodeProcessException If the CAPTCHA response could not be processed.
     */
    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("CaptchaEnterpriseNode started");
        Optional<CaptchaEnterpriseCallback> callback = context.getCallback(CaptchaEnterpriseCallback.class);
        NodeState state = context.getStateFor(this);
        if (callback.isEmpty()) {
            return send(new CaptchaEnterpriseCallback(config.siteKey(),
                    config.apiUri(), config.divClass())).build();
        }

        if (callback.get().getClientError() != null && !callback.get().getClientError().isEmpty()) {
            return handleError(state, FailureReason.CLIENT_ERROR, callback.get().getClientError());
        }

        if (callback.get().getToken() == null || callback.get().getToken().isEmpty()) {
            return handleError(state,  FailureReason.INVALID_TOKEN, "Token is empty");
        }
        String secretKey;
        try {
            secretKey = getSecretKey();
            if (secretKey == null || secretKey.isEmpty()) {
                return handleError(state,  FailureReason.INVALID_SECRET_KEY, "Secret key is empty");
            }
        } catch (NodeProcessException e) {
            return handleError(state,  FailureReason.INVALID_SECRET_KEY, e.getMessage());
        }

        if (config.projectId() == null || config.projectId().isEmpty()) {
            return handleError(state, FailureReason.INVALID_PROJECT_ID, "Project ID is empty");
        }

        try {
            Response verificationResponse = getCaptchaResponse(buildUri(secretKey),
                    callback.get().getToken(),
                    callback.get().getAction(),
                    callback.get().getPayload(),
                    context.request,
                    state);

            if (verificationResponse.getStatus().getCode() != 200) {
                return handleError(state, FailureReason.API_ERROR, verificationResponse.getEntity().getString());
            }
            JsonValue json = json(verificationResponse.getEntity().getJson());
            CaptchaResponse captchaResponse = new CaptchaResponse(json);
            if (config.storeAssessmentResult()) {
                state.putTransient(ASSESSMENT_RESULT, json);
            }
            if (!captchaResponse.success()
                    || !isValidScore(captchaResponse.score())) {
                return handleError(state, FailureReason.VALIDATION_ERROR, "CAPTCHA validation failed");
            }
            logger.debug("CAPTCHA successfully verified");
            return goTo(true).build();
        } catch (InterruptedException e) {
            logger.warn("Captcha Interrupted", e);
            return handleError(state,  FailureReason.API_ERROR, e.getMessage());
        } catch (IOException e) {
            logger.warn("Captcha IOError", e);
            return handleError(state,  FailureReason.IO_ERROR, e.getMessage());
        } catch (Exception e) {
            logger.warn("Captcha Enterprise failed", e);
            return handleError(state, FailureReason.UNKNOWN, e.getMessage());
        }
    }

    /**
     * Handles the CAPTCHA validation failure.
     *
     * @param state   The current node state.
     * @param reason  The reason for the failure.
     * @param message The message to store in the node state.
     * @return The next action.
     */
    private Action handleError(NodeState state, FailureReason reason, String message) {
        if (config.storeErrors()) {
            state.putTransient(FAILURE_REASON, reason.name() + ":" + message);
        }
        return goTo(false).build();
    }

    /**
     * Validates the input score.
     *
     * @param score a number between 0.0 and 1.0, may be null if using a CAPTCHA method that does not provide a score.
     * @return true if there is no score or if the score is greater than or equal to the threshold, else false.
     */
    private boolean isValidScore(Double score) {
        return score == null
                || config.scoreThreshold() == null
                || score >= Double.parseDouble(config.scoreThreshold());
    }

    /**
     * Retrieves the CAPTCHA response.
     *
     * @param uri             The URI for the CAPTCHA request.
     * @param token           The CAPTCHA token.
     * @param action          The action name for this request.
     * @param externalRequest The external request context.
     * @param state           The current node state.
     * @return The CAPTCHA response.
     * @throws InterruptedException If the request is interrupted.
     */
    private Response getCaptchaResponse(URI uri,
                                        String token,
                                        String action,
                                        String payload,
                                        ExternalRequestContext externalRequest,
                                        NodeState state) throws InterruptedException {
        Map<String, Object> captchaBody = buildCaptchaBody(token, action, payload, externalRequest, state);
        Request request = new Request().setUri(uri)
                .setMethod(HTTP_POST);
        request.getEntity().setJson(captchaBody);
        return handler.handle(new RootContext(), request)
                .getOrThrow();
    }

    /**
     * Builds the body of the CAPTCHA request.
     *
     * @param token           The CAPTCHA token.
     * @param action          The action name for this request.
     * @param externalRequest The external request context.
     * @param state           The current node state.
     * @return The body of the CAPTCHA request.
     */
    private Map<String, Object> buildCaptchaBody(String token,
                                                 String action,
                                                 String clientPayload,
                                                 ExternalRequestContext externalRequest,
                                                 NodeState state) {
        String userIPAddress = externalRequest.clientIp;
        String userAgent = String.join(",", externalRequest.headers.get(CaptchaEventConstant.USER_AGENT.toString()));
        Map<String, Object> captchaBody = object(field(CaptchaEventConstant.TOKEN.value, token),
                field(CaptchaEventConstant.SITE_KEY.toString(), config.siteKey()));
        if (userIPAddress != null && !userIPAddress.isEmpty()) {
            captchaBody.put(CaptchaEventConstant.USER_IPADDRESS.toString(), userIPAddress);
        }
        if (!userAgent.isEmpty()) {
            captchaBody.put(CaptchaEventConstant.USERAGENT.toString(), userAgent);
        }
        if (action != null && !action.isEmpty()) {
            captchaBody.put(CaptchaEventConstant.EXPECTED_ACTION.toString(), action);
        }
        if (clientPayload != null && !clientPayload.isEmpty()) {
            JsonValue value = JsonValueBuilder.toJsonValue(clientPayload);
            addMoreAttributes(captchaBody, value);
        }
        JsonValue sharedStatePayload = state.get(PAYLOAD);
        if (sharedStatePayload != null && sharedStatePayload.isNotNull()) {
            addMoreAttributes(captchaBody, sharedStatePayload);
        }
        return object(field(CaptchaEventConstant.EVENT.toString(), captchaBody));
    }

    /**
     * Adds the CAPTCHA protect data to the body of the CAPTCHA request.
     *
     * @param captchaBody The body of the CAPTCHA request.
     * @param payload    The payload.
     */
    private void addMoreAttributes(Map<String, Object> captchaBody, JsonValue payload) {
        payload.keys().forEach(key -> {
            if (payload.get(key).isNotNull()) {
                captchaBody.put(key, payload.get(key).getObject());
            }
        });
    }

    /**
     * Builds the URI for the CAPTCHA request.
     *
     * @param secretKey The secret key.
     * @return The URI for the CAPTCHA request.
     */
    private URI buildUri(String secretKey) {
        String path = "/projects/" + config.projectId() + "/assessments?key=" + secretKey;
        return URI.create(config.captchaUri() + path);
    }

    /**
     * Retrieves the secret key from the secret store.
     *
     * @return The secret key.
     */
    private String getSecretKey() throws NodeProcessException {
        return config.secretKeyPurpose()
                .map(passwordPurpose -> secretReferenceCache.realm(realm).active(passwordPurpose))
                .map(SecretReference::getAsync)
                .map(promise -> promise.then(s -> s.revealAsUtf8(String::new)))
                .map(promise -> promise.thenCatch(ex -> {
                    logger.debug("Failed to get secret from store ", ex);
                    return null;
                }))
                .map(Promise::getOrThrowIfInterrupted)
                .orElseThrow(() -> new NodeProcessException("Secret key could not be retrieved"));
    }

    /**
     * Constants representing the CAPTCHA event.
     */
    private enum CaptchaEventConstant {
        USER_AGENT("User-Agent"),
        USERAGENT("userAgent"),
        TOKEN("token"),
        EVENT("event"),
        SITE_KEY("siteKey"),
        USER_IPADDRESS("userIpAddress"),
        EXPECTED_ACTION("expectedAction");

        private final String value;

        CaptchaEventConstant(String value) {
            this.value = value;
        }

        public String toString() {
            return value;
        }
    }

    /**
     * Enum representing various CAPTCHA validation failure reasons.
     */
    private enum FailureReason {
        /**
         * Invalid Token.
         */
        INVALID_TOKEN,

        /**
         * Invalid Project ID.
         */
        INVALID_PROJECT_ID,

        /**
         * Client Error.
         */
        CLIENT_ERROR,

        /**
         * Invalid Secret Key.
         */
        INVALID_SECRET_KEY,

        /**
         * Validation Error.
         */
        VALIDATION_ERROR,

        /**
         * Interrupted Exception.
         */
        API_ERROR,

        /**
         * IO Exception.
         */
        IO_ERROR,

        /**
         * Unknown Error.
         */
        UNKNOWN;

        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

    }

    /**
     * A class representing the CAPTCHA response.
     * @param success Whether the CAPTCHA was successful.
     * @param score The score of the CAPTCHA.
     * @param action The action of the CAPTCHA.
     * @param invalidReason The reason the CAPTCHA was invalid.
     * @param reasonCodes The reason codes for the CAPTCHA.
     */
    public record CaptchaResponse(
            boolean success,
            Double score,
            String action,
            String invalidReason,
            List<String> reasonCodes) {

        /**
         * Constructs a new {@link CaptchaResponse} instance.
         *
         * @param json The JSON response.
         * @throws IOException If the JSON response could not be parsed.
         */
        public CaptchaResponse(JsonValue json) throws IOException {
            this(
                    json.get(ResponseConstant.TOKEN_PROPERTIES.toString())
                            .get(ResponseConstant.VALID.value).asBoolean(),
                    json.get(ResponseConstant.RISK_ANALYSIS.toString())
                            .get(ResponseConstant.SCORE.toString()).asDouble(),
                    json.get(ResponseConstant.TOKEN_PROPERTIES.toString())
                            .get(ResponseConstant.ACTION.toString()).asString(),
                    json.get(ResponseConstant.TOKEN_PROPERTIES.toString())
                            .get(ResponseConstant.INVALID_REASON.toString()).asString(),
                    json.get(ResponseConstant.RISK_ANALYSIS.toString())
                            .get(ResponseConstant.REASON_CODES.toString()).asList(String.class)
            );
        }

        /**
         * Constants representing the JSON keys returned by CAPTCHA providers.
         */
        private enum ResponseConstant {
            SCORE("score"),
            TOKEN_PROPERTIES("tokenProperties"),
            VALID("valid"),
            RISK_ANALYSIS("riskAnalysis"),
            ACTION("action"),
            INVALID_REASON("invalidReason"),
            REASON_CODES("reasons");

            private final String value;

            ResponseConstant(String value) {
                this.value = value;
            }

            public String toString() {
                return value;
            }
        }
    }


    /**
     * A config validator for the captcha node.
     *
     * Ensures that all required attributes are present and that either the secret key or secret key purpose is
     * provided.
     */
    static class ConfigValidator implements ServiceConfigValidator {

        private final AnnotatedServiceRegistry serviceRegistry;

        @Inject
        ConfigValidator(AnnotatedServiceRegistry serviceRegistry) {
            this.serviceRegistry = serviceRegistry;
        }

        @Override
        public void validate(Realm realm, List<String> configPath, Map<String, Set<String>> attributes)
                throws ServiceConfigException, ServiceErrorException {
            Reject.ifNull(configPath, "The configuration path is required for validation.");
            Reject.ifTrue(configPath.isEmpty(), "The configuration path is required for validation.");
            Reject.ifNull(realm, "The realm is required for validation.");
            Reject.ifNull(attributes, "Attributes are required for validation");
            String nodeId = configPath.get(configPath.size() - 1);
            try {
                serviceRegistry
                        .getRealmInstance(CaptchaEnterpriseNode.Config.class, realm, nodeId);
            } catch (SSOException | SMSException e) {
                logger.error("An error occurred validating CAPTCHA Node configuration", e);
                throw new ServiceErrorException("Error validating CAPTCHA Node configuration");
            }
        }
    }
}
