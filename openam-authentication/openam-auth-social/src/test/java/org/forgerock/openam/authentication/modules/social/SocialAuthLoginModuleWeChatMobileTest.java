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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.authentication.modules.social;

import static com.sun.identity.authentication.util.ISAuthConstants.FULL_LOGIN_URL;
import static com.sun.identity.authentication.util.ISAuthConstants.LOGIN_SUCCEED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.oauth.clients.oauth2.OAuth2Client.AUTHORIZATION_HEADER;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.COOKIE_ORIG_URL;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.NONCE_TOKEN_ID;
import static org.forgerock.openam.authentication.modules.social.SocialAuthLoginModule.RESUME_FROM_REGISTRATION_REDIRECT_STATE;
import static org.forgerock.openam.authentication.modules.social.SocialAuthLoginModuleWeChatMobile.OPENID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Function;

import javax.security.auth.Subject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.oauth.OAuthClient;
import org.forgerock.oauth.UserInfo;
import org.forgerock.openam.authentication.modules.common.AMLoginModuleBinder;
import org.forgerock.openam.authentication.modules.oauth2.EmailGateway;
import org.forgerock.openam.authentication.modules.oauth2.EmailGatewayLookup;
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
 * Test class for SocialAuthLoginModuleWeChatMobile.
 */
@ExtendWith({MockitoExtension.class})
public class SocialAuthLoginModuleWeChatMobileTest {

    private static final String ORIGINAL_URL = "http://originalUrl";
    private static final String DOMAIN_1 = "domain1";
    private static final String DOMAIN_2 = "domain2";
    private static final Subject SUBJECT = null;
    private static final Map SHARED_STATE = new HashMap();
    private ImmutableMap<String, Set<String>> options;
    private SocialAuthLoginModuleWeChatMobile module;

    @Mock
    private Logger debug;
    @Mock
    private SocialAuthModuleHelper authModuleHelper;
    @Mock
    private Function<Map, AbstractSmsSocialAuthConfiguration> configFunction;
    @Mock
    private OAuthClient client;
    @Mock
    private AMLoginModuleBinder binder;
    @Mock
    private SharedStateDataStore dataStore;
    @Mock
    private JwtHandlerConfig jwtHandlerConfig;
    @Mock
    private ProfileNormalizer profileNormalizer;
    @Mock
    private AbstractSmsSocialAuthConfiguration config;
    @Mock
    private UserInfo userInfo;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private ClientTokenJwtGenerator clientTokenJwtGenerator;
    @Mock
    private ResourceBundle bundle;
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

    @BeforeEach
    void setup() throws Exception {
        lenient().when(authModuleHelper.getOriginalUrl(request)).thenReturn(ORIGINAL_URL);
        lenient().when(authModuleHelper.getCookieDomainsForRequest(request))
                .thenReturn(CollectionUtils.asSet(DOMAIN_1, DOMAIN_2));

        lenient().when(binder.getHttpServletRequest()).thenReturn(request);
        lenient().when(binder.getHttpServletResponse()).thenReturn(response);
        lenient().when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn("Bearer token");
        lenient().when(request.getParameter(OPENID)).thenReturn("OPENID");

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

        module = new SocialAuthLoginModuleWeChatMobile(debug, authModuleHelper, configFunction, clientTokenJwtGenerator,
                idmConfigProvider);
        module.setAMLoginModule(binder);
    }

    @Test
    void shouldFailWhenSharedStateIsNull() {
        //given
        this.options = ImmutableMap.of();
        Map sharedState = null;

        //when
        assertThatThrownBy(() -> module.init(SUBJECT, sharedState, options))
                //then
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldFailWhenOptionsIsNull() throws Exception {
        //given
        this.options = null;

        //when
        assertThatThrownBy(() -> module.init(SUBJECT, SHARED_STATE, options))
                //then
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldAddDomainCookiesToResponse() throws Exception {
        //given
        String path = "/";
        String dataStoreId = "data_store_id";
        final String original_url_encoded = URLEncoder.encode(ORIGINAL_URL, StandardCharsets.UTF_8);
        given(dataStore.getId()).willReturn(dataStoreId);
        given(client.getUserInfo(dataStore)).willReturn(Promises.newResultPromise(userInfo));
        given(authModuleHelper.userExistsInTheDataStore(anyString(), any(), anyMap()))
                .willReturn(Optional.of("user"));
        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle, gatewayLookup);

        //when
        module.process(null, ISAuthConstants.LOGIN_START);

        //then
        verify(authModuleHelper, times(1)).addCookieToResponse(response, COOKIE_ORIG_URL, original_url_encoded, path, DOMAIN_1);
        verify(authModuleHelper, times(1)).addCookieToResponse(response, COOKIE_ORIG_URL, original_url_encoded, path, DOMAIN_2);
        verify(authModuleHelper, times(1)).addCookieToResponse(response, NONCE_TOKEN_ID, dataStoreId, path, DOMAIN_1);
        verify(authModuleHelper, times(1)).addCookieToResponse(response, NONCE_TOKEN_ID, dataStoreId, path, DOMAIN_2);
    }

    @Test
    void shouldAddOriginalUrlToSession() throws Exception {
        //given
        given(client.getUserInfo(dataStore)).willReturn(Promises.newResultPromise(userInfo));
        given(authModuleHelper.userExistsInTheDataStore(anyString(), any(), anyMap()))
                .willReturn(Optional.of("user"));
        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle, gatewayLookup);

        //when
        module.process(null, ISAuthConstants.LOGIN_START);

        //then
        verify(binder, times(1)).setUserSessionProperty(eq(FULL_LOGIN_URL), eq(ORIGINAL_URL));
    }

