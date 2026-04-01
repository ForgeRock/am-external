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
 * Copyright 2025-2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.radius;

import static java.util.Objects.requireNonNull;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.StaticOutcomeProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.radius.common.AttributeSet;
import org.forgerock.openam.radius.common.AttributeType;
import org.forgerock.openam.radius.common.packet.VendorSpecificAttribute;
import org.forgerock.openam.secrets.cache.SecretReferenceCache;
import org.forgerock.openam.sm.annotations.adapters.SecretPurpose;
import org.forgerock.openam.sm.validation.GreaterThanZeroValidator;
import org.forgerock.secrets.GenericSecret;
import org.forgerock.secrets.Purpose;
import org.forgerock.secrets.SecretReference;
import org.forgerock.util.i18n.PreferredLocales;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.authentication.modules.radius.RADIUSServer;
import com.sun.identity.authentication.modules.radius.client.ChallengeException;
import com.sun.identity.authentication.modules.radius.client.RadiusChallenge;
import com.sun.identity.authentication.modules.radius.client.RejectException;
import com.sun.identity.authentication.service.AMAccountLockoutTrees;
import com.sun.identity.idm.AMIdentity;

import jakarta.inject.Inject;

/**
 * A node that performs RADIUS authentication and handles challenge responses.
 */
@Node.Metadata(outcomeProvider = RadiusDecisionNode.RadiusOutcomeProvider.class,
        configClass = RadiusDecisionNode.Config.class,
        tags = {"org/forgerock/openam/auth/nodes/radius", "authentication"})
@Node.VersionMetadata(name = "RadiusDecisionNode", version = 1)
public class RadiusDecisionNode extends AbstractDecisionNode {

    private static final String BUNDLE = RadiusDecisionNode.class.getName();
    private static final String CHALLENGE_KEY = "radiusChallenge";
    private static final int DEFAULT_RADIUS_PORT = 1812;

    // Outcome IDs
    private static final String ACCEPT_ID = "acceptOutcome";
    private static final String REJECT_ID = "rejectOutcome";
    private static final String CHALLENGE_ID = "challengeOutcome";
    private static final String ERROR_ID = "errorOutcome";
    private static final String MISSING_USERNAME_OR_PASSWORD = "missingUsernameOrPassword";
    private static final String RADIUS_ATTRIBUTES = "radiusAttributes";

    private static final Logger logger = LoggerFactory.getLogger(RadiusDecisionNode.class);

    private final Config config;
    private final AMAccountLockoutTrees.Factory amAccountLockoutFactory;
    private final Realm realm;
    private final NodeUserIdentityProvider identityProvider;
    private final SecretReferenceCache secretReferenceCache;
    private final RadiusConnProvider radiusConnProvider;
    private final UUID nodeId;

    /**
     * Configuration for the RADIUS node.
     */
    public interface Config {
        /**
         * Primary RADIUS servers to connect to.
         * @return set of primary RADIUS server addresses.
         */
        @Attribute(order = 100)
        Set<String> primaryServers();

        /**
         * Secondary RADIUS servers for failover.
         * @return set of secondary RADIUS server addresses.
         */
        @Attribute(order = 200)
        Set<String> secondaryServers();

        /**
         * Purpose for retrieving the shared secret from the secret store.
         * @return the purpose for the shared secret.
         */
        @Attribute(order = 300)
        @SecretPurpose("am.authentication.nodes.radius.%s.secret")
        Optional<Purpose<GenericSecret>> sharedSecretPurpose();

        /**
         * Timeout in seconds for RADIUS server responses.
         * @return the timeout in seconds.
         */
        @Attribute(order = 400, validators = GreaterThanZeroValidator.class)
        default int timeout() {
            return 5;
        }

        /**
         * Health check interval in seconds for RADIUS servers.
         * @return the health check interval in seconds.
         */
        @Attribute(order = 500, validators = GreaterThanZeroValidator.class)
        default int healthCheckInterval() {
            return 5;
        }

        /**
         * Whether to check for account lockout before attempting RADIUS authentication.
         * @return true if account lockout should be checked, false otherwise.
         */
        @Attribute(order = 600)
        default boolean checkForLockout() {
            return false;
        }

