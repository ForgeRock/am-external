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
 * Copyright 2020-2021 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.OBJECT_ATTRIBUTES;
import static org.forgerock.openam.auth.node.api.TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE;
import static org.forgerock.openam.auth.nodes.SocialProviderHandlerNode.ALIAS_LIST;
import static org.forgerock.openam.auth.nodes.SocialProviderHandlerNode.SOCIAL_OAUTH_DATA;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.IDPS;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.SELECTED_IDP;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Provider;
import javax.script.ScriptException;

import org.forgerock.http.Handler;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.oauth.DataStore;
import org.forgerock.oauth.OAuthClient;
import org.forgerock.oauth.OAuthException;
import org.forgerock.oauth.UserInfo;
import org.forgerock.oauth.clients.oauth2.OAuth2ClientConfiguration;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper;
import org.forgerock.openam.authentication.callbacks.IdPCallback;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.http.CloseableHttpClientHandler;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.forgerock.openam.scripting.ScriptContext;
import org.forgerock.openam.scripting.ScriptEvaluator;
import org.forgerock.openam.scripting.SupportedScriptingLanguage;
import org.forgerock.openam.scripting.service.ScriptConfiguration;
import org.forgerock.openam.social.idp.RealmBasedHttpClientHandlerFactory;
import org.forgerock.openam.social.idp.SocialIdpConfigMapper;
import org.forgerock.openam.social.idp.OAuth2ClientConfig;
import org.forgerock.openam.social.idp.OpenIDConnectClientConfig;
import org.forgerock.openam.social.idp.SocialIdentityProviders;
import org.forgerock.util.Options;
import org.forgerock.util.promise.Promises;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mozilla.javascript.NativeJavaObject;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.authentication.spi.RedirectCallback;
import com.sun.identity.common.ShutdownManager;

public class SocialProviderHandlerNodeTest {
    private static final String PROVIDER_REDIRECT = "http://provider/redirect";
    private static final String PROVIDER_NAME = "test";

    private final OAuth2ClientConfig testClient = new TestClient();

    @Mock
    private SocialProviderHandlerNode.Config config;

    @Mock
    private SocialIdentityProviders providerConfigStore;

    @Mock
    private IdentityUtils identityUtils;

    @Mock
    private Realm realm;

    @Mock
    private OAuthClient oAuthClient;

    @Mock
    private IdmIntegrationService idmIntegrationService;

    @Mock
    private ScriptEvaluator scriptEvaluator;

    @Mock
    private Provider<SessionService> sessionServiceProvider;

    @Mock
    private SessionService sessionService;

    @Mock
    private OAuth2ClientConfig idpConfig;

    @Mock
    private OAuth2ClientConfiguration oAuth2ClientConfiguration;

    @Mock
    private SocialOAuth2Helper authModuleHelper;

    @Mock
    private SocialIdpConfigMapper socialIdpConfigMapper;

    @Mock
    RealmBasedHttpClientHandlerFactory httpClientHandlerFactory;

    @Mock
    Handler defaultHandler;

    @Captor
    ArgumentCaptor<Map<String, List<String>>> params;

    @Mock
    NativeJavaObject nativeJavaObject;

    private SocialProviderHandlerNode node;
    private ScriptConfiguration scriptConfiguration;

