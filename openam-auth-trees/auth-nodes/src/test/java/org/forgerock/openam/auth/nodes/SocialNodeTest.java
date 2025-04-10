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
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.EMAIL_ADDRESS;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.oauth.AbstractSocialAuthLoginNode.SocialAuthOutcome.ACCOUNT_EXISTS;
import static org.forgerock.openam.auth.nodes.oauth.AbstractSocialAuthLoginNode.SocialAuthOutcome.NO_ACCOUNT;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.http.Handler;
import org.forgerock.json.JsonValue;
import org.forgerock.oauth.UserInfo;
import org.forgerock.oauth.clients.oauth2.OAuth2Client;
import org.forgerock.oauth.clients.oauth2.OAuth2ClientConfiguration;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.oauth.ProfileNormalizer;
import org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper;
import org.forgerock.openam.core.realms.Realm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.RedirectCallback;

/**
 * Test the {@link org.forgerock.openam.auth.nodes.oauth.AbstractSocialAuthLoginNode}
 */
@ExtendWith(MockitoExtension.class)
public class SocialNodeTest {

    @Mock
    private SocialNode.Config config;
    @Mock
    private SocialOAuth2Helper helper;
    @Mock
    private OAuth2Client<OAuth2ClientConfiguration> client;
    @Mock
    private ProfileNormalizer profileNormalizer;
    @Mock
    private LegacyIdentityService identityService;
    @Mock
    private Realm realm;
    @Mock
    private Handler handler;
    private UUID nodeId;

    @BeforeEach
    void before() throws URISyntaxException, AuthLoginException {
        when(config.provider()).thenReturn("provider");
        when(config.authenticationIdKey()).thenReturn("idKey");
        when(config.clientId()).thenReturn("clientId");
        when(config.clientSecret()).thenReturn("clientSecret".toCharArray());
        when(config.authorizeEndpoint()).thenReturn("http://authoriseEndpoint");
        when(config.basicAuth()).thenReturn(true);
        when(config.userInfoEndpoint()).thenReturn("http://userEndpoint");
        when(config.tokenEndpoint()).thenReturn("http://tokenEndpoint");
        when(config.scopeString()).thenReturn("scope");
        when(config.scopeDelimiter()).thenReturn(" ");
        when(config.redirectURI()).thenReturn("http://redirectURI");
        when(helper.newOAuthClient(any(), any(OAuth2ClientConfiguration.class), any())).thenReturn(client);

        nodeId = UUID.randomUUID();

    }

    private void commonStubbings() throws Exception {
        when(config.cfgMixUpMitigation()).thenReturn(false);
        when(config.saveUserAttributesToSession()).thenReturn(false);
        when(profileNormalizer.getNormalisedAccountAttributes(any(), any(), any()))
                .thenReturn(singletonMap("attribute", singleton("value")));
        when(profileNormalizer.getNormalisedAttributes(any(), any(), any()))
                .thenReturn(singletonMap("mail", singleton("mail@mail")));
    }

    @Test
    void processCallForTheFirstTimeReturnARedirectCallbackWithTrackingCookie() throws Exception {
        //GIVEN
        String redirectURI = config.redirectURI();
        when(client.getAuthRedirect(any(), any(), any())).thenReturn(newResultPromise(new URI(redirectURI)));
        TreeContext context = new TreeContext(JsonValue.json(object(1)),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());
        SocialNode node = new SocialNode(handler, config, realm, helper, profileNormalizer, identityService, nodeId);

        //WHEN
        Action action = node.process(context);

        //THEN
        assertThat(action.sendingCallbacks()).isTrue();
        assertThat(action.callbacks).hasSize(1);
        assertThat(action.callbacks.get(0)).isInstanceOf(RedirectCallback.class);
        assertThat(((RedirectCallback) action.callbacks.get(0)).getTrackingCookie()).isTrue();
        assertThat(((RedirectCallback) action.callbacks.get(0)).getRedirectUrl()).isEqualTo(config.redirectURI());
    }

