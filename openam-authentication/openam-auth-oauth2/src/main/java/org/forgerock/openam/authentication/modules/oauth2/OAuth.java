/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2017 ForgeRock AS.
 * Copyright 2011 Cybernetica AS.
 * 
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Portions Copyrighted 2015 Nomura Research Institute, Ltd.
 */
package org.forgerock.openam.authentication.modules.oauth2;

import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.*;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthUtil.getOriginalUrl;
import static org.forgerock.openam.utils.Time.currentTimeMillis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.authentication.modules.common.mapping.AccountProvider;
import org.forgerock.openam.authentication.modules.common.mapping.AttributeMapper;
import org.forgerock.openam.authentication.modules.oidc.JwtHandler;
import org.forgerock.openam.authentication.modules.oidc.JwtHandlerConfig;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.IOUtils;
import org.forgerock.openam.utils.TimeUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.owasp.esapi.ESAPI;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.authentication.client.AuthClientUtils;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.RedirectCallback;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.common.HttpURLConnectionManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.encode.CookieUtils;

/**
 * This module is deprecated from OpenAM version 14.5.0 onwards.
 * Please use SocialAuthOAuth2 and SocialAuthOpenID instead.
 */
@Deprecated
public class OAuth extends AMLoginModule {

    public static final String PROFILE_SERVICE_RESPONSE = "ATTRIBUTES";
    public static final String OPENID_TOKEN = "OPENID_TOKEN";
    public static final String[] PARAMS = new String[] {"realm", "service", "goto"};
    private static Debug DEBUG = Debug.getInstance("amAuthOAuth2");
    private static final int CANCEL_ACTION_SELECTED = 1;

    private static final int REQUIRED_PASSWORD_LENGTH = 8;
    static final String ERR_PASSWORD_EMPTY = "errEmptyPass";
    static final String ERR_PASSWORD_LENGTH = "errLength";
    static final String ERR_PASSWORD_NO_MATCH = "errNoMatch";
    private String authenticatedUser = null;
    private Map sharedState;
    private OAuthConf config;
    private JwtHandlerConfig jwtHandlerConfig;
    String serverName = "";
    private ResourceBundle bundle = null;
    private static final SecureRandom random = new SecureRandom();
    String data = "";
    String userPassword = "";
    String proxyURL = "";
    private final CTSPersistentStore ctsStore;
    private String activationCode;

    /* default idle time for invalid sessions */
    private static final long maxDefaultIdleTime =
            SystemProperties.getAsLong("com.iplanet.am.session.invalidsessionmaxtime", 3);

    public OAuth() {
        OAuthUtil.debugMessage("OAuth()");
        ctsStore = InjectorHolder.getInstance(CTSPersistentStore.class);
    }

