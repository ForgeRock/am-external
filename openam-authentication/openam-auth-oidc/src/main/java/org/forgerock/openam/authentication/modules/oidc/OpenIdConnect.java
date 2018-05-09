/*
 * Copyright 2014-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.oidc;

import static org.forgerock.openam.authentication.modules.oidc.OpenIdConnectConfig.*;

import java.security.Principal;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;

import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.authentication.modules.common.mapping.AccountProvider;
import org.forgerock.openam.authentication.modules.common.mapping.AttributeMapper;
import org.forgerock.openam.oauth2.OAuth2Constants.ProofOfPossession;

import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.shared.debug.Debug;

/**
 * Because the OpenIdResolver instances, responsible for validating ID Tokens for a given issuer, require pulling
 * url state to initialize, I want to cache these instances once created. This raises concerns of cache coherence, especially
 * if the OpenIdResolvers are mapped with the issuer key, which could be the same for different OIDC ID Token providers
 * (at least nothing in the specs was found that mandate this uniqueness, and nothing enforces this uniqueness either).
 * So the OpenIdResolver instances will be mapped with unique keys corresponding to their crypto context - so either:
 * 1. the .well-known/openid-connect configuration url or 2. the jwk url or 3. the client_secret. Caching the OpenIdResolvers
 * with keys unique to a given crypto context excludes the cache conflict resulting from multiple OpenIdConnect modules
 * being created with the same issuer name (previously, the issuer name was used as the cache key). This will allow
 * for the definition of multiple OpenIdConnect modules with the same name, and if these multiple modules reference the
 * same crypto state with this name (e.g. they share the same config url, jwk url, or client_secret), a single cache entry
 * will satisfy both, as the cache key defines the crypto context. Likewise, if multiple OpenIdConnect instances are created
 * with the same issuer name, but different crypto context, cache entries will exist for both, as the respective OpenIdResolver
 * instances are cached with a key corresponding to the crypto context. And because the authN framework insures that a given
 * OpenIdConnect instance is initialized with the appropriate configuration state, it will be possible to determine which cache key to use,
 * as each module will be created with the specification of only a single crypto context (config url, jwk url, or client_secret).
 * Finally, I will validate the configured issuer name against the iss field in the ID Token jwk, so I can catch scenarios
 * where a ID Token is dispatched for validation against the wrong, or incorrectly configured, OpenIdConnect module.
 */
public class OpenIdConnect extends AMLoginModule {
    private static Debug logger = Debug.getInstance("amAuth");

    private OpenIdConnectConfig config;
    private String principalName;
    private JwtHandler jwtHandler;

    private Map<String, Object> sharedState;


    @Override
    public void init(Subject subject, Map sharedState, Map options) {
        config = new OpenIdConnectConfig(options);
        this.jwtHandler = new JwtHandler(config);
        this.sharedState = sharedState;
    }

    @Override
    public int process(Callback[] callbacks, int state) throws LoginException {
        final HttpServletRequest request = getHttpServletRequest();
        final String jwtValue = request.getHeader(config.getHeaderName());
        if (jwtValue == null || jwtValue.isEmpty()) {
            logger.error("No OpenIdConnect ID Token referenced by header value: " + config.getHeaderName());
            throw new AuthLoginException(RESOURCE_BUNDLE_NAME, BUNDLE_KEY_MISSING_HEADER, null);
        }

        JwtClaimsSet jwtClaims = jwtHandler.validateJwt(jwtValue);

        if (!JwtHandler.isIntendedForAudience(config.getAudienceName(), jwtClaims)) {
            logger.error("ID token is not for this audience.");
            throw new AuthLoginException(RESOURCE_BUNDLE_NAME, BUNDLE_KEY_ID_TOKEN_BAD_AUDIENCE, null);
        }
        if (JwtHandler.jwtHasAuthorizedPartyClaim(jwtClaims)) {
            if (!JwtHandler.isFromValidAuthorizedParty(config.getAcceptedAuthorizedParties(), jwtClaims)) {
                logger.error("ID token was received from invalid authorized party.");
                throw new AuthLoginException(RESOURCE_BUNDLE_NAME, BUNDLE_KEY_INVALID_AUTHORIZED_PARTY, null);
            }
        }
        principalName = mapPrincipal(jwtClaims);
        storeUsername(principalName);

        if (jwtClaims.isDefined(ProofOfPossession.CNF)) {
            sharedState.put("org.forgerock.openam.authentication.modules.jwtpop.cnf",
                    jwtClaims.get(ProofOfPossession.CNF));
        }

        return ISAuthConstants.LOGIN_SUCCEED;
    }

    private String mapPrincipal(JwtClaimsSet jwtClaimsSet) throws AuthLoginException {
        AttributeMapper<JwtClaimsSet> principalMapper = instantiatePrincipalMapper();
        AccountProvider accountProvider = instantiateAccountProvider();
        Map<String, Set<String>> lookupAttrs =
                principalMapper.getAttributes(config.getJwkToLocalAttributeMappings(), jwtClaimsSet);
        if (lookupAttrs.isEmpty()) {
            logger.error("None of the attributes specified in the mappings could be found in the Id Token.");
            throw new AuthLoginException(RESOURCE_BUNDLE_NAME, BUNDLE_KEY_NO_ATTRIBUTES_MAPPED, null);
        }
        final AMIdentity id = accountProvider.searchUser(getAMIdentityRepository(getRequestOrg()), lookupAttrs);
        if (id == null) {
            logger.message("Unable to determine principal from account mapper for {}", jwtClaimsSet.getSubject());
            if (config.isSubClaimUsedIfNoMatch()) {
                return jwtClaimsSet.getSubject();
            } else {
                throw new AuthLoginException(RESOURCE_BUNDLE_NAME, BUNDLE_KEY_PRINCIPAL_MAPPING_FAILURE, null);
            }
        }
        return id.getName();
    }

    private AccountProvider instantiateAccountProvider() throws AuthLoginException {
        try {
            return Class.forName(config.getAccountProviderClass()).asSubclass(AccountProvider.class).
                    newInstance();
        } catch (Exception e) {
            logger.error("Exception caught instantiating principal mapper class: " + e, e);
            throw new AuthLoginException(RESOURCE_BUNDLE_NAME, BUNDLE_KEY_PRINCIPAL_MAPPER_INSTANTIATION_ERROR, null);
        }
    }

    private AttributeMapper<JwtClaimsSet> instantiatePrincipalMapper() throws AuthLoginException {
        try {
            return Class.forName(config.getPrincipalMapperClass()).asSubclass(AttributeMapper.class).
                    newInstance();
        } catch (Exception e) {
            logger.error("Exception caught instantiating principal mapper class: " + e, e);
            throw new AuthLoginException(RESOURCE_BUNDLE_NAME, BUNDLE_KEY_PRINCIPAL_MAPPER_INSTANTIATION_ERROR, null);
        }
    }

    @Override
    public Principal getPrincipal() {
        return new Principal() {
            public String getName() {
                return principalName;
            }
        };
    }
}