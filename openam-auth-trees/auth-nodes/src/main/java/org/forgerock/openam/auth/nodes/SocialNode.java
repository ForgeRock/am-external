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

import static java.util.Collections.singletonMap;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.EMAIL_ADDRESS;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.oauth.AbstractSocialAuthLoginNode.SocialAuthOutcome.NO_ACCOUNT;
import static org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper.USER_INFO_SHARED_STATE_KEY;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

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
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.forgerock.openam.sm.annotations.adapters.Password;
import org.forgerock.openam.sm.validation.URLValidator;

import com.google.inject.assistedinject.Assisted;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * A social node class is used to configure a social oauth2 that respects the oauth2 specification.
 */
@Node.Metadata(outcomeProvider = AbstractSocialAuthLoginNode.SocialAuthOutcomeProvider.class,
        configClass = SocialNode.Config.class,
        tags = {"social", "federation"})
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
     * @param identityUtils The identity utils implementation.
     * @throws NodeProcessException if there is a problem during construction.
     */
    @Inject
    public SocialNode(@Assisted SocialNode.Config config, SocialOAuth2Helper authModuleHelper,
            ProfileNormalizer profileNormalizer, IdentityUtils identityUtils) throws NodeProcessException {
        super(config, authModuleHelper, authModuleHelper.newOAuthClient(getOAuthClientConfiguration(config)),
                profileNormalizer, identityUtils);
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