    public void init(Subject subject, Map sharedState, Map config) {
        this.sharedState = sharedState;
        this.config = new OAuthConf(config);
        this.jwtHandlerConfig = new JwtHandlerConfig(config);
        bundle = amCache.getResBundle(BUNDLE_NAME, getLoginLocale());
    }

    
    public int process(Callback[] callbacks, int state) throws LoginException {

        OAuthUtil.debugMessage("process: state = " + state);
        HttpServletRequest request = getHttpServletRequest();
        HttpServletResponse response = getHttpServletResponse();

        if (request == null) {
            OAuthUtil.debugError("OAuth.process(): The request was null, this is "
                    + "an interactive module");
            return ISAuthConstants.LOGIN_IGNORE;
        }

        // We are being redirected back from an OAuth 2 Identity Provider
        String code = request.getParameter(PARAM_CODE);
        if (code != null && state == LOGIN_START) {
            OAuthUtil.debugMessage("OAuth.process(): GOT CODE: " + code);
            state = GET_OAUTH_TOKEN_STATE;
        }

        // The Proxy is used to return with a POST to the module
        proxyURL = config.getProxyURL();

        switch (state) {
            case ISAuthConstants.LOGIN_START: {
                return loginToOIDCProvider(request, response);
            }

            case GET_OAUTH_TOKEN_STATE: {

                final String csrfState = request.getParameter("state");
                code = request.getParameter(PARAM_CODE);

                // Check to see if we are being redirected back from an OAuth 2 Identity Provider with no code (no
                // login occurred)
                if (code == null || code.isEmpty()) {
                    OAuthUtil.debugMessage("OAuth.process(): LOGIN_IGNORE");
                    return ISAuthConstants.LOGIN_START;
                }

                if (csrfState == null) {
                    OAuthUtil.debugError("OAuth.process(): Authorization call-back failed because there was no state "
                            + "parameter");
                    throw new AuthLoginException(BUNDLE_NAME, "noState", null);
                }

                if (config.isMixUpMitigationEnabled()) {
                    String clientId = request.getParameter("client_id");
                    if (!config.getClientId().equals(clientId)) {
                        DEBUG.warning("OAuth 2.0 mix-up mitigation is enabled, but the provided client_id '{}' does "
                                + "not belong to this client '{}'", clientId, config.getClientId());
                        throw new AuthLoginException(BUNDLE_NAME, "incorrectClientId", null);
                    }
                    String issuer = request.getParameter("iss");
                    if (issuer == null || !issuer.equals(jwtHandlerConfig.getConfiguredIssuer())) {
                        DEBUG.warning("OAuth 2.0 mix-up mitigation is enabled, but the provided iss '{}' does "
                                + "not match the issuer in the client configuration", issuer);
                        throw new AuthLoginException(BUNDLE_NAME, "incorrectIssuer", null);
                    }
                }

                try {
                    Token csrfStateToken = ctsStore.read(OAuthUtil.findCookie(request, NONCE_TOKEN_ID));
                    try {
                        if (csrfStateToken == null
                                || !csrfState.equals(csrfStateToken.getAttribute(CoreTokenField.STRING_ONE))) {
                            OAuthUtil.debugWarning("OAuth.process(): Authorization call-back failed " +
                                    "because the state parameter contained an unexpected value");
                            return loginToOIDCProvider(request, response);
                        }
                    } finally {
                        if (csrfStateToken != null) {
                            ctsStore.deleteAsync(csrfStateToken);
                        }
                    }

                    validateInput("code", code, "HTTPParameterValue", 2000, false);

                    OAuthUtil.debugMessage("OAuth.process(): code parameter: " + code);

                    String tokenSvcResponse = getContentUsingPOST(config.getTokenServiceUrl(), null, null,
                            config.getTokenServicePOSTparameters(code, proxyURL, csrfState));
                    OAuthUtil.debugMessage("OAuth.process(): token=" + tokenSvcResponse);

                    JwtClaimsSet jwtClaims = null;
                    String idToken = null;
                    if (config.isOpenIDConnect()) {
                        idToken = extractToken(ID_TOKEN, tokenSvcResponse);
                        setUserSessionProperty(OPENID_TOKEN, idToken);
                        JwtHandler jwtHandler = new JwtHandler(jwtHandlerConfig);
                        try {
                            jwtClaims = jwtHandler.validateJwt(idToken);
                        } catch (RuntimeException | AuthLoginException e) {
                            DEBUG.warning("Cannot validate JWT", e);
                            throw e;
                        }
                        if (!JwtHandler.isIntendedForAudience(config.getClientId(), jwtClaims)) {
                            OAuthUtil.debugError("OAuth.process(): ID token is not for this client as audience.");
                            throw new AuthLoginException(BUNDLE_NAME, "audience", null);
                        }

                        String expectedNonce = csrfStateToken.getAttribute(CoreTokenField.STRING_TWO);
                        validateNonce(jwtClaims, expectedNonce);
                    }

                    String token = extractToken(PARAM_ACCESS_TOKEN, tokenSvcResponse);

                    setUserSessionProperty(SESSION_OAUTH_TOKEN, token);

                    String profileSvcResponse = null;
                    if (StringUtils.isNotEmpty(config.getProfileServiceUrl())) {
                        profileSvcResponse = getContentUsingGET(config.getProfileServiceUrl(), "Bearer " + token,
                                config.getProfileServiceGetParameters());
                        OAuthUtil.debugMessage("OAuth.process(): Profile Svc response: " + profileSvcResponse);
                    }

                    String realm = getRequestOrg();

                    if (realm == null) {
                        realm = "/";
                    }

                    setUserSessionProperty(SESSION_LOGOUT_BEHAVIOUR,
                            config.getLogoutBhaviour());

                    AccountProvider accountProvider = instantiateAccountProvider();
                    AttributeMapper accountAttributeMapper = instantiateAccountMapper();
                    Map<String, Set<String>> userNames = OAuthUtil.getAttributes(profileSvcResponse,
                            config.getAccountMapperConfig(), accountAttributeMapper, jwtClaims);
                    
                    String user = null;
                    if (!userNames.isEmpty()) {
                      user = OAuthUtil.getUser(realm, accountProvider, userNames);
                    }

                    if (user == null && !config.getCreateAccountFlag()) {
                        authenticatedUser = getDynamicUser(userNames);

                        if (authenticatedUser != null) {
                            if (config.getSaveAttributesToSessionFlag()) {
                                Map <String, Set<String>> attributes = 
                                        getAttributesMap(profileSvcResponse, jwtClaims);
                                saveAttributes(attributes);
                            }
                            OAuthUtil.debugMessage("OAuth.process(): LOGIN_SUCCEED "
                                    + "with user " + authenticatedUser);
                            return loginSuccess();
                        } else {
                            throw new AuthLoginException("No user mapped!");
                        }

                    }

                    if (user == null && config.getCreateAccountFlag()) {
                        if (config.getPromptPasswordFlag()) {
                            setUserSessionProperty(PROFILE_SERVICE_RESPONSE, profileSvcResponse);
                            return SET_PASSWORD_STATE;
                        } else {
                            authenticatedUser = provisionAccountNow(accountProvider, realm, profileSvcResponse,
                                    getRandomData(), jwtClaims);
                            if (authenticatedUser != null) {
                                OAuthUtil.debugMessage("User created: " + authenticatedUser);
                                return loginSuccess();
                            } else {
                                return ISAuthConstants.LOGIN_IGNORE;
                            }
                        }
                    }

                    if (user != null) {
                        authenticatedUser = user;
                        OAuthUtil.debugMessage("OAuth.process(): LOGIN_SUCCEED "
                                + "with user " + authenticatedUser);
                        if (config.getSaveAttributesToSessionFlag()) {
                            Map<String, Set<String>> attributes = getAttributesMap(
                                    profileSvcResponse, jwtClaims);
                            saveAttributes(attributes);
                        }
                        return loginSuccess();
                    }

                } catch (CoreTokenException e) {
                    OAuthUtil.debugError("OAuth.process(): Authorization call-back failed because the state parameter "
                            + "contained an unexpected value");
                    throw new AuthLoginException(BUNDLE_NAME, "incorrectState", null, e);
                }
                break;
            }

            case SET_PASSWORD_STATE: {
                return processSetPasswordState();

            }

            case CREATE_USER_STATE: {
                return processCreateUserState();
            }

            default: {
                OAuthUtil.debugError("OAuth.process(): Illegal State");
                return ISAuthConstants.LOGIN_IGNORE;
            }
        }
        
        throw new AuthLoginException(BUNDLE_NAME, "unknownState", null);
    }

