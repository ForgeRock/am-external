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

import static com.sun.identity.authentication.util.ISAuthConstants.FULL_LOGIN_URL;
import static com.sun.identity.authentication.util.ISAuthConstants.LOGIN_SUCCEED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.oauth.clients.oauth2.OAuth2Client.AUTHORIZATION_HEADER;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.COOKIE_ORIG_URL;
import static org.forgerock.openam.authentication.modules.oauth2.OAuthParam.NONCE_TOKEN_ID;
import static org.forgerock.openam.authentication.modules.social.SocialAuthLoginModule.RESUME_FROM_REGISTRATION_REDIRECT_STATE;
import static org.forgerock.openam.authentication.modules.social.SocialAuthLoginModuleWeChatMobile.OPENID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Function;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.forgerock.guava.common.base.Optional;
import org.forgerock.guava.common.collect.ImmutableMap;
import org.forgerock.oauth.OAuthClient;
import org.forgerock.oauth.UserInfo;
import org.forgerock.openam.integration.idm.IdmIntegrationConfig;
import org.forgerock.openam.authentication.modules.common.AMLoginModuleBinder;
import org.forgerock.openam.authentication.modules.common.mapping.AccountProvider;
import org.forgerock.openam.authentication.modules.oidc.JwtHandlerConfig;
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
 * Test class for SocialAuthLoginModuleWeChatMobile.
 */
public class SocialAuthLoginModuleWeChatMobileTest {

    private static final String ORIGINAL_URL = "http://originalUrl";
    private static final String DOMAIN_1 = "domain1";
    private static final String DOMAIN_2 = "domain2";

    private ImmutableMap<String, Set<String>> options;
    private static final Subject SUBJECT = null;
    private static final Map SHARED_STATE = new HashMap();

    private SocialAuthLoginModuleWeChatMobile module;

    @Mock
    private Debug debug;
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
    private SocialAuthLoginModule.ClientTokenJwtGenerator clientTokenJwtGenerator;
    @Mock
    private ResourceBundle bundle;


    @BeforeMethod
    public void setup() throws Exception {

        MockitoAnnotations.initMocks(this);

        when(authModuleHelper.getOriginalUrl(request)).thenReturn(ORIGINAL_URL);
        when(authModuleHelper.getCookieDomainsForRequest(request))
                .thenReturn(CollectionUtils.asSet(DOMAIN_1, DOMAIN_2));


        when(binder.getHttpServletRequest()).thenReturn(request);
        when(binder.getHttpServletResponse()).thenReturn(response);
        when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn("Bearer token");
        when(request.getParameter(OPENID)).thenReturn("OPENID");

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

        module = new SocialAuthLoginModuleWeChatMobile(debug, authModuleHelper, configFunction, clientTokenJwtGenerator,
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
    public void shouldAddDomainCookiesToResponse() throws Exception {
        //given
        String path = "/";
        String dataStoreId = "data_store_id";
        given(dataStore.getId()).willReturn(dataStoreId);
        given(client.getUserInfo(dataStore)).willReturn(Promises.newResultPromise(userInfo));
        given(authModuleHelper.userExistsInTheDataStore(anyString(), any(), anyMap()))
                .willReturn(Optional.of("user"));
        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle);

        //when
        module.process(null, ISAuthConstants.LOGIN_START);

        //then
        verify(authModuleHelper, times(1)).addCookieToResponse(response, COOKIE_ORIG_URL, ORIGINAL_URL, path, DOMAIN_1);
        verify(authModuleHelper, times(1)).addCookieToResponse(response, COOKIE_ORIG_URL, ORIGINAL_URL, path, DOMAIN_2);
        verify(authModuleHelper, times(1)).addCookieToResponse(response, NONCE_TOKEN_ID, dataStoreId, path, DOMAIN_1);
        verify(authModuleHelper, times(1)).addCookieToResponse(response, NONCE_TOKEN_ID, dataStoreId, path, DOMAIN_2);
    }

    @Test
    public void shouldAddOriginalUrlToSession() throws Exception {
        //given
        given(client.getUserInfo(dataStore)).willReturn(Promises.newResultPromise(userInfo));
        given(authModuleHelper.userExistsInTheDataStore(anyString(), any(), anyMap()))
                .willReturn(Optional.of("user"));
        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle);

        //when
        module.process(null, ISAuthConstants.LOGIN_START);

        //then
        verify(binder, times(1)).setUserSessionProperty(eq(FULL_LOGIN_URL), eq(ORIGINAL_URL));
    }

    @Test(expectedExceptions = AuthLoginException.class, expectedExceptionsMessageRegExp = "Unable to retrieve access token from header")
    public void shouldFailWhenAccessTokenNotPresent() throws Exception {
        //given
        given(request.getHeader(AUTHORIZATION_HEADER)).willReturn(null);
        given(authModuleHelper.userExistsInTheDataStore(anyString(), any(AccountProvider.class), anyMap()))
                .willReturn(Optional.absent());
        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle);

