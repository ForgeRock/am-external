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
 * Copyright 2021-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.oauth.clients.oauth2.OAuth2Client.HTTP_POST;
import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.utils.CollectionUtils.getFirstItem;
import static org.forgerock.openam.utils.StringUtils.isBlank;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Form;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonException;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Namespace;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.validators.DecimalBetweenZeroAndOneValidator;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.secrets.cache.SecretReferenceCache;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.forgerock.openam.sm.ServiceConfigException;
import org.forgerock.openam.sm.ServiceConfigValidator;
import org.forgerock.openam.sm.ServiceErrorException;
import org.forgerock.openam.sm.annotations.adapters.SecretPurpose;
import org.forgerock.secrets.GenericSecret;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.SecretReference;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.callbacks.ReCaptchaCallback;
import com.sun.identity.sm.RequiredValueValidator;
import com.sun.identity.sm.SMSException;

/**
 * A node that displays a CAPTCHA to a user and verifies their response, the default values are for Google
 * ReCAPTCHA but other CAPTCHA providers can be used.
 * <p>
 * For hCAPTCHA use the following configuration values:
 *     <ul>
 *       <li> CAPTCHA Site Key: &lt;your site key&gt; </li>
 *       <li> CAPTCHA Secret Key: &lt;your secret key&gt; </li>
 *       <li> CAPTCHA Verification URL: https://hcaptcha.com/siteverify </li>
 *       <li> CAPTCHA API URL: https://hcaptcha.com/1/api.js </li>
 *       <li> Class of CAPTCHA &lt;div&gt; : h-captcha </li>
 *     </ul>
 * </p>
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = CaptchaNode.Config.class,
        tags = {"risk"},
        namespace = Namespace.PRODUCT, configValidator = CaptchaNode.ConfigValidator.class)
public class CaptchaNode extends AbstractDecisionNode {

    private static final Logger logger = LoggerFactory.getLogger(CaptchaNode.class);

    /**
     * Configuration for the node.
     */
    public interface Config {

        /**
         * The reCAPTCHA site key.
         *
         * @return the key.
         */
        @Attribute(order = 100, validators = {RequiredValueValidator.class})
        String siteKey();

        /**
         * The reCAPTCHA secret key.
         *
         * @return the key.
         */
        @Deprecated
        @Attribute(order = 200)
        Optional<String> secretKey();

        /**
         * The (optional) secret key purpose.
         *
         * @return The secret key purpose.
         */
        @Attribute(order = 250, resourceName = "secretLabelIdentifier")
        @SecretPurpose("am.authentication.nodes.captcha.%s.secret")
        Optional<Purpose<GenericSecret>> secretKeyPurpose();

        /**
         * The uri to verify the reCAPTCHA.
         *
         * @return the verification URI.
         */
        @Attribute(order = 300, validators = {RequiredValueValidator.class})
        default String captchaUri() {
            return "https://www.google.com/recaptcha/api/siteverify";
        }

        /**
         * The uri of the captcha API.
         *
         * @return the API uri
         */
        @Attribute(order = 400, validators = {RequiredValueValidator.class})
        default String apiUri() {
            return "https://www.google.com/recaptcha/api.js";
        }

        /**
         * The class of the captcha div that appears on the login screen.
         *
         * @return the class of the captcha div that appears on the login screen
         */
        @Attribute(order = 500, validators = {RequiredValueValidator.class})
        default String divClass() {
            return "g-recaptcha";
        }

        /**
         * Boolean representing whether this node is a ReCaptcha V3.
         *
         * @return true if using frictionless ReCaptcha V3, else false.
         */
        @Attribute(order = 600, validators = {RequiredValueValidator.class})
        default boolean reCaptchaV3() {
            return false;
        }

        /**
         * The threshold that the score must be greater than or equal to for a successful response. Currently only used
         * by ReCaptcha V3 and hCaptcha enterprise.
         *
         * @return the score threshold.
         */
        @Attribute(order = 700, validators = {DecimalBetweenZeroAndOneValidator.class})
        default String scoreThreshold() {
            return "0.0";
        }

        /**
         * Boolean representing whether to disable form submission until CAPTCHA verification has succeeded.
         *
         * @return true if disabling form submission until verified, else false.
         */
        @Attribute(order = 800, validators = {RequiredValueValidator.class})
        default boolean disableSubmission() {
            return true;
        }
    }

    private final Config config;
    private final Handler handler;
    private final Realm realm;
    private final SecretReferenceCache secretReferenceCache;

    /**
     * Guice constructor.
     *
     * @param config The node configuration.
     * @param handler The http handler.
     * @param realm The current realm.
     * @param secretReferenceCache The secret reference cache.
     */
    @Inject
    public CaptchaNode(@Assisted Config config, @Named("CloseableHttpClientHandler") Handler handler,
                       @Assisted Realm realm, SecretReferenceCache secretReferenceCache) {
        this.config = config;
        this.handler = handler;
        this.realm = realm;
        this.secretReferenceCache = secretReferenceCache;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("CaptchaNode started");
        Optional<ReCaptchaCallback> callback = context.getCallback(ReCaptchaCallback.class);

        if (callback.isEmpty()) {
            logger.debug("no callback present, creating new ReCaptchaCallback");
            return send(new ReCaptchaCallback(config.siteKey(), config.apiUri(), config.divClass(),
                    config.reCaptchaV3(), config.disableSubmission())).build();
        }

        if (callback.get().getResponse() == null || callback.get().getResponse().isEmpty()) {
            logger.debug("no CAPTCHA response in callback");
            throw new NodeProcessException("CAPTCHA response required for verification");
        }

        CaptchaResponse captchaResponse = getCaptchaResponse(callback.get().getResponse());

        if (captchaResponse.isSuccess() && validateScore(captchaResponse.getScore())) {
            logger.debug("CAPTCHA successfully verified");
            return goTo(true).build();
        }

        logger.debug("verification of CAPTCHA response was not successful");
        if (captchaResponse.getErrorCodes() != null) {
            logger.debug("Error codes: {}", captchaResponse.getErrorCodes());
        }

        return goTo(false).build();
    }

    /**
     * Validates the input score.
     *
     * @param score a number between 0.0 and 1.0, may be null if using a CAPTCHA method that does not provide a score.
     * @return true if there is no score or if the score is greater than or equal to the threshold, else false.
     */
    private boolean validateScore(Double score) {
        return score == null
                || config.scoreThreshold() == null
                || score >= Double.parseDouble(config.scoreThreshold());
    }

    private CaptchaResponse getCaptchaResponse(String response) throws NodeProcessException {
        URI uri = URI.create(config.captchaUri());
        try {
            Form form = new Form();
            form.fromFormString("secret=" + getSecretKey() + "&response=" + response);
            Request request = new Request().setUri(uri)
                    .setMethod(HTTP_POST);
            form.toRequestEntity(request);
            Response verificationResponse = handler.handle(new RootContext(), request)
                    .getOrThrow();
            return new CaptchaResponse(verificationResponse);
        } catch (InterruptedException e) {
            logger.debug("Unable to verify CAPTCHA response", e);
            throw new NodeProcessException("Unable to verify CAPTCHA response");
        } catch (IOException e) {
            logger.debug("Unable to retrieve state from token response", e);
            throw new JsonException("Unable to retrieve state from token response");
        }
    }

    private String getSecretKey() throws NodeProcessException {
        return config.secretKeyPurpose()
                .map(purpose -> secretReferenceCache.realm(realm).active(purpose))
                .map(SecretReference::getAsync)
                .map(promise -> promise.then(secret -> secret.revealAsUtf8(String::new))
                                        .thenCatch(ex -> null)
                                        .then(Optional::ofNullable))
                .map(Promise::getOrThrowIfInterrupted)
                .orElseGet(config::secretKey)
                .orElseThrow(() -> new NodeProcessException("No secret key found"));
    }

    /**
     * POJO representing the response from the CAPTCHA provider.
     */
    static class CaptchaResponse {

        /**
         * Whether this request was a valid CAPTCHA token for your site.
         */
        private final boolean success;

        /**
         * The score for this request (0.0 - 1.0). CAPTCHA V3 and hCaptcha enterprise only.
         */
        private final Double score;

        /**
         * The action name for this request.
         */
        private final String action;

        /**
         * The hostname of the site where the CAPTCHA was solved.
         */
        private final String hostname;

        /**
         * Error codes in response, optional.
         */
        private final List<String> errorCodes;

        /**
         * Reason(s) for score, hCaptcha enterprise only.
         */
        private final String scoreReason;

        CaptchaResponse(Response response) throws IOException {
            JsonValue json = json(response.getEntity().getJson());
            success = json.get(ResponseConstants.SUCCESS).asBoolean();
            score = json.get(ResponseConstants.SCORE).asDouble();
            action = json.get(ResponseConstants.ACTION).asString();
            hostname = json.get(ResponseConstants.HOSTNAME).asString();
            errorCodes = json.get(ResponseConstants.ERROR_CODES).asList(String.class);
            scoreReason = json.get(ResponseConstants.SCORE_REASON).toString();
        }

        public boolean isSuccess() {
            return success;
        }

        public Double getScore() {
            return score;
        }

        public String getAction() {
            return action;
        }

        public String getHostname() {
            return hostname;
        }

        public List<String> getErrorCodes() {
            return errorCodes;
        }

        public String getScoreReason() {
            return scoreReason;
        }

        /**
         * Constants representing the JSON keys returned by CAPTCHA providers.
         */
        private static final class ResponseConstants {
            private static final String SUCCESS = "success";
            private static final String SCORE = "score";
            private static final String SCORE_REASON = "score_reason";
            private static final String ACTION = "action";
            private static final String HOSTNAME = "hostname";
            private static final String ERROR_CODES = "error-codes";
        }
    }

    /**
     * A config validator for the captcha node.
     *
     * Ensures that all required attributes are present and that either the secret key or secret key purpose is
     * provided.
     */
    static class ConfigValidator implements ServiceConfigValidator {

        private static final String SECRET_KEY_METHOD_NAME = "secretKey";
        private static final String SECRET_KEY_PURPOSE_METHOD_NAME = "secretKeyPurpose";

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
            Optional<CaptchaNode.Config> possibleExistingConfig;
            try {
                possibleExistingConfig = serviceRegistry.getRealmInstance(CaptchaNode.Config.class, realm, nodeId);
            } catch (SSOException | SMSException e) {
                logger.error("An error occurred validating CAPTCHA Node configuration", e);
                throw new ServiceErrorException("Error validating CAPTCHA Node configuration");
            }
            String secretKey = getConfigValue(SECRET_KEY_METHOD_NAME, config -> config.secretKey().orElse(null),
                    possibleExistingConfig, attributes);
            String secretKeyPurpose = getConfigValue(
                    SECRET_KEY_PURPOSE_METHOD_NAME, config -> config.secretKeyPurpose().orElse(null),
                    possibleExistingConfig, attributes);

            if (isBlank(secretKey) && isBlank(secretKeyPurpose)) {
                throw new ServiceConfigException(
                        "Either CAPTCHA Secret Key or CAPTCHA Secret Label Identifier must be provided");
            }
        }

        private String getConfigValue(String attribute, Function<CaptchaNode.Config, ?> attributeMethod,
                Optional<CaptchaNode.Config> existingConfig, Map<String, Set<String>> attributes) {
            if (attributes.containsKey(attribute)) {
                return getFirstItem(attributes.get(attribute));
            }
            return existingConfig.map(attributeMethod).map(Object::toString).orElse(null);
        }
    }
}
