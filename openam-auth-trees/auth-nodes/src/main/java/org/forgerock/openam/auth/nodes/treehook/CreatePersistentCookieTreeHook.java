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
 * Copyright 2017-2018 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.treehook;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.openam.auth.node.api.TreeHook;
import org.forgerock.openam.auth.node.api.TreeHookException;
import org.forgerock.openam.auth.nodes.SetPersistentCookieNode;
import org.forgerock.openam.auth.nodes.jwt.InvalidPersistentJwtException;
import org.forgerock.openam.auth.nodes.jwt.PersistentJwtClaimsHandler;
import org.forgerock.openam.auth.nodes.jwt.PersistentJwtStringSupplier;
import org.forgerock.openam.session.Session;
import org.forgerock.util.time.TimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;
import com.iplanet.dpro.session.SessionException;
import com.sun.identity.authentication.util.ISAuthConstants;

/**
 * A TreeHook for creating persistent cookies.
 */
@TreeHook.Metadata(configClass = SetPersistentCookieNode.Config.class)
public class CreatePersistentCookieTreeHook implements TreeHook {

    private static final String SERVICE_SESSION_PROPERTY = "Service";
    private final Session session;
    private final Response response;
    private final Request request;
    private final SetPersistentCookieNode.Config config;
    private final PersistentJwtStringSupplier persistentJwtStringSupplier;
    private final PersistentJwtClaimsHandler persistentJwtClaimsHandler;
    private final PersistentCookieResponseHandler persistentCookieResponseHandler;
    private final Logger logger = LoggerFactory.getLogger("amAuth");

    /**
     * The CreatePersistentCookieTreeHook constructor.
     *
     * @param session the session.
     * @param response the response.
     * @param request the request.
     * @param config the config for creating a jwt.
     */
    @Inject
    public CreatePersistentCookieTreeHook(@Assisted Session session, @Assisted Response response,
                                          @Assisted SetPersistentCookieNode.Config config, @Assisted Request request) {
        this.session = session;
        this.response = response;
        this.config = config;
        this.request = request;
        this.persistentJwtStringSupplier = InjectorHolder.getInstance(PersistentJwtStringSupplier.class);
        this.persistentCookieResponseHandler = InjectorHolder.getInstance(PersistentCookieResponseHandler.class);
        this.persistentJwtClaimsHandler = new PersistentJwtClaimsHandler();
    }

    @Override
    public void accept() throws TreeHookException {
        logger.debug("creating persistent cookie tree hook");
        String clientId, service, clientIP;
        try {
            clientId = session.getClientID();
            service = session.getProperty(SERVICE_SESSION_PROPERTY);
            clientIP = session.getProperty(ISAuthConstants.HOST);
            logger.debug("clientId {} \n service {} \n clientIP {}", clientId, service, clientIP);
        } catch (SessionException e) {
            logger.error("Tree hook creation exception", e);
            throw new TreeHookException(e);
        }
        String orgName = PersistentCookieResponseHandler.getOrgName(response);
        Map<String, String> authContext = persistentJwtClaimsHandler.createJwtAuthContext(orgName, clientId, service,
                clientIP);
        String jwtString;

        try {
            jwtString = persistentJwtStringSupplier.createJwtString(orgName, authContext, config.maxLife()
                .to(TimeUnit.HOURS), config.idleTimeout().to(TimeUnit.HOURS), String.valueOf(config.hmacSigningKey()));
        } catch (InvalidPersistentJwtException e) {
            logger.error("Error creating jwt string", e);
            throw new TreeHookException(e);
        }

        if (jwtString != null && !jwtString.isEmpty()) {
            long expiryInMillis = TimeService.SYSTEM.now() + config.maxLife().to(TimeUnit.MILLISECONDS);
            persistentCookieResponseHandler.setCookieOnResponse(response, request, config.persistentCookieName(),
                    jwtString, new Date(expiryInMillis), config.useSecureCookie(), config.useHttpOnlyCookie());
        }
    }

}
