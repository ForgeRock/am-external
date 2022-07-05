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
 * Copyright 2018-2022 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.oauth;

import static com.iplanet.am.util.SecureRandomManager.getSecureRandom;
import static com.sun.identity.authentication.spi.AMLoginModule.getAMIdentityRepository;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.oauth.clients.oidc.JwtRequestParameterOption.REFERENCE;
import static org.forgerock.oauth.clients.oidc.JwtRequestParameterOption.VALUE;
import static org.forgerock.openam.oauth2.OAuth2Constants.ClientPurpose.RP_ID_TOKEN_DECRYPTION;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.REQUEST;
import static org.forgerock.openam.social.idp.ClientAuthenticationMethod.SELF_SIGNED_TLS_CLIENT_AUTH;
import static org.forgerock.openam.social.idp.ClientAuthenticationMethod.TLS_CLIENT_AUTH;
import static org.forgerock.openam.social.idp.UserInfoResponseType.SIGNED_JWT;
import static org.forgerock.openam.social.idp.UserInfoResponseType.SIGNED_THEN_ENCRYPTED_JWT;
import static org.forgerock.openam.utils.Time.getClock;

import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.client.utils.URIBuilder;
import org.forgerock.http.Handler;
import org.forgerock.oauth.DataStore;
import org.forgerock.oauth.OAuthClient;
import org.forgerock.oauth.OAuthClientConfiguration;
import org.forgerock.oauth.OAuthException;
import org.forgerock.oauth.clients.oauth2.OAuth2ClientConfiguration;
import org.forgerock.oauth.clients.oidc.JwtRequestParameterOption;
import org.forgerock.oauth.clients.oidc.OpenIDConnectClient;
import org.forgerock.oauth.clients.oidc.OpenIDConnectClientConfiguration;
import org.forgerock.oauth.resolvers.service.OpenIdResolverService;
import org.forgerock.oauth.resolvers.service.OpenIdResolverServiceConfigurator;
import org.forgerock.oauth.resolvers.service.OpenIdResolverServiceConfiguratorImpl;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.authentication.modules.common.mapping.AccountProvider;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.oauth2.requesturis.RequestUriObjectService;
import org.forgerock.openam.secrets.Secrets;
import org.forgerock.openam.services.baseurl.BaseURLProvider;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openam.services.baseurl.InvalidBaseUrlException;
import org.forgerock.openam.social.idp.ClientAuthenticationMethod;
import org.forgerock.openam.social.idp.OAuth2ClientConfig;
import org.forgerock.openam.social.idp.OAuthClientConfig;
import org.forgerock.openam.social.idp.OpenIDConnectClientConfig;
import org.forgerock.openam.social.idp.RealmBasedHttpClientHandlerFactory;
import org.forgerock.openam.social.idp.SocialIdpConfigMapper;
import org.forgerock.openidconnect.OpenIdResolverServiceFactory;
import org.forgerock.util.Reject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.shared.encode.Base64;

/**
 * This class provides helper functions for the social auth nodes.
 */
public class SocialOAuth2Helper {

    private static final Logger logger = LoggerFactory.getLogger(SocialOAuth2Helper.class);
    private static final int NONCE_NUM_BITS = 160;
    private static final int URI_MAX_LENGTH = 512;
    private static final String REQUEST_URI = "request_uri";
    private static final EnumSet<ClientAuthenticationMethod> MTLS_CLIENT_AUTHN_METHODS =
            EnumSet.of(TLS_CLIENT_AUTH, SELF_SIGNED_TLS_CLIENT_AUTH);

    /**
     * The scope delimiter for oauth2. This value exists because some provider may use different one
     * ( facebook used to use a comma for instance)
     */
    public static final String DEFAULT_OAUTH2_SCOPE_DELIMITER = " ";

    /**
     * Key to store and access the userinfo data in the shared state.
     */
    public static final String USER_INFO_SHARED_STATE_KEY = "userInfo";

    /**
     * Key to store and access the names data in the shared state.
     */
    public static final String USER_NAMES_SHARED_STATE_KEY = "userNames";

