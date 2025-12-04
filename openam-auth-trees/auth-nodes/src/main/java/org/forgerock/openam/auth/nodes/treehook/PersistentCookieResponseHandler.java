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
 * Copyright 2017-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.treehook;

import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import javax.inject.Inject;

import org.forgerock.http.header.SetCookieHeader;
import org.forgerock.http.protocol.Cookie;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.core.CoreWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.sm.DNMapper;

/**
 * Sets a persistent cookie on a Response. The format and parameters are specific to http set-cookie protocols.
 */
final class PersistentCookieResponseHandler {

    private static final String SET_COOKIE_HEADER_KEY = "Set-Cookie";
    private static final String DEFAULT_PATH = "/";

    private final CoreWrapper coreWrapper;
    private final Logger logger = LoggerFactory.getLogger(PersistentCookieResponseHandler.class);

    @Inject
    private PersistentCookieResponseHandler(CoreWrapper coreWrapper) {
        this.coreWrapper = coreWrapper;
    }

    /**
     * Sets a persistent cookie on a Response.
     *
     * @param response    the response.
     * @param cookieName  the name used as the persistent cookie.
     * @param cookieValue the value - usually a jwt.
     * @param isSecure   if true it adds the secure setting, for SSL only responses.
     * @param isHttpOnly if true it adds the http only setting.
     */
    void setCookieOnResponse(Response response, Request request, String cookieName, String cookieValue,
                             Date expiryDate, boolean isSecure, boolean isHttpOnly) {

        logger.debug("setCookieOnResponse");
        Collection<String> domains = coreWrapper.getCookieDomainsForRequest(request);
        if (!domains.isEmpty()) {
            logger.debug("domains is not empty");
            for (String domain : domains) {
                Cookie cookie = createCookie(cookieName, cookieValue, domain, expiryDate, isSecure, isHttpOnly);
                SetCookieHeader header = new SetCookieHeader(Collections.singletonList(cookie));
                for (String headerValue : header.getValues()) {
                    response.getHeaders().add(SET_COOKIE_HEADER_KEY, headerValue);
                }
            }
        } else {
            Cookie cookie = createCookie(cookieName, cookieValue, null, expiryDate, isSecure, isHttpOnly);
            response.getHeaders().put(SET_COOKIE_HEADER_KEY, cookie);
        }
    }

    private Cookie createCookie(String name, String value, String domain, Date expiryDate, boolean isSecure,
                                boolean isHttpOnly) {
        return new Cookie()
            .setName(name)
            .setValue(value)
            .setPath(DEFAULT_PATH)
            .setDomain(domain)
            .setExpires(expiryDate)
            .setSecure(isSecure)
            .setHttpOnly(isHttpOnly);
    }

    /**
     * Gets the org name from the response.
     *
     * @param response the response.
     * @return the org name.
     */
    static String getOrgName(Response response) {
        String realm = null;
        try {
            JsonValue jsonValues = JsonValue.json(response.getEntity().getJson());
            realm = jsonValues.get(REALM).asString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return DNMapper.orgNameToDN(realm);
    }
}
