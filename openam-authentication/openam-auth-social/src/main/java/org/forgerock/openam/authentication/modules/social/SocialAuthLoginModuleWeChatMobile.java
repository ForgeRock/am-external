/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.social;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.oauth.clients.oauth2.OAuth2Client.ACCESS_TOKEN;
import static org.forgerock.oauth.clients.oauth2.OAuth2Client.AUTHORIZATION_HEADER;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.CREATE_USER_STATE;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.SET_PASSWORD_STATE;

import java.util.Map;
import java.util.function.Function;

import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.forgerock.guava.common.annotations.VisibleForTesting;
import org.forgerock.json.JsonValue;
import org.forgerock.oauth.OAuthClient;
import org.forgerock.oauth.OAuthException;
import org.forgerock.oauth.UserInfo;
import org.forgerock.openam.integration.idm.IdmIntegrationService;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.debug.Debug;

/**
 * Social Authentication Module that can accept access token and complete the authentication flow.
 * <p>
 * This auth module uses OAuthClient for social provider related business logic.
 *
 * @see OAuthClient
 */
public class SocialAuthLoginModuleWeChatMobile extends AbstractSocialAuthLoginModule {

    static final String OPENID = "openid";

    /**
     * Constructs a SocialAuthLoginModuleWeChatMobile instance.
     */
    SocialAuthLoginModuleWeChatMobile(Function<Map, AbstractSmsSocialAuthConfiguration> configurationFunction) {
        super(configurationFunction);
    }

    @VisibleForTesting
    SocialAuthLoginModuleWeChatMobile(Debug debug, SocialAuthModuleHelper authModuleHelper,
            Function<Map, AbstractSmsSocialAuthConfiguration> configurationFunction,
            ClientTokenJwtGenerator clientTokenJwtGenerator, IdmIntegrationService idmIntegrationService) {
        super(debug, authModuleHelper, configurationFunction, clientTokenJwtGenerator, idmIntegrationService);
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
            return getClient().getUserInfo(dataStore).getOrThrowUninterruptibly();
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
        return header.substring("Bearer".length()).trim();
    }

    private String retrieveOpenId() throws AuthLoginException {
        HttpServletRequest request = getHttpServletRequest();
        String openId = request.getParameter(OPENID);
        if (StringUtils.isBlank(openId)) {
            throw new AuthLoginException("Unable to retrieve WeChat OpenId");
        }
        return openId;
    }
}
