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
 * Copyright 2017-2020 ForgeRock AS.
 */
package org.forgerock.openam.authentication.modules.social;

import static com.sun.identity.authentication.util.ISAuthConstants.FULL_LOGIN_URL;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.BUNDLE_NAME;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.COOKIE_LOGOUT_URL;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.COOKIE_ORIG_URL;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.CREATE_USER_STATE;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.NONCE_TOKEN_ID;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.PARAM_ACTIVATION;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.SESSION_LOGOUT_BEHAVIOUR;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.SESSION_OAUTH_TOKEN;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.SET_PASSWORD_STATE;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Function;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.oauth.OAuthClient;
import org.forgerock.oauth.OAuthException;
import org.forgerock.oauth.UserInfo;
import org.forgerock.oauth.clients.oidc.OpenIDConnectUserInfo;
import org.forgerock.openam.authentication.modules.common.AuthLoginModule;
import org.forgerock.openam.authentication.modules.oauth2.NoEmailSentException;
import org.forgerock.openam.authentication.modules.oauth2.OAuth;
import org.forgerock.openam.authentication.modules.oauth2.OAuthUtil;
import org.forgerock.openam.authentication.modules.oidc.JwtHandlerConfig;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.ClientTokenJwtGenerator;
import org.forgerock.openam.integration.idm.IdmIntegrationConfig;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.util.Reject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.annotations.VisibleForTesting;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.RedirectCallback;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.locale.AMResourceBundleCache;

/**
 * Abstract Social Auth Login Module
 *
 * The social auth login modules that extends should implement the methods
 * to retrieve the access token returned by the authenticating server
 * and getting user info from the authenticating server
 *
 * @see SocialAuthLoginModule
 * @see SocialAuthLoginModuleWeChatMobile
 */
abstract class AbstractSocialAuthLoginModule extends AuthLoginModule {

    public final static int RESUME_FROM_REGISTRATION_REDIRECT_STATE = 5;

    static final int CANCEL_ACTION_SELECTED = 1;
    static final String ERR_PASSWORD_EMPTY = "errEmptyPass";
    static final String ERR_PASSWORD_LENGTH = "errLength";
    static final String ERR_PASSWORD_NO_MATCH = "errNoMatch";

    private static final int REQUIRED_PASSWORD_LENGTH = 8;
    private static final String USER_STATUS = "inetuserstatus";
    private static final String USER_PASSWORD = "userPassword";
    private static final String ACTIVE = "Active";
    private static final String MIX_UP_MITIGATION_PARAM_CLIENT_ID = "client_id";
    private static final String MIX_UP_MITIGATION_PARAM_ISSUER = "iss";
    private static AMResourceBundleCache amCache = AMResourceBundleCache.getInstance();
    protected final Logger debug;
    private final SocialAuthModuleHelper authModuleHelper;
    private final Function<Map, AbstractSmsSocialAuthConfiguration> configurationFunction;
    private final ClientTokenJwtGenerator clientTokenJwtGenerator;
    private final IdmIntegrationConfig idmConfigProvider;
    private SocialAuthPrincipal principal;
    private AbstractSmsSocialAuthConfiguration config;
    private OAuthClient client;
    private SharedStateDataStore dataStore;
    private ProfileNormalizer profileNormalizer;
    private UserInfo userInfo;
    private String userPassword = "";
    private ResourceBundle bundle = null;
    private JwtClaimsSet jwtClaimsSet;
    private String activationCode;

    /**
     * Constructor of AbstractSocialAuthLoginModule for invocation by subclass constructors.
     */
    AbstractSocialAuthLoginModule(Function<Map, AbstractSmsSocialAuthConfiguration> configurationFunction) {
        this(LoggerFactory.getLogger(AbstractSocialAuthLoginModule.class),
                new SocialAuthModuleHelper(), configurationFunction, new ClientTokenJwtGenerator(),
                InjectorHolder.getInstance(IdmIntegrationConfig.class));
    }

    @VisibleForTesting
    AbstractSocialAuthLoginModule(Logger debug, SocialAuthModuleHelper authModuleHelper,
            Function<Map, AbstractSmsSocialAuthConfiguration> configurationFunction,
            ClientTokenJwtGenerator clientTokenJwtGenerator, IdmIntegrationConfig idmConfigProvider) {
        this.debug = debug;
        this.authModuleHelper = authModuleHelper;
        this.configurationFunction = configurationFunction;
        this.clientTokenJwtGenerator = clientTokenJwtGenerator;
        this.idmConfigProvider = idmConfigProvider;
    }

