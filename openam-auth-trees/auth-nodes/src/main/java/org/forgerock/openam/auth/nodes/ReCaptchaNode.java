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
 * Copyright 2019-2020 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.oauth.clients.oauth2.OAuth2Client.HTTP_POST;
import static org.forgerock.openam.auth.node.api.Action.send;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonException;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Namespace;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.authentication.callbacks.ReCaptchaCallback;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A node that displays a CAPTCHA to a user and verifies their response, the default values are for Google
 * ReCAPTCHA but other CAPTCHA providers can be used.
 * <p>
 *     For hCAPTCHA use the following configuration values:
 *     <ul>
 *       <li> CAPTCHA Site Key: &lt;your site key&gt; </li>
 *       <li> CAPTCHA Secret Key: &lt;your secret key&gt; </li>
 *       <li> CAPTCHA Verification URL: https://hcaptcha.com/siteverify </li>
 *       <li> CAPTCHA API URL: https://hcaptcha.com/1/api.js </li>
 *       <li> Class of CAPTCHA &lt;div&gt; : h-captcha </li>
 *     </ul>
 * </p>
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = ReCaptchaNode.Config.class,
        tags = {"risk"},
        namespace = Namespace.PRODUCT)
public class ReCaptchaNode extends SingleOutcomeNode {
    @VisibleForTesting
    static final String SUCCESS = "success";
    private static final String BUNDLE = "org/forgerock/openam/auth/nodes/ReCaptchaNode";
    private final Logger logger = LoggerFactory.getLogger(ReCaptchaNode.class);

    /**
     * Configuration for the node.
     */
    public interface Config {
        /**
         * The reCAPTCHA site key.
         * @return the key.
         */
        @Attribute(order = 100, validators = { RequiredValueValidator.class })
        String siteKey();

        /**
         * The reCAPTCHA secret key.
         * @return the key.
         */
        @Attribute(order = 200, validators = { RequiredValueValidator.class })
        String secretKey();

        /**
         * The uri to verify the reCAPTCHA.
         * @return the verification URI.
         */
        @Attribute(order = 300, validators = { RequiredValueValidator.class })
        default String reCaptchaUri() {
            return "https://www.google.com/recaptcha/api/siteverify";
        }

        /**
         * The uri of the captcha API.
         * @return the API uri
         */
        @Attribute(order = 400, validators = { RequiredValueValidator.class })
        default String apiUri() {
            return "https://www.google.com/recaptcha/api.js";
        }

        /**
         * The class of the captcha div that appears on the login screen.
         * @return the class of the captcha div that appears on the login screen
         */
        @Attribute(order = 500, validators = { RequiredValueValidator.class })
        default String divClass() {
            return "g-recaptcha";
        }
    }

    private final Config config;
    private final Handler handler;

    /**
     * Guice constructor.
     * @param config The node configuration.
     * @param handler the http handler.
     * @throws NodeProcessException If there is an error reading the configuration.
     */
    @Inject
    public ReCaptchaNode(@Assisted Config config, @Named("HttpClientHandler") Handler handler)
            throws NodeProcessException {
        this.config = config;
        this.handler = handler;

    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("ReCaptchaNode started");
        Optional<ReCaptchaCallback> callback = context.getCallback(ReCaptchaCallback.class);

        if (!callback.isPresent()) {
            logger.debug("no callback present, creating new ReCaptchaCallback");
            return send(new ReCaptchaCallback(config.siteKey(), config.apiUri(), config.divClass())).build();
        }

        if (callback.get().getResponse() == null || callback.get().getResponse().isEmpty()) {
            logger.debug("no response specified in callback");
            throw new NodeProcessException("reCAPTCHA response necessary for verification");
        }

        verifyRecaptchaResponse(callback.get().getResponse());

        logger.debug("reCAPTCHA successfully verified");
        return goToNext().build();
    }

    private void verifyRecaptchaResponse(String response) throws NodeProcessException {
        URI uri = URI.create(config.reCaptchaUri()
                + "?secret=" + config.secretKey()
                + "&response=" + response);
        try {
            Response verificationResponse = handler.handle(new RootContext(),
                    new Request().setUri(uri).setMethod(HTTP_POST))
                    .getOrThrow();

            if (!json(verificationResponse.getEntity().getJson()).get(SUCCESS).asBoolean()) {
                logger.debug("verification of reCAPTCHA response was not successful");
                throw new NodeProcessException("reCAPTCHA verification was not successful");
            }
        } catch (InterruptedException e) {
            logger.debug("Unable to verify reCAPTCHA response", e);
            throw new NodeProcessException("Unable to verify reCAPTCHA response");
        } catch (IOException e) {
            logger.debug("Unable to retrieve state from token response");
            throw new JsonException("Unable to retrieve state from token response");
        }
    }
}