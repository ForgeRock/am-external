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
 * Copyright 2018 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;
import static org.forgerock.cuppa.Cuppa.*;
import static org.forgerock.cuppa.Cuppa.when;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.*;
import static org.forgerock.openam.auth.nodes.ProvisionDynamicAccountNode.*;
import static org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper.USER_INFO_SHARED_STATE_KEY;
import static org.mockito.BDDMockito.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.callback.Callback;

import org.forgerock.cuppa.Test;
import org.forgerock.cuppa.junit.CuppaRunner;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper;
import org.forgerock.openam.core.realms.Realm;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@Test
@RunWith(CuppaRunner.class)
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
    private Realm realm;

    private JsonValue sharedState;

    private JsonValue transientState;

    {
        describe("Provision Dynamic Account Node", () -> {
            beforeEach(() -> {
                MockitoAnnotations.initMocks(this);

                given(authModuleHelper.provisionUser(anyString(), any(), anyMap())).willReturn(user);
                given(config.accountProviderClass())
                        .willReturn(
                                "org.forgerock.openam.authentication.modules.common.mapping.DefaultAccountProvider");
                given(realm.asPath()).willReturn("/");
            });

            when("there is no userinfo in the session", () -> {
                beforeEach(() -> {
                    node = new ProvisionDynamicAccountNode(config, realm, authModuleHelper);
                    sharedState = json(object(field("state", "initial")));
                    transientState = json(object(field("state", "initial")));
                });
                it("throws an exception", () -> {
                    TreeContext context = getContext(Collections.EMPTY_LIST);
                    assertThatThrownBy(() -> node.process(context)).isExactlyInstanceOf(NodeProcessException.class);
                });
            });

            when("no password has been provided", () -> {
                beforeEach(() -> {
                    node = new ProvisionDynamicAccountNode(config, realm, authModuleHelper);
                    sharedState = json(object(field("userInfo", getUserInfo()), field(REALM, realm)));
                    transientState = json(object(field("state", "initial")));
                    given(authModuleHelper.getRandomData()).willReturn("myRandomPassword");
                });
                it("creates an account with a generated password", () -> {
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

                });
            });

            when("a password has already been provided", () -> {
                beforeEach(() -> {
                    node = new ProvisionDynamicAccountNode(config, realm, authModuleHelper);
                    sharedState = json(object(field("userInfo", getUserInfo()), field(REALM, realm)));
                    transientState = json(object(field(PASSWORD, password)));
                });
                it("creates an account with the supplied password", () -> {
                    TreeContext context = getContext(Collections.EMPTY_LIST);
                    Action result = node.process(context);

                    verify(authModuleHelper, never()).getRandomData();
                    assertThat(result.transientState).isNullOrEmpty();
                    assertThat(result.outcome).isEqualTo("outcome");
                    assertThat(result.sharedState.get(USERNAME).asString()).isEqualTo(user);
                });
            });

            when("in a subrealm", () -> {
                beforeEach(() -> {
                    given(realm.asPath()).willReturn("/subrealm1/subrealm2");
                    node = new ProvisionDynamicAccountNode(config, realm, authModuleHelper);
                    sharedState = json(object(field("userInfo", getUserInfo()), field(REALM, realm)));
                    transientState = json(object(field(PASSWORD, password)));
                });
                it("creates an account with the supplied password", () -> {
                    TreeContext context = getContext(Collections.EMPTY_LIST);
                    Action result = node.process(context);
                    assertThat(result.transientState).isNullOrEmpty();
                    assertThat(result.outcome).isEqualTo("outcome");
                    assertThat(result.sharedState.get(USERNAME).asString()).isEqualTo(user);
                });
            });

            when("an invalid account provider has been configured", () -> {
                beforeEach(() -> {
                    given(config.accountProviderClass())
                            .willReturn("org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper");
                    node = new ProvisionDynamicAccountNode(config, realm, authModuleHelper);
                    sharedState = json(object(field("userInfo", getUserInfo()), field(REALM, realm)));
                    transientState = json(object(field(PASSWORD, password)));
                });
                it("throws an exception", () -> {
                    TreeContext context = getContext(Collections.EMPTY_LIST);
                    assertThatThrownBy(() -> node.process(context)).isExactlyInstanceOf(NodeProcessException.class);
                });
            });

        });
    }

    private TreeContext getContext(List<Callback> callbacks) {
        return new TreeContext(sharedState, transientState, new Builder().build(), callbacks);
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