        /**
         * Whether to require the RADIUS Message-Authenticator attribute in requests.
         * @return true if Message-Authenticator is required, false otherwise.
         */
        @Attribute(order = 700)
        default boolean requireMessageAuthenticator() {
            return true;
        }
    }

    /**
     * Create the node using Guice injection.
     * @param config The service config.
     * @param realm The realm
     * @param amAccountLockoutFactory Factory to create AMAccountLockoutTrees instances.
     * @param identityProvider The user identity provider.
     * @param secretReferenceCache The secret reference cache.
     * @param radiusConnProvider The RADIUS connection provider.
     * @param nodeId The node UUID.
     */
    @Inject
    public RadiusDecisionNode(@Assisted Config config,
                              @Assisted Realm realm,
                              AMAccountLockoutTrees.Factory amAccountLockoutFactory,
                              NodeUserIdentityProvider identityProvider,
                              SecretReferenceCache secretReferenceCache,
                              RadiusConnProvider radiusConnProvider, @Assisted UUID nodeId
    ) {
        this.amAccountLockoutFactory = amAccountLockoutFactory;
        this.config = config;
        this.realm = realm;
        this.identityProvider = identityProvider;
        this.secretReferenceCache = secretReferenceCache;
        this.radiusConnProvider = radiusConnProvider;
        this.nodeId = nodeId;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        NodeState nodeState = context.getStateFor(this);

        if (!nodeState.isDefined(USERNAME) || !nodeState.isDefined(PASSWORD)) {
            ResourceBundle bundle = context.request.locales.getBundleInPreferredLocale(BUNDLE,
                    getClass().getClassLoader());
            return Action.goTo(ERROR_ID).withErrorMessage(bundle.getString(MISSING_USERNAME_OR_PASSWORD)).build();
        }

        String username = requireNonNull(nodeState.get(USERNAME)).asString();
        String password = requireNonNull(nodeState.get(PASSWORD)).asString();
        RadiusNodeConn conn = null;

        try {
            if (config.checkForLockout() && isAccountLocked(username, context)) {
                logger.warn("User account is locked: {}", username);
                return Action.goTo(ERROR_ID).build();
            }
            conn = getRadiusConnection();
            // Check if we have a challenge response from the collector node
            JsonValue challengeResponseJson = nodeState.get(CHALLENGE_KEY);
            if (challengeResponseJson != null && !challengeResponseJson.isNull()) {
                RadiusChallenge challengeWithResponse = new RadiusChallenge(challengeResponseJson);
                conn.replyChallenge(username, challengeWithResponse.getResponse(), challengeWithResponse);
            } else {
                AttributeSet radiusAttributes = conn.authenticate(username, password);
                addVendorSpecificAttributesToNodeState(radiusAttributes, nodeState);
            }
            return Action.goTo(ACCEPT_ID).build();
        } catch (RejectException e) {
            logger.debug("RADIUS authentication rejected for user: {}", username);
            return Action.goTo(REJECT_ID).build();
        } catch (ChallengeException e) {
            logger.debug("RADIUS challenge received for user: {}", username);
            RadiusChallenge challenge = new RadiusChallenge(e.getStateAttribute(), e.getReplyMessage(), null);
            nodeState.putTransient(CHALLENGE_KEY, challenge.toJsonValue());
            return Action.goTo(CHALLENGE_ID).build();
        } catch (IOException | NoSuchAlgorithmException | NodeProcessException e) {
            logger.error("Error connecting to RADIUS server", e);
            return Action.goTo(ERROR_ID).withErrorMessage(e.getMessage()).build();
        } finally {
            shutdown(conn);
        }
    }

    private boolean isAccountLocked(String username, TreeContext context) {
        AMAccountLockoutTrees accountLockout = amAccountLockoutFactory.create(this.realm);
        if (!accountLockout.isLockoutEnabled()) {
            return false;
        }

        Optional<String> universalId = identityProvider.getUniversalId(username, realm);
        Optional<AMIdentity> userIdentity = identityProvider.getAMIdentity(universalId,
                context.getStateFor(this));
        if (userIdentity.isEmpty()) {
            logger.warn("Could not find user identity for username: {}", username);
            return true;
        }
        logger.debug("Checking user account status");
        return accountLockout.isAccountLocked(userIdentity.get());
    }

