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

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.oauth.clients.oauth2.OAuth2Client.ACCESS_TOKEN;
import static org.forgerock.oauth.clients.oauth2.OAuth2Client.AUTHORIZATION_HEADER;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.CREATE_USER_STATE;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.SET_PASSWORD_STATE;

import java.util.Map;
import java.util.function.Function;

import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.JsonValue;
import org.forgerock.oauth.OAuthClient;
import org.forgerock.oauth.OAuthException;
import org.forgerock.oauth.UserInfo;
import org.forgerock.openam.authentication.modules.oauth2.OAuthUtil;
import org.forgerock.openam.integration.idm.ClientTokenJwtGenerator;
import org.forgerock.openam.integration.idm.IdmIntegrationConfig;
import org.forgerock.openam.shared.security.crypto.Fingerprints;
import org.slf4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import org.slf4j.LoggerFactory;

/**
 * Social Authentication Module that can accept access token and complete the authentication flow.
 * <p>
 * This auth module uses OAuthClient for social provider related business logic.
 *
 * @see OAuthClient
 */
public class SocialAuthLoginModuleWeChatMobile extends AbstractSocialAuthLoginModule {

    private static final Logger logger = LoggerFactory.getLogger(SocialAuthLoginModuleWeChatMobile.class);
    static final String OPENID = "openid";

    /**
     * Constructs a SocialAuthLoginModuleWeChatMobile instance.
     */
    SocialAuthLoginModuleWeChatMobile(Function<Map, AbstractSmsSocialAuthConfiguration> configurationFunction) {
        super(configurationFunction);
    }

    @VisibleForTesting
    SocialAuthLoginModuleWeChatMobile(Logger debug, SocialAuthModuleHelper authModuleHelper,
            Function<Map, AbstractSmsSocialAuthConfiguration> configurationFunction,
            ClientTokenJwtGenerator clientTokenJwtGenerator, IdmIntegrationConfig idmConfigProvider) {
        super(debug, authModuleHelper, configurationFunction, clientTokenJwtGenerator, idmConfigProvider);
    }

    @Override
    public int process(Callback[] callbacks, int state) throws LoginException {
        switch (state) {
            case ISAuthConstants.LOGIN_START: {
                addDomainCookiesToResponse();
                addOriginalUrlToUserSession();
                return processOAuthTokenState();
            }
            case SET_PASSWORD_STATE: {
                return processSetPasswordState();
            }
            case CREATE_USER_STATE : {
                return processCreateUserState();
            }
            case RESUME_FROM_REGISTRATION_REDIRECT_STATE:
                return processResumeFromRegistration();
            default:
                throw new IllegalStateException("Unknown state " + state);
        }
    }

    @Override
    UserInfo getUserInfo() throws AuthLoginException {
        try {
            JsonValue data = json(object(
                field(ACCESS_TOKEN, retrieveAccessToken()),
                field(OPENID, retrieveOpenId())));
            SharedStateDataStore dataStore = getDataStore();
            dataStore.storeData(data);
            UserInfo userInfo = getClient().getUserInfo(dataStore).getOrThrowUninterruptibly();
            if (userInfo != null) {
                logger.debug("Retrieved user info for '{}'", userInfo.getSubject());
            }
            return userInfo;
        } catch (OAuthException e) {
            throw new AuthLoginException("Unable to get UserInfo details", e);
        }
    }

    @Override
    String retrieveAccessToken() throws AuthLoginException {
        HttpServletRequest request = getHttpServletRequest();
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.isBlank(header)) {
            throw new AuthLoginException("Unable to retrieve access token from header");
        }
        String accessToken = header.substring("Bearer".length()).trim();
        logger.debug("Retrieved access token Fingerprint:'{}'", Fingerprints.generate(accessToken));
        return accessToken;
    }

    private String retrieveOpenId() throws AuthLoginException {
        HttpServletRequest request = getHttpServletRequest();
        String openId = request.getParameter(OPENID);
        if (StringUtils.isBlank(openId)) {
            throw new AuthLoginException("Unable to retrieve WeChat OpenId");
        }
        logger.debug("Retrieved OIDC token Fingerprint:'{}'", Fingerprints.generate(openId));
        return openId;
    }
}
