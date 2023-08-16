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
 * Copyright 2020-2021 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static com.sun.identity.idm.IdType.USER;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.forgerock.json.JsonPointer.ptr;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.oauth.clients.twitter.TwitterClient.OAUTH_TOKEN;
import static org.forgerock.oauth.clients.twitter.TwitterClient.OAUTH_VERIFIER;
import static org.forgerock.openam.auth.node.api.Action.goTo;
import static org.forgerock.openam.auth.node.api.Action.send;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.ClientType.NATIVE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.IDPS;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.SELECTED_IDP;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.CODE;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.REQUEST;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.REQUEST_URI;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.STATE;
import static org.forgerock.openam.scripting.ScriptConstants.SOCIAL_IDP_PROFILE_TRANSFORMATION_NAME;
import static org.forgerock.openam.scripting.ScriptContext.SOCIAL_IDP_PROFILE_TRANSFORMATION;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import javax.security.auth.callback.Callback;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.forgerock.guava.common.collect.ListMultimap;
import org.forgerock.json.JsonValue;
import org.forgerock.oauth.DataStore;
import org.forgerock.oauth.OAuthClient;
import org.forgerock.oauth.OAuthException;
import org.forgerock.oauth.UserInfo;
import org.forgerock.oauth.clients.oauth2.OAuth2Client;
import org.forgerock.oauth.clients.oidc.OpenIDConnectClient;
import org.forgerock.oauth.clients.twitter.TwitterClient;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.OutcomeProvider;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.helpers.IdmIntegrationHelper;
import org.forgerock.openam.auth.nodes.oauth.SharedStateAdaptor;
import org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper;
import org.forgerock.openam.authentication.callbacks.IdPCallback;
import org.forgerock.openam.authentication.modules.common.mapping.DefaultAccountProvider;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.forgerock.openam.scripting.Script;
import org.forgerock.openam.scripting.ScriptEvaluator;
import org.forgerock.openam.scripting.ScriptObject;
import org.forgerock.openam.scripting.SupportedScriptingLanguage;
import org.forgerock.openam.scripting.service.ScriptConfiguration;
import org.forgerock.openam.session.Session;
import org.forgerock.openam.social.idp.OAuthClientConfig;
import org.forgerock.openam.social.idp.OpenIDConnectClientConfig;
import org.forgerock.openam.social.idp.SocialIdentityProviders;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.i18n.PreferredLocales;
import org.mozilla.javascript.NativeJavaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.authentication.spi.RedirectCallback;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.RequiredValueValidator;

/**
 * Redirects user to a social identity provider, handles post-auth, fetches and normalizes social userInfo and
 * determines whether this user has an existing AM account.
 */
@Node.Metadata(outcomeProvider = SocialProviderHandlerNode.SocialAuthOutcomeProvider.class,
        configClass = SocialProviderHandlerNode.Config.class,
        tags = {"social", "federation", "platform"})
public class SocialProviderHandlerNode implements Node {
    private static final String BUNDLE = "org.forgerock.openam.auth.nodes.SocialProviderHandlerNode";
    private static final String HEADERS_IDENTIFIER = "requestHeaders";
    private static final String EXISTING_SESSION = "existingSession";
    private static final String RAW_PROFILE_DATA = "rawProfile";
    private static final String NORMALIZED_PROFILE_DATA = "normalizedProfile";
    private static final String SHARED_STATE_IDENTIFIER = "sharedState";
    private static final String TRANSIENT_STATE_IDENTIFIER = "transientState";
    private static final String LOGGER_VARIABLE_NAME = "logger";
    private static final String REALM_IDENTIFIER = "realm";
    private static final String CALLBACKS_IDENTIFIER = "callbacks";
    private static final String QUERY_PARAMETER_IDENTIFIER = "requestParameters";
    private static final String AM_USER_ALIAS_LIST_ATTRIBUTE_NAME = "iplanet-am-user-alias-list";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String FORM_POST_ENTRY = "form_post_entry";

