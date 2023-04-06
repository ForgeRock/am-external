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
 * Copyright 2018-2022 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.cuppa.Cuppa.before;
import static org.forgerock.cuppa.Cuppa.beforeEach;
import static org.forgerock.cuppa.Cuppa.describe;
import static org.forgerock.cuppa.Cuppa.it;
import static org.forgerock.cuppa.Cuppa.when;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;

import java.security.KeyPairGenerator;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.openam.integration.idm.ClientTokenJwtGenerator;
import org.forgerock.openam.integration.idm.IdmIntegrationConfig;
import org.forgerock.openam.integration.idm.IdmIntegrationService;

import com.sun.identity.authentication.spi.RedirectCallback;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@Test
@RunWith(CuppaRunner.class)
public class ProvisionIdmAccountNodeTest {

    private static final String IDM_URL = "http://idmservice:9999";
    private TreeContext treeContext;
    private List<Callback> callbacks;
    private ProvisionIdmAccountNode node;

    private final String user = "squirrel";
    private final String password = "hazelnuts";

    @Mock private SocialOAuth2Helper authModuleHelper;
    @Mock private ProvisionIdmAccountNode.Config config;
    @Mock private IdmIntegrationService idmIntegrationService;
    @Mock private Certificate certificate;
    @Mock private Realm realm;
    @Mock private ClientTokenJwtGenerator clientTokenJwtGenerator;
    @Mock private IdmIntegrationConfig configProvider;
    @Mock private IdmIntegrationConfig.GlobalConfig idmConfig;
    @Mock private LegacyIdentityService identityService;

    private JsonValue initialSharedState;
    private JsonValue transientSharedState;

    {
        describe("Provision IDM Account Node", () -> {
            before(() -> {
                MockitoAnnotations.initMocks(this);
                given(configProvider.global())
                        .willReturn(idmConfig);
                given(idmConfig.enabled()).willReturn(true);
                given(idmConfig.idmDeploymentUrl()).willReturn(IDM_URL);
                given(idmConfig.provisioningSigningKeyAlias()).willReturn("test");
                given(idmConfig.provisioningEncryptionKeyAlias()).willReturn("test");
                given(idmConfig.provisioningSigningAlgorithm()).willReturn("HS256");
                given(idmConfig.provisioningEncryptionAlgorithm()).willReturn("RSAES_PKCS1_V1_5");
                given(idmConfig.provisioningEncryptionMethod()).willReturn("A128CBC_HS256");

                given(authModuleHelper.userExistsInTheDataStore(any(), any(), any()))
                        .willReturn(Optional.of(user));

                given(config.accountProviderClass())
                        .willReturn(
                                "org.forgerock.openam.authentication.modules.common.mapping.DefaultAccountProvider");
                given(realm.asPath())
                        .willReturn("/");
                given(certificate.getPublicKey())
                        .willReturn(KeyPairGenerator.getInstance("RSA").generateKeyPair().getPublic());

            });

            when("valid configuration is passed", () -> {
                beforeEach(() -> {
                    node = new ProvisionIdmAccountNode(config,
                            realm, authModuleHelper, clientTokenJwtGenerator, configProvider, identityService);
                    initialSharedState = json(object(field("content", "initial")));
                    transientSharedState = json(object(field("content", "initial")));

                });
                it("returns a valid request", () -> {
                    TreeContext context = getContext(initialSharedState, transientSharedState, Collections.EMPTY_LIST);
                    Action result = node.process(context);
                    assertThat(result.callbacks).hasSize(1);
                    assertThat(result.callbacks.get(0)).isInstanceOf(RedirectCallback.class);
                    assertThat(((RedirectCallback) result.callbacks.get(0)).getRedirectUrl())
                            .startsWith(IDM_URL + "/#handleOAuth/&");
                });
            });

            when("resuming from a callout to IDM", () -> {
                beforeEach(() -> {
                    node = new ProvisionIdmAccountNode(config,
                            realm, authModuleHelper, clientTokenJwtGenerator, configProvider, identityService);
                    initialSharedState = json(object(field("userInfo", getUserInfo()),
                            field(REALM, realm.asPath()),
                            field(ProvisionIdmAccountNode.IDM_FLOW_INITIATED_KEY, true)));
                });
                it("goes to a successful outcome", () -> {
                    TreeContext context = getContext(initialSharedState, transientSharedState, Collections.EMPTY_LIST);
                    Action result = node.process(context);
                    assertThat(result.outcome).isEqualTo("outcome");
                    assertThat(result.sharedState.get(USERNAME).asString()).isEqualTo(user);
                });
            });

            when("an invalid account provider has been configured", () -> {
                beforeEach(() -> {
                    given(config.accountProviderClass())
                            .willReturn("org.forgerock.openam.auth.nodes.oauth.SocialOAuth2Helper");
                    node = new ProvisionIdmAccountNode(config,
                            realm, authModuleHelper, clientTokenJwtGenerator, configProvider, identityService);
                    initialSharedState = json(object(field("userInfo", getUserInfo()),
                            field(REALM, realm),
                            field(ProvisionIdmAccountNode.IDM_FLOW_INITIATED_KEY, Boolean.TRUE)));
                });
                it("an exception is thrown", () -> {
                    TreeContext context = getContext(initialSharedState, transientSharedState, Collections.EMPTY_LIST);
                    assertThatThrownBy(() -> node.process(context)).isExactlyInstanceOf(NodeProcessException.class);
                });
            });
        });
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState, List<Callback> callbacks) {
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