    @Test
    void processWithACodeAndTheUserHasAnIdentityReturnAccountExistOutcome() throws Exception {
        //GIVEN
        commonStubbings();
        Map<String, String[]> parameters = getStateAndCodeAsParameter();

        TreeContext context = getTreeContext(parameters);
        mockToProviderUser(Optional.of("user"));

        SocialNode node = new SocialNode(handler, config, realm, helper, profileNormalizer, identityService, nodeId);

        //WHEN
        Action action = node.process(context);

        //THEN
        assertThat(action.outcome).isEqualTo(ACCOUNT_EXISTS.name());
    }

    @Test
    void processWithACodeAndTheUserHasAnIdentityAddTheUserInSharedState() throws Exception {
        //GIVEN
        commonStubbings();
        Map<String, String[]> parameters = getStateAndCodeAsParameter();

        TreeContext context = getTreeContext(parameters);
        mockToProviderUser(Optional.of("user"));

        SocialNode node = new SocialNode(handler, config, realm, helper, profileNormalizer, identityService, nodeId);

        //WHEN
        Action action = node.process(context);

        //THEN
        assertThat(action.sharedState.isDefined(USERNAME)).isTrue();
        assertThat(action.sharedState.get(USERNAME).asString()).isEqualTo("user");
        assertThat(action.sharedState.isDefined("attributes")).isFalse();
        assertThat(action.sharedState.isDefined("userNames")).isFalse();
    }

    @Test
    void processWithACodeButWithoutStateThrowException() throws Exception {
        //GIVEN
        when(config.cfgMixUpMitigation()).thenReturn(false);
        Map<String, String[]> parameters = new HashMap<>();
        parameters.put("code", new String[]{"the_code"});

        TreeContext context = getTreeContext(parameters);

        SocialNode node = new SocialNode(handler, config, realm, helper, profileNormalizer, identityService, nodeId);

        //WHEN
        assertThatThrownBy(() -> node.process(context))
                //THEN
                .isInstanceOf(NodeProcessException.class)
                .hasMessageContaining("Not having the state could mean that this request did not come from the IDP");

    }

    @Test
    void processWithACodeAndUserDoesntMatchIdentityWillReturnNoAccount() throws Exception {
        //GIVEN
        commonStubbings();
        Map<String, String[]> parameters = getStateAndCodeAsParameter();
        TreeContext context = getTreeContext(parameters);
        mockToProviderUser(Optional.empty());

        SocialNode node = new SocialNode(handler, config, realm, helper, profileNormalizer, identityService, nodeId);

        //WHEN
        Action action = node.process(context);

        //THEN
        assertThat(action.outcome).isEqualTo(NO_ACCOUNT.name());
    }

    @Test
    void processWithACodeAndUserDoesntMatchIdentityWillPopulateSharedState() throws Exception {
        //GIVEN
        commonStubbings();
        Map<String, String[]> parameters = getStateAndCodeAsParameter();

        TreeContext context = getTreeContext(parameters);
        mockToProviderUser(Optional.empty());

        SocialNode node = new SocialNode(handler, config, realm, helper, profileNormalizer, identityService, nodeId);

        //WHEN
        Action action = node.process(context);

        //THEN
        assertThat(action.sharedState.isDefined("userInfo")).isTrue();
        assertThat(action.sharedState.get("userInfo").isDefined("attributes")).isTrue();
        assertThat(action.sharedState.get("userInfo").isDefined("userNames")).isTrue();
        assertThat(action.sharedState.get(EMAIL_ADDRESS).asString()).isEqualTo("mail@mail");
    }

    @Test
    void processWithACodeAndAttributeToSessionDeactivatedDoesntAddAttributeToSession() throws Exception {
        //GIVEN
        commonStubbings();
        Map<String, String[]> parameters = getStateAndCodeAsParameter();

        TreeContext context = getTreeContext(parameters);
        mockToProviderUser(Optional.empty());

        SocialNode node = new SocialNode(handler, config, realm, helper, profileNormalizer, identityService, nodeId);

        //WHEN
        Action action = node.process(context);

        //THEN
        assertThat(action.sessionProperties).isEmpty();
    }

