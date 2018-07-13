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
 * Copyright 2017 ForgeRock AS.
 */

package org.forgerock.openam.services.push.dispatch.handlers;

import static org.forgerock.openam.services.push.PushNotificationConstants.*;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.services.push.PushNotificationConstants;
import org.forgerock.openam.services.push.dispatch.MessagePromise;

/**
 * Authentication cluster message handler.
 */
public class AuthClusterMessageHandler extends AbstractClusterMessageHandler {

    private final JwtReconstruction jwtReconstruction;

    /**
     * Generate a new Authentication cluster message handler.
     *
     * @param ctsPersistentStore For accessing tokens in the CTS.
     * @param jwtReconstruction For recreating JWTs from plain JSON Strings.
     */
    @Inject
    public AuthClusterMessageHandler(CTSPersistentStore ctsPersistentStore, JwtReconstruction jwtReconstruction) {
        super(ctsPersistentStore);
        this.jwtReconstruction = jwtReconstruction;
    }

    @Override
    public void update(Token token, JsonValue content) throws CoreTokenException {

        Jwt possibleDeny = jwtReconstruction.reconstructJwt(content.get(JWT).asString(), SignedJwt.class);

        if (possibleDeny.getClaimsSet().getClaim(JWT_DENY_CLAIM_KEY) != null) {
            token.setAttribute(PushNotificationConstants.CTS_ACCEPT_TOKEN_FIELD, DENY_VALUE);
        } else {
            token.setAttribute(PushNotificationConstants.CTS_ACCEPT_TOKEN_FIELD, ACCEPT_VALUE);
        }

        ctsPersistentStore.update(token);
    }


    @Override
    public JsonValue getContents(MessagePromise messagePromise) {
        return null; //not useful in standard impl.
    }
}
