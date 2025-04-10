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
 * Copyright 2023-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.oidc;

import static java.util.Collections.singletonList;
import static org.forgerock.openam.auth.nodes.helpers.ScriptedNodeHelper.WILDCARD;
import static org.forgerock.openam.social.idp.SocialIdPScriptContext.SOCIAL_IDP_PROFILE_TRANSFORMATION;
import static org.forgerock.openam.social.idp.SocialIdPScriptContext.SOCIAL_IDP_PROFILE_TRANSFORMATION_NAME;
import static org.forgerock.openam.utils.StringUtils.isBlank;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.AuthScriptUtilities;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.script.OidcNodeBindings;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.scripting.application.ScriptEvaluator;
import org.forgerock.openam.scripting.application.ScriptEvaluator.ScriptResult;
import org.forgerock.openam.scripting.application.ScriptEvaluatorFactory;
import org.forgerock.openam.scripting.domain.LegacyScriptBindings;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.forgerock.openam.scripting.persistence.config.consumer.ScriptContext;
import org.forgerock.openam.secrets.cache.SecretReferenceCache;
import org.forgerock.openam.sm.annotations.adapters.ExampleValue;
import org.forgerock.openam.sm.annotations.adapters.SecretPurpose;
import org.forgerock.secrets.GenericSecret;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.SecretReference;
import org.mozilla.javascript.NativeJavaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.sm.RequiredValueValidator;