    /**
     * Extracts vendor-specific attributes from the RADIUS response and adds them to the node state.
     *
     * @param radiusAttributes the attributes returned from the RADIUS server
     * @param nodeState the node state to add the vendor-specific attributes to
     */
    private void addVendorSpecificAttributesToNodeState(AttributeSet radiusAttributes, NodeState nodeState) {
        if (radiusAttributes == null) {
            return;
        }

        List<Map<String, Object>> vsaList = radiusAttributes.stream()
                .filter(a -> a.getType() == AttributeType.VENDOR_SPECIFIC)
                .map(a -> (VendorSpecificAttribute) a)
                .map(vsa -> Map.<String, Object>of(
                        "vendorId", vsa.getId(),
                        "bytes", vsa.getOctets()
                ))
                .collect(Collectors.toList());

        if (!vsaList.isEmpty()) {
            nodeState.putShared(RADIUS_ATTRIBUTES, new JsonValue(vsaList));
        }
    }

    private RadiusNodeConn getRadiusConnection() throws IOException, NodeProcessException {
        Set<RADIUSServer> primaryServers = parseServers(config.primaryServers());
        Set<RADIUSServer> secondaryServers = parseServers(config.secondaryServers());
        // Don't need health check interval here as RadiusMonitorPool handles that
        String sharedSecret = getSharedSecret()
                .orElseThrow(() -> new NodeProcessException("Secret key could not be retrieved"));
        return radiusConnProvider.createConnection(primaryServers, secondaryServers, sharedSecret,
                config.timeout(), config.requireMessageAuthenticator(), nodeId);
    }

    private Optional<String> getSharedSecret() {
        Optional<Purpose<GenericSecret>> purposeOpt = config.sharedSecretPurpose();
        try {
            return purposeOpt.map(p -> secretReferenceCache.realm(realm).active(p))
                    .map(SecretReference::getAsync)
                    .map(promise -> promise.then(s -> s.revealAsUtf8(String::new)))
                    .map(promise -> promise.thenCatch(ex -> {
                        logger.debug("Failed to get secret from store ", ex);
                        return null;
                    }))
                    .map(Promise::getOrThrowIfInterrupted);
        } catch (IllegalStateException e) {
            logger.debug("Failed to resolve shared secret for realm {}", realm.asPath(), e);
            return Optional.empty();
        }
    }

    private Set<RADIUSServer> parseServers(Set<String> servers) {
        if (servers == null) {
            return Collections.emptySet();
        }
        return servers.stream().map(s -> {
            String[] parts = s.split(":");
            if (parts.length == 2) {
                return new RADIUSServer(parts[0], Integer.parseInt(parts[1]));
            } else {
                return new RADIUSServer(parts[0], DEFAULT_RADIUS_PORT);
            }
        }).collect(Collectors.toSet());
    }

    /**
     * Defines the possible outcomes from the RADIUS node.
     */
    public static final class RadiusOutcomeProvider implements StaticOutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE, getClass().getClassLoader());
            return ImmutableList.of(
                    new Outcome(ACCEPT_ID, bundle.getString(ACCEPT_ID)),
                    new Outcome(REJECT_ID, bundle.getString(REJECT_ID)),
                    new Outcome(CHALLENGE_ID, bundle.getString(CHALLENGE_ID)),
                    new Outcome(ERROR_ID, bundle.getString(ERROR_ID)));
        }
    }

    private void shutdown(RadiusNodeConn conn) {
        if (conn == null) {
            return;
        }
        try {
            conn.disconnect();
        } catch (IOException ignored) {
        }
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[]{
                new InputState(USERNAME, true),
                new InputState(PASSWORD, true),
                new InputState(CHALLENGE_KEY, false)
        };
    }

    @Override
    public OutputState[] getOutputs() {
        return new OutputState[]{
                new OutputState(CHALLENGE_KEY),
                new OutputState(RADIUS_ATTRIBUTES)
        };
    }

}
