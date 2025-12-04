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

import static com.sun.identity.authentication.util.ISAuthConstants.FULL_LOGIN_URL;
import static com.sun.identity.authentication.util.ISAuthConstants.LOGIN_IGNORE;
import static com.sun.identity.authentication.util.ISAuthConstants.LOGIN_SUCCEED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.oauth.clients.oauth2.OAuth2Client.ACCESS_TOKEN;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.COOKIE_LOGOUT_URL;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.COOKIE_ORIG_URL;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.CREATE_USER_STATE;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_EMAIL_GWY_IMPL;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_SMTP_HOSTNAME;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_SMTP_PASSWORD;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_SMTP_PORT;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_SMTP_SSL_ENABLED;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.KEY_SMTP_USERNAME;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.NONCE_TOKEN_ID;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.SESSION_LOGOUT_BEHAVIOUR;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.SESSION_OAUTH_TOKEN;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.SET_PASSWORD_STATE;
import static org.forgerock.openam.authentication.modules.social.AbstractSocialAuthLoginModule.CANCEL_ACTION_SELECTED;
import static org.forgerock.openam.authentication.modules.social.SocialAuthLoginModule.GET_OAUTH_TOKEN_STATE;
import static org.forgerock.openam.authentication.modules.social.SocialAuthLoginModule.RESUME_FROM_REGISTRATION_REDIRECT_STATE;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.json.JsonValue;
import org.forgerock.oauth.OAuthClient;
import org.forgerock.oauth.UserInfo;
import org.forgerock.openam.authentication.modules.common.AMLoginModuleBinder;
import org.forgerock.openam.authentication.modules.oauth2.EmailGateway;
import org.forgerock.openam.authentication.modules.oauth2.EmailGatewayLookup;
import org.forgerock.openam.authentication.modules.oauth2.NoEmailSentException;
import org.forgerock.openam.authentication.modules.oidc.JwtHandlerConfig;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.ClientTokenJwtGenerator;
import org.forgerock.openam.integration.idm.IdmIntegrationConfig;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.util.promise.Promises;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.RedirectCallback;
import com.sun.identity.authentication.util.ISAuthConstants;

/**
 * Test class for SocialAuthLoginModule.
 */
@ExtendWith(MockitoExtension.class)
public class SocialAuthLoginModuleTest {

    private static final Subject SUBJECT = null;
    private static final Map SHARED_STATE = new HashMap();

    private static final String ORIGINAL_URL = "http://originalUrl";
    private static final String HTTP_AUTH_REDIRECT_URI = "http://authRedirectUri";
    private static final String LOGOUT_URL = "http://logoutUrl";
    private static final String LOGOUT_BEHAVIOUR = "prompt";
    private static final String ACCESS_TOKEN_1 = "access_token_1";
    private static final String DOMAIN_1 = "domain1";
    private static final String DOMAIN_2 = "domain2";

    private SocialAuthLoginModule module;
    private ImmutableMap<String, Set<String>> options;

    @Mock
    private AMLoginModuleBinder binder;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private SocialAuthModuleHelper authModuleHelper;
    @Mock
    private Logger debug;
    @Mock
    private AbstractSmsSocialAuthConfiguration config;
    @Mock
    private OAuthClient client;
    @Mock
    private SharedStateDataStore dataStore;
    @Mock
    private RedirectCallback redirectCallback;
    @Mock
    private JwtHandlerConfig jwtHandlerConfig;
    @Mock
    private ProfileNormalizer profileNormalizer;
    @Mock
    private UserInfo userInfo;
    @Mock
    private JsonValue jsonValue;
    @Mock
    private Map requestParameters;
    @Mock
    private Function<Map, AbstractSmsSocialAuthConfiguration> configFunction;
    @Mock
    private ClientTokenJwtGenerator clientTokenJwtGenerator;
    @Mock
    private IdmIntegrationConfig idmConfigProvider;
    @Mock
    private IdmIntegrationConfig.GlobalConfig idmConfig;
    @Mock
    private Realm realm;
    @Mock
    private EmailGatewayLookup gatewayLookup;
    @Mock
    private EmailGateway emailGateway;

