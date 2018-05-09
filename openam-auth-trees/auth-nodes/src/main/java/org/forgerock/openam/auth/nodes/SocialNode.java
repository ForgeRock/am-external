/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.forgerock.oauth.OAuthClientConfiguration;
import org.forgerock.oauth.clients.oauth2.OAuth2ClientConfiguration;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.nodes.oauth.AbstractSocialAuthLoginNode;
import org.forgerock.openam.auth.nodes.oauth.ProfileNormalizer;
import org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper;
import org.forgerock.openam.sm.annotations.adapters.Password;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;
import org.forgerock.openam.sm.validation.URLValidator;

/**
 * A social node class is used to configure a social oauth2 that respects the oauth2 specification.
 */
@Node.Metadata(outcomeProvider = AbstractSocialAuthLoginNode.SocialAuthOutcomeProvider.class,
        configClass = SocialNode.Config.class)
public class SocialNode extends AbstractSocialAuthLoginNode {

    /**
     * The interface Config.
     */
    public interface Config extends AbstractSocialAuthLoginNode.Config {

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
        @Attribute(order = 500, validators = {RequiredValueValidator.class, URLValidator.class})
        String userInfoEndpoint();

        /**
         * The scopes to request.
         * @return the scopes.
         */
        @Attribute(order = 600, validators = {RequiredValueValidator.class})
        String scopeString();

        /**
         * The scope delimiter.
         * @return the scope delimiter.
         */
        @Attribute(order = 700, validators = {RequiredValueValidator.class})
        String scopeDelimiter();

        /**
         * The URI the AS will redirect to.
         * @return the redirect URI
         */
        @Attribute(order = 800, validators = {RequiredValueValidator.class, URLValidator.class})
        String redirectURI();

        /**
         * The provider. (useful if using IDM)
         * @return the provider.
         */
        @Attribute(order = 900)
        String provider();

        /**
         * The authentication id key.
         * @return teh authentication id key.
         */
        @Attribute(order = 1000, validators = {RequiredValueValidator.class})
        String authenticationIdKey();

        /**
         * Tells if oauth2 must identify via basic header or not.
         * @return true to authenticate via basic header, false otherwise.
         */
        @Attribute(order = 1100)
        boolean basicAuth();

        /**
         * The account povider class.
         * @return The account povider class.
         */
        @Attribute(order = 1200, validators = {RequiredValueValidator.class})
        String cfgAccountProviderClass();

        /**
         * The account mapper class.
         * @return the account mapper class.
         */
        @Attribute(order = 1300, validators = {RequiredValueValidator.class})
        String cfgAccountMapperClass();

        /**
         * The attribute mapping classes.
         * @return the attribute mapping classes.
         */
        @Attribute(order = 1400, validators = {RequiredValueValidator.class})
        Set<String> cfgAttributeMappingClasses();

        /**
         * The account mapper configuration.
         * @return the account mapper configuration.
         */
        @Attribute(order = 1500, validators = {RequiredValueValidator.class})
        Map<String, String> cfgAccountMapperConfiguration();


        /**
         * The attribute mapping configuration.
         * @return the attribute mapping configuration
         */
        @Attribute(order = 1600, validators = {RequiredValueValidator.class})
        Map<String, String> cfgAttributeMappingConfiguration();

        /**
         * Specifies if the user attributes must be saved in session.
         * @return true to save the user attribute into the session, false otherwise.
         */
        @Attribute(order = 1700)
        boolean saveUserAttributesToSession();

        /**
         * Specify if the mixup mitigation must be activated.
         * The mixup mitigation add an extra level of security by checking the client_id and iss coming from the
         * authorizeEndpoint response.
         *
         * @return true to activate it , false otherwise
         */
        @Attribute(order = 1800)
        default boolean cfgMixUpMitigation() {
            return false;
        }

        /**
         * The issuer. Must be specify to use the mixup mitigation.
         * @return the issuer.
         */
        @Attribute(order = 1900)
        default String issuer() {
            return "";
        }
    }

    /**
     * Constructs a new {@code SocialNode} with the provided {@code AbstractSocialAuthLoginNode.Config}.
     *
     * @param config provides the settings for initialising an {@code SocialNode}.
     * @param authModuleHelper helper for oauth2
     * @param profileNormalizer User profile normaliser
     * @throws NodeProcessException if there is a problem during construction.
     */
    @Inject
    public SocialNode(@Assisted SocialNode.Config config, SocialOAuth2Helper authModuleHelper,
            ProfileNormalizer profileNormalizer) throws NodeProcessException {
        super(config, authModuleHelper, authModuleHelper.newOAuthClient(getOAuthClientConfiguration(config)),
                profileNormalizer);
    }

    private static OAuthClientConfiguration getOAuthClientConfiguration(SocialNode.Config config) {
        return OAuth2ClientConfiguration.oauth2ClientConfiguration()
                .withClientId(config.clientId())
                .withClientSecret(new String(config.clientSecret()))
                .withAuthorizationEndpoint(config.authorizeEndpoint())
                .withTokenEndpoint(config.tokenEndpoint())
                .withScope(Collections.singletonList(config.scopeString()))
                .withScopeDelimiter(config.scopeDelimiter())
                .withBasicAuth(config.basicAuth())
                .withUserInfoEndpoint(config.userInfoEndpoint())
                .withRedirectUri(URI.create(config.redirectURI()))
                .withProvider(config.provider())
                .withAuthenticationIdKey(config.authenticationIdKey()).build();
    }
}
