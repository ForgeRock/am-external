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
package org.forgerock.openam.services.push.dispatch.predicates;

import static org.forgerock.openam.services.push.PushNotificationConstants.JWT_CHALLENGE_RESPONSE_KEY;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.util.Reject;

import java.security.MessageDigest;

/**
 * Checks that the response to a number challenge is appropriate for the
 * user from whom it claims to be sent, by asserting that the
 * contents is the correct response to the challenge.
 */
public class PushNumberChallengeResponsePredicate extends AbstractPredicate {

    private String answer;
    private String location;

    /**
     * Create a new Predicate that will ensure that the contents of the JsonValue found at the location of the
     * JsonPointer is equal to a number challenge response value.
     *
     * @param answer Generated number challenge answer.
     * @param location Json Pointer string of the location of the value expected to be the challenge response.
     */
    public PushNumberChallengeResponsePredicate(String answer, String location) {
        Reject.ifNull(answer, location);
        this.location = location;
        this.answer = answer;
    }

    @Override
    public boolean perform(JsonValue content) {
        Jwt signedJwt = new JwtReconstruction().reconstructJwt(content.get(new JsonPointer(location)).asString(),
                Jwt.class);

        String response = (String) signedJwt.getClaimsSet().getClaim(JWT_CHALLENGE_RESPONSE_KEY);

        return MessageDigest.isEqual(answer.getBytes(), response.getBytes());
    }

    /**
     * Default constructor for the PushNumberChallengeResponsePredicate, used for serialization purposes.
     */
    public PushNumberChallengeResponsePredicate() {
        //This section intentionally left blank.
    }
}
