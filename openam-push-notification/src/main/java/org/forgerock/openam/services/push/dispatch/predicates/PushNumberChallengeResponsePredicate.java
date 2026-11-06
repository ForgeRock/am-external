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
 * Copyright 2019-2026 ForgeRock AS.
 */
package org.forgerock.openam.services.push.dispatch.predicates;

import static org.forgerock.openam.services.push.PushNotificationConstants.JWT_CHALLENGE_RESPONSE_KEY;

import java.security.MessageDigest;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.util.Reject;

/**
 * A predicate which verifies a number challenge response embedded within a JWT.
 * The JWT is extracted from the push message content at a given location, and the
 * {@code challengeResponse} claim within it is compared to the expected answer.
 */
public class PushNumberChallengeResponsePredicate extends AbstractPredicate {

    private String answer;
    private String location;

    /**
     * Constructs a new PushNumberChallengeResponsePredicate.
     *
     * @param answer the expected challenge answer
     * @param location the JSON pointer path within the push message content where the JWT is found
     */
    public PushNumberChallengeResponsePredicate(String answer, String location) {
        Reject.ifNull(answer, location);
        this.location = location;
        this.answer = answer;
    }

    /**
     * No-arg constructor required for CTS deserialisation.
     */
    public PushNumberChallengeResponsePredicate() {
        // This section intentionally left blank.
    }

    @Override
    public boolean perform(JsonValue content) {
        Jwt signedJwt = new JwtReconstruction().reconstructJwt(
                content.get(new JsonPointer(location)).asString(), Jwt.class);
        String response = (String) signedJwt.getClaimsSet().getClaim(JWT_CHALLENGE_RESPONSE_KEY);
        return MessageDigest.isEqual(answer.getBytes(), response.getBytes());
    }
}