    /**
     * Key to store and access the user attributes data in the shared state.
     */
    public static final String ATTRIBUTES_SHARED_STATE_KEY = "attributes";

    private final OpenIdResolverServiceFactory openIdResolverServiceFactory;
    private final BaseURLProviderFactory baseURLProviderFactory;
    private final RequestUriObjectService requestUriObjectService;
    private final Secrets secrets;
    private final SocialIdpConfigMapper socialIdpConfigMapper;
    private final RealmBasedHttpClientHandlerFactory httpClientHandlerFactory;
    private final Handler defaultHandler;

    /**
     * Constructor.
     * @param openIdResolverServiceFactory factory to get the resolver service for the OAuthClient.
     * @param baseURLProviderFactory An instance of the BaseURLProviderFactory.
     * @param requestUriObjectService An instance of the RequestUriObjectService.
     * @param secrets provides api for obtaining secrets.
     * @param socialIdpConfigMapper the config mapper instance.
     * @param httpClientHandlerFactory the handler factory instance.
     * @param defaultHandler the default handler to use for the client.
     */
    @Inject
    public SocialOAuth2Helper(OpenIdResolverServiceFactory openIdResolverServiceFactory,
            BaseURLProviderFactory baseURLProviderFactory, RequestUriObjectService requestUriObjectService,
            Secrets secrets, SocialIdpConfigMapper socialIdpConfigMapper,
            @Named("KeyManagerBasedHandler") RealmBasedHttpClientHandlerFactory httpClientHandlerFactory,
            @Named("CloseableHttpClientHandler") Handler defaultHandler) {
        this.openIdResolverServiceFactory = openIdResolverServiceFactory;
        this.baseURLProviderFactory = baseURLProviderFactory;
        this.requestUriObjectService = requestUriObjectService;
        this.secrets = secrets;
        this.socialIdpConfigMapper = socialIdpConfigMapper;
        this.httpClientHandlerFactory = httpClientHandlerFactory;
        this.defaultHandler = defaultHandler;
    }

    /**
     * Create a new OAuthClient instance based on the OAuthClientConfiguration.
     *
     * @param realm The relevant Realm.
     * @param config The OAuthClientConfiguration instance.
     * @param handler The Http Handler for client requests.
     * @return The new OAuthClient instance.
     *
     * @deprecated Use overloaded method with {@link OAuthClientConfig}
     */
    @Deprecated
    public OAuthClient newOAuthClient(Realm realm, OAuthClientConfiguration config, Handler handler) {
        try {
            final Class<? extends OAuthClient> oauthClient =
                    (Class<? extends OAuthClient>) Class.forName(config.getClientClass().getName(), true,
                            getClass().getClassLoader());
            OAuthClient instance;
            //OpenIdConnectClient handles the JWKStore cache that needs to be provided during instantiation.
            //Other OAuthClient implementations don't have built in support for that.
            if (OpenIDConnectClient.class.isAssignableFrom(oauthClient)) {
                instance = new OpenIDConnectClient(handler, (OpenIDConnectClientConfiguration) config, getClock(),
                        getSecureRandom(), openIdResolverServiceFactory.create(realm, RP_ID_TOKEN_DECRYPTION),
                        new OpenIdResolverServiceConfiguratorImpl());
            } else {
                instance = oauthClient.getConstructor(Handler.class, config.getClass(), Clock.class, SecureRandom.class)
                        .newInstance(handler, config, getClock(), getSecureRandom());
            }
            return instance;
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot create an instance of oAuthClient", e);
        }
    }

