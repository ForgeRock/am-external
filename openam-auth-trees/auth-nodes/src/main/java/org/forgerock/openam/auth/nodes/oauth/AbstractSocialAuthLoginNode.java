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

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toMap;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.Action.goTo;
import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.EMAIL_ADDRESS;
import static org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper.ATTRIBUTES_SHARED_STATE_KEY;
import static org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper.USER_INFO_SHARED_STATE_KEY;
import static org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper.USER_NAMES_SHARED_STATE_KEY;

import com.google.common.collect.ImmutableList;
import com.iplanet.am.util.SystemProperties;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.RedirectCallback;
import com.sun.identity.shared.Constants;
import java.net.URI;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.ResourceBundle;
import javax.security.auth.callback.Callback;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.oauth.DataStore;
import org.forgerock.oauth.OAuthClient;
import org.forgerock.oauth.OAuthException;
import org.forgerock.oauth.UserInfo;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.SharedStateConstants;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.util.i18n.PreferredLocales;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class serves as a base for social authentication login node.
 */
public abstract class AbstractSocialAuthLoginNode implements Node {
    private static final String BUNDLE = "org.forgerock.openam.auth.nodes.AbstractSocialAuthLoginNode";
    private static final String MIX_UP_MITIGATION_PARAM_CLIENT_ID = "client_id";
    private static final String MIX_UP_MITIGATION_PARAM_ISSUER = "iss";
    private static final String MAIL_KEY_MAPPING = "mail";

    private final AbstractSocialAuthLoginNode.Config config;
    private final ProfileNormalizer profileNormalizer;
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private final OAuthClient client;
    private final SocialOAuth2Helper authModuleHelper;

    /**
     * The interface Config.
     */
    public interface Config {

        /**
         * The client id.
         * @return the client id
         */
        String clientId();

        /**
         * The account povider class.
         * @return The account povider class.
         */
        String cfgAccountProviderClass();

        /**
         * The account mapper class.
         * @return the account mapper class.
         */
        String cfgAccountMapperClass();

        /**
         * The attribute mapping classes.
         * @return the attribute mapping classes.
         */
        Set<String> cfgAttributeMappingClasses();

        /**
         * The account mapper configuration.
         * @return the account mapper configuration.
         */
        Map<String, String> cfgAccountMapperConfiguration();

        /**
         * The attribute mapping configuration.
         * @return the attribute mapping configuration
         */
        Map<String, String> cfgAttributeMappingConfiguration();

        /**
         * Specifies if the user attributes must be saved in session.
         * @return true to save the user attribute into the session, false otherwise.
         */
        boolean saveUserAttributesToSession();

        /**
         * Specify if the mixup mitigation must be activated.
         * The mixup mitigation add an extra level of security by checking the client_id and iss coming from the
         * authorizeEndpoint response.
         *
         * @return true to activate it , false otherwise
         */
        default boolean cfgMixUpMitigation() {
            return false;
        }

        /**
         * The issuer. Must be specify to use the mixup mitigation.
         * @return the issuer.
         */
        default String issuer() {
            return null;
        }
    }

    /**
     * Constructs a new {@link AbstractSocialAuthLoginNode} with the provided
     * {@link AbstractSocialAuthLoginNode.Config}.
     *
     * @param config provides the settings for initialising an {@link AbstractSocialAuthLoginNode}.
     * @param authModuleHelper a social oauth2 helper.
     * @param client The oauth client to use. That's the client responsible to deal with the oauth workflow.
     * @param profileNormalizer User profile normaliser
     */
    public AbstractSocialAuthLoginNode(AbstractSocialAuthLoginNode.Config config, SocialOAuth2Helper authModuleHelper,
            OAuthClient client, ProfileNormalizer profileNormalizer) {
        this.config = config;
        this.authModuleHelper = authModuleHelper;
        this.client = client;
        this.profileNormalizer = profileNormalizer;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("Social auth node started");

        if (context.request.parameters.containsKey("code")) {
            logger.debug("the request parameters contains a code");
            return processOAuthTokenState(context);
        }

        DataStore dataStore = SharedStateAdaptor.toDatastore(json(context.sharedState));
        Callback callback = prepareRedirectCallback(dataStore);
        return send(callback)
                .replaceSharedState(SharedStateAdaptor.fromDatastore(dataStore))
                .build();
    }