    private int loginToOIDCProvider(HttpServletRequest request, HttpServletResponse response) throws AuthLoginException {
        config.validateConfiguration();
        serverName = request.getServerName();
        StringBuilder originalUrl = getOriginalUrl(request);

        // Find the domains for which we are configured

        String ProviderLogoutURL = config.getLogoutServiceUrl();

        String csrfStateTokenId = RandomStringUtils.randomAlphanumeric(32);
        String csrfState = createAuthorizationState();
        Token csrfStateToken = new Token(csrfStateTokenId, TokenType.GENERIC);
        csrfStateToken.setAttribute(CoreTokenField.STRING_ONE, csrfState);

        String nonce = null;
        if (config.isOpenIDConnect()) {
            nonce = newNonceValue();
            csrfStateToken.setAttribute(CoreTokenField.STRING_TWO, nonce);
        }

        long expiryTime = currentTimeMillis() + TimeUnit.MINUTES.toMillis(maxDefaultIdleTime);
        Calendar expiryTimeStamp = TimeUtils.fromUnixTime(expiryTime, TimeUnit.MILLISECONDS);
        csrfStateToken.setExpiryTimestamp(expiryTimeStamp);

        try {
            ctsStore.create(csrfStateToken);
        } catch (CoreTokenException e) {
            OAuthUtil.debugError("OAuth.process(): Authorization redirect failed to be sent because the state "
                    + "could not be stored");
            throw new AuthLoginException("OAuth.process(): Authorization redirect failed to be sent because "
                    + "the state could not be stored", e);
        }

        // Set the return URL Cookie
        // Note: The return URL cookie from the RedirectCallback can not
        // be used because the framework changes the order of the
        // parameters in the query. OAuth2 requires an identical URL
        // when retrieving the token
        for (String domain : AuthClientUtils.getCookieDomainsForRequest(request)) {
            CookieUtils.addCookieToResponse(response,
                    CookieUtils.newCookie(COOKIE_PROXY_URL, proxyURL, "/", domain));
            CookieUtils.addCookieToResponse(response,
                    CookieUtils.newCookie(COOKIE_ORIG_URL, originalUrl.toString(), "/", domain));
            CookieUtils.addCookieToResponse(response,
                    CookieUtils.newCookie(NONCE_TOKEN_ID, csrfStateTokenId, "/", domain));
            if (ProviderLogoutURL != null && !ProviderLogoutURL.isEmpty()) {
                CookieUtils.addCookieToResponse(response,
                        CookieUtils.newCookie(COOKIE_LOGOUT_URL, ProviderLogoutURL, "/", domain));
            }
        }

        // The Proxy is used to return with a POST to the module
        setUserSessionProperty(ISAuthConstants.FULL_LOGIN_URL, originalUrl.toString());

        String authServiceUrl = config.getAuthServiceUrl(proxyURL, csrfState, nonce);
        OAuthUtil.debugMessage("OAuth.process(): New RedirectURL=" + authServiceUrl);

        Callback[] callbacks1 = getCallback(2);
        RedirectCallback rc = (RedirectCallback) callbacks1[0];
        RedirectCallback rcNew = new RedirectCallback(authServiceUrl,
                null,
                "GET",
                rc.getStatusParameter(),
                rc.getRedirectBackUrlCookieName());
        rcNew.setTrackingCookie(true);
        replaceCallback(2, 0, rcNew);
        return GET_OAUTH_TOKEN_STATE;
    }

