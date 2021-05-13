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

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.cuppa.Cuppa.beforeEach;
import static org.forgerock.cuppa.Cuppa.describe;
import static org.forgerock.cuppa.Cuppa.it;
import static org.forgerock.cuppa.Cuppa.when;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.NODE_TYPE;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.RecoveryCodeCollectorDecisionNode.OATH_AUTH_TYPE;
import static org.forgerock.openam.auth.nodes.RecoveryCodeCollectorDecisionNode.PUSH_AUTH_TYPE;
import static org.forgerock.openam.auth.nodes.RecoveryCodeCollectorDecisionNode.WEB_AUTHN_AUTH_TYPE;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;

import org.forgerock.cuppa.Test;
import org.forgerock.cuppa.junit.CuppaRunner;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.RecoveryCodeCollectorDecisionNode.Config;
import org.forgerock.openam.auth.nodes.RecoveryCodeCollectorDecisionNode.RecoveryCodeType;
import org.forgerock.openam.core.rest.devices.DeviceProfileManager;
import org.forgerock.openam.core.rest.devices.DeviceSettings;
import org.forgerock.openam.core.rest.devices.oath.OathDeviceSettings;
import org.forgerock.openam.core.rest.devices.oath.UserOathDeviceProfileManager;
import org.forgerock.openam.core.rest.devices.push.PushDeviceSettings;
import org.forgerock.openam.core.rest.devices.push.UserPushDeviceProfileManager;
import org.forgerock.openam.core.rest.devices.webauthn.UserWebAuthnDeviceProfileManager;
import org.forgerock.openam.core.rest.devices.webauthn.WebAuthnDeviceSettings;

import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@Test
@RunWith(CuppaRunner.class)
@SuppressWarnings("unchecked")
public class RecoveryCodeCollectorDecisionNodeTest {

    private final String user = "badger";
    private final String realm = "weasel";
    @Mock
    private Config config;
    @Mock
    private UserOathDeviceProfileManager oathDeviceManager;
    @Mock
    private UserPushDeviceProfileManager pushDeviceManager;
    @Mock
    private UserWebAuthnDeviceProfileManager webAuthnDeviceManager;

    private OathDeviceSettings oathDeviceSettings;
    private PushDeviceSettings pushDeviceSettings;
    private WebAuthnDeviceSettings webAuthnDeviceSettings;

    private DeviceSettings settings;
    private DeviceProfileManager<? extends DeviceSettings> manager;
    private String deviceType;
    private TreeContext treeContext;
    private List<Callback> callbacks;
    private RecoveryCodeCollectorDecisionNode node;

    {
        describe("Recovery Code Collector Decision Node", () -> {
            beforeEach(() -> {
                MockitoAnnotations.initMocks(this);
                oathDeviceSettings = new OathDeviceSettings();
                pushDeviceSettings = new PushDeviceSettings();
                webAuthnDeviceSettings = new WebAuthnDeviceSettings();
            });
            for (String type : Arrays.asList("OATH", "PUSH", "WEB_AUTHN")) {
                when("recovery code type is: " + type, () -> {
                    beforeEach(() -> {
                        given(config.recoveryCodeType()).willReturn(RecoveryCodeType.valueOf(type));
                        node = new RecoveryCodeCollectorDecisionNode(config, pushDeviceManager, oathDeviceManager,
                                webAuthnDeviceManager);

                        switch (type) {
                        case "OATH":
                            manager = oathDeviceManager;
                            settings = oathDeviceSettings;
                            deviceType = OATH_AUTH_TYPE;
                            break;
                        case "PUSH":
                            manager = pushDeviceManager;
                            settings = pushDeviceSettings;
                            deviceType = PUSH_AUTH_TYPE;
                            break;
                        default:
                            manager = webAuthnDeviceManager;
                            settings = webAuthnDeviceSettings;
                            deviceType = WEB_AUTHN_AUTH_TYPE;
                            break;
                        }
                    });
                    when("there is no callback provided", () -> {
                        beforeEach(() -> {
                            callbacks = Collections.emptyList();
                            treeContext = getContext();
                        });
                        it("returns a NameCallback", this::returnsNameCallback);
                    });
                    when("an empty callback value is provided", () -> {
                        beforeEach(() -> {
                            NameCallback nameCallback = new NameCallback(("foo"));
                            nameCallback.setName("");
                        });
                        it("returns a NameCallback", this::returnsNameCallback);
                    });
                    when("a code is provided", () -> {
                        beforeEach(() -> {
                            NameCallback nameCallback = new NameCallback("foo");
                            nameCallback.setName("ferret");
                            callbacks = singletonList(nameCallback);
                            treeContext = getContext();
                        });
                        it("retrieves the recovery codes using the correct dao", () -> {
                            Action result = node.process(treeContext);

                            assertThat(result.outcome).isEqualTo("false");
                            verify(manager).getDeviceProfiles("badger", "weasel");
                        });
                        it("returns true if the recovery code is correct", () -> {
                            mockRecoveryCodes("ferret");
                            Action result = node.process(treeContext);

                            assertThat(result.outcome).isEqualTo("true");
                        });
                        it("returns false if the recovery code is incorrect", () -> {
                            mockRecoveryCodes("otter");
                            Action result = node.process(treeContext);

                            assertThat(result.outcome).isEqualTo("false");
                        });
                        it("removes the code from the list", () -> {
                            mockRecoveryCodes("ferret", "otter");
                            int recoveryCodesSize = settings.getRecoveryCodes().size();
                            Action result = node.process(treeContext);

                            assertThat(result.outcome).isEqualTo("true");
                            assertThat(settings.getRecoveryCodes().size()).isLessThan(recoveryCodesSize);
                        });
                        it("returns correct NodeType if the recovery code is correct", () -> {
                            mockRecoveryCodes("ferret");
                            Action result = node.process(treeContext);
                            String nodeType = result.sessionProperties.get(NODE_TYPE);

                            assertThat(nodeType).contains(deviceType);
                        });
                        it("returns no NodeType if the recovery code is incorrect", () -> {
                            mockRecoveryCodes("otter");
                            Action result = node.process(treeContext);

                            assertThat(result.sessionProperties).doesNotContainKey(NODE_TYPE);
                        });
                    });
                });
            }
        });
    }

    private void returnsNameCallback() {
        Action result = node.process(treeContext);

        assertThat(result.outcome).isNull();
        assertThat(result.callbacks).isNotNull().hasSize(1);
        assertThat(result.callbacks.get(0)).isInstanceOf(NameCallback.class);
    }

    private TreeContext getContext() {
        return new TreeContext(json(object(field(USERNAME, user), field(REALM, realm))),
                new Builder().build(), callbacks, Optional.empty());
    }

    private void mockRecoveryCodes(String... codes) throws Exception {
        given(manager.getDeviceProfiles(anyString(), anyString())).willAnswer(ignored -> singletonList(settings));
        settings.setRecoveryCodes(Arrays.asList(codes));
    }
}
