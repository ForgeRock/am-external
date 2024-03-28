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
 * Copyright 2017-2024 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static com.sun.identity.idm.IdType.USER;
import static java.util.Collections.singletonMap;
import static java.util.concurrent.TimeUnit.HOURS;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;

import javax.inject.Inject;

import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.jwt.InvalidPersistentJwtException;
import org.forgerock.openam.auth.nodes.jwt.JwtHeaderUtilities;
import org.forgerock.openam.auth.nodes.jwt.PersistentJwtClaimsHandler;
import org.forgerock.openam.auth.nodes.jwt.PersistentJwtProvider;
import org.forgerock.openam.auth.nodes.treehook.UpdatePersistentCookieTreeHook;
import org.forgerock.openam.auth.nodes.validators.HmacSigningKeyValidator;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.secrets.cache.SecretCache;
import org.forgerock.openam.secrets.cache.SecretReferenceCache;
import org.forgerock.openam.sm.annotations.adapters.Password;
import org.forgerock.openam.sm.annotations.adapters.SecretPurpose;
import org.forgerock.openam.sm.annotations.adapters.TimeUnit;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.ValidSecretsReference;
import org.forgerock.secrets.keys.SigningKey;
import org.forgerock.secrets.keys.VerificationKey;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

/**
 * A node that checks to see if there is a valid persistent cookie in the request.
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = PersistentCookieDecisionNode.Config.class,
        tags = {"contextual"})
public class PersistentCookieDecisionNode extends AbstractDecisionNode implements PersistentCookieNodeConfig {
    private static final Logger logger = LoggerFactory.getLogger(PersistentCookieDecisionNode.class);
    private static final String BUNDLE = PersistentCookieDecisionNode.class.getName();
    private static final String DEFAULT_COOKIE_NAME = "session-jwt";
    private static final Duration JWT_IDLE_TIMEOUT_IN_HOURS = Duration.duration(5, HOURS);
    private final PersistentJwtProvider persistentJwtProvider;
    private final PersistentJwtClaimsHandler persistentJwtClaimsHandler;
    private final UUID nodeId;
    private final Config config;
    private final CoreWrapper coreWrapper;
    private final LegacyIdentityService identityService;
    private final SecretCache secretCache;

    /**
     * Configuration for the node.
     */
    public interface Config extends HmacSigningKeyConfig {

        /**
         * The idle time out. If the cookie is not used within this time, the jwt becomes invalid.
         *
         * @return the idle time out in hours.
         */
        @Attribute(order = 100)
        @TimeUnit(HOURS)
        default Duration idleTimeout() {
            return JWT_IDLE_TIMEOUT_IN_HOURS;
        }

        /**
         * If true, the claim ip address must match the client request ip address.
         *
         * @return true to enfore the client ip.
         */
        @Attribute(order = 200)
        default boolean enforceClientIp() {
            return false;
        }

        /**
         * If true, instructs the browser to only send the cookie on secure connections.
         *
         * @return true to use secure cookie.
         */
        @Attribute(order = 300)
        default boolean useSecureCookie() {
            return true;
        }

        /**
         * If true, instructs the browser to prevent access to this cookie and only use it for http.
         *
         * @return true to use http only cookie.
         */
        @Attribute(order = 400)
        default boolean useHttpOnlyCookie() {
            return true;
        }

        /**
         * The signing key.
         *
         * @deprecated The secrets API enabled 'signingKeyPurpose' should be used instead.
         * @return the hmac signing key.
         */
        @Attribute(order = 500, validators = {HmacSigningKeyValidator.class})
        @Password
        @Deprecated
        @Override
        Optional<char[]> hmacSigningKey();


        @Attribute(order = 550, resourceName = "hmacSigningKeySecretLabelIdentifier")
        @SecretPurpose("am.authentication.nodes.persistentcookie.%s.signing")
        @Override
        Optional<Purpose<SigningKey>> signingKeyPurpose();


        /**
         * The name of the persistent cookie.
         *
         * @return the name of the persistent cookie.
         */
        @Attribute(order = 600)
        default String persistentCookieName() {
            return DEFAULT_COOKIE_NAME;
        }
    }

    /**
     * Creates a PersistentCookieDecisionNode.
     *
     * @param config                       the config
     * @param coreWrapper                  the core wrapper
     * @param identityService              an instance of the IdentityService.
     * @param nodeId                       the id of this node instance.
     * @param realm                        the realm.
     * @param persistentJwtProvider        the persistent JWT provider instance
     * @param persistentJwtClaimsHandler   the persistence JWT claims  handler
     * @param secretReferenceCache         the secret reference cache
     */
    @Inject
    public PersistentCookieDecisionNode(@Assisted Config config, CoreWrapper coreWrapper,
            LegacyIdentityService identityService, @Assisted UUID nodeId, @Assisted Realm realm,
            PersistentJwtProvider persistentJwtProvider, PersistentJwtClaimsHandler persistentJwtClaimsHandler,
            SecretReferenceCache secretReferenceCache) {
        this.config = config;
        this.coreWrapper = coreWrapper;
        this.identityService = identityService;
        this.persistentJwtProvider = persistentJwtProvider;
        this.nodeId = nodeId;
        this.persistentJwtClaimsHandler = persistentJwtClaimsHandler;
        this.secretCache = secretReferenceCache.realm(realm);
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("PersistentCookieDecisionNode started");
        ResourceBundle bundle = context.request.locales.getBundleInPreferredLocale(BUNDLE, getClass().getClassLoader());
        Action.ActionBuilder actionBuilder = goTo(true);
        String orgName = getRequestOrg(context);

        String jwtCookie = getJwtCookie(context);
        if (jwtCookie == null) {
            logger.debug("jwtCookie is null");
            return goTo(false).build();
        }

        Optional<String> kid = JwtHeaderUtilities.getHeader("kid", jwtCookie);
        Jwt jwt = getValidDecryptedJwt(jwtCookie, orgName,
                config.verificationKeysReference(kid.orElse(null), secretCache));

        if (jwt == null) {
            logger.error("jwt reconstruction error");
            actionBuilder = goTo(false);
            return actionBuilder.build();
        }
        try {
            var claimsSetContext = persistentJwtClaimsHandler.getClaimsSetContext(jwt, bundle);
            String clientIp = context.request.clientIp;
            logger.debug("clientIp {}", clientIp);
            persistentJwtClaimsHandler.validateClaims(claimsSetContext, bundle, orgName, clientIp,
                    config.enforceClientIp());
            String userName = persistentJwtClaimsHandler.getUsername(claimsSetContext, bundle);
            logger.debug("userName {}", userName);
            String realm = context.sharedState.get(REALM).asString();
            actionBuilder = actionBuilder
                    .replaceSharedState(context.sharedState.copy().put(USERNAME, userName))
                    .withUniversalId(identityService.getUniversalId(userName, realm, USER))
                    .withIdentifiedIdentity(userName, USER)
                    .putSessionProperty(generateSessionPropertyName(config.persistentCookieName()),
                            config.persistentCookieName())
                    .addSessionHook(UpdatePersistentCookieTreeHook.class, nodeId, getClass().getSimpleName());
        } catch (InvalidPersistentJwtException e) {
            logger.error(e.getLocalizedMessage());
            actionBuilder = goTo(false);
        }

        return actionBuilder.build();
    }


    private Jwt getValidDecryptedJwt(String jwtCookie, String orgName,
            ValidSecretsReference<VerificationKey, NeverThrowsException> verificationKeysReference) {
        try {
            return persistentJwtProvider.getValidDecryptedJwt(jwtCookie, orgName, verificationKeysReference);
        } catch (InvalidPersistentJwtException e) {
            logger.warn("Attempt to verify JWT failed, attempting other valid keys");
            return null;
        }
    }

    private String getJwtCookie(TreeContext context) {
        String jwtCookie = null;
        String cookieName = config.persistentCookieName();
        if (context.request.cookies.containsKey(cookieName)) {
            jwtCookie = context.request.cookies.get(cookieName);
        }
        return jwtCookie;
    }

    private String getRequestOrg(TreeContext context) {
        String requestRealm = context.sharedState.get(REALM).asString();
        return coreWrapper.convertRealmPathToRealmDn(requestRealm).toLowerCase();
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[]{
            new InputState(REALM)
        };
    }

    @Override
    public OutputState[] getOutputs() {
        return new OutputState[]{
            new OutputState(USERNAME, singletonMap(TRUE_OUTCOME_ID, true))
        };
    }

    @Override
    public String getPersistentCookieName() {
        return config.persistentCookieName();
    }
}