    private final ResourceBundle bundle = new ResourceBundle() {
        @Override
        protected Object handleGetObject(String key) {
            return "invalid password";
        }

        @Override
        public Enumeration<String> getKeys() {
            return Collections.emptyEnumeration();
        }
    };


    @BeforeEach
    void setup() throws Exception {
        lenient().when(request.getParameterMap()).thenReturn(requestParameters);

        lenient().when(authModuleHelper.getOriginalUrl(request)).thenReturn(ORIGINAL_URL);
        lenient().when(authModuleHelper.getCookieDomainsForRequest(request))
                .thenReturn(CollectionUtils.asSet(DOMAIN_1, DOMAIN_2));

        lenient().when(binder.getHttpServletRequest()).thenReturn(request);
        lenient().when(binder.getHttpServletResponse()).thenReturn(response);

        lenient().when(configFunction.apply(anyMap())).thenReturn(config);

        lenient().when(dataStore.retrieveData()).thenReturn(jsonValue);
        lenient().when(jsonValue.get(ACCESS_TOKEN)).thenReturn(JsonValue.json(ACCESS_TOKEN_1));
        lenient().when(config.getCfgLogoutUrl()).thenReturn(LOGOUT_URL);
        lenient().when(config.getCfgLogoutBehaviour()).thenReturn(LOGOUT_BEHAVIOUR);

        lenient().when(realm.asPath()).thenReturn("/");

        lenient().when(idmConfigProvider.global()).thenReturn(idmConfig);
        lenient().when(idmConfig.enabled()).thenReturn(true);
        lenient().when(idmConfig.idmDeploymentUrl()).thenReturn("IDM_URL");
        lenient().when(idmConfig.provisioningSigningKeyAlias()).thenReturn("test");
        lenient().when(idmConfig.provisioningEncryptionKeyAlias()).thenReturn("test");
        lenient().when(idmConfig.provisioningSigningAlgorithm()).thenReturn("HS256");
        lenient().when(idmConfig.provisioningEncryptionAlgorithm()).thenReturn("RSAES_PKCS1_V1_5");
        lenient().when(idmConfig.provisioningEncryptionMethod()).thenReturn("A128CBC_HS256");

        lenient().when(gatewayLookup.getEmailGateway(anyString())).thenReturn(emailGateway);


        this.module = new SocialAuthLoginModule(debug, authModuleHelper, configFunction, clientTokenJwtGenerator,
                idmConfigProvider);
        module.setAMLoginModule(binder);
    }

