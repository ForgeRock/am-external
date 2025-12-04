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
 * Copyright 2018-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.ProvisionDynamicAccountNode.ACTIVE;
import static org.forgerock.openam.auth.nodes.ProvisionDynamicAccountNode.USER_PASSWORD;
import static org.forgerock.openam.auth.nodes.ProvisionDynamicAccountNode.USER_STATUS;
import static org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper.USER_INFO_SHARED_STATE_KEY;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyMap;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.verify;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.security.auth.callback.Callback;

import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.identity.authentication.spi.AuthLoginException;

public class ProvisionDynamicAccountNodeTest {

    private TreeContext treeContext;
    private List<Callback> callbacks;
    private ProvisionDynamicAccountNode node;

    private final String user = "squirrel";
    private final String password = "hazelnuts";

    @Mock
    private SocialOAuth2Helper authModuleHelper;

    @Mock
    private ProvisionDynamicAccountNode.Config config;

    @Mock
    private LegacyIdentityService identityService;

    @Mock
    private IdmIntegrationService idmIntegrationService;

    @Mock
    private Realm realm;

    private JsonValue sharedState;

    private JsonValue transientState;

    ProvisionDynamicAccountNodeTest() throws AuthLoginException {
        MockitoAnnotations.initMocks(this);

        given(authModuleHelper.provisionUser(anyString(), any(), anyMap())).willReturn(user);
        given(config.accountProviderClass())
                .willReturn(
                        "org.forgerock.openam.authentication.modules.common.mapping.DefaultAccountProvider");
        given(realm.asPath()).willReturn("/");

    }

    @Nested
    @DisplayName(value = "Provision Dynamic Account Node")
    class ProvisionDynamicAccountNodeNested {

        @Nested
        @DisplayName(value = "there is no userinfo in the session")
        class ThereIsNoUserinfoInTheSession {

            @BeforeEach
            public void beforeEach() {
                node = new ProvisionDynamicAccountNode(config, realm, authModuleHelper, identityService,
                        idmIntegrationService);
                sharedState = json(object(field("state", "initial")));
                transientState = json(object(field("state", "initial")));
            }

            @Test
            @DisplayName(value = "throws an exception")
            public void testThrowsAnException() throws Exception {
                TreeContext context = getContext(Collections.EMPTY_LIST);
                assertThatThrownBy(() -> node.process(context)).isExactlyInstanceOf(NodeProcessException.class);
            }
        }

        @Nested
        @DisplayName(value = "no password has been provided")
        class NoPasswordHasBeenProvided {

            @BeforeEach
            public void beforeEach() throws NodeProcessException {
                node = new ProvisionDynamicAccountNode(config, realm, authModuleHelper, identityService,
                        idmIntegrationService);
                sharedState = json(object(field("userInfo", getUserInfo()), field(REALM, realm.asPath())));
                transientState = json(object(field("state", "initial")));
                given(authModuleHelper.getRandomData()).willReturn("myRandomPassword");
            }

            @Test
            @DisplayName(value = "creates an account with a generated password")
            public void testCreatesAnAccountWithAGeneratedPassword() throws Exception {
                TreeContext context = getContext(Collections.EMPTY_LIST);
                Action result = node.process(context);

                verify(authModuleHelper).getRandomData();
                Map<String, Set<String>> userAttributes = SocialOAuth2Helper.convertMapOfListToMapOfSet(
                        context.sharedState.get(USER_INFO_SHARED_STATE_KEY)
                                .get("attributes")
                                .asMapOfList(String.class));
                userAttributes.put(USER_PASSWORD, singleton("myRandomPassword"));
                userAttributes.put(USER_STATUS, Collections.singleton(ACTIVE));

                verify(authModuleHelper).provisionUser(any(), any(), eq(userAttributes));
                assertThat(result.outcome).isEqualTo("outcome");
                assertThat(result.sharedState.get(USERNAME).asString()).isEqualTo(user);
                assertThat(result.identifiedIdentity).isPresent();
                assertThat(result.identifiedIdentity.get().getUsername()).isEqualTo(user);

            }
        }

        @Nested
        @DisplayName(value = "a password has already been provided")
        class APasswordHasAlreadyBeenProvided {

            @BeforeEach
            public void beforeEach() {
                node = new ProvisionDynamicAccountNode(config, realm, authModuleHelper, identityService,
                        idmIntegrationService);
                sharedState = json(object(field("userInfo", getUserInfo()), field(REALM, realm.asPath())));
                transientState = json(object(field(PASSWORD, password)));
            }

            @Test
            @DisplayName(value = "creates an account with the supplied password")
            public void testCreatesAnAccountWithTheSuppliedPassword() throws Exception {
                TreeContext context = getContext(Collections.EMPTY_LIST);
                Action result = node.process(context);

                verify(authModuleHelper, never()).getRandomData();
                assertThat(result.transientState).isNullOrEmpty();
                assertThat(result.outcome).isEqualTo("outcome");
                assertThat(result.sharedState.get(USERNAME).asString()).isEqualTo(user);
                assertThat(result.identifiedIdentity).isPresent();
                assertThat(result.identifiedIdentity.get().getUsername()).isEqualTo(user);
            }
        }

        @Nested
        @DisplayName(value = "in a subrealm")
        class InASubrealm {

            @BeforeEach
            public void beforeEach() {
                given(realm.asPath()).willReturn("/subrealm1/subrealm2");
                node = new ProvisionDynamicAccountNode(config, realm, authModuleHelper, identityService,
                        idmIntegrationService);
                sharedState = json(object(field("userInfo", getUserInfo()), field(REALM, realm.asPath())));
                transientState = json(object(field(PASSWORD, password)));
            }

            @Test
            @DisplayName(value = "creates an account with the supplied password")
            public void testCreatesAnAccountWithTheSuppliedPassword() throws Exception {
                TreeContext context = getContext(Collections.EMPTY_LIST);
                Action result = node.process(context);
                assertThat(result.transientState).isNullOrEmpty();
                assertThat(result.outcome).isEqualTo("outcome");
                assertThat(result.sharedState.get(USERNAME).asString()).isEqualTo(user);
                assertThat(result.identifiedIdentity).isPresent();
                assertThat(result.identifiedIdentity.get().getUsername()).isEqualTo(user);
            }
        }

        @Nested
        @DisplayName(value = "an invalid account provider has been configured")
        class AnInvalidAccountProviderHasBeenConfigured {

            @BeforeEach
            public void beforeEach() {
                given(config.accountProviderClass())
                        .willReturn("org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper");
                node = new ProvisionDynamicAccountNode(config, realm, authModuleHelper, identityService,
                        idmIntegrationService);
                sharedState = json(object(field("userInfo", getUserInfo()), field(REALM, realm)));
                transientState = json(object(field(PASSWORD, password)));
            }

            @Test
            @DisplayName(value = "throws an exception")
            public void testThrowsAnException() throws Exception {
                TreeContext context = getContext(Collections.EMPTY_LIST);
                assertThatThrownBy(() -> node.process(context)).isExactlyInstanceOf(NodeProcessException.class);
            }
        }
    }

    private TreeContext getContext(List<Callback> callbacks) {
        return new TreeContext(sharedState, transientState, new Builder().build(), callbacks, Optional.empty());
    }

    private JsonValue getUserInfo() {
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("mail", singletonList("wood.mouse@forestschool.org"));
        Map<String, List<String>> userNames = new HashMap<>();
        userNames.put("iplanet-am-user-alias-list", singletonList("google-1234567890"));
        return json(object(
                field("attributes", attributes),
                field("userNames", userNames)
        ));
    }

}
