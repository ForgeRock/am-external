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
 * Copyright 2017 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.social;


import static com.sun.identity.shared.datastruct.CollectionHelper.getBooleanMapAttr;
import static com.sun.identity.shared.datastruct.CollectionHelper.getMapAttr;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_EMAIL_GWY_IMPL;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_SMTP_SSL_ENABLED;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_SMTP_USERNAME;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_SMTP_HOSTNAME;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_SMTP_PORT;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_SMTP_PASSWORD;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.forgerock.oauth.OAuthClientConfiguration;

/**
 * Base class for SMS based configurations for social auth modules.
 */
abstract class AbstractSmsSocialAuthConfiguration {

    static final String CFG_CLIENT_ID = "clientId";
    static final String CFG_CLIENT_SECRET = "clientSecret";
    static final String CFG_AUTH_ENDPOINT = "authorizeEndpoint";
    static final String CFG_TOKEN_ENDPOINT = "tokenEndpoint";
    static final String CFG_USER_INFO_ENDPOINT = "userInfoEndpoint";
    static final String CFG_SCOPE = "scope";
    static final String CFG_SCOPE_DELIMITER = "scopeDelimiter";
    static final String CFG_BASIC_AUTH = "usesBasicAuth";
    static final String CFG_PROXY_URL = "ssoProxyUrl";
    static final String CFG_SUBJECT_PROPERTY = "subjectProperty";
    static final String CFG_PROVIDER = "provider";
    static final String CFG_ISSUER_NAME = "issuerName";
    static final String CFG_LOGOUT_URL = "logoutServiceUrl";
    static final String CFG_LOGOUT_BEHAVIOUR = "logoutBehaviour";

    private static final String CFG_ACCOUNT_PROVIDER_CLASS = "accountProviderClass";
    private static final String CFG_ACCOUNT_MAPPER_CLASS = "accountMapperClass";
    private static final String CFG_ATTRIBUTE_MAPPING_CLASS = "attributeMappingClasses";
    private static final String CFG_ACCOUNT_MAPPER_CONFIGURATION = "accountMapperConfiguration";
    private static final String CFG_ATTRIBUTE_MAPPING_CONFIGURATION = "attributeMapperConfiguration";
    private static final String CFG_CREATE_ACCOUNT = "createAccount";
    private static final String CFG_ENABLE_REGISTRATION_SERVICE = "enableRegistrationService";
    private static final String CFG_SAVE_ATTRIBUTES_TO_SESSION = "saveAttributesInSession";
    private static final String CFG_MAP_TO_ANONYMOUS_USER = "mapToAnonymousUser";
    private static final String CFG_ANONYMOUS_USER_NAME = "anonymousUserName";
    private static final String CFG_PROMPT_FOR_PASSWORD = "promptPasswordFlag";
    private static final String CFG_FROM_EMAIL = "smtpFromAddress";
    private static final String CFG_EMAIL_GATE_WAY = "emailGateway";
    private static final String CFG_SMTP_HOST_NAME = "smtpHost";
    private static final String CFG_SMTP_PORT = "smtpPort";
    private static final String CFG_SMTP_USER_NAME = "smtpUsername";
    private static final String CFG_SMTP_USER_PASSWORD = "smtpPassword";
    private static final String CFG_SMTP_SSL_ENABLED = "smtpSslEnabled";
    private static final String CFG_EMAIL_ATTRIBUTE = "emailAttribute";
    private static final String CFG_MIX_UP_MITIGATION = "mixUpMitigation";

    protected final Map<String, Set<String>> options;

    /**
     * Constructs AbstractSmsSocialAuthConfiguration instance.
     *
     * @param options The configured SMS attributes.
     */
    AbstractSmsSocialAuthConfiguration(Map<String, Set<String>> options) {
        this.options = options;
    }

    /**
     * Returns the OAuthClientConfiguration instance.
     *
     * @return The OAuthClientConfiguration instance for the social auth module.
     */
    abstract OAuthClientConfiguration getOAuthClientConfiguration();

    /**
     * @return The account provider class name.
     */
    public String getCfgAccountProviderClass() {
        return getMapAttr(options, CFG_ACCOUNT_PROVIDER_CLASS);
    }

    /**
     * @return The account mapper class name.
     */
    public String getCfgAccountMapperClass() {
        return getMapAttr(options, CFG_ACCOUNT_MAPPER_CLASS);
    }

