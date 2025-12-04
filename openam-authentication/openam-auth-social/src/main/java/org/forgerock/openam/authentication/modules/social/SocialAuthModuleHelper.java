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
 * Copyright 2017-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.authentication.modules.social;

import static com.iplanet.am.util.SecureRandomManager.getSecureRandom;
import static com.sun.identity.authentication.spi.AMLoginModule.getIdentityStore;
import static com.sun.identity.shared.encode.CookieUtils.newCookie;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.BUNDLE_NAME;
import static org.forgerock.openam.utils.Time.getClock;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.http.Handler;
import org.forgerock.json.JsonValue;
import org.forgerock.oauth.OAuthClient;
import org.forgerock.oauth.OAuthClientConfiguration;
import org.forgerock.oauth.OAuthException;
import org.forgerock.oauth.UserInfo;
import org.forgerock.openam.authentication.modules.common.mapping.AccountProvider;
import org.forgerock.openam.authentication.modules.oauth2.OAuthUtil;
import org.owasp.esapi.ESAPI;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.sun.identity.authentication.client.AuthClientUtils;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.encode.CookieUtils;

/**
 * Helper class for the Social Auth Module.
 *
 * @see SocialAuthLoginModule
 */
class SocialAuthModuleHelper {

    /**
     * Gets the Original Url from the request.
     *
     * @param request The http request.
     * @return Original Url.
     */
    String getOriginalUrl(HttpServletRequest request) {
        return OAuthUtil.getOriginalUrl(request).toString();
    }

    /**
     * Gets all cookie domains associated with the request.
     *
     * @param request The http request.
     * @return Set of domain names.
     */
    Set<String> getCookieDomainsForRequest(HttpServletRequest request) {
        return AuthClientUtils.getCookieDomainsForRequest(request);
    }

    /**
     * Adds Cookie to the Response.
     *
     * @param response The Response instance.
     * @param name The Cookie Name.
     * @param value The Cookie Value.
     * @param path The Path.
     * @param domain The Cookie Domain.
     */
    void addCookieToResponse(HttpServletResponse response, String name, String value, String path, String domain) {
        CookieUtils.addCookieToResponse(response, newCookie(name, value, path, domain));
    }

    /**
     * Create a new OAuthClient instance based on the OAuthClientConfiguration.
     *
     * @param config The OAuthClientConfiguration instance.
     * @return The new OAuthClient instance.
     */
    OAuthClient newOAuthClient(OAuthClientConfiguration config) {
        try {
            final Class<? extends OAuthClient> oauthClient =
                    (Class<? extends OAuthClient>) Class.forName(config.getClientClass().getName(), true, getClass().getClassLoader());
            return oauthClient
                    .getConstructor(Handler.class, config.getClass(), Clock.class, SecureRandom.class)
                    .newInstance(getHandler(), config, getClock(), getSecureRandom());
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot create an instance of oAuthClient", e);
        }
    }

    private Handler getHandler() {
        return InjectorHolder.getInstance(Key.get(Handler.class, Names.named("CloseableHttpClientHandler")));
    }

    /**
     * Creates a new instance of DataStore.
     *
     * @param provider The Social Provider name.
     * @param sharedState Auth Module's shared state.
     * @return the new SharedStateDataStore instance.
     */
    SharedStateDataStore newDataStore(String provider, Map sharedState) {
        String dataStoreId = RandomStringUtils.randomAlphanumeric(32);
        return new SharedStateDataStore(dataStoreId, provider, sharedState);
    }

    /**
     * @return the Base64 encoded random string.
     */
    String getRandomData() throws AuthLoginException {
        byte[] pass = new byte[20];
        try {
            getSecureRandom().nextBytes(pass);
        } catch (Exception e) {
            throw new AuthLoginException("Error while generating random data", e);
        }
        return Base64.encode(pass);
    }

    /**
     * Gets and existing user from the data store, based on the given criteria.
     * @param realm The realm in which the user belongs.
     * @param accountProvider The provider class using the which the search will be performed.
     * @param userNames The name of the user.
     * @return The user name if exist in the data store.
     */
    public Optional<String> userExistsInTheDataStore(String realm, AccountProvider accountProvider,
            Map<String, Set<String>> userNames) {
        if (!userNames.isEmpty()) {
            final String user = OAuthUtil.getUser(realm, accountProvider, userNames);
            if (user != null) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    /**
     * Provisions a user with the specified attributes.
     *
     * @param realm The realm.
     * @param accountProvider The account provider for creating the user.
     * @param attributes The user attributes.
     * @return The name of the created user.
     * @throws AuthLoginException
     */
    public String provisionUser(String realm, AccountProvider accountProvider, Map<String, Set<String>> attributes) throws AuthLoginException {
        AMIdentity userIdentity = accountProvider.provisionUser(getIdentityStore(realm), attributes);
        return userIdentity.getName().trim();
    }

    /**
     *
     * The input would be Validated to rule out an attack using ESAPI Validator
     *
     * @param context A descriptive name of the parameter that is being validated (e.g., LoginPage_UsernameField).
     * @param input The actual value that is being validated
     * @param rule The regular expression name that maps to the actual regular expression from "ESAPI.properties".
     * @param maxLength The maximum length allowed.
     * @param allowNull Flag to specify if null or empty string is valid
     * @throws AuthLoginException
     */
    public void validateInput(String context, String input, String rule, int maxLength, boolean allowNull)
            throws AuthLoginException {
        if (!ESAPI.validator().isValidInput(context, input, rule, maxLength, allowNull)) {
            OAuthUtil.debugError("OAuth.validateInput(): OAuth 2.0 Not valid input !");
            String msgdata[] = {context, input};
            throw new AuthLoginException(BUNDLE_NAME, "invalidField", msgdata);
        }
    }

    /**
     *
     * Validates if the returned activation code matched the original
     *
     * @param original The activation original generated and sent out
     * @param returned The original returned by user
     * @return if the returned original matches the original original
     */
    public boolean isValidActivationCodeReturned(String original, String returned) {
        return StringUtils.isNotBlank(returned) && returned.trim().equals(original.trim());
    }

    /**
     *
     * @param userInfo The use info object
     * @param emailAttribute The attribute to lookup emil
     * @return The mapped email
     * @throws OAuthException
     */
    public String extractEmail(UserInfo userInfo, String emailAttribute) throws OAuthException {
        JsonValue emailAttributeValue = userInfo.getRawProfile().get(emailAttribute);
        if (emailAttributeValue.isNotNull()) {
            return emailAttributeValue.asString();
        }
        return null;
    }
}
