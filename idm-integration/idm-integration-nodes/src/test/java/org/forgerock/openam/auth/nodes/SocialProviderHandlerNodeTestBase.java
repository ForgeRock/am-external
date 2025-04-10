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
 * Copyright 2020-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static com.sun.identity.shared.FeatureEnablementConstants.OIDC_SOCIAL_PROVIDER_SUB_CLAIM_IS_NOT_UNIQUE;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.oauth.clients.oauth2.OAuth2Client.ACCESS_TOKEN;
import static org.forgerock.oauth.clients.oauth2.OAuth2Client.REFRESH_TOKEN;
import static org.forgerock.openam.auth.node.api.TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE;
import static org.forgerock.openam.auth.nodes.SocialProviderHandlerNode.ALIAS_LIST;
import static org.forgerock.openam.auth.nodes.SocialProviderHandlerNode.Config;
import static org.forgerock.openam.auth.nodes.SocialProviderHandlerNode.SOCIAL_OAUTH_DATA;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.IDPS;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.OBJECT_ATTRIBUTES;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.SELECTED_IDP;
import static org.forgerock.openam.social.idp.SocialIdPScriptContext.SOCIAL_IDP_PROFILE_TRANSFORMATION;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.inject.Provider;
import javax.script.ScriptException;

import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.oauth.DataStore;
import org.forgerock.oauth.OAuthClient;
import org.forgerock.oauth.OAuthException;
import org.forgerock.oauth.UserInfo;
import org.forgerock.oauth.clients.oidc.OpenIDConnectClient;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.AuthScriptUtilities;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.IdentifiedIdentity;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper;
import org.forgerock.openam.authentication.callbacks.IdPCallback;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.forgerock.openam.scripting.application.ScriptEvaluator;
import org.forgerock.openam.scripting.application.ScriptEvaluator.ScriptResult;
import org.forgerock.openam.scripting.application.ScriptEvaluatorFactory;
import org.forgerock.openam.scripting.domain.LegacyScriptBindings;
import org.forgerock.openam.scripting.domain.LegacyScriptContext;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.forgerock.openam.social.idp.AppleClientConfig;
import org.forgerock.openam.social.idp.OAuth2ClientConfig;
import org.forgerock.openam.social.idp.OpenIDConnectClientConfig;
import org.forgerock.openam.social.idp.RevocationOption;
import org.forgerock.openam.social.idp.SocialIdentityProviders;
import org.forgerock.secrets.GenericSecret;
import org.forgerock.secrets.Purpose;
import org.forgerock.util.promise.Promises;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mozilla.javascript.NativeJavaObject;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.authentication.spi.RedirectCallback;
import com.sun.identity.idm.IdType;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public abstract class SocialProviderHandlerNodeTestBase {
    private static final String PROVIDER_REDIRECT = "http://provider/redirect";
    protected static final String PROVIDER_NAME = "test";

    private final OAuth2ClientConfig testClient = new TestClient();

    protected Config config;

    @Mock
    protected SocialIdentityProviders providerConfigStore;

    @Mock
    protected LegacyIdentityService identityService;

    @Mock
    protected Realm realm;

    @Mock
    protected OAuthClient oAuthClient;

    @Mock
    protected IdmIntegrationService idmIntegrationService;

    @Mock
    ScriptEvaluatorFactory scriptEvaluatorFactory;

    @Mock
    protected ScriptEvaluator<LegacyScriptBindings> scriptEvaluator;

    @Mock
    protected Provider<SessionService> sessionServiceProvider;

    @Mock
    protected SocialOAuth2Helper authModuleHelper;

    @Captor
    ArgumentCaptor<Map<String, List<String>>> params;

    @Mock
    NativeJavaObject nativeJavaObject;
    @Mock
    AuthScriptUtilities authScriptUtilities;

    protected SocialProviderHandlerNode node;
    protected Script script;

    @BeforeEach
    void setUp() throws Exception {
        when(scriptEvaluatorFactory.create(any(LegacyScriptContext.class))).thenReturn(scriptEvaluator);
        when(authModuleHelper.newOAuthClient(any(Realm.class), any(OAuth2ClientConfig.class)))
                .thenReturn(oAuthClient);
        when(oAuthClient.getAuthRedirect(any(), any(), any()))
                .thenReturn(newResultPromise(new URI(PROVIDER_REDIRECT)));
        when(oAuthClient.handlePostAuth(any(), any())).thenAnswer(answer -> {
            DataStore dataStore = answer.getArgument(0);
            dataStore.storeData(json(object(field("testData", "testValue"),
                    field(ACCESS_TOKEN, "access_token"),
                    field(REFRESH_TOKEN, "refresh_token"))));
            return newResultPromise(json(object()));
        });
        when(oAuthClient.handleNativePostAuth(any(), any(), params.capture())).thenAnswer(answer -> {
            DataStore dataStore = answer.getArgument(1);
            dataStore.storeData(json(object(field("testData", "testValue"))));
            return newResultPromise(json(object()));
        });
        when(oAuthClient.getUserInfo(any())).thenReturn(Promises.newResultPromise(new UserInfo() {
            @Override
            public String getSubject() {
                return "user";
            }

            @Override
            public JsonValue getRawProfile() {
                return json(object());
            }
        }));
        script = Script.builder()
                .setId("1")
                .setLanguage(ScriptingLanguage.JAVASCRIPT)
                .setName("test")
                .setScript("return {};")
                .setContext(SOCIAL_IDP_PROFILE_TRANSFORMATION)
                .build();
        setConfigField();
        when(config.script()).thenReturn(script);
        when(config.usernameAttribute()).thenReturn("userName");
        when(config.clientType()).thenReturn(ClientType.BROWSER);
        when(providerConfigStore.getProviders(any(Realm.class))).thenReturn(singletonMap(PROVIDER_NAME, testClient));
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        when(idmIntegrationService.storeAttributeInState(any(), any(), any())).thenCallRealMethod();
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any(), any()))
                .thenReturn(json(object(field("userName", "bob"), field(IDPS, array("existingProvider-bob")))));
        when(idmIntegrationService.isEnabled()).thenReturn(true);
        when(nativeJavaObject.unwrap()).thenReturn(json(object()));
        ScriptResult<Object> scriptResult = mock(ScriptResult.class);
        when(scriptResult.getScriptReturnValue()).thenReturn(nativeJavaObject);
        when(scriptEvaluator.evaluateScript(any(), any(LegacyScriptBindings.class), eq(realm))).thenReturn(scriptResult);
        setNodeField();
    }

    protected abstract void setConfigField();

    protected abstract void setNodeField();

    @Test
    void testProcessAddsIdentifiedIdentityOfExistingUser() throws Exception {
        // Given
        Map<String, String[]> parameters = getStateAndCodeAsParameter();
        TreeContext context = new TreeContext(json(object(
                field(SELECTED_IDP, PROVIDER_NAME),
                field(OBJECT_ATTRIBUTES, object(field("userName", "bob")))
        )),
                new ExternalRequestContext.Builder()
                        .parameters(parameters)
                        .build(),
                emptyList(), Optional.empty());

        // When
        Action result = node.process(context);

        // Then
        assertThat(result.identifiedIdentity).isPresent();
        IdentifiedIdentity idid = result.identifiedIdentity.get();
        assertThat(idid.getUsername()).isEqualTo("bob");
        assertThat(idid.getIdentityType()).isEqualTo(IdType.USER);
    }

    @Test
    void processWithoutCodeResultsInRedirectCallbackWithCookie() throws Exception {
        TreeContext context = new TreeContext(DEFAULT_IDM_IDENTITY_RESOURCE,
                json(object(field(SELECTED_IDP, PROVIDER_NAME))),
                new ExternalRequestContext.Builder().build(), emptyList());

        Action action = node.process(context);

        assertThat(action.sendingCallbacks()).isTrue();
        assertThat(action.callbacks.size()).isEqualTo(1);
        assertThat(action.callbacks.get(0)).isInstanceOf(RedirectCallback.class);
        assertThat(((RedirectCallback) action.callbacks.get(0)).getTrackingCookie()).isTrue();
        assertThat(((RedirectCallback) action.callbacks.get(0)).getRedirectUrl()).isEqualTo(PROVIDER_REDIRECT);
    }

    @Test
    void processWithCodeResultsInCompletionWithDatastoreInTransientState() throws Exception {
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

        assertThat(action.transientState.isDefined(SOCIAL_OAUTH_DATA)).isTrue();
        assertThat(action.transientState.get(SOCIAL_OAUTH_DATA).isDefined(PROVIDER_NAME)).isTrue();
        assertThat(action.transientState.get(SOCIAL_OAUTH_DATA).get(PROVIDER_NAME).isDefined("testData")).isTrue();
        assertThat(action.transientState.get(SOCIAL_OAUTH_DATA).get(PROVIDER_NAME).get("testData").asString())
                .isEqualTo("testValue");
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).isDefined(ALIAS_LIST)).isTrue();
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get(ALIAS_LIST).asList().size()).isEqualTo(1);
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get(ALIAS_LIST).asList(String.class).get(0))
                .isEqualTo(PROVIDER_NAME + "-user");
        assertThat(context.request.parameters.get("state")).isNull();
        assertThat(context.request.parameters.get("code")).isNull();
    }

    @Test
    void processWithACodeButWithoutStateThrowsException() {
        Map<String, String[]> parameters = new HashMap<>();
        parameters.put("code", new String[]{"the_code"});

        TreeContext context = new TreeContext(DEFAULT_IDM_IDENTITY_RESOURCE,
                json(object(field(SELECTED_IDP, PROVIDER_NAME))),
                new ExternalRequestContext.Builder()
                        .parameters(parameters)
                        .build(),
                emptyList());

        assertThatThrownBy(() -> node.process(context))
                .isExactlyInstanceOf(NodeProcessException.class);

    }

    @Test
    void shouldThrowExceptionIfFailedToRetrieveProfile() {
        when(oAuthClient.getUserInfo(any())).thenReturn(new OAuthException("Failed").asPromise());
        Map<String, String[]> parameters = getStateAndCodeAsParameter();
        TreeContext context = new TreeContext(DEFAULT_IDM_IDENTITY_RESOURCE,
                json(object(field(SELECTED_IDP, PROVIDER_NAME))),
                new ExternalRequestContext.Builder()
                        .parameters(parameters)
                        .build(),
                emptyList());

        assertThatThrownBy(() -> node.process(context))
                .isExactlyInstanceOf(NodeProcessException.class);
    }

    @Test
    void shouldThrowExceptionIfScriptExecutionFailed() throws Exception {
        when(scriptEvaluator.evaluateScript(any(), any(LegacyScriptBindings.class), eq(realm)))
                .thenThrow(new ScriptException("Failed"));
        Map<String, String[]> parameters = getStateAndCodeAsParameter();
        TreeContext context = new TreeContext(DEFAULT_IDM_IDENTITY_RESOURCE,
                json(object(field(SELECTED_IDP, PROVIDER_NAME))),
                new ExternalRequestContext.Builder()
                        .parameters(parameters)
                        .build(),
                emptyList());

        assertThatThrownBy(() -> node.process(context))
                .isExactlyInstanceOf(NodeProcessException.class);
    }

    @Test
    void shouldPopulateTransientStateWithProfileData() throws Exception {
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
        ScriptResult<Object> scriptResult = mock(ScriptResult.class);
        when(scriptResult.getScriptReturnValue()).thenReturn(nativeJavaObject);
        when(scriptEvaluator.evaluateScript(any(), any(LegacyScriptBindings.class), eq(realm))).thenReturn(scriptResult);
        Script script = Script.builder().setId("1")
                .setLanguage(ScriptingLanguage.JAVASCRIPT).setName("test")
                .setScript("return {'attribute1':'value1'};")
                .setContext(SOCIAL_IDP_PROFILE_TRANSFORMATION).build();
        when(config.script()).thenReturn(script);

        node.process(context);

        assertThat(context.transientState.isDefined(OBJECT_ATTRIBUTES)).isTrue();
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).isDefined("attribute1")).isTrue();
        assertThat(idmIntegrationService.getAttributeFromContext(context, "attribute1").get().asString())
                .isEqualTo("value1");
    }

    @Test
    void shouldNotPopulateTransientStateWithTokenData() throws Exception {
        // Given
        when(config.storeTokens()).thenReturn(false);
        Map<String, String[]> parameters = getStateAndCodeAsParameter();
        TreeContext context = new TreeContext(DEFAULT_IDM_IDENTITY_RESOURCE,
                json(object(field(SELECTED_IDP, PROVIDER_NAME))),
                new ExternalRequestContext.Builder()
                        .parameters(parameters)
                        .build(),
                emptyList());

        node.process(context);

        assertThat(context.transientState.isDefined(ACCESS_TOKEN)).isFalse();
        assertThat(context.transientState.isDefined(REFRESH_TOKEN)).isFalse();
    }

    @Test
    void shouldPopulateTransientStateWithTokenData() throws Exception {
        // Given
        when(config.storeTokens()).thenReturn(true);
        Map<String, String[]> parameters = getStateAndCodeAsParameter();
        TreeContext context = new TreeContext(DEFAULT_IDM_IDENTITY_RESOURCE,
                json(object(field(SELECTED_IDP, PROVIDER_NAME))),
                new ExternalRequestContext.Builder()
                        .parameters(parameters)
                        .build(),
                emptyList());

        node.process(context);

        assertThat(context.transientState.isDefined(ACCESS_TOKEN)).isTrue();
        assertThat(context.transientState.isDefined(REFRESH_TOKEN)).isTrue();
        assertThat(context.transientState.get(ACCESS_TOKEN).asString()).isEqualTo("access_token");
        assertThat(context.transientState.get(REFRESH_TOKEN).asString()).isEqualTo("refresh_token");
    }

    @Test
    void shouldNotPutUserNameInTransientStateIfItExistsInContext() throws Exception {
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
        ScriptResult<Object> scriptResult = mock(ScriptResult.class);
        when(scriptResult.getScriptReturnValue()).thenReturn(nativeJavaObject);
        when(scriptEvaluator.evaluateScript(any(), any(LegacyScriptBindings.class), eq(realm))).thenReturn(scriptResult);
        Script script = Script.builder().setId("1")
                .setLanguage(ScriptingLanguage.JAVASCRIPT).setName("test")
                .setScript("return {'attribute1':'value1', 'userName':'newValue'};")
                .setContext(SOCIAL_IDP_PROFILE_TRANSFORMATION).build();
        when(config.script()).thenReturn(script);

        node.process(context);

        assertThat(context.transientState.isDefined(OBJECT_ATTRIBUTES)).isTrue();
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).isDefined("attribute1")).isTrue();
        assertThat(context.sharedState.get(OBJECT_ATTRIBUTES).get("userName").asString()).isEqualTo("bob");
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).isDefined("userName")).isFalse();
        assertThat(idmIntegrationService.getAttributeFromContext(context, "attribute1").get().asString())
                .isEqualTo("value1");
    }

    @Test
    void shouldAddReturnedAliasToAliasList() throws Exception {
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

        assertThat(action.transientState.isDefined(SOCIAL_OAUTH_DATA)).isTrue();
        assertThat(action.transientState.get(SOCIAL_OAUTH_DATA).isDefined(PROVIDER_NAME)).isTrue();
        assertThat(action.transientState.get(SOCIAL_OAUTH_DATA).get(PROVIDER_NAME).isDefined("testData"))
                .isTrue();
        assertThat(action.transientState.get(SOCIAL_OAUTH_DATA).get(PROVIDER_NAME).get("testData").asString())
                .isEqualTo("testValue");
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).isDefined(ALIAS_LIST)).isTrue();
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get(ALIAS_LIST).asList().size()).isEqualTo(2);
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get(ALIAS_LIST).asList(String.class)
                .contains(PROVIDER_NAME + "-user")).isTrue();
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get(ALIAS_LIST).asList(String.class)
                .contains("anotherIdp-user")).isTrue();
        assertThat(context.request.parameters.get("state")).isNull();
        assertThat(context.request.parameters.get("code")).isNull();
    }

    @Test
    void shouldMergeAllAliases() throws Exception {
        Map<String, String[]> parameters = getStateAndCodeAsParameter();
        TreeContext context = new TreeContext(json(object(field(SELECTED_IDP, PROVIDER_NAME))),
                json(object(field(OBJECT_ATTRIBUTES, object(field(ALIAS_LIST, array("anotherIdp-user")))))),
                new ExternalRequestContext.Builder()
                        .parameters(parameters)
                        .build(),
                emptyList(), Optional.empty());

        Action action = node.process(context);

        assertThat(action.transientState.isDefined(SOCIAL_OAUTH_DATA)).isTrue();
        assertThat(action.transientState.get(SOCIAL_OAUTH_DATA).isDefined(PROVIDER_NAME)).isTrue();
        assertThat(action.transientState.get(SOCIAL_OAUTH_DATA).get(PROVIDER_NAME).isDefined("testData")).isTrue();
        assertThat(action.transientState.get(SOCIAL_OAUTH_DATA).get(PROVIDER_NAME).get("testData").asString())
                .isEqualTo("testValue");
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).isDefined(ALIAS_LIST)).isTrue();
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get(ALIAS_LIST).asList().size()).isEqualTo(3);
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get(ALIAS_LIST).asList(String.class)
                .contains(PROVIDER_NAME + "-user")).isTrue();
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get(ALIAS_LIST).asList(String.class)
                .contains("anotherIdp-user")).isTrue();
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get(ALIAS_LIST).asList(String.class)
                .contains("existingProvider-bob")).isTrue();
        assertThat(context.request.parameters.get("state")).isNull();
        assertThat(context.request.parameters.get("code")).isNull();
    }

    @Test
    void shouldAddReturnedAliasToUsersExistingAliasList() throws Exception {
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

        assertThat(action.transientState.isDefined(SOCIAL_OAUTH_DATA)).isTrue();
        assertThat(action.transientState.get(SOCIAL_OAUTH_DATA).isDefined(PROVIDER_NAME)).isTrue();
        assertThat(action.transientState.get(SOCIAL_OAUTH_DATA).get(PROVIDER_NAME).isDefined("testData")).isTrue();
        assertThat(action.transientState.get(SOCIAL_OAUTH_DATA).get(PROVIDER_NAME).get("testData").asString())
                .isEqualTo("testValue");
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).isDefined(ALIAS_LIST)).isTrue();
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get(ALIAS_LIST).asList().size()).isEqualTo(2);
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get(ALIAS_LIST).asList(String.class)
                .contains(PROVIDER_NAME + "-user")).isTrue();
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get(ALIAS_LIST).asList(String.class)
                .contains("existingProvider-bob")).isTrue();
        assertThat(context.request.parameters.get("state")).isNull();
        assertThat(context.request.parameters.get("code")).isNull();
    }

    @Test
    void shouldThrowExceptionIfMatchedUserDiffersFromSharedState() {
        Map<String, String[]> parameters = getStateAndCodeAsParameter();
        TreeContext context = new TreeContext(json(object(
                field(SELECTED_IDP, PROVIDER_NAME),
                field(OBJECT_ATTRIBUTES, object(field("userName", "alice")))
        )),
                new ExternalRequestContext.Builder()
                        .parameters(parameters)
                        .build(),
                emptyList(), Optional.empty());

        assertThatThrownBy(() -> node.process(context))
                .isExactlyInstanceOf(NodeProcessException.class);
    }

    @Test
    void processWithoutCodeResultsInIdPCallbackWithOAuth2Client() throws Exception {
        when(config.clientType()).thenReturn(ClientType.NATIVE);
        TreeContext context = new TreeContext(DEFAULT_IDM_IDENTITY_RESOURCE,
                json(object(field(SELECTED_IDP, PROVIDER_NAME))),
                new ExternalRequestContext.Builder().build(), emptyList());

        Action action = node.process(context);

        assertThat(action.sendingCallbacks()).isTrue();
        assertThat(action.callbacks.size()).isEqualTo(1);
        assertThat(action.callbacks.get(0) instanceof IdPCallback).isTrue();
        assertThat(((IdPCallback) action.callbacks.get(0)).getClientId()).isEqualTo(testClient.clientId());
        assertThat(((IdPCallback) action.callbacks.get(0)).getRedirectUri()).isEqualTo(testClient.redirectURI());
        assertThat(((IdPCallback) action.callbacks.get(0)).getProvider()).isEqualTo(testClient.provider());
        assertThat(((IdPCallback) action.callbacks.get(0)).getScope()).isEqualTo(testClient.scopes());
        assertThat(((IdPCallback) action.callbacks.get(0)).getNonce()).isNull();
    }

    @Test
    void processWithoutCodeResultsInIdPCallbackWithOidcClient() throws Exception {
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

        assertThat(action.sendingCallbacks()).isTrue();
        assertThat(action.callbacks.size()).isEqualTo(1);
        assertThat(action.callbacks.get(0) instanceof IdPCallback).isTrue();
        assertThat(((IdPCallback) action.callbacks.get(0)).getClientId()).isEqualTo(oidcTestClient.clientId());
        assertThat(((IdPCallback) action.callbacks.get(0)).getRedirectUri()).isEqualTo(oidcTestClient.redirectURI());
        assertThat(((IdPCallback) action.callbacks.get(0)).getProvider()).isEqualTo(oidcTestClient.provider());
        assertThat(((IdPCallback) action.callbacks.get(0)).getScope()).isEqualTo(oidcTestClient.scopes());
        assertThat(((IdPCallback) action.callbacks.get(0)).getNonce()).isEqualTo("1234567");
        assertThat(((IdPCallback) action.callbacks.get(0)).getAcrValues()).isEqualTo(oidcTestClient.acrValues());
    }

    @Test
    void processWithOidcClientWithoutSubjectOverrideFindsExistingUser() throws Exception {
        // Given
        setupOidcClient("oidc-user", false);
        OidcTestClient oidcTestClient = new OidcTestClient();
        Map<String, String[]> parameters = getStateAndCodeAsParameter();
        when(providerConfigStore.getProviders(any(Realm.class)))
                .thenReturn(singletonMap("oidc-test-provider", oidcTestClient));
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any(), any()))
                .thenReturn(json(object(field("userName", "oidc-user"),
                        field(IDPS, array("oidc-test-provider-oidc-user")))));
        TreeContext context = new TreeContext(json(object(
                field(SELECTED_IDP, "oidc-test-provider"),
                field(OBJECT_ATTRIBUTES, object(field("userName", "oidc-user")))
        )), new ExternalRequestContext.Builder().parameters(parameters).build(),
                emptyList(), Optional.empty());

        // When
        Action action = node.process(context);

        // Then
        assertThat(action.outcome).isEqualTo("ACCOUNT_EXISTS");
        assertThat(action.identifiedIdentity).isPresent();
        IdentifiedIdentity identifiedIdentity = action.identifiedIdentity.get();
        assertThat(identifiedIdentity.getUsername()).isEqualTo("oidc-user");
        assertThat(identifiedIdentity.getIdentityType()).isEqualTo(IdType.USER);
        assertThat(context.transientState.isDefined(OBJECT_ATTRIBUTES)).isTrue();
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get("aliasList").asList()).containsExactly(
                "oidc-test-provider-oidc-user");
        assertThat(context.sharedState.get(OBJECT_ATTRIBUTES).get("userName").asString()).isEqualTo("oidc-user");

    }

    @Test
    void processWithOidcClientWithSubjectOverrideFindsExistingUser() throws Exception {
        // Given
        try (var ignored = mockStatic(SystemProperties.class)) {
            given(SystemProperties.getAsBoolean(OIDC_SOCIAL_PROVIDER_SUB_CLAIM_IS_NOT_UNIQUE, false))
                    .willReturn(true);
            setupOidcClient(UUID.randomUUID().toString(), true);
            var oidcTestClient = new OidcTestClient();
            Map<String, String[]> parameters = getStateAndCodeAsParameter();
            when(providerConfigStore.getProviders(any(Realm.class)))
                    .thenReturn(singletonMap("oidc-test-provider", oidcTestClient));
            when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any(), any()))
                    .thenReturn(json(object(field("userName", "oidc-user"),
                            field(IDPS, array("oidc-test-provider-oidc-user")))));
            var context = new TreeContext(json(object(
                    field(SELECTED_IDP, "oidc-test-provider"),
                    field(OBJECT_ATTRIBUTES, object(field("userName", "oidc-user")))
            )), new ExternalRequestContext.Builder().parameters(parameters).build(),
                    emptyList(), Optional.empty());

            // When
            Action action = node.process(context);

            // Then
            assertThat(action.outcome).isEqualTo("ACCOUNT_EXISTS");
            assertThat(action.identifiedIdentity).isPresent();
            IdentifiedIdentity identifiedIdentity = action.identifiedIdentity.get();
            assertThat(identifiedIdentity.getUsername()).isEqualTo("oidc-user");
            assertThat(identifiedIdentity.getIdentityType()).isEqualTo(IdType.USER);
            assertThat(context.transientState.isDefined(OBJECT_ATTRIBUTES)).isTrue();
            assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get("aliasList").asList()).containsExactly(
                    "oidc-test-provider-oidc-user");
            assertThat(context.sharedState.get(OBJECT_ATTRIBUTES).get("userName").asString()).isEqualTo("oidc-user");
        }
    }
    @Test
    void nativeProcessWhenInstructedShouldRequestSDKForUserInfoViaIdPCallback() throws Exception {
        when(config.clientType()).thenReturn(ClientType.NATIVE);
        when(providerConfigStore.getProviders(any(Realm.class)))
                .thenReturn(singletonMap(PROVIDER_NAME, new AppleTestClientRequestingUserInfo()));

        TreeContext context = new TreeContext(DEFAULT_IDM_IDENTITY_RESOURCE,
                json(object(field(SELECTED_IDP, PROVIDER_NAME))),
                new ExternalRequestContext.Builder().build(), emptyList());

        Action action = node.process(context);

        assertThat(((IdPCallback) action.callbacks.get(0)).isRequestNativeAppForUserInfo()).isEqualTo(true);
    }

    @Test
    void defaultNativeProcessShouldNotRequestSDKForUserInfoViaIdPCallback()
            throws Exception {
        when(config.clientType()).thenReturn(ClientType.NATIVE);
        when(providerConfigStore.getProviders(any(Realm.class)))
                .thenReturn(singletonMap(PROVIDER_NAME, new OidcTestClient()));

        TreeContext context = new TreeContext(DEFAULT_IDM_IDENTITY_RESOURCE,
                json(object(field(SELECTED_IDP, PROVIDER_NAME))),
                new ExternalRequestContext.Builder().build(), emptyList());

        Action action = node.process(context);

        assertThat(((IdPCallback) action.callbacks.get(0)).isRequestNativeAppForUserInfo()).isEqualTo(false);
    }

    @Test
    void processResultsInIdPCallbackWithOidcClientNonceDisabled() throws Exception {
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

        assertThat(action.sendingCallbacks()).isTrue();
        assertThat(action.callbacks.size()).isEqualTo(1);
        assertThat(action.callbacks.get(0) instanceof IdPCallback).isTrue();
        assertThat(((IdPCallback) action.callbacks.get(0)).getClientId()).isEqualTo(oidcTestClient.clientId());
        assertThat(((IdPCallback) action.callbacks.get(0)).getRedirectUri()).isEqualTo(oidcTestClient.redirectURI());
        assertThat(((IdPCallback) action.callbacks.get(0)).getProvider()).isEqualTo(oidcTestClient.provider());
        assertThat(((IdPCallback) action.callbacks.get(0)).getScope()).isEqualTo(oidcTestClient.scopes());
        assertThat(((IdPCallback) action.callbacks.get(0)).getNonce()).isNull();
        assertThat(((IdPCallback) action.callbacks.get(0)).getAcrValues()).isEqualTo(oidcTestClient.acrValues());
    }

    @Test
    void processWithIdTokenResultsInCompletion() throws Exception {
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any(), any()))
                .thenThrow(new NotFoundException());
        when(config.clientType()).thenReturn(ClientType.NATIVE);

        IdPCallback callback = new IdPCallback(testClient.provider(),
                testClient.clientId(),
                testClient.redirectURI(),
                testClient.scopes(), null, null, null, null, false);

        callback.setTokenType("id_token");
        callback.setToken("id_token_value");

        TreeContext context = new TreeContext(json(object(
                field(SELECTED_IDP, PROVIDER_NAME),
                field(OBJECT_ATTRIBUTES, object(field("userName", "bob")))
        )), new ExternalRequestContext.Builder().build(),
                Collections.singletonList(callback),
                Optional.empty());

        Action action = node.process(context);

        assertThat(action.transientState.isDefined(SOCIAL_OAUTH_DATA)).isTrue();
        assertThat(action.transientState.get(SOCIAL_OAUTH_DATA).isDefined(PROVIDER_NAME)).isTrue();
        assertThat(action.transientState.get(SOCIAL_OAUTH_DATA).get(PROVIDER_NAME).isDefined("testData")).isTrue();
        assertThat(action.transientState.get(SOCIAL_OAUTH_DATA).get(PROVIDER_NAME).get("testData").asString())
                .isEqualTo("testValue");
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).isDefined(ALIAS_LIST)).isTrue();
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get(ALIAS_LIST).asList().size()).isEqualTo(1);
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get(ALIAS_LIST).asList(String.class).get(0))
                .isEqualTo(PROVIDER_NAME + "-user");
    }

    @Test
    void processWithAuthorizationCodeResultsInCompletion() throws Exception {
        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any(), any()))
                .thenThrow(new NotFoundException());
        when(config.clientType()).thenReturn(ClientType.NATIVE);

        IdPCallback callback = new IdPCallback(testClient.provider(),
                testClient.clientId(),
                testClient.redirectURI(),
                testClient.scopes(), null, null, null, null, false);

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

        assertThat(action.transientState.isDefined(SOCIAL_OAUTH_DATA)).isTrue();
        assertThat(action.transientState.get(SOCIAL_OAUTH_DATA).isDefined(PROVIDER_NAME)).isTrue();
        assertThat(action.transientState.get(SOCIAL_OAUTH_DATA).get(PROVIDER_NAME).isDefined("testData")).isTrue();
        assertThat(action.transientState.get(SOCIAL_OAUTH_DATA).get(PROVIDER_NAME).get("testData").asString())
                .isEqualTo("testValue");
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).isDefined(ALIAS_LIST)).isTrue();
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get(ALIAS_LIST).asList().size()).isEqualTo(1);
        assertThat(context.transientState.get(OBJECT_ATTRIBUTES).get(ALIAS_LIST).asList(String.class).get(0))
                .isEqualTo(PROVIDER_NAME + "-user");
    }

    private void setupOidcClient(String subject, boolean overrideOIDCSubject) throws URISyntaxException {
        OpenIDConnectClient openIDConnectClient = mock(OpenIDConnectClient.class);
        when(openIDConnectClient.getAuthRedirect(any(), any(), any()))
                .thenReturn(newResultPromise(new URI(PROVIDER_REDIRECT)));
        when(openIDConnectClient.handlePostAuth(any(), any())).thenAnswer(answer -> {
            DataStore dataStore = answer.getArgument(0);
            dataStore.storeData(json(object(field("testData", "testValue"))));
            return newResultPromise(json(object()));
        });
        when(openIDConnectClient.getUserInfo(any())).thenReturn(Promises.newResultPromise(new UserInfo() {
            @Override
            public String getSubject() {
                return subject;
            }

            @Override
            public JsonValue getRawProfile() {
                return overrideOIDCSubject ? json(object(field("subclaim", "oidc-user"))) : json(object());
            }
        }));
        when(authModuleHelper.newOAuthClient(any(Realm.class), any(OAuth2ClientConfig.class)))
                .thenReturn(openIDConnectClient);
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
        public Script transform() {
            return script;
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
        public Set<RevocationOption> revocationCheckOptions() {
            return Set.of();
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
        public Optional<Purpose<GenericSecret>> clientSecretPurpose() {
            return Optional.of(Purpose.PASSWORD);
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
        public Optional<String> wellKnownEndpoint() {
            return Optional.empty();
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

        @Override
        public String claims() {
            return "";
        }

        @Override
        public Set<RevocationOption> revocationCheckOptions() {
            return Set.of(RevocationOption.DISABLE_REVOCATION_CHECKING);
        }

        @Override
        public String authenticationIdKey() {
            return "subclaim";
        }

    }

    private class AppleTestClientRequestingUserInfo extends OidcTestClient implements AppleClientConfig {

        @Override
        public boolean requestNativeAppForUserInfo() {
            return true;
        }
    }
}
