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
 * Copyright 2017-2019 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.treehook;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.http.protocol.Cookie;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.openam.auth.node.api.TreeHook;
import org.forgerock.openam.auth.node.api.TreeHookException;
import org.forgerock.openam.auth.nodes.jwt.PersistentJwtProvider;
import org.forgerock.openam.auth.nodes.PersistentCookieDecisionNode;
import org.forgerock.openam.auth.nodes.jwt.InvalidPersistentJwtException;
import org.forgerock.openam.auth.nodes.jwt.PersistentJwtStringSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

/**
 * A TreeHook for updating a persistent cookie.
 */
@TreeHook.Metadata(configClass = PersistentCookieDecisionNode.Config.class)
public class UpdatePersistentCookieTreeHook implements TreeHook {

    private final Request request;
    private final Response response;
    private final PersistentCookieDecisionNode.Config config;
    private final PersistentJwtProvider persistentJwtProvider;
    private final PersistentJwtStringSupplier persistentJwtStringSupplier;
    private final PersistentCookieResponseHandler persistentCookieResponseHandler;
    private final Logger logger = LoggerFactory.getLogger(UpdatePersistentCookieTreeHook.class);

    /**
     * The UpdatePersistentCookieTreeHook Constructor.
     *
     * @param request The request.
     * @param response The response.
     * @param config the config for updating the cookie.
     */
    @Inject
    public UpdatePersistentCookieTreeHook(@Assisted Request request, @Assisted Response response,
                                          @Assisted PersistentCookieDecisionNode.Config config) {
        this.request = request;
        this.response = response;
        this.config = config;
        this.persistentJwtProvider = InjectorHolder.getInstance(PersistentJwtProvider.class);
        this.persistentJwtStringSupplier = InjectorHolder.getInstance(PersistentJwtStringSupplier.class);
        this.persistentCookieResponseHandler = InjectorHolder.getInstance(PersistentCookieResponseHandler.class);
    }

    @Override
    public void accept() throws TreeHookException {
        logger.debug("UpdatePersistentCookieTreeHook.accept");
        String orgName = PersistentCookieResponseHandler.getOrgName(response);
        Cookie originalJwt = getJwtCookie(request, config.persistentCookieName());
        if (originalJwt != null) {
            String jwtString;
            Date expirationTime;
            try {
                jwtString = persistentJwtStringSupplier.getUpdatedJwt(originalJwt.getValue(), orgName,
                        String.valueOf(config.hmacSigningKey()), config.idleTimeout().to(TimeUnit.HOURS));
                Jwt jwt = persistentJwtProvider.getValidDecryptedJwt(jwtString, orgName,
                        String.valueOf(config.hmacSigningKey()));
                expirationTime = jwt.getClaimsSet().getExpirationTime();
            } catch (InvalidPersistentJwtException e) {
                logger.error("Invalid jwt", e);
                throw new TreeHookException(e);
            }

            if (jwtString != null && !jwtString.isEmpty()) {
                persistentCookieResponseHandler.setCookieOnResponse(response, request, config.persistentCookieName(),
                        jwtString, expirationTime, config.useSecureCookie(), config.useHttpOnlyCookie());
            }
        }
    }

    private Cookie getJwtCookie(Request request, String cookieName) {
        if (request.getCookies().containsKey(cookieName)) {
            List<Cookie> cookies = request.getCookies().get(cookieName);
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(cookieName)) {
                    return cookie;
                }
            }
        }
        return null;
    }

}
