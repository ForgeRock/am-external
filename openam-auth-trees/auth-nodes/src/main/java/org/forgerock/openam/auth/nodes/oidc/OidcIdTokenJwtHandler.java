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
 * Copyright 2023 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.oidc;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.sun.identity.authentication.spi.AuthLoginException;

import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.exceptions.FailedToLoadJWKException;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.JwsAlgorithmType;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.oauth.resolvers.OpenIdResolver;
import org.forgerock.oauth.resolvers.SharedSecretOpenIdResolverImpl;
import org.forgerock.oauth.resolvers.exceptions.OpenIdConnectVerificationException;
import org.forgerock.openam.jwt.JwtClaimsValidationHandler;
import org.forgerock.openam.jwt.JwtClaimsValidationOptions;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.secrets.GenericSecret;
import org.forgerock.secrets.NoSuchSecretException;
import org.forgerock.secrets.SecretReference;
import org.forgerock.util.Strings;
import org.forgerock.util.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * The logic required to validate the integrity of an OIDC ID token JWT.
 */
class OidcIdTokenJwtHandler {

    private final Logger logger = LoggerFactory.getLogger(OidcIdTokenJwtHandler.class);

    private final OidcResolverCache oidcResolverCache;

    private final JwtReconstruction jwtReconstruction;

    private final OidcNode.Config nodeConfig;
    private final SecretReference<GenericSecret> clientSecret;

    /**
     * OidcIdTokenJwtHandler Constructor.
     *
     * @param idResolverCache interface implemented by the OidcResolverCacheImpl
     * @param nodeConfig Assisted config for the OidcNode
     * @param jwtReconstruction JwtReconstruction object
     * @param clientSecret Secret reference to a secret in secret stores
     */
    @Inject
    OidcIdTokenJwtHandler(OidcResolverCacheImpl idResolverCache, @Assisted OidcNode.Config nodeConfig,
                          JwtReconstruction jwtReconstruction,
                          @Assisted Optional<SecretReference<GenericSecret>> clientSecret) {
        this.oidcResolverCache = idResolverCache;
        this.nodeConfig = nodeConfig;
        this.clientSecret = clientSecret.orElse(null);
        this.jwtReconstruction = jwtReconstruction;
    }

    /**
     * Retrieve the actual JWT token from the encoded JWT token String.
     *
     * @param jwtString The encoded JWT string
     * @return The reconstructed JWT object
     */
    public SignedJwt createJwtFromString(String jwtString) {
        return jwtReconstruction.reconstructJwt(jwtString, SignedJwt.class);
    }

    /**
     * Validate the integrity of the JWT OIDC token, according to the spec
     * (<a href="http://openid.net/specs/openid-connect-core-1_0.html#IDTokenValidation">...</a>).
     *
     * @param jwt The JWT Token
     * @throws AuthLoginException If the JWT is not valid
     * @return true if the JWT is valid
     */
    public boolean isJwtValid(SignedJwt jwt) throws AuthLoginException {

        List<String> audience = jwt.getClaimsSet().getAudience();
        String authorizedParties = jwt.getClaimsSet().get(OAuth2Constants.JWTTokenParams.AZP).asString();
        JwsAlgorithm algorithm = jwt.getHeader().getAlgorithm();
        Duration unreasonableLifetimeLimit = Duration.duration(nodeConfig.unreasonableLifetimeLimit(),
                TimeUnit.MINUTES);

        JwtClaimsValidationOptions<AuthLoginException> validationOptions =
                new JwtClaimsValidationOptions<>(AuthLoginException::new)
                        .setIssuer(nodeConfig.idTokenIssuer())
                        .setAcceptedAudiences(List.of(nodeConfig.audienceName()))
                        .setUnreasonableLifetimeLimit(unreasonableLifetimeLimit)
                        .setAudienceRequired(true);
        JwtClaimsValidationHandler<AuthLoginException> claimsValidator =
                new JwtClaimsValidationHandler<>(validationOptions, jwt.getClaimsSet());

        // 1. If JWT is encrypted, decrypt it using the keys/algorithm specified
        // This step is done within the 'reconstructJwt' method

        // validateClaims() validates steps 2, 3, 9 and 10:
        // - Issuer of JWT MUST match that of the provider
        // - Audience of JWT MUST match that of the provider
        // - The current time MUST be before the time represented by the 'exp' Claim.
        // - The iat Claim CAN be used to reject tokens that were issued at an unreasonable time
        claimsValidator.validateClaims();

        // 4. If there are multiple audiences, the Client SHOULD verify that an 'azp' claim is present
        if (audience.size() > 1 && !authorizedPartiesExists(authorizedParties)) {
            logger.debug("Missing 'azp' claim in JWT token when having multiple audiences");
            return false;
        }

        // 5. If an 'azp' claim exists, the Client SHOULD verify that its client_id is the Claim Value
        if (authorizedPartiesExists(authorizedParties) && !isAuthorizedPartiesValid(authorizedParties)) {
            logger.debug("'azp' claim does not match that of the provider");
            return false;
        }

        // 7. The 'alg' value SHOULD be the default of RS256 or one specified during registration
        // NOTE: Cannot access the client's specified algorithm, therefore cannot check whether it matches
        if (!isAlgorithmDefaultValue(algorithm)) {
            logger.debug("'alg' is not the default algorithm RS256");
        }

        // 8. If JWT uses MAC based algorithm, the validation type MUST be client secret
        if (usingMacBasedAlgorithmAndNotClientSecret(algorithm)) {
            logger.debug("Client secret must be used when using a MAC based algorithm");
            return false;
        }

        // 6. The Client MUST validate the signature according to JWS using the algorithm specified in the
        // JWT alg Header Parameter. The Client MUST use the keys provided by the Issuer.
        OpenIdResolver resolver = handleResolver(jwt);
        try {
            resolver.validateIdentity(jwt);
        } catch (OpenIdConnectVerificationException e) {
            logger.debug("Verification of ID Token failed");
            return false;
        }

        // Return true if JWT is valid
        return true;
    }


