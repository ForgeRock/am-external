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

import static java.util.Collections.singletonList;
import static org.forgerock.oauth.clients.oauth2.OAuth2Client.ACCESS_TOKEN;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.CREATE_USER_STATE;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.SET_PASSWORD_STATE;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;

import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.exceptions.JwsException;
import org.forgerock.oauth.OAuthClient;
import org.forgerock.oauth.OAuthException;
import org.forgerock.oauth.UserInfo;
import org.forgerock.openam.integration.idm.ClientTokenJwtGenerator;
import org.forgerock.openam.integration.idm.IdmIntegrationService;

import com.google.common.annotations.VisibleForTesting;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.RedirectCallback;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.debug.Debug;

/**
 * Social Authentication Module.
 *
 * This auth module uses OAuthClient for social provider related business logic.
 *
 * @see OAuthClient
 */
public class SocialAuthLoginModule extends AbstractSocialAuthLoginModule {

    public final static int GET_OAUTH_TOKEN_STATE = 2;

    /**
     * Constructs a SocialAuthLoginModule instance.
     */
    SocialAuthLoginModule(Function<Map, AbstractSmsSocialAuthConfiguration> configurationFunction) {
        super(configurationFunction);
    }

    @VisibleForTesting
    SocialAuthLoginModule(Debug debug, SocialAuthModuleHelper authModuleHelper,
            Function<Map, AbstractSmsSocialAuthConfiguration> configurationFunction,
            ClientTokenJwtGenerator clientTokenJwtGenerator, IdmIntegrationService idmIntegrationService) {
        super(debug, authModuleHelper, configurationFunction, clientTokenJwtGenerator, idmIntegrationService);
    }

    @Override
    public int process(Callback[] callbacks, int state) throws LoginException {
        switch (state) {
            case ISAuthConstants.LOGIN_START: {
                return processLoginStart();
            }
            case GET_OAUTH_TOKEN_STATE: {
                return processOAuthTokenState();
            }
            case SET_PASSWORD_STATE: {
                return processSetPasswordState();
            }
            case CREATE_USER_STATE : {
                return processCreateUserState();
            }
            case RESUME_FROM_REGISTRATION_REDIRECT_STATE : {
                return processResumeFromRegistration();
            }
            default:
                throw new IllegalStateException("Unknown state " + state);
        }
    }

    @Override
    UserInfo getUserInfo() throws AuthLoginException {
        try {
            return getClient().handlePostAuth(getDataStore(), getRequestParameters())
                    .thenAsync(value -> getClient().getUserInfo(getDataStore())).getOrThrowUninterruptibly();
        } catch (OAuthException e) {
            throw new AuthLoginException("Unable to get UserInfo details", e);
        } catch (JwsException jse) {
            debug.error("JwsException: {}", jse.getMessage(), jse);
            throw jse;
        }
    }

    @Override
    String retrieveAccessToken() throws AuthLoginException {
        JsonValue accessToken = getDataStore().retrieveData().get(ACCESS_TOKEN);
        if (accessToken == null) {
            throw new AuthLoginException("Access token not found");
        }
        return accessToken.asString();
    }

    private int processLoginStart() throws LoginException {
        final int nextState = GET_OAUTH_TOKEN_STATE;
        prepareRedirectCallback(nextState);
        addDomainCookiesToResponse();
        addOriginalUrlToUserSession();
        return nextState;
    }

    private void prepareRedirectCallback(int nextState) throws LoginException {
        final int callbackIndex = 0;
        URI authRedirect = getAuthRedirectUri();
        Callback[] callbacks = getCallback(nextState);
        RedirectCallback rc = (RedirectCallback) callbacks[callbackIndex];
        RedirectCallback rcNew = new RedirectCallback(authRedirect.toString(), null, "GET", rc.getStatusParameter(),
                rc.getRedirectBackUrlCookieName());
        rcNew.setTrackingCookie(true);
        replaceCallback(nextState, callbackIndex, rcNew);
    }

    private URI getAuthRedirectUri() throws LoginException {
        try {
            return getClient().getAuthRedirect(getDataStore(), null, null).getOrThrow();
        } catch (InterruptedException | OAuthException e) {
            throw new LoginException(e.getMessage());
        }
    }

    private Map<String, List<String>> getRequestParameters() {
        Map<String, List<String>> parameters = new HashMap<>();
        for (Map.Entry<String, String[]> param : getHttpServletRequest().getParameterMap().entrySet()) {
            parameters.put(param.getKey(), singletonList(param.getValue()[0]));
        }
        return parameters;
    }
}
