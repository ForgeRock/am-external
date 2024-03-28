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
 * Copyright 2024 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.jwt;

import java.util.Map;
import java.util.Optional;

import org.forgerock.json.jose.exceptions.InvalidJwtException;
import org.forgerock.json.jose.utils.Utils;
import org.forgerock.util.Reject;

/**
 * Utility class for JWT headers.
 */
public final class JwtHeaderUtilities {

    private JwtHeaderUtilities() {
    }

    /**
     * Get the value of a header from a JWT.
     *
     * @param headerName the name of the header
     * @param jwtString  the JWT string
     * @return the value of the header
     */
    public static Optional<String> getHeader(String headerName, String jwtString) {
        Reject.ifNull(headerName);
        var headerValue = getHeaders(jwtString).get(headerName);
        return headerValue == null ? Optional.empty() : Optional.of((String) headerValue);
    }

    private static Map<String, Object> getHeaders(String jwtString) {
        Reject.ifNull(jwtString);
        String[] splitJwt = jwtString.split("\\.", -1);
        if (splitJwt.length < 2) {
            throw new InvalidJwtException("Invalid JWT string, missing dots");
        }
        String header = Utils.decodeJwtComponent(splitJwt[0]);
        return Utils.parseJson(header);
    }
}
