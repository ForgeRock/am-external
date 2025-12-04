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
 * Copyright 2023-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;

import java.util.List;
import java.util.Optional;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.binding.DeviceBindingJsonUtils;
import org.forgerock.openam.core.rest.devices.binding.DeviceBindingManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;

/**
 * Test for Device Unbinding.
 */
@ExtendWith(MockitoExtension.class)
public class DeviceBindingStorageNodeTest {

    @Mock
    DeviceBindingManager deviceBindingManager;

    @Mock
    AMIdentity amIdentity;

    @Mock
    DeviceBindingJsonUtils deviceBindingJsonUtils;

    @Mock
    NodeUserIdentityProvider identityProvider;

    @InjectMocks
    DeviceBindingStorageNode node;

    @BeforeEach
    void setup() throws IdRepoException, SSOException, DevicePersistenceException {
        given(identityProvider.getAMIdentity(any(), any())).willReturn(Optional.of(amIdentity));
    }

    @Test
    void testDeviceMissing()
            throws NodeProcessException {
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "/realm")));
        JsonValue transientState = json(object());

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));
        assertThat(result.outcome).isEqualTo("false");

    }

    @Test
    void testDeviceStorage() throws Exception {
        doNothing().when(deviceBindingManager).saveDeviceProfile(anyString(), anyString(), any());
        given(amIdentity.getName()).willReturn("bob");
        given(amIdentity.getRealm()).willReturn("/");
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "/realm")));
        JsonValue transientState = json(object(field(DeviceBinding.DEVICE, "dummy")));

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));
        assertThat(result.outcome).isEqualTo("true");

    }


    private TreeContext getContext(JsonValue sharedState, JsonValue transientState,
            List<? extends Callback> callbacks) {
        return new TreeContext(sharedState, transientState, new Builder().build(), callbacks,
                Optional.empty());
    }

}
