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
 * Copyright 2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.radius;

import java.util.Optional;

import javax.security.auth.callback.PasswordCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeState;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.authentication.modules.radius.client.RadiusChallenge;

/**
 * A node that collects the user's response to a RADIUS challenge.
 */
@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
        configClass = RadiusChallengeCollectorNode.Config.class,
        tags = {"iam", "org/forgerock/openam/auth/nodes/radius", "authentication"})
@Node.VersionMetadata(name = "RadiusChallengeCollectorNode", version = 1)
public class RadiusChallengeCollectorNode extends SingleOutcomeNode {

    private static final String CHALLENGE_KEY = "radiusChallenge";

    private final Logger logger = LoggerFactory.getLogger(RadiusChallengeCollectorNode.class);

    /**
     * Configuration for the RadiusChallengeCollectorNode.
     */
    public interface Config {
        // No configuration required for this node.
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        NodeState nodeState = context.getStateFor(this);
        JsonValue challengeObj = nodeState.get(CHALLENGE_KEY);
        if (challengeObj == null) {
            throw new NodeProcessException("RADIUS challenge not found in state");
        }
        RadiusChallenge challenge = new RadiusChallenge(challengeObj);

        Optional<PasswordCallback> challengeCallback = context.getCallback(PasswordCallback.class);
        if (challengeCallback.isEmpty() || StringUtils.isEmpty(new String(challengeCallback.get().getPassword()))) {
            return collectChallengeResponse(challenge);
        }

        char[] passwordChars = challengeCallback.get().getPassword();
        String challengeResponse = new String(passwordChars);
        RadiusChallenge challengeWithResponse =
                new RadiusChallenge(challenge.getStateAttribute(), challenge.getReplyMessage(), challengeResponse);
        nodeState.putTransient(CHALLENGE_KEY, challengeWithResponse.toJsonValue());
        return goToNext().build();
    }

    private Action collectChallengeResponse(RadiusChallenge challenge) {
        logger.debug("collecting RADIUS challenge response");
        return Action.send(new PasswordCallback(challenge.getReplyMessage(), false)).build();
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[]{
                new InputState(CHALLENGE_KEY, true)
        };
    }

    @Override
    public OutputState[] getOutputs() {
        return new OutputState[]{
                new OutputState(CHALLENGE_KEY)
        };
    }
}