    /**
     * Create a new OAuthClient instance based on the OAuthClientConfiguration and extra options that may only be
     * available in AMs OAuthClientConfig configuration.
     *
     * @param realm The relevant Realm.
     * @param idpConfig AM specific OAuth Client Configuration.
     * @return The new OAuthClient instance.
     */
    public OAuthClient newOAuthClient(Realm realm, OAuthClientConfig idpConfig) {
        try {
            OAuthClientConfiguration config = socialIdpConfigMapper.map(realm, idpConfig);
            Handler handler = getClientHandler(realm, idpConfig);
            final Class<? extends OAuthClient> oauthClient =
                    (Class<? extends OAuthClient>) Class.forName(config.getClientClass().getName(), true,
                            getClass().getClassLoader());
            OAuthClient instance;
            //OpenIdConnectClient handles the JWKStore cache that needs to be provided during instantiation.
            //Other OAuthClient implementations don't have built in support for that.
            if (OpenIDConnectClient.class.isAssignableFrom(oauthClient)) {

                if (userInfoSignedOrSignedAndEncrypted((OpenIDConnectClientConfig) idpConfig)) {
                    // Create the AMOpenIDConnectClient which is capable of decrypting the encrypted user info and
                    // verifying signed user info response.
                    instance = new AMOpenIDConnectClient(handler, (OpenIDConnectClientConfiguration) config, getClock(),
                            getSecureRandom(), openIdResolverServiceFactory.create(realm, RP_ID_TOKEN_DECRYPTION),
                            new OpenIdResolverServiceConfiguratorImpl(), (OpenIDConnectClientConfig) idpConfig,
                            secrets.getRealmSecrets(realm));
                } else {
                    instance = oauthClient.getConstructor(Handler.class, config.getClass(), Clock.class,
                                    SecureRandom.class, OpenIdResolverService.class,
                                    OpenIdResolverServiceConfigurator.class)
                            .newInstance(handler, config, getClock(), getSecureRandom(),
                                    openIdResolverServiceFactory.create(realm, RP_ID_TOKEN_DECRYPTION),
                                    new OpenIdResolverServiceConfiguratorImpl());
                }
            } else {
                instance = oauthClient.getConstructor(Handler.class, config.getClass(), Clock.class, SecureRandom.class)
                        .newInstance(handler, config, getClock(), getSecureRandom());
            }
            return instance;
        } catch (Exception e) {
            logger.error("Failed to create instance of OAuth client", e);
            throw new IllegalArgumentException("Cannot create an instance of oAuthClient", e);
        }
    }

    /**
     * Gets and existing user from the data store, based on the given criteria.
     *
     * @param realm The realm in which the user belongs.
     * @param accountProvider The provider class using the which the search will be performed.
     * @param userNames The name of the user.
     * @return The user name if exist in the data store.
     */
    public Optional<String> userExistsInTheDataStore(String realm, AccountProvider accountProvider,
            Map<String, Set<String>> userNames) {
        if (!userNames.isEmpty()) {
            final String user = getUser(realm, accountProvider, userNames);
            if (user != null) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    private String getUser(String realm, AccountProvider accountProvider,
            Map<String, Set<String>> userNames) {

        String user = null;
        if ((userNames != null) && !userNames.isEmpty()) {
            AMIdentity userIdentity = accountProvider.searchUser(
                    AuthD.getAuth().getAMIdentityRepository(realm), userNames);
            if (userIdentity != null) {
                user = userIdentity.getName();
            }
        }

        return user;
    }

    /**
     * Generates a string of 20 random bytes.
     * @return the Base64 encoded random string.
     * @throws NodeProcessException Thrown if an error occurs while generating a random string
     */
    public String getRandomData() throws NodeProcessException {
        byte[] pass = new byte[20];
        try {
            getSecureRandom().nextBytes(pass);
        } catch (Exception e) {
            throw new NodeProcessException("Error while generating random data", e);
        }
        return Base64.encode(pass);
    }

    /**
     * Provisions a user with the specified attributes.
     *
     * @param realm The realm.
     * @param accountProvider The account provider for creating the user.
     * @param attributes The user attributes.
     * @return The name of the created user.
     * @throws AuthLoginException If an error occurs creating the user.
     */
    public String provisionUser(String realm, AccountProvider accountProvider, Map<String, Set<String>> attributes)
            throws AuthLoginException {
        AMIdentity userIdentity = accountProvider.provisionUser(getAMIdentityRepository(realm), attributes);
        return userIdentity.getName();
    }

    /**
     * Converts a Map of List, of the kind returned by {@link org.forgerock.json.JsonValue} into a Map of Set.
     * @param mapOfList The source Map of List
     * @return The converted Map
     */
    public static Map<String, Set<String>> convertMapOfListToMapOfSet(Map<String, List<String>> mapOfList) {
        return mapOfList.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, p -> new HashSet<>(p.getValue())));
    }

