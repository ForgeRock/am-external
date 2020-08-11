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
 * Copyright 2018-2020 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.EMAIL_ADDRESS;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.oauth.AbstractSocialAuthLoginNode.SocialAuthOutcome.NO_ACCOUNT;
import static org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper.DEFAULT_OAUTH2_SCOPE_DELIMITER;
import static org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper.USER_INFO_SHARED_STATE_KEY;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.oauth.OAuthClientConfiguration;
import org.forgerock.oauth.UserInfo;
import org.forgerock.oauth.clients.oidc.OpenIDConnectClientConfiguration;
import org.forgerock.oauth.clients.oidc.OpenIDConnectUserInfo;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.nodes.oauth.AbstractSocialAuthLoginNode;
import org.forgerock.openam.auth.nodes.oauth.ProfileNormalizer;
import org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.forgerock.openam.sm.annotations.adapters.Password;
import org.forgerock.openam.sm.validation.URLValidator;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * The social openid connect node. Contains pre-populated configuration for google.
 */
@Node.Metadata(outcomeProvider = AbstractSocialAuthLoginNode.SocialAuthOutcomeProvider.class,
        configClass = SocialOpenIdConnectNode.OpenIdConfig.class,
        tags = {"social", "federation"})
public class SocialOpenIdConnectNode extends AbstractSocialAuthLoginNode {

    /**
     * The node config with default values for openid connect.
     */
    public interface OpenIdConfig extends AbstractSocialAuthLoginNode.Config {
        /**
         * the client id.
         * @return the client id
         */
        @Attribute(order = 100, validators = {RequiredValueValidator.class})
        String clientId();

        /**
         * The client secret.
         * @return the client secret
         */
        @Attribute(order = 200, validators = {RequiredValueValidator.class})
        @Password
        char[] clientSecret();

        /**
         * The authorization endpoint.
         * @return The authorization endpoint.
         */
        @Attribute(order = 300, validators = {RequiredValueValidator.class, URLValidator.class})
        String authorizeEndpoint();

        /**
         * The token endpoint.
         * @return The token endpoint.
         */
        @Attribute(order = 400, validators = {RequiredValueValidator.class, URLValidator.class})
        String tokenEndpoint();

        /**
         * The userinfo endpoint.
         * @return the userinfo endpoint.
         */
        @Attribute(order = 500, validators = {URLValidator.class})
        String userInfoEndpoint();

        /**
         * The scopes to request.
         * @return the scopes.
         */
        @Attribute(order = 600, validators = {RequiredValueValidator.class})
        default String scopeString() {
            return "openid";
        }

        /**
         * The URI the AS will redirect to.
         * @return the redirect URI
         */
        @Attribute(order = 700, validators = {RequiredValueValidator.class, URLValidator.class})
        default String redirectURI() {
            return getServerURL();
        }

        /**
         * The provider. (useful if using IDM)
         * @return the provider.
         */
        @Attribute(order = 800)
        String provider();

        /**
         * The authentication id key.
         * @return the authentication id key.
         */
        @Attribute(order = 900, validators = {RequiredValueValidator.class})
        default String authenticationIdKey() {
            return "sub";
        }

        /**
         * Tells if OIDC client must identify via basic header or not.
         * @return true to authenticate via basic header, false otherwise.
         */
        @Attribute(order = 1000)
        default boolean basicAuth() {
            return true;
        }

        /**
         * The account provider class.
         * @return The account provider class.
         */
        @Attribute(order = 1100, validators = {RequiredValueValidator.class})
        default String cfgAccountProviderClass() {
            return "org.forgerock.openam.authentication.modules.common.mapping.DefaultAccountProvider";
        }

        /**
         * The account mapper class.
         * @return the account mapper class.
         */
        @Attribute(order = 1200, validators = {RequiredValueValidator.class})
        default String cfgAccountMapperClass() {
            return "org.forgerock.openam.authentication.modules.oidc.JwtAttributeMapper|*|openid-";
        }

        /**
         * The attribute mapping classes.
         * @return the attribute mapping classes.
         */
        @Attribute(order = 1300, validators = {RequiredValueValidator.class})
        default Set<String> cfgAttributeMappingClasses() {
            return singleton("org.forgerock.openam.authentication.modules.oidc"
                    + ".JwtAttributeMapper|iplanet-am-user-alias-list|openid-");
        }

        /**
         * The account mapper configuration.
         * @return the account mapper configuration.
         */
        @Attribute(order = 1400, validators = {RequiredValueValidator.class})
        default Map<String, String> cfgAccountMapperConfiguration() {
            return singletonMap("sub", "iplanet-am-user-alias-list");
        }

        /**
         * The attribute mapping configuration.
         * @return the attribute mapping configuration
         */
        @Attribute(order = 1500, validators = {RequiredValueValidator.class})
        default Map<String, String> cfgAttributeMappingConfiguration() {
            return singletonMap("sub", "iplanet-am-user-alias-list");
        }

