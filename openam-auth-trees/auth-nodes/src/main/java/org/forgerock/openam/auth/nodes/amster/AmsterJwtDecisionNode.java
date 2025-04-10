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
 * Copyright 2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.amster;

import static com.sun.identity.idm.IdType.USER;
import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.exceptions.JwtReconstructionException;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.sm.annotations.TagSwappableDefault;
import org.forgerock.openam.sm.annotations.adapters.Placeholder;
import org.forgerock.util.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;

/**
 * The authentication node for authenticating Amster connections.
 * <p>
 *     This auth node supports authentication using established SSH keys, which involves signing a JWT that
 *     contains subject and expiration claims. The subject claim is interpreted as the username of the principal. This
 *     works as follows:
 * </p>
 * <ul>
 *     <li>
 *         The Amster client signs the JWT using a local private key, and the server verifies the signature using the
 *         list of public keys in the {@code $BASE_DIR/authorized_keys} (or other configured location) file for the OS
 *         user that is running OpenAM, by finding a key that matches the JWT's {@code kid} claim. If the entry in the
 *         authorized keys file contains a {@code from} parameter, then only connections that originate from a
 *         qualifying host/address will be permitted.
 *     </li>
 * </ul>
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = AmsterJwtDecisionNode.Config.class,
        tags = {"utilities"})
public class AmsterJwtDecisionNode extends AbstractDecisionNode {

    /**
     * Configuration for the Amster Jwt decision node.
     */
    public interface Config {

        /**
         * The authorized keys.
         * <p>The location of the authorized_keys file (which has the same format as an OpenSSH \
         *   authorized_keys file) to use to validate remote Amster connections.
         *   </p>
         * @return The path to the authorized keys.
         */
        @Attribute(order = 1)
        @Placeholder(value = "&{amster.secrets.keys.path|%}")
        @TagSwappableDefault("@BASE_DIR@/security/keys/amster/authorized_keys")
        String authorizedKeys();
    }

    private static final String NONCE_STATE_KEY = "amster.nonce";
    private static final String JWT_NONCE_CLAIM = "nonce";
    private final Logger logger = LoggerFactory.getLogger(AmsterJwtDecisionNode.class);
    private final AmsterJwtDecisionNode.Config config;
    private final AuthorizedKeys authorizedKeys;

    /**
     * Constructs an AmsterJwtDecisionNode.
     *
     * @param config The node configuration.
     */
    @Inject
    public AmsterJwtDecisionNode(@Assisted AmsterJwtDecisionNode.Config config) {
        this(config, new AuthorizedKeys());
    }

    @VisibleForTesting
    AmsterJwtDecisionNode(AmsterJwtDecisionNode.Config config, AuthorizedKeys authorizedKeys) {
        this.config = config;
        this.authorizedKeys = authorizedKeys;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {

        Optional<HiddenValueCallback> callback = context.getCallback(HiddenValueCallback.class);

        NodeState nodeState = context.getStateFor(this);

        Optional<String> nonce = Optional.ofNullable(nodeState.get(NONCE_STATE_KEY))
                .map(JsonValue::asString);

        if (nonce.isPresent()) {
            try {
                if (callback.isPresent()) {
                    String jwtString = callback.get().getValue();
                    SignedJwt jwt = new JwtReconstruction().reconstructJwt(jwtString, SignedJwt.class);
                    if (nonce.get().equals(jwt.getClaimsSet().get(JWT_NONCE_CLAIM).asString())) {
                        for (Key key : loadAuthorisedKeys()) {
                            if (key.isValid(jwt, context.request.servletRequest)) {
                                String subject = jwt.getClaimsSet().getSubject();
                                nodeState.putShared(USERNAME, subject);
                                return goTo(true)
                                        .withIdentifiedIdentity(subject, USER)
                                        .build();
                            }
                        }
                    }
                }
            } catch (JwtReconstructionException e) {
                logger.debug("Failed to reconstruct JWT", e);
                nodeState.remove(NONCE_STATE_KEY);
                return goTo(false)
                        .build();
            }
        } else {
            // Send initial nonce
            String newNonce = UUID.randomUUID().toString();
            nodeState.putTransient(NONCE_STATE_KEY, newNonce);
            return send(new HiddenValueCallback("jwt", newNonce))
                    .build();
        }

        return goTo(false)
                .build();
    }

    private Set<Key> loadAuthorisedKeys() {
        try (InputStream stream = new BufferedInputStream(new FileInputStream(config.authorizedKeys()))) {
            return authorizedKeys.read(stream);
        } catch (IOException e) {
            logger.error("AmsterJwtDecisionNode: Could not read authorized keys file {}", config.authorizedKeys(), e);
        }

        return Set.of();
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[] {
                new InputState(NONCE_STATE_KEY, false)
        };
    }
}