    @Test
    void shouldFailWhenAccessTokenNotPresent() throws Exception {
        //given
        given(request.getHeader(AUTHORIZATION_HEADER)).willReturn(null);
        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle, gatewayLookup);

        //when
        assertThatThrownBy(() -> module.process(null, ISAuthConstants.LOGIN_START))
                //then
                .isInstanceOf(AuthLoginException.class)
                .hasMessage("Unable to retrieve access token from header");
    }

    @Test
    void shouldFailWhenOpenIdNotPresent() throws Exception {
        //given
        given(request.getParameter(OPENID)).willReturn(null);
        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle, gatewayLookup);

        //when
        assertThatThrownBy(() -> module.process(null, ISAuthConstants.LOGIN_START))
                //then
                .isInstanceOf(AuthLoginException.class)
                .hasMessage("Unable to retrieve WeChat OpenId");
    }


    @Test
    void shouldSucceedWhenUserIsPresent() throws Exception {
        //given
        given(client.getUserInfo(dataStore)).willReturn(Promises.newResultPromise(userInfo));
        given(authModuleHelper.userExistsInTheDataStore(anyString(), any(), anyMap()))
                .willReturn(Optional.of("user"));
        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle, gatewayLookup);

        //when
        int nextState = module.process(null, ISAuthConstants.LOGIN_START);

        //then
        verify(binder, times(1)).storeUsernamePasswd(eq("user"), isNull(String.class));
        assertThat(nextState).isEqualTo(ISAuthConstants.LOGIN_SUCCEED);
    }

    @Test
    void shouldSaveAttributesToSessionOnSuccessWhenConfigured() throws Exception {
        //given
        given(client.getUserInfo(dataStore)).willReturn(Promises.newResultPromise(userInfo));
        given(authModuleHelper.userExistsInTheDataStore(anyString(), any(), anyMap()))
                .willReturn(Optional.of("user"));
        given(config.getSaveAttributesToSessionFlag()).willReturn(true);
        given(profileNormalizer.getNormalisedAttributes(userInfo, null))
                .willReturn(ImmutableMap.of("name", CollectionUtils.asSet("user")));
        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle, gatewayLookup);

        //when
        module.process(null, ISAuthConstants.LOGIN_START);

        //then
        verify(binder, times(1)).setUserSessionProperty(eq("name"), eq("user"));
    }

    @Test
    void shouldReturnResumeRegistrationStateWhenConfiguredToUseRegistrationService() throws Exception {
        //given
        given(client.getUserInfo(dataStore)).willReturn(Promises.newResultPromise(userInfo));
        given(authModuleHelper.userExistsInTheDataStore(anyString(), any(), anyMap()))
                .willReturn(Optional.empty());
        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle, gatewayLookup);
        given(config.getCfgCreateAccount()).willReturn(true);
        given(config.isCfgRegistrationServiceEnabled()).willReturn(true);

        //when
        int nextState = module.process(null, ISAuthConstants.LOGIN_START);

        //then
        verify(binder, times(1))
                .replaceCallback(eq(RESUME_FROM_REGISTRATION_REDIRECT_STATE), eq(0), any(RedirectCallback.class));
        assertThat(nextState).isEqualTo(RESUME_FROM_REGISTRATION_REDIRECT_STATE);
    }

    @Test
    void shouldSucceedWhenConfiguredToProvisionLocally() throws Exception {
        //given
        given(client.getUserInfo(dataStore)).willReturn(Promises.newResultPromise(userInfo));
        given(authModuleHelper.userExistsInTheDataStore(anyString(), any(), anyMap()))
                .willReturn(Optional.empty());
        given(authModuleHelper.provisionUser(anyString(), any(), anyMap())).willReturn("user");
        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle, gatewayLookup);
        given(config.getCfgCreateAccount()).willReturn(true);

        //when
        int nextState = module.process(null, ISAuthConstants.LOGIN_START);

        //then
        assertThat(nextState).isEqualTo(LOGIN_SUCCEED);
    }

    @Test
    void shouldSucceedWhenConfiguredToUseAnonymousUser() throws Exception {
        //given
        given(client.getUserInfo(dataStore)).willReturn(Promises.newResultPromise(userInfo));
        given(authModuleHelper.userExistsInTheDataStore(anyString(), any(), anyMap()))
                .willReturn(Optional.empty());
        given(authModuleHelper.provisionUser(anyString(), any(), anyMap())).willReturn("user");
        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle, gatewayLookup);
        given(config.getCfgCreateAccount()).willReturn(true);
        lenient().when(config.getMapToAnonymousUser()).thenReturn(true);
        lenient().when(config.getAnonymousUserName()).thenReturn("user");

        //when
        int nextState = module.process(null, ISAuthConstants.LOGIN_START);

        //then
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
                //then Exception
                .isInstanceOf(AuthLoginException.class)
                .hasMessage("No user mapped!");
    }
}