/**
 * A node that evaluates the oidc_token from a request header and decides if the user exists in the LDAP database.
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = OidcNode.Config.class,
        tags = {"social", "federation"})
public class OidcNode extends AbstractDecisionNode {
    /**
     * The lookup attributes.
     */
    public static final String LOOKUP_ATTRIBUTES = "lookupAttributes";
    private static final Logger logger = LoggerFactory.getLogger(OidcNode.class);
    private final OidcIdTokenJwtHandler jwtHandler;
    private final OidcNode.Config config;
    private final Realm realm;
    private final ScriptEvaluator<LegacyScriptBindings> scriptEvaluator;
    private final AuthScriptUtilities authScriptUtils;


    /**
     * OidcNode constructor.
     *
     * @param config                       Config for the node
     * @param realm                        Realm for the node
     * @param oidcIdTokenJwtHandlerFactory Factory for producing OidcIdTokenJwtHandler
     * @param scriptEvaluatorFactory       Factory for the scriptEvaluator
     * @param secretReferenceCache         The secret reference cache
     * @param authScriptUtils              Utilities for scripted nodes
     * @throws NodeProcessException when an error occurs while processing the node
     */
    @Inject
    public OidcNode(@Assisted Config config,
            @Assisted Realm realm,
            OidcIdTokenJwtHandlerFactory oidcIdTokenJwtHandlerFactory,
            ScriptEvaluatorFactory scriptEvaluatorFactory,
            SecretReferenceCache secretReferenceCache, AuthScriptUtilities authScriptUtils)
            throws NodeProcessException {
        this.config = config;
        this.realm = realm;
        // Set up the needed configuration for the client secret validation method
        SecretReference<GenericSecret> secret = null;
        if (config.oidcValidationType().equals(OpenIdValidationType.CLIENT_SECRET)) {
            secret = secretReferenceCache.realm(realm).active(this.config.secretId()
                    .orElseThrow(() -> new NodeProcessException("Client Secret Id is empty in node configuration")));
        }
        this.jwtHandler = oidcIdTokenJwtHandlerFactory.createOidcIdTokenJwtHandler(config, Optional.ofNullable(secret));
        this.scriptEvaluator = scriptEvaluatorFactory.create(SOCIAL_IDP_PROFILE_TRANSFORMATION);
        this.authScriptUtils = authScriptUtils;
    }

    /**
     * A method to get the outcome of the node.
     *
     * @param context The context of the tree authentication
     * @return outcome of the node
     * @throws NodeProcessException when an error occurs while processing the node
     */
    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("OpenId Connect ID Token Bearer node started");
        final HttpServletRequest request = context.request.servletRequest;
        final String jwtValue = request.getHeader(config.headerName());
        if (isBlank(jwtValue)) {
            logger.warn("No OpenIdConnect ID Token referenced by header value: {}", config.headerName());
            return goTo(false).build();
        }

        logger.debug("OpenId Connect ID Token Bearer: process provided idtoken: {} ", jwtValue);

        try {
            SignedJwt jwt = jwtHandler.createJwtFromString(jwtValue);
            JwtClaimsSet jwtClaims = jwt.getClaimsSet();
            if (!jwtHandler.isJwtValid(jwt)) {
                logger.debug("Could not validate the Id Token: {}", jwtValue);
                return goTo(false).build();
            }

            JsonValue objectData = evaluateScript(context, config.script(), jwtClaims.toJsonValue());

            logger.info("JWT claims successfully transformed into object data: {}", objectData);

            NodeState nodeState = context.getStateFor(this);
            nodeState.putShared(LOOKUP_ATTRIBUTES, objectData);

            if (jwtClaims.isDefined(OAuth2Constants.ProofOfPossession.CNF)) {
                nodeState.putShared("org.forgerock.openam.authentication.modules.jwtpop.cnf",
                        jwtClaims.get(OAuth2Constants.ProofOfPossession.CNF));
            }

            return goTo(true).build();

        } catch (AuthLoginException e) {
            logger.error(e.getMessage(), e);
            return goTo(false).build();
        }
    }

    private JsonValue evaluateScript(TreeContext context, Script script,
            JsonValue inputData) throws NodeProcessException {
        try {

            OidcNodeBindings scriptBindings = OidcNodeBindings.builder()
                    .withJwtClaims(inputData)
                    .withNodeState(context.getStateFor(this))
                    .withHeaders(authScriptUtils.convertHeadersToModifiableObjects(context.request.headers))
                    .withExistingSession(authScriptUtils.getSessionProperties(context.request.ssoTokenId))
                    .build();

            ScriptResult<Object> scriptResult = scriptEvaluator.evaluateScript(script, scriptBindings, realm);
            logger.debug("script {} \n binding {}", script, scriptResult.getBindings());

            return (JsonValue) (script.getLanguage().equals(ScriptingLanguage.JAVASCRIPT)
                    ? ((NativeJavaObject) scriptResult.getScriptReturnValue()).unwrap()
                    : scriptResult.getScriptReturnValue());
        } catch (javax.script.ScriptException e) {
            logger.warn("Error evaluating the script", e);
            throw new NodeProcessException(e);
        }
    }

    @Override
    public InputState[] getInputs() {
        return config.inputs().stream()
                .map(input -> new InputState(input, true))
                .toArray(InputState[]::new);
    }

    /**
     * Which way will the Open ID Connect node validate the ID token from the OpenID Connect provider.
     */
    public enum OpenIdValidationType {
        /**
         * Retrieve the provider keys based on the information provided in the OpenID Connect Provider Configuration
         * Document.
         */
        WELL_KNOWN_URL,
        /**
         * Use the client secret that you specify in the Client Secret property as the key to validate the ID token
         * signature according to the HMAC, using the client secret to the decrypt the hash and then checking that
         * the hash matches the hash of the ID token JWT.
         */
        CLIENT_SECRET,
        /**
         * Retrieve the provider's JSON web key set at the URL that you specify in the OpenID Connect validation
         * configuration value property.
         */
        JWK_URL
    }

    /**
     * The interface Config.
     */
    public interface Config {

        /**
         * The OpenID Connect validation type.
         *
         * @return The OpenID Connect validation type.
         */
        @Attribute(order = 100, requiredValue = true)
        default OpenIdValidationType oidcValidationType() {
            return OpenIdValidationType.WELL_KNOWN_URL;
        }

        /**
         * The OpenID Connect validation value.
         *
         * @return The OpenID Connect validation value.
         */
        @Attribute(order = 200, requiredValue = true)
        @ExampleValue("https://accounts.google.com/.well-known/openid-configuration")
        String oidcValidationValue();

        /**
         * The client secret.
         *
         * @return The client secret id for the Secrets API
         */
        @Attribute(order = 300)
        @ExampleValue("clientsecret")
        @SecretPurpose("%s")
        Optional<Purpose<GenericSecret>> secretId();

        /**
         * The name of header referencing the ID token.
         *
         * @return The name of header referencing the ID token.
         */
        @Attribute(order = 400, requiredValue = true)
        default String headerName() {
            return "oidc_id_token";
        }

        /**
         * The name of the OpenID Connect ID token issuer.
         *
         * @return The name of the OpenID Connect ID token issuer.
         */
        @Attribute(order = 500, requiredValue = true)
        @ExampleValue("https://accounts.google.com")
        String idTokenIssuer();

        /**
         * The audience name for this OpenID Connect node.
         *
         * @return The audience name for this OpenID Connect node.
         */
        @Attribute(order = 600, requiredValue = true)
        String audienceName();

        /**
         * A list of accepted authorized parties.
         *
         * @return A list of accepted authorized parties.
         */
        @Attribute(order = 700, requiredValue = true)
        Set<String> authorisedParties();

        /**
         * The script configuration for transforming attributes from the ID token.
         *
         * @return The script configuration
         */
        @Attribute(order = 800, validators = {RequiredValueValidator.class})
        @ScriptContext(legacyContext = SOCIAL_IDP_PROFILE_TRANSFORMATION_NAME)
        Script script();

        /**
         * List of required inputs.
         *
         * @return The list of state inputs required by the script.
         */
        @Attribute(order = 900)
        default List<String> inputs() {
            return singletonList(WILDCARD);
        }

        /**
         * The unreasonable lifetime limit of an ID token in minutes.
         *
         * @return The unreasonable lifetime limit.
         */
        @Attribute(order = 1000, requiredValue = true)
        default int unreasonableLifetimeLimit() {
            return 60;
        }
    }
}