    /**
     * Constructs the server URL using the AM server protocol, host, port and services deployment descriptor from
     * {@link SystemProperties}. If any of these properties are not available, an empty string is returned instead.
     *
     * @return The server URL.
     */
    protected static String getServerURL() {
        final String protocol = SystemProperties.get(Constants.AM_SERVER_PROTOCOL);
        final String host = SystemProperties.get(Constants.AM_SERVER_HOST);
        final String port = SystemProperties.get(Constants.AM_SERVER_PORT);
        final String descriptor = SystemProperties.get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);

        if (protocol != null && host != null && port != null && descriptor != null) {
            return protocol + "://" + host + ":" + port + descriptor;
        } else {
            return "";
        }
    }

    private Callback prepareRedirectCallback(DataStore dataStore) throws NodeProcessException {
        RedirectCallback redirectCallback;
        try {
            URI uri = client.getAuthRedirect(dataStore, null, null).getOrThrow();
            redirectCallback = new RedirectCallback(uri.toString(), null, "GET");
            redirectCallback.setTrackingCookie(true);
        } catch (InterruptedException | OAuthException e) {
            throw new NodeProcessException(e);
        }

        return redirectCallback;
    }

    /*
         1. Get the userInformation by calling the token endpoint to fetch the access token and then call the
         userEndpoint.
         2. Parse the user information with the mapping supplied in the configuration to populate two map
            2.1 attributes are the user information to add the profile
            2.2 userNames are the information used to look up a user.
         3. If a profile is found for the information in userNames then we return an ACCOUNT_EXISTS outcome otherwise
            we store the attributes and userNames in the sharedState and return a NO_ACCOUNT outcome

     */
    private Action processOAuthTokenState(TreeContext context) throws NodeProcessException {
        performMixUpMitigationProtectionCheck(context.request);

        Optional<String> user;
        Map<String, Set<String>> attributes;
        Map<String, Set<String>> userNames;
        try {
            UserInfo userInfo = getUserInfo(context);

            attributes = profileNormalizer.getNormalisedAttributes(userInfo, getJwtClaims(userInfo), config);
            userNames = profileNormalizer.getNormalisedAccountAttributes(userInfo, getJwtClaims(userInfo), config);

            addLogIfTooManyUsernames(userNames, userInfo);

            user = authModuleHelper.userExistsInTheDataStore(context.sharedState.get("realm").asString(),
                    profileNormalizer.getAccountProvider(config), userNames);
        } catch (AuthLoginException e) {
            throw new NodeProcessException(e);
        }

        return getAction(context, user, attributes, userNames);
    }

    /**
     * Making this protected allows other Social Nodes (specifically the Social OpenId Connect node) to provide their
     * own implementations.
     * @param userInfo The user information.
     * @return The jwt claims.
     */
    protected JwtClaimsSet getJwtClaims(UserInfo userInfo) {
        return null;
    }

    private void addLogIfTooManyUsernames(Map<String, Set<String>> userNames, UserInfo userInfo) {
        if (userNames.values().size() > 1) {
            if (logger.isWarnEnabled()) {
                String usernamesAsString = config.cfgAccountMapperConfiguration().entrySet()
                        .stream()
                        .map(entry -> entry.getKey() + " - " + entry.getValue())
                        .collect(Collectors.joining(", "));
                logger.warn("Multiple usernames have been found for the user information {} with your configuration "
                        + "mapping {}", userInfo.toString(), usernamesAsString);
            }
        }
    }

    private Action getAction(TreeContext context, Optional<String> user, Map<String,
            Set<String>> attributes, Map<String, Set<String>> userNames) {
        Action.ActionBuilder action;
        if (user.isPresent()) {
            logger.debug("The user {} already have an account. Go to {} outcome",
                    user.get(), SocialAuthOutcome.ACCOUNT_EXISTS.name());

            action = goTo(SocialAuthOutcome.ACCOUNT_EXISTS.name())
                    .replaceSharedState(context.sharedState.add(SharedStateConstants.USERNAME, user.get()));
        } else {
            logger.debug("The user doesn't have an account");

            JsonValue sharedState = context.sharedState.put(USER_INFO_SHARED_STATE_KEY,
                    json(object(
                            field(ATTRIBUTES_SHARED_STATE_KEY, convertToMapOfList(attributes)),
                            field(USER_NAMES_SHARED_STATE_KEY, convertToMapOfList(userNames))
                    )));

            if (attributes.get(MAIL_KEY_MAPPING) != null) {
                sharedState = sharedState.add(EMAIL_ADDRESS, attributes.get(MAIL_KEY_MAPPING).stream().findAny().get());
            } else {
                logger.debug("Unable to ascertain email address because the information is not available. "
                        + "It's possible you need to add a scope or that the configured provider does not have this "
                        + "information");
            }

            logger.debug("Go to " + SocialAuthOutcome.NO_ACCOUNT.name() + " outcome");
            action = goTo(SocialAuthOutcome.NO_ACCOUNT.name()).replaceSharedState(sharedState);
        }

        if (config.saveUserAttributesToSession()) {
            logger.debug("user attributes are going to be saved in the session");
            attributes.forEach((key, value) -> action.putSessionProperty(key, value.stream().findFirst().get()));
        }
        return action.build();
    }

    private void performMixUpMitigationProtectionCheck(ExternalRequestContext request) throws NodeProcessException {
        if (config.cfgMixUpMitigation()) {
            List<String> clientId = request.parameters.get(MIX_UP_MITIGATION_PARAM_CLIENT_ID);
            if (clientId == null || clientId.size() != 1) {
                throw new NodeProcessException("OAuth 2.0 mix-up mitigation is enabled, but the client_id has not been "
                        + "provided properly");
            }  else if (!config.clientId().equals(clientId.get(0))) {
                throw new NodeProcessException("OAuth 2.0 mix-up mitigation is enabled, but the provided client_id "
                        + clientId.get(0) + " does not belong to this client " + config.clientId());
            }

            List<String> issuer = request.parameters.get(MIX_UP_MITIGATION_PARAM_ISSUER);
            if (issuer == null || issuer.size() != 1) {
                throw new NodeProcessException("OAuth 2.0 mix-up mitigation is enabled, but the iss has not been "
                        + "provided properly");
            } else if (!config.issuer().equals(issuer.get(0))) {
                throw new NodeProcessException("OAuth 2.0 mix-up mitigation is enabled, but the provided iss "
                        + issuer.get(0) + " does not match the issuer in the client configuration " + config.issuer());
            }
        }
    }

    private Map<String, ArrayList<String>> convertToMapOfList(Map<String, Set<String>> mapToConvert) {
        return mapToConvert.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> new ArrayList<>(e.getValue())));
    }

    private UserInfo getUserInfo(TreeContext context) throws NodeProcessException {
        DataStore dataStore = SharedStateAdaptor.toDatastore(context.sharedState);
        try {
            if (!context.request.parameters.containsKey("state")) {
                throw new NodeProcessException("Not having the state could mean that this request did not come from "
                        + "the IDP");
            }
            HashMap<String, List<String>> parameters = new HashMap<>();
            parameters.put("state", singletonList(context.request.parameters.get("state").get(0)));
            parameters.put("code", singletonList(context.request.parameters.get("code").get(0)));

            logger.debug("fetching the access token ...");
            return client.handlePostAuth(dataStore, parameters)
                    .thenAsync(value -> {
                        logger.debug("Fetch user info from userInfo endpoint");
                        return client.getUserInfo(dataStore);
                    }).getOrThrowUninterruptibly();
        } catch (OAuthException e) {
            throw new NodeProcessException("Unable to get UserInfo details from provider", e);
        }
    }

    /**
     * The possible outcomes for the AbstractSocialAuthLoginNode.
     */
    public enum SocialAuthOutcome {
        /**
         * Successful authentication.
         */
        ACCOUNT_EXISTS,
        /**
         * Authentication failed.
         */
        NO_ACCOUNT
    }

    /**
     * Defines the possible outcomes from this node.
     */
    public static class SocialAuthOutcomeProvider implements OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(AbstractSocialAuthLoginNode.BUNDLE,
                    AbstractSocialAuthLoginNode.SocialAuthOutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(AbstractSocialAuthLoginNode.SocialAuthOutcome.ACCOUNT_EXISTS.name(),
                            bundle.getString("accountExistsOutcome")),
                    new Outcome(AbstractSocialAuthLoginNode.SocialAuthOutcome.NO_ACCOUNT.name(),
                            bundle.getString("noAccountOutcome")));
        }
    }
}
