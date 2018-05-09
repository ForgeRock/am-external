/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.social;

import static com.sun.identity.authentication.util.ISAuthConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.oauth.clients.oauth2.OAuth2Client.ACCESS_TOKEN;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.*;
import static org.forgerock.openam.authentication.modules.social.AbstractSocialAuthLoginModule.CANCEL_ACTION_SELECTED;
import static org.forgerock.openam.authentication.modules.social.SocialAuthLoginModule.GET_OAUTH_TOKEN_STATE;
import static org.forgerock.openam.authentication.modules.social.SocialAuthLoginModule.RESUME_FROM_REGISTRATION_REDIRECT_STATE;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Function;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.forgerock.guava.common.base.Optional;
import org.forgerock.guava.common.collect.ImmutableMap;
import org.forgerock.json.JsonValue;
import org.forgerock.oauth.OAuthClient;
import org.forgerock.oauth.UserInfo;
import org.forgerock.openam.authentication.modules.common.AMLoginModuleBinder;
import org.forgerock.openam.authentication.modules.oauth2.EmailGateway;
import org.forgerock.openam.authentication.modules.oauth2.NoEmailSentException;
import org.forgerock.openam.authentication.modules.oidc.JwtHandlerConfig;
import org.forgerock.openam.integration.idm.IdmIntegrationConfig;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.util.promise.Promises;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.RedirectCallback;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.debug.Debug;