    static {
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    static final String SOCIAL_OAUTH_DATA = "socialOAuthData";
    static final String ALIAS_LIST = "aliasList";

    private final Logger logger = LoggerFactory.getLogger(SocialProviderHandlerNode.class);
    private final SocialOAuth2Helper authModuleHelper;
    private final SocialIdentityProviders providerConfigStore;
    private final IdentityUtils identityUtils;
    private final Realm realm;
    private final ScriptEvaluator scriptEvaluator;
    private final Provider<SessionService> sessionServiceProvider;
    private final Config config;
    private final IdmIntegrationService idmIntegrationService;

    /**
     * Configuration holder for the node.
     */
    public interface Config {
        /**
         * The script configuration for transforming the normalized social profile to OBJECT_ATTRIBUTES data in
         * the shared state.
         *
         * @return The script configuration
         */
        @Attribute(order = 100, validators = {RequiredValueValidator.class})
        @Script(SOCIAL_IDP_PROFILE_TRANSFORMATION_NAME)
        ScriptConfiguration script();

        /**
         * The attribute in which username may be found.
         *
         * @return the attribute
         */
        @Attribute(order = 300, validators = {RequiredValueValidator.class})
        default String usernameAttribute() {
            return DEFAULT_IDM_IDENTITY_ATTRIBUTE;
        }

        /**
         * The client type used to authenticate with the identity provider.
         * Default to {@link ClientType#BROWSER}
         *
         * @return The client type
         */
        @Attribute(order = 400)
        default ClientType clientType() {
            return ClientType.BROWSER;
        }
    }

    /**
     * Constructor.
     *
     * @param config node configuration instance
     * @param authModuleHelper helper for oauth2
     * @param providerConfigStore service containing social provider configurations
     * @param identityUtils an instance of the IdentityUtils
     * @param realm the realm context
     * @param scriptEvaluator service to execute script
     * @param sessionServiceProvider  provider of the session service
     * @param idmIntegrationService service that provides connectivity to IDM
     */
    @Inject
    public SocialProviderHandlerNode(@Assisted Config config,
            SocialOAuth2Helper authModuleHelper,
            SocialIdentityProviders providerConfigStore,
            IdentityUtils identityUtils,
            @Assisted Realm realm,
            @Named(SOCIAL_IDP_PROFILE_TRANSFORMATION_NAME) ScriptEvaluator scriptEvaluator,
            Provider<SessionService> sessionServiceProvider,
            IdmIntegrationService idmIntegrationService) {
        this.config = config;
        this.authModuleHelper = authModuleHelper;
        this.providerConfigStore = providerConfigStore;
        this.identityUtils = identityUtils;
        this.realm = realm;
        this.scriptEvaluator = scriptEvaluator;
        this.sessionServiceProvider = sessionServiceProvider;
        this.idmIntegrationService = idmIntegrationService;
    }

    @Override
    public Action process(TreeContext context) throws NodeProcessException {
        logger.debug("Social provider redirect node started");

        if (!context.sharedState.isDefined(SELECTED_IDP)) {
            throw new NodeProcessException(SELECTED_IDP + " not found in state");
        }
        final String selectedIdp = context.sharedState.get(SELECTED_IDP).asString();
        final OAuthClientConfig idpConfig = Optional.ofNullable(providerConfigStore.getProviders(realm)
                .get(selectedIdp))
                .orElseThrow(() -> new NodeProcessException("Selected provider does not exist."));
        final OAuthClient client = authModuleHelper.newOAuthClient(realm, idpConfig);
        final DataStore dataStore = SharedStateAdaptor.toDatastore(json(context.sharedState));

        Action action = handleCallback(context, selectedIdp, idpConfig, client, dataStore);
        if (action != null) {
            return action;
        }

        if (authModuleHelper.shouldPassRequestObject(idpConfig)) {
            authModuleHelper.passRequestObject(context.request.servletRequest, realm,
                    (OpenIDConnectClientConfig) idpConfig, dataStore);
        }

        if (config.clientType() == NATIVE) {
            logger.debug("Sending Social Login callback");
            return send(prepareIdPCallback(idpConfig, dataStore)).build();
        }
        logger.debug("Sending redirect callback");
        return send(prepareRedirectCallback(client, dataStore)).build();
    }

    private Action handleCallback(TreeContext context, String selectedIdp, OAuthClientConfig idpConfig,
                             OAuthClient client, DataStore dataStore) throws NodeProcessException {
        if (config.clientType() == NATIVE) {
            //Handle IdPCallback
            return handleIdPCallback(context, client, idpConfig, selectedIdp, dataStore);
        }
        //Handle redirect from idp.
        return handleRedirect(context, client, idpConfig, selectedIdp, dataStore);
    }

    private Action handleIdPCallback(TreeContext context, OAuthClient client,
            OAuthClientConfig idpConfig, String selectedIdp,
            DataStore dataStore) throws NodeProcessException {

        Optional<IdPCallback> opt = context.getCallback(IdPCallback.class);
        if (opt.isPresent()) {
            IdPCallback callback = opt.get();
            final HashMap<String, List<String>> parameters = new HashMap<>();

            parameters.put(OAuth2Client.TOKEN_TYPE, Collections.singletonList(callback.getTokenType()));
            if (callback.getToken().equals(FORM_POST_ENTRY)) {
                parameters.put(callback.getTokenType(), context.request.parameters.remove(CODE));
            } else {
                parameters.put(callback.getTokenType(), Collections.singletonList(callback.getToken()));
            }

            try {
                client.handleNativePostAuth(null, dataStore, parameters).getOrThrow();
                return handleUser(context, client, idpConfig, selectedIdp, dataStore);
            } catch (OAuthException e) {
                logger.error("Failed to handle native post auth", e);
                throw new NodeProcessException(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new NodeProcessException(e);
            }
        }
        return null;
    }

    private Action handleRedirect(TreeContext context, OAuthClient client,
            OAuthClientConfig idpConfig, String selectedIdp,
            DataStore dataStore) throws NodeProcessException {

        if (isAllRequiredParametersPresent(client, context.request.parameters)) {

            final HashMap<String, List<String>> parameters = new HashMap<>();
            parameters.put(STATE, context.request.parameters.remove(STATE));
            parameters.put(CODE, context.request.parameters.remove(CODE));
            parameters.put(OAUTH_TOKEN, context.request.parameters.get(OAUTH_TOKEN));
            parameters.put(OAUTH_VERIFIER, context.request.parameters.get(OAUTH_VERIFIER));

            try {
                client.handlePostAuth(dataStore, parameters).getOrThrow();
                logger.debug("Social provider redirect node completed");

                return handleUser(context, client, idpConfig, selectedIdp, dataStore);

            } catch (OAuthException e) {
                logger.error("Failed to handle post auth", e);
                throw new NodeProcessException(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new NodeProcessException(e);
            }
        }
        return null;
    }

    private boolean isAllRequiredParametersPresent(OAuthClient client, Map<String, List<String>> parameters)
            throws NodeProcessException {
        if (client instanceof TwitterClient) {
            return parameters.containsKey(OAUTH_TOKEN) && parameters.containsKey(OAUTH_VERIFIER);
        } else {
            if (parameters.containsKey(CODE)) {
                logger.debug("User agent returned from social authorization server with a code parameter");
                if (!parameters.containsKey(STATE)) {
                    logger.debug("Request contained a code parameter but did not include a state parameter");
                    throw new NodeProcessException("Not having the state could mean that this request did not come from"
                            + " the IDP");
                }
                return true;
            }
        }
        return false;
    }

    private Action handleUser(TreeContext context, OAuthClient client,
            OAuthClientConfig idpConfig, String selectedIdp,
            DataStore dataStore) throws NodeProcessException, OAuthException {
        // Fetch the social profile from the IdP
        UserInfo profile = getUserInfo(client, dataStore);

        // Normalize the social profile using the normalizer transform from the client's config
        JsonValue normalized = evaluateScript(context, idpConfig.transform(),
                RAW_PROFILE_DATA, profile.getRawProfile());

        // Transform the normalized profile to object data using the configured script
        JsonValue objectData = evaluateScript(context, config.script(), NORMALIZED_PROFILE_DATA, normalized);

        // Store the profile in OBJECT_ATTRIBUTES
        for (Map.Entry<String, Object> entry : objectData.asMap().entrySet()) {
            if (!entry.getKey().equals(config.usernameAttribute())) {
                idmIntegrationService.storeAttributeInState(context.transientState,
                        entry.getKey(), entry.getValue());
            }
        }
        // Record the social identity subject in the profile, too
        String identity = selectedIdp + "-" + profile.getSubject();
        Optional<String> contextId = idmIntegrationService.getAttributeFromContext(context,
                config.usernameAttribute())
                .map(JsonValue::asString);
        Optional<JsonValue> user = getUser(context, identity);

        String resolvedId;
        if (contextId.isPresent()) {
            if (user.isPresent()
                    && !contextId.get().equals(user.get().get(config.usernameAttribute()).asString())) {
                throw new NodeProcessException("Account does not belong to user in share state.");
            }
            resolvedId = contextId.get();
        } else {
            resolvedId = user.isPresent()
                    ? user.get().get(config.usernameAttribute()).asString()
                    : objectData.get(config.usernameAttribute()).asString();
            idmIntegrationService.storeAttributeInState(context.sharedState, config.usernameAttribute(),
                    resolvedId);
        }

        if (resolvedId != null) {
            context.sharedState.put(USERNAME, resolvedId);
        }

        if (idmIntegrationService.isEnabled()) {
            idmIntegrationService.storeAttributeInState(context.transientState, ALIAS_LIST,
                    getAliasList(context, identity, user, contextId));
        }

        Optional<String> universalId = identityUtils.getUniversalId(resolvedId, realm.asPath(), USER);

        return goTo(user.isPresent()
                ? SocialAuthOutcome.ACCOUNT_EXISTS.name()
                : SocialAuthOutcome.NO_ACCOUNT.name())
                .withUniversalId(universalId)
                .replaceSharedState(context.sharedState.copy())
                .replaceTransientState(context.transientState.copy()
                        .putPermissive(ptr(SOCIAL_OAUTH_DATA).child(selectedIdp), dataStore.retrieveData()))
                .build();
    }

    private Optional<JsonValue> getUser(TreeContext context, String identity) throws NodeProcessException {
        if (idmIntegrationService.isEnabled()) {
            Optional<JsonValue> user = IdmIntegrationHelper.getObject(idmIntegrationService, realm,
                    context.request.locales, context.identityResource, ALIAS_LIST, Optional.of(identity),
                    config.usernameAttribute(), ALIAS_LIST);
            return user;
        } else {
            AMIdentity amIdentity = new DefaultAccountProvider().searchUser(
                    AuthD.getAuth().getAMIdentityRepository(realm.asDN()),
                    singletonMap(AM_USER_ALIAS_LIST_ATTRIBUTE_NAME, singleton(identity)));
            return Optional.ofNullable(amIdentity)
                    .map(id -> json(object(field(config.usernameAttribute(), id.getName()))));
        }
    }

    private UserInfo getUserInfo(OAuthClient client, DataStore dataStore) throws NodeProcessException {
        try {
            return client.getUserInfo(dataStore).getOrThrow();
        } catch (OAuthException e) {
            logger.debug("Failed to retrieve social profile data", e);
            throw new NodeProcessException("Failed to retrieve social profile data", e);
        } catch (InterruptedException e) {
            logger.debug("Interrupted while retrieving social profile data");
            Thread.currentThread().interrupt();
            throw new NodeProcessException("Process interrupted", e);
        }
    }

    private Callback prepareRedirectCallback(OAuthClient client,
            DataStore dataStore)
            throws NodeProcessException {

        RedirectCallback redirectCallback;
        try {
            URI uri = client.getAuthRedirect(dataStore, null, null).getOrThrow();
            redirectCallback = new RedirectCallback(uri.toString(), null, "GET");
            redirectCallback.setTrackingCookie(true);
        } catch (OAuthException e) {
            throw new NodeProcessException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NodeProcessException(e);
        }

        return redirectCallback;
    }

    private Callback prepareIdPCallback(OAuthClientConfig idpConfig, DataStore dataStore)
            throws NodeProcessException {

        IdPCallback callback = MAPPER.convertValue(idpConfig, IdPCallback.class);
        authModuleHelper.createNonce(idpConfig, dataStore);
        //populate other parameters if exists in the dataStore
        try {
            callback.setRequest(dataStore.retrieveData().get(REQUEST).asString());
            if (dataStore.retrieveData().isDefined(REQUEST_URI)) {
                callback.setRequestUri(dataStore.retrieveData().get(REQUEST_URI).toString());
            }
            callback.setNonce(dataStore.retrieveData().get(OpenIDConnectClient.NONCE).asString());
        } catch (OAuthException e) {
            throw new NodeProcessException(e);
        }
        return callback;
    }

    private JsonValue evaluateScript(TreeContext context, ScriptConfiguration scriptConfig,
            String inputKey, JsonValue inputData) throws NodeProcessException {
        try {
            ScriptObject script = new ScriptObject(scriptConfig.getName(), scriptConfig.getScript(),
                    scriptConfig.getLanguage());
            Bindings binding = new SimpleBindings();
            binding.put(SHARED_STATE_IDENTIFIER, context.sharedState.getObject());
            binding.put(TRANSIENT_STATE_IDENTIFIER, context.transientState.getObject());
            binding.put(CALLBACKS_IDENTIFIER, context.getAllCallbacks());
            binding.put(HEADERS_IDENTIFIER, convertHeadersToModifiableObjects(context.request.headers));
            binding.put(LOGGER_VARIABLE_NAME,
                    Debug.getInstance("scripts." + SOCIAL_IDP_PROFILE_TRANSFORMATION.name()
                            + "." + scriptConfig.getId()));
            binding.put(REALM_IDENTIFIER, realm.asPath());
            if (!StringUtils.isEmpty(context.request.ssoTokenId)) {
                binding.put(EXISTING_SESSION, getSessionProperties(context.request.ssoTokenId));
            }
            binding.put(QUERY_PARAMETER_IDENTIFIER,
                    convertParametersToModifiableObjects(context.request.parameters));
            binding.put(SELECTED_IDP, context.sharedState.get(SELECTED_IDP).asString());
            binding.put(inputKey, inputData);
            JsonValue output = evaluateScript(script, binding);
            logger.debug("script {} \n binding {}", script, binding);

            return output;
        } catch (javax.script.ScriptException e) {
            logger.warn("Error evaluating the script", e);
            throw new NodeProcessException(e);
        }
    }

    private JsonValue evaluateScript(ScriptObject script, Bindings binding) throws javax.script.ScriptException {
        if (script.getLanguage().equals(SupportedScriptingLanguage.JAVASCRIPT)) {
            NativeJavaObject result = scriptEvaluator.evaluateScript(script, binding);
            return (JsonValue) result.unwrap();
        } else {
            return scriptEvaluator.evaluateScript(script, binding);
        }
    }

    /**
     * The request headers are unmodifiable, this prevents them being converted into javascript. This method
     * iterates the underlying collections, adding the values to modifiable collections.
     *
     * @param input the headers
     * @return the headers in modifiable collections
     */
    private Map<String, List<String>> convertHeadersToModifiableObjects(ListMultimap<String, String> input) {
        Map<String, List<String>> mapCopy = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String key : input.keySet()) {
            mapCopy.put(key, new ArrayList<>(input.get(key)));
        }
        return mapCopy;
    }

    /**
     * The request parameters are unmodifiable, this prevents them being converted into javascript. This method
     * copies unmofifiable to modifiable collections.
     *
     * @param input the parameters
     * @return the parameters in modifiable collections
     */
    private Map<String, List<String>> convertParametersToModifiableObjects(Map<String, List<String>> input) {
        Map<String, List<String>> mapCopy = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, List<String>> entry : input.entrySet()) {
            mapCopy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return mapCopy;
    }

    private Map<String, String> getSessionProperties(String ssoTokenId) {
        Map<String, String> properties = null;
        try {
            Session session = sessionServiceProvider.get().getSession(new SessionID(ssoTokenId));
            if (session != null) {
                properties = new HashMap<>(session.getProperties());
            }
        } catch (SessionException e) {
            logger.error("Failed to get existing session", e);
        }
        return properties;
    }

    private List<String> getAliasList(TreeContext context, String identity, Optional<JsonValue> user,
            Optional<String> contextId) throws NodeProcessException {
        Set<String> aliasList = new HashSet<>();
        aliasList.add(identity);
        idmIntegrationService.getAttributeFromContext(context, ALIAS_LIST)
                .ifPresent(list -> aliasList.addAll(list.asList(String.class)));
        if (user.isPresent()) {
            aliasList.addAll(user.get().get(ALIAS_LIST).asList(String.class));
        } else if (contextId.isPresent()) {
            // try to look up user's existing aliasList if identity attribute already existed in shared state
            aliasList.addAll(IdmIntegrationHelper.getObject(idmIntegrationService, realm,
                    context.request.locales, context.identityResource, config.usernameAttribute(),
                    contextId, config.usernameAttribute(), ALIAS_LIST)
                    .map(u -> u.get(ALIAS_LIST).asList(String.class))
                    .orElse(Collections.emptyList()));
        }
        return new ArrayList<>(aliasList);
    }

    /**
     * The possible outcomes for the SocialProviderHandlerNode.
     */
    public enum SocialAuthOutcome {
        /**
         * Subject match found.
         */
        ACCOUNT_EXISTS,
        /**
         * Subject match not found.
         */
        NO_ACCOUNT
    }

    /**
     * Defines the possible outcomes from this node.
     */
    public static class SocialAuthOutcomeProvider implements OutcomeProvider {
        @Override
        public List<Outcome> getOutcomes(PreferredLocales locales, JsonValue nodeAttributes) {
            ResourceBundle bundle = locales.getBundleInPreferredLocale(BUNDLE,
                    SocialAuthOutcomeProvider.class.getClassLoader());
            return ImmutableList.of(
                    new Outcome(SocialAuthOutcome.ACCOUNT_EXISTS.name(),
                            bundle.getString("accountExistsOutcome")),
                    new Outcome(SocialAuthOutcome.NO_ACCOUNT.name(),
                            bundle.getString("noAccountOutcome")));
        }
    }

    @Override
    public OutputState[] getOutputs() {
        return new OutputState[]{
            new OutputState(SOCIAL_OAUTH_DATA, json(object(
                    field(SocialAuthOutcome.ACCOUNT_EXISTS.name(), true),
                    field(SocialAuthOutcome.NO_ACCOUNT.name(), false))).asMap(Boolean.class)),
            new OutputState(USERNAME, json(object(
                    field(SocialAuthOutcome.ACCOUNT_EXISTS.name(), true),
                    field(SocialAuthOutcome.NO_ACCOUNT.name(), false))).asMap(Boolean.class))
        };
    }

    @Override
    public InputState[] getInputs() {
        return new InputState[]{
            new InputState(SELECTED_IDP),
            new InputState(config.usernameAttribute(), false),
            new InputState(IDPS, false)
        };
    }
}