    @Override
    public void init(Subject subject, Map sharedState, Map options) {
        Reject.ifNull(sharedState, options);

        final AbstractSmsSocialAuthConfiguration config = configurationFunction.apply(options);
        this.init(subject, config,
                authModuleHelper.newOAuthClient(config.getOAuthClientConfiguration()),
                authModuleHelper.newDataStore(config.getOAuthClientConfiguration().getProvider(), sharedState),
                new JwtHandlerConfig(options), new ProfileNormalizer(config),
                amCache.getResBundle(BUNDLE_NAME, getLoginLocale()));
    }

    @VisibleForTesting
    void init(Subject subject, AbstractSmsSocialAuthConfiguration config,
            OAuthClient client, SharedStateDataStore dataStore,
            JwtHandlerConfig jwtHandlerConfig, ProfileNormalizer profileNormalizer, ResourceBundle bundle) {
        Reject.ifNull(config, client, dataStore, jwtHandlerConfig, profileNormalizer);

        this.config = config;
        this.client = client;
        this.dataStore = dataStore;
        this.profileNormalizer = profileNormalizer;
        this.bundle = bundle;
    }

    @Override
    public Principal getPrincipal() {
        return principal;
    }

    /**
     * Gets the UserInfo of the authenticated user from the authentication server
     *
     * @return The UserInfo
     * @throws AuthLoginException Thrown when UserInfo cannot be accessed
     */
    abstract UserInfo getUserInfo() throws AuthLoginException;

    /**
     * Retrieves the access token returned by the authentication server
     *
     * @return The access token
     * @throws AuthLoginException Thrown when access token cannot be retrieved
     */
    abstract String retrieveAccessToken() throws AuthLoginException;

    /**
     * The process performed on getting token
     *
     * @return The next state to be processed in the process cycle
     * @throws LoginException Thrown if the processing fails.
     */
    int processOAuthTokenState() throws LoginException {
        performMixUpMitigationProtectionCheck();

        userInfo = getUserInfo();

        jwtClaimsSet = getJwtClaimSet();

        Map<String, Set<String>> attributes = profileNormalizer.getNormalisedAttributes(userInfo, jwtClaimsSet);
        Map<String, Set<String>> userNames = profileNormalizer.getNormalisedAccountAttributes(userInfo, jwtClaimsSet);
        Optional<String> user = authModuleHelper.userExistsInTheDataStore(getRealm().asPath(),
                profileNormalizer.getAccountProvider(), userNames);

        if (user.isPresent()) {
            return loginSuccess(attributes, user.get());
        }

        if (configuredToProvisionAccount()) {
            IdmIntegrationConfig.GlobalConfig idmConfig = idmConfigProvider.global();
            if (configuredToProvisionAccountByExternalService(idmConfig)) {
                int nextState = RESUME_FROM_REGISTRATION_REDIRECT_STATE;
                prepareRedirectCallbackForExternalRegistrationService(nextState);
                return nextState;
            }
            if (configuredToProvisionAccountLocally(idmConfig)) {
                if (configuredToPromptPassword()) {
                    return SET_PASSWORD_STATE;
                } else {
                    return loginSuccess(attributes, provisionAccountNow(attributes, authModuleHelper.getRandomData()));
                }
            }
            throw new AuthLoginException("Failed to provision account");
        }

        if (configuredToLoginAsAnonymousUser()) {
            return loginSuccess(attributes, getAnonymousUser());
        }

        return loginSuccess(attributes, getMappedUsername(userNames));
    }

    private void performMixUpMitigationProtectionCheck() throws AuthLoginException {
        HttpServletRequest request = getHttpServletRequest();
        if (config.getCfgMixUpMitigation()) {
            String clientId = request.getParameter(MIX_UP_MITIGATION_PARAM_CLIENT_ID);
            if (!config.getCfgClientId().equals(clientId)) {
                OAuthUtil.debugWarning("OAuth 2.0 mix-up mitigation is enabled, but the provided client_id '{}' does "
                        + "not belong to this client '{}'", clientId, config.getCfgClientId());
                throw new AuthLoginException(BUNDLE_NAME, "incorrectClientId", null);
            }
            String issuer = request.getParameter(MIX_UP_MITIGATION_PARAM_ISSUER);
            if (issuer == null || !issuer.equals(config.getCfgIssuerName())) {
                OAuthUtil.debugWarning("OAuth 2.0 mix-up mitigation is enabled, but the provided iss '{}' does "
                        + "not match the issuer in the client configuration", issuer);
                throw new AuthLoginException(BUNDLE_NAME, "incorrectIssuer", null);
            }
        }
    }