    @Test
    void shouldFailWhenSharedStateIsNull() throws Exception {
        //given
        this.options = ImmutableMap.of();
        Map sharedState = null;

        //when
        assertThatThrownBy(() -> module.init(SUBJECT, sharedState, options))
                //then
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldFailWhenOptionsIsNull() {
        //given
        this.options = null;

        //when
        assertThatThrownBy(() -> module.init(SUBJECT, SHARED_STATE, options))
                //then
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldProcessLoginStartReturnNextStage() throws Exception {
        final String dataStoreId = "data_store_id";
        final String path = "/";
        final String original_url_encoded = URLEncoder.encode(ORIGINAL_URL, StandardCharsets.UTF_8);

        //given
        given(client.getAuthRedirect(dataStore, null, null))
                .willReturn(Promises.newResultPromise(new URI(HTTP_AUTH_REDIRECT_URI)));

        given(redirectCallback.getStatusParameter()).willReturn("statusParam");
        given(redirectCallback.getRedirectBackUrlCookieName()).willReturn("cookieName");

        given(binder.getCallback(GET_OAUTH_TOKEN_STATE)).willReturn(new Callback[]{redirectCallback});

        given(dataStore.getId()).willReturn(dataStoreId);

        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle, gatewayLookup);

        //when
        int nextState = module.process(null, ISAuthConstants.LOGIN_START);

        //then
        verify(client, times(1)).getAuthRedirect(dataStore, null, null);

        verify(binder, times(1)).replaceCallback(eq(GET_OAUTH_TOKEN_STATE), eq(0), any(RedirectCallback.class));

        verify(authModuleHelper, times(1)).addCookieToResponse(response, COOKIE_ORIG_URL, original_url_encoded, path, DOMAIN_1);
        verify(authModuleHelper, times(1)).addCookieToResponse(response, COOKIE_ORIG_URL, original_url_encoded, path, DOMAIN_2);
        verify(authModuleHelper, times(1)).addCookieToResponse(response, NONCE_TOKEN_ID, dataStoreId, path, DOMAIN_1);
        verify(authModuleHelper, times(1)).addCookieToResponse(response, NONCE_TOKEN_ID, dataStoreId, path, DOMAIN_2);
        verify(authModuleHelper, times(1)).addCookieToResponse(response, COOKIE_LOGOUT_URL, LOGOUT_URL, path, DOMAIN_1);
        verify(authModuleHelper, times(1)).addCookieToResponse(response, COOKIE_LOGOUT_URL, LOGOUT_URL, path, DOMAIN_2);

        verify(binder, times(1)).setUserSessionProperty(eq(FULL_LOGIN_URL), eq(ORIGINAL_URL));

        assertThat(nextState).isEqualTo(GET_OAUTH_TOKEN_STATE);
    }

    @Test
    void shouldProcessOAuthTokenStateReturnLoginSucceedWhenUserAlreadyExists() throws Exception {
        final String user = "user1";

        //given
        given(client.handlePostAuth(eq(dataStore), anyMap())).willReturn(Promises.newResultPromise(jsonValue));
        given(client.getUserInfo(dataStore)).willReturn(Promises.newResultPromise(userInfo));

        given(authModuleHelper.userExistsInTheDataStore(anyString(), any(), anyMap()))
                .willReturn(Optional.of(user));

        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle, gatewayLookup);

        //when
        int nextState = module.process(null, GET_OAUTH_TOKEN_STATE);

        //then
        verify(binder, times(1)).storeUsernamePasswd(eq(user), isNull(String.class));

        assertThat(nextState).isEqualTo(LOGIN_SUCCEED);
        assertThat(module.getPrincipal().getName()).isEqualTo(user);
    }

    @Test
    void shouldProcessOAuthTokenStateReturnNextStateWhenConfiguredToUseRegistrationService() throws Exception {
        //given
        given(client.handlePostAuth(eq(dataStore), anyMap())).willReturn(Promises.newResultPromise(jsonValue));
        given(client.getUserInfo(dataStore)).willReturn(Promises.newResultPromise(userInfo));

        given(dataStore.retrieveData())
                .willReturn(JsonValue.json(JsonValue.object(JsonValue.field(ACCESS_TOKEN, "access_token_1"))));

        given(authModuleHelper.userExistsInTheDataStore(anyString(), any(), anyMap()))
                .willReturn(Optional.empty());

        given(config.getCfgCreateAccount()).willReturn(true);
        given(config.isCfgRegistrationServiceEnabled()).willReturn(true);

        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle, gatewayLookup);

        //when
        int nextState = module.process(null, GET_OAUTH_TOKEN_STATE);

        //then
        verify(binder, times(1))
                .replaceCallback(eq(RESUME_FROM_REGISTRATION_REDIRECT_STATE), eq(0), any(RedirectCallback.class));