    /**
     * Creates the request object and pass it to OP.
     *
     * @param servletRequest The http servlet request.
     * @param realm The realm where the oauth2 reliant party is configured.
     * @param oidcConfig The oidc client configuration.
     * @param dataStore The data store.
     * @throws NodeProcessException Thrown when request object can't be created or data store can't be updated.
     */
    public void passRequestObject(HttpServletRequest servletRequest,
            Realm realm, OpenIDConnectClientConfig oidcConfig,
            DataStore dataStore) throws NodeProcessException {
        try {
            if (oidcConfig.jwtRequestParameterOption() == REFERENCE) {
                logger.debug("request object reference sent to the social provider");
                String storedId = requestUriObjectService.createRequestObjectReference(oidcConfig);
                BaseURLProvider baseURLProvider = baseURLProviderFactory.get(realm.asPath());
                String realmOauthUrlStr = baseURLProvider
                        .getRealmURL(servletRequest, "/oauth2", realm).concat("/request_uri");
                URI requestUri = new URIBuilder(realmOauthUrlStr)
                        .addParameter("requestObjectId", storedId).build();
                Reject.ifTrue(requestUri.toString().length() > URI_MAX_LENGTH,
                        "Request URI MUST NOT exceed 512 ASCII characters as per OIDC specification");
                dataStore.storeData(json(object(field(REQUEST_URI, requestUri))));
            } else if (oidcConfig.jwtRequestParameterOption() == VALUE) {
                logger.debug("request object value sent to the social provider");
                dataStore.storeData(json(object(
                        field(REQUEST, requestUriObjectService.createRequestObject(oidcConfig, realm)))));
            }
        } catch (InvalidBaseUrlException | OAuthException | URISyntaxException e) {
            throw new NodeProcessException(e);
        }
    }

    /**
     * Checks if the OIDC Relying Party is configured to pass the request object to the OpenID Provider.
     *
     * @param idpConfig The idp client config.
     * @return true if the RP is configured to pass request object to OpenID Provider.
     */
    public boolean shouldPassRequestObject(OAuthClientConfig idpConfig) {
        if (idpConfig instanceof OpenIDConnectClientConfig) {
            JwtRequestParameterOption option = ((OpenIDConnectClientConfig) idpConfig).jwtRequestParameterOption();
            return option == REFERENCE || option == VALUE;
        }
        return false;
    }

    /**
     * Checks if the OIDC Relying Party is configured to generate Nonce for Native Client.
     * If configured, generate the Nonce and store to the provided dataStore
     *
     * @param idpConfig The idp client config.
     * @param dataStore The data store.
     * @throws NodeProcessException Thrown when data store can't be updated.
     */
    public void createNonce(OAuthClientConfig idpConfig, DataStore dataStore) throws NodeProcessException {
        if (idpConfig instanceof OpenIDConnectClientConfig
                && ((OpenIDConnectClientConfig) idpConfig).enableNativeNonce()) {
            try {
                dataStore.storeData(json(object(
                        field(OpenIDConnectClient.NONCE, createNonce()))));
            } catch (OAuthException e) {
                throw new NodeProcessException(e);
            }
        }
    }

    private String createNonce() {
        return new BigInteger(NONCE_NUM_BITS, getSecureRandom()).toString(Character.MAX_RADIX);
    }

    private Handler getClientHandler(Realm realm, OAuthClientConfig config) {
        if (OAuth2ClientConfiguration.class.isAssignableFrom(config.clientImplementation())
                && MTLS_CLIENT_AUTHN_METHODS.contains(((OAuth2ClientConfig) config).clientAuthenticationMethod())) {
            return httpClientHandlerFactory.create(realm);
        }

        return defaultHandler;
    }

    private boolean userInfoSignedOrSignedAndEncrypted(OpenIDConnectClientConfig idpConfig) {
        return idpConfig.userInfoResponseType() == SIGNED_JWT
                || idpConfig.userInfoResponseType() == SIGNED_THEN_ENCRYPTED_JWT;
    }
}