    @Test
    void processWithACodeAndAttributeToSessionActivatedAddAttributeToSession() throws Exception {
        //GIVEN
        commonStubbings();
        Map<String, String[]> parameters = getStateAndCodeAsParameter();

        TreeContext context = getTreeContext(parameters);
        mockToProviderUser(Optional.empty());

        when(config.saveUserAttributesToSession()).thenReturn(true);
        SocialNode node = new SocialNode(handler, config, realm, helper, profileNormalizer, identityService, nodeId);

        //WHEN
        Action action = node.process(context);

        //THEN
        assertThat(action.sessionProperties.containsKey("mail")).isTrue();
        assertThat(action.sessionProperties.containsKey("attribute")).isFalse();
        assertThat(action.sessionProperties.get("mail")).isEqualTo("mail@mail");
    }

    @Test
    void processWithMixUpMitigateActivatedShouldThrowAnExceptionIfIssDoesntMatch() throws Exception {
        //GIVEN
        when(config.cfgMixUpMitigation()).thenReturn(false);
        when(config.issuer()).thenReturn("issuer");
        Map<String, String[]> parameters = getStateAndCodeAsParameter();
        parameters.put("iss", new String[]{"dummyIss"});
        parameters.put("client_id", new String[]{config.clientId()});

        TreeContext context = getTreeContext(parameters);

        when(config.cfgMixUpMitigation()).thenReturn(true);
        SocialNode node = new SocialNode(handler, config, realm, helper, profileNormalizer, identityService, nodeId);

        //WHEN
        try {
            node.process(context);
        } catch (NodeProcessException e) {
            //THEN
            assertThat(e.getMessage()).contains("OAuth 2.0 mix-up mitigation is enabled, but the provided iss");
        }
    }

    @Test
    void processWithMixUpMitigateActivatedShouldThrowAnExceptionIfClientIdDoesntMatch() throws Exception {
        //GIVEN
        when(config.cfgMixUpMitigation()).thenReturn(false);
        when(config.issuer()).thenReturn("issuer");
        Map<String, String[]> parameters = getStateAndCodeAsParameter();
        parameters.put("iss", new String[]{config.issuer()});
        parameters.put("client_id", new String[]{"dummy_clientId"});

        TreeContext context = getTreeContext(parameters);

        when(config.cfgMixUpMitigation()).thenReturn(true);
        SocialNode node = new SocialNode(handler, config, realm, helper, profileNormalizer, identityService, nodeId);

        try {
            node.process(context);
        } catch (NodeProcessException e) {
            //THEN
            assertThat(e.getMessage()).contains("OAuth 2.0 mix-up mitigation is enabled, but the provided "
                    + "client_id");
        }
    }

    @Test
    void processWithMixUpMitigateActivatedShouldntThrowAnExceptionIfIssAndClientIdMatch() throws Exception {
        //GIVEN
        commonStubbings();
        when(config.issuer()).thenReturn("issuer");
        Map<String, String[]> parameters = getStateAndCodeAsParameter();
        parameters.put("iss", new String[]{config.issuer()});
        parameters.put("client_id", new String[]{config.clientId()});

        TreeContext context = getTreeContext(parameters);

        mockToProviderUser(Optional.empty());

        when(config.cfgMixUpMitigation()).thenReturn(true);
        SocialNode node = new SocialNode(handler, config, realm, helper, profileNormalizer, identityService, nodeId);

        //WHEN
        assertThatCode(() -> node.process(context)).doesNotThrowAnyException();
    }

    private Map<String, String[]> getStateAndCodeAsParameter() {
        Map<String, String[]> parameters = new HashMap<>();
        parameters.put("code", new String[]{"the_code"});
        parameters.put("state", new String[]{"the_state"});
        return parameters;
    }

    private void mockToProviderUser(Optional<String> user) {
        UserInfo userInfo = mock(UserInfo.class);
        when(helper.userExistsInTheDataStore(any(), any(), any())).thenReturn(user);
        when(client.handlePostAuth(any(), any())).thenReturn(newResultPromise(JsonValue.json(object())));
        when(client.getUserInfo(any())).thenReturn(newResultPromise(userInfo));
    }

    private TreeContext getTreeContext(Map<String, String[]> parameters) {
        JsonValue sharedState = JsonValue.json(object(field(
                "redirectRequestSentForNode-" + nodeId, true)));
        return new TreeContext(sharedState,
                new ExternalRequestContext.Builder()
                        .parameters(parameters)
                        .build(),
                emptyList(), Optional.empty());
    }
}