        //when
        module.process(null, ISAuthConstants.LOGIN_START);

        //then Exception
    }

    @Test(expectedExceptions = AuthLoginException.class, expectedExceptionsMessageRegExp = "Unable to retrieve WeChat OpenId")
    public void shouldFailWhenOpenIdNotPresent() throws Exception {
        //given
        given(request.getParameter(OPENID)).willReturn(null);
        given(authModuleHelper.userExistsInTheDataStore(anyString(), any(AccountProvider.class), anyMap()))
                .willReturn(Optional.absent());
        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle);

        //when
        module.process(null, ISAuthConstants.LOGIN_START);

        //then Exception
    }


    @Test
    public void shouldSucceedWhenUserIsPresent() throws Exception {
        //given
        given(client.getUserInfo(dataStore)).willReturn(Promises.newResultPromise(userInfo));
        given(authModuleHelper.userExistsInTheDataStore(anyString(), any(), anyMap()))
                .willReturn(Optional.of("user"));
        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle);

        //when
        int nextState = module.process(null, ISAuthConstants.LOGIN_START);

        //then
        verify(binder, times(1)).storeUsernamePasswd(eq("user"), isNull(String.class));
        assertThat(nextState).isEqualTo(ISAuthConstants.LOGIN_SUCCEED);
    }

    @Test
    public void shouldSaveAttributesToSessionOnSuccessWhenConfigured() throws Exception {
        //given
        given(client.getUserInfo(dataStore)).willReturn(Promises.newResultPromise(userInfo));
        given(authModuleHelper.userExistsInTheDataStore(anyString(), any(), anyMap()))
                .willReturn(Optional.of("user"));
        given(config.getSaveAttributesToSessionFlag()).willReturn(true);
        given(profileNormalizer.getNormalisedAttributes(userInfo, null))
                .willReturn(ImmutableMap.of("name", CollectionUtils.asSet("user")));
        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle);

        //when
        module.process(null, ISAuthConstants.LOGIN_START);

        //then
        verify(binder, times(1)).setUserSessionProperty(eq("name"), eq("user"));
    }

    @Test
    public void shouldReturnResumeRegistrationStateWhenConfiguredToUseRegistrationService() throws Exception {
        //given
        given(client.getUserInfo(dataStore)).willReturn(Promises.newResultPromise(userInfo));
        given(authModuleHelper.userExistsInTheDataStore(anyString(), any(), anyMap()))
                .willReturn(Optional.absent());
        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle);
        given(config.getCfgCreateAccount()).willReturn(true);
        given(config.isCfgRegistrationServiceEnabled() ).willReturn(true);

        //when
        int nextState = module.process(null, ISAuthConstants.LOGIN_START);

        //then
        verify(binder, times(1))
                .replaceCallback(eq(RESUME_FROM_REGISTRATION_REDIRECT_STATE), eq(0), any(RedirectCallback.class));
        assertThat(nextState).isEqualTo(RESUME_FROM_REGISTRATION_REDIRECT_STATE);
    }

    @Test
    public void shouldSucceedWhenConfiguredToProvisionLocally() throws Exception {
        //given
        given(client.getUserInfo(dataStore)).willReturn(Promises.newResultPromise(userInfo));
        given(authModuleHelper.userExistsInTheDataStore(anyString(), any(), anyMap()))
                .willReturn(Optional.absent());
        given(authModuleHelper.provisionUser(anyString(), any(), anyMap())).willReturn("user");
        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle);
        given(config.getCfgCreateAccount()).willReturn(true);

        //when
        int nextState = module.process(null, ISAuthConstants.LOGIN_START);

        //then
        assertThat(nextState).isEqualTo(LOGIN_SUCCEED);
    }

    @Test
    public void shouldSucceedWhenConfiguredToUseAnonymousUser() throws Exception {
        //given
        given(client.getUserInfo(dataStore)).willReturn(Promises.newResultPromise(userInfo));
        given(authModuleHelper.userExistsInTheDataStore(anyString(), any(), anyMap()))
                .willReturn(Optional.absent());
        given(authModuleHelper.provisionUser(anyString(), any(), anyMap())).willReturn("user");
        module.init(SUBJECT, config, client, dataStore, jwtHandlerConfig, profileNormalizer, bundle);
        given(config.getCfgCreateAccount()).willReturn(true);
        given(config.getMapToAnonymousUser()).willReturn(true);
        given(config.getAnonymousUserName()).willReturn("user");

        //when
        int nextState = module.process(null, ISAuthConstants.LOGIN_START);

        //then
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
}
