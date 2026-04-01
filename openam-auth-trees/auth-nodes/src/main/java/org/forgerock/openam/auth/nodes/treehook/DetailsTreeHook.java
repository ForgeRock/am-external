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
 * Copyright 2024-2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.treehook;

import static com.sun.identity.shared.Constants.AM_COOKIE_DEFAULT_VALUE;
import static com.sun.identity.shared.Constants.AM_COOKIE_NAME;
import static com.sun.identity.shared.Constants.AM_LB_COOKIE_DEFAULT_VALUE;
import static com.sun.identity.shared.Constants.AM_LB_COOKIE_NAME;

import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;

import org.forgerock.http.util.Json;
import org.forgerock.openam.auth.node.api.TreeHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iplanet.am.util.SystemPropertiesWrapper;

/**
 * A tree hook that sets details on a JSON response.
 */
abstract class DetailsTreeHook implements TreeHook {

    private static final Logger logger = LoggerFactory.getLogger(DetailsTreeHook.class);

    /**
     * Reserved headers that should not be overridden.
     */
    public static final Set<String> RESERVED_HEADERS = Set.of("X-Frame-Options", "X-Content-Type-Options",
            "Cache-Control", "Content-API-Version", "Expires", "Pragma", "Content-Type", "Content-Length", "Date",
            "Keep-Alive", "Connection", "Transfer-Encoding", "Server", "Trailer", "Upgrade");

    private final Pattern cookiePattern;

    protected DetailsTreeHook(SystemPropertiesWrapper systemProperties) {
        String ssoCookieName = systemProperties.get(AM_COOKIE_NAME, AM_COOKIE_DEFAULT_VALUE);
        String lbCookieName = systemProperties.get(AM_LB_COOKIE_NAME, AM_LB_COOKIE_DEFAULT_VALUE);
        String combinedPattern = Pattern.quote(ssoCookieName) + "|" + Pattern.quote(lbCookieName);
        cookiePattern = Pattern.compile(combinedPattern, Pattern.CASE_INSENSITIVE);
    }

    /**
     * Converts a string value to an object if it is a string representation of some valid JSON, otherwise returns
     * the input string.
     * @param value The string value to convert
     * @return The object, or the input string if conversion fails.
     */
    protected Object convertStringToJson(String value) {
        try {
            return Json.readJson(value);
        } catch (IOException e) {
            logger.debug("Failed to parse JSON value: {}", value, e);
            return value;
        }
    }

    /**
     * Checks if the header is not a reserved cookie.
     * @param key The header key.
     * @param value The header value.
     * @return true if the header is not a reserved cookie.
     */
    protected boolean notReservedCookie(String key, String value) {
        return !key.equalsIgnoreCase("Set-Cookie")
                       || !cookiePattern.matcher(value).find();
    }
}