/**
 * Test class for SocialAuthLoginModule.
 */
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
    private Debug debug;
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
    private Function<Map,AbstractSmsSocialAuthConfiguration> configFunction;
    @Mock
    private SocialAuthLoginModule.ClientTokenJwtGenerator clientTokenJwtGenerator;

    private ResourceBundle bundle = new ResourceBundle() {
        @Override
        protected Object handleGetObject(String key) {
            return "invalid password";
        }

        @Override
        public Enumeration<String> getKeys() {
            return Collections.emptyEnumeration();
        }


    };

    @BeforeMethod
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(request.getParameterMap()).thenReturn(requestParameters);

        when(authModuleHelper.getOriginalUrl(request)).thenReturn(ORIGINAL_URL);
        when(authModuleHelper.getCookieDomainsForRequest(request))
                .thenReturn(CollectionUtils.asSet(DOMAIN_1, DOMAIN_2));

        when(binder.getHttpServletRequest()).thenReturn(request);
        when(binder.getHttpServletResponse()).thenReturn(response);

        when(configFunction.apply(anyMap())).thenReturn(config);

        when(dataStore.retrieveData()).thenReturn(jsonValue);
        when(jsonValue.get(ACCESS_TOKEN)).thenReturn(JsonValue.json(ACCESS_TOKEN_1));
        when(config.getCfgLogoutUrl()).thenReturn(LOGOUT_URL);
        when(config.getCfgLogoutBehaviour()).thenReturn(LOGOUT_BEHAVIOUR);

        Map<String, Set<String>> idmIntegrationConfigAttributes = new HashMap<>();
        idmIntegrationConfigAttributes.put("enabled", Collections.singleton(Boolean.TRUE.toString()));
        idmIntegrationConfigAttributes.put("idmDeploymentUrl", Collections.singleton("IDM_URL"));
        idmIntegrationConfigAttributes.put("provisioningSigningKeyAlias", Collections.singleton("test"));
        idmIntegrationConfigAttributes.put("provisioningEncryptionKeyAlias", Collections.singleton("test"));
        idmIntegrationConfigAttributes.put("provisioningSigningAlgorithm", Collections.singleton("HS256"));
        idmIntegrationConfigAttributes.put("provisioningEncryptionAlgorithm", Collections.singleton("RSAES_PKCS1_V1_5"));
        idmIntegrationConfigAttributes.put("provisioningEncryptionMethod", Collections.singleton("A128CBC_HS256"));
        IdmIntegrationService idmIntegrationService = mock(IdmIntegrationService.class);
        given(idmIntegrationService.getConfig("/")).willReturn(new IdmIntegrationConfig(idmIntegrationConfigAttributes));

        this.module = new SocialAuthLoginModule(debug, authModuleHelper, configFunction, clientTokenJwtGenerator,
                idmIntegrationService);
        module.setAMLoginModule(binder);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldFailWhenSharedStateIsNull() throws Exception {
        //given
        this.options = ImmutableMap.of();
        Map sharedState = null;

        //when
        module.init(SUBJECT, sharedState, options);

        //then Exception
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldFailWhenOptionsIsNull() throws Exception {
        //given
        this.options = null;

        //when
        module.init(SUBJECT, SHARED_STATE, options);

        //then Exception
    }

    @Test
    public void shouldProcessLoginStartReturnNextStage() throws Exception {
        final String dataStoreId = "data_store_id";
        final String path = "/";

        //given
        given(client.getAuthRedirect(dataStore, null, null))
                .willReturn(Promises.newResultPromise(new URI(HTTP_AUTH_REDIRECT_URI)));

        given(redirectCallback.getStatusParameter()).willReturn("statusParam");
        given(redirectCallback.getRedirectBackUrlCookieName()).willReturn("cookieName");

        given(binder.getCallback(GET_OAUTH_TOKEN_STATE)).willReturn(new Callback[]{redirectCallback});

        given(dataStore.getId()).willReturn(dataStoreId);

        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle);

        //when
        int nextState = module.process(null, ISAuthConstants.LOGIN_START);

        //then
        verify(client, times(1)).getAuthRedirect(dataStore, null, null);

        verify(binder, times(1)).replaceCallback(eq(GET_OAUTH_TOKEN_STATE), eq(0), any(RedirectCallback.class));

        verify(authModuleHelper, times(1)).addCookieToResponse(response, COOKIE_ORIG_URL, ORIGINAL_URL, path, DOMAIN_1);
        verify(authModuleHelper, times(1)).addCookieToResponse(response, COOKIE_ORIG_URL, ORIGINAL_URL, path, DOMAIN_2);
        verify(authModuleHelper, times(1)).addCookieToResponse(response, NONCE_TOKEN_ID, dataStoreId, path, DOMAIN_1);
        verify(authModuleHelper, times(1)).addCookieToResponse(response, NONCE_TOKEN_ID, dataStoreId, path, DOMAIN_2);
        verify(authModuleHelper, times(1)).addCookieToResponse(response, COOKIE_LOGOUT_URL, LOGOUT_URL, path, DOMAIN_1);
        verify(authModuleHelper, times(1)).addCookieToResponse(response, COOKIE_LOGOUT_URL, LOGOUT_URL, path, DOMAIN_2);

        verify(binder, times(1)).setUserSessionProperty(eq(FULL_LOGIN_URL), eq(ORIGINAL_URL));

        assertThat(nextState).isEqualTo(GET_OAUTH_TOKEN_STATE);
    }

    @Test
    public void shouldProcessOAuthTokenStateReturnLoginSucceedWhenUserAlreadyExists() throws Exception {
        final String user = "user1";

        //given
        given(client.handlePostAuth(eq(dataStore), anyMap())).willReturn(Promises.newResultPromise(jsonValue));
        given(client.getUserInfo(dataStore)).willReturn(Promises.newResultPromise(userInfo));

        given(authModuleHelper.userExistsInTheDataStore(anyString(), any(), anyMap()))
                .willReturn(Optional.of(user));

        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle);

        //when
        int nextState = module.process(null, GET_OAUTH_TOKEN_STATE);

        //then
        verify(binder, times(1)).storeUsernamePasswd(eq(user), isNull(String.class));

        assertThat(nextState).isEqualTo(LOGIN_SUCCEED);
        assertThat(module.getPrincipal().getName()).isEqualTo(user);
    }

    @Test
    public void shouldProcessOAuthTokenStateReturnNextStateWhenConfiguredToUseRegistrationService() throws Exception {
        //given
        given(client.handlePostAuth(eq(dataStore), anyMap())).willReturn(Promises.newResultPromise(jsonValue));
        given(client.getUserInfo(dataStore)).willReturn(Promises.newResultPromise(userInfo));

        given(dataStore.retrieveData())
                .willReturn(JsonValue.json(JsonValue.object(JsonValue.field(ACCESS_TOKEN, "access_token_1"))));

        given(authModuleHelper.userExistsInTheDataStore(anyString(), any(), anyMap()))
                .willReturn(Optional.absent());

        given(config.getCfgCreateAccount()).willReturn(true);
        given(config.isCfgRegistrationServiceEnabled()).willReturn(true);

        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle);

        //when
        int nextState = module.process(null, GET_OAUTH_TOKEN_STATE);

        //then
        verify(binder, times(1))
                .replaceCallback(eq(RESUME_FROM_REGISTRATION_REDIRECT_STATE), eq(0), any(RedirectCallback.class));

        assertThat(nextState).isEqualTo(RESUME_FROM_REGISTRATION_REDIRECT_STATE);
    }

    @Test
    public void shouldProcessOAuthTokenStateReturnLoginSucceedAfterProvisioningUserLocally() throws Exception {
        final String user = "user1";

        //given
        given(client.handlePostAuth(eq(dataStore), anyMap())).willReturn(Promises.newResultPromise(jsonValue));
        given(client.getUserInfo(dataStore)).willReturn(Promises.newResultPromise(userInfo));

        given(dataStore.retrieveData())
                .willReturn(JsonValue.json(JsonValue.object(JsonValue.field(ACCESS_TOKEN, "access_token_1"))));

        given(authModuleHelper.userExistsInTheDataStore(anyString(), any(), anyMap()))
                .willReturn(Optional.absent());
        given(authModuleHelper.provisionUser(anyString(), any(), anyMap())).willReturn(user);

        given(profileNormalizer.getNormalisedAttributes(userInfo, null))
                .willReturn(ImmutableMap.of("name", CollectionUtils.asSet(user)));

        given(config.getCfgCreateAccount()).willReturn(true);
        given(config.getSaveAttributesToSessionFlag()).willReturn(true);

        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle);

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
    public void shouldProcessOAuthTokenStateReturnLoginSucceedWhenConfiguredToUseAnonymousUser() throws Exception {
        final String user = "user1";

        //given
        given(client.handlePostAuth(eq(dataStore), anyMap())).willReturn(Promises.newResultPromise(jsonValue));
        given(client.getUserInfo(dataStore)).willReturn(Promises.newResultPromise(userInfo));

        given(dataStore.retrieveData())
                .willReturn(JsonValue.json(JsonValue.object(JsonValue.field(ACCESS_TOKEN, "access_token_1"))));

        given(authModuleHelper.userExistsInTheDataStore(anyString(), any(), anyMap()))
                .willReturn(Optional.absent());
        given(authModuleHelper.provisionUser(anyString(), any(), anyMap())).willReturn(user);

        given(profileNormalizer.getNormalisedAttributes(userInfo, null))
                .willReturn(ImmutableMap.of("name", CollectionUtils.asSet(user)));

        given(config.getCfgCreateAccount()).willReturn(false);
        given(config.getSaveAttributesToSessionFlag()).willReturn(true);
        given(config.getMapToAnonymousUser()).willReturn(true);
        given(config.getAnonymousUserName()).willReturn(user);

        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle);

        //when
        int nextState = module.process(null, GET_OAUTH_TOKEN_STATE);

        //then
        verify(binder, times(1)).setUserSessionProperty(eq("name"), eq(user));
        verify(binder, times(1)).storeUsernamePasswd(eq(user), isNull(String.class));

        assertThat(nextState).isEqualTo(LOGIN_SUCCEED);
    }

    @Test
    public void shouldProcessOAuthTokenStateReturnLoginSucceedWhenConfiguredToUseMappedUsername() throws Exception {
        final String user = "user1";

        //given
        given(client.handlePostAuth(eq(dataStore), anyMap())).willReturn(Promises.newResultPromise(jsonValue));
        given(client.getUserInfo(dataStore)).willReturn(Promises.newResultPromise(userInfo));

        given(dataStore.retrieveData())
                .willReturn(JsonValue.json(JsonValue.object(JsonValue.field(ACCESS_TOKEN, "access_token_1"))));

        given(authModuleHelper.userExistsInTheDataStore(anyString(), any(), anyMap()))
                .willReturn(Optional.absent());
        given(authModuleHelper.provisionUser(anyString(), any(), anyMap())).willReturn(user);

        given(profileNormalizer.getNormalisedAttributes(userInfo, null))
                .willReturn(ImmutableMap.of("name", CollectionUtils.asSet(user)));
        given(profileNormalizer.getNormalisedAccountAttributes(userInfo, null))
                .willReturn(ImmutableMap.of("name", CollectionUtils.asSet(user)));

        given(config.getCfgCreateAccount()).willReturn(false);
        given(config.getSaveAttributesToSessionFlag()).willReturn(true);
        given(config.getMapToAnonymousUser()).willReturn(false);

        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle);

        //when
        int nextState = module.process(null, GET_OAUTH_TOKEN_STATE);

        //then
        verify(binder, times(1)).setUserSessionProperty(eq("name"), eq(user));
        verify(binder, times(1)).storeUsernamePasswd(eq(user), isNull(String.class));

        assertThat(nextState).isEqualTo(LOGIN_SUCCEED);
    }

    @Test
    public void shouldSucceedWhenResumedFromRegistrationAndUserFound() throws Exception {
        //given
        given(authModuleHelper.userExistsInTheDataStore(anyString(),
                any(), anyMap())).willReturn(Optional.of("user"));

        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle);

        //when
        int nextState = module.process(null, RESUME_FROM_REGISTRATION_REDIRECT_STATE);

        //then
        assertThat(nextState).isEqualTo(LOGIN_SUCCEED);
    }

    @Test(expectedExceptions = AuthLoginException.class)
    public void shouldFailWhenResumedFromRegistrationAndUserNotFound() throws Exception {
        //given
        given(authModuleHelper.userExistsInTheDataStore(anyString(),
                any(), anyMap())).willReturn(Optional.absent());

        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle);

        //when
        module.process(null, RESUME_FROM_REGISTRATION_REDIRECT_STATE);

        //then Exception
    }

    @Test
    public void shouldReturnSetPasswordStateWhenConfiguredToPromptUser() throws Exception {
        //given
        given(client.handlePostAuth(eq(dataStore), anyMap())).willReturn(Promises.newResultPromise(jsonValue));
        given(client.getUserInfo(dataStore)).willReturn(Promises.newResultPromise(userInfo));

        given(dataStore.retrieveData())
                .willReturn(JsonValue.json(JsonValue.object(JsonValue.field(ACCESS_TOKEN, "access_token_1"))));

        given(authModuleHelper.userExistsInTheDataStore(anyString(), any(), anyMap()))
                .willReturn(Optional.absent());

        given(config.getCfgCreateAccount()).willReturn(true);
        given(config.getCfgPromptForPassword()).willReturn(true);

        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle);

        //when
        int nextState = module.process(null, GET_OAUTH_TOKEN_STATE);

        //then
        assertThat(nextState).isEqualTo(SET_PASSWORD_STATE);
    }

    @Test
    public void shouldReturnLoginIgnoreWhenUserCancelsPromptPassword() throws Exception {
        //given
        ConfirmationCallback callback = mock(ConfirmationCallback.class);
        given(callback.getSelectedIndex()).willReturn(CANCEL_ACTION_SELECTED);
        given(binder.getCallback(SET_PASSWORD_STATE)).willReturn(new Callback[] {callback, callback, callback});

        //when
        int nextState = module.process(null, SET_PASSWORD_STATE);

        //then
        assertThat(nextState).isEqualTo(LOGIN_IGNORE);
    }

    @Test
    public void shouldErrorWhenInvalidPasswordProvided() throws Exception {
        //given
        ConfirmationCallback confirmationCallback = mock(ConfirmationCallback.class);
        PasswordCallback passwordCallback = mock(PasswordCallback.class);
        given(confirmationCallback.getSelectedIndex()).willReturn(0);
        given(passwordCallback.getPassword()).willReturn(null);
        given(binder.getCallback(SET_PASSWORD_STATE))
                .willReturn(new Callback[] {passwordCallback, passwordCallback, confirmationCallback});

        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle);

        //when
        int nextState = module.process(null, SET_PASSWORD_STATE);

        //then
        assertThat(nextState).isEqualTo(SET_PASSWORD_STATE);
        verify(binder, times(1)).substituteHeader(eq(SET_PASSWORD_STATE), eq("invalid password"));
    }

    @Test
    public void shouldReturnCreateUserStateWhenPasswordProvided() throws Exception {
        //given
        ConfirmationCallback confirmationCallback = mock(ConfirmationCallback.class);
        PasswordCallback passwordCallback = mock(PasswordCallback.class);
        given(confirmationCallback.getSelectedIndex()).willReturn(0);
        given(passwordCallback.getPassword()).willReturn("newpassword".toCharArray());
        given(binder.getCallback(SET_PASSWORD_STATE))
                .willReturn(new Callback[] {passwordCallback, passwordCallback, confirmationCallback});
        given(binder.getCallback(SET_PASSWORD_STATE))
                .willReturn(new Callback[] {passwordCallback, passwordCallback, confirmationCallback});
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
        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle);

        //when
        int nextState = module.process(null, SET_PASSWORD_STATE);

        //then
        assertThat(nextState).isEqualTo(CREATE_USER_STATE);
    }

    @Test
    public void shouldReturnLoginIgnoreWhenUserCancelsActivation() throws Exception {
        //given
        ConfirmationCallback callback = mock(ConfirmationCallback.class);
        given(callback.getSelectedIndex()).willReturn(CANCEL_ACTION_SELECTED);
        given(binder.getCallback(CREATE_USER_STATE)).willReturn(new Callback[] {callback, callback, callback});

        //when
        int nextState = module.process(null, CREATE_USER_STATE);

        //then
        assertThat(nextState).isEqualTo(LOGIN_IGNORE);
    }

    @Test
    public void shouldSucceedWhenActivatedUserProvisioned() throws Exception {
        //given
        String user = "user1";
        given(config.getSaveAttributesToSessionFlag()).willReturn(true);
        ConfirmationCallback confirmationCallback = mock(ConfirmationCallback.class);
        NameCallback nameCallback = mock(NameCallback.class);
        given(confirmationCallback.getSelectedIndex()).willReturn(0);
        given(nameCallback.getName()).willReturn("activationCode");
        given(authModuleHelper.getRandomData()).willReturn("activationCode");
        given(authModuleHelper.isValidActivationCodeReturned(any(), anyString())).willReturn(true);
        given(binder.getCallback(CREATE_USER_STATE))
                .willReturn(new Callback[] {nameCallback, nameCallback, confirmationCallback});
        given(profileNormalizer.getNormalisedAttributes(userInfo, null))
                .willReturn(ImmutableMap.of("name", CollectionUtils.asSet(user)));
        given(authModuleHelper.provisionUser(anyString(), any(), anyMap())).willReturn(user);
        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle);

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