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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2021-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.oauth;

import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.exceptions.FailedToLoadJWKException;
import org.forgerock.json.jose.exceptions.InvalidJwtException;
import org.forgerock.json.jose.jwe.EncryptedJwt;
import org.forgerock.json.jose.jwe.JweHeader;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.oauth.DataStore;
import org.forgerock.oauth.OAuthException;
import org.forgerock.oauth.UserInfo;
import org.forgerock.oauth.clients.oidc.OpenIDConnectClient;
import org.forgerock.oauth.clients.oidc.OpenIDConnectClientConfiguration;
import org.forgerock.oauth.resolvers.JWKOpenIdResolverImpl;
import org.forgerock.oauth.resolvers.OpenIdResolver;
import org.forgerock.oauth.resolvers.exceptions.OpenIdConnectVerificationException;
import org.forgerock.oauth.resolvers.service.OpenIdResolverService;
import org.forgerock.oauth.resolvers.service.OpenIdResolverServiceConfigurator;
import org.forgerock.oauth.resolvers.service.OpenIdResolverServiceImpl;
import org.forgerock.openam.jwt.JwtDecryptionHandler;
import org.forgerock.openam.jwt.JwtEncryptionOptions;
import org.forgerock.openam.jwt.exceptions.DecryptionFailedException;
import org.forgerock.openam.secrets.SecretsProviderFacade;
import org.forgerock.openam.social.idp.OpenIDConnectClientConfig;
import org.forgerock.openam.social.idp.UserInfoResponseType;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Clock;

import static org.forgerock.http.protocol.Responses.noopExceptionAsyncFunction;
import static org.forgerock.openam.oauth2.OAuth2Constants.ClientPurpose.RP_ID_TOKEN_DECRYPTION;
import static org.forgerock.util.CloseSilentlyAsyncFunction.closeSilently;
import static org.forgerock.util.Closeables.closeSilentlyAsync;
import static org.forgerock.util.promise.Promises.newResultPromise;

/**
 * OpenID Connect Client Implementation that supports decryption and validation of user info responses in JWT format.
 */
public class AMOpenIDConnectClient extends OpenIDConnectClient {

    private static final Logger logger = LoggerFactory.getLogger(AMOpenIDConnectClient.class);

    private final SecretsProviderFacade secretsProvider;
    private final UserInfoResponseType userInfoResponseType;
    private final OpenIdResolverService resolverService;
    private final String issuer;

    /**
     * Constructs the AM-specific OpenID Connect Client.
     *
     * @param httpHandler Handler used to make Http calls to auth and resource servers.
     * @param config configuration that will be used to drive oauth flow.
     * @param clock time service.
     * @param random {@link SecureRandom} used to generate opaque, cryptographically secure strings.
     * @param resolverService {@link OpenIdResolverServiceImpl}. OpenID resolvers instance that also contains the
     *                        JwksStoreCache. By providing the same instance of resolvers for every OpenIdConnectClient
     *                        the same JwksCache will be used.
     * @param secretsProvider provider of AM client secrets.
     * @param amConfig the AM-specific OIDC client configuration
     * @param serviceConfigurator {@link OpenIdResolverServiceConfigurator}.
     */
    public AMOpenIDConnectClient(Handler httpHandler, OpenIDConnectClientConfiguration config, Clock clock,
            SecureRandom random, OpenIdResolverService resolverService,
            OpenIdResolverServiceConfigurator serviceConfigurator,
            OpenIDConnectClientConfig amConfig, SecretsProviderFacade secretsProvider) {
        super(httpHandler, config, clock, random, resolverService, serviceConfigurator);

        this.secretsProvider = secretsProvider;
        this.userInfoResponseType = amConfig.userInfoResponseType();
        this.issuer = amConfig.issuer();
        this.resolverService = resolverService;
    }

    @Override
    public Promise<UserInfo, OAuthException> getUserInfo(DataStore dataStore) {
        try {
            final JwtClaimsSet jwtClaims = getJwtClaimsSet(dataStore);
            if (getConfig().getUserInfoEndpoint() == null) {
                return newResultPromise(createUserInfoFromIdTokenJwtClaims(jwtClaims));
            }
            try {
                getAccessToken(dataStore.retrieveData());
            } catch (OAuthException e) {
                return newResultPromise(createUserInfoFromIdTokenJwtClaims(jwtClaims));
            }

            Request request = createRequestForUserInfoEndpoint(getAccessToken(dataStore.retrieveData()));
            return httpHandler.handle(new RootContext(), request)
                    .thenAlways(closeSilentlyAsync(request))
                    .thenAsync(closeSilently(handleUserInfoJwtResponse()), noopExceptionAsyncFunction())
                    .then(mapToUserInfo());
        } catch (OAuthException e) {
            return e.asPromise();
        }
    }

