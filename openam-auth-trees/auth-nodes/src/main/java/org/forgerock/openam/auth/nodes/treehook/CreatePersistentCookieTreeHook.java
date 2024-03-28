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
 * Copyright 2017-2024 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.treehook;

import static org.forgerock.openam.auth.nodes.HmacSigningKeyConfig.DEPRECATED_STABLE_ID;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.openam.auth.node.api.TreeHook;
import org.forgerock.openam.auth.node.api.TreeHookException;
import org.forgerock.openam.auth.nodes.SetPersistentCookieNode;
import org.forgerock.openam.auth.nodes.jwt.InvalidPersistentJwtException;
import org.forgerock.openam.auth.nodes.jwt.PersistentJwtClaimsHandler;
import org.forgerock.openam.auth.nodes.jwt.PersistentJwtStringSupplier;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.secrets.cache.SecretCache;
import org.forgerock.openam.secrets.cache.SecretReferenceCache;
import org.forgerock.openam.session.Session;
import org.forgerock.openam.utils.Time;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.secrets.SecretReference;
import org.forgerock.secrets.keys.SigningKey;
import org.forgerock.util.Reject;
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
    private final SecretCache secretCache;
    private final Logger logger = LoggerFactory.getLogger(CreatePersistentCookieTreeHook.class);

    /**
     * The CreatePersistentCookieTreeHook constructor.
     *
     * @param session                         the session.
     * @param response                        the response.
     * @param config                          the config for creating a jwt.
     * @param request                         the request.
     * @param realm                           the realm.
     * @param persistentJwtStringSupplier     reference to a class which creates and updates jwts.
     * @param persistentCookieResponseHandler reference to a class which sets a persistent cookie on a Response.
     * @param persistentJwtClaimsHandler      reference to a class which can perform several operations on Jwt claims.
     * @param secretReferenceCache            the secret reference cache.
     */
    @Inject
    CreatePersistentCookieTreeHook(@Assisted Session session, @Assisted Response response,
            @Assisted SetPersistentCookieNode.Config config, @Assisted Request request,
            @Assisted Realm realm,
            PersistentJwtStringSupplier persistentJwtStringSupplier,
            PersistentCookieResponseHandler persistentCookieResponseHandler,
            PersistentJwtClaimsHandler persistentJwtClaimsHandler,
            SecretReferenceCache secretReferenceCache) {
        Reject.ifNull(session);
        this.session = session;
        this.response = response;
        this.config = config;
        this.request = request;
        this.persistentJwtStringSupplier = persistentJwtStringSupplier;
        this.persistentCookieResponseHandler = persistentCookieResponseHandler;
        this.persistentJwtClaimsHandler = persistentJwtClaimsHandler;
        this.secretCache = secretReferenceCache.realm(realm);
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

        SecretReference<SigningKey> signingReference = config.signingKeyReference(secretCache);

        String orgName = PersistentCookieResponseHandler.getOrgName(response);
        var authContext = persistentJwtClaimsHandler.createJwtAuthContext(orgName, clientId, service, clientIP);
        SigningKey signingKey;
        try {
            signingKey = signingReference.get();
        } catch (NoSuchSecretException e) {
            logger.error("No signing keys available to sign JWT.");
            throw new TreeHookException();
        }
        String kid = DEPRECATED_STABLE_ID.equals(signingKey.getStableId()) ? null : signingKey.getStableId();
        String jwtString = getJwtString(orgName, authContext, signingKey, kid);

        if (jwtString != null && !jwtString.isEmpty()) {
            long expiryInMillis = Time.currentTimeMillis() + config.maxLife().to(TimeUnit.MILLISECONDS);
            persistentCookieResponseHandler.setCookieOnResponse(response, request, config.persistentCookieName(),
                    jwtString, new Date(expiryInMillis), config.useSecureCookie(), config.useHttpOnlyCookie());
        }
    }

    private String getJwtString(String orgName, Map<String, String> authContext, SigningKey signingKey,
            String kid) throws TreeHookException {
        String jwtString;
        try {
            jwtString = persistentJwtStringSupplier.createJwtString(
                    orgName, authContext, config.maxLife().to(TimeUnit.HOURS),
                    config.idleTimeout().to(TimeUnit.HOURS), signingKey, kid
            );
        } catch (InvalidPersistentJwtException e) {
            logger.error("Error creating jwt string", e);
            throw new TreeHookException(e);
        }
        return jwtString;
    }
}
