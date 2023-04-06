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

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.EMAIL_ADDRESS;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.oauth.AbstractSocialAuthLoginNode.SocialAuthOutcome.NO_ACCOUNT;
import static org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper.DEFAULT_OAUTH2_SCOPE_DELIMITER;
import static org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper.USER_INFO_SHARED_STATE_KEY;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.forgerock.http.Handler;
import org.forgerock.oauth.OAuthClientConfiguration;
import org.forgerock.oauth.clients.oauth2.OAuth2ClientConfiguration;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.nodes.oauth.AbstractSocialAuthLoginNode;
import org.forgerock.openam.auth.nodes.oauth.ProfileNormalizer;
import org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.openam.sm.annotations.adapters.Password;
import org.forgerock.openam.sm.validation.URLValidator;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * The social facebook node. Contains pre-populated configuration for facebook.
 *
 *
 * @deprecated Use {@link org.forgerock.openam.auth.nodes.SelectIdPNode} and
 * {@link org.forgerock.openam.auth.nodes.SocialProviderHandlerNode} instead.
 */
@Deprecated
@Node.Metadata(outcomeProvider = AbstractSocialAuthLoginNode.SocialAuthOutcomeProvider.class,
        configClass = SocialFacebookNode.FacebookOAuth2Config.class,
        tags = {"social", "federation"})
public class SocialFacebookNode extends AbstractSocialAuthLoginNode {

    /**
     * Configuration with default values for facebook.
     */
    public interface FacebookOAuth2Config extends AbstractSocialAuthLoginNode.Config {

        /**
         * the client id.
         * @return teh client id
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
        default String authorizeEndpoint() {
            return "https://www.facebook.com/dialog/oauth";
        }

        /**
         * The token endpoint.
         * @return The token endpoint.
         */
        @Attribute(order = 400, validators = {RequiredValueValidator.class, URLValidator.class})
        default String tokenEndpoint() {
            return "https://graph.facebook.com/v2.12/oauth/access_token";
        }

        /**
         * The userinfo endpoint.
         * @return the userinfo endpoint.
         */
        @Attribute(order = 500, validators = {RequiredValueValidator.class, URLValidator.class})
        default String userInfoEndpoint() {
            return "https://graph.facebook.com/v2.6/me?fields=name%2Cemail%2Cfirst_name%2Clast_name";
        }

        /**
         * The scopes to request.
         * @return the scopes.
         */
        @Attribute(order = 600, validators = {RequiredValueValidator.class})
        default String scopeString() {
            return "public_profile,email";
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
        default String provider() {
            return "facebook";
        }

        /**
         * The authentication id key.
         * @return teh authentication id key.
         */
        @Attribute(order = 900, validators = {RequiredValueValidator.class})
        default String authenticationIdKey() {
            return "id";
        }

        /**
         * Tells if oauth2 must identify via basic header or not.
         * @return true to authenticate via basic header, false otherwise.
         */
        @Attribute(order = 1000)
        default boolean basicAuth() {
            return true;
        }

        /**
         * The account povider class.
         * @return The account povider class.
         */
        @Attribute(order = 1100)
        default String cfgAccountProviderClass() {
            return "org.forgerock.openam.authentication.modules.common.mapping.DefaultAccountProvider";
        }

        /**
         * The account mapper class.
         * @return the account mapper class.
         */
        @Attribute(order = 1200)
        default String cfgAccountMapperClass() {
            return "org.forgerock.openam.authentication.modules.common.mapping.JsonAttributeMapper|*|facebook-";
        }

        /**
         * The attribute mapping classes.
         * @return the attribute mapping classes.
         */
        @Attribute(order = 1300, validators = {RequiredValueValidator.class})
        default Set<String> cfgAttributeMappingClasses() {
            return singleton("org.forgerock.openam.authentication.modules.common.mapping."
                    + "JsonAttributeMapper|iplanet-am-user-alias-list|facebook-");
        }

        /**
         * The account mapper configuration.
         * @return teh account mapper configuration.
         */
        @Attribute(order = 1400, validators = {RequiredValueValidator.class})
        default Map<String, String> cfgAccountMapperConfiguration() {
            return singletonMap("id", "iplanet-am-user-alias-list");
        }

        /**
         * The attribute mapping configuration.
         * @return the attribute mapping configuration
         */
        @Attribute(order = 1500, validators = {RequiredValueValidator.class})
        default Map<String, String> cfgAttributeMappingConfiguration() {
            final Map<String, String> attributeMappingConfiguration = new HashMap<>();
            attributeMappingConfiguration.put("id", "iplanet-am-user-alias-list");
            attributeMappingConfiguration.put("first_name", "givenName");
            attributeMappingConfiguration.put("last_name", "sn");
            attributeMappingConfiguration.put("email", "mail");
            attributeMappingConfiguration.put("name", "cn");
            return attributeMappingConfiguration;
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
         * The issuer. Must be specify to use the mixup mitigation.
         * @return the issuer.
         */
        @Attribute(order = 1800)
        default String issuer() {
            return "";
        }
    }

    /**
     * Constructs a new {@link SocialFacebookNode} with the provided {@link Config}.
     *
     * @param handler http handler for client requests
     * @param config provides the settings for initialising an {@link SocialFacebookNode}.
     * @param authModuleHelper helper for oauth2
     * @param profileNormalizer User profile normaliser
     * @param identityService The identity service.
     * @param realm The relevant realm.
     * @param nodeId The UUID of the current authentication tree node.
     * @throws NodeProcessException if there is a problem during construction.
     */
    @Inject
    public SocialFacebookNode(@Named("CloseableHttpClientHandler") Handler handler,
            @Assisted FacebookOAuth2Config config, @Assisted Realm realm, SocialOAuth2Helper authModuleHelper,
            ProfileNormalizer profileNormalizer, LegacyIdentityService identityService, @Assisted UUID nodeId)
            throws NodeProcessException {
        super(config, authModuleHelper, authModuleHelper.newOAuthClient(realm, getOAuthClientConfiguration(config),
                handler), profileNormalizer, identityService, nodeId);
    }

    private static OAuthClientConfiguration getOAuthClientConfiguration(FacebookOAuth2Config config) {
        return OAuth2ClientConfiguration.oauth2ClientConfiguration()
                .withClientId(config.clientId())
                .withClientSecret(new String(config.clientSecret()))
                .withAuthorizationEndpoint(config.authorizeEndpoint())
                .withTokenEndpoint(config.tokenEndpoint())
                .withScope(Collections.singletonList(config.scopeString()))
                .withScopeDelimiter(DEFAULT_OAUTH2_SCOPE_DELIMITER)
                .withBasicAuth(config.basicAuth())
                .withUserInfoEndpoint(config.userInfoEndpoint())
                .withRedirectUri(URI.create(config.redirectURI()))
                .withProvider(config.provider())
                .withAuthenticationIdKey(config.authenticationIdKey()).build();
    }

    @Override
    public InputState[] getInputs() {
        return super.getInputs();
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