    /**
     * @return The account mapper configurations.
     */
    public Set<String> getCfgAccountMapperConfiguration() {
        return options.get(CFG_ACCOUNT_MAPPER_CONFIGURATION);
    }

    /**
     * @return The attribute mappers.
     */
    public Set<String> getCfgAttributeMappingClasses() {
        return options.get(CFG_ATTRIBUTE_MAPPING_CLASS);
    }

    /**
     * @return The attribute mapper configurations.
     */
    public Set<String> getCfgAttributeMappingConfiguration() {
        return options.get(CFG_ATTRIBUTE_MAPPING_CONFIGURATION);
    }

    /**
     * @return The create account flag configuration value.
     */
    public boolean getCfgCreateAccount() {
        return getBooleanMapAttr(options, CFG_CREATE_ACCOUNT, true);
    }

    /**
     * @return {@code true} if the external registration service is enabled.
     */
    public boolean isCfgRegistrationServiceEnabled() {
        return getBooleanMapAttr(options, CFG_ENABLE_REGISTRATION_SERVICE, false);
    }

    /**
     * @return The save attributes to session flag value.
     */
    public boolean getSaveAttributesToSessionFlag() {
        return getBooleanMapAttr(options, CFG_SAVE_ATTRIBUTES_TO_SESSION, true);
    }

    /**
     * @return The map to anonymous user flag value.
     */
    public boolean getMapToAnonymousUser() {
        return getBooleanMapAttr(options, CFG_MAP_TO_ANONYMOUS_USER, false);
    }

    /**
     * @return The configured anonymous user name.
     */
    public String getAnonymousUserName() {
        return getMapAttr(options, CFG_ANONYMOUS_USER_NAME, "anonymous");
    }

    /**
     * @return The prompt for password flag
     */
    public boolean getCfgPromptForPassword() {
        return getBooleanMapAttr(options, CFG_PROMPT_FOR_PASSWORD, false);
    }

    /**
     * @return The configured from email
     */
    public String getCfgEmailFrom() {
        return getMapAttr(options, CFG_FROM_EMAIL);
    }

    /**
     * @return The configured attribute to indicate the email field in response
     */
    public String getCfgMailAttribute() {
        return getMapAttr(options, CFG_EMAIL_ATTRIBUTE);
    }

    /**
     * @return The configured attribute indicating mix up mitigation is enabled or disable
     */
    public boolean getCfgMixUpMitigation() {
        return getBooleanMapAttr(options, CFG_MIX_UP_MITIGATION, false);
    }

    /**
     * @return The configured client id
     */
    public String getCfgClientId() {
        return getMapAttr(options, CFG_CLIENT_ID);
    }

    /**
     * @return The configured issuer name
     */
    public String getCfgIssuerName() {
        return getMapAttr(options, CFG_ISSUER_NAME);
    }

    /**
     * @return The configured logout behaviour
     */
    public String getCfgLogoutBehaviour() {
        return getMapAttr(options, CFG_LOGOUT_BEHAVIOUR);
    }

    /**
     * @return The configured logout url
     */
    public String getCfgLogoutUrl() {
        return getMapAttr(options, CFG_LOGOUT_URL);
    }

    /**
     * @return The configured proxy url
     */
    public String getCfgProxyUrl() {
        return getMapAttr(options, CFG_PROXY_URL);
    }

    protected static URI getRedirectUri(String redirectUri) {
        if (redirectUri != null) {
            try {
                return new URI(redirectUri);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Invalid redirect URI", e);
            }
        }
        return null;
    }

    /**
     * @return The configured SMTP details
     */
    public Map<String,String> getSMTPConfig() {
        Map<String, String> config = new HashMap<String, String>();
        config.put(KEY_EMAIL_GWY_IMPL, getMapAttr(options, CFG_EMAIL_GATE_WAY));
        config.put(KEY_SMTP_HOSTNAME, getMapAttr(options, CFG_SMTP_HOST_NAME));
        config.put(KEY_SMTP_PORT, getMapAttr(options, CFG_SMTP_PORT));
        config.put(KEY_SMTP_USERNAME, getMapAttr(options, CFG_SMTP_USER_NAME));
        config.put(KEY_SMTP_PASSWORD, getMapAttr(options, CFG_SMTP_USER_PASSWORD));
        config.put(KEY_SMTP_SSL_ENABLED, getMapAttr(options, CFG_SMTP_SSL_ENABLED));
        return config;
    }
}