    private JwtClaimsSet getJwtClaimSet() {
        JwtClaimsSet jwtClaimsSet = null;
        if (userInfo instanceof OpenIDConnectUserInfo) {
            jwtClaimsSet = ((OpenIDConnectUserInfo) userInfo).getJwtClaimsSet();
        }
        return jwtClaimsSet;
    }

    /**
     * The process cycle performed when the user needs to be prompted to set password
     *
     * @return The next state to be processed in the process cycle
     */
    int processSetPasswordState() throws AuthLoginException {
        Callback[] callbacks = getCallback(SET_PASSWORD_STATE);
        if (isCancelActionSelected(callbacks[2])) {
            return ISAuthConstants.LOGIN_IGNORE;
        } else if (!isPasswordValid(callbacks)) {
            return SET_PASSWORD_STATE;
        }
        else {
            userPassword = extractPassword((PasswordCallback) callbacks[0]);
            emailActivationCode();
            OAuthUtil.debugMessage("User to be created, we need to activate: " + activationCode);
            return CREATE_USER_STATE;
        }

    }

    /**
     * The process cycle performed to create user after capturing the password from user
     *
     * @return The next state to be processed in the process cycle
     */
    int processCreateUserState() throws AuthLoginException {
        Callback[] callbacks = getCallback(CREATE_USER_STATE);
        if (isCancelActionSelected(callbacks[2])) {
            return ISAuthConstants.LOGIN_IGNORE;
        } else {
            String returnedCode = ((NameCallback) callbacks[1]).getName();
            authModuleHelper.validateInput(PARAM_ACTIVATION, returnedCode, "HTTPParameterValue", 512, false);
            OAuthUtil.debugMessage("code entered by the user: " + returnedCode);
            if (!authModuleHelper.isValidActivationCodeReturned(activationCode, returnedCode)) {
                return CREATE_USER_STATE;
            }
            Map<String, Set<String>> attributes = profileNormalizer.getNormalisedAttributes(userInfo, jwtClaimsSet);
            String user =  provisionAccountNow(attributes, userPassword);
            if (user != null) {
                OAuthUtil.debugMessage("User created: " + user);
                return loginSuccess(attributes, user);
            } else {
                return ISAuthConstants.LOGIN_IGNORE;
            }
        }
    }

    /**
     * The process cycle performed after resuming from the external registration
     *
     * @return The next state to be processed in the process cycle
     * @throws AuthLoginException Thrown if the registered user is not found
     */
    int processResumeFromRegistration() throws AuthLoginException {
        Map<String, Set<String>> userNames = profileNormalizer.getNormalisedAccountAttributes(userInfo, jwtClaimsSet);
        Optional<String> user = authModuleHelper.userExistsInTheDataStore(getRealm().asPath(),
                profileNormalizer.getAccountProvider(), userNames);
        if (user.isPresent()) {
            OAuthUtil.debugMessage("OAuth.process(): LOGIN_SUCCEED with user " + user.get());
            Map<String, Set<String>> attributes =
                    new HashMap<>(profileNormalizer.getNormalisedAttributes(userInfo, jwtClaimsSet));

            return loginSuccess(attributes, user.get());
        }
        throw new AuthLoginException("No user mapped!");
    }

    /**
     * Adds the domain cookies on to the response
     */
    void addDomainCookiesToResponse() {
        HttpServletRequest request = getHttpServletRequest();
        HttpServletResponse response = getHttpServletResponse();
        authModuleHelper.getCookieDomainsForRequest(request).stream().forEach((String domain) -> {
            authModuleHelper.addCookieToResponse(response, COOKIE_ORIG_URL, authModuleHelper.getOriginalUrl(request), "/", domain);
            authModuleHelper.addCookieToResponse(response, NONCE_TOKEN_ID, getDataStore().getId(), "/", domain);
            authModuleHelper.addCookieToResponse(response, COOKIE_LOGOUT_URL, config.getCfgLogoutUrl(), "/", domain);
        });
    }

    /**
     * Stores the original as a property on the session
     *
     * @throws AuthLoginException If the session is invalid
     */
    void addOriginalUrlToUserSession() throws AuthLoginException {
        setUserSessionProperty(FULL_LOGIN_URL, authModuleHelper.getOriginalUrl(getHttpServletRequest()));
    }

    /**
     * Get the shared data store
     *
     * @return The shared data store
     */
    SharedStateDataStore getDataStore() {
        return dataStore;
    }