    /**
     * A method to check if authorized parties exist.
     *
     * @param authorizedParties ID token accepted authorized parties
     * @return true if authorized parties exist
     */
    public boolean authorizedPartiesExists(String authorizedParties) {
        return !Strings.isBlank(authorizedParties);
    }

    /**
     * A method to check if the authorized parties of the token match the node configuration.
     *
     * @param authorizedParties ID token accepted authorized parties
     * @return true if the authorized parties are valid
     */
    public boolean isAuthorizedPartiesValid(String authorizedParties) {
        return nodeConfig.authorisedParties().contains(authorizedParties);
    }

    /**
     * A method to check if the default algorithm (RS256) is being used.
     *
     * @param algorithm JwsAlgorithm being used
     * @return true if the default algorithm is being used
     */
    public boolean isAlgorithmDefaultValue(JwsAlgorithm algorithm) {
        return JwsAlgorithm.RS256.equals(algorithm);
    }

    /**
     * A method to check if a MAC-based algorithm is being used CLIENT_SECRET is not the validation type.
     *
     * @param algorithm JwsAlgorithm being used
     * @return true if a MAC-based algorithm is being used and CLIENT_SECRET is not the validation type
     */
    public boolean usingMacBasedAlgorithmAndNotClientSecret(JwsAlgorithm algorithm) {
        return JwsAlgorithmType.HMAC.equals(algorithm.getAlgorithmType())
                && !OidcNode.OpenIdValidationType.CLIENT_SECRET.equals(nodeConfig.oidcValidationType());
    }

    /**
     * A method to create a specific resolver based on the node's validation type configuration. This includes
     * the process of checking if a suitable resolver already exists within the oidcResolverCache. This
     * resolver is created in order to use the correct verifySignature method.
     *
     * @param jwt SignedJwt token
     * @return OpenIdResolver A suitable resolver
     * @throws AuthLoginException catches MalformedURLException and FailedToLoadJWKException
     */
    public OpenIdResolver handleResolver(SignedJwt jwt) throws AuthLoginException {

        OpenIdResolver resolver = null;

        String issuer = jwt.getClaimsSet().getIssuer();

        try {

            // If a client secret is being used, create new SharedSecretOpenIdResolverImpl
            if (OidcNode.OpenIdValidationType.CLIENT_SECRET.equals(nodeConfig.oidcValidationType())) {
                if (clientSecret != null) {
                    String clientSecret = getTextFromClientSecret();
                    resolver = new SharedSecretOpenIdResolverImpl(issuer, clientSecret);
                } else {
                    throw new AuthLoginException("ValidationType is Client Secret but Client Secret parameter is null");
                }
                // If there is a validation value, check if a resolver already exists in the cache
            } else if (StringUtils.isNotEmpty(nodeConfig.oidcValidationValue())) {
                resolver = oidcResolverCache.getResolverForIssuer(issuer, nodeConfig.oidcValidationValue());
            }

            // If not resolver exists in the cache, create a new one
            if (resolver == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Creating OpenIdResolver for issuer {} using config url {}", issuer,
                            nodeConfig.oidcValidationType());
                }
                URL validationValueURL = new URL(nodeConfig.oidcValidationValue());
                resolver = oidcResolverCache.createResolver(issuer, String.valueOf(nodeConfig.oidcValidationType()),
                        nodeConfig.oidcValidationValue(), validationValueURL);
            }
        } catch (MalformedURLException e) {
            throw new AuthLoginException("Failed to create URL Object from validation type", e);
        } catch (FailedToLoadJWKException e) {
            throw new AuthLoginException("Could not create a new OpenIdResolver", e);
        } catch (NoSuchSecretException e) {
            throw new AuthLoginException("Configured secret does not exist", e);
        }

        return resolver;
    }

    /**
     * A method to retrieve client secret from the memory as a plain text to use in SharedSecretOpenIdResolverImpl.
     *
     * @return Plain text form of the client secret
     */
    private String getTextFromClientSecret() throws NoSuchSecretException {
        return clientSecret.get().revealAsUtf8(String::new);
    }
}