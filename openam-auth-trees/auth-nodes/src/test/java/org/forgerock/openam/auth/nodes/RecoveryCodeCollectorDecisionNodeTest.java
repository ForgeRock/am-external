/*
 * Copyright 2018 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.cuppa.Cuppa.*;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.BDDMockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;

import org.forgerock.cuppa.Test;
import org.forgerock.cuppa.junit.CuppaRunner;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.RecoveryCodeCollectorDecisionNode.Config;
import org.forgerock.openam.auth.nodes.RecoveryCodeCollectorDecisionNode.RecoveryCodeType;
import org.forgerock.openam.core.rest.devices.DeviceSettings;
import org.forgerock.openam.core.rest.devices.UserDeviceSettingsDao;
import org.forgerock.openam.core.rest.devices.oath.OathDeviceSettings;
import org.forgerock.openam.core.rest.devices.push.PushDeviceSettings;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
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
    private UserDeviceSettingsDao<OathDeviceSettings> oathDevicesDao;
    @Mock
    private UserDeviceSettingsDao<PushDeviceSettings> pushDevicesDao;
    @Mock
    private DeviceSettings deviceSettings;
    private UserDeviceSettingsDao<? extends DeviceSettings> dao;
    private TreeContext treeContext;
    private List<Callback> callbacks;
    private RecoveryCodeCollectorDecisionNode node;

    {
        describe("Recovery Code Collector Decision Node", () -> {
            beforeEach(() -> {
                MockitoAnnotations.initMocks(this);
            });
            for (String type : Arrays.asList("OATH", "PUSH")) {
                when("recovery code type is: " + type, () -> {
                    beforeEach(() -> {
                        given(config.recoveryCodeType()).willReturn(RecoveryCodeType.valueOf(type));
                        node = new RecoveryCodeCollectorDecisionNode(config, oathDevicesDao, pushDevicesDao);
                        dao = "OATH".equals(type) ? oathDevicesDao : pushDevicesDao;
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
                            verify(dao).readDeviceSettings("badger", "weasel");
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
                            Action result = node.process(treeContext);

                            assertThat(result.outcome).isEqualTo("true");
                            ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
                            verify(deviceSettings).setRecoveryCodes(captor.capture());
                            assertThat(captor.getValue()).hasSize(1).containsOnly("otter");

                            ArgumentCaptor<List> listCaptor = ArgumentCaptor.forClass(List.class);
                            verify(dao).saveDeviceSettings(eq(user), eq(realm), listCaptor.capture());
                            assertThat(listCaptor.getValue()).hasSize(1).containsOnly(deviceSettings);
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
        return new TreeContext(json(object(field(USERNAME, user), field(REALM, realm))), new Builder().build(),
                callbacks);
    }

    private void mockRecoveryCodes(String... codes) throws Exception {
        given(dao.readDeviceSettings(anyString(), anyString())).willAnswer(ignored -> singletonList(deviceSettings));
        given(deviceSettings.getRecoveryCodes()).willReturn(Arrays.asList(codes));
    }
}