    @BeforeMethod
    public void setUp() throws Exception {
        initMocks(this);
        when(socialIdpConfigMapper.map(realm, testClient)).thenReturn(oAuth2ClientConfiguration);
        when(authModuleHelper.newOAuthClient(any(Realm.class), any(OAuth2ClientConfig.class)))
                .thenReturn(oAuthClient);
        when(oAuthClient.getAuthRedirect(any(), any(), any()))
                .thenReturn(newResultPromise(new URI(PROVIDER_REDIRECT)));
        when(oAuthClient.handlePostAuth(any(), any())).thenAnswer(answer -> {
            DataStore dataStore = answer.getArgument(0);
            dataStore.storeData(json(object(field("testData", "testValue"))));
            return newResultPromise(json(object()));
        });
        when(oAuthClient.handleNativePostAuth(any(), any(), params.capture())).thenAnswer(answer -> {
            DataStore dataStore = answer.getArgument(1);
            dataStore.storeData(json(object(field("testData", "testValue"))));
            return newResultPromise(json(object()));
        });
        when(oAuthClient.getUserInfo(any())).thenReturn(Promises.newResultPromise(new UserInfo() {
            @Override
            public String getSubject() throws OAuthException {
                return "user";
            }

            @Override
            public JsonValue getRawProfile() throws OAuthException {
                return json(object());
            }
        }));
        scriptConfiguration = ScriptConfiguration.builder()
                .setId("1")
                .setLanguage(SupportedScriptingLanguage.JAVASCRIPT)
                .setName("test")
                .setScript("return {};")
                .setContext(ScriptContext.SOCIAL_IDP_PROFILE_TRANSFORMATION)
                .build();
        when(idpConfig.transform()).thenReturn(scriptConfiguration);
        when(config.script()).thenReturn(scriptConfiguration);
        when(config.usernameAttribute()).thenReturn("userName");
        when(config.clientType()).thenReturn(ClientType.BROWSER);
        when(providerConfigStore.getProviders(any(Realm.class))).thenReturn(singletonMap(PROVIDER_NAME, testClient));
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        when(idmIntegrationService.getSharedAttributesFromContext(any())).thenCallRealMethod();
        when(idmIntegrationService.storeAttributeInState(any(), any(), any())).thenCallRealMethod();
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any(), any()))
                .thenReturn(json(object(field("userName", "bob"), field(IDPS, array("existingProvider-bob")))));
        when(idmIntegrationService.isEnabled()).thenReturn(true);
        when(sessionService.getSession(any())).thenReturn(null);
        when(sessionServiceProvider.get()).thenReturn(sessionService);
        when(nativeJavaObject.unwrap()).thenReturn(json(object()));
        when(scriptEvaluator.evaluateScript(any(), any())).thenReturn(nativeJavaObject);
        when(httpClientHandlerFactory.create(any()))
                .thenReturn(new CloseableHttpClientHandler(ShutdownManager.getInstance(), Options.defaultOptions()));
        node = new SocialProviderHandlerNode(config, authModuleHelper, providerConfigStore, identityUtils, realm,
                scriptEvaluator, sessionServiceProvider, idmIntegrationService);
    }

    @Test
    public void processWithoutCodeResultsInRedirectCallbackWithCookie() throws Exception {
        TreeContext context = new TreeContext(DEFAULT_IDM_IDENTITY_RESOURCE,
                json(object(field(SELECTED_IDP, PROVIDER_NAME))),
                new ExternalRequestContext.Builder().build(), emptyList());

        Action action = node.process(context);

        Assert.assertTrue(action.sendingCallbacks());
        Assert.assertEquals(action.callbacks.size(), 1);
        Assert.assertTrue(action.callbacks.get(0) instanceof RedirectCallback);
        Assert.assertTrue(((RedirectCallback) action.callbacks.get(0)).getTrackingCookie());
        Assert.assertEquals(((RedirectCallback) action.callbacks.get(0)).getRedirectUrl(), PROVIDER_REDIRECT);
    }

    @Test
    public void processWithCodeResultsInCompletionWithDatastoreInTransientState() throws Exception {
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any(), any()))
                .thenThrow(new NotFoundException());
        Map<String, String[]> parameters = getStateAndCodeAsParameter();
        TreeContext context = new TreeContext(DEFAULT_IDM_IDENTITY_RESOURCE,
                json(object(field(SELECTED_IDP, PROVIDER_NAME))),
                new ExternalRequestContext.Builder()
                        .parameters(parameters)
                        .build(),
                emptyList());

        Action action = node.process(context);

        Assert.assertTrue(action.transientState.isDefined(SOCIAL_OAUTH_DATA));
        Assert.assertTrue(action.transientState.get(SOCIAL_OAUTH_DATA).isDefined(PROVIDER_NAME));
        Assert.assertTrue(action.transientState.get(SOCIAL_OAUTH_DATA).get(PROVIDER_NAME).isDefined("testData"));
        Assert.assertEquals(action.transientState.get(SOCIAL_OAUTH_DATA).get(PROVIDER_NAME).get("testData").asString(),
                "testValue");
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).isDefined(ALIAS_LIST)).isTrue();
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get(ALIAS_LIST).asList().size()).isEqualTo(1);
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get(ALIAS_LIST).asList(String.class).get(0))
                .isEqualTo(PROVIDER_NAME + "-user");
        Assert.assertNull(context.request.parameters.get("state"));
        Assert.assertNull(context.request.parameters.get("code"));
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void processWithACodeButWithoutStateThrowsException() throws Exception {
        Map<String, String[]> parameters = new HashMap<>();
        parameters.put("code", new String[]{"the_code"});

        TreeContext context = new TreeContext(DEFAULT_IDM_IDENTITY_RESOURCE,
                json(object(field(SELECTED_IDP, PROVIDER_NAME))),
                new ExternalRequestContext.Builder()
                        .parameters(parameters)
                        .build(),
                emptyList());

        node.process(context);
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void shouldThrowExceptionIfFailedToRetrieveProfile() throws Exception {
        when(oAuthClient.getUserInfo(any())).thenReturn(new OAuthException("Failed").asPromise());
        Map<String, String[]> parameters = getStateAndCodeAsParameter();
        TreeContext context = new TreeContext(DEFAULT_IDM_IDENTITY_RESOURCE,
                json(object(field(SELECTED_IDP, PROVIDER_NAME))),
                new ExternalRequestContext.Builder()
                        .parameters(parameters)
                        .build(),
                emptyList());

        node.process(context);
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void shouldThrowExceptionIfScriptExecutionFailed() throws Exception {
        when(scriptEvaluator.evaluateScript(any(), any())).thenThrow(new ScriptException("Failed"));
        Map<String, String[]> parameters = getStateAndCodeAsParameter();
        TreeContext context = new TreeContext(DEFAULT_IDM_IDENTITY_RESOURCE,
                json(object(field(SELECTED_IDP, PROVIDER_NAME))),
                new ExternalRequestContext.Builder()
                        .parameters(parameters)
                        .build(),
                emptyList());

        node.process(context);
    }

    @Test
    public void shouldPopulateTransientStateWithProfileData() throws Exception {
        Map<String, String[]> parameters = getStateAndCodeAsParameter();
        TreeContext context = new TreeContext(DEFAULT_IDM_IDENTITY_RESOURCE,
                json(object(field(SELECTED_IDP, PROVIDER_NAME))),
                new ExternalRequestContext.Builder()
                        .parameters(parameters)
                        .build(),
                emptyList());
        JsonValue objectData = json(object(
                field("attribute1", "value1")
        ));
        when(nativeJavaObject.unwrap()).thenReturn(objectData);
        when(scriptEvaluator.evaluateScript(any(), any())).thenReturn(nativeJavaObject);
        ScriptConfiguration scriptConfiguration = ScriptConfiguration.builder().setId("1")
                .setLanguage(SupportedScriptingLanguage.JAVASCRIPT).setName("test")
                .setScript("return {'attribute1':'value1'};")
                .setContext(ScriptContext.SOCIAL_IDP_PROFILE_TRANSFORMATION).build();
        when(config.script()).thenReturn(scriptConfiguration);

        node.process(context);

        assertThat(context.transientState.isDefined(OBJECT_ATTRIBUTES)).isTrue();
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).isDefined("attribute1")).isTrue();
        assertThat(idmIntegrationService.getAttributeFromContext(context, "attribute1").get().asString())
                .isEqualTo("value1");
    }

    @Test
    public void shouldNotPutUserNameInTransientStateIfItExistsInContext() throws Exception {
        Map<String, String[]> parameters = getStateAndCodeAsParameter();
        TreeContext context = new TreeContext(json(object(
                field(SELECTED_IDP, PROVIDER_NAME),
                field(OBJECT_ATTRIBUTES, object(field("userName", "bob")))
        )),
                new ExternalRequestContext.Builder()
                        .parameters(parameters)
                        .build(),
                emptyList(), Optional.empty());
        JsonValue objectData = json(object(
                field("attribute1", "value1"),
                field("userName", "newValue")
        ));
        when(nativeJavaObject.unwrap()).thenReturn(objectData);
        when(scriptEvaluator.evaluateScript(any(), any())).thenReturn(nativeJavaObject);
        ScriptConfiguration scriptConfiguration = ScriptConfiguration.builder().setId("1")
                .setLanguage(SupportedScriptingLanguage.JAVASCRIPT).setName("test")
                .setScript("return {'attribute1':'value1', 'userName':'newValue'};")
                .setContext(ScriptContext.SOCIAL_IDP_PROFILE_TRANSFORMATION).build();
        when(config.script()).thenReturn(scriptConfiguration);

        node.process(context);

        assertThat(context.transientState.isDefined(OBJECT_ATTRIBUTES)).isTrue();
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).isDefined("attribute1")).isTrue();
        assertThat(context.sharedState.get(OBJECT_ATTRIBUTES).get("userName").asString()).isEqualTo("bob");
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).isDefined("userName")).isFalse();
        assertThat(idmIntegrationService.getAttributeFromContext(context, "attribute1").get().asString())
                .isEqualTo("value1");
    }

    @Test
    public void shouldAddReturnedAliasToAliasList() throws Exception {
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any(), any()))
                .thenThrow(new NotFoundException());
        Map<String, String[]> parameters = getStateAndCodeAsParameter();
        TreeContext context = new TreeContext(json(object(field(SELECTED_IDP, PROVIDER_NAME))),
                json(object(field(OBJECT_ATTRIBUTES, object(field(ALIAS_LIST, array("anotherIdp-user")))))),
                new ExternalRequestContext.Builder()
                        .parameters(parameters)
                        .build(),
                emptyList(), Optional.empty());

        Action action = node.process(context);

        Assert.assertTrue(action.transientState.isDefined(SOCIAL_OAUTH_DATA));
        Assert.assertTrue(action.transientState.get(SOCIAL_OAUTH_DATA).isDefined(PROVIDER_NAME));
        Assert.assertTrue(action.transientState.get(SOCIAL_OAUTH_DATA).get(PROVIDER_NAME).isDefined("testData"));
        Assert.assertEquals(action.transientState.get(SOCIAL_OAUTH_DATA).get(PROVIDER_NAME).get("testData").asString(),
                "testValue");
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).isDefined(ALIAS_LIST)).isTrue();
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get(ALIAS_LIST).asList().size()).isEqualTo(2);
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get(ALIAS_LIST).asList(String.class)
                .contains(PROVIDER_NAME + "-user")).isTrue();
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get(ALIAS_LIST).asList(String.class)
                .contains("anotherIdp-user")).isTrue();
        Assert.assertNull(context.request.parameters.get("state"));
        Assert.assertNull(context.request.parameters.get("code"));
    }

    @Test
    public void shouldMergeAllAliases() throws Exception {
        Map<String, String[]> parameters = getStateAndCodeAsParameter();
        TreeContext context = new TreeContext(json(object(field(SELECTED_IDP, PROVIDER_NAME))),
                json(object(field(OBJECT_ATTRIBUTES, object(field(ALIAS_LIST, array("anotherIdp-user")))))),
                new ExternalRequestContext.Builder()
                        .parameters(parameters)
                        .build(),
                emptyList(), Optional.empty());

        Action action = node.process(context);

        Assert.assertTrue(action.transientState.isDefined(SOCIAL_OAUTH_DATA));
        Assert.assertTrue(action.transientState.get(SOCIAL_OAUTH_DATA).isDefined(PROVIDER_NAME));
        Assert.assertTrue(action.transientState.get(SOCIAL_OAUTH_DATA).get(PROVIDER_NAME).isDefined("testData"));
        Assert.assertEquals(action.transientState.get(SOCIAL_OAUTH_DATA).get(PROVIDER_NAME).get("testData").asString(),
                "testValue");
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).isDefined(ALIAS_LIST)).isTrue();
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get(ALIAS_LIST).asList().size()).isEqualTo(3);
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get(ALIAS_LIST).asList(String.class)
                .contains(PROVIDER_NAME + "-user")).isTrue();
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get(ALIAS_LIST).asList(String.class)
                .contains("anotherIdp-user")).isTrue();
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get(ALIAS_LIST).asList(String.class)
                .contains("existingProvider-bob")).isTrue();
        Assert.assertNull(context.request.parameters.get("state"));
        Assert.assertNull(context.request.parameters.get("code"));
    }

    @Test
    public void shouldAddReturnedAliasToUsersExistingAliasList() throws Exception {
        Map<String, String[]> parameters = getStateAndCodeAsParameter();
        TreeContext context = new TreeContext(json(object(
                field(SELECTED_IDP, PROVIDER_NAME),
                field(OBJECT_ATTRIBUTES, object(field("userName", "bob")))
        )),
                new ExternalRequestContext.Builder()
                        .parameters(parameters)
                        .build(),
                emptyList(), Optional.empty());

        Action action = node.process(context);

        Assert.assertTrue(action.transientState.isDefined(SOCIAL_OAUTH_DATA));
        Assert.assertTrue(action.transientState.get(SOCIAL_OAUTH_DATA).isDefined(PROVIDER_NAME));
        Assert.assertTrue(action.transientState.get(SOCIAL_OAUTH_DATA).get(PROVIDER_NAME).isDefined("testData"));
        Assert.assertEquals(action.transientState.get(SOCIAL_OAUTH_DATA).get(PROVIDER_NAME).get("testData").asString(),
                "testValue");
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).isDefined(ALIAS_LIST)).isTrue();
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get(ALIAS_LIST).asList().size()).isEqualTo(2);
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get(ALIAS_LIST).asList(String.class)
                .contains(PROVIDER_NAME + "-user")).isTrue();
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get(ALIAS_LIST).asList(String.class)
                .contains("existingProvider-bob")).isTrue();
        Assert.assertNull(context.request.parameters.get("state"));
        Assert.assertNull(context.request.parameters.get("code"));
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void shouldThrowExceptionIfMatchedUserDiffersFromSharedState() throws Exception {
        Map<String, String[]> parameters = getStateAndCodeAsParameter();
        TreeContext context = new TreeContext(json(object(
                field(SELECTED_IDP, PROVIDER_NAME),
                field(OBJECT_ATTRIBUTES, object(field("userName", "alice")))
        )),
                new ExternalRequestContext.Builder()
                        .parameters(parameters)
                        .build(),
                emptyList(), Optional.empty());

        Action action = node.process(context);
    }

    @Test
    public void processWithoutCodeResultsInIdPCallbackWithOAuth2Client() throws Exception {
        when(config.clientType()).thenReturn(ClientType.NATIVE);
        TreeContext context = new TreeContext(DEFAULT_IDM_IDENTITY_RESOURCE,
                json(object(field(SELECTED_IDP, PROVIDER_NAME))),
                new ExternalRequestContext.Builder().build(), emptyList());

        Action action = node.process(context);

        Assert.assertTrue(action.sendingCallbacks());
        Assert.assertEquals(action.callbacks.size(), 1);
        Assert.assertTrue(action.callbacks.get(0) instanceof IdPCallback);
        Assert.assertEquals(((IdPCallback) action.callbacks.get(0)).getClientId(), testClient.clientId());
        Assert.assertEquals(((IdPCallback) action.callbacks.get(0)).getRedirectUri(), testClient.redirectURI());
        Assert.assertEquals(((IdPCallback) action.callbacks.get(0)).getProvider(), testClient.provider());
        Assert.assertEquals(((IdPCallback) action.callbacks.get(0)).getScope(), testClient.scopes());
        Assert.assertNull(((IdPCallback) action.callbacks.get(0)).getNonce());
    }

    @Test
    public void processWithoutCodeResultsInIdPCallbackWithOidcClient() throws Exception {
        when(config.clientType()).thenReturn(ClientType.NATIVE);
        doAnswer(invocationOnMock -> {
            DataStore dataStore = invocationOnMock.getArgument(1);
            dataStore.storeData(json(object(field("nonce", "1234567"))));
            return null;
        }).when(authModuleHelper).createNonce(any(), any());

        OidcTestClient oidcTestClient = new OidcTestClient();
        when(providerConfigStore.getProviders(any(Realm.class)))
                .thenReturn(singletonMap(PROVIDER_NAME, oidcTestClient));
        TreeContext context = new TreeContext(DEFAULT_IDM_IDENTITY_RESOURCE,
                json(object(field(SELECTED_IDP, PROVIDER_NAME))),
                new ExternalRequestContext.Builder().build(), emptyList());

        Action action = node.process(context);

        Assert.assertTrue(action.sendingCallbacks());
        Assert.assertEquals(action.callbacks.size(), 1);
        Assert.assertTrue(action.callbacks.get(0) instanceof IdPCallback);
        Assert.assertEquals(((IdPCallback) action.callbacks.get(0)).getClientId(), oidcTestClient.clientId());
        Assert.assertEquals(((IdPCallback) action.callbacks.get(0)).getRedirectUri(), oidcTestClient.redirectURI());
        Assert.assertEquals(((IdPCallback) action.callbacks.get(0)).getProvider(), oidcTestClient.provider());
        Assert.assertEquals(((IdPCallback) action.callbacks.get(0)).getScope(), oidcTestClient.scopes());
        Assert.assertEquals(((IdPCallback) action.callbacks.get(0)).getNonce(), "1234567");
        Assert.assertEquals(((IdPCallback) action.callbacks.get(0)).getAcrValues(), oidcTestClient.acrValues());
    }

    @Test
    public void processResultsInIdPCallbackWithOidcClientNonceDisabled() throws Exception {
        when(config.clientType()).thenReturn(ClientType.NATIVE);
        OidcTestClient oidcTestClient = new OidcTestClient() {
            @Override
            public Boolean enableNativeNonce() {
                return false;
            }
        };
        when(providerConfigStore.getProviders(any(Realm.class)))
                .thenReturn(singletonMap(PROVIDER_NAME, oidcTestClient));
        TreeContext context = new TreeContext(DEFAULT_IDM_IDENTITY_RESOURCE,
                json(object(field(SELECTED_IDP, PROVIDER_NAME))),
                new ExternalRequestContext.Builder().build(), emptyList());

        Action action = node.process(context);

        Assert.assertTrue(action.sendingCallbacks());
        Assert.assertEquals(action.callbacks.size(), 1);
        Assert.assertTrue(action.callbacks.get(0) instanceof IdPCallback);
        Assert.assertEquals(((IdPCallback) action.callbacks.get(0)).getClientId(), oidcTestClient.clientId());
        Assert.assertEquals(((IdPCallback) action.callbacks.get(0)).getRedirectUri(), oidcTestClient.redirectURI());
        Assert.assertEquals(((IdPCallback) action.callbacks.get(0)).getProvider(), oidcTestClient.provider());
        Assert.assertEquals(((IdPCallback) action.callbacks.get(0)).getScope(), oidcTestClient.scopes());
        Assert.assertNull(((IdPCallback) action.callbacks.get(0)).getNonce());
        Assert.assertEquals(((IdPCallback) action.callbacks.get(0)).getAcrValues(), oidcTestClient.acrValues());
    }

    @Test
    public void processWithIdTokenResultsInCompletion() throws Exception {
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any(), any()))
                .thenThrow(new NotFoundException());
        when(config.clientType()).thenReturn(ClientType.NATIVE);

        IdPCallback callback = new IdPCallback(testClient.provider(),
                testClient.clientId(),
                testClient.redirectURI(),
                testClient.scopes(), null, null, null, null);

        callback.setTokenType("id_token");
        callback.setToken("id_token_value");

        TreeContext context = new TreeContext(json(object(
                field(SELECTED_IDP, PROVIDER_NAME),
                field(OBJECT_ATTRIBUTES, object(field("userName", "bob")))
        )), new ExternalRequestContext.Builder().build(),
                Collections.singletonList(callback),
                Optional.empty());

        Action action = node.process(context);

        Assert.assertTrue(action.transientState.isDefined(SOCIAL_OAUTH_DATA));
        Assert.assertTrue(action.transientState.get(SOCIAL_OAUTH_DATA).isDefined(PROVIDER_NAME));
        Assert.assertTrue(action.transientState.get(SOCIAL_OAUTH_DATA).get(PROVIDER_NAME).isDefined("testData"));
        Assert.assertEquals(action.transientState.get(SOCIAL_OAUTH_DATA).get(PROVIDER_NAME).get("testData").asString(),
                "testValue");
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).isDefined(ALIAS_LIST)).isTrue();
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get(ALIAS_LIST).asList().size()).isEqualTo(1);
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get(ALIAS_LIST).asList(String.class).get(0))
                .isEqualTo(PROVIDER_NAME + "-user");
    }

    @Test
    public void processWithAuthorizationCodeResultsInCompletion() throws Exception {
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any(), any()))
                .thenThrow(new NotFoundException());
        when(config.clientType()).thenReturn(ClientType.NATIVE);

        IdPCallback callback = new IdPCallback(testClient.provider(),
                testClient.clientId(),
                testClient.redirectURI(),
                testClient.scopes(), null, null, null, null);

        callback.setTokenType("authorization_code");
        callback.setToken("form_post_entry");

        Map<String, String[]> parameters = new HashMap<>();
        String[] values = {"dummy_auth_code"};
        parameters.put("code", values);

        TreeContext context = new TreeContext(json(object(
                field(SELECTED_IDP, PROVIDER_NAME),
                field(OBJECT_ATTRIBUTES, object(field("userName", "bob")))
        )), new ExternalRequestContext.Builder().parameters(parameters).build(),
                Collections.singletonList(callback),
                Optional.empty());

        Action action = node.process(context);

        //If IdPCallback does not provide the code, it gets the code from request parameter
        assertThat(params.getValue().get("authorization_code")).contains("dummy_auth_code");

        Assert.assertTrue(action.transientState.isDefined(SOCIAL_OAUTH_DATA));
        Assert.assertTrue(action.transientState.get(SOCIAL_OAUTH_DATA).isDefined(PROVIDER_NAME));
        Assert.assertTrue(action.transientState.get(SOCIAL_OAUTH_DATA).get(PROVIDER_NAME).isDefined("testData"));
        Assert.assertEquals(action.transientState.get(SOCIAL_OAUTH_DATA).get(PROVIDER_NAME).get("testData").asString(),
                "testValue");
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).isDefined(ALIAS_LIST)).isTrue();
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get(ALIAS_LIST).asList().size()).isEqualTo(1);
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get(ALIAS_LIST).asList(String.class).get(0))
                .isEqualTo(PROVIDER_NAME + "-user");
    }


    private Map<String, String[]> getStateAndCodeAsParameter() {
        Map<String, String[]> parameters = new HashMap<>();
        parameters.put("code", new String[]{"the_code"});
        parameters.put("state", new String[]{"the_state"});
        return parameters;
    }

    private class TestClient implements OAuth2ClientConfig {

        @Override
        public String provider() {
            return PROVIDER_NAME;
        }

        @Override
        public String authenticationIdKey() {
            return "sub";
        }

        @Override
        public Map<String, String> uiConfig() {
            return emptyMap();
        }

        @Override
        public ScriptConfiguration transform() {
            return scriptConfiguration;
        }

        @Override
        public String jwksUriEndpoint() {
            return null;
        }

        @Override
        public JwsAlgorithm jwtSigningAlgorithm() {
            return null;
        }

        @Override
        public String jwtEncryptionAlgorithm() {
            return null;
        }

        @Override
        public String jwtEncryptionMethod() {
            return null;
        }

        @Override
        public String clientId() {
            return "test";
        }

        @Override
        public Optional<char[]> clientSecret() {
            return Optional.of("secret".toCharArray());
        }

        @Override
        public String authorizationEndpoint() {
            return "https://localhost/authorize";
        }

        @Override
        public String tokenEndpoint() {
            return "https://localhost/token";
        }

        @Override
        public String userInfoEndpoint() {
            return "https://localhost/userInfo";
        }

        @Override
        public String introspectEndpoint() {
            return "https://localhost/introspect";
        }

        @Override
        public String redirectURI() {
            return "https://localhost/redirect";
        }

        @Override
        public String redirectAfterFormPostURI() {
            return null;
        }

        @Override
        public List<String> scopes() {
            return singletonList("profile");
        }
    }

    private class OidcTestClient extends TestClient implements OpenIDConnectClientConfig {

        @Override
        public List<String> acrValues() {
            return singletonList("TestTree");
        }

        @Override
        public String wellKnownEndpoint() {
            return null;
        }

        @Override
        public String requestObjectAudience() {
            return null;
        }

        @Override
        public Boolean encryptedIdTokens() {
            return null;
        }

        @Override
        public String issuer() {
            return "iss";
        }

        @Override
        public Boolean enableNativeNonce() {
            return true;
        }

        @Override
        public String redirectAfterFormPostURI() {
            return null;
        }
    }
}
