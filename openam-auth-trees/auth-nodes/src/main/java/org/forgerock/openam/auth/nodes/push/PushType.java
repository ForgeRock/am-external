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
 * Copyright 2022-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.push;

import org.forgerock.json.jose.builders.JwtClaimsSetBuilder;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.utils.StringUtils;

import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.NUMBERS_CHALLENGE_KEY;
import static org.forgerock.openam.auth.nodes.push.PushNodeConstants.PUSH_NUMBER_CHALLENGE_KEY;

/**
 * The supported Push types.
 */
public enum PushType {

    /**
     * Default. Push to accept notification.
     */
    DEFAULT("default") {
        @Override
        void updateClaims(JwtClaimsSetBuilder jwtClaimsSetBuilder) { }

        @Override
        void updateState(TreeContext context, Node node) { }
    },
    /**
     * Push to Challenge notification.
     */
    CHALLENGE("challenge") {
        final PushNumbersChallenge pushChallenge = new PushNumbersChallenge();

        @Override
        void updateClaims(JwtClaimsSetBuilder jwtClaimsSetBuilder) throws NodeProcessException {
            String challengeNumbers = pushChallenge.getNextChallenge();
            if (StringUtils.isNotEmpty(challengeNumbers)) {
                jwtClaimsSetBuilder.claim(NUMBERS_CHALLENGE_KEY, challengeNumbers);
            } else {
                throw new NodeProcessException("Error generating Push Numbers challenge.");
            }
        }

        @Override
        void updateState(TreeContext context, Node node) {
            context.getStateFor(node)
                    .putShared(PUSH_NUMBER_CHALLENGE_KEY, String.valueOf(pushChallenge.getAnswer()));
        }
    },
    /**
     * Push to Biometric notification.
     */
    BIOMETRIC("biometric") {
        @Override
        void updateClaims(JwtClaimsSetBuilder jwtClaimsSetBuilder) { }

        @Override
        void updateState(TreeContext context, Node node) { }
    };

    private String value;

    /**
     * The constructor.
     * @param value the value as a string.
     */
    PushType(String value) {
        this.value = value;
    }

    /**
     * Gets the push type value.
     * @return the value.
     */
    public String getValue() {
        return value;
    }

    /**
     * Update the JSON Web Token (JWT) claims set for the Push Type.
     *
     * @param jwtClaimsSetBuilder builder for constructing JWT claims sets.
     * @throws NodeProcessException if an error occur while making changes to the claims.
     */
    abstract void updateClaims(JwtClaimsSetBuilder jwtClaimsSetBuilder) throws NodeProcessException;

    /**
     * Update the state according to the Push Type.
     *
     * @param context the current {@link TreeContext} which will provide access to the SharedState.
     * @param node the {@link Node} requesting update state.
     * @throws NodeProcessException if an error occur while updating the state.
     */
    abstract void updateState(TreeContext context, Node node) throws NodeProcessException;

}
