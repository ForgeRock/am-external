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
 * Copyright 2018-2020 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.EMAIL_ADDRESS;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.oauth.AbstractSocialAuthLoginNode.SocialAuthOutcome.ACCOUNT_EXISTS;
import static org.forgerock.openam.auth.nodes.oauth.AbstractSocialAuthLoginNode.SocialAuthOutcome.NO_ACCOUNT;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.RedirectCallback;

import org.forgerock.openam.identity.idm.IdentityUtils;
import org.mockito.Mock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test the {@link org.forgerock.openam.auth.nodes.oauth.AbstractSocialAuthLoginNode}
 */
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
    private IdentityUtils identityUtils;

    @BeforeMethod
    public void before() throws URISyntaxException, AuthLoginException {
        initMocks(this);
        when(config.provider()).thenReturn("provider");
        when(config.authenticationIdKey()).thenReturn("idKey");
        when(config.clientId()).thenReturn("clientId");
        when(config.clientSecret()).thenReturn("clientSecret".toCharArray());
        when(config.authorizeEndpoint()).thenReturn("http://authoriseEndpoint");
        when(config.basicAuth()).thenReturn(true);
        when(config.cfgAccountMapperClass()).thenReturn("accountMapperClass");
        when(config.cfgAccountProviderClass()).thenReturn("accountProviderClass");
        when(config.cfgAttributeMappingConfiguration()).thenReturn(singletonMap("sub", "uid"));
        when(config.cfgMixUpMitigation()).thenReturn(false);
        when(config.cfgAccountMapperConfiguration()).thenReturn(singletonMap("sub", "uid"));
        when(config.userInfoEndpoint()).thenReturn("http://userEndpoint");
        when(config.tokenEndpoint()).thenReturn("http://tokenEndpoint");
        when(config.scopeString()).thenReturn("scope");
        when(config.scopeDelimiter()).thenReturn(" ");
        when(config.saveUserAttributesToSession()).thenReturn(false);
        when(config.redirectURI()).thenReturn("http://redirectURI");
        when(config.issuer()).thenReturn("issuer");
        when(config.cfgAttributeMappingClasses()).thenReturn(singleton("JsonAttributeMapper"));

        when(helper.newOAuthClient(any())).thenReturn(client);
        String redirectURI = config.redirectURI();
        when(client.getAuthRedirect(any(), any(), any())).thenReturn(newResultPromise(new URI(redirectURI)));

        when(profileNormalizer.getNormalisedAccountAttributes(any(), any(), any()))
                .thenReturn(singletonMap("attribute", singleton("value")));
        when(profileNormalizer.getNormalisedAttributes(any(), any(), any()))
                .thenReturn(singletonMap("mail", singleton("mail@mail")));

    }

    @Test
    public void processCallForTheFirstTimeReturnARedirectCallbackWithTrackingCookie() throws Exception {
        //GIVEN
        TreeContext context = new TreeContext(JsonValue.json(object(1)),
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());
        SocialNode node = new SocialNode(config, helper, profileNormalizer, identityUtils);

        //WHEN
        Action action = node.process(context);

        //THEN
        Assert.assertTrue(action.sendingCallbacks());
        Assert.assertEquals(action.callbacks.size(), 1);
        Assert.assertTrue(action.callbacks.get(0) instanceof RedirectCallback);
        Assert.assertTrue(((RedirectCallback) action.callbacks.get(0)).getTrackingCookie());
        Assert.assertEquals(((RedirectCallback) action.callbacks.get(0)).getRedirectUrl(), config.redirectURI());
    }

    @Test
    public void processWithACodeAndTheUserHasAnIdentityReturnAccountExistOutcome() throws Exception {
        //GIVEN
        Map<String, String[]> parameters = getStateAndCodeAsParameter();

        TreeContext context = getTreeContext(parameters);
        mockToProviderUser(Optional.of("user"));

        SocialNode node = new SocialNode(config, helper, profileNormalizer, identityUtils);

        //WHEN
        Action action = node.process(context);

        //THEN
        Assert.assertEquals(action.outcome, ACCOUNT_EXISTS.name());
    }

    @Test
    public void processWithACodeAndTheUserHasAnIdentityAddTheUserInSharedState() throws Exception {
        //GIVEN
        Map<String, String[]> parameters = getStateAndCodeAsParameter();

        TreeContext context = getTreeContext(parameters);
        mockToProviderUser(Optional.of("user"));

        SocialNode node = new SocialNode(config, helper, profileNormalizer, identityUtils);

        //WHEN
        Action action = node.process(context);

        //THEN
        Assert.assertTrue(action.sharedState.isDefined(USERNAME));
        Assert.assertEquals(action.sharedState.get(USERNAME).asString(), "user");
        Assert.assertFalse(action.sharedState.isDefined("attributes"));
        Assert.assertFalse(action.sharedState.isDefined("userNames"));
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void processWithACodeButWithoutStateThrowException() throws Exception {
        //GIVEN
        Map<String, String[]> parameters = new HashMap<>();
        parameters.put("code", new String[] {"the_code"});

        TreeContext context = getTreeContext(parameters);
        mockToProviderUser(Optional.of("user"));

        SocialNode node = new SocialNode(config, helper, profileNormalizer, identityUtils);

        //WHEN
        node.process(context);

        //THEN
        // throw an exception

    }

    @Test
    public void processWithACodeAndUserDoesntMatchIdentityWillReturnNoAccount() throws Exception {
        //GIVEN
        Map<String, String[]> parameters = getStateAndCodeAsParameter();
        TreeContext context = getTreeContext(parameters);
        mockToProviderUser(Optional.empty());

        SocialNode node = new SocialNode(config, helper, profileNormalizer, identityUtils);

        //WHEN
        Action action = node.process(context);

        //THEN
        Assert.assertEquals(action.outcome, NO_ACCOUNT.name());
    }

    @Test
    public void processWithACodeAndUserDoesntMatchIdentityWillPopulateSharedState() throws Exception {
        //GIVEN
        Map<String, String[]> parameters = getStateAndCodeAsParameter();

        TreeContext context = getTreeContext(parameters);
        mockToProviderUser(Optional.empty());

        SocialNode node = new SocialNode(config, helper, profileNormalizer, identityUtils);

        //WHEN
        Action action = node.process(context);

        //THEN
        Assert.assertTrue(action.sharedState.isDefined("userInfo"));
        Assert.assertTrue(action.sharedState.get("userInfo").isDefined("attributes"));
        Assert.assertTrue(action.sharedState.get("userInfo").isDefined("userNames"));
        Assert.assertEquals(action.sharedState.get(EMAIL_ADDRESS).asString(), "mail@mail");
    }

    @Test
    public void processWithACodeAndAttributeToSessionDeactivatedDoesntAddAttributeToSession() throws Exception {
        //GIVEN
        Map<String, String[]> parameters = getStateAndCodeAsParameter();

        TreeContext context = getTreeContext(parameters);
        mockToProviderUser(Optional.empty());

        SocialNode node = new SocialNode(config, helper, profileNormalizer, identityUtils);

        //WHEN
        Action action = node.process(context);

        //THEN
        Assert.assertEquals(action.sessionProperties.size(), 0);
    }

    @Test
    public void processWithACodeAndAttributeToSessionActivatedAddAttributeToSession() throws Exception {
        //GIVEN
        Map<String, String[]> parameters = getStateAndCodeAsParameter();

        TreeContext context = getTreeContext(parameters);
        mockToProviderUser(Optional.empty());

        when(config.saveUserAttributesToSession()).thenReturn(true);
        SocialNode node = new SocialNode(config, helper, profileNormalizer, identityUtils);

        //WHEN
        Action action = node.process(context);

        //THEN
        Assert.assertTrue(action.sessionProperties.containsKey("mail"));
        Assert.assertFalse(action.sessionProperties.containsKey("attribute"));
        Assert.assertEquals(action.sessionProperties.get("mail"), "mail@mail");
    }

    @Test
    public void processWithMixUpMitigateActivatedShouldThrowAnExceptionIfIssDoesntMatch() throws Exception {
        //GIVEN
        Map<String, String[]> parameters = getStateAndCodeAsParameter();
        parameters.put("iss", new String[] { "dummyIss" });
        parameters.put("client_id", new String[] { config.clientId() });

        TreeContext context = getTreeContext(parameters);
        mockToProviderUser(Optional.empty());

        when(config.cfgMixUpMitigation()).thenReturn(true);
        SocialNode node = new SocialNode(config, helper, profileNormalizer, identityUtils);

        //WHEN
        try {
            node.process(context);
        } catch (NodeProcessException e) {
            //THEN
            Assert.assertTrue(e.getMessage().contains("OAuth 2.0 mix-up mitigation is enabled, but the provided"
                    + " iss"));
        }
    }

    @Test
    public void processWithMixUpMitigateActivatedShouldThrowAnExceptionIfClientIdDoesntMatch() throws Exception {
        //GIVEN
        Map<String, String[]> parameters = getStateAndCodeAsParameter();
        parameters.put("iss", new String[] { config.issuer() });
        parameters.put("client_id", new String[] { "dummy_clientId" });

        TreeContext context = getTreeContext(parameters);
        mockToProviderUser(Optional.empty());

        when(config.cfgMixUpMitigation()).thenReturn(true);
        SocialNode node = new SocialNode(config, helper, profileNormalizer, identityUtils);

        try {
            node.process(context);
        } catch (NodeProcessException e) {
            //THEN
            Assert.assertTrue(e.getMessage().contains("OAuth 2.0 mix-up mitigation is enabled, but the provided "
                    + "client_id"));
        }
    }

    @Test
    public void processWithMixUpMitigateActivatedShouldntThrowAnExceptionIfIssAndClientIdMatch() throws Exception {
        //GIVEN
        Map<String, String[]> parameters = getStateAndCodeAsParameter();
        parameters.put("iss", new String[] { config.issuer() });
        parameters.put("client_id", new String[] { config.clientId() });

        TreeContext context = getTreeContext(parameters);

        mockToProviderUser(Optional.empty());

        when(config.cfgMixUpMitigation()).thenReturn(true);
        SocialNode node = new SocialNode(config, helper, profileNormalizer, identityUtils);

        //WHEN
        node.process(context);

        //THEN
        // no exception
        Assert.assertTrue(true);
    }

    private Map<String, String[]> getStateAndCodeAsParameter() {
        Map<String, String[]> parameters = new HashMap<>();
        parameters.put("code", new String[] {"the_code"});
        parameters.put("state", new String[] {"the_state"});
        return parameters;
    }

    private void mockToProviderUser(Optional<String> user) {
        UserInfo userInfo = mock(UserInfo.class);
        when(helper.userExistsInTheDataStore(any(), any(), any())).thenReturn(user);
        when(client.handlePostAuth(any(), any())).thenReturn(newResultPromise(JsonValue.json(object())));
        when(client.getUserInfo(any())).thenReturn(newResultPromise(userInfo));
    }

    private TreeContext getTreeContext(Map<String, String[]> parameters) {
        return new TreeContext(JsonValue.json(object(1)),
                    new ExternalRequestContext.Builder()
                            .parameters(parameters)
                            .build(),
                    emptyList(), Optional.empty());
    }
}