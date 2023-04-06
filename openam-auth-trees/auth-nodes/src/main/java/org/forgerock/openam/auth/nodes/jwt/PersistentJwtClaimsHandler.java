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
 * Copyright 2017-2023 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.jwt;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.forgerock.am.identity.domain.UniversalId;
import org.forgerock.caf.authentication.framework.AuthenticationFramework;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.opendj.ldap.Dn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.idm.IdRepoException;

/**
 * Handles several operations relating to Jwt claims.
 */
public class PersistentJwtClaimsHandler {

    private static final Logger logger = LoggerFactory.getLogger(PersistentJwtClaimsHandler.class);

    private static final String RLM_CLAIM_KEY = "openam.rlm";
    private static final String USR_CLAIM_KEY = "openam.usr";
    private static final String ATY_CLAIM_KEY = "openam.aty";
    private static final String CLIENTIP_CLAIM_KEY = "openam.clientip";
    private static final String OPENAM_CLIENT_IP_CLAIM_KEY = "openam.clientip";
    private static final String OPENAM_REALM_CLAIM_KEY = "openam.rlm";
    private static final String ID_ATTRIBUTE = "id";
    private static final String OPENAM_USER_CLAIM_KEY = "openam.usr";

    /**
     * Create an auth context. An auth context is a container that stores several authentication claims.
     *
     * @param orgName  the org.
     * @param clientId the client id.
     * @param service  the service.
     * @param clientIP the client IP address.
     * @return A map of authentication claims.
     */
    public Map<String, String> createJwtAuthContext(String orgName, String clientId, String service, String clientIP) {
        Map<String, String> authContext = new HashMap<>();
        if (orgName != null) {
            authContext.put(RLM_CLAIM_KEY, orgName.toLowerCase());
        }
        authContext.put(USR_CLAIM_KEY, clientId);
        authContext.put(ATY_CLAIM_KEY, service);
        authContext.put(CLIENTIP_CLAIM_KEY, clientIP);
        return authContext;
    }

    /**
     * Gets the claims set context.
     *
     * @param jwt    the jwt.
     * @param bundle the resource bundle.
     * @return the claims as a forgerock authentication Map.
     * @throws InvalidPersistentJwtException if the jwt has no claims.
     */
    public Map getClaimsSetContext(Jwt jwt, ResourceBundle bundle) throws InvalidPersistentJwtException {
        Map claimsSetContext = jwt.getClaimsSet().getClaim(AuthenticationFramework.ATTRIBUTE_AUTH_CONTEXT, Map.class);
        if (claimsSetContext == null) {
            throw new InvalidPersistentJwtException(bundle.getString("claims.context.not.found"));
        }
        return claimsSetContext;
    }

    /**
     * Validates the claims. Checks IP if enforcing IP is enabled. Checks claims org matches request org.
     *
     * @param claimsSetContext the claims context.
     * @param bundle           the resource bundle containing external string.
     * @param requestOrg       the org of the request.
     * @param clientIp         the IP of the request.
     * @param enforceClientIp  if true, validate the request IP matches the claim IP.
     * @throws InvalidPersistentJwtException if validation fails.
     */
    public void validateClaims(Map claimsSetContext, ResourceBundle bundle, String requestOrg, String clientIp,
            boolean enforceClientIp) throws InvalidPersistentJwtException {
        String jwtOrg = (String) claimsSetContext.get(OPENAM_REALM_CLAIM_KEY);
        String jwtClaimIP = (String) claimsSetContext.get(OPENAM_CLIENT_IP_CLAIM_KEY);
        validateRequestOrg(jwtOrg, requestOrg, bundle);

        if (enforceClientIp) {
            validateRequestIp(clientIp, jwtClaimIP, bundle);
        }
    }

    /**
     * Gets a username from a jwt claims context.
     *
     * @param claimsSetContext the claims set context.
     * @param bundle the resource bundle.
     * @return the username.
     * @throws InvalidPersistentJwtException if it cannot get the username.
     */
    public String getUsername(Map claimsSetContext, ResourceBundle bundle) throws InvalidPersistentJwtException {
        if (claimsSetContext == null) {
            throw new InvalidPersistentJwtException(bundle.getString("auth.failed.no.user.null.claims"));
        }
        String username = (String) claimsSetContext.get(OPENAM_USER_CLAIM_KEY);
        if (username == null) {
            throw new InvalidPersistentJwtException(bundle.getString("auth.failed.no.user.empty.claims"));
        }
        try {
            return UniversalId.of(Dn.valueOf(username)).getName();
        } catch (IdRepoException e) {
            logger.warn("Failed to parse universal id '{}' from claim '{}'", username, OPENAM_USER_CLAIM_KEY, e);
            throw new InvalidPersistentJwtException("Failed to parse universal Id from claim: "
                    + OPENAM_USER_CLAIM_KEY, e);
        }
    }

    private void validateRequestOrg(String jwtOrg, String requestOrg, ResourceBundle bundle)
            throws InvalidPersistentJwtException {
        if (!requestOrg.equals(jwtOrg)) {
            throw new InvalidPersistentJwtException(bundle.getString("auth.failed.diff.realm"));
        }
    }

    private void validateRequestIp(String requestIP, String jwtClaimIP, ResourceBundle bundle)
            throws InvalidPersistentJwtException {
        if (jwtClaimIP == null || jwtClaimIP.isEmpty()) {
            throw new InvalidPersistentJwtException(bundle.getString("auth.failed.ip.mismatch"));
        }
        if (requestIP == null || requestIP.isEmpty()) {
            throw new InvalidPersistentJwtException(bundle.getString("auth.failed.ip.mismatch"));
        }
        if (!jwtClaimIP.equals(requestIP)) {
            throw new InvalidPersistentJwtException(bundle.getString("auth.failed.ip.mismatch"));
        }
    }

}