        /**
         * Specifies if the user attributes must be saved in session.
         * @return true to save the user attribute into the session, false otherwise.
         */
        @Attribute(order = 1600)
        default boolean saveUserAttributesToSession() {
            return true;
        }

        /**
         * Specify if the mixup mitigation must be activated.
         * The mixup mitigation add an extra level of security by checking the client_id and iss coming from the
         * authorizeEndpoint response.
         *
         * @return true to activate it , false otherwise
         */
        @Attribute(order = 1700)
        default boolean cfgMixUpMitigation() {
            return false;
        }

        /**
         * The issuer. Must be specified to use mixup mitigation.
         * @return the issuer.
         */
        @Attribute(order = 1800)
        String issuer();

        /**
         * The openid connect validation method.
         * @return the openid connect validation method.
         */
        @Attribute(order = 1900, validators = {RequiredValueValidator.class})
        default OpenIDValidationMethod openIdValidationMethod() {
            return OpenIDValidationMethod.WELL_KNOWN_URL;
        }


        /**
         * The openid connect validation value.
         * @return the openid validation value.
         */
        @Attribute(order = 2000)
        String openIdValidationValue();

    }

    /**
     * Constructs a new {@link SocialOpenIdConnectNode} with the provided {@link Config}.
     *
     * @param config           provides the settings for initialising an {@link SocialGoogleNode}.
     * @param authModuleHelper helper for oauth2
     * @param profileNormalizer User profile normaliser
     * @param identityUtils The identity utils implementation.
     */
    @Inject
    public SocialOpenIdConnectNode(@Assisted OpenIdConfig config, SocialOAuth2Helper authModuleHelper,
                                   ProfileNormalizer profileNormalizer, IdentityUtils identityUtils) {
        super(config, authModuleHelper, authModuleHelper.newOAuthClient(getOAuthClientConfiguration(config)),
                profileNormalizer, identityUtils);
    }

    private static OAuthClientConfiguration getOAuthClientConfiguration(OpenIdConfig config) {
        OpenIDConnectClientConfiguration.Builder<?, OpenIDConnectClientConfiguration> builder =
                OpenIDConnectClientConfiguration.openIdConnectClientConfiguration();
        builder.withClientId(config.clientId())
                .withClientSecret(new String(config.clientSecret()))
                .withAuthorizationEndpoint(config.authorizeEndpoint())
                .withTokenEndpoint(config.tokenEndpoint())
                .withScope(Collections.singletonList(config.scopeString()))
                .withScopeDelimiter(DEFAULT_OAUTH2_SCOPE_DELIMITER)
                .withBasicAuth(config.basicAuth())
                .withUserInfoEndpoint(config.userInfoEndpoint())
                .withRedirectUri(URI.create(config.redirectURI()))
                .withProvider(config.provider())
                .withIssuer(config.issuer())
                .withAuthenticationIdKey(config.authenticationIdKey())
                .build();

        if (config.openIdValidationMethod().equals(OpenIDValidationMethod.JWK_URL)) {
            builder.withJwk(config.openIdValidationValue());
        } else if (config.openIdValidationMethod().equals(OpenIDValidationMethod.WELL_KNOWN_URL)) {
            builder.withWellKnownEndpoint(config.openIdValidationValue());
        }

        return builder.build();
    }

    /**
     * Overriding this method to return JWT claims if the user info is of type OpenIDConnectUserInfo.
     * @param userInfo The user information.
     * @return The jwt claims.
     */
    @Override
    protected JwtClaimsSet getJwtClaims(UserInfo userInfo) {
        return userInfo instanceof OpenIDConnectUserInfo ? ((OpenIDConnectUserInfo) userInfo).getJwtClaimsSet()
                : super.getJwtClaims(userInfo);
    }

    /**
     * Which way will the Open ID Connect node validate the ID token from the OpenID Connect provider.
     */
    public enum OpenIDValidationMethod {
        /**
         * Retrieve the provider keys based on the information provided in the OpenID Connect Provider Configuration
         * Document.
         */
        WELL_KNOWN_URL,
        /**
         * Use the client secret that you specify in the Client Secret property as the key to validate the ID token
         * signature according to the HMAC, using the client secret to the decrypt the hash and then checking that
         * the hash matches the hash of the ID token JWT.
         */
        CLIENT_SECRET,
        /**
         * Retrieve the provider's JSON web key set at the URL that you specify in the OpenID Connect validation
         * configuration value property.
         */
        JWK_URL
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[] {
            new InputState(REALM)
        };
    }

    @Override
    public OutputState[] getOutputs() {
        return new OutputState[] {
            new OutputState(USERNAME, singletonMap("ACCOUNT_EXISTS", true)),
            new OutputState(USER_INFO_SHARED_STATE_KEY, singletonMap(NO_ACCOUNT.toString(), true)),
            new OutputState(EMAIL_ADDRESS, singletonMap(NO_ACCOUNT.toString(), true))
        };
    }
}
