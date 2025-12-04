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

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.forgerock.http.protocol.Cookie;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.openam.auth.node.api.TreeHook;
import org.forgerock.openam.auth.node.api.TreeHookException;
import org.forgerock.openam.auth.nodes.PersistentCookieDecisionNode;
import org.forgerock.openam.auth.nodes.jwt.InvalidPersistentJwtException;
import org.forgerock.openam.auth.nodes.jwt.JwtHeaderUtilities;
import org.forgerock.openam.auth.nodes.jwt.PersistentJwtStringSupplier;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.secrets.cache.SecretCache;
import org.forgerock.openam.secrets.cache.SecretReferenceCache;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.secrets.SecretReference;
import org.forgerock.secrets.ValidSecretsReference;
import org.forgerock.secrets.keys.SigningKey;
import org.forgerock.secrets.keys.VerificationKey;
import org.forgerock.util.promise.NeverThrowsException;
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
    private final PersistentJwtStringSupplier persistentJwtStringSupplier;
    private final PersistentCookieResponseHandler persistentCookieResponseHandler;
    private final Logger logger = LoggerFactory.getLogger(UpdatePersistentCookieTreeHook.class);
    private final SecretCache secretCache;

    /**
     * The UpdatePersistentCookieTreeHook Constructor.
     *
     * @param request                         The request.
     * @param response                        The response.
     * @param config                          The config for updating the cookie.
     * @param realm                           The realm.
     * @param persistentJwtStringSupplier     The persistentJwtStringSupplier.
     * @param persistentCookieResponseHandler The persistentCookieResponseHandler.
     * @param secretReferenceCache            The secretReferenceCache.
     */
    @Inject
    UpdatePersistentCookieTreeHook(@Assisted Request request, @Assisted Response response,
            @Assisted PersistentCookieDecisionNode.Config config, @Assisted Realm realm,
            PersistentJwtStringSupplier persistentJwtStringSupplier,
            PersistentCookieResponseHandler persistentCookieResponseHandler,
            SecretReferenceCache secretReferenceCache) {
        this.request = request;
        this.response = response;
        this.config = config;
        this.persistentJwtStringSupplier = persistentJwtStringSupplier;
        this.persistentCookieResponseHandler = persistentCookieResponseHandler;
        this.secretCache = secretReferenceCache.realm(realm);
    }

    @Override
    public void accept() throws TreeHookException {
        logger.debug("UpdatePersistentCookieTreeHook.accept");
        String orgName = PersistentCookieResponseHandler.getOrgName(response);
        Cookie originalJwt = getJwtCookie(request, config.persistentCookieName());
        if (originalJwt == null) {
            return;
        }
        Date expirationTime;
        Optional<String> kid = JwtHeaderUtilities.getHeader("kid", originalJwt.getValue());
        SecretReference<SigningKey> signingKeySecretReference = config.signingKeyReference(secretCache);
        ValidSecretsReference<VerificationKey, NeverThrowsException> verificationKeysReference =
                config.verificationKeysReference(kid.orElse(null), secretCache);
        Jwt jwt = getUpdatedJwt(originalJwt, orgName, signingKeySecretReference, verificationKeysReference);
        if (jwt == null) {
            throw new TreeHookException("Jwt reconstruction error");
        }
        expirationTime = jwt.getClaimsSet().getExpirationTime();

        String jwtString = jwt.build();
        if (jwtString != null && !jwtString.isEmpty()) {
            persistentCookieResponseHandler.setCookieOnResponse(response, request,
                    config.persistentCookieName(),
                    jwtString, expirationTime, config.useSecureCookie(), config.useHttpOnlyCookie());
        }
    }

    private Jwt getUpdatedJwt(Cookie jwtCookie, String orgName,
            SecretReference<SigningKey> signingKeyReference, ValidSecretsReference<VerificationKey,
            NeverThrowsException> verificationKeysReference) {
        try {
            return persistentJwtStringSupplier.getUpdatedJwt(jwtCookie.getValue(), orgName,
                    signingKeyReference, verificationKeysReference, config.idleTimeout().to(TimeUnit.HOURS));
        } catch (InvalidPersistentJwtException e) {
            logger.error("Attempt to verify JWT failed");
            return null;
        } catch (NoSuchSecretException e) {
            logger.error("Could not find signing key");
            return null;
        }
    }

    private Cookie getJwtCookie(Request request, String cookieName) {
        if (!request.getCookies().containsKey(cookieName)) {
            return null;
        }
        List<Cookie> cookies = request.getCookies().get(cookieName);
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(cookieName)) {
                return cookie;
            }
        }
        return null;
    }

}