    /**
     * The process cycle performed when the user needs to be prompted to set password
     *
     * @return The next state to be processed in the process cycle
     */
    private int processSetPasswordState() throws AuthLoginException {
        Callback[] callbacks = getCallback(SET_PASSWORD_STATE);
        if (isCancelActionSelected(callbacks[2])) {
            return ISAuthConstants.LOGIN_IGNORE;
        } else {
            if (isPasswordValid(callbacks)) {
                userPassword = extractPassword((PasswordCallback) callbacks[0]);
            } else {
                return SET_PASSWORD_STATE;
            }
        }
        emailActivationCode();
        OAuthUtil.debugMessage("User to be created, we need to activate: " + activationCode);
        return CREATE_USER_STATE;
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
            validateInput(PARAM_ACTIVATION, returnedCode, "HTTPParameterValue", 512, false);
            OAuthUtil.debugMessage("code entered by the user: " + returnedCode);
            if (StringUtils.isBlank(returnedCode) || !returnedCode.trim().equals(activationCode.trim())) {
                return CREATE_USER_STATE;
            }
            String profileSvcResponse = getUserSessionProperty(PROFILE_SERVICE_RESPONSE);
            String idToken = getUserSessionProperty(ID_TOKEN);
            String realm = getRequestOrg();
            if (realm == null) {
                realm = "/";
            }

            OAuthUtil.debugMessage("Got Attributes: " + profileSvcResponse);
            AccountProvider accountProvider = instantiateAccountProvider();
            JwtClaimsSet jwtClaims = null;
            if (idToken != null) {
                jwtClaims = new JwtHandler(jwtHandlerConfig).getJwtClaims(idToken);
            }
            authenticatedUser = provisionAccountNow(accountProvider, realm, profileSvcResponse, userPassword, jwtClaims);
            if (authenticatedUser != null) {
                OAuthUtil.debugMessage("User created: " + authenticatedUser);
                return loginSuccess();
            } else {
                return ISAuthConstants.LOGIN_IGNORE;
            }
        }
    }

    private int loginSuccess() {
        storeUsernamePasswd(authenticatedUser, null);
        return ISAuthConstants.LOGIN_SUCCEED;
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

    private boolean isCancelActionSelected(Callback callback) {
        return ((ConfirmationCallback) callback).getSelectedIndex() == CANCEL_ACTION_SELECTED;
    }

    private void emailActivationCode() throws AuthLoginException {
        activationCode = getRandomData();
        String profileSvcResponse = getUserSessionProperty("ATTRIBUTES");
        String mail = getMail(profileSvcResponse, config.getMailAttribute());
        if (mail == null) {
            OAuthUtil.debugError("Email id not found in the profile response");
            throw new AuthLoginException("Aborting authentication, because "
                    + "the email id to send mail to could not be found in the profile response");
        }
        OAuthUtil.debugMessage("Mail found = " + mail);
        try {
            OAuthUtil.sendEmail(config.getEmailFrom(), mail, activationCode,
                    config.getSMTPConfig(), bundle, config.getProxyURL());
        } catch (NoEmailSentException ex) {
            OAuthUtil.debugError("No mail sent due to error", ex);
            throw new AuthLoginException("Aborting authentication: error sending emai");
        }
    }

    private void validateNonce(JwtClaimsSet jwtClaims, String expectedNonce) throws AuthLoginException {
        boolean validNonce = jwtClaims.get("nonce") != null
                && expectedNonce.equals(jwtClaims.get("nonce").asString());

        if (!validNonce) {
            OAuthUtil.debugError("OAuth.process(): Authorization call-back failed because " +
                    "the nonce parameter contained an unexpected value");
            throw new AuthLoginException(BUNDLE_NAME, "incorrectNonce", null);
        }
    }

    private String newNonceValue() {
        return UUID.randomUUID().toString();
    }

    private String createAuthorizationState() {
        return new BigInteger(160, new SecureRandom()).toString(Character.MAX_RADIX);
    }

    // Generate random data
    private String getRandomData() {
	        byte[] pass = new byte[20];
	        random.nextBytes(pass);
	       return Base64.encode(pass);
     }

    // Create an instance of the pluggable account mapper
    private AttributeMapper<?> instantiateAccountMapper () throws AuthLoginException {
        return OAuthUtil.instantiateAccountMapper(config.getAccountMapper());
    }

    // Create an instance of the pluggable account mapper
    private AccountProvider instantiateAccountProvider() throws AuthLoginException {
        return OAuthUtil.instantiateAccountProvider(config.getAccountProvider());
    }


    // Obtain the attributes configured for the module, by using the pluggable
    // Attribute mapper
    private Map<String, Set<String>> getAttributesMap(String svcProfileResponse, JwtClaimsSet jwtClaims) {
        return OAuthUtil.getAttributesMap(config.getAttributeMapperConfig(), config.getAttributeMappers(), svcProfileResponse, jwtClaims);
    }


    // Save the attributes configured for the attribute mapper as session attributes
    public void saveAttributes(Map<String, Set<String>> attributes) throws AuthLoginException {

        if (attributes != null && !attributes.isEmpty()) {
            for (String attributeName : attributes.keySet()) {
                String attributeValue = attributes.get(attributeName).
                        iterator().next().toString();
                setUserSessionProperty(attributeName, attributeValue);
                OAuthUtil.debugMessage("OAuth.saveAttributes: "
                        + attributeName + "=" + attributeValue);
            }
        } else {
            OAuthUtil.debugMessage("OAuth.saveAttributes: NO attributes to set");
        }
    }
    
    // Generate a user name, either using the anonymous user if configured or by
    // extracting the user from the userName map.
    // Return null, if nothing was found
    private String getDynamicUser(Map<String, Set<String>> userNames)
            throws AuthLoginException {

        String dynamicUser = null;
        if (config.getUseAnonymousUserFlag()) {
            String anonUser = config.getAnonymousUser();
            if (anonUser != null && !anonUser.isEmpty()) {
                dynamicUser = anonUser;
            }
        } else { // Do not use anonymous
            if (userNames != null && !userNames.isEmpty()) {
                Iterator<Set<String>> usersIt = userNames.values().iterator();
                dynamicUser = usersIt.next().iterator().next();
            }

        }
        return dynamicUser;
    }

    private String getContentUsingPOST(String serviceUrl, String authorizationHeader, Map<String, String> getParameters,
            Map<String, String> postParameters) throws LoginException {
        return getContent(serviceUrl, authorizationHeader, getParameters, postParameters, "POST");
    }

    private String getContentUsingGET(String serviceUrl, String authorizationHeader, Map<String, String> getParameters)
            throws LoginException {
        return getContent(serviceUrl, authorizationHeader, getParameters, null, "GET");

    }

    private String getContent(String serviceUrl, String authorizationHeader, Map<String, String> getParameters,
            Map<String, String> postParameters, String httpMethod) throws LoginException {

        InputStream inputStream;
        if ("GET".equals(httpMethod)) {
            inputStream = getContentStreamByGET(serviceUrl, authorizationHeader, getParameters);
        } else if ("POST".equals(httpMethod)) {
            inputStream = getContentStreamByPOST(serviceUrl, authorizationHeader, getParameters, postParameters);
        } else {
            throw new IllegalArgumentException("httpMethod='" + httpMethod + "' is not valid. Expecting POST or GET");
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));

        StringBuilder buf = new StringBuilder();
        try {
            String str;
            while ((str = in.readLine()) != null) {
                buf.append(str);
            }
        } catch (IOException ioe) {
            OAuthUtil.debugError("OAuth.getContent: IOException: " + ioe.getMessage());
            throw new AuthLoginException(BUNDLE_NAME, "ioe", null, ioe);
        } finally {
            IOUtils.closeIfNotNull(in);
        }
        return buf.toString();
    }

    // Create the account in the realm, by using the pluggable account mapper and
    // the attributes configured in the attribute mapper
    public String provisionAccountNow(AccountProvider accountProvider, String realm, String profileSvcResponse,
            String userPassword, JwtClaimsSet jwtClaims)
            throws AuthLoginException {

            Map<String, Set<String>> attributes = getAttributesMap(profileSvcResponse, jwtClaims);
            if (config.getSaveAttributesToSessionFlag()) {
                saveAttributes(attributes);
            }
            attributes.put("userPassword", CollectionUtils.asSet(userPassword));
            attributes.put("inetuserstatus", CollectionUtils.asSet("Active"));
            AMIdentity userIdentity =
                    accountProvider.provisionUser(getAMIdentityRepository(realm),
                    attributes);
            if (userIdentity != null) {
                return userIdentity.getName().trim();
            } else {
                return null;      
            }     
    }

    private String appendParametersToUrl(Map<String, String> parameters,
                                         String urlString) throws UnsupportedEncodingException {
        if (!CollectionUtils.isEmpty(parameters)) {
            if (!urlString.contains("?")) {
                urlString += "?";
            } else {
                urlString += "&";
            }
            urlString += getDataString(parameters);
        }
        return urlString;
    }

    public InputStream getContentStreamByGET(String serviceUrl, String authorizationHeader,
            Map<String, String> getParameters) throws LoginException {

        OAuthUtil.debugMessage("service url: " + serviceUrl);
        OAuthUtil.debugMessage("GET parameters: " + getParameters);
        try {
            InputStream is;
            serviceUrl = appendParametersToUrl(getParameters, serviceUrl);
            URL urlC = new URL(serviceUrl);

            HttpURLConnection connection = HttpURLConnectionManager.getConnection(urlC);
            connection.setDoOutput(true);
            connection.setRequestMethod("GET");
            if (authorizationHeader != null) {
                connection.setRequestProperty("Authorization", authorizationHeader);
            }
            connection.connect();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                OAuthUtil.debugMessage("OAuth.getContentStreamByGET: HTTP Conn OK");
                is = connection.getInputStream();
            } else {
                // Server returned HTTP error code.
                String errorStream = getErrorStream(connection);
                if (OAuthUtil.debugMessageEnabled()) {
                  OAuthUtil.debugMessage("OAuth.getContentStreamByGET: HTTP Conn Error:\n" +
                        " Response code: " + connection.getResponseCode() + "\n " +
                        " Response message: " + connection.getResponseMessage() + "\n" +
                        " Error stream: " + errorStream + "\n");
                }
                is = getContentStreamByPOST(serviceUrl, authorizationHeader, getParameters, Collections
                .<String, String>emptyMap());
            }

            return is;

        } catch (MalformedURLException mfe) {
            throw new AuthLoginException(BUNDLE_NAME,"malformedURL", null, mfe);
        } catch (IOException ioe) {
            DEBUG.warning("OAuth.getContentStreamByGET URL={} caught IOException", serviceUrl, ioe);
            throw new AuthLoginException(BUNDLE_NAME,"ioe", null, ioe);
        }
    }
    
    private String getErrorStream(HttpURLConnection connection) {
        InputStream errStream = connection.getErrorStream();
        if (errStream == null) {
            return "Empty error stream";
        } else {
            BufferedReader in = new BufferedReader(new InputStreamReader(errStream));
            StringBuilder buf = new StringBuilder();
            try {
                String str;
                while ((str = in.readLine()) != null) {
                    buf.append(str);
                }
            }
            catch (IOException ioe) {
                OAuthUtil.debugError("OAuth.getErrorStream: IOException: " + ioe.getMessage());
            } finally {
                IOUtils.closeIfNotNull(in);
            }
            return buf.toString();
        }
    }

    public InputStream getContentStreamByPOST(String serviceUrl, String authorizationHeader,
            Map<String, String> getParameters, Map<String, String> postParameters) throws LoginException {

        InputStream is = null;

        try {
            OAuthUtil.debugMessage("OAuth.getContentStreamByPOST: URL = " + serviceUrl);
            OAuthUtil.debugMessage("OAuth.getContentStreamByPOST: GET parameters = " + getParameters);
            OAuthUtil.debugMessage("OAuth.getContentStreamByPOST: POST parameters = " + stripClientSecretFromParams(postParameters));

            serviceUrl = appendParametersToUrl(getParameters, serviceUrl);
            URL url = new URL(serviceUrl);
            String query = url.getQuery();
            OAuthUtil.debugMessage("OAuth.getContentStreamByPOST: Query: " + query);

            HttpURLConnection connection = HttpURLConnectionManager.getConnection(url);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            if (authorizationHeader != null) {
                connection.setRequestProperty("Authorization", authorizationHeader);
            }
            if (postParameters != null && !postParameters.isEmpty()) {
                OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                writer.write(getDataString(postParameters));
                writer.close();
            }
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                OAuthUtil.debugMessage("OAuth.getContentStreamByPOST: HTTP Conn OK");
                is = connection.getInputStream();
            } else { // Error Code
                String data2[] = {String.valueOf(connection.getResponseCode())};
                OAuthUtil.debugError("OAuth.getContentStreamByPOST: HTTP Conn Error:\n" +
                        " Response code: " + connection.getResponseCode() + "\n" +
                        " Response message: " + connection.getResponseMessage() + "\n" +
                        " Error stream: " + getErrorStream(connection) + "\n");
                throw new AuthLoginException(BUNDLE_NAME, "httpErrorCode", data2);
            }
        } catch (MalformedURLException e) {
            throw new AuthLoginException(BUNDLE_NAME,"malformedURL", null, e);
        } catch (IOException e) {
            DEBUG.warning("OAuth.getContentStreamByPOST URL={} caught IOException", serviceUrl, e);
            throw new AuthLoginException(BUNDLE_NAME,"ioe", null, e);
        }

        return is;
    }

    // Extract the Token from the OAuth 2.0 response
    // Todo: Maybe this should be pluggable
    public String extractToken(String tokenName, String response) {

        String token = "";
        try {
            JSONObject jsonToken = new JSONObject(response);
            if (jsonToken != null
                    && !jsonToken.isNull(tokenName)) {
                token = jsonToken.getString(tokenName);
                OAuthUtil.debugMessage(tokenName + ": " + token);
            }
        } catch (JSONException je) {
            OAuthUtil.debugMessage("OAuth.extractToken: Not in JSON format" + je);
            token = OAuthUtil.getParamValue(response, tokenName);
        }

        return token;
    }

    // Obtain the email address field from the response provided by the
    // OAuth 2.0 Profile service.
    public String getMail(String svcResponse, String mailAttribute) {
        String mail = "";
        OAuthUtil.debugMessage("mailAttribute: " + mailAttribute);
        try {
            JSONObject jsonData = new JSONObject(svcResponse);

            if (mailAttribute != null && mailAttribute.indexOf(".") != -1) {
                StringTokenizer parts = new StringTokenizer(mailAttribute, ".");
                mail = jsonData.getJSONObject(parts.nextToken()).getString(parts.nextToken());
            } else {
                mail = jsonData.getString(mailAttribute);
            }
            OAuthUtil.debugMessage("mail: " + mail);

        } catch (JSONException je) {
            OAuthUtil.debugMessage("OAuth.getMail: Not in JSON format" + je);
        }

        return mail;
    }
    
    // Validate the field provided as input
    public void validateInput(String tag, String inputField,
            String rule, int maxLength, boolean allowNull)
            throws AuthLoginException {
        if (!ESAPI.validator().isValidInput(tag, inputField, rule, maxLength, allowNull)) {
            OAuthUtil.debugError("OAuth.validateInput(): OAuth 2.0 Not valid input !");
            String msgdata[] = {tag, inputField};
            throw new AuthLoginException(BUNDLE_NAME, "invalidField", msgdata);
        };
    }
    
    
    public Principal getPrincipal() {
        if (authenticatedUser != null) {
            return new OAuthPrincipal(authenticatedUser);
        }
        return null;
    }

    public void destroyModuleState() {
        authenticatedUser = null;
    }

    public void nullifyUsedVars() {
        config = null;
        sharedState = null;
    }

    private String getDataString(Map<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if(result.length() > 0) {
                result.append("&");
            }
            // We don't need to encode the key/value as they are already encoded
            result.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return result.toString();
    }

    private Map<String, String> stripClientSecretFromParams(Map<String, String> params) {
        Map<String, String> resultMap = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            if (key.equals(OAuth2Constants.Params.CLIENT_SECRET)) {
                resultMap.put(key, "CLIENT_SECRET");
            } else {
                resultMap.put(key, entry.getValue());
            }
        }
        return resultMap;
    }

}