    /**
     * Get the OAuth Client
     *
     * @return the OAuth Client
     */
    OAuthClient getClient() {
        return client;
    }

    private boolean isPasswordValid(Callback[] callbacks) throws AuthLoginException {
        String password = extractPassword((PasswordCallback) callbacks[0]);
        String passwordConfirm  = extractPassword((PasswordCallback) callbacks[1]);
        if (StringUtils.isBlank(password)) {
            substituteHeader(SET_PASSWORD_STATE, bundle.getString(ERR_PASSWORD_EMPTY));
            return false;
        } else if (password.length() < REQUIRED_PASSWORD_LENGTH) {
            substituteHeader(SET_PASSWORD_STATE, bundle.getString(ERR_PASSWORD_LENGTH));
            return false;
        } else if (!password.equals(passwordConfirm)) {
            substituteHeader(SET_PASSWORD_STATE, bundle.getString(ERR_PASSWORD_NO_MATCH));
            return false;
        }
        return true;
    }

    private String extractPassword(PasswordCallback callback) {
        char[] passwordChars = callback.getPassword();
        return callback.getPassword() == null ? null : String.valueOf(passwordChars);
    }

    private void emailActivationCode() throws AuthLoginException {
        activationCode = authModuleHelper.getRandomData();
        String mail = null;
        try {
            String emailAttribute = config.getCfgMailAttribute();
            if (StringUtils.isEmpty(emailAttribute)) {
                OAuthUtil.debugMessage("'emailAttribute' parameter not configured.");
            } else {
                mail = authModuleHelper.extractEmail(userInfo, emailAttribute);
            }
        } catch (OAuthException e) {
            OAuthUtil.debugError("Email id not found in the profile response");
            throw new AuthLoginException("Aborting authentication, because "
                    + "the email id to sent mail could not be found in the profile response");
        }
        if (mail == null) {
            OAuthUtil.debugError("Email id not found in the profile response");
            throw new AuthLoginException("Aborting authentication, because "
                    + "the email id to sent mail could not be found in the profile response");
        }
        OAuthUtil.debugMessage("Mail found = " + mail);
        try {
            OAuthUtil.sendEmail(config.getCfgEmailFrom(), mail, activationCode,
                    config.getSMTPConfig(), bundle, config.getCfgProxyUrl());
        } catch (NoEmailSentException ex) {
            OAuthUtil.debugError("No mail sent due to error", ex);
            throw new AuthLoginException("Aborting authentication, because "
                    + "the mail could not be sent due to a mail sending error");
        }
    }

    private boolean isCancelActionSelected(Callback callback) {
        return  ((ConfirmationCallback) callback).getSelectedIndex() == CANCEL_ACTION_SELECTED;
    }

    private int loginSuccess(Map<String, Set<String>> attributes, String user) throws AuthLoginException {
        if(config.getCfgLogoutBehaviour() != null) {
            // post-authn plugin session properties are only required if logout behaviour options are available
            storeSessionPropertiesForPostAuthnPluginOnLogout();
        }

        saveAttributesToSessionIfRequired(attributes);
        this.principal = new SocialAuthPrincipal(user);
        storeUsernameAndPassword(user, null);
        return ISAuthConstants.LOGIN_SUCCEED;
    }

    private void storeSessionPropertiesForPostAuthnPluginOnLogout() throws AuthLoginException {
        setUserSessionProperty(SESSION_LOGOUT_BEHAVIOUR, config.getCfgLogoutBehaviour());
        String oauthToken = retrieveAccessToken();
        if (oauthToken != null) {
            setUserSessionProperty(SESSION_OAUTH_TOKEN, oauthToken);
        }
    }

    private Realm getRealm() {
        final String realm = getRequestOrg() == null ? "/" : getRequestOrg();
        return new Realm() {
            @Override
            public String asPath() {
                return realm;
            }

            @Override
            public String asRoutingPath() {
                return realm;
            }

            @Override
            public String asDN() {
                return realm;
            }
        };
    }

    private void saveAttributesToSessionIfRequired(Map<String, Set<String>> attributes) throws AuthLoginException {
        if (config.getSaveAttributesToSessionFlag()) {
            setUserSessionProperties(attributes);
        }
    }

    private void setUserSessionProperties(Map<String, Set<String>> attributes) throws AuthLoginException {
        if (attributes != null && !attributes.isEmpty()) {
            for (String attributeName : attributes.keySet()) {
                String attributeValue = attributes.get(attributeName).iterator().next().toString();
                setUserSessionProperty(attributeName, attributeValue);
                OAuthUtil.debugMessage("OAuth.setUserSessionProperties: " + attributeName + "=" + attributeValue);
            }
        } else {
            OAuthUtil.debugMessage("OAuth.setUserSessionProperties: NO attributes to set");
        }
    }

