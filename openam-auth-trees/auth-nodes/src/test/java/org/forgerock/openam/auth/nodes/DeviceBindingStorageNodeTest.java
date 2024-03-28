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
 * Copyright 2023-2024 ForgeRock AS.
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.security.auth.callback.Callback;

import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.binding.DeviceBindingJsonUtils;
import org.forgerock.openam.core.rest.devices.binding.DeviceBindingManager;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;

/**
 * Test for Device Unbinding.
 */
public class DeviceBindingStorageNodeTest {

    @Mock
    DeviceBindingManager deviceBindingManager;

    @Mock
    CoreWrapper coreWrapper;

    @Mock
    AMIdentity amIdentity;

    @Mock
    LegacyIdentityService identityService;

    @Mock
    DeviceBindingJsonUtils deviceBindingJsonUtils;

    @InjectMocks
    DeviceBindingStorageNode node;

    @BeforeMethod
    public void setup() throws IdRepoException, SSOException, DevicePersistenceException {
        node = null;
        openMocks(this);

        given(identityService.getAmIdentity(any(SSOToken.class), any(String.class), eq(IdType.USER), any()))
                .willReturn(amIdentity);

        given(identityService.getUniversalId(any(), (IdType) any(), any()))
                .willReturn(UUID.randomUUID().toString());

        given(identityService.getUniversalId(any(), any(), (IdType) any())).willReturn(Optional.of("bob"));

        given(coreWrapper.getIdentity(anyString())).willReturn(amIdentity);

        doNothing().when(deviceBindingManager).saveDeviceProfile(anyString(), anyString(), any());

        given(amIdentity.isExists()).willReturn(true);
        given(amIdentity.isActive()).willReturn(true);
        given(amIdentity.getName()).willReturn("bob");
        given(amIdentity.getRealm()).willReturn("/");
        given(amIdentity.getUniversalId()).willReturn("bob");

    }

    @Test
    public void testDeviceMissing()
            throws NodeProcessException {
        JsonValue sharedState = json(object(field(USERNAME, "bob"), field(REALM, "/realm")));
        JsonValue transientState = json(object());

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));
        assertThat(result.outcome).isEqualTo("false");

    }

    @Test
    public void testDeviceStorage()
            throws NodeProcessException {
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