        assertThat(nextState).isEqualTo(RESUME_FROM_REGISTRATION_REDIRECT_STATE);
    }

    @Test
    void shouldProcessOAuthTokenStateReturnLoginSucceedAfterProvisioningUserLocally() throws Exception {
        final String user = "user1";

        //given
        given(client.handlePostAuth(eq(dataStore), anyMap())).willReturn(Promises.newResultPromise(jsonValue));
        given(client.getUserInfo(dataStore)).willReturn(Promises.newResultPromise(userInfo));

        given(dataStore.retrieveData())
                .willReturn(JsonValue.json(JsonValue.object(JsonValue.field(ACCESS_TOKEN, "access_token_1"))));

        given(authModuleHelper.userExistsInTheDataStore(anyString(), any(), anyMap()))
                .willReturn(Optional.empty());
        given(authModuleHelper.provisionUser(anyString(), any(), anyMap())).willReturn(user);

        given(profileNormalizer.getNormalisedAttributes(userInfo, null))
                .willReturn(ImmutableMap.of("name", CollectionUtils.asSet(user)));

        given(config.getCfgCreateAccount()).willReturn(true);
        given(config.getSaveAttributesToSessionFlag()).willReturn(true);

        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle, gatewayLookup);

        //when
        int nextState = module.process(null, GET_OAUTH_TOKEN_STATE);

        //then
        verify(binder, times(1)).setUserSessionProperty(eq("name"), eq(user));
        verify(binder, times(1)).setUserSessionProperty(eq(SESSION_LOGOUT_BEHAVIOUR), eq(LOGOUT_BEHAVIOUR));
        verify(binder, times(1)).setUserSessionProperty(eq(SESSION_OAUTH_TOKEN), eq(ACCESS_TOKEN_1));
        verify(binder, times(1)).storeUsernamePasswd(eq(user), isNull(String.class));

        assertThat(nextState).isEqualTo(LOGIN_SUCCEED);
    }

    @Test
    void shouldProcessOAuthTokenStateReturnLoginSucceedWhenConfiguredToUseAnonymousUser() throws Exception {
        final String user = "user1";

        //given
        given(client.handlePostAuth(eq(dataStore), anyMap())).willReturn(Promises.newResultPromise(jsonValue));
        given(client.getUserInfo(dataStore)).willReturn(Promises.newResultPromise(userInfo));

        given(dataStore.retrieveData())
                .willReturn(JsonValue.json(JsonValue.object(JsonValue.field(ACCESS_TOKEN, "access_token_1"))));

        given(authModuleHelper.userExistsInTheDataStore(anyString(), any(), anyMap()))
                .willReturn(Optional.empty());
        lenient().when(authModuleHelper.provisionUser(anyString(), any(), anyMap())).thenReturn(user);

        lenient().when(profileNormalizer.getNormalisedAttributes(userInfo, null))
                .thenReturn(ImmutableMap.of("name", CollectionUtils.asSet(user)));

        given(config.getCfgCreateAccount()).willReturn(false);
        given(config.getSaveAttributesToSessionFlag()).willReturn(true);
        given(config.getMapToAnonymousUser()).willReturn(true);
        given(config.getAnonymousUserName()).willReturn(user);

        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle, gatewayLookup);

        //when
        int nextState = module.process(null, GET_OAUTH_TOKEN_STATE);

        //then
        verify(binder, times(1)).setUserSessionProperty(eq("name"), eq(user));
        verify(binder, times(1)).storeUsernamePasswd(eq(user), isNull(String.class));

        assertThat(nextState).isEqualTo(LOGIN_SUCCEED);
    }

    @Test
    void shouldProcessOAuthTokenStateReturnLoginSucceedWhenConfiguredToUseMappedUsername() throws Exception {
        final String user = "user1";

        //given
        given(client.handlePostAuth(eq(dataStore), anyMap())).willReturn(Promises.newResultPromise(jsonValue));
        given(client.getUserInfo(dataStore)).willReturn(Promises.newResultPromise(userInfo));

        given(dataStore.retrieveData())
                .willReturn(JsonValue.json(JsonValue.object(JsonValue.field(ACCESS_TOKEN, "access_token_1"))));

        given(authModuleHelper.userExistsInTheDataStore(anyString(), any(), anyMap()))
                .willReturn(Optional.empty());
        lenient().when(authModuleHelper.provisionUser(anyString(), any(), anyMap())).thenReturn(user);

        given(profileNormalizer.getNormalisedAttributes(userInfo, null))
                .willReturn(ImmutableMap.of("name", CollectionUtils.asSet(user)));
        given(profileNormalizer.getNormalisedAccountAttributes(userInfo, null))
                .willReturn(ImmutableMap.of("name", CollectionUtils.asSet(user)));

        given(config.getCfgCreateAccount()).willReturn(false);
        given(config.getSaveAttributesToSessionFlag()).willReturn(true);
        given(config.getMapToAnonymousUser()).willReturn(false);

        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle, gatewayLookup);

        //when
        int nextState = module.process(null, GET_OAUTH_TOKEN_STATE);

        //then
        verify(binder, times(1)).setUserSessionProperty(eq("name"), eq(user));
        verify(binder, times(1)).storeUsernamePasswd(eq(user), isNull(String.class));

        assertThat(nextState).isEqualTo(LOGIN_SUCCEED);
    }

    @Test
    void shouldSucceedWhenResumedFromRegistrationAndUserFound() throws Exception {
        //given
        given(authModuleHelper.userExistsInTheDataStore(anyString(),
                any(), anyMap())).willReturn(Optional.of("user"));

        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle, gatewayLookup);

        //when
        int nextState = module.process(null, RESUME_FROM_REGISTRATION_REDIRECT_STATE);

        //then
        assertThat(nextState).isEqualTo(LOGIN_SUCCEED);
    }

    @Test
    void shouldFailWhenResumedFromRegistrationAndUserNotFound() throws Exception {
        //given
        given(authModuleHelper.userExistsInTheDataStore(anyString(),
                any(), anyMap())).willReturn(Optional.empty());

        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle, gatewayLookup);

        //when
        assertThatThrownBy(() -> module.process(null, RESUME_FROM_REGISTRATION_REDIRECT_STATE))
                //then
                .isInstanceOf(AuthLoginException.class);
    }

    @Test
    void shouldReturnSetPasswordStateWhenConfiguredToPromptUser() throws Exception {
        //given
        given(client.handlePostAuth(eq(dataStore), anyMap())).willReturn(Promises.newResultPromise(jsonValue));
        given(client.getUserInfo(dataStore)).willReturn(Promises.newResultPromise(userInfo));

        lenient().when(dataStore.retrieveData())
                .thenReturn(JsonValue.json(JsonValue.object(JsonValue.field(ACCESS_TOKEN, "access_token_1"))));

        given(authModuleHelper.userExistsInTheDataStore(anyString(), any(), anyMap()))
                .willReturn(Optional.empty());

        given(config.getCfgCreateAccount()).willReturn(true);
        given(config.getCfgPromptForPassword()).willReturn(true);

        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle, gatewayLookup);

        //when
        int nextState = module.process(null, GET_OAUTH_TOKEN_STATE);

        //then
        assertThat(nextState).isEqualTo(SET_PASSWORD_STATE);
    }

    @Test
    void shouldReturnLoginIgnoreWhenUserCancelsPromptPassword() throws Exception {
        //given
        ConfirmationCallback callback = mock(ConfirmationCallback.class);
        given(callback.getSelectedIndex()).willReturn(CANCEL_ACTION_SELECTED);
        given(binder.getCallback(SET_PASSWORD_STATE)).willReturn(new Callback[]{callback, callback, callback});

        //when
        int nextState = module.process(null, SET_PASSWORD_STATE);

        //then
        assertThat(nextState).isEqualTo(LOGIN_IGNORE);
    }

    @Test
    void shouldErrorWhenInvalidPasswordProvided() throws Exception {
        //given
        ConfirmationCallback confirmationCallback = mock(ConfirmationCallback.class);
        PasswordCallback passwordCallback = mock(PasswordCallback.class);
        given(confirmationCallback.getSelectedIndex()).willReturn(0);
        given(passwordCallback.getPassword()).willReturn(null);
        given(binder.getCallback(SET_PASSWORD_STATE))
                .willReturn(new Callback[]{passwordCallback, passwordCallback, confirmationCallback});

        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle, gatewayLookup);

        //when
        int nextState = module.process(null, SET_PASSWORD_STATE);

        //then
        assertThat(nextState).isEqualTo(SET_PASSWORD_STATE);
        verify(binder, times(1)).substituteHeader(eq(SET_PASSWORD_STATE), eq("invalid password"));
    }

    @Test
    void shouldReturnCreateUserStateWhenPasswordProvided() throws Exception {
        //given
        ConfirmationCallback confirmationCallback = mock(ConfirmationCallback.class);
        PasswordCallback passwordCallback = mock(PasswordCallback.class);
        given(confirmationCallback.getSelectedIndex()).willReturn(0);
        given(passwordCallback.getPassword()).willReturn("newpassword".toCharArray());
        given(binder.getCallback(SET_PASSWORD_STATE))
                .willReturn(new Callback[]{passwordCallback, passwordCallback, confirmationCallback});
        given(binder.getCallback(SET_PASSWORD_STATE))
                .willReturn(new Callback[]{passwordCallback, passwordCallback, confirmationCallback});
        given(config.getCfgMailAttribute()).willReturn("email");
        Map<String, String> smtpConf = new HashMap<String, String>();
        smtpConf.put(KEY_EMAIL_GWY_IMPL,
                "org.forgerock.openam.authentication.modules.social.SocialAuthLoginModuleTest$DummyGateway");
        smtpConf.put(KEY_SMTP_HOSTNAME, "localhost");
        smtpConf.put(KEY_SMTP_PORT, "2525");
        smtpConf.put(KEY_SMTP_USERNAME, "user");
        smtpConf.put(KEY_SMTP_PASSWORD, "pass");
        smtpConf.put(KEY_SMTP_SSL_ENABLED, "false");
        given(config.getSMTPConfig()).willReturn(smtpConf);
        given(authModuleHelper.extractEmail(any(), anyString())).willReturn("info@forgerock.com");
        given(authModuleHelper.getRandomData()).willReturn("randomString");
        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle, gatewayLookup);

        //when
        int nextState = module.process(null, SET_PASSWORD_STATE);

        //then
        assertThat(nextState).isEqualTo(CREATE_USER_STATE);
    }

    @Test
    void shouldReturnLoginIgnoreWhenUserCancelsActivation() throws Exception {
        //given
        ConfirmationCallback callback = mock(ConfirmationCallback.class);
        given(callback.getSelectedIndex()).willReturn(CANCEL_ACTION_SELECTED);
        given(binder.getCallback(CREATE_USER_STATE)).willReturn(new Callback[]{callback, callback, callback});

        //when
        int nextState = module.process(null, CREATE_USER_STATE);

        //then
        assertThat(nextState).isEqualTo(LOGIN_IGNORE);
    }

    @Test
    void shouldSucceedWhenActivatedUserProvisioned() throws Exception {
        //given
        String user = "user1";
        given(config.getSaveAttributesToSessionFlag()).willReturn(true);
        ConfirmationCallback confirmationCallback = mock(ConfirmationCallback.class);
        NameCallback nameCallback = mock(NameCallback.class);
        given(confirmationCallback.getSelectedIndex()).willReturn(0);
        given(nameCallback.getName()).willReturn("activationCode");
        given(authModuleHelper.isValidActivationCodeReturned(any(), anyString())).willReturn(true);
        given(binder.getCallback(CREATE_USER_STATE))
                .willReturn(new Callback[]{nameCallback, nameCallback, confirmationCallback});
        given(authModuleHelper.provisionUser(anyString(), any(), anyMap())).willReturn(user);
        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle, gatewayLookup);

        //when
        int nextState = module.process(null, CREATE_USER_STATE);

        //then
        verify(binder, times(1)).storeUsernamePasswd(eq(user), isNull());
        assertThat(nextState).isEqualTo(LOGIN_SUCCEED);
    }

    public static class DummyGateway implements EmailGateway {

        @Override
        public void sendEmail(String from, String to, String subject,
                String message, Map<String, String> options) throws NoEmailSentException {
            //do nothing
        }
    }

}