    private boolean configuredToProvisionAccount() {
        return config.getCfgCreateAccount();
    }

    private boolean configuredToProvisionAccountLocally(IdmIntegrationConfig.GlobalConfig idmConfig)
            throws AuthLoginException {
        return configuredToProvisionAccount() && (!config.isCfgRegistrationServiceEnabled() || !idmConfig.enabled());
    }

    private boolean configuredToPromptPassword() {
        return config.getCfgPromptForPassword();
    }

    private boolean configuredToProvisionAccountByExternalService(IdmIntegrationConfig.GlobalConfig idmConfig)
            throws AuthLoginException {
        return configuredToProvisionAccount() && config.isCfgRegistrationServiceEnabled() && idmConfig.enabled();
    }

    private String getMappedUsername(Map<String, Set<String>> userNames) throws AuthLoginException {
        if (userNames != null && !userNames.isEmpty()) {
            Iterator<Set<String>> usersIt = userNames.values().iterator();
            return usersIt.next().iterator().next();
        }
        throw new AuthLoginException("Username not found in the mapped attributes");
    }

    private String getAnonymousUser() throws AuthLoginException {
        String anonUser = config.getAnonymousUserName();
        if (anonUser != null && !anonUser.isEmpty()) {
            return anonUser;
        }
        throw new AuthLoginException("Anonymous user name could not be found in the configuration");
    }

    private boolean configuredToLoginAsAnonymousUser() {
        return config.getMapToAnonymousUser();
    }

    private void prepareRedirectCallbackForExternalRegistrationService(int nextState) throws AuthLoginException {
        String registrationServiceUrl = prepareRegistrationServiceUrl(getHttpServletRequest(), dataStore.retrieveData());
        OAuthUtil.debugMessage("OAuthRegistrationRedirect::registrationServiceUrl:" + registrationServiceUrl);

        RedirectCallback rcNew = new RedirectCallback(registrationServiceUrl, null, "GET");
        rcNew.setTrackingCookie(true);
        replaceCallback(nextState, 0, rcNew);
    }

    private String provisionAccountNow(Map<String, Set<String>> attributes, String userPassword) throws AuthLoginException {
        Map<String, Set<String>> attributesFinal = new HashMap<>(attributes);
        attributesFinal.put(USER_PASSWORD, CollectionUtils.asSet(userPassword));
        attributesFinal.put(USER_STATUS, CollectionUtils.asSet(ACTIVE));

        String user = authModuleHelper.provisionUser(getRealm().asPath(),
                profileNormalizer.getAccountProvider(), attributesFinal);
        if (user == null) {
            throw new AuthLoginException("Unable to create user");
        }
        OAuthUtil.debugMessage("User created: " + user);
        return user;
    }

    private String prepareRegistrationServiceUrl(HttpServletRequest request, JsonValue dataStoreContents)
            throws AuthLoginException {
        URIBuilder uriBuilder = new URIBuilder();
        ArrayList<String> returnParamPairs = new ArrayList<>();
        try {
            for (String param : OAuth.PARAMS) {
                if (request.getParameter(param) != null) {
                    returnParamPairs.add(param + "=" + URLEncoder.encode(request.getParameter(param),
                        StandardCharsets.UTF_8.toString()));
                }
            }
            uriBuilder.addParameter("returnParams",
                    URLEncoder.encode(StringUtils.join(returnParamPairs.toArray(new String[0]), "&"),
                            StandardCharsets.UTF_8.toString()));
        } catch (UnsupportedEncodingException e) {
            debug.error("SocialAuthLoginModule.prepareRegistrationServiceUrl(): UnsupportedEncodingException: {}",
                    e.getMessage(), e);
        }

        try {
            IdmIntegrationConfig.GlobalConfig idmConfig = idmConfigProvider.global();
            uriBuilder.addParameter("clientToken", clientTokenJwtGenerator.generate(idmConfig, dataStoreContents));
            String registrationServiceUrl = idmConfig.idmDeploymentUrl();
            if (!registrationServiceUrl.endsWith("/")) {
                registrationServiceUrl += "/";
            }
            return registrationServiceUrl + "#handleOAuth/&" + uriBuilder.toString().substring(1);
        } catch (JsonProcessingException e) {
            throw new AuthLoginException(e);
        }
    }

}