    private AsyncFunction<Response, JsonValue, OAuthException> handleUserInfoJwtResponse() {
        return response -> {
            if (!response.getStatus().isSuccessful()) {
                throw new OAuthException("Unable to process request: " + response.getEntity(), response.getCause());
            }

            switch (userInfoResponseType) {
            case SIGNED_THEN_ENCRYPTED_JWT:
                return handleSignedThenEncryptedJwtResponse(response);
            case SIGNED_JWT:
                return handleSignedJwtResponse(response);
            case JSON:
            default:
                return response.getEntity().getJsonAsync().then(JsonValue::json, e -> {
                    throw new OAuthException("Unable to process request: " + response.getEntity(), e);
                });
            }
        };
    }

    private Promise<JsonValue, OAuthException> handleSignedJwtResponse(Response response) throws OAuthException {
        try {
            SignedJwt signedJwt = new JwtReconstruction().reconstructJwt(response.getEntity().getString(),
                    SignedJwt.class);
            validateSignedJwt(signedJwt);
            return Promises.newResultPromise(signedJwt.getClaimsSet().toJsonValue());
        } catch (IOException | OpenIdConnectVerificationException | FailedToLoadJWKException | ClassCastException e) {
            throw new OAuthException("Unable to process request: " + response.getEntity(), e);
        }
    }

    private Promise<JsonValue, OAuthException> handleSignedThenEncryptedJwtResponse(Response response)
            throws OAuthException {
        try {
            SignedJwt signedJwt = decryptJwt(response.getEntity().getString());
            validateSignedJwt(signedJwt);
            return Promises.newResultPromise(signedJwt.getClaimsSet().toJsonValue());
        } catch (IOException | DecryptionFailedException | OpenIdConnectVerificationException
                | FailedToLoadJWKException | ClassCastException | InvalidJwtException e) {
            throw new OAuthException("Unable to process request: " + response.getEntity(), e);
        }
    }

    /**
     *  We cannot necessarily rely on the resolver service for decryption as the options for id token decryption and
     *  userinfo decryption are independently configurable. To rely on the the resolver service for decryption of
     *  userinfo would require that we had created a resolver that could handle decryption, which will only be the case
     *  if we expect id tokens to be encrypted. To be safe, lets handle decryption without the resolver, but use it for
     *  validating the SignedJwt.
     */
    private SignedJwt decryptJwt(String encryptedJwtString) throws DecryptionFailedException {
        EncryptedJwt encryptedJwt = new JwtReconstruction().reconstructJwt(encryptedJwtString, EncryptedJwt.class);
        JweHeader encryptedJwtHeader = encryptedJwt.getHeader();
        JwtEncryptionOptions jwtEncryptionOptions = new JwtEncryptionOptions(secretsProvider,
                encryptedJwtHeader.getAlgorithm(), encryptedJwtHeader.getEncryptionMethod());
        JwtDecryptionHandler jwtDecryptionHandler = new JwtDecryptionHandler(jwtEncryptionOptions);
        return jwtDecryptionHandler.decryptJwe(encryptedJwt, RP_ID_TOKEN_DECRYPTION);
    }

    /**
     *  We cannot rely on the resolver service for validation as the commons resolvers validate issuer and exp as part
     *  of the validation process, which the userinfo response jwts will not necessarily have. However, as long as the
     *  resolver is an {@link JWKOpenIdResolverImpl}, we can call verifySignature. JWKOpenIdResolverImpl is used in all
     *  OIDC client configurations, and we only support processing of jwts from userinfo for oidc clients, so we throw
     *  an exception if we attempt to validate a signed jwt where a JWKOpenIdResolverImpl is not configured.
     */
    private void validateSignedJwt(SignedJwt signedJwt) throws OpenIdConnectVerificationException,
            FailedToLoadJWKException, OAuthException {
        OpenIdResolver openIdResolver = resolverService.getResolverForIssuer(issuer).orElseThrow();
        if (openIdResolver instanceof JWKOpenIdResolverImpl) {
            ((JWKOpenIdResolverImpl) openIdResolver).verifySignature(signedJwt);
        } else {
            logger.warn(String.format("Unable to verify signature of user info response as resolver for issuer %s is "
                    + "not JWKOpenIdResolverImpl but %s", issuer, openIdResolver.getClass().toString()));
            throw new OAuthException("Unable to verify signature");
        }
    }
}
